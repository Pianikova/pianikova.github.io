/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;

interface IEnvironment
{
    String getOSName();

    String getOSVersion();

    String getArch();

    int getAvailableProcessors();

    Optional<String> getProcessorName();

    Optional<Long> getTotalPhysicalMemorySize();

    Optional<Long> getFreePhysicalMemorySize();
}
