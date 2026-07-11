/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import java.util.Map;
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
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ConversationSession;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.google.inject.Inject;

/**
 * Менеджер фонового анализа изменений в коде.
 *
 * @author Bogdan Sushkov
 */
public class BackgroundAnalysisManager
{
    private static final class FileAnalysisState
    {
        final AtomicReference<Job> currentJob = new AtomicReference<>();
        final AtomicLong generation = new AtomicLong();
        // отдельный диалог на файл
        final AtomicReference<ConversationSession> conversationSession = new AtomicReference<>();
    }

    private final IConversationFacade conversationFacade;
    private final IDispatcher dispatcher;
    private final ISkillExecutor skillExecutor;
    private final ILog log;

    private final ConcurrentMap<IFile, FileAnalysisState> states = new ConcurrentHashMap<>();

    @Inject
    public BackgroundAnalysisManager(IConversationFacade conversationFacade, IDispatcher dispatcher,
        ISkillExecutor skillExecutor, ILog log)
    {
        this.conversationFacade = conversationFacade;
        this.dispatcher = dispatcher;
        this.skillExecutor = skillExecutor;
        this.log = log;
    }

    /**
     * Файл сохранён. Отменяем предыдущий анализ этого же файла, берём новое
     * поколение и запускаем self-review последних изменений по локальной истории IDE
     * (git не требуется). Single-flight внутри файла, параллельно между файлами.
     */
    @SuppressWarnings("nls")
    public void onFileSaved(IFile file)
    {
        if (!shouldAnalyze(file))
        {
            return;
        }
        FileAnalysisState state = states.computeIfAbsent(file, f -> new FileAnalysisState());
        long myGeneration = state.generation.incrementAndGet();

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
            analyzeChanges(request, state, token).exceptionally(error -> {
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
     * Запускает self-review последних изменений файла (diff последней ревизии
     * локальной истории против текущего состояния). Диапазон определяет сам скилл
     * через тулы localhistory/localchanges.
     */
    @SuppressWarnings("nls")
    private CompletableFuture<Void> analyzeChanges(AutoAnalysisRequest request, FileAnalysisState state,
        ICancellationToken cancellationToken)
    {
        if (request == null)
        {
            return CompletableFuture.completedFuture(null);
        }
        log.trace(TracingSources.TOOLS, "Background self-review of file: " + request.getFile(), () -> "");

        var result = new CompletableFuture<Void>();

        dispatcher.createJob("Background AI Code Analysis", context -> {
            var token = cancellationToken;

//            var absolutePath = ;
//            var project = projectProvider.getProject(absolutePath)
//                .orElseThrow(() -> new RuntimeException("Project not found"));

            // @formatter:off
            SkillExecutionRequest skillRequest = new SkillExecutionRequest("code-review-last-changes",
                Map.of("project_name", request.getProjectId().toString(),
                       "relative_file_path", request.getFile().getProjectRelativePath().toString(),
                       "absolute_file_path", request.getFile().getLocation().toOSString()));
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
                    return null;
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
