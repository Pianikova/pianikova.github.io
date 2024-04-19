/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import org.e1c.edt.ai.client.AISettings;

public interface ISettingsProvider
{
    Optional<AISettings> getSettings();
}
