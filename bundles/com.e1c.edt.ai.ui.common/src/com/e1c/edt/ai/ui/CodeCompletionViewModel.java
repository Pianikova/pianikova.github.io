/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.Delimiters;
import com.e1c.edt.ai.HintPart;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ICodeCompletionActionHandler;
import com.e1c.edt.ai.ICodeCompletionContext;
import com.e1c.edt.ai.ICodeCompletionSession;
import com.e1c.edt.ai.ICodeCompletionStatistics;
import com.e1c.edt.ai.ICodeCompletionTokenizer;
import com.e1c.edt.ai.ICodeProvider;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.IHintHistory;
import com.e1c.edt.ai.IInputDelayStatistics;
import com.e1c.edt.ai.ILocalContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.Observers;
import com.e1c.edt.ai.Sources;
import com.e1c.edt.ai.StatisticsType;
import com.e1c.edt.ai.Text;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ICodeAssistant;
import com.e1c.edt.ai.assistent.ICompletionRequestProvider;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.CompletionRequest;
import com.e1c.edt.ai.assistent.model.Proposal;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class CodeCompletionViewModel
    implements ICodeCompletionViewModel<CodeCompletionContext>, VerifyKeyListener, CaretListener, TraverseListener,
    ModifyListener, SelectionListener, ControlListener, MouseListener, IDocumentListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final ISettings settings;
    private final ICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
    private final IDispatcher dispatcher;
    private final IHintPainter hintPainter;
    private final IVerticalRulerPainter verticalRulerPainter;
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
    private final ILocalContext localContext;
    private final IHotKeys hotKeys;
    private final IGlobalContextManager globalContextManager;
    private final ISyntaxVaidator syntaxVaidator;
    private final IProposalsProvider proposalsProvider;
    private final ICodeParser codeParser;
    private final ITextWidgetInfoUpdater textWidgetInfoUpdater;
    private final ICodeCompletionStatistics statistics;
    private final ICodeCompletionTokenizer tokenizer;
    private final IVerticalRulerManager rulerManager;
    private final IClipboard clipboard;
    private final ArrayList<CodeMethod> methods = new ArrayList<>();
    private ICodeCompletionSession<CodeCompletionContext> lastSession;
    private StyledText textWidget;
    private SourceViewer sourceViewer;
    private AutoCloseable feedbackToken = Closeables.Empty;
    private AutoCloseable rulerManagerFreezeToken = Closeables.Empty;
    private Job lastJob;
    private Job lastUpdateMethodJob;
    private Job commitJob;
    private List<Proposal> lastProposals = new ArrayList<>();
    private Duration requestDuration = Duration.ZERO;
    private boolean isTraversed;
    private AssistantListener assistantListener = new AssistantListener();
    private Optional<CodeMethod> prevMethod = Optional.empty();
    private boolean isTextModifed;

    @Inject
    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, ISettings settings,
        ICodeAssistant codeAssistant,
        IAIContextProvider aiContextProvider,
        IDispatcher dispatcher, IHintPainter hintPainter, IVerticalRulerPainter verticalRulerPainter,
        IInputDelayStatistics inputRateStatistics,
        IClock clock,
        Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider,
        ICodeCompletionActionHandler<CodeCompletionContext> handler, IHintHistory history, IUserActions userActions,
        ICodeCompletionContext codeCompletionContext, IUI ui, ICodeProvider codeProvider,
        ILocalContext localContext, IHotKeys hotKeys,
        IGlobalContextManager globalContextManager, ISyntaxVaidator syntaxVaidator,
        IProposalsProvider proposalsProvider, ICodeParser codeParser, ITextWidgetInfoUpdater textWidgetInfoUpdater,
        ICodeCompletionStatistics statistics, ICodeCompletionTokenizer tokenizer, IVerticalRulerManager rulerManager,
        IClipboard clipboard)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(codeAssistant);
        Preconditions.checkNotNull(aiContextProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(hintPainter);
        Preconditions.checkNotNull(verticalRulerPainter);
        Preconditions.checkNotNull(inputRateStatistics);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(sessionProvider);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(history);
        Preconditions.checkNotNull(userActions);
        Preconditions.checkNotNull(codeCompletionContext);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(codeProvider);
        Preconditions.checkNotNull(localContext);
        Preconditions.checkNotNull(hotKeys);
        Preconditions.checkNotNull(globalContextManager);
        Preconditions.checkNotNull(syntaxVaidator);
        Preconditions.checkNotNull(proposalsProvider);
        Preconditions.checkNotNull(codeParser);
        Preconditions.checkNotNull(textWidgetInfoUpdater);
        Preconditions.checkNotNull(statistics);
        Preconditions.checkNotNull(tokenizer);
        Preconditions.checkNotNull(rulerManager);
        Preconditions.checkNotNull(clipboard);
        this.log = log;
        this.codeAssistant = codeAssistant;
        this.settings = settings;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.hintPainter = hintPainter;
        this.verticalRulerPainter = verticalRulerPainter;
        this.inputRateStatistics = inputRateStatistics;
        this.clock = clock;
        this.sessionProvider = sessionProvider;
        this.handler = handler;
        this.history = history;
        this.userActions = userActions;
        this.codeCompletionContext = codeCompletionContext;
        this.ui = ui;
        this.codeProvider = codeProvider;
        this.localContext = localContext;
        this.hotKeys = hotKeys;
        this.globalContextManager = globalContextManager;
        this.syntaxVaidator = syntaxVaidator;
        this.proposalsProvider = proposalsProvider;
        this.codeParser = codeParser;
        this.textWidgetInfoUpdater = textWidgetInfoUpdater;
        this.statistics = statistics;
        this.tokenizer = tokenizer;
        this.rulerManager = rulerManager;
        this.clipboard = clipboard;
    }

    @Override
    public AutoCloseable activate(StyledText textWidget)
    {
        synchronized (methods)
        {
            methods.clear();
        }

        synchronized (lockObject)
        {
            reset();
            lastSession = null;
            isTextModifed = false;
            prevMethod = Optional.empty();
            this.textWidget = textWidget;
            if (!textWidget.isDisposed())
            {
                sourceViewer = ui.getSourceViewer(textWidget).orElse(null);
                if (sourceViewer != null)
                {
                    addListeners(textWidget, sourceViewer);
                    redraw();
                    updateGlobalContext();
                    warmup();
                    var rulerManagerToken = rulerManager.activate(sourceViewer, () -> reset());
                    var activationToken = Closeables.create(() -> deactivate(textWidget, sourceViewer));
                    return Closeables.create(rulerManagerToken, activationToken);
                }
            }

            return Closeables.Empty;
        }
    }

    private boolean isEnabled()
    {
        return settings.isEnabled();
    }

    private boolean isBalanced()
    {
        return CodeCompletionPolicy.MODERATE.isMeet(settings.getCodeCompletionPolicy());
    }

    private boolean isCreative()
    {
        return CodeCompletionPolicy.INTENSVE.isMeet(settings.getCodeCompletionPolicy());
    }

    private void reset()
    {
        cancel();
        Job localLastUpdateMethodJob;
        synchronized (lockObject)
        {
            localLastUpdateMethodJob = lastUpdateMethodJob;
            lastUpdateMethodJob = null;
        }

        if (localLastUpdateMethodJob != null)
        {
            localLastUpdateMethodJob.cancel();
        }

        lastProposals.clear();
        history.clear();
        hideHint();
    }

    private void hideHint()
    {
        dispatcher.dispatch(() -> {
            try
            {
                rulerManagerFreezeToken.close();
            }
            catch (Exception e)
            {
                //
            }

            hintPainter.reset();
            verticalRulerPainter.reset();
            rulerManager.reset(sourceViewer);
            redraw();
        });
    }

    private void update(ICodeCompletionSession<CodeCompletionContext> session)
    {
        var content = session.getContext();
        var widget = content.getWidget();
        var hint = session.getHint();
        var offset = widget.getCaretOffset();
        hintPainter.pinOffset(textWidget, offset, true, session.getContext().isSingleWordMode());
        hintPainter.setHintAt(hint.getText(HintPart.LINES).getText(), hint.getText(HintPart.TOKEN).getText(),
            hint.getAcceptedTokens());
        verticalRulerPainter.pin(textWidget, hintPainter.getDisplayedHintText());
        textWidget.showSelection();
        redraw();
    }

    private void askNew()
    {
        if (!isEnabled())
        {
            return;
        }

        var delayBeforeShow = inputRateStatistics.registerAndPredictDelay();
        var delay = delayBeforeShow.minus(requestDuration);
        if (delay.toNanos() < settings.getMinRequestDelay().toNanos())
        {
            delay = settings.getMinRequestDelay();
        }

        log.trace(
            TracingSources.CODE_COMPETION,
            "Predicted hint delay " + delayBeforeShow.toMillis() + " ms, actual delay " + delay.toMillis() + " ms", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            () -> ""); //$NON-NLS-1$
        reset();
        askWithDelay(delay, delayBeforeShow, settings.getMinRequestDelay(), settings.getCodeCompletionLinesCount(),
            null, false, false);
    }

    private void askWithDelay(Duration delayBeforeAsk, Duration delayBeforeShow, Duration maxDuration,
        int codeCompletionLinesCount, CompletionRequestProvider localContextProvider, boolean forced,
        boolean contentAssist)
    {
        if (!isEnabled())
        {
            return;
        }

        cancel();
        synchronized (lockObject)
        {
            lastJob = dispatcher.createJob(Messages.CodeCompletionJobName, jobCtx -> {
                getAiContext(jobCtx.CancellationTokenSource).ifPresent(aiCtx -> {
                    var startTime = clock.now();
                    final var contextProvider =
                        CreateContextProvider(localContextProvider, maxDuration, forced, contentAssist);
                    var newDelayBeforeShow = calculateDelay(startTime, delayBeforeShow);
                    ask(aiCtx, contextProvider, newDelayBeforeShow, codeCompletionLinesCount,
                        jobCtx.CancellationTokenSource);
                });
            }, false, CancellationTokens.NONE);
            lastJob.setSystem(true);
            lastJob.setPriority(Job.INTERACTIVE);
            lastJob.schedule(delayBeforeAsk.toMillis());
        }
    }

    private void askWithoutDelay(boolean forced, boolean contentAssist)
    {
        if (!isEnabled())
        {
            return;
        }

        askWithDelay(Duration.ZERO, Duration.ZERO, settings.getMinRequestDelay(),
            settings.getCodeCompletionLinesCount(), null, forced, contentAssist);
    }

    private void warmup()
    {
        if (!isEnabled())
        {
            return;
        }

        var warmupJob =
            dispatcher.createJob(Messages.CodeCompletionJobName,
                ct -> CreateContextProvider(null, settings.getTimeout(), false, false), false, CancellationTokens.NONE);
        warmupJob.setSystem(true);
        warmupJob.setPriority(Job.DECORATE);
        warmupJob.schedule();
    }

    private void updateGlobalContext()
    {
        dispatcher.dispatch(
            () -> aiContextProvider.create(sourceViewer, new AITarget(textWidget, true, false),
                CancellationTokens.NONE))
            .flatMap(i -> i)
            .ifPresent(aiCtx -> globalContextManager.update(aiCtx, CancellationTokens.NONE));
    }

    private CompletionRequestProvider CreateContextProvider(CompletionRequestProvider localContextProvider,
        Duration maxDuration, boolean forced, boolean contentAssist)
    {
        return localContextProvider != null && localContextProvider.isForced() == forced
            && localContextProvider.isContentAssist() == contentAssist ? localContextProvider
                : new CompletionRequestProvider(maxDuration, forced, contentAssist);
    }

    private void deactivate(StyledText textWidget, SourceViewer sourceViewer)
    {
        synchronized (lockObject)
        {
            if (isTextModifed)
            {
                updateGlobalContext();
            }

            commit(lastSession);
            try
            {
                feedbackToken.close();
            }
            catch (Exception e)
            {
                // ignored
            }

            methodChanged(prevMethod.orElse(null), null);
            reset();
            if (!textWidget.isDisposed())
            {
                removeListeners(textWidget, sourceViewer);
                redraw();
            }

            lastSession = null;
            isTextModifed = false;
            prevMethod = Optional.empty();
        }
    }

    private void addListeners(StyledText textWidget, SourceViewer sourceViewer)
    {
        removeListeners(textWidget, sourceViewer);
        textWidget.addPaintListener(hintPainter);
        textWidget.addTraverseListener(this);
        textWidget.addCaretListener(this);
        textWidget.addVerifyKeyListener(this);
        textWidget.addModifyListener(this);
        textWidget.addControlListener(this);
        textWidget.addMouseListener(this);
        Optional.ofNullable(textWidget.getHorizontalBar()).ifPresent(scroll -> scroll.addSelectionListener(this));
        Optional.ofNullable(textWidget.getVerticalBar()).ifPresent(scroll -> scroll.addSelectionListener(this));
        Optional.ofNullable(sourceViewer.getContentAssistantFacade())
            .ifPresent(assistant -> assistant.addCompletionListener(assistantListener));
        var document = sourceViewer.getDocument();
        document.addDocumentListener(this);
    }

    private void removeListeners(StyledText textWidget, SourceViewer sourceViewer)
    {
        Optional.ofNullable(textWidget.getHorizontalBar())
            .ifPresent(scroll -> scroll.removeSelectionListener(this));
        Optional.ofNullable(textWidget.getVerticalBar())
            .ifPresent(scroll -> scroll.removeSelectionListener(this));
        Optional.ofNullable(sourceViewer.getContentAssistantFacade())
            .ifPresent(assistant -> assistant.removeCompletionListener(assistantListener));
        textWidget.removePaintListener(hintPainter);
        textWidget.removeCaretListener(this);
        textWidget.removeVerifyKeyListener(this);
        textWidget.removeTraverseListener(this);
        textWidget.removeModifyListener(this);
        textWidget.removeMouseListener(this);
        var document = sourceViewer.getDocument();
        document.removeDocumentListener(this);
    }


    @SuppressWarnings("nls")
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

            log.trace(TracingSources.CODE_COMPETION, "AI context " + cancellationTokenSource, () -> aiCtx.toString()); //$NON-NLS-1$
            var delay = calculateDelay(startTime, delayBeforeShow);
            if (cancellationTokenSource.isCanceled())
            {
                reset();
                return;
            }

            dispatcher.dispatch(() -> {
                hintPainter.reset();
                verticalRulerPainter.reset();
                hintPainter.pinOffset(textWidget, aiCtx.getCaretOffset(),
                    isCreative() || (delay.isNegative() || delay == Duration.ZERO),
                    singleWordMode);
                hintPainter.setHintAt("", "", 0);
                textWidget.showSelection();
                redraw();
            });

            getCurrentMethod(aiCtx.getTextOffset()).ifPresent(currentMethod -> session.setMethod(currentMethod));

            var completionSource =
                codeAssistant.createSource(aiCtx.getProjectId(), localContextProvider, cancellationTokenSource);
            requestDuration = Duration.between(startTime, clock.now());
            var processingStatistics = new ProcessingStatistics();
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
                    }

                    var hint = session.getHint();
                    var text = data.text;
                    if (lastProposals.size() > 0) {
                        hint.append(new Text(lastProposals.get(0).prefix, session));
                        lastProposals.clear();
                    }

                    hint.append(new Text(text, session));
                    showWithDelay(session, calculateDelay(startTime, delayBeforeShow), processingStatistics);
                    processingStatistics.totalDuration = processingStatistics.totalDuration.plus(Duration.between(data.startTime, clock.now()));
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
                    if (lastProposals.size() > 0) {
                        hint.append(new Text(lastProposals.get(0).prefix, Sources.UNKNOWN));
                        lastProposals.clear();
                    }

                    log.trace(TracingSources.CODE_COMPETION, "AI generated text " + cancellationTokenSource, () -> {
                        var message = new StringBuilder();
                        message.append(format(hint.toString()));

                        message.append(System.lineSeparator());
                        message.append("Total duration: ");
                        message.append(processingStatistics.totalDuration);

                        message.append(System.lineSeparator());
                        message.append("Syntax check duration: ");
                        message.append(processingStatistics.syntaxCheckDuration);
                        return message.toString();
                    });

                    if (hint.isEmpty() || (!isCreative() && hint.isBlank()))
                    {
                        hideHint();
                    }
                    else
                    {
                        showWithDelay(session, calculateDelay(startTime, delayBeforeShow), processingStatistics);
                    }

                    session.complete();
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
            reset();
        }
    }

    private Optional<CodeMethod> getCurrentMethod(int offset)
    {
        var result = getExistingMethod(offset);
        if (result.isPresent())
        {
            return result;
        }

        var newMethod =
            codeParser.parse(sourceViewer).flatMap(parseResult -> codeProvider.getMethod(parseResult, offset));

        if (newMethod.isPresent())
        {
            synchronized (methods)
            {
                if (getExistingMethod(offset).isEmpty())
                {
                    methods.add(newMethod.get());
                }
            }
        }

        return newMethod;
    }

    private Optional<CodeMethod> getExistingMethod(int offset)
    {
        synchronized (methods)
        {
            for (var method : methods)
            {
                if (offset >= method.getStartOffest() && offset <= method.getEndOffest())
                {
                    return Optional.of(method);
                }
            }
        }

        return Optional.empty();
    }

    private void cancel()
    {
        showTimer.purge();
        Job localLastJob;
        ICodeCompletionSession<CodeCompletionContext> localLastSession;
        synchronized (lockObject)
        {
            localLastJob = lastJob;
            lastJob = null;
            lastUpdateMethodJob = null;
            localLastSession = lastSession;
            lastSession = null;
            isTraversed = false;
        }

        if (localLastJob != null)
        {
            localLastJob.cancel();
        }

        if (localLastSession != null)
        {
            localLastSession.getContext().getCancellationTokenSource().cancel();
            localLastSession.reset();
        }
    }

    private void showWithDelay(ICodeCompletionSession<CodeCompletionContext> session, Duration delayBeforeShow,
        ProcessingStatistics processingStatistics)
    {
        showTimer.purge();
        if (delayBeforeShow.isNegative() || delayBeforeShow == Duration.ZERO || isCreative())
        {
            show(session, processingStatistics);
            return;
        }

        // @formatter:off
        showTimer.schedule(
            new TimerTask() {
                @Override
                public void run()
                {
                    show(session, processingStatistics);
                }
            },
            delayBeforeShow.toMillis());
        // @formatter:on
    }

    private void show(ICodeCompletionSession<CodeCompletionContext> session, ProcessingStatistics processingStatistics)
    {
        if (!isEnabled())
        {
            return;
        }

        if (session.getContext().getCancellationTokenSource().isCanceled())
        {
            return;
        }

        var context = session.getContext();
        var hint = session.getHint();
        if (hint.isEmpty())
        {
            return;
        }

        if (!isCreative() && hint.isBlank())
        {
            return;
        }

        var hintText = hint.getText(HintPart.LINES).getText();
        var startTime = clock.now();
        var optionalCode = dispatcher.dispatch(() -> new Code(textWidget.getText(), textWidget.getCaretOffset()));
        if (optionalCode.isEmpty())
        {
            return;
        }

        var code = optionalCode.get();
        var validHint = getCurrentMethod(code.offset).map(method -> syntaxVaidator.getValidHint(method, code.code,
            code.offset, hintText, context.getCancellationTokenSource()))
            .orElse(hintText);
        processingStatistics.syntaxCheckDuration =
            processingStatistics.syntaxCheckDuration.plus(Duration.between(startTime, clock.now()));
        if (validHint.length() > 0)
        {
            var nextToken = tokenizer.getNext(1, validHint, Delimiters::isTokenDelimiter);
            dispatcher.dispatch(() -> {
                hintPainter.setHintAt(validHint, nextToken.getValue(), hint.getAcceptedTokens());
                verticalRulerPainter
                    .pin(textWidget, hintPainter.getDisplayedHintText());
                try
                {
                    rulerManagerFreezeToken.close();
                }
                catch (Exception e)
                {
                    //
                }

                rulerManagerFreezeToken = rulerManager.freeze(sourceViewer);
                redraw();
            });
        }
        else
        {
            if (session.isCompleted())
            {
                reset();
            }
        }
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

    @SuppressWarnings("nls")
    @Override
    public void verifyKey(VerifyEvent event)
    {
        if (!isEnabled())
        {
            return;
        }

        ICodeCompletionSession<CodeCompletionContext> session;
        synchronized (lockObject)
        {
            session = lastSession;
        }

        var actionToProcess = userActions.getAction(event);
        var isContinuousCodeCompletion = isBalanced();
        var action = handler.handle(session, actionToProcess, event.character, hintPainter.getOffset(),
            isContinuousCodeCompletion);
        switch (action)
        {
        case SUGGEST:
            reset();
            askWithoutDelay(true, false);
            event.doit = false;
            break;

        case UPDATE:
            if (session != null)
            {
                textWidget.setFocus();
                update(session);
                if (session.isDone() && !session.getContext().isSingleWordMode())
                {
                    commit(session);
                    askWithoutDelay(false, false);
                }

                event.doit = false;
            }
            break;

        case ASK_NEW:
            commit(session);
            if (!textWidget.isTextSelected())
            {
                askNew();
            }
            break;

        case RESET:
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

        log.trace(TracingSources.CODE_COMPETION, "AI action", () -> {
            var message = new StringBuilder();
            message.append(actionToProcess.toString());
            message.append(" -> ");
            message.append(action);
            message.append(System.lineSeparator());
            message.append("handle: ");
            message.append(event.doit);
            message.append(System.lineSeparator());
            message.append("character code: ");
            message.append((int)event.character);
            message.append(System.lineSeparator());
            message.append("isContinuousCodeCompletion: ");
            message.append(isContinuousCodeCompletion);
            if (session != null)
            {
                message.append(System.lineSeparator());
                var aiCtx = session.getContext().getAiContext();
                message.append("offset: ");
                message.append(aiCtx.getCaretOffset());
            }

            return message.toString();
        });
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
        if (!isEnabled())
        {
            return;
        }

        updateMethodAsync();
        synchronized (lockObject)
        {
            if (isTraversed || lastSession == null || lastSession.isAccepting())
            {
                return;
            }
        }

        commit(lastSession);
        reset();
    }

    private void updateMethodAsync()
    {
        if (!isEnabled())
        {
            return;
        }

        Job localLastUpdateMethodJob;
        synchronized (lockObject)
        {
            localLastUpdateMethodJob = lastUpdateMethodJob;
            lastUpdateMethodJob = dispatcher.createJob(Messages.CodeCompletionJobName,
                jobCtx -> updateMethod(jobCtx.CancellationTokenSource), false, CancellationTokens.NONE);
            lastUpdateMethodJob.setSystem(true);
            lastUpdateMethodJob.setPriority(Job.DECORATE);
            lastUpdateMethodJob.schedule(100);
        }

        if (localLastUpdateMethodJob != null)
        {
            localLastUpdateMethodJob.cancel();
        }
    }

    @SuppressWarnings("nls")
    private void updateMethod(ICancellationToken cancellationToken)
    {
        if (!isEnabled())
        {
            return;
        }

        // sync
        var offset = dispatcher.dispatch(() -> textWidget.getCaretOffset());
        if (offset.isEmpty() || cancellationToken.isCanceled())
        {
            return;
        }

        var newMethod = getCurrentMethod(offset.get());
        if (cancellationToken.isCanceled())
        {
            return;
        }

        var newMethodName = newMethod.map(i -> i.getUniqueName()).orElse("");
        var prevMethodName = prevMethod.map(i -> i.getUniqueName()).orElse("");
        try
        {
            if (!newMethodName.equals(prevMethodName))
            {
                methodChanged(prevMethod.orElse(null), newMethod.orElse(null));
            }
        }
        finally
        {
            prevMethod = newMethod;
        }
    }

    @Override
    public void modifyText(ModifyEvent e)
    {
        synchronized (lockObject)
        {
            methods.clear();
            isTextModifed = true;
        }
    }

    @Override
    public void widgetSelected(SelectionEvent e)
    {
        if (hintPainter.getOffset() != -1)
        {
            redraw();
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e)
    {
        if (hintPainter.getOffset() != -1)
        {
            redraw();
        }
    }

    @Override
    public void controlMoved(ControlEvent e)
    {
        if (hintPainter.getOffset() != -1)
        {
            redraw();
        }
    }

    private void redraw()
    {
        if (textWidget != null && !textWidget.isDisposed())
        {
            textWidget.redraw();
            rulerManager.redraw(sourceViewer);
        }
    }

    @Override
    public void controlResized(ControlEvent e)
    {
        if (hintPainter.getOffset() != -1)
        {
            redraw();
        }
    }

    @Override
    public void mouseDoubleClick(MouseEvent e)
    {
        //
    }

    @Override
    public void mouseDown(MouseEvent e)
    {
        reset();
        var offset = textWidget.getOffsetAtPoint(new Point(e.x, e.y));
        if (offset < 0)
        {
            var line = textWidget.getLineIndex(e.y);
            if (line < textWidget.getLineCount() - 1)
            {
                offset = textWidget.getOffsetAtLine(line + 1) - 1;
            }
            else
            {
                offset = textWidget.getOffsetAtLine(line);
            }
        }

        textWidgetInfoUpdater.setLastMouseOffset(textWidget, offset);
    }

    @Override
    public void mouseUp(MouseEvent e)
    {
        //
    }

    @Override
    public void documentAboutToBeChanged(DocumentEvent event)
    {
        //
    }

    @Override
    public void documentChanged(DocumentEvent event)
    {
        if (!isEnabled())
        {
            return;
        }

        if (clipboard.isPasting())
        {
            ICodeCompletionSession<CodeCompletionContext> session;
            synchronized (lockObject)
            {
                session = lastSession;
            }

            if (session != null)
            {
                commit(session);
            }

            log.trace(TracingSources.CODE_COMPETION, "Clipboard paste", () -> '[' + event.fText + ']'); //$NON-NLS-1$
            dispatcher.dispatchAsync(() -> askNew());
        }
    }

    @SuppressWarnings("nls")
    private void methodChanged(CodeMethod prevMethod, CodeMethod newMethod)
    {
        log.trace(TracingSources.CODE_COMPETION, "Method was changed",
            () -> {
                var message = new StringBuilder();
                message.append("from: ");
                message.append(prevMethod != null ? prevMethod.getUniqueName() : "null");
                message.append(System.lineSeparator());
                message.append("to: "); //$NON-NLS-1$
                message.append(newMethod != null ? newMethod.getUniqueName() : "null");
                return message.toString();
            });

        if (isTextModifed && newMethod != null)
        {
            isTextModifed = false;
            updateGlobalContext();
        }

        if (prevMethod != null)
        {
            statistics.addMethod(prevMethod, null,
                i -> prevMethod.getParseResult()
                    .flatMap(parseResult -> codeProvider.getMethodBody(parseResult, prevMethod))
                    .orElse(""));
        }
    }

    private void commit(ICodeCompletionSession<CodeCompletionContext> session)
    {
        if (session == null)
        {
            return;
        }

        if (!isEnabled())
        {
            return;
        }

        if (commitJob != null)
        {
            commitJob.cancel();
        }

        var job = dispatcher.createJob(Messages.CodeCompletionJobName,
            jobCtx -> session.getContext().commit(session.getId(), session.getContext().getAiContext().getTextOffset()),
            false, CancellationTokens.NONE);
        job.setSystem(true);
        job.setPriority(Job.DECORATE);
        this.commitJob = job;
        job.schedule();
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
            localContext =
                new CompletionRequestProvider(settings.getMinRequestDelay(), false, true);
        }

        @Override
        public void assistSessionEnded(ContentAssistEvent event)
        {
            lastProposals.clear();
            localContext = null;
            lastProp = null;
        }

        @Override
        public void selectionChanged(ICompletionProposal prop, boolean smartToggle)
        {
            if (!isBalanced())
            {
                return;
            }

            if (lastProp == prop)
            {
                return;
            }

            lastProp = prop;
            reset();
            var optionalProposal = getAiContext(CancellationTokens.NONE)
                .flatMap(ctx -> proposalsProvider.getProposal(prop, 0, ctx.getPrefix()));

            if (optionalProposal.isEmpty())
            {
                return;
            }

            lastProposals.add(optionalProposal.get());
            askWithDelay(Duration.ZERO, settings.getMinRequestDelay(), Duration.ZERO, 1, localContext, false, true);
        }
    }

    public Optional<AIContext> getAiContext(ICancellationToken cancellationToken)
    {
        return dispatcher.dispatch(
            () -> aiContextProvider.create(sourceViewer, new AITarget(textWidget, true, false), cancellationToken)
                .orElse(null));
    }

    private class CompletionRequestProvider
        implements ICompletionRequestProvider
    {
        private final Duration maxDuration;
        private final boolean forced;
        private final boolean contentAssist;
        private AIContext lastAiContext;
        private CompletionRequest lastRequest;
        private String originalPrefix;

        public CompletionRequestProvider(Duration maxDuration, boolean forced,
            boolean contentAssist)
        {
            Preconditions.checkNotNull(maxDuration);
            Preconditions.checkNotNull(forced);
            Preconditions.checkNotNull(contentAssist);
            this.maxDuration = maxDuration;
            this.forced = forced;
            this.contentAssist = contentAssist;
        }

        @Override
        public synchronized Optional<CompletionRequest> get(IStatistics statistics,
            ICancellationToken cancellationToken)
        {
            dispatcher.checkThread(false, true);
            AIContext aiCtx;
            try (var measurement = statistics.measureDuration(StatisticsType.AI_CONTEXT_DURATUION))
            {
                var optionalAiCtx = getAiContext(cancellationToken);
                if (optionalAiCtx.isEmpty())
                {
                    return Optional.empty();
                }

                aiCtx = optionalAiCtx.get();
                if (lastRequest != null && lastAiContext != null && lastProposals.size() > 0
                    && lastAiContext.equals(aiCtx))
                {
                    lastRequest.localContext.prefix = originalPrefix + lastProposals.get(0).prefix;
                    return Optional.of(lastRequest);
                }
            }
            catch (Exception error)
            {
                log.logError(error);
                return Optional.empty();
            }

            if (maxDuration != Duration.ZERO)
            {
                var expirationDate = clock.now().plus(maxDuration);
                cancellationToken = CancellationTokens.expiresAt(cancellationToken, clock, expirationDate);
            }

            lastAiContext = aiCtx;
            lastRequest = new CompletionRequest();
            lastRequest.localContext =
                localContext.create(aiCtx, statistics, cancellationToken);
            originalPrefix = lastRequest.localContext.prefix;
            if (lastProposals.size() > 0)
            {
                lastRequest.localContext.prefix = originalPrefix + lastProposals.get(0).prefix;
                lastRequest.localContext.proposals = lastProposals;
            }
            else
            {
                lastRequest.localContext.proposals =
                    proposalsProvider.getProposals(aiCtx, sourceViewer, 600, cancellationToken)
                    .orElseGet(() -> new ArrayList<>());
            }

            lastRequest.localContext.forced = isForced();
            lastRequest.localContext.contentAssist = isContentAssist();
            clipboard.getClipboardInfo()
                .ifPresent(clipboardText -> lastRequest.localContext.clipboard = clipboardText);
            return Optional.of(lastRequest);
        }

        public boolean isForced()
        {
            return forced;
        }

        public boolean isContentAssist()
        {
            return contentAssist;
        }
    }

    private static class ProcessingStatistics
    {
        public Duration totalDuration = Duration.ZERO;
        public Duration syntaxCheckDuration = Duration.ZERO;
    }

    private static class Code
    {
        public final String code;
        public final int offset;

        public Code(String code, int offset)
        {
            this.code = code;
            this.offset = offset;
        }
    }
}