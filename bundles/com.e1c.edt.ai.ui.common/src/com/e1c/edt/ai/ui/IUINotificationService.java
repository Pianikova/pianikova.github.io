/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.widgets.Shell;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IUINotificationService
{
    public void createNotification(Shell parentShell, String message, String linkText, String url,
        UINotificationType type,
        Class<?> sourceClass);

    public void createNotificationWithAction(Shell parentShell, String message, Runnable action,
        UINotificationType type, Class<?> sourceClass);
}

