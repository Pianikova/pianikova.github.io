/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class UI
    implements IUIInitializer, IUI, Listener
{
    private final ILog log;
    private final ICodeCompletionViewModel<CodeCompletionContext> codeCompletionViewModel;
    private final IDispatcher dispatcher;
    private final ITextWidgetInfoUpdater textWidgetInfoUpdater;
    private StyledText textWidget;
    private SourceViewer lastSourceViewer;
    private AutoCloseable queryToken = Closeables.Empty;

    @Inject
    public UI(ILog log, ICodeCompletionViewModel<CodeCompletionContext> codeCompletionViewModel,
        IDispatcher dispatcher, ITextWidgetInfoUpdater textWidgetInfoUpdater)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(codeCompletionViewModel);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(textWidgetInfoUpdater);
        this.log = log;
        this.codeCompletionViewModel = codeCompletionViewModel;
        this.dispatcher = dispatcher;
        this.textWidgetInfoUpdater = textWidgetInfoUpdater;
    }

    @Override
    public void initialize()
    {
        dispatcher.dispatchAsync(() -> {
            var display = Display.getDefault();
            display.addFilter(SWT.FocusIn, this);
            display.addFilter(SWT.FocusOut, this);
            var curControl = display.getFocusControl();
            if (curControl instanceof Widget)
            {
                var initEvent = new Event();
                initEvent.type = SWT.FocusIn;
                initEvent.widget = curControl;
                handleEvent(initEvent);
            }
        });
    }

    @Override
    public Optional<Shell> getShell()
    {
        return Optional.ofNullable(Display.getCurrent().getActiveShell());
    }

    @Override
    public synchronized void handleEvent(Event event)
    {
        Preconditions.checkNotNull(event);
        if (event.type == SWT.FocusIn && event.widget != textWidget && event.widget instanceof StyledText)
        {
            var newTextWidget = (StyledText)event.widget;
            if (isValidWidget(newTextWidget))
            {
                try
                {
                    queryToken.close();
                }
                catch (Exception e)
                {
                    // ignored
                }

                textWidget = newTextWidget;
                lastSourceViewer = getSourceViewer(newTextWidget).orElse(null);
                textWidgetInfoUpdater.reset();
                queryToken = Closeables.Empty;
                dispatcher.dispatchAsync(() -> {
                    queryToken = codeCompletionViewModel.activate(newTextWidget);
                });
            }
            else
            {
                textWidget = null;
            }
        }
    }

    @Override
    public synchronized Optional<StyledText> getTextWidget()
    {
        var widget = textWidget;
        if (!isValidWidget(widget))
        {
            return Optional.empty();
        }

        return Optional.of(widget);
    }

    @Override
    public synchronized Optional<SourceViewer> getLastSourceViewer()
    {
        return Optional.ofNullable(lastSourceViewer);
    }

    @Override
    public Optional<SourceViewer> getSourceViewer(StyledText textWidget)
    {
        Preconditions.checkNotNull(textWidget);
        if (textWidget.isDisposed())
        {
            return Optional.empty();
        }

        for (var workbench : PlatformUI.getWorkbench().getWorkbenchWindows())
        {
            for (var page : workbench.getPages())
            {
                for (var editorRef : page.getEditorReferences())
                {
                    var editor = editorRef.getEditor(false);
                    if (editor == null)
                    {
                        continue;
                    }

                    var curSourceViewer = editor.getAdapter(ITextOperationTarget.class);
                    if (curSourceViewer instanceof SourceViewer)
                    {
                        var surceViewer = (SourceViewer)curSourceViewer;
                        if (surceViewer.getTextWidget() == textWidget)
                        {
                            return Optional.of(surceViewer);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<IFile> getFile(SourceViewer sourceViewer)
    {
        for (var workbench : PlatformUI.getWorkbench().getWorkbenchWindows())
        {
            for (var page : workbench.getPages())
            {
                for (var editorRef : page.getEditorReferences())
                {
                    var editor = editorRef.getEditor(false);
                    if (editor == null)
                    {
                        continue;
                    }

                    var curSourceViewer = editor.getAdapter(ITextOperationTarget.class);
                    if (sourceViewer == curSourceViewer)
                    {
                        var file =
                            Optional.ofNullable(editor.getEditorInput()).map(input -> input.getAdapter(IFile.class));
                        if (file.isPresent())
                        {
                            return file;
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<IViewPart> showView(String viewId)
    {
        Preconditions.checkNotNull(viewId);
        return getActivePage().map(activePage -> {
            try
            {
                return activePage.showView(viewId);
            }
            catch (PartInitException e)
            {
                log.logError(e);
            }

            return null;
        });
    }

    private boolean isValidWidget(StyledText widget)
    {
        return widget != null && !widget.isDisposed() && widget.isEnabled()
            && widget.getVisible() && getSourceViewer(widget).isPresent();
    }

    private Optional<IWorkbenchPage> getActivePage()
    {
        return Optional.ofNullable(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
    }
}