/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.Images;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIMarkerResolution
    implements IMarkerResolution2
{
    private final McpToolCall call;
    private final SetMarkersMcpTool.MarkerRequest markerRequest;

    @Inject
    IChat chat;
    @Inject
    IJson json;

    public AIMarkerResolution(McpToolCall call, SetMarkersMcpTool.MarkerRequest markerRequest)
    {
        Preconditions.checkNotNull(call);
        Preconditions.checkNotNull(markerRequest);
        this.call = call;
        this.markerRequest = markerRequest;
        BaseActivator.injectMembers(this);
    }

    @Override
    public String getLabel()
    {
        return markerRequest.actionTitle != null ? markerRequest.actionTitle : "Apply AI Suggestion"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public void run(IMarker marker)
    {
        var prompt = new StringBuilder();
        prompt.append(markerRequest.actionPrompt);
        prompt.append("\n\nDetails:\n```\\n");
        prompt.append(json.serialize(markerRequest));
        prompt.append("\n```\nDo ONLY what is asked.");
        prompt.append("\nUpdate marker with id=`" + markerRequest.id + "` after fix.");
        chat.continueChat(call.sourceChatId, prompt.toString());
    }

    @Override
    public String getDescription()
    {
        return markerRequest.actionDescription != null ? markerRequest.actionDescription
            : "Execute AI-assisted code transformation"; //$NON-NLS-1$
    }

    @Override
    public Image getImage()
    {
        return BaseActivator.getImage(Images.AI);
    }
}