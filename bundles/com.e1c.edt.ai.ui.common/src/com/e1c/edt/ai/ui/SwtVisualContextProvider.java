/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.Section;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.assistent.model.VisualContext;
import com.e1c.edt.ai.assistent.model.VisualEditorInfo;
import com.e1c.edt.ai.assistent.model.VisualField;
import com.e1c.edt.ai.assistent.model.VisualGroup;
import com.e1c.edt.ai.assistent.model.VisualSnapshot;
import com.e1c.edt.ai.assistent.model.VisualWindow;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Generic SWT implementation of {@link IVisualContextProvider}: captures what the user sees in the IDE.
 * Data selection follows the viewport principle: tables/trees report only the rows visible to the user,
 * the active editor reports only the visible part of the document. The capture budget caps are safety
 * guards against pathological cases only.
 */
@SuppressWarnings("nls")
public class SwtVisualContextProvider
    implements IVisualContextProvider
{
    private static final int MAX_CONTROLS = 2000;
    private static final int MAX_VALUE_LENGTH = 2000;
    private static final int MAX_EDITOR_TEXT_LENGTH = 8192;
    private static final int MAX_OPTIONS = 100;
    private static final int MAX_ROWS = 200;
    private static final long MAX_CAPTURE_TIME_NANOS = 1_000_000_000L;

    private final IDispatcher dispatcher;
    private final IClipboard clipboard;
    private final IUI ui;
    private final ILog log;

    @Inject
    public SwtVisualContextProvider(IDispatcher dispatcher, IClipboard clipboard, IUI ui, ILog log)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(clipboard);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(log);
        this.dispatcher = dispatcher;
        this.clipboard = clipboard;
        this.ui = ui;
        this.log = log;
    }

    @Override
    public VisualContext create(Object controlObject, ICancellationToken cancellationToken)
    {
        var ctx = new VisualContext();
        if (!(controlObject instanceof Control))
        {
            return ctx;
        }

        var control = (Control)controlObject;
        var root = control.getParent();
        var rootGroup = new VisualGroup();
        rootGroup.fields = new ArrayList<>();
        while (root != null && !cancellationToken.isCanceled())
        {
            if (root instanceof Form)
            {
                rootGroup.title = ((Form)root).getText();
                break;
            }

            if (root instanceof Shell)
            {
                rootGroup.title = ((Shell)root).getText();
                break;
            }

            if (root instanceof Section)
            {
                rootGroup.title = ((Section)root).getText();
            }

            root = root.getParent();
        }

        if (root == null || cancellationToken.isCanceled())
        {
            return ctx;
        }

        var groups = new ArrayList<VisualGroup>();
        collectFrom(control, root, rootGroup, groups, new CaptureBudget(), cancellationToken);

        if (rootGroup.title != null && !rootGroup.title.isBlank())
        {
            ctx.title = rootGroup.title;
        }

        if (!rootGroup.fields.isEmpty())
        {
            ctx.fields = rootGroup.fields;
        }

        if (!groups.isEmpty())
        {
            ctx.groups = groups;
        }

        return ctx;
    }

    @Override
    public VisualSnapshot createSnapshot(ICancellationToken cancellationToken)
    {
        return dispatcher.<VisualSnapshot> dispatch(() -> capture(cancellationToken)).orElseGet(VisualSnapshot::new);
    }

    /**
     * Collects fields starting from the resolved root. Subclasses may override to route custom
     * widget toolkits (e.g. 1C LWT in EDT).
     */
    protected void collectFrom(Control control, Composite root, VisualGroup rootGroup, List<VisualGroup> groups,
        CaptureBudget budget, ICancellationToken cancellationToken)
    {
        findElements(root, rootGroup, groups, budget, cancellationToken);
    }

    /**
     * Extension hook called for every SWT child before the generic mapping.
     *
     * @return true if the child was fully handled and generic traversal must not process it
     */
    protected boolean visitCustomChild(Control child, VisualGroup currentGroup, List<VisualGroup> groups,
        CaptureBudget budget, ICancellationToken cancellationToken)
    {
        return false;
    }

    protected void findElements(Composite composite, VisualGroup currentGroup, List<VisualGroup> groups,
        CaptureBudget budget, ICancellationToken cancellationToken)
    {
        var pending = new VisualField();
        for (var child : composite.getChildren())
        {
            if (cancellationToken.isCanceled() || budget.isExhausted())
            {
                return;
            }

            budget.onControl();
            if (!child.getVisible())
            {
                continue;
            }

            if (visitCustomChild(child, currentGroup, groups, budget, cancellationToken))
            {
                continue;
            }

            pending = visitControl(child, pending, currentGroup, groups, budget, cancellationToken);
        }
    }

    private VisualField visitControl(Control child, VisualField pending, VisualGroup currentGroup,
        List<VisualGroup> groups, CaptureBudget budget, ICancellationToken cancellationToken)
    {
        if (child instanceof Label)
        {
            appendName(pending, ((Label)child).getText());
            return pending;
        }

        if (child instanceof CLabel)
        {
            appendName(pending, ((CLabel)child).getText());
            return pending;
        }

        if (child instanceof Link)
        {
            pending.kind = "link";
            pending.value = ((Link)child).getText();
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof org.eclipse.swt.widgets.Text)
        {
            var item = (org.eclipse.swt.widgets.Text)child;
            pending.kind = "text";
            pending.isMultiline = (item.getStyle() & SWT.MULTI) != 0;
            pending.value = truncate(item.getText(), pending, budget.getMaxValueLength());
            setSelectedText(pending, item.getSelectionText(), budget);
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof StyledText)
        {
            var item = (StyledText)child;
            if (ui.getSourceViewer(item).isPresent())
            {
                // Source editors are reported via VisualSnapshot.activeEditor (viewport only),
                // dumping the whole document here would blow up the context.
                return pending;
            }

            pending.kind = "text";
            pending.isMultiline = (item.getStyle() & SWT.MULTI) != 0;
            pending.value = truncate(item.getText(), pending, budget.getMaxValueLength());
            setSelectedText(pending, item.getSelectionText(), budget);
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof Button)
        {
            var item = (Button)child;
            var style = item.getStyle();
            if ((style & SWT.CHECK) != 0)
            {
                pending.kind = "checkbox";
            }
            else if ((style & SWT.RADIO) != 0)
            {
                pending.kind = "radio";
            }
            else
            {
                pending.kind = "button";
            }

            appendName(pending, item.getText());
            if ((style & (SWT.CHECK | SWT.RADIO | SWT.TOGGLE)) != 0)
            {
                pending.isChecked = item.getSelection();
            }

            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof Combo)
        {
            var item = (Combo)child;
            pending.kind = "combo";
            pending.value = truncate(item.getText(), pending, budget.getMaxValueLength());
            pending.options = capOptions(List.of(item.getItems()), pending);
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof CCombo)
        {
            var item = (CCombo)child;
            pending.kind = "combo";
            pending.value = truncate(item.getText(), pending, budget.getMaxValueLength());
            pending.options = capOptions(List.of(item.getItems()), pending);
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof org.eclipse.swt.widgets.List)
        {
            var item = (org.eclipse.swt.widgets.List)child;
            pending.kind = "list";
            pending.options = capOptions(List.of(item.getItems()), pending);
            var selection = item.getSelection();
            if (selection.length > 0)
            {
                pending.value = truncate(String.join(", ", selection), pending, budget.getMaxValueLength());
            }

            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof Spinner)
        {
            var item = (Spinner)child;
            pending.kind = "spinner";
            pending.value = item.getText();
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof DateTime)
        {
            var item = (DateTime)child;
            pending.kind = "datetime";
            if ((item.getStyle() & SWT.TIME) != 0)
            {
                pending.value = String.format("%02d:%02d:%02d", item.getHours(), item.getMinutes(), item.getSeconds());
            }
            else
            {
                pending.value = String.format("%04d-%02d-%02d", item.getYear(), item.getMonth() + 1, item.getDay());
            }

            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof Table)
        {
            captureTable((Table)child, pending, budget);
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof Tree)
        {
            captureTree((Tree)child, pending, budget);
            fillCommon(pending, child);
            return complete(pending, currentGroup);
        }

        if (child instanceof TabFolder)
        {
            var item = (TabFolder)child;
            var options = new ArrayList<String>();
            for (var tab : item.getItems())
            {
                options.add(tab.getText());
            }

            pending.kind = "tabs";
            pending.options = capOptions(options, pending);
            var selection = item.getSelection();
            if (selection.length > 0)
            {
                pending.value = selection[0].getText();
                complete(pending, currentGroup);
                descendIntoTab(selection[0].getControl(), selection[0].getText(), groups, budget, cancellationToken);
                return new VisualField();
            }

            return complete(pending, currentGroup);
        }

        if (child instanceof CTabFolder)
        {
            var item = (CTabFolder)child;
            var options = new ArrayList<String>();
            for (var tab : item.getItems())
            {
                options.add(tab.getText());
            }

            pending.kind = "tabs";
            pending.options = capOptions(options, pending);
            var selection = item.getSelection();
            if (selection != null)
            {
                pending.value = selection.getText();
                complete(pending, currentGroup);
                descendIntoTab(selection.getControl(), selection.getText(), groups, budget, cancellationToken);
                return new VisualField();
            }

            return complete(pending, currentGroup);
        }

        if (child instanceof ToolBar)
        {
            var options = new ArrayList<String>();
            for (var item : ((ToolBar)child).getItems())
            {
                var text = item.getText();
                if (text != null && !text.isBlank())
                {
                    options.add(text);
                }
            }

            if (!options.isEmpty())
            {
                pending.kind = "toolbar";
                pending.options = capOptions(options, pending);
                return complete(pending, currentGroup);
            }

            return pending;
        }

        if (child instanceof Group)
        {
            var item = (Group)child;
            descendIntoGroup(item, item.getText(), groups, budget, cancellationToken);
            return pending;
        }

        if (child instanceof Section)
        {
            var item = (Section)child;
            descendIntoGroup(item, item.getText(), groups, budget, cancellationToken);
            return pending;
        }

        if (child instanceof Form)
        {
            var item = (Form)child;
            descendIntoGroup(item.getBody(), item.getText(), groups, budget, cancellationToken);
            return pending;
        }

        if (child instanceof Composite)
        {
            findElements((Composite)child, currentGroup, groups, budget, cancellationToken);
            return pending;
        }

        return pending;
    }

    private void descendIntoTab(Control control, String title, List<VisualGroup> groups, CaptureBudget budget,
        ICancellationToken cancellationToken)
    {
        if (control instanceof Composite)
        {
            descendIntoGroup((Composite)control, title, groups, budget, cancellationToken);
        }
    }

    private void descendIntoGroup(Composite composite, String title, List<VisualGroup> groups, CaptureBudget budget,
        ICancellationToken cancellationToken)
    {
        var group = new VisualGroup();
        group.title = title;
        group.fields = new ArrayList<>();
        groups.add(group);
        findElements(composite, group, groups, budget, cancellationToken);
        if (group.fields.isEmpty())
        {
            group.fields = null;
        }
    }

    private void captureTable(Table table, VisualField field, CaptureBudget budget)
    {
        field.kind = "table";
        var columnCount = table.getColumnCount();
        if (columnCount > 0)
        {
            var columns = new ArrayList<String>();
            for (var column : table.getColumns())
            {
                columns.add(column.getText());
            }

            field.columns = columns;
        }

        var cellCount = Math.max(1, columnCount);
        var rows = new ArrayList<List<String>>();
        var visibleRows = visibleRowCount(table.getClientArea().height, table.getItemHeight());
        var top = table.getTopIndex();
        var end = Math.min(table.getItemCount(), top + visibleRows);
        for (var i = top; i < end && rows.size() < MAX_ROWS; i++)
        {
            rows.add(tableRow(table.getItem(i), cellCount, budget));
        }

        var selection = table.getSelection();
        if (selection.length > 0)
        {
            field.selectedText =
                truncate(String.join(" | ", tableRow(selection[0], cellCount, budget)), field, MAX_VALUE_LENGTH);
        }

        // Selected rows are always included, even when scrolled out of the viewport
        for (var index : table.getSelectionIndices())
        {
            if ((index < top || index >= end) && rows.size() < MAX_ROWS)
            {
                rows.add(tableRow(table.getItem(index), cellCount, budget));
            }
        }

        if (!rows.isEmpty())
        {
            field.rows = rows;
        }
    }

    private List<String> tableRow(TableItem item, int cellCount, CaptureBudget budget)
    {
        var cells = new ArrayList<String>();
        for (var i = 0; i < cellCount; i++)
        {
            cells.add(truncate(item.getText(i), null, budget.getMaxValueLength()));
        }

        return cells;
    }

    private void captureTree(Tree tree, VisualField field, CaptureBudget budget)
    {
        field.kind = "tree";
        var columnCount = tree.getColumnCount();
        if (columnCount > 0)
        {
            var columns = new ArrayList<String>();
            for (var column : tree.getColumns())
            {
                columns.add(column.getText());
            }

            field.columns = columns;
        }

        var cellCount = Math.max(1, columnCount);
        var rows = new ArrayList<List<String>>();
        var visibleRows = visibleRowCount(tree.getClientArea().height, tree.getItemHeight());
        var item = tree.getTopItem();
        while (item != null && rows.size() < visibleRows && rows.size() < MAX_ROWS)
        {
            rows.add(treeRow(item, cellCount, budget));
            item = nextVisibleTreeItem(tree, item);
        }

        var selection = tree.getSelection();
        if (selection.length > 0)
        {
            field.selectedText =
                truncate(String.join(" | ", treeRow(selection[0], cellCount, budget)), field, MAX_VALUE_LENGTH);
        }

        if (!rows.isEmpty())
        {
            field.rows = rows;
        }
    }

    private List<String> treeRow(TreeItem item, int cellCount, CaptureBudget budget)
    {
        var cells = new ArrayList<String>();
        var depth = 0;
        for (var parent = item.getParentItem(); parent != null; parent = parent.getParentItem())
        {
            depth++;
        }

        for (var i = 0; i < cellCount; i++)
        {
            var text = truncate(item.getText(i), null, budget.getMaxValueLength());
            cells.add(i == 0 ? "  ".repeat(depth) + text : text);
        }

        return cells;
    }

    private TreeItem nextVisibleTreeItem(Tree tree, TreeItem item)
    {
        if (item.getExpanded() && item.getItemCount() > 0)
        {
            return item.getItem(0);
        }

        for (var current = item; current != null; current = current.getParentItem())
        {
            var parent = current.getParentItem();
            var siblings = parent == null ? tree.getItems() : parent.getItems();
            for (var i = 0; i < siblings.length; i++)
            {
                if (siblings[i] == current)
                {
                    if (i + 1 < siblings.length)
                    {
                        return siblings[i + 1];
                    }

                    break;
                }
            }
        }

        return null;
    }

    private int visibleRowCount(int clientAreaHeight, int itemHeight)
    {
        return Math.max(1, clientAreaHeight / Math.max(1, itemHeight) + 1);
    }

    private VisualSnapshot capture(ICancellationToken cancellationToken)
    {
        var snapshot = new VisualSnapshot();
        var windows = new ArrayList<VisualWindow>();
        snapshot.windows = windows;
        var display = Display.getCurrent();
        if (display == null)
        {
            return snapshot;
        }

        var budget = new CaptureBudget();
        var activeShell = display.getActiveShell();
        var shells = new ArrayList<Shell>();
        if (activeShell != null && activeShell.isVisible())
        {
            shells.add(activeShell);
        }

        for (var shell : display.getShells())
        {
            if (shell != activeShell && shell.isVisible())
            {
                shells.add(shell);
            }
        }

        for (var shell : shells)
        {
            if (cancellationToken.isCanceled() || budget.isExhausted())
            {
                break;
            }

            var window = new VisualWindow();
            window.title = shell.getText();
            if (shell == activeShell)
            {
                window.isActive = Boolean.TRUE;
            }

            if ((shell.getStyle() & (SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL | SWT.PRIMARY_MODAL)) != 0)
            {
                window.isModal = Boolean.TRUE;
            }

            if (shell.getParent() != null)
            {
                window.isDialog = Boolean.TRUE;
            }

            window.fields = new ArrayList<>();
            var groups = new ArrayList<VisualGroup>();
            findElements(shell, window, groups, budget, cancellationToken);
            if (window.fields.isEmpty())
            {
                window.fields = null;
            }

            if (!groups.isEmpty())
            {
                window.groups = groups;
            }

            windows.add(window);
        }

        try
        {
            snapshot.activeEditor = captureActiveEditor();
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        try
        {
            snapshot.clipboard = clipboard.getClipboardInfo().orElse(null);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        return snapshot;
    }

    private VisualEditorInfo captureActiveEditor()
    {
        var workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null)
        {
            return null;
        }

        var page = workbenchWindow.getActivePage();
        if (page == null)
        {
            return null;
        }

        var editor = page.getActiveEditor();
        if (editor == null)
        {
            return null;
        }

        var info = new VisualEditorInfo();
        info.title = editor.getTitle();
        info.isDirty = editor.isDirty();
        var input = editor.getEditorInput();
        if (input != null)
        {
            var file = input.getAdapter(IFile.class);
            if (file != null)
            {
                info.path = file.getFullPath().makeRelative().toPortableString();
            }
        }

        var target = editor.getAdapter(ITextOperationTarget.class);
        if (target instanceof ITextViewer)
        {
            var textWidget = ((ITextViewer)target).getTextWidget();
            if (textWidget != null && !textWidget.isDisposed())
            {
                fillEditorViewport(info, textWidget);
            }
        }

        return info;
    }

    private void fillEditorViewport(VisualEditorInfo info, StyledText textWidget)
    {
        // Only what the user sees: the viewport lines, never the whole document
        var topLine = JFaceTextUtil.getPartialTopIndex(textWidget);
        var bottomLine = JFaceTextUtil.getPartialBottomIndex(textWidget);
        if (bottomLine >= topLine && textWidget.getLineCount() > 0)
        {
            var start = textWidget.getOffsetAtLine(topLine);
            var end = textWidget.getOffsetAtLine(bottomLine) + textWidget.getLine(bottomLine).length();
            if (end > start)
            {
                var visibleText = textWidget.getText(start, end - 1);
                if (visibleText.length() > MAX_EDITOR_TEXT_LENGTH)
                {
                    visibleText = visibleText.substring(0, MAX_EDITOR_TEXT_LENGTH);
                }

                info.visibleText = visibleText;
            }
        }

        var selectedText = textWidget.getSelectionText();
        if (selectedText != null && !selectedText.isEmpty())
        {
            if (selectedText.length() > MAX_EDITOR_TEXT_LENGTH)
            {
                selectedText = selectedText.substring(0, MAX_EDITOR_TEXT_LENGTH);
            }

            info.selectedText = selectedText;
        }

        var caretOffset = textWidget.getCaretOffset();
        var caretLine = textWidget.getLineAtOffset(caretOffset);
        info.cursorLine = caretLine + 1;
        info.cursorColumn = caretOffset - textWidget.getOffsetAtLine(caretLine) + 1;
    }

    private void appendName(VisualField field, String text)
    {
        if (text == null)
        {
            return;
        }

        var name = field.name;
        field.name = ((name == null ? "" : name + " ") + text).trim();
    }

    private void fillCommon(VisualField field, Control control)
    {
        field.isFocused = control.isFocusControl();
        if (!control.isEnabled())
        {
            field.isEnabled = Boolean.FALSE;
        }
    }

    private void setSelectedText(VisualField field, String selectedText, CaptureBudget budget)
    {
        if (selectedText != null && !selectedText.isEmpty())
        {
            field.selectedText = truncate(selectedText, field, budget.getMaxValueLength());
        }
    }

    private VisualField complete(VisualField field, VisualGroup currentGroup)
    {
        if (currentGroup.fields == null)
        {
            currentGroup.fields = new ArrayList<>();
        }

        currentGroup.fields.add(field);
        return new VisualField();
    }

    private List<String> capOptions(List<String> options, VisualField field)
    {
        if (options.isEmpty())
        {
            return null;
        }

        if (options.size() <= MAX_OPTIONS)
        {
            return options;
        }

        field.isTruncated = Boolean.TRUE;
        return new ArrayList<>(options.subList(0, MAX_OPTIONS));
    }

    protected String truncate(String value, VisualField field, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }

        if (field != null)
        {
            field.isTruncated = Boolean.TRUE;
        }

        return value.substring(0, maxLength);
    }

    /**
     * Safety guards against pathological cases; data selection itself is viewport-driven.
     */
    public static class CaptureBudget
    {
        private final long deadlineNanos = System.nanoTime() + MAX_CAPTURE_TIME_NANOS;
        private int remainingControls = MAX_CONTROLS;

        public boolean isExhausted()
        {
            return remainingControls <= 0 || System.nanoTime() > deadlineNanos;
        }

        public void onControl()
        {
            remainingControls--;
        }

        public int getMaxValueLength()
        {
            return MAX_VALUE_LENGTH;
        }
    }
}
