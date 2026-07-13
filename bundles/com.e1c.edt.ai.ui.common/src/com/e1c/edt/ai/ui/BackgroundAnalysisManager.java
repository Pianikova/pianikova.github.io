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
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IConversationFacade;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ConversationSession;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.e1c.edt.ai.assistent.model.ProjectId;
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
    // Local history for the just-saved change is written as part of the save but can lag behind the
    // POST_CHANGE notification that triggers analysis. Retry briefly so the first save of a file with
    // no prior history still resolves a diff base instead of falling back to an empty "latest".
    private static final int BASE_RESOLVE_RETRIES = 5;
    private static final long BASE_RESOLVE_RETRY_DELAY_MS = 100;

    private static final class FileAnalysisState
    {
        final AtomicReference<Job> currentJob = new AtomicReference<>();
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

    private final ConcurrentMap<IFile, FileAnalysisState> states = new ConcurrentHashMap<>();

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
     * Файл сохранён. Отменяем предыдущий анализ этого же файла, берём новое
     * поколение и запускаем self-review последних изменений по локальной истории IDE
     * (git не требуется). Single-flight внутри файла, параллельно между файлами.
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
        FileAnalysisState state = states.computeIfAbsent(file, f -> new FileAnalysisState());
        long myGeneration = state.generation.incrementAndGet();
        log.trace(TracingSources.TOOLS,
            "[bg-analysis] scheduling gen=" + myGeneration + " file=" + file.getFullPath(), () -> "");

        Job previousJob = state.currentJob.get();
        if (previousJob != null)
        {
            previousJob.cancel();
        }

        ProjectId projectId = new ProjectId(file.getProject());
        AutoAnalysisRequest request = new AutoAnalysisRequest(file, projectId);

        Job job = dispatcher.createJob("Background Analysis", context -> {
            ICancellationToken token = context.CancellationTokenSource;
            if (token.isCanceled() || state.generation.get() != myGeneration)
            {
                return; // вытеснены до старта
            }
            analyzeChanges(request, state, myGeneration, token).exceptionally(error -> {
                log.logError(error);
                return null;
            });
        }, false, CancellationTokens.NONE);

        state.currentJob.set(job);
        job.schedule();
    }

    /**
     * Файл закрыт — убираем состояние (в т.ч. историю чата по файлу) и гасим job.
     * Если события закрытия нет — можно не звать, мапа маленькая.
     */
    public void onFileClosed(IFile file)
    {
        FileAnalysisState state = states.remove(file);
        if (state != null)
        {
            Job job = state.currentJob.get();
            if (job != null)
            {
                job.cancel();
            }
        }
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

        dispatcher.createJob("Background AI Code Analysis", context -> {
            var token = cancellationToken;

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

            // @formatter:off
            SkillExecutionRequest skillRequest = new SkillExecutionRequest("code-review-last-changes",
                Map.of("project_name", request.getProjectId().toString(),
                       "relative_file_path", request.getFile().getProjectRelativePath().toString(),
                       "absolute_file_path", request.getFile().getLocation().toOSString(),
                       "from_revision_id", baseRevisionId));
            // @formatter:on

            skillExecutor.executeAsync(skillRequest, token).handle((response, exception) -> {
                if (exception != null)
                {
                    log.logError(exception);
                    result.completeExceptionally(exception);
                    return null;
                }
                if (token.isCanceled())
                {
                    result.completeExceptionally(new RuntimeException("Analysis was cancelled"));
                    return null;
                }
                return response;
            }).thenCompose(skillResponse -> {
                if (skillResponse == null)
                {
                    result.completeExceptionally(new RuntimeException("Skill execution returned null"));
                    return CompletableFuture.completedFuture(null);
                }
                if (token.isCanceled())
                {
                    result.completeExceptionally(new RuntimeException("Analysis was cancelled before chat"));
                    return CompletableFuture.<Void> completedFuture(null);
                }

                ConversationSession session = state.conversationSession.get();
                boolean forceNew = session == null;
                var newReq = new SendUserMessageRequest(request.getProjectId(), skillResponse.getPrompt(),
                    session, forceNew);

                return conversationFacade.sendAsync(newReq, token).thenAccept(resultMessage -> {
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
                    // Если нас уже вытеснило новое поколение, база остаётся закреплённой за ним.
                    boolean stillCurrent = state.generation.get() == generation;
                    if (stillCurrent)
                    {
                        state.pendingBaseRevisionId.set(null);
                    }
                    var replyLen = generatedMessage == null ? 0 : generatedMessage.length();
                    log.trace(TracingSources.TOOLS, "[bg-analysis] done gen=" + generation + " stillCurrent="
                        + stillCurrent + " replyChars=" + replyLen + " file=" + request.getFile().getName(), () -> "");

                    result.complete(null);
                });
            }).exceptionally(error -> {
                log.logError(error);
                result.completeExceptionally(error);
                return null;
            });
        }, false, CancellationTokens.NONE).schedule();

        return result;
    }
}
