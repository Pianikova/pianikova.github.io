/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.IInputDelayStatistics;
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
    private final IAIContextProvider<Void> aiContextProvider;
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final IHotKeys hotKeys;
    private final ICodeCompletionTokenizer tokenizer;
    private final IHintPainter hintPainter;
    private final IUISettings uiSettings;
    private final IInputDelayStatistics inputRateStatistics;
    private final IClock clock;
    private final Stack<String> tokens = new Stack<>();
    private final Timer showTimer = new Timer(true);
    private CancellationToken askCancellationToken = CancellationToken.NONE;
    private StringBuilder hint = new StringBuilder();
    private boolean inProgress;
    private CompletableFuture<Void> currentResponse = CompletableFuture.completedFuture(null);

    @Inject
    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, IAICodeAssistant codeAssistant,
        IAIContextProvider<Void> aiContextProvider,
        IDispatcher dispatcher, IUI ui, IHotKeys hotKeys,
        ICodeCompletionTokenizer tokenizer,
        IHintPainter hintPainter, IUISettings uiSettings, IInputDelayStatistics inputRateStatistics, IClock clock)
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
        Preconditions.checkNotNull(inputRateStatistics);
        Preconditions.checkNotNull(clock);
        this.log = log;
        this.codeAssistant = codeAssistant;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.ui = ui;
        this.hotKeys = hotKeys;
        this.tokenizer = tokenizer;
        this.hintPainter = hintPainter;
        this.uiSettings = uiSettings;
        this.inputRateStatistics = inputRateStatistics;
        this.clock = clock;
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
            askByJob(Duration.ZERO);
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
        cancel();
        dispatcher.dispatch(() -> {
            tokens.clear();
            inProgress = false;
            hintPainter.reset();
        });
    }

    private void cancel()
    {
        showTimer.purge();
        synchronized (lockObject)
        {
            if (askCancellationToken.isCanceled())
            {
                return;
            }

            askCancellationToken.cancel();
        }
    }

    private void askByJob(Duration delayBeforeShow)
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
                ask(curHint, delayBeforeShow, cancellationToken);
                return cancellationToken.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        }.schedule();
    }

    private void show(AIContext aiContext, StringBuilder curHint, Duration delayBeforeShow,
        CancellationToken cancellationToken)
    {
        showTimer.purge();
        if (delayBeforeShow.isNegative() || delayBeforeShow == Duration.ZERO)
        {
            show(aiContext, curHint, cancellationToken);
            return;
        }

        showTimer.schedule(new TimerTask()
            {
            @Override
            public void run()
                {
                show(aiContext, curHint, cancellationToken);
                }
        }, delayBeforeShow.toMillis());
    }

    private void show(AIContext aiContext, StringBuilder curHint, CancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            return;
        }

        dispatcher.dispatch(() -> {
            ui.getTextWidget().ifPresent(textWidget -> {
                hintPainter.setHintAt(textWidget.getCaretOffset(), getHintLines(curHint));
                textWidget.redraw();
            });
        });
    }

    private void ask(StringBuilder curHint, Duration delayBeforeShow, CancellationToken cancellationToken)
    {
        try
        {
            var startTime = clock.now();
            var ctx = dispatcher.dispatch(() -> aiContextProvider.create(null, cancellationToken).orElse(null));
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
                        show(aiContext, curHint, delayBeforeShow.minus(Duration.between(startTime, clock.now())),
                            cancellationToken);
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
                        show(aiContext, curHint, delayBeforeShow.minus(Duration.between(startTime, clock.now())), cancellationToken);
                    });
                }
            };

            dispatcher.dispatch(() -> {
                if (cancellationToken.isCanceled())
                {
                    return;
                }

                hintPainter.reset();
                var delay = delayBeforeShow.minus(Duration.between(startTime, clock.now()));
                ui.getTextWidget()
                    .ifPresent(textWidget -> hintPainter.pinOffset(textWidget, textWidget.getCaretOffset(),
                        delay.isNegative() || delay == Duration.ZERO));
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
            askByJob(Duration.ZERO);
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
                        askByJob(Duration.ZERO);
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
                        askByJob(Duration.ZERO);
                    }
                });

                return;
            }
        }

        if (curHint.length() > 0 && curHint.charAt(0) == e.character)
        {
            inputRateStatistics.registerAndPredictDelay();
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
                askByJob(Duration.ZERO);
            }

            return;
        }

        var charType = Character.getType(e.character);
        if (charType != Character.CONTROL && !uiSettings.isContinuousCodeCompletion())
        {
            reset();
            return;
        }

        if (uiSettings.isContinuousCodeCompletion() && e.character == '\r' || e.character == '\n'
            || charType != Character.SPACE_SEPARATOR && charType != Character.CONTROL)
        {
            var delayBeforeShow = inputRateStatistics.registerAndPredictDelay();
            log.trace("Predicted hint delay " + delayBeforeShow.toMillis() + " ms", ""); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            reset();
            inProgress = true;
            askByJob(delayBeforeShow);
            return;
        }

        inProgress = false;
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
                        var start = offset;
                        if (offset < 0)
                        {
                            start = 0;
                        }

                        var contentLength = textWidget.getCharCount();
                        if (start > contentLength)
                        {
                            start = contentLength;
                        }

                        textWidget.replaceTextRange(start, 0, hintText);
                        textWidget.setCaretOffset(start + hintText.length());
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
                ui.getTextWidget().ifPresent(textWidget -> {
                    var start = offset - hintText.length();
                    if (offset < 0)
                    {
                        start = 0;
                    }

                    var contentLength = textWidget.getCharCount();
                    if (start > contentLength)
                    {
                        start = contentLength;
                    }

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
        CancellationToken cancellationToken;
        synchronized (lockObject)
        {
            cancellationToken = askCancellationToken;
        }

        ui.getTextWidget().ifPresent(textWidget -> {
            var offset = textWidget.getCaretOffset();
            hintPainter.pinOffset(textWidget, offset, true);
            hintPainter.setHintAt(offset, hint);
        });

        if (hint.isEmpty())
        {
            askByJob(Duration.ZERO);
        }
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