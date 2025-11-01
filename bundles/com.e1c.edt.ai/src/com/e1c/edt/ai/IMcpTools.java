/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.McpToolCalls;

public interface IMcpTools
{
    List<McpToolCallSpecification> getSpecifications();

    CompletableFuture<List<ToolCallMessage>> callTools(McpToolCalls calls, ICancellationToken сancellationToken);
}
