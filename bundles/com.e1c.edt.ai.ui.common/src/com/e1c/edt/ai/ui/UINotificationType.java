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
    INFO(Images.INFO),
    WARNING(Images.WARNING),
    ERROR(Images.ERROR);

    private final String imageId;

    private UINotificationType(String imageId)
    {
        this.imageId = imageId;
    }

    public String getImageId()
    {
        return imageId;
    }

}
