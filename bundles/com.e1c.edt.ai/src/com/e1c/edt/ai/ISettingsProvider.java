/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.client.AISettings;

public interface ISettingsProvider
{
    AISettings getSettings();

    void applyUserParameters(Parameters userParameters);
}
