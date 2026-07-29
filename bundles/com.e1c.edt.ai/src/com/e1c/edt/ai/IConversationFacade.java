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
    /**
     * Sends the request without reporting progress.
     *
     * @param request request to send, cannot be {@code null}
     * @param token cancellation token, cannot be {@code null}
     * @return future with the result of the conversation
     */
    default CompletableFuture<SendMessageResult> sendAsync(SendUserMessageRequest request, ICancellationToken token)
    {
        return sendAsync(request, token, null);
    }

    /**
     * Sends the request and reports the answer being received to {@code progressListener}.
     * <p>
     * The listener is called on the response stream thread and very often — see
     * {@link IConversationProgressListener}.
     *
     * @param request request to send, cannot be {@code null}
     * @param token cancellation token, cannot be {@code null}
     * @param progressListener listener of liveness updates, may be {@code null} when not needed
     * @return future with the result of the conversation
     */
    CompletableFuture<SendMessageResult> sendAsync(SendUserMessageRequest request, ICancellationToken token,
        IConversationProgressListener progressListener);
}
