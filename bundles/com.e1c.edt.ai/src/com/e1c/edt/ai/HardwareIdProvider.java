/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;

import com.e1c.edt.ai.assistent.model.SystemInfo;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class HardwareIdProvider implements IIdProvider
{
    private static final Comparator<NetworkInterface> NetworkInterfaceComparator =
        Comparator.comparing(ni -> ni.getName() + ':' + ni.getDisplayName());
    private final ILog log;
    private final Provider<MessageDigest> messageDigestProvider;
    private final IHashTools hashTools;
    private String id;

    @Inject
    public HardwareIdProvider(ILog log, Provider<MessageDigest> messageDigestProvider, IHashTools hashTools)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(messageDigestProvider);
        Preconditions.checkNotNull(hashTools);
        this.log = log;
        this.messageDigestProvider = messageDigestProvider;
        this.hashTools = hashTools;
    }

    @Override
    public synchronized String getId()
    {
        if (id != null)
        {
            return id;
        }

        var systemInfo = new SystemInfo();
        var info = new StringBuilder();
        info.append(systemInfo.osName);
        info.append('\n');
        info.append(systemInfo.osVersion);
        info.append('\n');
        info.append(systemInfo.processorName);
        info.append('\n');
        info.append(systemInfo.availableProcessors);
        info.append('\n');
        info.append(systemInfo.totalPhysicalMemorySize);
        var bytes = info.toString().getBytes(StandardCharsets.UTF_8);
        var hash = messageDigestProvider.get();
        hash.update(bytes);

        try
        {
            var networkInterfaces = new ArrayList<NetworkInterface>();
            var networkInterfacesEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfacesEnumeration.hasMoreElements())
            {
                networkInterfaces.add(networkInterfacesEnumeration.nextElement());
            }

            networkInterfaces.sort(NetworkInterfaceComparator);
            for (var networkInterface : networkInterfaces)
            {
                var hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress != null)
                {
                    hash.update(hardwareAddress);
                }
            }
        }
        catch (SocketException error)
        {
            log.logError(error);
        }

        id = hashTools.format(hash, false);
        return id;
    }
}
