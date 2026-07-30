/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.ToolCallMessage;

/**
 * Tests for skill preprocessing tool calls.
 */
@SuppressWarnings("nls")
public class McpToolInvokerTest
{
    @Test
    public void compactsJsonToolResultBeforePromptSubstitution()
    {
        var mcpTools = mock(IMcpTools.class);
        when(mcpTools.callTools(any(), any())).thenReturn(result("{\n  \"value\": 1,\n  \"items\": [\n    2\n  ]\n}"));
        var json = mock(IJson.class);
        when(json.serialize(any())).thenReturn("{}");
        when(json.compactJson(any())).thenReturn("{\"value\":1,\"items\":[2]}");
        var invoker = new McpToolInvoker(mcpTools, json);

        var actual = invoker.invokeAsync(mockRequest("GetVisualContext"),
            mock(ICancellationToken.class)).join();

        assertEquals("{\"value\":1,\"items\":[2]}", actual);
    }

    @Test
    public void preservesNonJsonToolResult()
    {
        var mcpTools = mock(IMcpTools.class);
        when(mcpTools.callTools(any(), any())).thenReturn(result("plain\ntext"));
        var json = mock(IJson.class);
        when(json.serialize(any())).thenReturn("{}");
        when(json.compactJson("plain\ntext")).thenReturn("plain\ntext");
        var invoker = new McpToolInvoker(mcpTools, json);

        var actual = invoker.invokeAsync(mockRequest("JGit"),
            mock(ICancellationToken.class)).join();

        assertEquals("plain\ntext", actual);
    }

    private static ToolRequestSpecification mockRequest(String name)
    {
        var request = mock(ToolRequestSpecification.class);
        when(request.getName()).thenReturn(name);
        return request;
    }

    private static CompletableFuture<McpCallToolsResult> result(String content)
    {
        var message = new ToolCallMessage();
        message.content = content;
        var result = new McpCallToolsResult();
        result.messages = List.of(message);
        return CompletableFuture.completedFuture(result);
    }
}
