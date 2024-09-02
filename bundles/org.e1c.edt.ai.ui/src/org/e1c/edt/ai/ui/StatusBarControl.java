/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
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

/**
 *
 * @author Bogdan Sushkov
 *
 */
public class StatusBarControl
    extends WorkbenchWindowControlContribution
    implements IHealthChecker
{
    private Image image;
    private ImageDescriptor imageDesc;
    private Label iconLabel;

    @Override
    protected Control createControl(Composite parent)
    {
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

        var job = new StatusUpdateJob("Server status updater", this); //$NON-NLS-1$
        job.setPriority(Job.DECORATE);
        job.schedule();

        parent.getParent().setRedraw(true);
        return composite;
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    @Override
    public void setStatus(int status)
    {
        String path = status == 200 ? "icons/obj16/status_ok.png" : "icons/obj16/status_not_ok.png"; //$NON-NLS-1$ //$NON-NLS-2$
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
