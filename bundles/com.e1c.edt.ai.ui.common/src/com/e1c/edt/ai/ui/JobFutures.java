/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;

/**
 * Keeps an Eclipse job alive while the asynchronous operation it started is still running.
 * <p>
 * A job body that only chains {@link CompletableFuture} callbacks returns within milliseconds, so
 * the job is done long before the work is: the standard progress UI — the progress region of the
 * status bar, the Progress view row and its Cancel button — flashes and disappears at once, and the
 * user gets no sign that anything is running. Waiting for the terminal future of the chain makes
 * the job lifetime match the operation.
 * <p>
 * Cancellation has to be forwarded explicitly. {@code Dispatcher} wires the progress monitor to the
 * job-local token only, and the outer token — the one the operation itself was started with — is
 * never touched by it. Without forwarding, Cancel would just remove the progress row while the
 * request kept running and still landed its result.
 *
 * @author Skill Test
 */
public final class JobFutures
{
    /** How often the monitor is re-checked while the operation is running. */
    private static final long POLL_INTERVAL_MS = 200;

    /**
     * Blocks the calling job thread until {@code operation} completes or the job is canceled; on
     * cancellation the operation is canceled too, so it stops instead of finishing unobserved.
     * <p>
     * Failures are not rethrown: the future chain reports them through its own handlers, and this
     * wait exists only to hold the job open.
     *
     * @param jobContext context of the running job, cannot be {@code null}
     * @param operation terminal future of the operation started by the job, cannot be {@code null}
     * @param operationToken token the operation was started with, cannot be {@code null}; canceled
     * when the job is canceled, which requires it to be a {@link CancellationTokenSource}
     */
    public static void await(JobContext jobContext, CompletableFuture<?> operation,
        ICancellationToken operationToken)
    {
        while (!operation.isDone())
        {
            if (jobContext.Monitor.isCanceled() || jobContext.CancellationTokenSource.isCanceled())
            {
                if (operationToken instanceof CancellationTokenSource)
                {
                    ((CancellationTokenSource)operationToken).cancel();
                }

                return;
            }

            try
            {
                operation.get(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException error)
            {
                // Still running — loop back and re-check the monitor.
            }
            catch (InterruptedException error)
            {
                Thread.currentThread().interrupt();
                return;
            }
            catch (ExecutionException error)
            {
                return;
            }
        }
    }

    private JobFutures()
    {
        // Utility class
    }
}
