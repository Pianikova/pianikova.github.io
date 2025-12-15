/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.IClientTokenValidator;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IAIStateListener;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
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
    private IUINotificationService notificationService;
    @Inject
    private ISettings settings;
    @Inject
    private ISettingsSetter settingsSetter;
    @Inject
    private IReflection reflection;
    @Inject
    private IClientTokenValidator clientTokenValidator;

    private final CodeCompletionPolicy[] policies;
    private final String[] policyNames;
    private boolean hintWasShown = false;
    private Font font;
    private Label iconLabel;
    private Label statusLabel;
    private CCombo policyCombo;
    private DefaultToolTip policyTooltip;

    public BaseStatusBarControl()
    {
        BaseActivator.injectMembers(this);
        policies = new CodeCompletionPolicy[CodeCompletionPolicy.values().length];
        policyNames = new String[CodeCompletionPolicy.values().length];
        for (var codeCompletionPolicy : CodeCompletionPolicy.values())
        {
            policies[codeCompletionPolicy.getIndex()] = codeCompletionPolicy;
            policyNames[codeCompletionPolicy.getIndex()] = codeCompletionPolicy.getName().toLowerCase();
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
        iconLabel.setImage(BaseActivator.getImage(Images.OFF));
        iconLabel.getShell();
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

        policyCombo = new CCombo(composite, SWT.READ_ONLY);
        policyCombo.setItems(policyNames);
        var policy = settings.getCodeCompletionPolicy();
        policyCombo.select(policy.getIndex());
        policyCombo.addSelectionListener(this);
        policyTooltip = new DefaultToolTip(policyCombo);
        policyTooltip.setText(policy.getDescription());
        policyTooltip.setHideOnMouseDown(true);
        policyTooltip.setPopupDelay(500);
        policyTooltip.setHideDelay(5000);
        policyTooltip.activate();
        try
        {
            reflection.getField(CCombo.class, policyCombo, "list", List.class).ifPresent(list -> { //$NON-NLS-1$
                list.addMouseMoveListener(new MouseMoveListener()
                {
                    @SuppressWarnings("nls")
                    @Override
                    public void mouseMove(MouseEvent e)
                    {
                        int itemHeight = policyCombo.getItemHeight();
                        if (itemHeight == 0)
                        {
                            return;
                        }

                        Integer index = (e.y - policyCombo.getBounds().y) / itemHeight;
                        if (index < 0 || index >= policies.length)
                        {
                            return;
                        }

                        policyCombo.select(index);
                        list.redraw();
                        if (index.equals(policyTooltip.getData("index")))
                        {
                            return;
                        }

                        var codeCompletionPolicy = policies[index];
                        policyTooltip.setText(codeCompletionPolicy.getDescription());
                        policyTooltip.setData("index", index);
                        var comboBounds = policyCombo.getBounds();
                        var listBounds = list.getBounds();
                        policyTooltip.show(new Point(0, -(comboBounds.height + listBounds.height + 90)));
                    }
                });

                list.getParent().addListener(SWT.Hide, new Listener()
                {
                    @Override
                    public void handleEvent(Event event)
                    {
                        policyTooltip.hide();
                    }
                });
            });
        }
        catch (Exception e)
        {
            //
        }

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

    @Override
    public void widgetDisposed(DisposeEvent e)
    {
        stateService.removeListener(this);
        font.dispose();
    }

    @Override
    public void onStateChange(AIState state)
    {
        dispatcher.dispatch(() -> changeState(state));
    }

    @SuppressWarnings("incomplete-switch")
    private void changeState(AIState state)
    {
        var policy = settings.getCodeCompletionPolicy();
        var info = versionProvider.getPluginVersion().toString();
        var serviceState = state.getServiceState();
        if (serviceState == ServiceState.SETTINGS_CHANGED || serviceState == ServiceState.ONLINE)
        {
            hintWasShown = false;
        }

        if (settings.isEnabled())
        {
            switch (serviceState)
            {
            case TOKEN_FAILED:
                if (!hintWasShown)
                {
                    if (clientTokenValidator.isValid(settings.getClientToken()))
                    {
                        notificationService.createNotification(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.StatusTokenFailed,
                            Messages.Support, "https://code.1c.ai/troubleshooting/#issue_missing_token", //$NON-NLS-1$
                            UINotificationType.ERROR);
                    }
                    else
                    {
                        settingsSetter.setCodeCompletionPolicy(CodeCompletionPolicy.OFF);
                        notificationService.createNotification(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.NotActivated,
                            Messages.Activation, "https://code.1c.ai/", //$NON-NLS-1$
                            UINotificationType.INFO);
                    }

                    hintWasShown = true;
                }
                break;

            case SSL_ERROR:
                if (!hintWasShown)
                {
                    notificationService.createNotification(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.StatusSSLFailed,
                        Messages.Support, "https://code.1c.ai/troubleshooting/#issue_ssl_error", //$NON-NLS-1$
                        UINotificationType.ERROR);
                    hintWasShown = true;
                }
                break;
            }
        }

        policy = settings.getCodeCompletionPolicy();
        if (policy == CodeCompletionPolicy.OFF)
        {
            iconLabel.setImage(BaseActivator.getImage(Images.OFF));
        }
        else
        {
            switch (state.getServiceState())
            {
            case ONLINE:
                hintWasShown = false;
                info = info + ' ' + Messages.StatusOnline;
                switch (state.getActionState())
                {
                case BUSY:
                    iconLabel.setImage(BaseActivator.getImage(Images.BUSY));
                    break;

                default:
                    iconLabel.setImage(BaseActivator.getImage(Images.ONLINE));
                    break;
                }
                break;

            case SETTINGS_CHANGED:
                iconLabel.setImage(BaseActivator.getImage(Images.OFF));
                break;

            default:
                iconLabel.setImage(BaseActivator.getImage(Images.OFFLINE));
                break;
            }
        }

        iconLabel.setToolTipText(info);
        statusLabel.setToolTipText(info);
        policyCombo.select(policy.getIndex());
        policyTooltip.setText(policy.getDescription());
        iconLabel.setRedraw(true);
    }

    @Override
    public void widgetSelected(SelectionEvent e)
    {
        policyTooltip.hide();
        var index = policyCombo.getSelectionIndex();
        if (index < 0 && index >= policies.length)
        {
            return;
        }

        var codeCompletionPolicy = policies[index];
        settingsSetter.setCodeCompletionPolicy(codeCompletionPolicy);
        policyTooltip.setText(codeCompletionPolicy.getDescription());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e)
    {
        //
    }
}
