/**
 *
 */
package com.e1c.edt.semantic;

import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;

public class McpToolsCallMessageFactory
    implements IMcpToolsCallMessageFactory
{
    @Override
    public ToolCallMessage createMessage(IMcpTool tool, McpToolCall call, String content)
    {
        throw new UnsupportedOperationException("Not supported yet."); //$NON-NLS-1$
    }

    @Override
    public ToolCallMessage createError(IMcpTool tool, McpToolCall call, String errorMessage)
    {
        throw new UnsupportedOperationException("Not supported yet."); //$NON-NLS-1$
    }
}
