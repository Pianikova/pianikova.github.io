/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;

public class Web
    implements IWeb
{
    private final ILog log;

    @Inject
    public Web(ILog log)
    {
        this.log = log;
    }

    @Override
    public void browse(String url)
    {
        try
        {
            if (Desktop.isDesktopSupported())
            {
                var desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE))
                {
                    desktop.browse(new URI(url));
                }
            }
        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }

    @Override
    public void open(File file)
    {
        try
        {
            if (Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().open(file);
            }
        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }
}
