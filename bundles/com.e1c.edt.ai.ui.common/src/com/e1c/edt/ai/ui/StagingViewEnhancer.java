/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.staging.StagingEntry;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.Section;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.IObserver;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class StagingViewEnhancer implements IStagingViewEnhancer
{
    private final IDispatcher dispatcher;
    private final IReflection reflection;
    private final IWidgets widgets;
    private final IGitActions gitActions;
    private CancellationTokenSource createCommitMessageCancellationToken = new CancellationTokenSource();

    @Inject
    public StagingViewEnhancer(IDispatcher dispatcher, IReflection reflection, IWidgets widgets, IGitActions gitActions)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(reflection);
        Preconditions.checkNotNull(widgets);
        Preconditions.checkNotNull(gitActions);
        this.dispatcher = dispatcher;
        this.reflection = reflection;
        this.widgets = widgets;
        this.gitActions = gitActions;
    }

    @Override
    @SuppressWarnings("nls")
    public String getViewId()
    {
        return "org.eclipse.egit.ui.StagingView";
    }

    @Override
    @SuppressWarnings({ "nls" })
    public void setup(StagingView stagingView)
    {
        var stagedViewer =
            reflection.getField(StagingView.class, stagingView, "stagedViewer", TreeViewer.class).orElse(null);

        if (stagedViewer == null)
        {
            return;
        }

        var commitMessageSection =
            reflection.getField(StagingView.class, stagingView, "commitMessageSection", Section.class).orElse(null);

        if (commitMessageSection == null)
        {
            return;
        }

        var commitMessageToolBar = widgets.getChildren(commitMessageSection)
            .map(control -> ToolBar.class.isAssignableFrom(control.getClass()) ? ToolBar.class.cast(control) : null)
            .filter(i -> i != null)
            .findFirst()
            .orElse(null);

        if (commitMessageToolBar == null)
        {
            return;
        }

        var commitMessageComponent =
            reflection.getField(StagingView.class, stagingView, "commitMessageComponent", CommitMessageComponent.class)
                .orElse(null);

        if (commitMessageComponent == null)
        {
            return;
        }

        var createMessageButton = new ToolItem(commitMessageToolBar, SWT.BUTTON1);
        createMessageButton.setImage(BaseActivator.getImage(Images.GENERATE_DOC_COMMENTS));
        createMessageButton.setToolTipText(Messages.CommitMessage);
        createMessageButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                var stagingEntries = getStagingEntries(stagedViewer.getTree());
                var diffs = stagingEntries.stream()
                    .collect(Collectors.groupingBy(StagingEntry::getRepository))
                    .entrySet()
                    .stream()
                    .map(i -> new GitDiff(i.getKey(),
                        i.getValue().stream().map(j -> j.getPath()).collect(Collectors.toList())))
                    .collect(Collectors.toList());

                var newCancellationToken = new CancellationTokenSource();
                createCommitMessageCancellationToken.cancel();
                createCommitMessageCancellationToken = newCancellationToken;
                var commitMessageSource = gitActions.ceateGitCommitMessageSource(
                    commitMessageComponent.getCommitMessage(), diffs, newCancellationToken);
                commitMessageSource.subscribe(new IObserver<String>()
                {
                    @Override
                    public void onNext(String value)
                    {
                        dispatcher.dispatch(() -> {
                            if (newCancellationToken.isCanceled())
                            {
                                return;
                            }

                            commitMessageComponent.setCommitMessage(value);
                            commitMessageComponent.updateUI();
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

        reflection.getField(StagingView.class, stagingView, "unstageAllAction", IAction.class)
            .ifPresent(unstageAllAction -> {
                createMessageButton.setEnabled(unstageAllAction.isEnabled());
                unstageAllAction.addPropertyChangeListener(new IPropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent event)
                    {
                        if (event.getProperty().equals("enabled"))
                        {
                            var newVal = event.getNewValue();
                            if (newVal instanceof Boolean)
                            {
                                createMessageButton.setEnabled((Boolean)newVal);
                            }
                        }
                    }
                });
            });

        commitMessageComponent.updateUI();
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
}
