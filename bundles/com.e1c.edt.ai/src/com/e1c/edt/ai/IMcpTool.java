/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;

public interface IMcpTool
{
    McpToolCallSpecification getSpecification();

    CompletableFuture<ToolCallMessage> call(McpToolCall calls, ICancellationToken сancellationToken);
}
