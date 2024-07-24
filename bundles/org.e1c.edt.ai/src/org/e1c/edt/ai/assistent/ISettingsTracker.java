/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;

import org.e1c.edt.ai.client.AISettings;

public interface ISettingsTracker
{
    boolean register(String owner, Optional<AISettings> settings);
}
