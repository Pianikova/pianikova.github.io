/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IConversationFacade;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class StagingViewEnhancer
    implements IViewEnhancer
{
    private static final String COMMIT_MESSAGE_SKILL = "edt"; //$NON-NLS-1$
    private static final int COMMIT_MESSAGE_MAX_TOOL_ROUNDS = 200;

    private final Optional<String> viewId = Optional.of("org.eclipse.egit.ui.StagingView"); //$NON-NLS-1$
    private final IDispatcher dispatcher;
    private final IReflection reflection;
    private final IWidgets widgets;
    private final IGitActions gitActions;
    private final IConversationFacade conversationFacade;
    private final IProjectProvider projectProvider;
    private final ISettings settings;
    private final IStateService stateService;
    private final IPreferenceStore preferenceStore;
    private final ILog log;
    private final ISkillExecutor skillExecutor;
    private final IChat chat;
    private CancellationTokenSource reviewChangesCancellationToken = new CancellationTokenSource();
    private CancellationTokenSource createCommitMessageCancellationToken = new CancellationTokenSource();
    private volatile boolean commitMessageGenerating;

    @Inject
    public StagingViewEnhancer(IDispatcher dispatcher, IReflection reflection, IWidgets widgets, IGitActions gitActions,
        IConversationFacade conversationFacade, IProjectProvider projectProvider, ILog log, ISettings settings,
        IStateService stateService, ISkillExecutor skillExecutor, IChat chat, IPreferenceStore preferenceStore)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(reflection);
        Preconditions.checkNotNull(widgets);
        Preconditions.checkNotNull(gitActions);
        Preconditions.checkNotNull(conversationFacade);
        Preconditions.checkNotNull(projectProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(skillExecutor);
        Preconditions.checkNotNull(chat);
        Preconditions.checkNotNull(preferenceStore);
        this.chat = chat;
        this.skillExecutor = skillExecutor;
        this.log = log;
        this.dispatcher = dispatcher;
        this.reflection = reflection;
        this.widgets = widgets;
        this.gitActions = gitActions;
        this.conversationFacade = conversationFacade;
        this.projectProvider = projectProvider;
        this.settings = settings;
        this.stateService = stateService;
        this.preferenceStore = preferenceStore;
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
                    var repository = stagingView.getCurrentRepository();
                    if (repository == null || repository.isBare())
                    {
                        log.logError("Repository is null or bare");
                        return;
                    }
                    var newCancellationToken = new CancellationTokenSource();
                    reviewChangesCancellationToken.cancel();
                    reviewChangesCancellationToken = newCancellationToken;
                    performSelfReview(repository, newCancellationToken);
                }
            };

            selfReviewAction.setEnabled(false);
            stagedToolBarManager.add(selfReviewAction);
            stagedToolBarManager.update(true);
            addStageListener(stagingView, stagedViewer.getControl(),
                isEnabled -> dispatcher.dispatchAsync(() -> selfReviewAction.setEnabled(isEnabled)));
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
                    Consumer<Boolean> enabledHandler = isEnabled -> dispatcher.dispatchAsync(() -> {
                        if (createMessageButton.isDisposed())
                        {
                            return;
                        }
                        if (commitMessageGenerating && isEnabled)
                        {
                            return;
                        }
                        createMessageButton.setEnabled(isEnabled);
                    });
                    var refreshCreateMessageEnabled = addStageListener(stagingView, createMessageButton,
                        enabledHandler);
                    createMessageButton.addSelectionListener(new SelectionAdapter()
                    {
                        @Override
                        public void widgetSelected(SelectionEvent e)
                        {
                            commitMessages.clear();
                            var repository = stagingView.getCurrentRepository();
                            if (repository == null || repository.isBare())
                            {
                                log.logError("Repository is null or bare");
                                return;
                            }
                            var newCancellationToken = new CancellationTokenSource();
                            createCommitMessageCancellationToken.cancel();
                            createCommitMessageCancellationToken = newCancellationToken;
                            var baseMessage = commitMessageComponent.getCommitMessage().trim();
                            commitMessageGenerating = true;
                            createMessageButton.setEnabled(false);
                            createCommitMessageUsingSkills(baseMessage, newCancellationToken, repository,
                                commitMessageComponent, commitMessages, refreshCreateMessageEnabled);
                        }
                    });

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
    private void performSelfReview(Repository repository, CancellationTokenSource newCancellationToken)
    {
        var workingDirectory = repository.getWorkTree().getAbsolutePath();
        final var project = projectProvider.getProject(workingDirectory);
        if (project.isEmpty())
        {
            log.logError("Cannot determine project for Git repository " + workingDirectory); //$NON-NLS-1$
            return;
        }

        SkillExecutionRequest skillRequest =
            new SkillExecutionRequest("git-review", Map.of("working_directory", workingDirectory));


        skillExecutor.executeAsync(skillRequest, newCancellationToken).thenAccept(result -> {
            var prompt = result.getPrompt();
            if (prompt != null)
            {
                var ctx = new AIContext(project.get(), "", (IDocument)null); //$NON-NLS-1$
                dispatcher.dispatchAsync(() -> chat.reviewCodeWithDetails(ctx, null, prompt));
            }
            else
            {
                log.logError("Git review skill returned null prompt");
            }
        }).exceptionally(error -> {
            log.logError(error);
            return null;
        });
    }

    @SuppressWarnings("nls")
    private void createCommitMessageUsingSkills(String baseMessage, ICancellationToken cancellationToken,
        Repository repository, CommitMessageComponent commitMessageComponent,
        ArrayList<CommitMessageInfo> commitMessages,
        Runnable refreshCreateMessageEnabled)
    {
        Runnable finishGeneration = () -> dispatcher.dispatch(() -> {
            commitMessageGenerating = false;
            refreshCreateMessageEnabled.run();
        });

        var job = dispatcher.createJob(Messages.CommitMessageJobName, jobCtx -> {
            // The bar appears only after beginTask, and UNKNOWN because the number of tool rounds is
            // decided by the model; the reporter then keeps overwriting this task name.
            jobCtx.Monitor.beginTask(Messages.ConversationProgressStarting, IProgressMonitor.UNKNOWN);
            var progressReporter = new ConversationProgressReporter(jobCtx.Monitor);
            var workingDirectory = repository.getWorkTree().getAbsolutePath();

            final var project = projectProvider.getProject(workingDirectory);
            if (project.isEmpty())
            {
                log.logError("Cannot determine project for Git repository " + workingDirectory); //$NON-NLS-1$
                finishGeneration.run();
                return;
            }


            // @formatter:off
            SkillExecutionRequest skillRequest = new SkillExecutionRequest("git-commit",
                Map.of("user_text", baseMessage,
                       "working_directory", workingDirectory,
                       "max_commit_count", String.valueOf(5)));
            // @formatter:on

            var operation = skillExecutor.executeAsync(skillRequest, cancellationToken).handle((response, exception) -> {
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

                var request = new SendUserMessageRequest(project.get(), result.getPrompt(), null, true,
                    COMMIT_MESSAGE_SKILL, Boolean.TRUE, Integer.valueOf(COMMIT_MESSAGE_MAX_TOOL_ROUNDS),
                    result.getAllowedTools().orElse(null));

                return conversationFacade.sendAsync(request, cancellationToken, progressReporter);
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

                // The answer goes into the commit message field as is, and the skill's ban on a
                // preamble or code fence is a prompt rule, not a guarantee — enforce it here.
                var messageText = CommitMessageSanitizer.sanitize(generatedMessage);

                var commitMessage =
                    new CommitMessage(project.get(), resultMessage.getSession().getReplyToMessageUuid(), messageText);
                dispatcher.dispatch(() -> {
                    if (cancellationToken.isCanceled())
                    {
                        return;
                    }
                    commitMessageComponent.setCommitMessage(messageText);
                    commitMessageComponent.updateUI();
                    commitMessages.clear();
                    commitMessages.add(new CommitMessageInfo(commitMessage, cancellationToken));
                });
            }).exceptionally(error -> {
                log.logError(error);
                return null;
            }).whenComplete((r, t) -> finishGeneration.run());

            // Hold the job open for the whole generation, otherwise the progress UI disappears at once.
            JobFutures.await(jobCtx, operation, cancellationToken);
        }, false, cancellationToken);
        // Generation is triggered by the user and is short — it must not queue behind long jobs.
        job.setPriority(Job.INTERACTIVE);
        // Cancel ends the job while the canceled request may still be unwinding, and the button is
        // re-enabled by finishGeneration at the end of the chain — so also release it on the job
        // itself, or a canceled generation would leave the button disabled.
        job.addJobChangeListener(new JobChangeAdapter()
        {
            @Override
            public void done(IJobChangeEvent event)
            {
                finishGeneration.run();
            }
        });
        job.schedule();
    }

    @SuppressWarnings("nls")
    private Runnable addStageListener(StagingView stagingView, Widget disposeTarget, Consumer<Boolean> enabledHandler)
    {
        var unstageAllAction =
            reflection.getField(StagingView.class, stagingView, "unstageAllAction", IAction.class).orElse(null);

        if (unstageAllAction == null)
        {
            return () -> enabledHandler.accept(settings.isEnabled());
        }

        Runnable refresh = () -> enabledHandler.accept(settings.isEnabled() && unstageAllAction.isEnabled());

        var stateListener = new IStateListener()
        {
            @Override
            public void onServiceStateChange(ServiceState serviceState)
            {
                refresh.run();
            }

            @Override
            public void onActionStateChange(ActionState actionState)
            {
                refresh.run();
            }
        };
        stateService.addListener(stateListener);

        IPropertyChangeListener settingsListener = event -> {
            if (ISettingsStore.CODE_COMPLETION_POLICY.equals(event.getProperty())
                || ISettingsStore.CLIENT_TOKEN.equals(event.getProperty()))
            {
                refresh.run();
            }
        };
        preferenceStore.addPropertyChangeListener(settingsListener);

        IPropertyChangeListener actionListener = event -> {
            if (event.getProperty().equals("enabled"))
            {
                refresh.run();
            }
        };
        unstageAllAction.addPropertyChangeListener(actionListener);

        disposeTarget.addDisposeListener(e -> {
            stateService.removeListener(stateListener);
            preferenceStore.removePropertyChangeListener(settingsListener);
            unstageAllAction.removePropertyChangeListener(actionListener);
        });

        refresh.run();
        return refresh;
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
