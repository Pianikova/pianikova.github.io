/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIContextSettings;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class AIContextSettingsProvider
    implements Provider<AIContextSettings>
{
    private final IUISettings uiSettings;

    @Inject
    public AIContextSettingsProvider(IUISettings uiSettings)
    {
        Preconditions.checkNotNull(uiSettings);
        this.uiSettings = uiSettings;
    }

    @Override
    public AIContextSettings get()
    {
        return new AIContextSettings(uiSettings.getMaxAssistantTextSize(), uiSettings.isTemplatedContext());
    }
}
