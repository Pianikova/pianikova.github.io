/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ILog;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class UI
    implements IUI, Listener
{
    private final ILog log;
    private final Provider<ICodeCompletionViewModel<CodeCompletionContext>> codeCompletionViewModelProvider;
    private final Provider<IFinalCodeFeedbackViewModel> feedbackViewModelProvider;
    private final IDispatcher dispatcher;
    private StyledText textWidget;
    private AutoCloseable queryToken = Closeables.Empty;

    @Inject
    public UI(ILog log, Provider<ICodeCompletionViewModel<CodeCompletionContext>> codeCompletionViewModelProvider,
        Provider<IFinalCodeFeedbackViewModel> feedbackViewModelProvider, IDispatcher dispatcher)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(codeCompletionViewModelProvider);
        Preconditions.checkNotNull(feedbackViewModelProvider);
        Preconditions.checkNotNull(dispatcher);
        this.log = log;
        this.codeCompletionViewModelProvider = codeCompletionViewModelProvider;
        this.feedbackViewModelProvider = feedbackViewModelProvider;
        this.dispatcher = dispatcher;
    }

    public void initialize()
    {
        dispatcher.dispatch(() -> {
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
        try
        {
            queryToken.close();
        }
        catch (Exception e)
        {
            // ignored
        }

        if (event.type == SWT.FocusIn && event.widget instanceof StyledText)
        {
            var newTextWidget = (StyledText)event.widget;
            if (isValidWidget(newTextWidget))
            {
                textWidget = newTextWidget;
                queryToken = Closeables.create(codeCompletionViewModelProvider.get().activate(newTextWidget),
                    feedbackViewModelProvider.get().activate(newTextWidget));
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
    public Optional<SourceViewer> getSourceViewer(StyledText textWidget)
    {
        Preconditions.checkNotNull(textWidget);
        if (textWidget.isDisposed())
        {
            return Optional.empty();
        }

        return getAncestors(textWidget).filter(i -> i instanceof Canvas)
            .map(i -> getSourceViewer(((Canvas)i).getLayout()))
            .filter(i -> i != null)
            .findFirst();
    }

    @Override
    public Optional<IEditorPart> getEditor(ISourceViewer sourceViewer)
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
                        return Optional.of(editor);
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
        Preconditions.checkNotNull(widget);
        return widget != null && !widget.isDisposed() && widget.getEditable() && widget.isEnabled()
            && widget.getVisible() && getSourceViewer(widget).isPresent();
    }

    private SourceViewer getSourceViewer(Layout layout)
    {
        Preconditions.checkNotNull(layout);
        var fields = layout.getClass().getDeclaredFields();
        for (var field : fields)
        {
            if ("this$0".equals(field.getName())) //$NON-NLS-1$
            {
                field.setAccessible(true);
                try
                {
                    var outer = field.get(layout);
                    if (outer != null && outer instanceof SourceViewer)
                    {
                        return (SourceViewer)outer;
                    }
                }
                catch (Exception e)
                {
                    //
                }
            }
        }

        return null;
    }

    private Stream<Composite> getAncestors(Composite current)
    {
        Preconditions.checkNotNull(current);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new AncestorsIterator(current),
            Spliterator.IMMUTABLE & Spliterator.DISTINCT & Spliterator.NONNULL), false);
    }

    private Optional<IWorkbenchPage> getActivePage()
    {
        return Optional.ofNullable(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
    }

    private class AncestorsIterator
        implements Iterator<Composite>
    {
        private Composite current;

        public AncestorsIterator(Composite current)
        {
            Preconditions.checkNotNull(current);
            this.current = current;
        }

        @Override
        public boolean hasNext()
        {
            return current.getParent() != null;
        }

        @Override
        public Composite next()
        {
            current = current.getParent();
            return current;
        }
    }
}