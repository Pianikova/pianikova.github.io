/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.staging.StagingEntry;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IConversationFacade;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class StagingViewEnhancer
    implements IViewEnhancer
{
    private final Optional<String> viewId = Optional.of("org.eclipse.egit.ui.StagingView"); //$NON-NLS-1$
    private final IDispatcher dispatcher;
    private final IReflection reflection;
    private final IWidgets widgets;
    private final IGitActions gitActions;
    private final IConversationFacade conversationFacade;
    private final IProjectIdProvider projectIdProvider;
    private final ISettings settings;
    private final IStateService stateService;
    private final ILog log;
    private final ISkillExecutor skillExecutor;
    private CancellationTokenSource reviewChangesCancellationToken = new CancellationTokenSource();
    private CancellationTokenSource createCommitMessageCancellationToken = new CancellationTokenSource();

    @Inject
    public StagingViewEnhancer(IDispatcher dispatcher, IReflection reflection, IWidgets widgets, IGitActions gitActions,
        IConversationFacade conversationFacade, IProjectIdProvider projectIdProvider, ILog log, ISettings settings,
        IStateService stateService,
        ISkillExecutor skillExecutor)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(reflection);
        Preconditions.checkNotNull(widgets);
        Preconditions.checkNotNull(gitActions);
        Preconditions.checkNotNull(conversationFacade);
        Preconditions.checkNotNull(projectIdProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(skillExecutor);
        this.skillExecutor = skillExecutor;
        this.log = log;
        this.dispatcher = dispatcher;
        this.reflection = reflection;
        this.widgets = widgets;
        this.gitActions = gitActions;
        this.conversationFacade = conversationFacade;
        this.projectIdProvider = projectIdProvider;
        this.settings = settings;
        this.stateService = stateService;
    }

    @Override
    public Optional<String> getViewId()
    {
        return viewId;
    }

    @Override
    @SuppressWarnings({ "nls" })
    public void setup(IWorkbenchPart view)
    {
        if (!(view instanceof StagingView))
        {
            return;
        }

        var stagingView = (StagingView)view;
        var stagedViewer =
            reflection.getField(StagingView.class, stagingView, "stagedViewer", TreeViewer.class).orElse(null);

        if (stagedViewer == null)
        {
            return;
        }

        var stagedToolBarManager =
            reflection.getField(StagingView.class, stagingView, "stagedToolBarManager", ToolBarManager.class)
                .orElse(null);

        if (stagedToolBarManager != null)
        {
            var selfReviewAction = new Action(Messages.GitReview, BaseActivator.getImageDescriptor(Images.GIT_REVIEW))
            {
                @Override
                public void run()
                {
                    var newCancellationToken = new CancellationTokenSource();
                    reviewChangesCancellationToken.cancel();
                    reviewChangesCancellationToken = newCancellationToken;
                    var stagingEntries = getStagingEntries(stagedViewer.getTree());
                    var diffs = getDiffs(stagingEntries);
                    gitActions.reviewGitChanges(diffs, reviewChangesCancellationToken);
                }
            };

            selfReviewAction.setEnabled(false);
            stagedToolBarManager.add(selfReviewAction);
            stagedToolBarManager.update(true);
            addStageListener(stagingView,
                isEnabled -> dispatcher.dispatch(() -> selfReviewAction.setEnabled(isEnabled)));
        }

        var commitMessageSection =
            reflection.getField(StagingView.class, stagingView, "commitMessageSection", Section.class).orElse(null);

        if (commitMessageSection != null)
        {
            var commitMessageToolBar = widgets.getChildren(commitMessageSection)
                .map(control -> ToolBar.class.isAssignableFrom(control.getClass()) ? ToolBar.class.cast(control) : null)
                .filter(i -> i != null)
                .findFirst()
                .orElse(null);

            if (commitMessageToolBar != null)
            {
                var commitMessageComponent = reflection
                    .getField(StagingView.class, stagingView, "commitMessageComponent", CommitMessageComponent.class)
                    .orElse(null);

                if (commitMessageComponent != null)
                {
                    var commitMessages = new ArrayList<CommitMessageInfo>();
                    var createMessageButton = new ToolItem(commitMessageToolBar, SWT.BUTTON1);
                    createMessageButton.setImage(BaseActivator.getImage(Images.GIT_MESSAGE));
                    createMessageButton.setToolTipText(Messages.CommitMessage);
                    createMessageButton.addSelectionListener(new SelectionAdapter()
                    {
                        @Override
                        public void widgetSelected(SelectionEvent e)
                        {
                            commitMessages.clear();
                            var stagingEntries = getStagingEntries(stagedViewer.getTree());
                            var diffs = getDiffs(stagingEntries);
                            var newCancellationToken = new CancellationTokenSource();
                            createCommitMessageCancellationToken.cancel();
                            createCommitMessageCancellationToken = newCancellationToken;
                            var baseMessage = commitMessageComponent.getCommitMessage().trim();
                            createCommitMessageUsingSkills(baseMessage, diffs, newCancellationToken,
                                commitMessageComponent, commitMessages);
                        }
                    });

                    addStageListener(stagingView,
                        isEnabled -> dispatcher.dispatch(() -> createMessageButton.setEnabled(isEnabled)));

                    reflection.getField(StagingView.class, stagingView, "commitButton", Button.class)
                        .ifPresent(button -> {
                            button.addSelectionListener(new SelectionAdapter()
                            {
                                @Override
                                public void widgetSelected(SelectionEvent e)
                                {
                                    commit(commitMessages, commitMessageComponent.getCommitMessage());
                                }
                            });
                        });

                    reflection.getField(StagingView.class, stagingView, "commitAndPushButton", Button.class)
                        .ifPresent(button -> {
                            button.addSelectionListener(new SelectionAdapter()
                            {
                                @Override
                                public void widgetSelected(SelectionEvent e)
                                {
                                    commit(commitMessages, commitMessageComponent.getCommitMessage());
                                }
                            });
                        });
                }
            }
        }
    }

    @SuppressWarnings("nls")
    private void createCommitMessageUsingSkills(String baseMessage, List<GitDiff> diffs,
        ICancellationToken cancellationToken, CommitMessageComponent commitMessageComponent,
        ArrayList<CommitMessageInfo> commitMessages)
    {
        var job = dispatcher.createJob(Messages.BackgroundJobName, jobCtx -> {
            if (diffs.isEmpty())
                {
                    return;
                }

            var repository = diffs.get(0).getRepository();
            var workingDirectory = repository.getWorkTree().getAbsolutePath();

            var optionalProjectId = diffs.stream()
                .flatMap(diff -> diff.getPaths().stream())
                .map(path -> projectIdProvider.getProjectId(path, cancellationToken))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

            var projectId = optionalProjectId.orElse(ProjectId.Default);

            // @formatter:off
            SkillExecutionRequest skillRequest = new SkillExecutionRequest("git-commit",
                Map.of("user_text", baseMessage,
                       "working_directory", workingDirectory,
                       "max_commit_count", String.valueOf(5)));
            // @formatter:on

            skillExecutor.executeAsync(skillRequest, cancellationToken).handle((response, exception) -> {
                if (exception != null)
                {
                    log.logError(exception);
                }
                return response;
            }).thenCompose(result -> {
                if (result == null)
                {
                    return CompletableFuture.completedFuture(null);
                }

                var request = new SendUserMessageRequest(projectId, result.getPrompt(), null, true);

                return conversationFacade.sendAsync(request, cancellationToken);
            }).thenAccept(resultMessage -> {
                if (resultMessage == null || cancellationToken.isCanceled())
                    {
                        return;
                    }

                var generatedMessage = resultMessage.getText();

                if (generatedMessage == null || generatedMessage.isBlank())
                {
                    log.logError("Generated commit message is null or empty");
                    return;
                }

                var commitMessage =
                    new CommitMessage(projectId, resultMessage.getSession().getReplyToMessageUuid(), generatedMessage);
                dispatcher.dispatch(() -> {
                    if (cancellationToken.isCanceled())
                    {
                        return;
                    }
                    commitMessageComponent.setCommitMessage(generatedMessage);
                    commitMessageComponent.updateUI();
                    commitMessages.clear();
                    commitMessages.add(new CommitMessageInfo(commitMessage, cancellationToken));
                });
            }).exceptionally(error -> {
                log.logError(error);
                return null;
                });
        }, false, cancellationToken);
        job.schedule();
    }

    @SuppressWarnings("nls")
    private void addStageListener(StagingView stagingView, Consumer<Boolean> enabledHandler)
    {
        reflection.getField(StagingView.class, stagingView, "unstageAllAction", IAction.class)
            .ifPresent(unstageAllAction -> {
                stateService.addListener(new IStateListener()
                {
                    @Override
                    public void onServiceStateChange(ServiceState serviceState)
                    {
                        enabledHandler.accept(settings.isEnabled() && unstageAllAction.isEnabled());
                    }

                    @Override
                    public void onActionStateChange(ActionState actionState)
                    {
                        enabledHandler.accept(settings.isEnabled() && unstageAllAction.isEnabled());
                    }
                });

                enabledHandler.accept(unstageAllAction.isEnabled());
                unstageAllAction.addPropertyChangeListener(new IPropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent event)
                    {
                        if (!settings.isEnabled())
                        {
                            enabledHandler.accept(false);
                            return;
                        }

                        if (event.getProperty().equals("enabled"))
                        {
                            var newVal = event.getNewValue();
                            if (newVal instanceof Boolean)
                            {
                                enabledHandler.accept((Boolean)newVal);
                            }
                        }
                    }
                });
            });
    }

    private void commit(ArrayList<CommitMessageInfo> commitMessages, String finalText)
    {
        if (commitMessages.isEmpty())
        {
            return;
        }

        var messageInfo = commitMessages.get(0);
        gitActions.feedbackAsync(messageInfo.message, finalText, messageInfo.cancellationToken);
    }

    private List<StagingEntry> getStagingEntries(Tree tree)
    {
        var stagingEntries = new ArrayList<StagingEntry>();
        var stack = new Stack<TreeItem>();
        for (var child : tree.getItems())
        {
            stack.push(child);
        }

        while (!stack.isEmpty())
        {
            var item = stack.pop();
            var data = item.getData();
            if (data instanceof StagingEntry)
            {
                stagingEntries.add((StagingEntry)data);
            }

            for (var child : item.getItems())
            {
                stack.push(child);
            }
        }

        return stagingEntries;
    }

    private List<GitDiff> getDiffs(List<StagingEntry> stagingEntries)
    {
        return stagingEntries.stream()
            .collect(Collectors.groupingBy(StagingEntry::getRepository))
            .entrySet()
            .stream()
            .map(i -> new GitDiff(i.getKey(), i.getValue().stream().map(j -> j.getPath()).collect(Collectors.toList())))
            .collect(Collectors.toList());
    }

    private static class CommitMessageInfo
    {
        private final CommitMessage message;
        private final ICancellationToken cancellationToken;

        public CommitMessageInfo(CommitMessage message, ICancellationToken cancellationToken)
        {
            Preconditions.checkNotNull(message);
            Preconditions.checkNotNull(cancellationToken);
            this.message = message;
            this.cancellationToken = cancellationToken;
        }
    }
}
