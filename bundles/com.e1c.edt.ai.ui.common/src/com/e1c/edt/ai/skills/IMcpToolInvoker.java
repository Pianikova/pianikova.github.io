/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IMcpToolInvoker
{
    CompletableFuture<String> invokeAsync(ToolRequestSpecification request, ICancellationToken token);
}
