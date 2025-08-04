/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IAIStateListener;
import com.e1c.edt.ai.assistent.IStateService;
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
    private IUISettings settings;

    private final CodeCompletionPolicy[] policies;
    private final String[] policyNames;
    private final Image OFFLINE = createImage("icons/obj16/status_offline.png"); //$NON-NLS-1$
    private final Image ONLINE = createImage("icons/obj16/status_online.png"); //$NON-NLS-1$
    private final Image BUSY = createImage("icons/obj16/status_busy.png"); //$NON-NLS-1$
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
        iconLabel.setImage(OFFLINE);
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
        policyCombo.setVisible(false);
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
            var listField = policyCombo.getClass().getDeclaredField("list"); //$NON-NLS-1$
            listField.setAccessible(true);
            var list = (List)listField.get(policyCombo);
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

    @SuppressWarnings("incomplete-switch")
    private void changeState(AIState state)
    {
        var version = versionProvider.getPluginVersion().toString();
        if (state.getServiceState() == ServiceState.ONLINE)
        {
            hintWasShown = false;
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
            policyTooltip.setText(policy.getDescription());
        }
        else
        {
            var offlineInfo = version + ' ' + Messages.StatusOffline;
            iconLabel.setToolTipText(offlineInfo);
            statusLabel.setToolTipText(offlineInfo);

            iconLabel.setImage(OFFLINE);
            policyCombo.setVisible(false);
            policyTooltip.setText(""); //$NON-NLS-1$
        }

        if (!hintWasShown)
        {
            switch (state.getServiceState())
            {
            case TOKEN_FAILED:
                Display.getDefault()
                    .asyncExec(() -> notificationService.createNotification(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.StatusTokenFailed,
                        Messages.Support, "https://code.1c.ai/troubleshooting/#issue_missing_token", //$NON-NLS-1$
                        this.getClass()));
                hintWasShown = true;
                break;

            case SSL_ERROR:
                Display.getDefault()
                    .asyncExec(() -> notificationService.createNotification(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.StatusSSLFailed,
                        Messages.Support, "https://code.1c.ai/troubleshooting/#issue_ssl_error", //$NON-NLS-1$
                        this.getClass()));
                hintWasShown = true;
                break;
            }
        }

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
        settings.setCodeCompletionPolicy(codeCompletionPolicy);
        policyTooltip.setText(codeCompletionPolicy.getDescription());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e)
    {
        //
    }
}
