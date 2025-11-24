/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IPluginUpdateService
{
    void checkForUpdates(IProgressMonitor monitor);
}
