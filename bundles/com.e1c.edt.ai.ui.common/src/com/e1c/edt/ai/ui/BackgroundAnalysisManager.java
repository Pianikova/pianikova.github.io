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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.lib.Repository;

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
import com.e1c.edt.ai.tools.IJGitCommonHelper;
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
    private final IJGitCommonHelper jGitCommonHelper;

    private final ConcurrentMap<IFile, FileAnalysisState> states = new ConcurrentHashMap<>();

    @Inject
    public BackgroundAnalysisManager(IConversationFacade conversationFacade, IDispatcher dispatcher,
        ISkillExecutor skillExecutor, ILog log, IJGitCommonHelper jGitCommonHelper)
    {
        this.conversationFacade = conversationFacade;
        this.dispatcher = dispatcher;
        this.skillExecutor = skillExecutor;
        this.log = log;
        this.jGitCommonHelper = jGitCommonHelper;
    }

    /**
     * Файл сохранён. Отменяем предыдущий анализ этого же файла, берём новое
     * поколение и запускаем self-review незакоммиченного diff против HEAD.
     * Single-flight внутри файла, параллельно между файлами.
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

        // тело job в onFileSaved: precheck git
        Job job = dispatcher.createJob("Background Analysis", context -> {
            ICancellationToken token = context.CancellationTokenSource;
            if (token.isCanceled() || state.generation.get() != myGeneration)
            {
                return; // вытеснены до старта
            }
            isUnderGit(file, token).thenCompose(underGit -> {
                if (!underGit || token.isCanceled() || state.generation.get() != myGeneration)
                {
                    return CompletableFuture.<Void> completedFuture(null); // не под git — падаем
                }
                return analyzeChanges(request, state, token);
            }).exceptionally(error -> {
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
     * Гейт запуска: находится ли файл под контролем git. true — репозиторий есть и
     * файл существует под git; false — репозитория нет либо файл не под git.
     * Наружу исключений не пускает, при любой ошибке возвращает false (анализ не запускаем).
     */
    @SuppressWarnings("nls")
    private CompletableFuture<Boolean> isUnderGit(IFile file, ICancellationToken token)
    {
        return CompletableFuture.supplyAsync(() -> {
            @SuppressWarnings("resource")
            Repository repository = null;
            try
            {
                IProject project = file.getProject();
                if (project == null || !project.isAccessible())
                {
                    return false;
                }

                // Проверяем наличие git репозитория через JGit API
                String workingDir = project.getLocation().toOSString();
                repository = jGitCommonHelper.openRepository(workingDir);

                if (repository == null || repository.isBare())
                {
                    return false;
                }

                // Репозиторий найден и доступен
                return true;
            }
            catch (Exception e)
            {
                log.trace(TracingSources.TOOLS, "git precheck failed, skip analysis: " + e.getMessage(), () -> "");
                return false;
            }
            finally
            {
                if (repository != null)
                {
                    try
                    {
                        repository.close();
                    }
                    catch (Exception ignored)
                    {
                        //
                    }
                }
            }
        });
    }

    /**
     * Запускает self-review незакоммиченных изменений файла (git diff против HEAD).
     * Диапазон определяет сам скилл через git
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
            // @formatter:off
            SkillExecutionRequest skillRequest = new SkillExecutionRequest("code-review-last-changes",
                Map.of("project_name", request.getProjectId().toString(),
                       "file_path", request.getFile().getLocation().toOSString()));
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
