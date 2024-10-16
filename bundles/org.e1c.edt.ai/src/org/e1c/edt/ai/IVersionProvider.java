/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

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
