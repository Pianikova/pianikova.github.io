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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.lib.Repository;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.ITools;
import com.e1c.edt.ai.assistent.model.ToolFeedbackFinalTextRequest;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class GitActions implements IGitActions
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IProjectProvider projectProvider;
    private final ISettings settings;
    private final IGitTools gitTools;
    private final ITools tools;
    private final IChat chat;
    private Job currentJob;

    @Inject
    public GitActions(ILog log, IDispatcher dispatcher, IProjectProvider projectProvider, ISettings settings,
        IGitTools gitTools, ITools tools, IChat chat)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(gitTools);
        Preconditions.checkNotNull(tools);
        Preconditions.checkNotNull(chat);
        this.log = log;
        this.dispatcher = dispatcher;
        this.projectProvider = projectProvider;
        this.settings = settings;
        this.gitTools = gitTools;
        this.tools = tools;
        this.chat = chat;
    }

    @Override
    public void reviewGitChanges(List<GitDiff> diffs, ICancellationToken cancellationToken)
    {
        var job = dispatcher.createJob(Messages.BackgroundJobName, jobCtx -> {
            try
            {
                var diff = getDiff(diffs, cancellationToken);
                IProject firstProject = null;
                var diffText = new StringBuilder();
                for (var diffItem : diff.entrySet())
                {
                    if (cancellationToken.isCanceled())
                    {
                        break;
                    }

                    if (firstProject == null)
                    {
                        firstProject = diffItem.getKey();
                    }
                    else
                    {
                        diffText.append(System.lineSeparator());
                    }

                    diffText.append(diffItem.getValue());
                }

                if (firstProject != null)
                {
                    final var project = firstProject;
                    var ctx = new AIContext(project, "", (IDocument)null); //$NON-NLS-1$
                    var diffStr = diffText.toString();
                    dispatcher.dispatchAsync(() -> chat.reviewCode(ctx, diffStr));

                }
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }, false, cancellationToken);
        runJob(job);
    }

    @Override
    public CompletableFuture<Optional<String>> feedbackAsync(CommitMessage commitMessage, String finalText,
        ICancellationToken cancellationToken)
    {
        var request = new ToolFeedbackFinalTextRequest();
        request.uuid = commitMessage.getUuid();
        request.finalText = finalText;
        return tools.feedbackAsync(commitMessage.getProject(), request, cancellationToken)
            .thenApplyAsync(response -> response.map(i -> i.uuid));
    }

    @SuppressWarnings("nls")
    private Map<IProject, String> getDiff(List<GitDiff> diffs, ICancellationToken cancellationToken)
    {
        var result = new HashMap<IProject, String>();
        var groupsByRepo = groupChangesByRepo(diffs);
        for (var groupByRepo : groupsByRepo.entrySet())
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var optionalProject = getProject(groupByRepo);
            if (optionalProject.isEmpty())
            {
                log.warning("Git", () -> "No project id found for diffs");
                continue;
            }

            var repository = groupByRepo.getKey();
            try (var gitDiffStream = new ByteArrayOutputStream())
            {
                var project = optionalProject.get();
                gitTools.getDiff(repository, settings.getGitDiffContextLines(project), gitDiffStream);
                var gitDiff = gitDiffStream.toString("UTF-8");
                result.put(project, gitDiff);
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }

        return result;
    }

    private Optional<IProject> getProject(Entry<Repository, List<GitDiff>> groupByRepo)
    {
        return groupByRepo.getValue()
            .stream()
            .flatMap(diff -> diff.getPaths().stream())
            .map(projectProvider::getProject)
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
