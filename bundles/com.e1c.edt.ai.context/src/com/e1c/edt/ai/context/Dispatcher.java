/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.e1c.edt.ai.ILog;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

class Dispatcher
    implements IDispatcher
{
    private ILog log;

    @Inject
    public Dispatcher(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public <T> Optional<T> dispatch(Supplier<? extends T> supplier, Duration timeout)
    {
        Preconditions.checkNotNull(supplier);
        Preconditions.checkNotNull(timeout);
        var executor = Executors.newSingleThreadExecutor();
        try
        {
            var result = executor.submit(() -> supplier.get()).get(timeout.toNanos(), TimeUnit.NANOSECONDS);
            return Optional.ofNullable(result);
        }
        catch (InterruptedException | ExecutionException | TimeoutException error)
        {
            log.trace("Dispatch", () -> error.toString()); //$NON-NLS-1$
            return Optional.empty();
        }
        finally
        {
            executor.shutdown();
        }
    }
}
