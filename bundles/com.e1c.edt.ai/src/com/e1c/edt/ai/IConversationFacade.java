/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.SendMessageResult;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IConversationFacade
{
    CompletableFuture<SendMessageResult> sendAsync(SendUserMessageRequest request, ICancellationToken token);
}
