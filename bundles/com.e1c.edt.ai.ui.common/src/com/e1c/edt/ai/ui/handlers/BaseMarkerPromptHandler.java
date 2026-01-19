package com.e1c.edt.ai.ui.handlers;

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.google.inject.Inject;

public class BaseMarkerPromptHandler
    extends AbstractHandler
{
    @Inject
    IChat chat;
    @Inject
    IJson json;

    public BaseMarkerPromptHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        var selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection)
        {
            var element = ((IStructuredSelection)selection).getFirstElement();
            if (element instanceof IMarker)
            {
                processMarker((IMarker)element);
            }
        }

        return null;
    }

    public void processMarker(IMarker marker)
    {
        try
        {
            var call = marker.getAttribute(SetMarkersMcpTool.ACTION_CALL_ATTRIBUTE);
            var details = marker.getAttribute(SetMarkersMcpTool.ACTION_DETAILS_ATTRIBUTE);
            if (call != null && call instanceof McpToolCall && details != null
                && details instanceof SetMarkersMcpTool.MarkerRequest)
            {
                // Execute AI prompt
                executeAiPrompt(marker, (McpToolCall)call, (SetMarkersMcpTool.MarkerRequest)details);
            }
        }
        catch (CoreException e)
        {
            //
        }
    }

    protected void executeAiPrompt(IMarker marker, McpToolCall call, SetMarkersMcpTool.MarkerRequest details)
    {
        var message = new ToolCallMessage();
        message.role = "user"; //$NON-NLS-1$
        message.tool_call_id = call.id;
        message.content = "Fix this:\n" + json.serialize(details); //$NON-NLS-1$;
        var result = new McpCallToolsResult();
        result.messages = new ArrayList<>();
        result.messages.add(message);
        chat.addToolsResult(call.sourceChatId, call.sourceMessageId, result);
    }
}