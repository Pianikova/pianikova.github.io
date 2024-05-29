/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeCompletionViewModel
    implements ICodeCompletionViewModel, VerifyKeyListener, CaretListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final IAICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final IHotKeys hotKeys;
    private final ICodeCompletionTokenizer tokenizer;
    private final IHintPainter hintPainter;
    private final IUISettings uiSettings;
    private final Stack<String> tokens = new Stack<>();
    private CancellationToken askCancellationToken = CancellationToken.NONE;
    private StringBuilder hint = new StringBuilder();
    private boolean inProgress;
    private CompletableFuture<Void> currentResponse = CompletableFuture.completedFuture(null);

    @Inject
    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, IAICodeAssistant codeAssistant,
        IAIContextProvider aiContextProvider,
        IDispatcher dispatcher, IUI ui, IHotKeys hotKeys,
        ICodeCompletionTokenizer tokenizer,
        IHintPainter hintPainter, IUISettings uiSettings)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(codeAssistant);
        Preconditions.checkNotNull(aiContextProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(hotKeys);
        Preconditions.checkNotNull(tokenizer);
        Preconditions.checkNotNull(tokenizer);
        Preconditions.checkNotNull(uiSettings);
        this.log = log;
        this.codeAssistant = codeAssistant;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.ui = ui;
        this.hotKeys = hotKeys;
        this.tokenizer = tokenizer;
        this.hintPainter = hintPainter;
        this.uiSettings = uiSettings;
    }

    @Override
    public AutoCloseable activate(boolean askImmediately)
    {
        reset();
        dispatcher.dispatch(() -> {
            ui.getTextWidget().ifPresent(textWidget -> {
                textWidget.addPaintListener(hintPainter);
                textWidget.addCaretListener(this);
                textWidget.addVerifyKeyListener(this);
                textWidget.redraw();
            });
        });

        if (askImmediately)
        {
            askByJob(0);
        }

        return Closeables.create(() -> deactivate());
    }

    private void deactivate()
    {
        reset();
        dispatcher.dispatch(() -> {
            ui.getTextWidget().ifPresent(textWidget -> {
                textWidget.removePaintListener(hintPainter);
                textWidget.removeCaretListener(this);
                textWidget.removeVerifyKeyListener(this);
                textWidget.redraw();
            });
        });
    }

    private void reset()
    {
        synchronized (lockObject)
        {
            if (!cancel())
            {
                return;
            }
        }

        dispatcher.dispatch(() -> {
            tokens.clear();
            inProgress = false;
            hintPainter.reset();
        });
    }

    private boolean cancel()
    {
        synchronized (lockObject)
        {
            if (askCancellationToken.isCanceled())
            {
                return false;
            }

            askCancellationToken.cancel();
            return true;
        }
    }

    private void askByJob(long delay)
    {
        cancel();
        var model = this;
        new Job(Messages.CodeCompletionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                var cancellationToken = new JobCancellationToken();
                StringBuilder curHint;
                synchronized (lockObject)
                {
                    model.cancel();
                    askCancellationToken = cancellationToken;
                    hint = new StringBuilder();
                    curHint = hint;
                }

                cancellationToken.attachMonitor(monitor);
                ask(curHint, cancellationToken);
                return cancellationToken.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        }.schedule(delay);
    }

    private void ask(StringBuilder curHint, CancellationToken cancellationToken)
    {
        try
        {
            var ctx = dispatcher.dispatch(() -> {
                return aiContextProvider.create().orElse(null);
            });

            if (cancellationToken.isCanceled() || ctx.isEmpty())
            {
                return;
            }

            var aiContext = ctx.get();
            var context = aiContext.getContext();
            log.trace("AI context " + cancellationToken, aiContext.toString()); //$NON-NLS-1$
            var observer = new IObserver<String>()
            {
                @Override
                public boolean onNext(String value)
                {
                    dispatcher.dispatch(() -> {
                        if (cancellationToken.isCanceled())
                        {
                            return;
                        }

                        curHint.append(value);
                        hintPainter.setHintAt(aiContext.getCursorOffset(), getHintLines(curHint));
                        ui.getTextWidget().ifPresent(textWidget -> textWidget.redraw());
                    });

                    return true;
                }

                @Override
                public void onError(Throwable error)
                {
                    if (cancellationToken.isCanceled())
                    {
                        return;
                    }

                    log.logError(error);
                    reset();
                }

                @Override
                public void onCompleted()
                {
                    dispatcher.dispatch(() -> {
                        if (cancellationToken.isCanceled())
                        {
                            return;
                        }

                        log.trace("AI generated text " + cancellationToken, curHint.toString()); //$NON-NLS-1$
                        hintPainter.setHintAt(aiContext.getCursorOffset(), getHintLines(curHint));
                        ui.getTextWidget().ifPresent(textWidget -> textWidget.redraw());
                    });
                }
            };

            dispatcher.dispatch(() -> {
                if (cancellationToken.isCanceled())
                {
                    return;
                }

                hintPainter.reset();
                ui.getTextWidget()
                    .ifPresent(textWidget -> hintPainter.pinOffset(textWidget, aiContext.getCursorOffset()));
            });

            var response = codeAssistant.generateText(context, observer, cancellationToken);
            if (cancellationToken.isCanceled() || response.isEmpty())
            {
                return;
            }

            synchronized (lockObject)
            {
                currentResponse = response.get();
            }
        }
        catch (CancellationException e)
        {
            //  ignored
        }
        catch (Exception e)
        {
            log.logError(e);
            deactivate();
        }
    }

    @Override
    public void verifyKey(VerifyEvent e)
    {
        StringBuilder curHint;
        synchronized (lockObject)
        {
            curHint = hint;
        }

        if (hotKeys.isTriggered(IHotKeys.SUGGEST, e))
        {
            e.doit = false;
            reset();
            askByJob(0);
            return;
        }

        var offset = hintPainter.getOffset();
        if (offset >= 0)
        {
            if (hotKeys.isTriggered(IHotKeys.STOP, e))
            {
                e.doit = false;
                reset();
                return;
            }

            if (hotKeys.isTriggered(IHotKeys.ACCEPT_PART, e))
            {
                e.doit = false;
                dispatcher.dispatch(() -> {
                    var hintText = hintPainter.getHintText();
                    var token = tokenizer.getNext(1, hintText, this::isTextDelimiter);
                    var text = token.getValue();
                    apply(text, offset);
                    tokens.push(text);
                    curHint.delete(0, text.length());
                    var hintLines = getHintLines(curHint);
                    continueAsk(hintLines);
                    if (curHint.length() == 0 && currentResponse.isDone())
                    {
                        askByJob(0);
                    }
                });

                return;
            }

            if (hotKeys.isTriggered(IHotKeys.ROLLBACK_PART, e))
            {
                e.doit = false;
                dispatcher.dispatch(() -> {
                    if (tokens.size() > 0)
                    {
                        var text = tokens.pop();
                        rollback(text, offset);
                        curHint.insert(0, text);
                        var hintLines = getHintLines(curHint);
                        continueAsk(hintLines);
                    }
                });

                return;
            }

            if (hotKeys.isTriggered(IHotKeys.ACCEPT, e))
            {
                e.doit = false;
                dispatcher.dispatch(() -> {
                    var hintText = hintPainter.getHintText();
                    apply(hintText, offset);
                    tokens.push(hintText);
                    curHint.delete(0, hintText.length());
                    var hintLines = getHintLines(curHint);
                    continueAsk(hintLines);
                    if (curHint.length() == 0 && currentResponse.isDone())
                    {
                        askByJob(0);
                    }
                });

                return;
            }
        }

        if (curHint.length() > 0 && curHint.charAt(0) == e.character)
        {
            e.doit = false;
            var chars = new char[1];
            chars[0] = e.character;
            var text = new String(chars);
            apply(text, offset);
            tokens.push(text);
            curHint.delete(0, 1);
            var hintLines = getHintLines(curHint);
            continueAsk(hintLines);
            if (curHint.length() == 0 && currentResponse.isDone())
            {
                askByJob(0);
            }

            return;
        }

        if (!uiSettings.isContinuousCodeCompletion())
        {
            reset();
            return;
        }

        var charType = Character.getType(e.character);
        if (e.character == '\r' || e.character == '\n'
            || charType != Character.SPACE_SEPARATOR && charType != Character.CONTROL)
        {
            reset();
            askByJob(500);
        }
    }

    private void apply(String hintText, int offset)
    {
        if (!hintText.isEmpty())
        {
            try
            {
                inProgress = true;
                if (hintText.length() > 0)
                {
                    ui.getTextWidget().ifPresent(textWidget -> {
                        textWidget.replaceTextRange(offset, 0, hintText);
                        textWidget.setCaretOffset(offset + hintText.length());
                    });
                }
            }
            finally
            {
                inProgress = false;
            }
        }
    }

    private void rollback(String hintText, int offset)
    {
        if (!hintText.isEmpty())
        {
            try
            {
                inProgress = true;
                var start = offset - hintText.length();
                ui.getTextWidget().ifPresent(textWidget -> {
                    textWidget.replaceTextRange(start, hintText.length(), ""); //$NON-NLS-1$
                    textWidget.setCaretOffset(start);
                });
            }
            finally
            {
                inProgress = false;
            }
        }
    }

    private void continueAsk(String hint)
    {
        aiContextProvider.create().ifPresent(ctx -> {
            ui.getTextWidget().ifPresent(textWidget -> hintPainter.pinOffset(textWidget, ctx.getCursorOffset()));
            hintPainter.setHintAt(ctx.getCursorOffset(), hint);
            if (hint.isEmpty())
            {
                askByJob(0);
            }
        });
    }

    @Override
    public void caretMoved(CaretEvent event)
    {
        if (!inProgress)
        {
            reset();
        }
    }

    private String getHintLines(StringBuilder hint)
    {
        var maxLines = uiSettings.getCodeCompletionLinesCount();
        var lines = new StringBuilder();
        var text = hint.toString();
        while (maxLines-- > 0 && !text.isEmpty())
        {
            var token = tokenizer.getNext(2, text, this::isLineDelimiter);
            var value = token.getValue();
            lines.append(value);
            text = token.getText();
        }

        if (lines.length() == 0 || lines.charAt(lines.length() - 1) == '\n')
        {
            lines.append(System.lineSeparator());
        }

        return lines.toString();
    }

    private Boolean isTextDelimiter(char ch)
    {
        return (ch == ' ') || (ch == '\t') || isLineDelimiter(ch);
    }

    private Boolean isLineDelimiter(char ch)
    {
        return (ch == '\n') || (ch == '\r');
    }
}