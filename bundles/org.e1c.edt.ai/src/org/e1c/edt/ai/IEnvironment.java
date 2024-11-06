/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

public interface IEnvironment
{
    String getOSName();

    String getOSVersion();

    String getArch();

    int getAvailableProcessors();

    Optional<String> getProcessorName();

    Optional<Long> getTotalPhysicalMemorySize();

    Optional<Long> getFreePhysicalMemorySize();
}
