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
import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.Closeables;
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
import org.e1c.edt.ai.Text;
import org.e1c.edt.ai.assistent.ICodeAssistant;
import org.e1c.edt.ai.assistent.ICompletionRequestProvider;
import org.e1c.edt.ai.assistent.model.CompletionRequest;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class CodeCompletionViewModel
    implements ICodeCompletionViewModel<CodeCompletionContext>, VerifyKeyListener, CaretListener, TraverseListener
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
    private ICodeCompletionSession<CodeCompletionContext> lastSession;
    private StyledText textWidget;
    private AutoCloseable feedbackToken = Closeables.Empty;
    private Job lastJob;
    private String proposal = ""; //$NON-NLS-1$
    private Duration requestDuration = Duration.ZERO;
    private boolean isTraversed;
    private AssistantListener assistantListener = new AssistantListener();

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
        IGlobalContextManager globalContextManager, ISyntaxVaidator syntaxVaidator)
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
    }

    @Override
    public AutoCloseable activate(StyledText textWidget)
    {
        synchronized (lockObject)
        {
            this.textWidget = textWidget;
            reset();
            if (!textWidget.isDisposed())
            {
                textWidget.addPaintListener(hintPainter);
                textWidget.addTraverseListener(this);
                textWidget.addCaretListener(this);
                textWidget.addVerifyKeyListener(this);
            }

            dispatcher.dispatchAsync(() -> {
                if (!textWidget.isDisposed())
                {
                    getContentAssistant().ifPresent(assistant -> assistant.addCompletionListener(assistantListener));
                    textWidget.redraw();

                    // Warm up
                    aiContextProvider.create(new AITarget(textWidget, 0, false), CancellationTokens.NONE)
                        .ifPresent(aiCtx -> globalContextManager.warmup(aiCtx, CancellationTokens.NONE));
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
            final var contextProvider = CreateContextProvider(localContextProvider, maxDuration);
            getAiContext(jobCtx.CancellationTokenSource)
                .ifPresent(aiCtx -> ask(aiCtx, contextProvider, delayBeforeShow, codeCompletionLinesCount,
                    jobCtx.CancellationTokenSource));
        }, null);
        job.setPriority(Job.SHORT);
        this.lastJob = job;
        job.schedule(delayBeforeAsk.toMillis());
    }

    private void warmupLocalContext()
    {
        var warmupJob =
            dispatcher.createJob(Messages.CodeCompletionJobName,
                ct -> CreateContextProvider(null, uiSettings.getTimeout()), null);
        warmupJob.schedule();
    }

    private CompletionRequestProvider CreateContextProvider(CompletionRequestProvider localContextProvider,
        Duration maxDuration)
    {
        return localContextProvider != null ? localContextProvider : new CompletionRequestProvider(maxDuration);
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
            }

            dispatcher.dispatch(() -> {
                if (!textWidget.isDisposed())
                {
                    getAiContext(CancellationTokens.NONE).ifPresent(aiCtx -> globalContextManager.sync(aiCtx));
                    getContentAssistant().ifPresent(assistant -> assistant.removeCompletionListener(assistantListener));
                    textWidget.redraw();
                }
            });
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
                .map(sourceViewer -> {
                    var doc = sourceViewer.getDocument();
                    if (!(doc instanceof IXtextDocument))
                    {
                        return null;
                    }

                    return ((IXtextDocument)doc).readOnly(s -> s.getParseResult());
                })
                .flatMap(parseResult -> codeProvider.getMethod(parseResult, aiCtx.getTextOffset()))
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
        var prefix = source.substring(0, sourceOffset);
        dispatcher.dispatch(() -> {
            String postfix;
            if (session.getContext().isSingleWordMode())
            {
                postfix = source.substring(sourceOffset);
                var lineFinish = postfix.indexOf(widget.getLineDelimiter());
                if (lineFinish > 0)
                {
                    postfix = postfix.substring(0, lineFinish);
                }
            }
            else
            {
                postfix = widget.getLineDelimiter();
            }

            var code = prefix + hintLines + postfix;
            var validCodeSize =
                syntaxVaidator.getValidCodeSize(aiContext.getPath(), code) - prefix.length();
            if (validCodeSize <= 0)
            {
                validCodeSize = 0;
            }

            if (validCodeSize > hintLines.length())
            {
                validCodeSize = hintLines.length();
            }

            var validHintLines = hintLines.substring(0, validCodeSize);
            if (validHintLines.length() > 0)
            {
                hintPainter.setHintAt(session.getContext().getAiContext().getСaretOffset(), validHintLines,
                    hint.getText(HintPart.TOKEN).getText());
            }

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
        synchronized (lockObject)
        {
            if (isTraversed || lastSession == null || lastSession.isAccepting())
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
            localContext = new CompletionRequestProvider(uiSettings.getMinRequestDelay());
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
            getProposalText(prop).ifPresent(proposalText -> {
                proposal = proposalText;
                askWithDelay(Duration.ZERO, Duration.ZERO, Duration.ZERO, 1, localContext);
            });
        }
    }

    private Optional<String> getProposalText(ICompletionProposal proposal)
    {
        var content = textWidget.getText();
        var proposalDoc = new Document(content);
        proposal.apply(proposalDoc);
        var newContent = proposalDoc.get();
        var min = Integer.min(content.length(), newContent.length());
        int start;
        for (start = 0; start < min; start++)
        {
            if (content.charAt(start) != newContent.charAt(start))
            {
                break;
            }
        }

        if (start == min)
        {
            return Optional.empty();
        }

        int finish;
        var max = Integer.max(content.length(), newContent.length());
        for (finish = max - 1; finish > start; finish--)
        {
            if (content.charAt(finish - (max - content.length())) != newContent
                .charAt(finish - (max - newContent.length())))
            {
                break;
            }
        }

        var result = newContent.substring(start, finish + 1);
        if (!result.isBlank() && !proposal.getDisplayString().startsWith(result))
        {
            for (finish = 0; finish < result.length(); finish++)
            {
                if (!Character.isLetterOrDigit(result.charAt(finish)))
                {
                    finish++;
                    break;
                }
            }

            if (finish > result.length())
            {
                finish = result.length();
            }

            result = result.substring(0, finish);
        }

        if (result.isBlank())
        {
            return Optional.empty();
        }

        return Optional.of(result);
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
        private AIContext lastAiContext;
        private CompletionRequest lastRequest;
        private String originalPrefix;

        public CompletionRequestProvider(Duration maxDuration)
        {
            Preconditions.checkNotNull(maxDuration);
            this.maxDuration = maxDuration;
        }

        @Override
        public synchronized Optional<CompletionRequest> get(IStatistics statistics,
            ICancellationToken cancellationToken)
        {
            var optionalAiCtx = getAiContext(cancellationToken);
            if (optionalAiCtx.isEmpty())
            {
                return Optional.empty();
            }

            var aiCtx = optionalAiCtx.get();
            if (lastRequest != null && lastAiContext != null && lastAiContext.equals(aiCtx))
            {
                lastRequest.localContext.prefix = originalPrefix + proposal;
                return Optional.of(lastRequest);
            }

            var expirationDate = clock.now().plus(maxDuration);
            var expiringCancellationToken = CancellationTokens.expiresAt(cancellationToken, clock, expirationDate);
            lastAiContext = aiCtx;
            lastRequest = new CompletionRequest();
            lastRequest.localContext =
                localContextFactory.createLocalContext(aiCtx, statistics, expiringCancellationToken);
            originalPrefix = lastRequest.localContext.prefix;
            lastRequest.localContext.prefix = originalPrefix + proposal;
            return Optional.of(lastRequest);
        }
    }
}