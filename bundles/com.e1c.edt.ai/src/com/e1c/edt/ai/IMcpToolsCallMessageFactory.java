/**
 *
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.McpToolCall;

public interface IMcpToolsCallMessageFactory
{
    ToolCallMessage createMessage(IMcpTool tool, McpToolCall call, String content, ToolCallMessageDetails details);

    ToolCallMessage createError(IMcpTool tool, McpToolCall call, String errorMessage);
}
