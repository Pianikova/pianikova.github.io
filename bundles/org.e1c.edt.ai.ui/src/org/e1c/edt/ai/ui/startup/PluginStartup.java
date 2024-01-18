/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.startup;

import org.eclipse.ui.IStartup;

/**
 * This class serves as an activation point for plugins
 * at platform startup. It is instantiated automatically
 * at platform startup, which causes other plugins to be
 * pulled in.
 *
 * @author Bogdan Sushkov
 *
 */
public class PluginStartup
    implements IStartup
{

    @Override
    public void earlyStartup()
    {
        // Empty method
    }

}
