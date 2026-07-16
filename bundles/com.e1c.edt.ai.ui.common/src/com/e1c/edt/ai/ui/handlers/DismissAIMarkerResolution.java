/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.Messages;
import com.google.inject.Inject;

/** Removes an AI marker without applying its suggested change. */
public class DismissAIMarkerResolution
    implements IMarkerResolution2
{
    @Inject
    ILog log;

    public DismissAIMarkerResolution()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public String getLabel()
    {
        return Messages.DismissAIMarker;
    }

    @Override
    public String getDescription()
    {
        return Messages.DismissAIMarkerDescription;
    }

    @Override
    public Image getImage()
    {
        return null;
    }

    @Override
    public void run(IMarker marker)
    {
        try
        {
            marker.delete();
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }
}
