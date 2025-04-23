/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public interface IEnvironment
{
    String getOSName();

    OS getOS();

    String getOSVersion();

    String getArch();

    int getAvailableProcessors();

    Optional<String> getProcessorName();

    Optional<Long> getTotalPhysicalMemorySize();

    Optional<Long> getFreePhysicalMemorySize();
}
