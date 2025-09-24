/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.lib.Repository;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.assistent.ITools;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ToolFeedbackFinalTextRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequestContent;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class GitActions implements IGitActions
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IProjectIdProvider projectIdProvider;
    private final ISettings settings;
    private final IGitTools gitTools;
    private final ITools tools;
    private final IResourceProvider resourceProvider;
    private final IChat chat;
    private Job currentJob;

    @Inject
    public GitActions(ILog log, IDispatcher dispatcher, IProjectIdProvider projectIdProvider, ISettings settings,
        IGitTools gitTools, ITools tools, IResourceProvider resourceProvider,
        IChat chat)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectIdProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(gitTools);
        Preconditions.checkNotNull(tools);
        Preconditions.checkNotNull(resourceProvider);
        Preconditions.checkNotNull(chat);
        this.log = log;
        this.dispatcher = dispatcher;
        this.projectIdProvider = projectIdProvider;
        this.settings = settings;
        this.gitTools = gitTools;
        this.tools = tools;
        this.resourceProvider = resourceProvider;
        this.chat = chat;
    }

    @Override
    public IObservable<CommitMessage> ceateGitCommitMessageSource(String baseCommitMessage, List<GitDiff> diffs,
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

    @Override
    public void reviewGitChanges(List<GitDiff> diffs, ICancellationToken cancellationToken)
    {
        var job = dispatcher.createJob(Messages.BackgroundJobName, jobCtx -> {
            try
            {
                var diff = getDiff(diffs, cancellationToken);
                ProjectId firstProjectId = null;
                var diffText = new StringBuilder();
                for (var diffItem : diff.entrySet())
                {
                    if (cancellationToken.isCanceled())
                    {
                        break;
                    }

                    if (firstProjectId == null)
                    {
                        firstProjectId = diffItem.getKey();
                    }
                    else
                    {
                        diffText.append(System.lineSeparator());
                    }

                    diffText.append(diffItem.getValue());
                }

                if (firstProjectId != null)
                {
                    final var projectId = firstProjectId;
                    var ctx = new AIContext(projectId, "", (IDocument)null); //$NON-NLS-1$
                    var diffStr = diffText.toString();
                    dispatcher.dispatchAsync(() -> chat.reviewCode(ctx, diffStr));

                }
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }, cancellationToken);
        runJob(job);
    }

    @Override
    public CompletableFuture<Optional<String>> feedbackAsync(CommitMessage commitMessage, String finalText,
        ICancellationToken cancellationToken)
    {
        var request = new ToolFeedbackFinalTextRequest();
        request.uuid = commitMessage.getUuid();
        request.finalText = finalText;
        return tools.feedbackAsync(commitMessage.getProjectId(), request, cancellationToken)
            .thenApplyAsync(response -> response.map(i -> i.uuid));
    }

    @SuppressWarnings("nls")
    private void getGitCommitMessage(String baseCommitMessage, List<GitDiff> diffs,
        IObserver<CommitMessage> observer, ICancellationToken cancellationToken)
    {
        var diff = getDiff(diffs, cancellationToken);
        ProjectId firstProjectId = null;
        var diffText = new StringBuilder();
        for (var diffItem : diff.entrySet())
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            if (firstProjectId == null)
            {
                firstProjectId = diffItem.getKey();
            }
            else
            {
                diffText.append(System.lineSeparator());
            }

            diffText.append(diffItem.getValue());
        }

        if (firstProjectId == null)
        {
            observer.onCompleted();
            return;
        }

        final var projectId = firstProjectId;
        var toolInvokeRequest = new ToolInvokeRequest();
        toolInvokeRequest.toolName = "raw";
        toolInvokeRequest.uiLanguage = settings.getLanguage();
        toolInvokeRequest.programmingLanguage = "git diff";
        var content = new ToolInvokeRequestContent();
        toolInvokeRequest.content = content;
        content.instruction = resourceProvider.getTextResource(IResourceProvider.PROMTS_GIT_COMMIT)
            .orElse("")
            .replace("${language}", settings.getLanguage())
            .replace("${base_commit_message}",
                Optional.ofNullable(baseCommitMessage == null || baseCommitMessage.isBlank() ? null : baseCommitMessage)
                    .orElse("no additional lines"))
            .replace("${git_dif}", diffText.toString());

        log.debug("Prompt", () -> content.instruction);
        var message = new StringBuilder();
        var uudi = new StringBuilder();
        var invokeSource = tools.createInvokeSource(firstProjectId, toolInvokeRequest, cancellationToken);
        invokeSource.subscribe(new IObserver<ToolInvokeResponse>()
        {
            @Override
            public void onNext(ToolInvokeResponse value)
            {
                var content = value.content;
                if (value.uuid != null)
                {
                    uudi.setLength(0);
                    uudi.append(value.uuid);
                }

                if (content != null)
                {
                    var text = content.text;
                    if (text != null)
                    {
                        if (value.finished)
                        {
                            text = text.trim();
                        }
                        else
                        {
                            synchronized (message)
                            {
                                message.append(text);
                                text = message.toString().trim();
                            }
                        }

                        if (!text.isBlank())
                        {
                            observer.onNext(new CommitMessage(projectId, uudi.toString(), text));
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable error)
            {
                uudi.setLength(0);
                observer.onError(error);
            }

            @Override
            public void onCompleted()
            {
                uudi.setLength(0);
                observer.onCompleted();
            }
        });

    }

    @SuppressWarnings("nls")
    private Map<ProjectId, String> getDiff(List<GitDiff> diffs, ICancellationToken cancellationToken)
    {
        var result = new HashMap<ProjectId, String>();
        var groupsByRepo = groupChangesByRepo(diffs);
        for (var groupByRepo : groupsByRepo.entrySet())
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var optionalProjectId = getProjectId(cancellationToken, groupByRepo);
            if (optionalProjectId.isEmpty())
            {
                log.warning("Git", () -> "No project id found for diffs");
                continue;
            }

            var repository = groupByRepo.getKey();
            try (var gitDiffStream = new ByteArrayOutputStream())
            {
                var projectId = optionalProjectId.get();
                gitTools.getDiff(repository, settings.getGitDiffContextLines(projectId), gitDiffStream);
                var gitDiff = gitDiffStream.toString("UTF-8");
                result.put(projectId, gitDiff);
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }

        return result;
    }

    private Optional<ProjectId> getProjectId(ICancellationToken cancellationToken,
        Entry<Repository, List<GitDiff>> groupByRepo)
    {
        return groupByRepo.getValue()
            .stream()
            .flatMap(diff -> diff.getPaths().stream())
            .map(diff -> projectIdProvider.getProjectId(diff, cancellationToken))
            .filter(project -> project.isPresent())
            .map(project -> project.get())
            .findFirst();
    }

    private Map<Repository, List<GitDiff>> groupChangesByRepo(List<GitDiff> diffs)
    {
        return diffs.stream().collect(Collectors.groupingBy(diff -> diff.getRepository()));
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
