/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.widgets.Shell;

/**
 * @author Bogdan Sushkov
 *
 */
public class UINotificationService
    implements IUINotificationService
{
    private UINotification lastNotification;

    @Override
    public void createNotification(Shell parentShell, String message, String linkText, String url,
        UINotificationType type)
    {
        createNotification(parentShell, message, linkText, url, type, null);
    }

    @Override
    public void createNotification(Shell parentShell, String message, String linkText, String url,
        UINotificationType type, Runnable dontShowAgainAction)
    {
        UINotification popup = new UINotification(parentShell, message, type, linkText, url, dontShowAgainAction);
        popup.setBlockOnOpen(false);
        closeNotificationIfOpen();
        lastNotification = popup;
        popup.open();
    }

    @Override
    public void createNotificationWithAction(Shell parentShell, String message, Runnable action,
        UINotificationActionType actionType, UINotificationType type)
    {
        UINotification popup =
            new UINotification(parentShell, message, type, null, null, action, actionType);
        popup.setBlockOnOpen(false);
        closeNotificationIfOpen();
        popup.open();
    }

    @Override
    public void closeNotificationIfOpen()
    {
        if (lastNotification != null)
        {
            lastNotification.close();
            lastNotification = null;
        }
    }

    public static enum UINotificationActionType
    {
        UPDATE(Messages.UpdateButton, Messages.UpdatePluginJob),
        RELOAD(Messages.RestartButton, Messages.RestartJob);

        private final String buttonText;
        private final String jobName;

        private UINotificationActionType(String buttonText, String jobName)
        {
            this.buttonText = buttonText;
            this.jobName = jobName;
        }

        public String getActionText()
        {
            return buttonText;
        }

        public String getJobName()
        {
            return jobName;
        }
    }


}

