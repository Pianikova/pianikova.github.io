/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextSettings implements IAIContextSettings
{
    private final IUISettings uiSettings;

    @Inject
    public AIContextSettings(IUISettings uiSettings)
    {
        Preconditions.checkNotNull(uiSettings);
        this.uiSettings = uiSettings;
    }

    @Override
    public int getMaxLength()
    {
        return uiSettings.getMaxAssistantTextSize();
    }

    @Override
    public boolean isTempleted()
    {
        return uiSettings.isTemplatedContext();
    }
}
