/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;

/**
 * Provides scenario-oriented guidance for writing JShell code.
 */
public interface IJShellManualProvider
{
    /**
     * Returns manual entries that describe how to implement supported scenarios.
     *
     * @return manual entries
     */
    Collection<JShellManualEntry> getManualEntries();
}
