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
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class UI
    implements IUI, Listener
{
    private Object lock = new Object();
    private final ILog log;
    private final Provider<ICodeCompletionViewModel<CodeCompletionContext>> codeCompletionViewModelProvider;
    private final Provider<IFeedbackViewModel> feedbackViewModelProvider;
    private StyledText textWidget;
    private AutoCloseable queryToken = Closeables.Empty;

    @Inject
    public UI(ILog log, Provider<ICodeCompletionViewModel<CodeCompletionContext>> codeCompletionViewModelProvider,
        Provider<IFeedbackViewModel> feedbackViewModelProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(codeCompletionViewModelProvider);
        Preconditions.checkNotNull(feedbackViewModelProvider);
        this.log = log;
        this.codeCompletionViewModelProvider = codeCompletionViewModelProvider;
        this.feedbackViewModelProvider = feedbackViewModelProvider;
    }

    @Override
    public void handleEvent(Event event)
    {
        Preconditions.checkNotNull(event);
        if (event.type == SWT.FocusOut)
        {
            synchronized (lock)
            {
                try
                {
                    queryToken.close();
                }
                catch (Exception e)
                {
                    // ignored
                }
            }
        }

        if (event.type == SWT.FocusIn && event.widget instanceof StyledText)
        {
            synchronized (lock)
            {
                try
                {
                    queryToken.close();
                }
                catch (Exception e)
                {
                    // ignored
                }

                var newTextWidget = (StyledText)event.widget;
                if (isValidWidget(newTextWidget))
                {
                    textWidget = newTextWidget;
                    queryToken = Closeables.create(codeCompletionViewModelProvider.get().activate(newTextWidget),
                        feedbackViewModelProvider.get().activate(newTextWidget));
                }
            }
        }
    }

    @Override
    public Optional<StyledText> getTextWidget()
    {
        synchronized (lock)
        {
            var widget = textWidget;
            if (!isValidWidget(widget))
            {
                return Optional.empty();
            }

            return Optional.of(widget);
        }
    }

    @Override
    public Optional<SourceViewer> getSourceViewer(StyledText textWidget)
    {
        Preconditions.checkNotNull(textWidget);
        return getAncestors(textWidget).filter(i -> i instanceof Canvas)
            .map(i -> getSourceViewer(((Canvas)i).getLayout()))
            .filter(i -> i != null)
            .findFirst();
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