/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ui.Messages;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class FixDialog
    extends TitleAreaDialog implements IFixDialog
{
    public final static String BOUNDS_STORE_KEY = "FixDialogBounds"; //$NON-NLS-1$
    private final IPreferenceStore preferenceStore;
    private final IJson json;
    private Text detailsText;
    private String details = Messages.FixCodeDefaultDetails;

    @Inject
    public FixDialog(IPreferenceStore preferenceStore, IJson json)
    {
        super(Display.getCurrent().getActiveShell());
        Preconditions.checkNotNull(preferenceStore);
        Preconditions.checkNotNull(json);
        this.preferenceStore = preferenceStore;
        this.json = json;
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
        setMessage(Messages.FixCodeRequestDetails);
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.AIName);
        var boundsStr = preferenceStore.getString(BOUNDS_STORE_KEY);
        if (boundsStr != null)
        {
            json.deserialize(boundsStr, org.eclipse.swt.graphics.Rectangle.class)
                .ifPresent(bounds -> shell.setBounds(bounds));
        }
    }

    @Override
    public boolean close()
    {
        preferenceStore.setValue(BOUNDS_STORE_KEY, json.serialize(getShell().getBounds()));
        return super.close();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite area = (Composite)super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 10;
        container.setLayout(layout);

        var detailsTextGrid = new GridData();
        detailsTextGrid.grabExcessHorizontalSpace = true;
        detailsTextGrid.grabExcessHorizontalSpace = true;
        detailsTextGrid.horizontalAlignment = GridData.FILL;
        detailsTextGrid.grabExcessVerticalSpace = true;
        detailsTextGrid.verticalAlignment = SWT.FILL;

        detailsText = new Text(container, SWT.BORDER | SWT.MULTI);
        detailsText.setText(details);
        detailsText.setLayoutData(detailsTextGrid);
        detailsText.setFocus();
        detailsText.selectAll();
        return area;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    private void saveData()
    {
        details = detailsText.getText();
    }

    @Override
    protected void okPressed()
    {
        saveData();
        super.okPressed();
    }

    @Override
    public String getDetails()
    {
        return details;
    }
}
