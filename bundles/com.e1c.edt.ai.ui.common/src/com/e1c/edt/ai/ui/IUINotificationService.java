/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.widgets.Shell;

import com.e1c.edt.ai.ui.UINotificationService.UINotificationActionType;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IUINotificationService
{
    public void createNotification(Shell parentShell, String message, String linkText, String url,
        UINotificationType type);

    public void createNotification(Shell parentShell, String message, String linkText, String url,
        UINotificationType type, Runnable dontShowAgainAction);

    public void createNotificationWithAction(Shell parentShell, String message, Runnable action,
        UINotificationActionType actionType, UINotificationType type);

    public void closeNotificationIfOpen();
}

