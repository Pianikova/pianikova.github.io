/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.IAISettingsListener;
import com.e1c.edt.ai.assistent.ISettingsTracker;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class UINotificationService
    implements IUINotificationService, IAISettingsListener
{
    private final Set<String> shownNotifications = new HashSet<>();
    private final ILog log;
    private final ISettingsTracker settingsTracker;

    @Inject
    public UINotificationService(ISettingsTracker settingsTracker, ILog log)
    {
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(log);
        this.settingsTracker = settingsTracker;
        this.log = log;
        this.settingsTracker.addListener(this);
    }

    @Override
    public void createNotification(Shell parentShell, String message, String linkText, String url,
        UINotificationType type, Class<?> sourceClass)
    {
        String notificationKey = sourceClass.getName();
        if (!shownNotifications.contains(notificationKey))
        {
            UINotification popup = new UINotification(parentShell, message, type, linkText, url);
            popup.setBlockOnOpen(false);
            popup.open();
            shownNotifications.add(notificationKey);
            log.trace("Notification shown: " + notificationKey, () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public void createNotificationWithAction(Shell parentShell, String message, Runnable action,
        UINotificationType type, Class<?> sourceClass)
    {
        String notificationKey = sourceClass.getName();
        UINotification popup = new UINotification(parentShell, message, type, null, null, action);
        popup.setBlockOnOpen(false);
        popup.open();
        log.trace("Notification shown: " + notificationKey, () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void resetAllTriggers()
    {
        shownNotifications.clear();
        log.trace("All notification triggers reset", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void onSettingsChanged()
    {
        resetAllTriggers();
    }

}

