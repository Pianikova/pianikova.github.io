/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ServerAccessType;
import org.e1c.edt.ai.assistent.IServerAccessService;
import org.e1c.edt.ai.assistent.ServerAccessListener;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.google.inject.Inject;

/**
 *
 * @author Bogdan Sushkov
 *
 */
public class StatusBarControl
    extends WorkbenchWindowControlContribution
{
    private Image image;
    private ImageDescriptor imageDesc;
    private Label iconLabel;
    @Inject
    private IServerAccessService serverAccess;
    @Inject
    private IDispatcher dispatcher;

    @Override
    protected Control createControl(Composite parent)
    {
        Activator.injectMembers(this);
        var version = Activator.getDefault().getPluginVersion().get().toString();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = 2;
        gridLayout.marginHeight = -5;
        gridLayout.marginBottom = -5;
        composite.setLayout(gridLayout);

        // Icon
        imageDesc = ImageDescriptor.createFromURL(
            FileLocator.find(Activator.getDefault().getBundle(), new Path("icons/obj16/status_not_ok.png"), null)); //$NON-NLS-1$
        iconLabel = new Label(composite, SWT.NONE);
        image = imageDesc.createImage();

        iconLabel.setImage(image);
        iconLabel.setToolTipText(version);

        composite.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                image.dispose();
            }
        });

        serverAccess.addServerAccessListener(new ServerAccessListener()
        {

            @Override
            public void onServerAccessChange(ServerAccessType currentStatus)
            {
                dispatcher.dispatchAsync(() -> changeStatus(currentStatus));
            }
        });

        // Status
        Label status = new Label(composite, SWT.NONE);
        status.setToolTipText(version);
        status.setText(Messages.AIName);
        var font = status.getFont();
        var fontData = font.getFontData()[0];
        fontData.setHeight((int)(fontData.getHeight() * .9));
        var smalFont = new Font(font.getDevice(), fontData);
        status.setFont(smalFont);

        var statusGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        status.setLayoutData(statusGridData);

        parent.getParent().setRedraw(true);
        return composite;
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    private void changeStatus(ServerAccessType status)
    {
        String path =
            status == ServerAccessType.ACCESS_PRESENT ? "icons/obj16/status_ok.png" : "icons/obj16/status_not_ok.png"; //$NON-NLS-1$ //$NON-NLS-2$
        imageDesc =
            ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(path), null));
        if (!iconLabel.isDisposed())
        {
            image.dispose();
            image = imageDesc.createImage();
            iconLabel.setImage(image);
        }
    }
}
