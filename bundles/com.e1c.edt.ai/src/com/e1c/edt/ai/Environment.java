/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Optional;

import javax.management.MBeanServer;
import javax.management.ObjectName;

class Environment
    implements IEnvironment
{
    private final OperatingSystemMXBean osMXBean =
        ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    private final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
    private OS os;

    @Override
    public String getOSName()
    {
        return osMXBean.getName();
    }

    @Override
    public synchronized OS getOS()
    {
        if (os != null)
        {
            return os;
        }

        os = getOSInternal();
        return os;
    }

    @Override
    public String getOSVersion()
    {
        return osMXBean.getVersion();
    }

    @Override
    public String getArch()
    {
        return osMXBean.getArch();
    }

    @Override
    public int getAvailableProcessors()
    {
        return osMXBean.getAvailableProcessors();
    }

    @Override
    public Optional<String> getProcessorName()
    {
        return Optional.ofNullable(System.getenv("PROCESSOR_IDENTIFIER")); //$NON-NLS-1$
    }

    @Override
    public Optional<Long> getTotalPhysicalMemorySize()
    {
        return getLongAttribute("TotalPhysicalMemorySize"); //$NON-NLS-1$
    }

    @Override
    public Optional<Long> getFreePhysicalMemorySize()
    {
        return getLongAttribute("FreePhysicalMemorySize"); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private Optional<Long> getLongAttribute(String name)
    {
        try
        {
            var attribute =
                beanServer.getAttribute(new ObjectName("java.lang", "type", "OperatingSystem"), name);

            return Optional.of(Long.parseLong(attribute.toString()));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    @SuppressWarnings("nls")
    private OS getOSInternal()
    {
        if (os != null)
        {
            return os;
        }

        var osName = osMXBean.getName();
        if (osName == null)
        {
            return OS.WINDOWS;
        }

        osName = osName.toLowerCase();
        if (osName.contains("windows"))
        {
            return OS.WINDOWS;
        }

        if (osName.contains("mac"))
        {
            return OS.MAC;
        }

        return OS.LINUX;
    }
}
