/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Test;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;

/**
 * Tests for {@link JobFutures}.
 */
public class JobFuturesTest
{
    private final NullProgressMonitor monitor = new NullProgressMonitor();
    private final CancellationTokenSource jobToken = new CancellationTokenSource();
    private final JobContext jobContext = new JobContext(mock(Job.class), monitor, jobToken);

    @Test
    public void returnsWhenTheOperationIsAlreadyComplete()
    {
        var operationToken = new CancellationTokenSource();

        JobFutures.await(jobContext, CompletableFuture.completedFuture("done"), operationToken); //$NON-NLS-1$

        assertFalse(operationToken.isCanceled().booleanValue());
    }

    @Test
    public void waitsUntilTheOperationCompletes() throws InterruptedException
    {
        var operationToken = new CancellationTokenSource();
        var operation = new CompletableFuture<String>();
        var completer = new Thread(() -> {
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException error)
            {
                Thread.currentThread().interrupt();
            }
            operation.complete("done"); //$NON-NLS-1$
        });

        completer.start();
        JobFutures.await(jobContext, operation, operationToken);
        completer.join();

        assertTrue(operation.isDone());
        assertFalse(operationToken.isCanceled().booleanValue());
    }

    /**
     * The progress monitor is wired to the job-local token only, so Cancel from the progress UI has
     * to be forwarded here — otherwise the request keeps running and still lands its result.
     */
    @Test
    public void cancelsTheOperationWhenTheMonitorIsCanceled()
    {
        var operationToken = new CancellationTokenSource();
        monitor.setCanceled(true);

        JobFutures.await(jobContext, new CompletableFuture<String>(), operationToken);

        assertTrue(operationToken.isCanceled().booleanValue());
    }

    @Test
    public void cancelsTheOperationWhenTheJobTokenIsCanceled()
    {
        var operationToken = new CancellationTokenSource();
        jobToken.cancel();

        JobFutures.await(jobContext, new CompletableFuture<String>(), operationToken);

        assertTrue(operationToken.isCanceled().booleanValue());
    }

    @Test
    public void toleratesATokenThatCannotBeCanceled()
    {
        ICancellationToken plainToken = () -> Boolean.FALSE;
        monitor.setCanceled(true);

        JobFutures.await(jobContext, new CompletableFuture<String>(), plainToken);
    }
}
