/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

/**
 * @author Bogdan Sushkov
 *
 */
public enum UINotificationType
{
    INFO("icons/obj16/info.png"), //$NON-NLS-1$
    WARNING("icons/obj16/warning.png"), //$NON-NLS-1$
    ERROR("icons/obj16/error.png"); //$NON-NLS-1$

    private final String iconPath;

    private UINotificationType(String iconPath)
    {
        this.iconPath = iconPath;
    }

    public String getIconPath()
    {
        return iconPath;
    }

}
