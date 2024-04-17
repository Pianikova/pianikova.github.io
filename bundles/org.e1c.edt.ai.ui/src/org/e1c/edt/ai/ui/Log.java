/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ILog;

/**
 * @author Nikolay Pyanikov
 *
 */
public class Log
    implements ILog
{

    @Override
    public void logError(Exception exception)
    {
        Activator.logError(exception);

    }
}
