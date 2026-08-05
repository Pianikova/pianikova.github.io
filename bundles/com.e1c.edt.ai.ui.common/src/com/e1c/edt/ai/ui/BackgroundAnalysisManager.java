/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IConversationFacade;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ConversationSession;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.e1c.edt.ai.tools.ILocalHistoryUtils;
import com.google.inject.Inject;

/**
 * Менеджер фонового анализа изменений в коде.
 *
 * @author Bogdan Sushkov
 */
public class BackgroundAnalysisManager
{
    private static final String LATEST_REVISION = "latest"; //$NON-NLS-1$
    // Background review always runs under the lightweight "edt" conversation skill without is_chat.
    private static final String CONVERSATION_SKILL = "edt"; //$NON-NLS-1$
    // Local history for the just-saved change is written as part of the save but can lag behind the
    // POST_CHANGE notification that triggers analysis. Retry briefly so the first save of a file with
    // no prior history still resolves a diff base instead of falling back to an empty "latest".
    private static final int BASE_RESOLVE_RETRIES = 5;
    private static final long BASE_RESOLVE_RETRY_DELAY_MS = 100;
    // Debounce before starting a review: a full review takes minutes (several slow LLM rounds),
    // so starting it on every save just produces runs that the next save cancels. Wait for a
    // quiet period instead — a burst of saves yields one review, started after the last save.
    private static final long DEBOUNCE_DELAY_MS = 1_000;

    private static final class FileAnalysisState
    {
        // Токен последнего запущенного для этого файла ревью — чтобы погасить его при закрытии файла.
        final AtomicReference<CancellationTokenSource> currentToken = new AtomicReference<>();
        final AtomicLong generation = new AtomicLong();
        // отдельный диалог на файл
        final AtomicReference<ConversationSession> conversationSession = new AtomicReference<>();
        // База диффа для следующего ревью. null — ревью не "в долгу", брать «latest».
        // Непустое значение — id ревизии локальной истории, зафиксированный на старте
        // ревью, которое было вытеснено/упало: следующее ревью "диффует" от него,
        // чтобы дельты отменённых ревью не терялись.
        final AtomicReference<String> pendingBaseRevisionId = new AtomicReference<>();
    }

    private final IConversationFacade conversationFacade;
    private final IDispatcher dispatcher;
    private final ISkillExecutor skillExecutor;
    private final ILog log;
    private final ILocalHistoryUtils localHistoryUtils;
    private final ISettings settings;

    // Keyed by the file's on-disk location, not by IFile: the same physical file can belong to
    // several overlapping/nested projects in the workspace, so a single save fires onFileSaved with
    // several distinct IFile handles. Keying by location collapses them into one single-flight state,
    // so only one review runs instead of one per project (which produced duplicate markers).
    private final ConcurrentMap<String, FileAnalysisState> states = new ConcurrentHashMap<>();

    // Global single-flight: at most one background review runs at a time across all files. A new save
    // (of any file) cancels whatever is currently in flight, so a slow/previous review can no longer
    // land stale markers after a newer save — including markers from a review of a file the user has
    // since navigated away from. Aligns with IDEAI-510 (only the active editor file is analyzed).
    private final AtomicReference<CancellationTokenSource> activeRun = new AtomicReference<>();

    @Inject
    public BackgroundAnalysisManager(IConversationFacade conversationFacade, IDispatcher dispatcher,
        ISkillExecutor skillExecutor, ILog log, ILocalHistoryUtils localHistoryUtils, ISettings settings)
    {
        this.conversationFacade = conversationFacade;
        this.dispatcher = dispatcher;
        this.skillExecutor = skillExecutor;
        this.log = log;
        this.localHistoryUtils = localHistoryUtils;
        this.settings = settings;
    }

    /**
     * Файл сохранён. Гасим анализ, который сейчас в работе (этого или любого другого файла —
     * глобальный single-flight), берём новое поколение и планируем self-review последних
     * изменений с debounce-задержкой: ревью стартует только после {@link #DEBOUNCE_DELAY_MS}
     * «тишины», так что серия быстрых сохранений порождает один прогон, а не цепочку
     * начатых-и-отменённых. Отмена «честная»: собственный {@link CancellationTokenSource}
     * прокидывается в скилл и в диалог, поэтому вытесненное ревью реально останавливается
     * до следующего вызова setmarkers и не пишет устаревшие маркеры.
     */
    @SuppressWarnings("nls")
    public void onFileSaved(IFile file)
    {
        if (!settings.isEnabled() || !settings.isBackgroundAnalysisEnabled() || !shouldAnalyze(file))
        {
            log.trace(TracingSources.TOOLS,
                "[bg-analysis] skip: enabled=" + settings.isEnabled() + " bgEnabled="
                    + settings.isBackgroundAnalysisEnabled() + " shouldAnalyze=" + shouldAnalyze(file) + " file=" + file,
                () -> "");
            return;
        }
        FileAnalysisState state = states.computeIfAbsent(fileKey(file), k -> new FileAnalysisState());
        long myGeneration = state.generation.incrementAndGet();

        // Собственный токен этого прогона. Он же — «активный прогон» менеджера: ставим его сразу
        // (ещё до debounce-паузы) и гасим предыдущий активный прогон (любого файла) — и уже
        // работающий диалог, и чужой ещё-не-стартовавший debounce. Дельта отменённого ревью
        // остаётся закреплённой в pendingBaseRevisionId и попадёт в следующий прогон.
        CancellationTokenSource myToken = new CancellationTokenSource();
        state.currentToken.set(myToken);
        CancellationTokenSource previous = activeRun.getAndSet(myToken);
        if (previous != null)
        {
            previous.cancel();
        }

        log.trace(TracingSources.TOOLS,
            "[bg-analysis] scheduling gen=" + myGeneration + " debounceMs=" + DEBOUNCE_DELAY_MS + " file="
                + file.getFullPath() + " (cancelled previous run: " + (previous != null) + ")",
            () -> "");

        AutoAnalysisRequest request = new AutoAnalysisRequest(file, file.getProject());

        dispatcher.createJob("Background Analysis", context -> {
            // Пока шла debounce-пауза, могло прийти новое сохранение (нашего или другого файла) —
            // тогда наш токен уже отменён и/или поколение устарело: тихо уступаем место.
            if (myToken.isCanceled() || state.generation.get() != myGeneration)
            {
                log.trace(TracingSources.TOOLS,
                    "[bg-analysis] debounced-out gen=" + myGeneration + " file=" + request.getFile().getName(),
                    () -> "");
                return;
            }
            analyzeChanges(request, state, myGeneration, myToken).exceptionally(error -> {
                log.logError(error);
                return null;
            });
        }, false, CancellationTokens.NONE).schedule(DEBOUNCE_DELAY_MS);
    }

    /**
     * Файл закрыт — убираем состояние (в т.ч. историю чата по файлу) и гасим прогон.
     * Если события закрытия нет — можно не звать, мапа маленькая.
     */
    public void onFileClosed(IFile file)
    {
        FileAnalysisState state = states.remove(fileKey(file));
        if (state != null)
        {
            CancellationTokenSource token = state.currentToken.get();
            if (token != null)
            {
                token.cancel();
            }
        }
    }

    /**
     * Stable single-flight key for a file: its on-disk location, shared by all IFile handles that
     * point to the same physical file across overlapping/nested projects. Falls back to the
     * workspace path for resources without a filesystem location (e.g. linked/virtual).
     */
    private static String fileKey(IFile file)
    {
        var location = file.getLocation();
        return location != null ? location.toOSString() : file.getFullPath().toString();
    }

    private boolean shouldAnalyze(IFile file)
    {
        if (file == null || !file.exists())
        {
            return false;
        }
        String ext = file.getFileExtension();
        return "java".equalsIgnoreCase(ext) || "bsl".equalsIgnoreCase(ext); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Resolves the newest local-history revision id, retrying briefly while the history is still
     * empty. On the first save of a file with no prior history the entry for the just-saved change
     * can be written slightly after the POST_CHANGE notification; without the retry the diff base
     * would fall back to an unresolved "latest" and no markers would be produced on that first save.
     * Runs on a background worker thread, so a short blocking wait is acceptable; honours cancellation.
     */
    private Optional<String> resolveLatestWithRetry(IFile file, ICancellationToken token)
    {
        var latest = localHistoryUtils.getLatestRevisionId(file);
        for (int attempt = 0; latest.isEmpty() && attempt < BASE_RESOLVE_RETRIES && !token.isCanceled(); attempt++)
        {
            try
            {
                Thread.sleep(BASE_RESOLVE_RETRY_DELAY_MS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                break;
            }
            latest = localHistoryUtils.getLatestRevisionId(file);
        }
        return latest;
    }

    /**
     * Запускает self-review последних изменений файла (diff последней ревизии
     * локальной истории против текущего состояния). Диапазон определяет сам скилл
     * через тулы localhistory/localchanges.
     */
    @SuppressWarnings("nls")
    private CompletableFuture<Void> analyzeChanges(AutoAnalysisRequest request, FileAnalysisState state,
        long generation, ICancellationToken cancellationToken)
    {
        if (request == null)
        {
            return CompletableFuture.completedFuture(null);
        }
        log.trace(TracingSources.TOOLS, "Background self-review of file: " + request.getFile(), () -> "");

        var result = new CompletableFuture<Void>();

        dispatcher.createJob(NLS.bind(Messages.BackgroundAnalysisJobName, request.getFile().getName()), context -> {
            var token = cancellationToken;
            // The bar appears only after beginTask, and UNKNOWN because the number of tool rounds is
            // decided by the model; the reporter then keeps overwriting this task name.
            context.Monitor.beginTask(Messages.ConversationProgressStarting, IProgressMonitor.UNKNOWN);
            var progressReporter = new ConversationProgressReporter(context.Monitor);

            if (token.isCanceled())
            {
                result.complete(null); // вытеснены новым сохранением до старта — база остаётся закреплённой
                return;
            }

            // Фиксируем базу диффа на старте: если предыдущее ревью не завершилось,
            // база остаётся от него — его дельта попадёт в это ревью.
            String pinned = state.pendingBaseRevisionId.get();
            boolean freshPin = pinned == null;
            String baseRevisionId;
            boolean latestPresent;
            if (freshPin)
            {
                var latestOpt = resolveLatestWithRetry(request.getFile(), token);
                latestPresent = latestOpt.isPresent();
                baseRevisionId = latestOpt.orElse(LATEST_REVISION);
                state.pendingBaseRevisionId.set(baseRevisionId);
            }
            else
            {
                baseRevisionId = pinned;
                latestPresent = true;
            }
            var traceBase = baseRevisionId;
            var traceLatest = latestPresent;
            log.trace(TracingSources.TOOLS,
                "[bg-analysis] run gen=" + generation + " freshPin=" + freshPin + " latestPresent=" + traceLatest
                    + " base=" + traceBase + " file=" + request.getFile().getName(),
                () -> "");

            var problemLevel = settings.getBackgroundAnalysisProblemLevel();
            // @formatter:off
            SkillExecutionRequest skillRequest = new SkillExecutionRequest("background-code-analysis",
                Map.of("project_name", request.getProject().getName(),
                       "relative_file_path", request.getFile().getProjectRelativePath().toString(),
                       "absolute_file_path", request.getFile().getLocation().toOSString(),
                       "from_revision_id", baseRevisionId,
                       "allowed_severities", problemLevel.getAllowedSeverities()));
            // @formatter:on

            skillExecutor.executeAsync(skillRequest, token).handle((response, exception) -> {
                // Отмена — штатное событие (нас вытеснило новое сохранение), не ошибка: тихо завершаем.
                if (token.isCanceled())
                {
                    result.complete(null);
                    return null;
                }
                if (exception != null)
                {
                    result.completeExceptionally(exception);
                    return null;
                }
                return response;
            }).thenCompose(skillResponse -> {
                if (result.isDone())
                {
                    return CompletableFuture.<Void> completedFuture(null); // уже завершено выше (отмена/ошибка)
                }
                if (token.isCanceled())
                {
                    result.complete(null);
                    return CompletableFuture.<Void> completedFuture(null);
                }
                if (skillResponse == null)
                {
                    result.completeExceptionally(new RuntimeException("Skill execution returned null"));
                    return CompletableFuture.<Void> completedFuture(null);
                }

                ConversationSession session = state.conversationSession.get();
                boolean forceNew = session == null;
                var newReq = new SendUserMessageRequest(request.getProject(), skillResponse.getPrompt(),
                    session, forceNew, CONVERSATION_SKILL, Boolean.FALSE, null,
                    skillResponse.getAllowedTools().orElse(null), skillResponse.getCompletionPolicy().orElse(null));

                return conversationFacade.sendAsync(newReq, token, progressReporter).thenAccept(resultMessage -> {
                    if (token.isCanceled())
                    {
                        result.complete(null);
                        return;
                    }
                    if (resultMessage == null)
                    {
                        result.completeExceptionally(new RuntimeException("No message from conversation facade"));
                        return;
                    }
                    state.conversationSession.set(resultMessage.getSession());

                    var generatedMessage = resultMessage.getText();
                    if (generatedMessage == null || generatedMessage.isBlank())
                    {
                        log.warning("No generated message", () -> ""); //$NON-NLS-1$
                    }

                    // Ревью завершилось — долгов нет, следующее ревью диффует от «latest».
                    // Если нас уже вытеснило новое поколение (или другой файл занял активный
                    // прогон), база остаётся закреплённой, а activeRun не трогаем.
                    boolean stillCurrent = state.generation.get() == generation;
                    if (stillCurrent)
                    {
                        state.pendingBaseRevisionId.set(null);
                    }
                    activeRun.compareAndSet(cancellationToken instanceof CancellationTokenSource
                        ? (CancellationTokenSource)cancellationToken : null, null);

                    var replyLen = generatedMessage == null ? 0 : generatedMessage.length();
                    // The reply text goes into the lazy details supplier: it is the key diagnostic
                    // for silent early exits (the model finishing the review without setmarkers).
                    log.trace(TracingSources.TOOLS,
                        "[bg-analysis] done gen=" + generation + " stillCurrent=" + stillCurrent + " replyChars="
                            + replyLen + " file=" + request.getFile().getName(),
                        () -> generatedMessage == null ? "" : "reply: " + generatedMessage);

                    result.complete(null);
                });
            }).exceptionally(error -> {
                // Отмена (вытеснение новым сохранением) — не ошибка: завершаем тихо, в лог не пишем.
                // Настоящую ошибку прокидываем в result — её один раз залогирует вызывающий onFileSaved.
                if (token.isCanceled())
                {
                    result.complete(null);
                }
                else
                {
                    result.completeExceptionally(error);
                }
                return null;
            });

            // Hold the job open until the review actually finishes, so the Progress view shows it.
            JobFutures.await(context, result, token);
        }, false, CancellationTokens.NONE).schedule();

        return result;
    }
}
