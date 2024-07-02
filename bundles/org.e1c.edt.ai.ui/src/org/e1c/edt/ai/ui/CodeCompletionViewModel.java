/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.CodeCompletionType;
import org.e1c.edt.ai.HintPart;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.ICodeCompletionActionHandler;
import org.e1c.edt.ai.ICodeCompletionSession;
import org.e1c.edt.ai.IHintHistory;
import org.e1c.edt.ai.IInputDelayStatistics;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Observers;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CodeCompletionViewModel
    implements ICodeCompletionViewModel<CodeCompletionContext>, VerifyKeyListener, CaretListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final IUISettings uiSettings;
    private final IAICodeAssistant codeAssistant;
    private final IAIContextProvider<Void> aiContextProvider;
    private final IDispatcher dispatcher;
    private final IHintPainter hintPainter;
    private final IInputDelayStatistics inputRateStatistics;
    private final IClock clock;
    private final Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider;
    private final ICodeCompletionActionHandler<CodeCompletionContext> handler;
    private final IHintHistory history;
    private final Timer showTimer = new Timer(true);
    private ICodeCompletionSession<CodeCompletionContext> lastSession;
    private StyledText textWidget;
    private IUserActions userActions;

    @Inject
    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, IUISettings uiSettings,
        IAICodeAssistant codeAssistant,
        IAIContextProvider<Void> aiContextProvider,
        IDispatcher dispatcher, IHintPainter hintPainter, IInputDelayStatistics inputRateStatistics,
        IClock clock,
        Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider,
        ICodeCompletionActionHandler<CodeCompletionContext> handler, IHintHistory history, IUserActions userActions)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(codeAssistant);
        Preconditions.checkNotNull(aiContextProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(hintPainter);
        Preconditions.checkNotNull(inputRateStatistics);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(sessionProvider);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(history);
        Preconditions.checkNotNull(userActions);
        this.log = log;
        this.codeAssistant = codeAssistant;
        this.uiSettings = uiSettings;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.hintPainter = hintPainter;
        this.inputRateStatistics = inputRateStatistics;
        this.clock = clock;
        this.sessionProvider = sessionProvider;
        this.handler = handler;
        this.history = history;
        this.userActions = userActions;
    }

    @Override
    public AutoCloseable activate(StyledText textWidget)
    {
        this.textWidget = textWidget;
        reset();
        dispatcher.dispatch(() -> {
            textWidget.addPaintListener(hintPainter);
            textWidget.addCaretListener(this);
            textWidget.addVerifyKeyListener(this);
            textWidget.redraw();
        });

        return Closeables.create(() -> deactivate());
    }

    private void reset()
    {
        cancel();
        history.clear();
        dispatcher.dispatch(() -> {
            hintPainter.reset();
        });
    }

    private void update(CodeCompletionType codeCompletionType, ICodeCompletionSession<CodeCompletionContext> session)
    {
        var content = session.getContext();
        var widget = content.getWidget();
        var hint = session.getHint();
        var offset = widget.getCaretOffset();
        dispatcher.dispatch(() -> {
            hintPainter.pinOffset(widget, offset, true,
                codeCompletionType == CodeCompletionType.CodeSingleWord);
            hintPainter.setHintAt(offset, hint.getText(HintPart.LINES), hint.getText(HintPart.TOKEN));
        });
    }

    private void askNew(CodeCompletionType codeCompletionType)
    {
        var delayBeforeShow = inputRateStatistics.registerAndPredictDelay();
        log.trace("Predicted hint delay " + delayBeforeShow.toMillis() + " ms", ""); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
        reset();
        askWithDelay(codeCompletionType, delayBeforeShow);
    }

    private void askWithDelay(CodeCompletionType codeCompletionType, Duration delayBeforeShow)
    {
        cancel();
        new Job(Messages.CodeCompletionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                var cancellationTokenSource = new JobCancellationTokenSource();
                cancellationTokenSource.attachMonitor(monitor);
                ask(codeCompletionType, delayBeforeShow, cancellationTokenSource);
                return cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        }.schedule();
    }

    private void deactivate()
    {
        reset();
        dispatcher.dispatch(() -> {
            textWidget.removePaintListener(hintPainter);
            textWidget.removeCaretListener(this);
            textWidget.removeVerifyKeyListener(this);
            textWidget.redraw();
        });
    }

    private void ask(CodeCompletionType codeCompletionType, Duration delayBeforeShow,
        CancellationTokenSource cancellationTokenSource)
    {
        try
        {
            var startTime = clock.now();
            var aiCtx = dispatcher.dispatch(
                () -> aiContextProvider
                    .create(new AITarget(textWidget, 0, codeCompletionType), null, cancellationTokenSource)
                    .orElse(null));
            if (aiCtx.isEmpty())
            {
                return;
            }

            var aiContext = aiCtx.get();
            var complitionType = aiContext.getComplitionType();
            var codeCompletionCtx = new CodeCompletionContext(aiContext, textWidget, cancellationTokenSource);
            var session = sessionProvider.get()
                .initiaize(codeCompletionCtx, history, complitionType == CodeCompletionType.CodeSingleWord);
            synchronized (lockObject)
            {
                if (lastSession != null)
                {
                    lastSession.getContext().getCancellationTokenSource().cancel();
                    lastSession.reset();
                }

                lastSession = session;
            }

            log.trace("AI context " + cancellationTokenSource, aiContext.toString()); //$NON-NLS-1$
            var delay = calculateDelay(startTime, delayBeforeShow);
            if (cancellationTokenSource.isCanceled())
            {
                return;
            }

            dispatcher.dispatch(() -> {
                hintPainter.reset();
                hintPainter.pinOffset(textWidget, textWidget.getCaretOffset(),
                    delay.isNegative() || delay == Duration.ZERO,
                    complitionType == CodeCompletionType.CodeSingleWord);
            });

            var codeCompletionSource = codeAssistant.generate(aiContext, cancellationTokenSource);

            // @formatter:off
            codeCompletionSource.subscribe(Observers.create(
                value -> {
                    if (cancellationTokenSource.isCanceled())
                    {
                        return;
                    }

                    var hint = session.getHint();

                    // temporarily for comments
                    if (complitionType == CodeCompletionType.CodeComments)
                    {
                        value = value.replace("`", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    }

                    hint.append(value);
                    showWithDelay(session, calculateDelay(startTime, delayBeforeShow));
                },
                error -> {
                    if (cancellationTokenSource.isCanceled())
                    {
                        return;
                    }

                    log.logError(error);
                    reset();
                },
                () -> {
                    if (cancellationTokenSource.isCanceled())
                    {
                        return;
                    }

                    var hint = session.getHint();
                    log.trace("AI generated text " + cancellationTokenSource, format(hint.toString())); //$NON-NLS-1$
                    if (hint.isBlank())
                    {
                        hint.clear();
                    }

                    if (complitionType != CodeCompletionType.CodeSingleWord && hint.isEmpty())
                    {
                        hint.append("\n"); //$NON-NLS-1$
                    }

                    session.complete();
                    showWithDelay(session, calculateDelay(startTime, delayBeforeShow));
                }));
            // @formatter:on
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

    private void cancel()
    {
        showTimer.purge();
        synchronized (lockObject)
        {
            if (lastSession != null)
            {
                lastSession.getContext().getCancellationTokenSource().cancel();
                lastSession.reset();
            }
        }
    }

    private void showWithDelay(ICodeCompletionSession<CodeCompletionContext> session, Duration delayBeforeShow)
    {
        showTimer.purge();
        if (delayBeforeShow.isNegative() || delayBeforeShow == Duration.ZERO)
        {
            show(session);
            return;
        }

        // @formatter:off
        showTimer.schedule(
            new TimerTask() {
                @Override
                public void run()
                {
                    show(session);
                }
            },
            delayBeforeShow.toMillis());

        // @formatter:on
    }

    private void show(ICodeCompletionSession<CodeCompletionContext> session)
    {
        if (session.getContext().getCancellationTokenSource().isCanceled())
        {
            return;
        }

        var content = session.getContext();
        var widget = content.getWidget();
        var hint = session.getHint();
        dispatcher.dispatch(() -> {
            hintPainter.setHintAt(widget.getCaretOffset(), hint.getText(HintPart.LINES), hint.getText(HintPart.TOKEN));
            widget.redraw();
        });
    }

    private Duration calculateDelay(LocalDateTime startTime, Duration delayBeforeShow)
    {
        return delayBeforeShow.minus(Duration.between(startTime, clock.now()));
    }

    @SuppressWarnings("nls")
    private static String format(String text)
    {
        return "[" + text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "]";
    }

    @Override
    public void verifyKey(VerifyEvent event)
    {
        ICodeCompletionSession<CodeCompletionContext> session;
        synchronized (lockObject)
        {
            session = lastSession;
        }

        var action = userActions.getAction(event);
        var isContinuousCodeCompletion = uiSettings.isContinuousCodeCompletion();
        action = handler.handle(session, action, event.character, hintPainter.getOffset(), isContinuousCodeCompletion);
        var complitionType = session.getContext().getAiContext().getComplitionType();
        switch (action)
        {
        case SUGGEST:
            reset();
            askWithDelay(CodeCompletionType.CodeLines, Duration.ZERO);
            event.doit = false;
            break;

        case UPDATE:
            update(complitionType, session);
            if (session.isDone() && complitionType == CodeCompletionType.CodeLines)
            {
                askWithDelay(complitionType, Duration.ZERO);
            }

            event.doit = false;
            break;

        case ASK_NEW:
            askNew(CodeCompletionType.CodeLines);
            break;

        case RESET:
            reset();
            event.doit = false;
            break;

        case HANDLE:
            if (complitionType == CodeCompletionType.CodeLines)
            {
                event.doit = false;
            }

            break;

        default:
            break;
        }
    }

    @Override
    public void caretMoved(CaretEvent event)
    {
        synchronized (lockObject)
        {
            if (lastSession == null || lastSession.isAccepting())
            {
                return;
            }
        }

        reset();
    }
}