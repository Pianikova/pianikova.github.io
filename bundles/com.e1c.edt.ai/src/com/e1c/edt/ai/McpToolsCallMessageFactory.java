/**
 *
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.McpToolCall;

public class McpToolsCallMessageFactory
    implements IMcpToolsCallMessageFactory
{
    @Override
    public ToolCallMessage createMessage(IMcpTool tool, McpToolCall call, String content)
    {
        var message = new ToolCallMessage();
        message.role = "tool"; //$NON-NLS-1$
        message.content = content;
        if (call != null)
        {
            message.tool_call_id = call.id;
        }

        if (tool != null)
        {
            message.specification = tool.getSpecification();
        }

        message.call = call;
        return message;
    }

    @SuppressWarnings("nls")
    @Override
    public ToolCallMessage createError(IMcpTool tool, McpToolCall call, String errorMessage)
    {
        return createMessage(tool, call, "Error: \"" + errorMessage + "\"");
    }
}
