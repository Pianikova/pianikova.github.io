/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
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

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.IAIStateListener;
import com.e1c.edt.ai.assistent.IStateService;
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
    private final ISettings settings;
    private final IStateService stateService;
    private CancellationTokenSource reviewChangesCancellationToken = new CancellationTokenSource();
    private CancellationTokenSource createCommitMessageCancellationToken = new CancellationTokenSource();

    @Inject
    public StagingViewEnhancer(IDispatcher dispatcher, IReflection reflection, IWidgets widgets, IGitActions gitActions,
        ISettings settings, IStateService stateService)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(reflection);
        Preconditions.checkNotNull(widgets);
        Preconditions.checkNotNull(gitActions);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(stateService);
        this.dispatcher = dispatcher;
        this.reflection = reflection;
        this.widgets = widgets;
        this.gitActions = gitActions;
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
            addStageListener(stagingView, isEnabled -> selfReviewAction.setEnabled(isEnabled));
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
                            var commitMessageSource =
                                gitActions.ceateGitCommitMessageSource(baseMessage, diffs, newCancellationToken);
                            commitMessageSource.subscribe(new IObserver<CommitMessage>()
                            {
                                @Override
                                public void onNext(CommitMessage commitMessage)
                                {
                                    dispatcher.dispatch(() -> {
                                        if (newCancellationToken.isCanceled())
                                        {
                                            return;
                                        }

                                        var message = commitMessage.getMessage();
                                        if (!baseMessage.isBlank())
                                        {
                                            message =
                                                baseMessage + System.lineSeparator() + System.lineSeparator() + message;
                                        }

                                        commitMessageComponent.setCommitMessage(message);
                                        commitMessageComponent.updateUI();
                                        commitMessages.clear();
                                        commitMessages.add(new CommitMessageInfo(commitMessage, newCancellationToken));
                                    });
                                }

                                @Override
                                public void onError(Throwable error)
                                {
                                    //
                                }

                                @Override
                                public void onCompleted()
                                {
                                    //
                                }
                            });
                        }
                    });

                    addStageListener(stagingView, isEnabled -> createMessageButton.setEnabled(isEnabled));

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
    private void addStageListener(StagingView stagingView, Consumer<Boolean> enabledHandler)
    {
        reflection.getField(StagingView.class, stagingView, "unstageAllAction", IAction.class)
            .ifPresent(unstageAllAction -> {
                stateService.addListener(new IAIStateListener()
                {
                    @Override
                    public void onStateChange(AIState state)
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
