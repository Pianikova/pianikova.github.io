/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.inject.Inject;

public class EndpointDialog
    extends TitleAreaDialog
    implements IEndpointDialog
{
    public final static String BOUNDS_STORE_KEY = "EndpointDialogBounds"; //$NON-NLS-1$

    private Text portText;
    private int port = 9000;

    @Inject
    public EndpointDialog()
    {
        super(Display.getCurrent().getActiveShell());
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
        setMessage("Please configure the endpoint"); //$NON-NLS-1$
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText("Semantic Endpoint"); //$NON-NLS-1$
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

        // Port
        var portLabel = new Label(container, SWT.NONE);
        portLabel.setText("Port"); //$NON-NLS-1$

        var portTextGrid = new GridData();
        portTextGrid.grabExcessHorizontalSpace = true;
        portTextGrid.horizontalAlignment = GridData.FILL;

        portText = new Text(container, SWT.BORDER);
        portText.setText(Integer.toString(port));
        portText.setLayoutData(portTextGrid);
        portText.setFocus();
        portText.selectAll();
        return area;
    }

    @Override
    protected Point getInitialSize()
    {
        return new Point(300, 180);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    private void saveData()
    {
        try {
            port = Integer.parseInt(portText.getText());
        }
        catch (NumberFormatException e)
        {
            //
        }
    }

    @Override
    protected void okPressed()
    {
        saveData();
        super.okPressed();
    }

    @Override
    public int getPort()
    {
        return port;
    }
}
