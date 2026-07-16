/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

/**
 * Quick-fix generator for STANDARD editor problem markers (BSL validation errors in 1C:EDT,
 * Java problems in Eclipse) — offers a single "Fix with 1C:Workmate" resolution that opens the
 * chat with a new fix conversation. Our own AI markers are excluded: they carry their own
 * resolution via {@link AIMarkerResolutionGenerator}.
 * <p>
 * Registered in each variant's plugin.xml for the concrete error marker types (Xtext/BSL check
 * markers in EDT, {@code org.eclipse.jdt.core.problem} in Eclipse).
 */
public class ExternalProblemMarkerResolutionGenerator
    implements IMarkerResolutionGenerator2
{
    private static final String AI_MARKER_TYPE_PREFIX = "com.e1c.edt.ai."; //$NON-NLS-1$

    @Inject
    ISettings settings;
    @Inject
    IEditingSupport editingSupport;

    public ExternalProblemMarkerResolutionGenerator()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean hasResolutions(IMarker marker)
    {
        if (!settings.isEnabled())
        {
            return false;
        }
        try
        {
            int severity = marker.getAttribute(IMarker.SEVERITY, -1);
            if (severity != IMarker.SEVERITY_ERROR && severity != IMarker.SEVERITY_WARNING
                && severity != IMarker.SEVERITY_INFO)
            {
                return false;
            }
            // Our AI markers have their own resolution (AIMarkerResolutionGenerator) — skip to
            // avoid a duplicate entry. Prefix check covers AIError/AIWarning/AIInfo and the base.
            if (marker.getType().startsWith(AI_MARKER_TYPE_PREFIX))
            {
                return false;
            }
            var message = marker.getAttribute(IMarker.MESSAGE, null);
            if (message == null || message.isBlank())
            {
                return false;
            }
            var resource = marker.getResource();
            if (!(resource instanceof IFile))
            {
                return false;
            }
            // Full vendor support configurations are read-only for AI edits — no point offering
            // a fix. Indeterminate states report writable (guidance, not a security boundary).
            return !editingSupport.isReadOnly(resource.getProject());
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker)
    {
        if (!hasResolutions(marker))
        {
            return new IMarkerResolution[0];
        }
        return new IMarkerResolution[] { new ExternalProblemMarkerResolution() };
    }
}
