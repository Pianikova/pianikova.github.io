/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.CodeMethod;
import org.e1c.edt.ai.HintPart;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.ICodeCompletionActionHandler;
import org.e1c.edt.ai.ICodeCompletionContext;
import org.e1c.edt.ai.ICodeCompletionSession;
import org.e1c.edt.ai.ICodeProvider;
import org.e1c.edt.ai.IHintHistory;
import org.e1c.edt.ai.IInputDelayStatistics;
import org.e1c.edt.ai.ILocalContextFactory;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Observers;
import org.e1c.edt.ai.StatisticsType;
import org.e1c.edt.ai.Text;
import org.e1c.edt.ai.assistent.ICodeAssistant;
import org.e1c.edt.ai.assistent.ICompletionRequestProvider;
import org.e1c.edt.ai.assistent.model.CompletionRequest;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class CodeCompletionViewModel
    implements ICodeCompletionViewModel<CodeCompletionContext>, VerifyKeyListener, CaretListener, TraverseListener,
    ModifyListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final IUISettings uiSettings;
    private final ICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
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
    private final ILocalContextFactory localContextFactory;
    private final IHotKeys hotKeys;
    private final IGlobalContextManager globalContextManager;
    private final ISyntaxVaidator syntaxVaidator;
    private final IProposalsProvider proposalsProvider;
    private ICodeCompletionSession<CodeCompletionContext> lastSession;
    private StyledText textWidget;
    private AutoCloseable feedbackToken = Closeables.Empty;
    private Job lastJob;
    private String proposal = ""; //$NON-NLS-1$
    private Duration requestDuration = Duration.ZERO;
    private boolean isTraversed;
    private AssistantListener assistantListener = new AssistantListener();
    private CodeMethod lastCurrentMethod;
    private boolean isModifed;

    @Inject
    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, IUISettings uiSettings,
        ICodeAssistant codeAssistant,
        IAIContextProvider aiContextProvider,
        IDispatcher dispatcher, IHintPainter hintPainter, IInputDelayStatistics inputRateStatistics,
        IClock clock,
        Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider,
        ICodeCompletionActionHandler<CodeCompletionContext> handler, IHintHistory history, IUserActions userActions,
        ICodeCompletionContext codeCompletionContext, IUI ui, ICodeProvider codeProvider,
        ILocalContextFactory localContextFactory, IHotKeys hotKeys,
        IGlobalContextManager globalContextManager, ISyntaxVaidator syntaxVaidator,
        IProposalsProvider proposalsProvider)
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
        Preconditions.checkNotNull(localContextFactory);
        Preconditions.checkNotNull(hotKeys);
        Preconditions.checkNotNull(globalContextManager);
        Preconditions.checkNotNull(syntaxVaidator);
        Preconditions.checkNotNull(proposalsProvider);
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
        this.localContextFactory = localContextFactory;
        this.hotKeys = hotKeys;
        this.globalContextManager = globalContextManager;
        this.syntaxVaidator = syntaxVaidator;
        this.proposalsProvider = proposalsProvider;
    }

    @Override
    public AutoCloseable activate(StyledText textWidget)
    {
        synchronized (lockObject)
        {
            this.textWidget = textWidget;
            lastSession = null;
            isModifed = false;
            lastCurrentMethod = null;

            reset();
            if (!textWidget.isDisposed())
            {
                textWidget.addPaintListener(hintPainter);
                textWidget.addTraverseListener(this);
                textWidget.addCaretListener(this);
                textWidget.addVerifyKeyListener(this);
                textWidget.addModifyListener(this);
            }

            dispatcher.dispatchAsync(() -> {
                if (!textWidget.isDisposed())
                {
                    getContentAssistant().ifPresent(assistant -> assistant.addCompletionListener(assistantListener));
                    textWidget.redraw();

                    // Warm up
                    aiContextProvider.create(new AITarget(textWidget, 0, false), CancellationTokens.NONE)
                        .ifPresent(aiCtx -> globalContextManager.update(aiCtx, CancellationTokens.NONE));
                    warmupLocalContext();
                }
            });

            return Closeables.create(() -> deactivate());
        }
    }

    private void reset()
    {
        cancel();
        history.clear();
        hideHint();
    }

    private void hideHint()
    {
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
            "Predicted hint delay " + delayBeforeShow.toMillis() + " ms, actual delay " + delay.toMillis() + " ms", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            () -> ""); //$NON-NLS-1$
        reset();
        askWithDelay(delay, delayBeforeShow, uiSettings.getMinRequestDelay(), uiSettings.getCodeCompletionLinesCount(),
            null);
    }

    private void askWithDelay(Duration delayBeforeAsk, Duration delayBeforeShow, Duration maxDuration,
        int codeCompletionLinesCount, CompletionRequestProvider localContextProvider)
    {
        cancel();
        var job = dispatcher.createJob(Messages.CodeCompletionJobName, jobCtx -> {
            getAiContext(jobCtx.CancellationTokenSource)
                .ifPresent(aiCtx -> {
                    var startTime = clock.now();
                    var proposals = proposalsProvider.getProposals(aiCtx, textWidget, jobCtx.CancellationTokenSource)
                        .orElseGet(() -> new ArrayList<>());
                    final var contextProvider = CreateContextProvider(localContextProvider, maxDuration, proposals);
                    var newDelayBeforeShow = calculateDelay(startTime, delayBeforeShow);
                    ask(aiCtx, contextProvider, newDelayBeforeShow, codeCompletionLinesCount,
                        jobCtx.CancellationTokenSource);
                });
        }, null);
        job.setPriority(Job.SHORT);
        this.lastJob = job;
        job.schedule(delayBeforeAsk.toMillis());
    }

    private void warmupLocalContext()
    {
        var warmupJob =
            dispatcher.createJob(Messages.CodeCompletionJobName,
                ct -> CreateContextProvider(null, uiSettings.getTimeout(), new ArrayList<>()), null);
        warmupJob.schedule();
    }

    private CompletionRequestProvider CreateContextProvider(CompletionRequestProvider localContextProvider,
        Duration maxDuration, List<String> proposals)
    {
        return localContextProvider != null ? localContextProvider
            : new CompletionRequestProvider(maxDuration, proposals);
    }

    private void deactivate()
    {
        synchronized (lockObject)
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
            if (!textWidget.isDisposed())
            {
                textWidget.removePaintListener(hintPainter);
                textWidget.removeCaretListener(this);
                textWidget.removeVerifyKeyListener(this);
                textWidget.removeTraverseListener(this);
                textWidget.removeModifyListener(this);
            }

            dispatcher.dispatch(() -> {
                if (!textWidget.isDisposed())
                {
                    getContentAssistant().ifPresent(assistant -> assistant.removeCompletionListener(assistantListener));
                    textWidget.redraw();
                }
            });

            lastSession = null;
            isModifed = false;
            lastCurrentMethod = null;
        }
    }

    private void ask(AIContext aiCtx, CompletionRequestProvider localContextProvider,
        Duration delayBeforeShow,
        int codeCompletionLinesCount,
        CancellationTokenSource cancellationTokenSource)
    {
        try
        {
            var startTime = clock.now();
            var codeCompletionCtx =
                new CodeCompletionContext(codeCompletionContext, aiCtx, textWidget, cancellationTokenSource);
            var singleWordMode = dispatcher.dispatch(() -> codeCompletionCtx.isSingleWordMode()).orElse(false);
            var session =
                sessionProvider.get().initiaize(codeCompletionCtx, history, codeCompletionLinesCount, singleWordMode);
            synchronized (lockObject)
            {
                if (lastSession != null)
                {
                    lastSession.getContext().getCancellationTokenSource().cancel();
                    lastSession.reset();
                }

                lastSession = session;
            }

            log.trace("AI context " + cancellationTokenSource, () -> aiCtx.toString()); //$NON-NLS-1$
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
                .flatMap(sourceViewer -> getCurrentMethod(sourceViewer, aiCtx.getTextOffset()))
                .ifPresent(method -> session.setMethod(method));

            var completionSource =
                codeAssistant.createSource(aiCtx.getProjectId(), localContextProvider, cancellationTokenSource);
            requestDuration = Duration.between(startTime, clock.now());

            // @formatter:off
            completionSource.subscribe(Observers.create(
                data -> {
                    if (cancellationTokenSource.isCanceled())
                    {
                        return;
                    }

                    globalContextManager.update(aiCtx, data, cancellationTokenSource);
                    var uuid = data.uuid;
                    if (uuid != null && !uuid.isBlank())
                    {
                        session.setId(uuid);
                        cancellationTokenSource.setName(uuid);
                    }

                    var hint = session.getHint();
                    var text = data.text;
                    if (!proposal.isBlank()) {
                        hint.append(new Text(proposal, session));
                        proposal = "";  //$NON-NLS-1$
                    }

                    hint.append(new Text(text, session));
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
                    log.trace("AI generated text " + cancellationTokenSource, () -> format(hint.toString())); //$NON-NLS-1$
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

    private Optional<CodeMethod> getCurrentMethod(SourceViewer sourceViewer, int offset)
    {
        return dispatcher.dispatch(() -> {
            var doc = sourceViewer.getDocument();
            if (!(doc instanceof IXtextDocument))
            {
                return null;
            }

            return ((IXtextDocument)doc).readOnly(s -> s.getParseResult());
        }).flatMap(parseResult -> codeProvider.getMethod(parseResult, offset));
    }

    private void cancel()
    {
        showTimer.purge();
        synchronized (lockObject)
        {
            if (lastJob != null)
            {
                lastJob.cancel();
                lastJob = null;
            }

            if (lastSession != null)
            {
                lastSession.getContext().getCancellationTokenSource().cancel();
                lastSession.reset();
                lastSession = null;
                isTraversed = false;
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

        var context = session.getContext();
        var widget = context.getWidget();
        var hint = session.getHint();
        var hintLines = hint.getText(HintPart.LINES).getText();
        var aiContext = context.getAiContext();
        var source = aiContext.getSource();
        var sourceOffset = aiContext.getSourceOffset();
        var prefixStart = dispatcher.dispatch(
            () -> ui.getSourceViewer(textWidget).flatMap(sourceViewer -> getCurrentMethod(sourceViewer, sourceOffset)))
            .flatMap(i -> i)
            .map(i -> i.getStartOffest())
            .orElse(0);

        if (prefixStart > sourceOffset)
        {
            prefixStart = sourceOffset;
        }

        var prefix = source.substring(prefixStart, sourceOffset);
        var postfix = dispatcher.dispatch(() -> {
            if (session.getContext().isSingleWordMode())
            {
                var result = source.substring(sourceOffset);
                var lineFinish = result.indexOf(widget.getLineDelimiter());
                if (lineFinish > 0)
                {
                    return result.substring(0, lineFinish);
                }
            }

            return widget.getLineDelimiter();
        }).orElse(""); //$NON-NLS-1$

        var code = prefix + hintLines + postfix;
        var validCodeSize =
            syntaxVaidator.getValidCodeSize(aiContext.getPath(), code, prefix.length()) - prefix.length();
        if (validCodeSize <= 0)
        {
            validCodeSize = 0;
        }

        if (validCodeSize > hintLines.length())
        {
            validCodeSize = hintLines.length();
        }

        var validHintLines = hintLines.substring(0, validCodeSize);
        dispatcher.dispatch(() -> {
            if (uiSettings.traceMode())
            {
                log.trace("Syntax check " + context.getCancellationTokenSource(), () -> { //$NON-NLS-1$
                    var message = new StringBuilder();
                    if (hintLines.length() != validHintLines.length())
                    {
                        message.append("Original hint: ["); //$NON-NLS-1$
                        message.append(hintLines);
                        message.append(']');
                        message.append(System.lineSeparator());
                        message.append(System.lineSeparator());

                        message.append("Valid hint:    ["); //$NON-NLS-1$
                        message.append(validHintLines);
                        message.append(']');
                        message.append(System.lineSeparator());
                        message.append(System.lineSeparator());
                    }
                    else
                    {
                        message.append("Hint is valid: ["); //$NON-NLS-1$
                        message.append(validHintLines);
                        message.append(']');
                        message.append(System.lineSeparator());
                        message.append(System.lineSeparator());
                    }

                    message.append("Code to check: ["); //$NON-NLS-1$
                    message.append(code);
                    message.append(']');

                    return message.toString();
                });
            }

            if (validHintLines.length() > 0)
            {
                hintPainter.setHintAt(session.getContext().getAiContext().getСaretOffset(), validHintLines,
                    hint.getText(HintPart.TOKEN).getText());

                widget.redraw();
            }
            else
            {
                if (session.isСompleted())
                {
                    reset();
                }
            }
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
            askWithDelay(Duration.ZERO, Duration.ZERO, uiSettings.getMinRequestDelay(),
                uiSettings.getCodeCompletionLinesCount(), null);
            event.doit = false;
            break;

        case UPDATE:
            if (session != null)
            {
                textWidget.setFocus();
                update(session);
                if (session.isDone() && !session.getContext().isSingleWordMode())
                {
                    askWithDelay(Duration.ZERO, Duration.ZERO, uiSettings.getMinRequestDelay(),
                        uiSettings.getCodeCompletionLinesCount(), null);
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
            var isSingleWordMode = dispatcher.dispatch(() -> session.getContext().isSingleWordMode()).orElse(false);
            event.doit = !isSingleWordMode;
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

        if (!event.doit)
        {
            isTraversed = false;
        }
    }

    @Override
    public void keyTraversed(TraverseEvent event)
    {
        synchronized (lockObject)
        {
            isTraversed = lastSession != null && hotKeys.isTriggered(event);
        }
    }

    @Override
    public void caretMoved(CaretEvent event)
    {
        // sync
        dispatcher.dispatchAsync(() -> ui.getSourceViewer(textWidget)
                .flatMap(sourceViewer -> getCurrentMethod(sourceViewer, textWidget.getCaretOffset()))
                .ifPresent(currentMethod -> {
                if (!currentMethod.equals(lastCurrentMethod))
                    {
                    if (isModifed)
                    {
                        lastCurrentMethod = currentMethod;
                        isModifed = false;
                        methodChanged(currentMethod);
                    }
                    }
            }));

        synchronized (lockObject)
        {
            if (isTraversed || lastSession == null || lastSession.isAccepting())
            {
                return;
            }
        }

        reset();
    }

    @Override
    public void modifyText(ModifyEvent e)
    {
        isModifed = true;
    }

    private void methodChanged(CodeMethod method)
    {
        if (textWidget == null)
        {
            return;
        }

        aiContextProvider.create(new AITarget(textWidget, 0, false), CancellationTokens.NONE)
            .ifPresent(aiCtx -> globalContextManager.update(aiCtx, CancellationTokens.NONE));
    }

    private void commit(ICodeCompletionSession<CodeCompletionContext> session)
    {
        if (session == null)
        {
            return;
        }

        session.getContext().commit(session.getId(), session.getContext().getAiContext().getTextOffset());
    }

    private Optional<ContentAssistantFacade> getContentAssistant()
    {
        return ui.getSourceViewer(textWidget).map(sourceViewer -> sourceViewer.getContentAssistantFacade());
    }

    private class AssistantListener
        implements ICompletionListener, ICompletionListenerExtension2
    {
        private CompletionRequestProvider localContext;
        private ICompletionProposal lastProp;

        @Override
        public void applied(ICompletionProposal pro)
        {
            reset();
        }

        @Override
        public void assistSessionStarted(ContentAssistEvent event)
        {
            reset();
            localContext = new CompletionRequestProvider(uiSettings.getMinRequestDelay(), new ArrayList<>());
        }

        @Override
        public void assistSessionEnded(ContentAssistEvent event)
        {
            proposal = ""; //$NON-NLS-1$
            localContext = null;
            lastProp = null;
        }

        @Override
        public void selectionChanged(ICompletionProposal prop, boolean smartToggle)
        {
            if (lastProp == prop)
            {
                return;
            }

            lastProp = prop;
            reset();
            proposalsProvider.getProposal(textWidget.getText(), prop).ifPresent(proposalText -> {
                proposal = proposalText;
                askWithDelay(Duration.ZERO, Duration.ZERO, Duration.ZERO, 1, localContext);
            });
        }
    }

    public Optional<AIContext> getAiContext(ICancellationToken cancellationToken)
    {
        return dispatcher.dispatch(
            () -> aiContextProvider.create(new AITarget(textWidget, 0, false), cancellationToken).orElse(null));
    }

    private class CompletionRequestProvider
        implements ICompletionRequestProvider
    {
        private final Duration maxDuration;
        private final List<String> proposals;
        private AIContext lastAiContext;
        private CompletionRequest lastRequest;
        private String originalPrefix;

        public CompletionRequestProvider(Duration maxDuration, List<String> proposals)
        {
            Preconditions.checkNotNull(maxDuration);
            Preconditions.checkNotNull(proposals);
            this.maxDuration = maxDuration;
            this.proposals = proposals;
        }

        @Override
        public synchronized Optional<CompletionRequest> get(IStatistics statistics,
            ICancellationToken cancellationToken)
        {
            AIContext aiCtx;
            try (var measurement = statistics.measureDuration(StatisticsType.AI_CONTEXT_DURATUION))
            {
                var optionalAiCtx = getAiContext(cancellationToken);
                if (optionalAiCtx.isEmpty())
                {
                    return Optional.empty();
                }

                aiCtx = optionalAiCtx.get();
                if (lastRequest != null && lastAiContext != null && lastAiContext.equals(aiCtx))
                {
                    lastRequest.localContext.prefix = originalPrefix + proposal;
                    return Optional.of(lastRequest);
                }
            }
            catch (Exception error)
            {
                log.logError(error);
                return Optional.empty();
            }

            var expirationDate = clock.now().plus(maxDuration);
            var expiringCancellationToken = CancellationTokens.expiresAt(cancellationToken, clock, expirationDate);
            lastAiContext = aiCtx;
            lastRequest = new CompletionRequest();
            lastRequest.localContext =
                localContextFactory.createLocalContext(aiCtx, statistics, expiringCancellationToken);
            originalPrefix = lastRequest.localContext.prefix;
            lastRequest.localContext.prefix = originalPrefix + proposal;
            lastRequest.localContext.proposals = proposals;
            return Optional.of(lastRequest);
        }
    }
}