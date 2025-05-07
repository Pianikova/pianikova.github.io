/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.CodeCompletionPolicy;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.assistent.IAIStateListener;
import com.e1c.edt.ai.assistent.IStateService;
import com.google.inject.Inject;

/**
 *
 * @author Bogdan Sushkov
 *
 */
public class BaseStatusBarControl
    extends WorkbenchWindowControlContribution
    implements IAIStateListener, DisposeListener, SelectionListener
{
    @Inject
    private IStateService stateService;
    @Inject
    private IDispatcher dispatcher;
    @Inject
    private IVersionProvider versionProvider;
    @Inject
    private IUISettings settings;

    private final String[] policies;
    private final Image OFFLINE = createImage("icons/obj16/status_offline.png"); //$NON-NLS-1$
    private final Image ONLINE = createImage("icons/obj16/status_online.png"); //$NON-NLS-1$
    private final Image BUSY = createImage("icons/obj16/status_busy.png"); //$NON-NLS-1$
    private Font font;
    private Label iconLabel;
    private Label statusLabel;
    private Combo policyCombo;

    public BaseStatusBarControl()
    {
        BaseActivator.injectMembers(this);
        policies = new String[CodeCompletionPolicy.values().length];
        for (var codeCompletionPolicy : CodeCompletionPolicy.values())
        {
            policies[codeCompletionPolicy.getIndex()] = codeCompletionPolicy.getName().toLowerCase();
        }

    }

    @Override
    protected Control createControl(Composite parent)
    {
        var composite = new Composite(parent, SWT.NONE);
        var gridLayout = new GridLayout(3, false);
        gridLayout.marginWidth = 2;
        gridLayout.marginHeight = -5;
        gridLayout.marginBottom = -5;
        composite.setLayout(gridLayout);

        // Icon
        iconLabel = new Label(composite, SWT.NONE);
        iconLabel.setImage(OFFLINE);
        var iconGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        iconLabel.setLayoutData(iconGridData);

        // Status
        statusLabel = new Label(composite, SWT.NONE);
        statusLabel.setText(Messages.AIName);
        var font = statusLabel.getFont();
        var fontData = font.getFontData()[0];
        fontData.setHeight((int)(fontData.getHeight() * .9));
        this.font = new Font(font.getDevice(), fontData);
        statusLabel.setFont(this.font);

        var statusGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        statusLabel.setLayoutData(statusGridData);

        policyCombo = new Combo(composite, SWT.READ_ONLY);
        policyCombo.setVisible(false);
        policyCombo.setItems(policies);
        var policy = settings.getCodeCompletionPolicy();
        policyCombo.select(policy.getIndex());
        policyCombo.setToolTipText(policy.getDescription());
        policyCombo.addSelectionListener(this);

        var policyGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        policyCombo.setLayoutData(policyGridData);

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
            var onlineIninfo = version + ' ' + Messages.StatusOnline;
            iconLabel.setToolTipText(onlineIninfo);
            statusLabel.setToolTipText(onlineIninfo);
            switch (state.getActionState())
            {
            case BUSY:
                iconLabel.setImage(BUSY);
                break;

            default:
                iconLabel.setImage(ONLINE);
                break;
            }

            var policy = settings.getCodeCompletionPolicy();
            policyCombo.select(policy.getIndex());
            policyCombo.setVisible(true);
            policyCombo.setToolTipText(policy.getDescription());
            break;

        default:
            var offlineInfo = version + ' ' + Messages.StatusOffline;
            iconLabel.setToolTipText(offlineInfo);
            statusLabel.setToolTipText(offlineInfo);
            iconLabel.setImage(OFFLINE);
            policyCombo.setVisible(false);
            policyCombo.setToolTipText(""); //$NON-NLS-1$
            break;
        }

        iconLabel.setRedraw(true);
    }

    @Override
    public void widgetSelected(SelectionEvent e)
    {
        var index = policyCombo.getSelectionIndex();
        for (var codeCompletionPolicy : CodeCompletionPolicy.values())
        {
            if (codeCompletionPolicy.getIndex() == index)
            {
                settings.setCodeCompletionPolicy(codeCompletionPolicy);
                policyCombo.setToolTipText(codeCompletionPolicy.getDescription());
                break;
            }
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e)
    {
        //
    }
}
