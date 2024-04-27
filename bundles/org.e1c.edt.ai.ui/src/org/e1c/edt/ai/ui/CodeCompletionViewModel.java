/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.assistent.CancellationToken;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

public class CodeCompletionViewModel
    implements ICodeCompletionViewModel, VerifyKeyListener, CaretListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final IAICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final ICodeCompletionTokenizer tokenizer;
    private final IHintPainter hintPainter;
    private CancellationToken askCancellationToken = new CancellationToken();

    public CodeCompletionViewModel(ILog log, IAICodeAssistant codeAssistant, IAIContextProvider aiContextProvider,
        IDispatcher dispatcher, IUI ui,
        ICodeCompletionTokenizer tokenizer,
        IHintPainter hintPainter)
    {
        this.log = log;
        this.codeAssistant = codeAssistant;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.ui = ui;
        this.tokenizer = tokenizer;
        this.hintPainter = hintPainter;
    }

    @Override
    public void activate()
    {
        CancellationToken cancellationToken = new CancellationToken();
        synchronized (lockObject)
        {
            askCancellationToken.cancel();
            askCancellationToken = cancellationToken;
        }

        dispatcher.dispatch(() -> {
            ui.getTextViewerExtension2().ifPresent(viewer -> viewer.addPainter(hintPainter));
            ui.getTextViewer().ifPresent(viewer -> {
                var textWidget = viewer.getTextWidget();
                textWidget.addCaretListener(this);
                textWidget.addVerifyKeyListener(this);
            });
        });

        new Job(Messages.CodeCompletionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                var cancellationToken = new JobCancellationToken(monitor);
                askCancellationToken.cancel();
                askCancellationToken = cancellationToken;
                ask(cancellationToken);
                return cancellationToken.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    public void deactivate()
    {
        CancellationToken cancellationToken = new CancellationToken();
        synchronized (lockObject)
        {
            askCancellationToken.cancel();
            askCancellationToken = cancellationToken;
        }

        dispatcher.dispatch(() -> {
            ui.getTextViewerExtension2().ifPresent(viewer -> viewer.removePainter(hintPainter));
            ui.getTextViewer().ifPresent(viewer -> {
                var textWidget = viewer.getTextWidget();
                textWidget.removeCaretListener(this);
                textWidget.removeVerifyKeyListener(this);
            });
        });
    }

    private void ask(CancellationToken cancellationToken)
    {
        try
        {
            if (cancellationToken.isCanceled())
            {
                return;
            }

            var ctx = dispatcher.dispatch(() -> aiContextProvider.create().orElse(null));
            if (cancellationToken.isCanceled() || ctx.isEmpty())
            {
                dispatcher.dispatch(() -> hintPainter.reset());
                return;
            }

            var aiContext = ctx.get();
            var context = aiContext.getContext();
            if (context.isBlank())
            {
                return;
            }

            var cursorOffset = aiContext.getCursorOffset();
            dispatcher.dispatch(() -> hintPainter.pinOffset(cursorOffset));

            var response = codeAssistant.generateText(context, cancellationToken);
            if (cancellationToken.isCanceled() || response.isEmpty())
            {
                dispatcher.dispatch(() -> hintPainter.reset());
                return;
            }

            var hintText = response.get().getGeneratedText();
            dispatcher.dispatch(() -> hintPainter.setHintAt(cursorOffset, hintText));
        }
        catch (CancellationException e)
        {
            //  ignored
        }
        catch (Exception e)
        {
            Activator.logError(e);
            deactivate();
        }
    }

    @Override
    public void verifyKey(VerifyEvent e)
    {
        var offset = hintPainter.getOffset();
        if (offset < 0)
        {
            return;
        }

        var viewer = ui.getTextViewer();
        if (viewer.isEmpty())
        {
            return;
        }

        var hintText = hintPainter.getHintText();
        var applyHint = false;
        if (e.keyCode == SWT.ARROW_RIGHT)
        {
            e.doit = false;
            applyHint = true;
            var token = tokenizer.getNext(hintText);
            var tokenValue = token.getValue();
            if (!tokenValue.isEmpty())
            {
                dispatcher.dispatch(() -> {
                    apply(viewer.get(), tokenValue, offset);
                    aiContextProvider.create().ifPresent(ctx -> {
                        var cursorOffset = ctx.getCursorOffset();
                        hintPainter.pinOffset(cursorOffset);
                        hintPainter.setHintAt(cursorOffset, token.getText());
                    });
                });

                activate();
                return;
            }
        }

        if (applyHint || e.character == '\t')
        {
            e.doit = false;
            dispatcher.dispatch(() -> apply(viewer.get(), hintPainter.getHintText(), offset));
        }
    }

    private void apply(ITextViewer viewer, String hintText, int offset)
    {
        if (hintText.isEmpty())
        {
            return;
        }

        try
        {
            viewer.getDocument().replace(offset, 0, hintText);
            viewer.getSelectionProvider().setSelection(new TextSelection(offset + hintText.length(), 0));
        }
        catch (BadLocationException e)
        {
            log.logError(e);
        }
    }

    @Override
    public void caretMoved(CaretEvent event)
    {
        deactivate();
    }
}