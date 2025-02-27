/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIState;
import org.e1c.edt.ai.IVersionProvider;
import org.e1c.edt.ai.assistent.IAIStateListener;
import org.e1c.edt.ai.assistent.IStateService;
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
public class BaseStatusBarControl
    extends WorkbenchWindowControlContribution
    implements IAIStateListener, DisposeListener
{
    @Inject
    private IStateService stateService;
    @Inject
    private IDispatcher dispatcher;
    @Inject
    private IVersionProvider versionProvider;

    private final Image OFFLINE = createImage("icons/obj16/status_offline.png"); //$NON-NLS-1$
    private final Image ONLINE = createImage("icons/obj16/status_online.png"); //$NON-NLS-1$
    private final Image BUSY = createImage("icons/obj16/status_busy.png"); //$NON-NLS-1$
    private Font font;
    private Label iconLabel;

    public BaseStatusBarControl()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    protected Control createControl(Composite parent)
    {
        var composite = new Composite(parent, SWT.NONE);
        var gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = 2;
        gridLayout.marginHeight = -5;
        gridLayout.marginBottom = -5;
        composite.setLayout(gridLayout);

        // Icon
        iconLabel = new Label(composite, SWT.NONE);
        iconLabel.setImage(OFFLINE);

        // Status
        var status = new Label(composite, SWT.NONE);
        status.setText(Messages.AIName);
        var font = status.getFont();
        var fontData = font.getFontData()[0];
        fontData.setHeight((int)(fontData.getHeight() * .9));
        this.font = new Font(font.getDevice(), fontData);
        status.setFont(this.font);

        var statusGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        status.setLayoutData(statusGridData);

        parent.getParent().setRedraw(true);
        composite.addDisposeListener(this);
        stateService.addListener(this);
        return composite;
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    private static Image createImage(String path)
    {
        var descriptor = ImageDescriptor
            .createFromURL(FileLocator.find(BaseActivator.getDefault().getBundle(), new Path(path), null));
        return descriptor.createImage();
    }

    @Override
    public void widgetDisposed(DisposeEvent e)
    {
        stateService.removeListener(this);
        font.dispose();
        OFFLINE.dispose();
        ONLINE.dispose();
        BUSY.dispose();
    }

    @Override
    public void onStateChange(AIState state)
    {
        dispatcher.dispatchAsync(() -> changeState(state));
    }

    private void changeState(AIState state)
    {
        var version = versionProvider.getPluginVersion().toString();
        switch (state.getServiceState())
        {
        case ONLINE:
            iconLabel.setToolTipText(Messages.StatusOnline + System.lineSeparator() + version);
            switch (state.getActionState())
            {
            case BUSY:
                iconLabel.setImage(BUSY);
                break;

            default:
                iconLabel.setImage(ONLINE);
                break;
            }

            break;

        default:
            iconLabel.setToolTipText(Messages.StatusOffline + System.lineSeparator() + version);
            iconLabel.setImage(OFFLINE);
            break;
        }

        iconLabel.setRedraw(true);
    }
}
