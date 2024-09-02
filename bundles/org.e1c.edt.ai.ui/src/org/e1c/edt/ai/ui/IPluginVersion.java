/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.osgi.framework.Version;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IPluginVersion
{
    Optional<Version> getPluginVersion();
}
