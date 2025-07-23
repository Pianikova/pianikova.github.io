/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.ISettingsProvider;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.assistent.IConversations;
import com.e1c.edt.ai.assistent.model.ConversationAskRequest;
import com.e1c.edt.ai.assistent.model.ConversationAskResponse;
import com.e1c.edt.ai.assistent.model.ConversationRequest;
import com.e1c.edt.ai.assistent.model.ConversationRequestContent;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class GitActions implements IGitActions
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IProjectIdProvider projectIdProvider;
    private final IUISettings uiSettings;
    private final ISettingsProvider settingsProvider;
    private final IGitTools gitTools;
    private final IConversations conversations;
    private final IResourceProvider resourceProvider;
    private Job currentJob;

    @Inject
    public GitActions(ILog log, IDispatcher dispatcher, IProjectIdProvider projectIdProvider, IUISettings uiSettings,
        ISettingsProvider settingsProvider,
        IGitTools gitTools, IConversations conversations, IResourceProvider resourceProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectIdProvider);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(gitTools);
        Preconditions.checkNotNull(conversations);
        Preconditions.checkNotNull(resourceProvider);
        this.log = log;
        this.dispatcher = dispatcher;
        this.projectIdProvider = projectIdProvider;
        this.uiSettings = uiSettings;
        this.settingsProvider = settingsProvider;
        this.gitTools = gitTools;
        this.conversations = conversations;
        this.resourceProvider = resourceProvider;
    }

    @Override
    public IObservable<String> ceateGitCommitMessageSource(String baseCommitMessage, List<GitDiff> diffs,
        ICancellationToken cancellationToken)
    {
        return Observables.create(observer -> {
            var job = dispatcher.createJob(Messages.BackgroundJobName, jobCtx -> {
                try
                {
                    getGitCommitMessage(baseCommitMessage, diffs, observer, cancellationToken);
                }
                catch (Exception error)
                {
                    log.logError(error);
                    observer.onError(error);
                }
            }, cancellationToken);
            runJob(job);
            return Closeables.Empty;
        });
    }

    @SuppressWarnings("nls")
    public void getGitCommitMessage(String baseCommitMessage, List<GitDiff> diffs,
        IObserver<String> observer, ICancellationToken cancellationToken)
    {
        var groupsByRepo = diffs.stream().collect(Collectors.groupingBy(diff -> diff.getRepository()));
        for (var groupByRepo : groupsByRepo.entrySet())
        {
            var optionalProjectId = groupByRepo.getValue()
                .stream()
                .flatMap(diff -> diff.getPaths().stream())
                .map(diff -> projectIdProvider.getProjectId(diff, cancellationToken))
                .filter(project -> project.isPresent())
                .map(project -> project.get())
                .findFirst();

            if (optionalProjectId.isEmpty())
            {
                log.warning("Git", () -> "No project id found for diffs");
                continue;
            }

            var repository = groupByRepo.getKey();
            try (var gitDiffStream = new ByteArrayOutputStream())
            {
                var projectId = optionalProjectId.get();

                var conversationRequest = new ConversationRequest();
                conversationRequest.toolName = "custom";
                conversationRequest.uiLanguage = uiSettings.getLanguage();
                conversationRequest.programmingLanguage = "git diff";
                var conversationResponse =
                    conversations.createConversationAsync(projectId, conversationRequest, cancellationToken).get();
                if (conversationResponse.isEmpty())
                {
                    log.warning("Git", () -> "No conversation id found");
                    continue;
                }

                var askRequest = new ConversationAskRequest();
                var content = new ConversationRequestContent();
                askRequest.content = content;
                gitTools.getDiff(repository, settingsProvider.getSettings().getLlmParameters().gitDiffContextLines,
                    gitDiffStream);
                var gitDiff = gitDiffStream.toString("UTF-8");
                content.instruction = resourceProvider.getTextResource(IResourceProvider.PROMTS_GIT_COMMIT)
                    .orElse("")
                    .replace("${language}", uiSettings.getLanguage())
                    .replace("${base_commit_message}",
                        Optional
                            .ofNullable(
                                baseCommitMessage == null || baseCommitMessage.isBlank() ? null : baseCommitMessage)
                            .orElse("no additional lines"))
                    .replace("${git_dif}", gitDiff);

                log.debug("Prompt", () -> content.instruction);

                var askSource = conversations.createAskSource(projectId, conversationResponse.get().uuid, askRequest,
                    cancellationToken);
                askSource.subscribe(new IObserver<ConversationAskResponse>()
                {
                    @Override
                    public void onNext(ConversationAskResponse value)
                    {
                        var content = value.content;
                        if (content != null)
                        {
                            var text = content.text;
                            if (text != null)
                            {
                                text = text.trim();
                                if (!text.isBlank())
                                {
                                    observer.onNext(value.content.text.trim());
                                }
                            }
                        }

                    }

                    @Override
                    public void onError(Throwable error)
                    {
                        observer.onError(error);
                    }

                    @Override
                    public void onCompleted()
                    {
                        observer.onCompleted();
                    }
                });
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }
    }

    private synchronized void runJob(Job job)
    {
        if (currentJob != null)
        {
            currentJob.cancel();
            currentJob = null;
        }

        currentJob = job;
        currentJob.setPriority(Job.DECORATE);
        job.schedule();
    }
}
