/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.assistent.CancellationToken;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

public class CodeCompletionViewModel
    implements ICodeCompletionViewModel, VerifyKeyListener
{
    private final Object lockObject = new Object();
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final ILog log;
    private final IAICodeAssistant codeAssistant;
    private final IAIContext aiContext;
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final ICodeCompletionTokenizer tokenizer;
    private final IHintPainter hintPainter;
    private CancellationToken askCancellationToken = new CancellationToken();

    public CodeCompletionViewModel(ILog log, IAICodeAssistant codeAssistant, IAIContext aiContext,
        IDispatcher dispatcher, IUI ui,
        ICodeCompletionTokenizer tokenizer,
        IHintPainter hintPainter)
    {
        this.log = log;
        this.codeAssistant = codeAssistant;
        this.aiContext = aiContext;
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
            ui.getTextViewer().ifPresent(viewer -> viewer.getTextWidget().addVerifyKeyListener(this));
        });

        threadPool.execute(() -> ask(cancellationToken));
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
            ui.getTextViewer().ifPresent(viewer -> viewer.getTextWidget().removeVerifyKeyListener(this));
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

            var ctx = dispatcher.dispatch(() -> aiContext.create().orElse(null));
            if (cancellationToken.isCanceled() || ctx.isEmpty())
            {
                dispatcher.dispatch(() -> hintPainter.reset());
                return;
            }

            var aiContext = ctx.get();
            var prefix = aiContext.getPrefix();
            if (prefix.isBlank())
            {
                return;
            }

            var cursorOffset = aiContext.getCursorOffset();
            dispatcher.dispatch(() -> hintPainter.pinOffset(cursorOffset));

            var response = codeAssistant.generateText(prefix, cancellationToken);
            if (cancellationToken.isCanceled() || response.isEmpty())
            {
                dispatcher.dispatch(() -> hintPainter.reset());
                return;
            }

            var hintText = response.get().getGeneratedText().trim();
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
                    aiContext.create().ifPresent(ctx -> {
                        var cursorOffset = ctx.getCursorOffset();
                        hintPainter.pinOffset(cursorOffset);
                        hintPainter.setHintAt(cursorOffset, token.getText());
                    });
                });

                return;
            }
        }

        if (applyHint || e.character == '\t')
        {
            e.doit = false;
            dispatcher.dispatch(() -> apply(viewer.get(), hintPainter.getHintText(), offset));
        }

        deactivate();
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
}