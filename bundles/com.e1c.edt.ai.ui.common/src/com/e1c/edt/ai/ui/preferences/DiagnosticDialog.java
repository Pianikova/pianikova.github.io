/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.DiagnosticResult;
import com.e1c.edt.ai.assistent.DiagnosticSeverity;
import com.e1c.edt.ai.assistent.IDiagnosticContext;
import com.e1c.edt.ai.assistent.IDiagnosticTest;
import com.e1c.edt.ai.assistent.IDiagnosticsFactory;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IWeb;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class DiagnosticDialog
    extends TitleAreaDialog
{
    private final List<IDiagnosticTest> tests;

    private ProgressBar progressBar;
    private Label currentTestLabel;

    private Table resultTable;
    private TableColumn iconColumn;
    private TableColumn titleColumn;
    private TableColumn statusColumn;
    private TableColumn buttonColumn;
    private Label summaryLabel;

    private final List<TableEditor> tableEditors = new ArrayList<>();
    private final AtomicReference<Job> runningJob = new AtomicReference<>();
    private final List<Map.Entry<IDiagnosticTest, DiagnosticResult>> outcomes = new ArrayList<>();

    @Inject
    private IDispatcher dispatcher;
    @Inject
    private IDiagnosticsFactory diagnosticsFactory;
    @Inject
    private IDiagnosticContext context;
    @Inject
    private IDiagnosticReportDialogProvider diagnosticsReportDialogProvider;
    @Inject
    private ISettings settings;
    @Inject
    private IWeb web;

    public DiagnosticDialog(Shell shell)
    {
        super(shell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        BaseActivator.injectMembers(this);
        this.tests = diagnosticsFactory.createDiagnostics();
        setHelpAvailable(false);
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setMinimumSize(720, 360);
    }

    @Override
    public void create()
    {
        super.create();
        setTitle(Messages.DiagnosticDialog_Title);
        setMessage(Messages.DiagnosticDialog_Message);
        dispatcher.dispatchAsync(() -> {
            if (getShell() != null && !getShell().isDisposed())
            {
                startDiagnostic();
            }
        });
        getShell().addListener(SWT.Dispose, e -> cancelDiagnostic());
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        var area = (Composite)super.createDialogArea(parent);
        var root = new Composite(area, SWT.NONE);
        root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        var gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 12;
        gridLayout.marginHeight = 10;
        gridLayout.verticalSpacing = 8;
        root.setLayout(gridLayout);

        progressBar = new ProgressBar(root, SWT.SMOOTH);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        progressBar.setMinimum(0);
        progressBar.setMaximum(Math.max(1, tests.size()));
        progressBar.setSelection(0);

        currentTestLabel = new Label(root, SWT.WRAP);
        var currentTest = new GridData(SWT.FILL, SWT.TOP, true, false);
        currentTest.widthHint = 620;
        currentTestLabel.setLayoutData(currentTest);
        currentTestLabel.setText(Messages.DiagnosticDialog_Preparing);

        summaryLabel = new Label(root, SWT.WRAP);
        var result = new GridData(SWT.FILL, SWT.TOP, true, false);
        result.widthHint = 620;
        summaryLabel.setLayoutData(result);
        summaryLabel.setText(""); //$NON-NLS-1$
        setExcluded(summaryLabel, true);

        resultTable = new Table(root, SWT.FULL_SELECTION | SWT.BORDER);
        resultTable.setHeaderVisible(false);
        resultTable.setLinesVisible(true);

        var tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableGd.heightHint = 220;
        resultTable.setLayoutData(tableGd);

        iconColumn = new TableColumn(resultTable, SWT.LEFT); // icon
        titleColumn = new TableColumn(resultTable, SWT.LEFT); // test name
        statusColumn = new TableColumn(resultTable, SWT.LEFT); // result
        buttonColumn = new TableColumn(resultTable, SWT.LEFT); // report button (optional)

        resultTable.addListener(SWT.Resize, e -> {
            if (resultTable == null || resultTable.isDisposed())
            {
                return;
            }
            int width = resultTable.getClientArea().width;
            if (width <= 0)
            {
                return;
            }
            int iconColumnWidth = 40;
            int statusColumnWidth = 90;
            int buttonColumnWidth = 140;
            int titleColumnWidth = width - iconColumnWidth - statusColumnWidth - buttonColumnWidth;
            if (titleColumnWidth < 150)
            {
                titleColumnWidth = 150;
            }
            titleColumn.setWidth(titleColumnWidth);
            iconColumn.setWidth(iconColumnWidth);
            statusColumn.setWidth(statusColumnWidth);
            buttonColumn.setWidth(buttonColumnWidth);
        });
        resultTable.redraw();
        setExcluded(resultTable, true);

        var troubleshootingLink = new Link(root, SWT.NONE);
        var linkGd = new GridData(SWT.FILL, SWT.END, true, false);
        troubleshootingLink.setLayoutData(linkGd);
        troubleshootingLink.setText(Messages.DiagnosticDialog_TroubleshootingLink);
        troubleshootingLink.addSelectionListener(new SelectionListener()
        {

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String homePage = settings.getHomePage();
                if (homePage != null && !homePage.isEmpty())
                {
                    web.browse(homePage + "/troubleshooting/"); //$NON-NLS-1$
                }
                else
                {
                    web.browse("https://code.1c.ai/troubleshooting/"); //$NON-NLS-1$
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
            }
        });

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, Messages.DiagnosticDialog_CloseButton, true);
    }

    @SuppressWarnings("nls")
    private void startDiagnostic()
    {
        if (runningJob.get() != null)
        {
            return;
        }

        // reset UI
        progressBar.setMaximum(tests.size());
        progressBar.setSelection(0);
        currentTestLabel.setText(Messages.DiagnosticDialog_Preparing);
        setMessage(Messages.DiagnosticDialog_CheckingConnection);

        outcomes.clear();
        clearResultsUI();

        var job = dispatcher.createJob(Messages.DiagnosticDialog_Title, jobCtx -> {
            var project = ProjectId.Default;
            var stopFuther = false;
            context.setProject(project);
            context.setAIContext(new AIContext(project, "", null)); //$NON-NLS-1$
            for (int i = 0; i < tests.size(); i++) {
                if (jobCtx.Monitor.isCanceled())
                {
                    return;
                }
                var test = tests.get(i);
                int step = i + 1;
                dispatcher.dispatchAsync(() -> {
                    if (isUiGone()) {
                        return;
                    }
                    currentTestLabel.setText(test.title());
                    currentTestLabel.getParent().layout(true, true);
                });

                DiagnosticResult result;
                if (stopFuther)
                {
                    result = DiagnosticResult.defaultResult();
                }
                else
                {
                    try
                    {
                        result = test.execute(context, jobCtx.Monitor);
                        if (result == null)
                        {
                            result = DiagnosticResult.error(Messages.DiagnosticDialog_ExecutionFailed,
                                ServiceState.NONE, null, null, "DiagnosticResult was null");
                        }
                    }
                    catch (Throwable t)
                    {
                        result = DiagnosticResult.error(Messages.DiagnosticDialog_ExecutionFailed, ServiceState.NONE,
                            null, t, stackTraceToString(t));
                    }
                }
                if (result.getSeverity() != DiagnosticSeverity.OK)
                {
                    stopFuther = true;
                }

                outcomes.add(Map.entry(test, result));

                dispatcher.dispatchAsync(() -> {
                    if (isUiGone())
                    {
                        return;
                    }
                    progressBar.setSelection(step);
                });
            }
            dispatcher.dispatchAsync(() -> showResults(outcomes));
            context.releaseContext();
            return;
        }, false, CancellationTokens.NONE);

        runningJob.set(job);
        job.addJobChangeListener(new JobChangeAdapter()
        {
            @Override
            public void done(IJobChangeEvent job)
            {
                runningJob.set(null);
            }
        });
        job.schedule();
    }

    /**
     * Set the exclude (hidden) flag of the given control.
     * <p>
     * <code>True</code> in order to hide the control, <code>false</code> to show it.
     *
     * @param control
     * @param excluded
     */
    private void setExcluded(Control control, boolean excluded)
    {
        var gd = (GridData)control.getLayoutData();
        gd.exclude = excluded;
        control.setVisible(!excluded);
    }

    private void buildResultsTable(List<Map.Entry<IDiagnosticTest, DiagnosticResult>> results)
    {
        for (var editor : tableEditors)
        {
            var control = editor.getEditor();
            if (control != null && !control.isDisposed())
            {
                control.dispose();
            }
            editor.dispose();
        }
        tableEditors.clear();

        for (var entry : results)
        {
            var processedSeverity = processSeverity(entry.getValue());
            var severity = processedSeverity.getValue();
            var image = processedSeverity.getKey();
            var tableItem = new TableItem(resultTable, SWT.NONE);
            tableItem.setImage(0, image);
            tableItem.setText(1, entry.getKey().title());
            tableItem.setText(2, severity);

            if (entry.getValue().getSeverity() == DiagnosticSeverity.ERROR)
            {
                var reportButton = new Button(resultTable, SWT.PUSH);
                reportButton.setText(Messages.DiagnosticDialog_OpenReport);
                reportButton.addSelectionListener(new SelectionListener()                {

                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        diagnosticsReportDialogProvider.openErrorDialog(getShell(), entry.getKey(), entry.getValue(),
                            context);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e)
                    {
                        // nothing
                    }
                });

                var editor = new TableEditor(resultTable);
                editor.grabHorizontal = true;
                editor.minimumWidth = 130;
                editor.setEditor(reportButton, tableItem, 3);

                tableEditors.add(editor);
                tableItem.addDisposeListener(new DisposeListener()
                {

                    @Override
                    public void widgetDisposed(DisposeEvent e)
                    {
                        if (!reportButton.isDisposed())
                        {
                            reportButton.dispose();
                        }
                        editor.dispose();
                    }
                });
            }
            else
            { // just not to destroy the layout
                tableItem.setText(3, ""); //$NON-NLS-1$
            }
        }
    }

    private Map.Entry<Image, String> processSeverity(DiagnosticResult result)
    {
        Display display = Display.getCurrent();
        Map.Entry<Image, String> entry = null;
        switch (result.getSeverity())
        {
        case DEFAULT:
            entry = Map.entry(display.getSystemImage(SWT.ICON_WARNING), Messages.DiagnosticDialog_Skip);
            break;
        case OK:
            entry = Map.entry(display.getSystemImage(SWT.ICON_INFORMATION), Messages.DiagnosticDialog_OK);
            break;
        case ERROR:
        default:
            entry = Map.entry(display.getSystemImage(SWT.ICON_ERROR), Messages.DiagnosticDialog_Failed);
            break;
        }

        return entry;
    }

    private void clearResultsUI()
    {
        setExcluded(summaryLabel, true);
        setExcluded(resultTable, true);
        summaryLabel.setText(""); //$NON-NLS-1$
        for (var editor : tableEditors)
        {
            var control = editor.getEditor();
            if (control != null && !control.isDisposed())
            {
                control.dispose();
            }
            editor.dispose();
        }
        tableEditors.clear();
        resultTable.removeAll();
        resultTable.getParent().layout(true, true);
    }

    private void showResults(List<Map.Entry<IDiagnosticTest, DiagnosticResult>> outcomes)
    {
        if (isUiGone())
        {
            return;
        }
        setMessage(Messages.DiagnosticDialog_Ready);
        currentTestLabel.setText(""); //$NON-NLS-1$

        int errorCount = 0;
        for (var entry : outcomes)
        {
            if (entry.getValue().getSeverity() == DiagnosticSeverity.ERROR)
            {
                errorCount++;
            }
        }

        if (errorCount == 0)
        {
            summaryLabel.setText(Messages.DiagnosticDialog_Successful);
        }
        else
        {
            summaryLabel.setText(Messages.DiagnosticDialog_ProblemsDetected + errorCount);
        }
        setExcluded(summaryLabel, false);
        setExcluded(resultTable, false);
        buildResultsTable(outcomes);

        summaryLabel.getParent().layout(true, true);
    }

    // @formatter:off
    private boolean isUiGone()
    {
        return getShell() == null || getShell().isDisposed()
            || progressBar == null || progressBar.isDisposed()
            || summaryLabel == null || summaryLabel.isDisposed()
            || currentTestLabel == null || currentTestLabel.isDisposed();
    }
    // @formatter:on

    private void cancelDiagnostic()
    {
        var job = runningJob.get();
        if (job != null)
        {
            job.cancel();
        }
    }

    private String stackTraceToString(Throwable t)
    {
        var sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
