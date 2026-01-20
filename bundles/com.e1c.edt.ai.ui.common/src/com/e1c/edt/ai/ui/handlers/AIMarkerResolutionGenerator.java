/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;

/**
 * Marker resolution generator for AI markers that provides quick fix actions.
 */
public class AIMarkerResolutionGenerator
    implements IMarkerResolutionGenerator
{
    @Override
    public IMarkerResolution[] getResolutions(IMarker marker)
    {
        try
        {
            var call = marker.getAttribute(SetMarkersMcpTool.ACTION_CALL_ATTRIBUTE);
            var details = marker.getAttribute(SetMarkersMcpTool.ACTION_DETAILS_ATTRIBUTE);
            if (call != null && call instanceof McpToolCall && details != null
                && details instanceof SetMarkersMcpTool.MarkerRequest)
            {
                var resolutions = new IMarkerResolution[1];
                resolutions[0] = new AIMarkerResolution((McpToolCall)call, (SetMarkersMcpTool.MarkerRequest)details);
                return resolutions;
            }
        }
        catch (CoreException e)
        {
            // Log error but don't fail
        }

        return new IMarkerResolution[0];
    }
}