/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.HintPart;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.ICodeCompletionActionHandler;
import org.e1c.edt.ai.ICodeCompletionContext;
import org.e1c.edt.ai.ICodeCompletionSession;
import org.e1c.edt.ai.IContextEntities;
import org.e1c.edt.ai.IHintHistory;
import org.e1c.edt.ai.IInputDelayStatistics;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Observers;
import org.e1c.edt.ai.Text;
import org.e1c.edt.ai.assistent.ICodeAssistant;
import org.e1c.edt.ai.assistent.model.LocalContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CodeCompletionViewModel
    implements ICodeCompletionViewModel<CodeCompletionContext>, VerifyKeyListener, CaretListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final IUISettings uiSettings;
    private final ICodeAssistant codeAssistant;
    private final IAIContextProvider<Void> aiContextProvider;
    private final IDispatcher dispatcher;
    private final IHintPainter hintPainter;
    private final IInputDelayStatistics inputRateStatistics;
    private final IClock clock;
    private final Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider;
    private final ICodeCompletionActionHandler<CodeCompletionContext> handler;
    private final IHintHistory history;
    private final IUserActions userActions;
    private final ICodeCompletionContext codeCompletionContext;
    private final Timer showTimer = new Timer(true);
    private final IUI ui;
    private final ICodeProvider codeProvider;
    private final IContextEntities contextEntities;
    private ICodeCompletionSession<CodeCompletionContext> lastSession;
    private StyledText textWidget;
    private AutoCloseable feedbackToken = Closeables.Empty;
    private Job lastJob;
    private boolean isProposalMenuOpened = false;
    private Duration requestDuration = Duration.ZERO;

    @Inject
    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, IUISettings uiSettings,
        ICodeAssistant codeAssistant,
        IAIContextProvider<Void> aiContextProvider,
        IDispatcher dispatcher, IHintPainter hintPainter, IInputDelayStatistics inputRateStatistics,
        IClock clock,
        Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider,
        ICodeCompletionActionHandler<CodeCompletionContext> handler, IHintHistory history, IUserActions userActions,
        ICodeCompletionContext codeCompletionContext, IUI ui, ICodeProvider codeProvider,
        IContextEntities contextEntities)
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
        Preconditions.checkNotNull(codeCompletionContext);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(codeProvider);
        Preconditions.checkNotNull(contextEntities);
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
        this.codeCompletionContext = codeCompletionContext;
        this.ui = ui;
        this.codeProvider = codeProvider;
        this.contextEntities = contextEntities;
    }

    @Override
    public AutoCloseable activate(StyledText textWidget)
    {
        this.textWidget = textWidget;
        getContentAssistant().ifPresent(assistant -> addCompletionListener(assistant));
        this.isProposalMenuOpened = false;
        reset();
        dispatcher.dispatch(() -> {
            textWidget.addPaintListener(hintPainter);
            textWidget.addCaretListener(this);
            textWidget.addVerifyKeyListener(this);
            textWidget.redraw();
            warmUp();
        });

        return Closeables.create(() -> deactivate());
    }

    private void warmUp()
    {
        cancel();
        var cancellationTokenSource = new JobCancellationTokenSource();
        dispatcher.dispatchAsync(
            () -> aiContextProvider.create(new AITarget(textWidget, 0, false), null, cancellationTokenSource)
                .ifPresent(aiCtx -> {
                    var job = new Job(Messages.CodeCompletionJobName)
                    {
                        @Override
                        protected IStatus run(IProgressMonitor monitor)
                        {
                            cancellationTokenSource.attachMonitor(monitor);
                            contextEntities.fill(aiCtx, new LocalContext(), IStatistics.Empty, cancellationTokenSource);
                            return cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
                        }
                    };

                    this.lastJob = job;
                    job.schedule(0);
                }));
    }

    private void reset()
    {
        cancel();
        history.clear();
        dispatcher.dispatch(() -> {
            hintPainter.reset();
        });
    }

    private void update(ICodeCompletionSession<CodeCompletionContext> session)
    {
        var content = session.getContext();
        var widget = content.getWidget();
        var hint = session.getHint();
        var offset = widget.getCaretOffset();
        dispatcher.dispatch(() -> {
            hintPainter.pinOffset(widget, offset, true, session.getContext().isSingleWordMode());
            hintPainter.setHintAt(offset, hint.getText(HintPart.LINES).getText(),
                hint.getText(HintPart.TOKEN).getText());
        });
    }

    private void askNew()
    {
        var delayBeforeShow = inputRateStatistics.registerAndPredictDelay();
        var delay = delayBeforeShow.minus(requestDuration);
        if (delay.toNanos() < uiSettings.getMinRequestDelay().toNanos())
        {
            delay = uiSettings.getMinRequestDelay();
        }

        log.trace(
            "Predicted hint delay " + delayBeforeShow.toMillis() + " ms, actual delay " + delay.toMillis() + " ms", ""); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        reset();
        askWithDelay(delay, Duration.ofMillis(150));
    }

    private void askWithDelay(Duration delayBeforeAsk, Duration delayBeforeShow)
    {
        cancel();
        var cancellationTokenSource = new JobCancellationTokenSource();
        dispatcher
            .dispatchAsync(
                () -> aiContextProvider.create(new AITarget(textWidget, 0, false), null, cancellationTokenSource)
                .ifPresent(aiCtx -> {
                    var job = new Job(Messages.CodeCompletionJobName)
                    {
                        @Override
                        protected IStatus run(IProgressMonitor monitor)
                        {
                            cancellationTokenSource.attachMonitor(monitor);
                            ask(aiCtx, delayBeforeShow, cancellationTokenSource);
                            return cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
                        }
                    };

                    this.lastJob = job;
                    job.schedule(delayBeforeAsk.toMillis());
                }));
    }

    private void deactivate()
    {
        codeCompletionContext.commit("", -1); //$NON-NLS-1$
        try
        {
            feedbackToken.close();
        }
        catch (Exception e)
        {
            // ignored
        }

        reset();
        dispatcher.dispatch(() -> {
            textWidget.removePaintListener(hintPainter);
            textWidget.removeCaretListener(this);
            textWidget.removeVerifyKeyListener(this);
            if (!textWidget.isDisposed())
            {
                textWidget.redraw();
            }
        });
    }

    private void ask(AIContext aiCtx, Duration delayBeforeShow, CancellationTokenSource cancellationTokenSource)
    {
        try
        {
            var startTime = clock.now();
            var codeCompletionCtx =
                new CodeCompletionContext(codeCompletionContext, aiCtx, textWidget, cancellationTokenSource);
            var singleWordMode = dispatcher.dispatch(() -> codeCompletionCtx.isSingleWordMode()).orElse(false);
            var session = sessionProvider.get().initiaize(codeCompletionCtx, history, singleWordMode);
            synchronized (lockObject)
            {
                if (lastSession != null)
                {
                    lastSession.getContext().getCancellationTokenSource().cancel();
                    lastSession.reset();
                }

                lastSession = session;
            }

            log.trace("AI context " + cancellationTokenSource, aiCtx.toString()); //$NON-NLS-1$
            var delay = calculateDelay(startTime, delayBeforeShow);
            if (cancellationTokenSource.isCanceled())
            {
                return;
            }

            dispatcher
                .dispatch(() -> ui.getTextWidget().flatMap(textWidget -> {
                    hintPainter.reset();
                    hintPainter.pinOffset(textWidget, aiCtx.getСaretOffset(),
                        delay.isNegative() || delay == Duration.ZERO, singleWordMode);
                    return ui.getSourceViewer(textWidget);
                }).orElse(null))
                .flatMap(sourceViewer -> codeProvider.getParseResult(sourceViewer))
                .flatMap(parseResult -> codeProvider.getMethod(parseResult, aiCtx.getTextOffset()))
                .ifPresent(method -> session.setMethod(method));

            var completionSource = codeAssistant.createSource(aiCtx, cancellationTokenSource);
            requestDuration = Duration.between(startTime, clock.now());

            // @formatter:off
            completionSource.subscribe(Observers.create(
                data -> {
                    if (cancellationTokenSource.isCanceled())
                    {
                        return;
                    }

                    var uuid = data.uuid;
                    if (uuid != null && !uuid.isBlank())
                    {
                        session.setId(uuid);
                        cancellationTokenSource.setName(uuid);
                    }

                    var hint = session.getHint();
                    hint.append(new Text(data.text, session));
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
                        reset();
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
            var lastJob = this.lastJob;
            if (lastJob != null)
            {
                lastJob.cancel();
                lastJob = null;
            }

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
            if (isProposalMenuOpened)
            {
                // close proposal menu before rising UI-hint
                getContentAssistant().ifPresent(assistant -> assistant.requestWidgetToken(null, 40));
            }
            hintPainter.setHintAt(session.getContext().getAiContext().getСaretOffset(),
                hint.getText(HintPart.LINES).getText(),
                hint.getText(HintPart.TOKEN).getText());
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
        switch (action)
        {
        case SUGGEST:
            reset();
            askWithDelay(Duration.ZERO, Duration.ZERO);
            event.doit = false;
            break;

        case UPDATE:
            if (session != null)
            {
                update(session);
                if (session.isDone() && !session.getContext().isSingleWordMode())
                {
                    askWithDelay(Duration.ZERO, Duration.ZERO);
                }

                event.doit = false;
            }
            break;

        case ASK_NEW:
            commit(session);
            askNew();
            break;

        case RESET:
            commit(session);
            reset();
            event.doit = false;
            break;

        case HANDLE:
            event.doit = false;
            break;

        case SKIP:
            commit(session);
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

    private void commit(ICodeCompletionSession<CodeCompletionContext> session)
    {
        if (session == null)
        {
            return;
        }

        session.getContext().commit(session.getId(), session.getContext().getAiContext().getTextOffset());
    }

    private Optional<ContentAssistant> getContentAssistant()
    {
        var viewer = ui.getSourceViewer(textWidget).get();
        if (viewer instanceof XtextSourceViewer)
        {
            IContentAssistant assistant = ((XtextSourceViewer)viewer).getContentAssistant();
            if (assistant instanceof ContentAssistant)
            {
                return Optional.ofNullable((ContentAssistant)assistant);
            }
        }
        return Optional.empty();
    }

    private void addCompletionListener(ContentAssistant contentAssistant)
    {
        if (contentAssistant != null)
        {
            contentAssistant.addCompletionListener(new ICompletionListener()
            {
                @Override
                public void assistSessionStarted(ContentAssistEvent event)
                {
                    isProposalMenuOpened = true;
                    reset();
                }

                @Override
                public void assistSessionEnded(ContentAssistEvent event)
                {
                    isProposalMenuOpened = false;
                }

                @Override
                public void selectionChanged(ICompletionProposal proposal, boolean smartToggle)
                {
                    isProposalMenuOpened = true;
                    reset();
                }
            });
        }
    }
}