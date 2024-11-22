/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

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

    @Override
    public String getOSName()
    {
        return osMXBean.getName();
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
}
