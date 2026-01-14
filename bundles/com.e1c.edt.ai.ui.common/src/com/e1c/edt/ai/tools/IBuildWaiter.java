/**
 * 
 */
package com.e1c.edt.ai.tools;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;

public interface IBuildWaiter
{
    /**
     * Asynchronously waits for ongoing builds to complete
     *
     * @param cancellationToken Token to monitor for cancellation
     * @return CompletableFuture that completes when builds finish or fails on error
     */
    CompletableFuture<Void> waitForBuilds(ICancellationToken cancellationToken);
}
