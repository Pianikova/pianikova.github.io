/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import org.osgi.framework.Version;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IVersionProvider
{
    Version getPluginVersion();

    String getPlatformVersion();
}
