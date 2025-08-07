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
import com.e1c.edt.ai.assistent.ITools;
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
    private final IUISettings uiSettings;
    private final ISettingsProvider settingsProvider;
    private final IGitTools gitTools;
    private final ITools tools;
    private final IResourceProvider resourceProvider;
    private Job currentJob;

    @Inject
    public GitActions(ILog log, IDispatcher dispatcher, IProjectIdProvider projectIdProvider, IUISettings uiSettings,
        ISettingsProvider settingsProvider,
        IGitTools gitTools, ITools tools, IResourceProvider resourceProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectIdProvider);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(gitTools);
        Preconditions.checkNotNull(tools);
        Preconditions.checkNotNull(resourceProvider);
        this.log = log;
        this.dispatcher = dispatcher;
        this.projectIdProvider = projectIdProvider;
        this.uiSettings = uiSettings;
        this.settingsProvider = settingsProvider;
        this.gitTools = gitTools;
        this.tools = tools;
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

                var toolInvokeRequest = new ToolInvokeRequest();
                toolInvokeRequest.toolName = "custom";
                toolInvokeRequest.uiLanguage = uiSettings.getLanguage();
                toolInvokeRequest.programmingLanguage = "git diff";
                var content = new ToolInvokeRequestContent();
                toolInvokeRequest.content = content;
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

                var message = new StringBuilder();
                var invokeSource = tools.createInvokeSource(projectId, toolInvokeRequest, cancellationToken);
                invokeSource.subscribe(new IObserver<ToolInvokeResponse>()
                {
                    @Override
                    public void onNext(ToolInvokeResponse value)
                    {
                        var content = value.content;
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
                                    observer.onNext(text);
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
