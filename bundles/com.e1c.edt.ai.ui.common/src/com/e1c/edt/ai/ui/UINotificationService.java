/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.HashMap;

import org.eclipse.swt.widgets.Shell;

/**
 * @author Bogdan Sushkov
 *
 */
public class UINotificationService
    implements IUINotificationService
{
    private HashMap<String, UINotification> lastPopups = new HashMap<>();

    @Override
    public void createNotification(Shell parentShell, String message, String linkText, String url,
        UINotificationType type)
    {
        UINotification popup = new UINotification(parentShell, message, type, linkText, url);
        popup.setBlockOnOpen(false);
        var lastPopup = lastPopups.get(message);
        if (lastPopup != null)
        {
            lastPopup.close();
        }

        lastPopups.put(message, popup);
        popup.open();
    }

    @Override
    public void createNotificationWithAction(Shell parentShell, String message, Runnable action,
        UINotificationActionType actionType,
        UINotificationType type)
    {
        UINotification popup = new UINotification(parentShell, message, type, null, null, action, actionType);
        popup.setBlockOnOpen(false);
        var lastPopup = lastPopups.get(message);
        if (lastPopup != null)
        {
            lastPopup.close();
        }

        lastPopups.put(message, popup);
        popup.open();
    }

    public static enum UINotificationActionType
    {
        UPDATE(Messages.UpdateButton),
        RELOAD(Messages.RestartButton);

        private final String buttonText;

        private UINotificationActionType(String buttonText)
        {
            this.buttonText = buttonText;
        }

        public String getActionText()
        {
            return buttonText;
        }
    }

}

