/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

/**
 * Marker resolution generator for AI markers that provides quick fix actions.
 */
public class AIMarkerResolutionGenerator
    implements IMarkerResolutionGenerator2
{
    @Inject
    IJson json;

    public AIMarkerResolutionGenerator()
    {
        BaseActivator.injectMembers(this);
    }

    /**
     * Marker resolution generator for AI markers that provides quick fix actions.
     */
    @Override
    public boolean hasResolutions(IMarker marker)
    {
        return marker.getAttribute(SetMarkersMcpTool.ACTION_CHAT_ID_ATTRIBUTE, null) != null
            && marker.getAttribute(SetMarkersMcpTool.ACTION_DETAILS_ATTRIBUTE, null) != null;
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker)
    {
        try
        {
            var chatId = marker.getAttribute(SetMarkersMcpTool.ACTION_CHAT_ID_ATTRIBUTE);
            var details = marker.getAttribute(SetMarkersMcpTool.ACTION_DETAILS_ATTRIBUTE);
            if (chatId instanceof String && details instanceof String)
            {
                var optionalDetails = json.deserialize((String)details, SetMarkersMcpTool.MarkerRequest.class);
                if (optionalDetails.isPresent())
                {
                    var resolutions = new IMarkerResolution[1];
                    resolutions[0] = new AIMarkerResolution((String)chatId, optionalDetails.get());
                    return resolutions;
                }
            }
        }
        catch (CoreException e)
        {
            // Log error but don't fail
        }

        return new IMarkerResolution[0];
    }
}
