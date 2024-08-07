/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.assistent.model.IssueType;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class FeedbackDialog
    extends TitleAreaDialog
    implements IFeedbackDialog
{
    public final static String BOUNDS_STORE_KEY = "FeedbackDialogBounds"; //$NON-NLS-1$

    private final ISettingsStore settingsStore;
    private Button attachCodeCompletionCheckbox;
    private List issueTypeList;
    private StyledText issueDescriptionText;
    private boolean hasCodeCompletion;
    private IssueType issueType;
    private String issueDescription;

    @Inject
    public FeedbackDialog(IUI ui, ISettingsStore settingsStore)
    {
        super(ui.getShell().get());
        this.settingsStore = settingsStore;
        Preconditions.checkNotNull(settingsStore);
    }

    @Override
    public int show()
    {
        create();
        return open();
    }

    @Override
    public void create()
    {
        super.create();
        setTitle(Messages.FeedbackDialogTitle);
        setMessage(Messages.FeedbackDialogMessage, IMessageProvider.NONE);
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.FeedbackDialogBoxTitle);
        settingsStore.getValue(BOUNDS_STORE_KEY, org.eclipse.swt.graphics.Rectangle.class)
            .ifPresent(bounds -> shell.setBounds(bounds));
    }

    @Override
    public boolean close()
    {
        settingsStore.setValue(BOUNDS_STORE_KEY, getShell().getBounds());
        return super.close();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite area = (Composite)super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 10;
        container.setLayout(layout);

        // Attach last code completion
        @SuppressWarnings("unused")
        Label attachCodeCompletionLabel = new Label(container, SWT.NONE);
        GridData attachCodeCompletionGrid = new GridData();
        attachCodeCompletionGrid.grabExcessHorizontalSpace = true;
        attachCodeCompletionGrid.horizontalAlignment = GridData.FILL;
        attachCodeCompletionCheckbox = new Button(container, SWT.CHECK);
        attachCodeCompletionCheckbox.setText(Messages.FeedbackDialogRefersToCodeCompletion);
        attachCodeCompletionCheckbox.setLayoutData(attachCodeCompletionGrid);
        attachCodeCompletionCheckbox.setFocus();
        attachCodeCompletionCheckbox.setEnabled(hasCodeCompletion);

        // Type
        Label issueTypeLabel = new Label(container, SWT.NONE);
        issueTypeLabel.setText(Messages.FeedbackDialogIssueType);
        GridData issueTypeLabelGrid = new GridData();
        issueTypeLabelGrid.verticalAlignment = GridData.BEGINNING;
        issueTypeLabel.setLayoutData(issueTypeLabelGrid);
        GridData issueTypeGrid = new GridData();
        issueTypeGrid.grabExcessHorizontalSpace = true;
        issueTypeGrid.horizontalAlignment = GridData.FILL;
        issueTypeList = new List(container, SWT.BORDER | SWT.SINGLE);
        issueTypeList.setLayoutData(issueTypeGrid);
        for (var type : IssueType.values())
        {
            issueTypeList.add(type.Title, type.Index);
        }

        issueTypeList.select(IssueType.Undefined.Index);

        // Description
        Label issueDescriptionLabel = new Label(container, SWT.NONE);
        issueDescriptionLabel.setText(Messages.FeedbackDialogDescription);
        GridData issueDescriptionLabelGrid = new GridData();
        issueDescriptionLabelGrid.verticalAlignment = GridData.BEGINNING;
        issueDescriptionLabel.setLayoutData(issueDescriptionLabelGrid);
        GridData issueDescriptionGrid = new GridData();
        issueDescriptionGrid.grabExcessHorizontalSpace = true;
        issueDescriptionGrid.horizontalAlignment = GridData.FILL;
        issueDescriptionGrid.grabExcessVerticalSpace = true;
        issueDescriptionGrid.verticalAlignment = SWT.FILL;
        issueDescriptionGrid.heightHint = 100;
        issueDescriptionText = new StyledText(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        issueDescriptionText.setAlwaysShowScrollBars(false);
        issueDescriptionText.setLayoutData(issueDescriptionGrid);

        return area;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    private void saveData()
    {
        issueType = IssueType.Undefined;
        for (var type : IssueType.values())
        {
            if (issueTypeList.getSelectionIndex() == type.Index)
            {
                issueType = type;
                break;
            }
        }

        issueDescription = issueDescriptionText.getText();
    }

    @Override
    protected void okPressed()
    {
        saveData();
        super.okPressed();
    }

    @Override
    public void setHasCodeCompletion(boolean hasCodeCompletion)
    {
        this.hasCodeCompletion = hasCodeCompletion;
    }

    @Override
    public IssueType getIssueType()
    {
        return issueType;
    }

    @Override
    public String getIssueDescription()
    {
        return issueDescription;
    }
}
