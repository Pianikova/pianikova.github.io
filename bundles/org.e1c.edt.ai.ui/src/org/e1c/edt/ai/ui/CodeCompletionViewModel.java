/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IObserver;
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
    private boolean isCompleted = true;
    private boolean inProgress = false;

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

        isCompleted = true;
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
            if (context == null || context.isBlank())
            {
                return;
            }

            inProgress = true;
            dispatcher.dispatch(() -> hintPainter.pinOffset(aiContext.getCursorOffset()));
            var observer = new IObserver<String>()
            {
                @Override
                public boolean onNext(String value)
                {
                    dispatcher.dispatch(() -> {
                        if (!value.isEmpty())
                        {
                            var updatedHint = hintPainter.getHintText() + value;
                            hintPainter.setHintAt(aiContext.getCursorOffset(), updatedHint);
                        }
                    });

                    return true;
                }

                @Override
                public void onError(Throwable error)
                {
                    log.logError(error);
                    inProgress = false;
                    deactivate();
                }

                @Override
                public void onCompleted()
                {
                    dispatcher.dispatch(() -> {
                        var hint = hintPainter.getHintText();
                        if (hint.isEmpty())
                        {
                            hintPainter.setHintAt(aiContext.getCursorOffset(), System.lineSeparator());
                            inProgress = false;
                        }
                    });
                }
            };

            var response = codeAssistant.generateText(context, observer, cancellationToken);
            if (cancellationToken.isCanceled() || response.isEmpty())
            {
                dispatcher.dispatch(() -> hintPainter.reset());
                return;
            }
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

        if (e.keyCode == SWT.ARROW_RIGHT)
        {
            e.doit = false;
            dispatcher.dispatch(() -> {
                var hintText = hintPainter.getHintText();
                var token = tokenizer.getNext(hintText, this::isDelimiter);
                var tokenValue = token.getValue();
                var text = token.getText();
                apply(viewer.get(), tokenValue, offset, !(inProgress || text == null || !text.isEmpty()));
                aiContextProvider.create().ifPresent(ctx -> {
                    hintPainter.pinOffset(ctx.getCursorOffset());
                    hintPainter.setHintAt(ctx.getCursorOffset(), text);
                });
            });
        }

        if (e.character == '\t')
        {
            e.doit = false;
            dispatcher.dispatch(() -> apply(viewer.get(), hintPainter.getHintText(), offset, true));
        }
    }

    private void apply(ITextViewer viewer, String hintText, int offset, boolean isCompleted)
    {
        this.isCompleted = isCompleted;
        try
        {
            if (!hintText.isEmpty())
            {
                viewer.getDocument().replace(offset, 0, hintText);
                viewer.getSelectionProvider().setSelection(new TextSelection(offset + hintText.length(), 0));
            }
        }
        catch (BadLocationException e)
        {
            log.logError(e);
        }
        finally
        {
            this.isCompleted = true;
        }
    }

    @Override
    public void caretMoved(CaretEvent event)
    {
        if (!isCompleted)
        {
            return;
        }

        deactivate();
    }

    private Boolean isDelimiter(char ch)
    {
        return (ch == ' ') || (ch == '\t') || (ch == '\n') || (ch == '\r');
    }
}