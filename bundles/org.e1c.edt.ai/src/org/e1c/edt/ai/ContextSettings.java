/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextSettings implements IContextSettings
{
    private final IUISettings uiSettings;

    @Inject
    public ContextSettings(IUISettings uiSettings)
    {
        Preconditions.checkNotNull(uiSettings);
        this.uiSettings = uiSettings;
    }

    @Override
    public int getMaxLength()
    {
        return uiSettings.getMaxAssistantTextSize();
    }
}
