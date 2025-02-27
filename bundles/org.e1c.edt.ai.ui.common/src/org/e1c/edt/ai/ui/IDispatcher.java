/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Supplier;

public interface IDispatcher
{
    <T> Optional<T> dispatch(Supplier<? extends T> supplier);

    Boolean dispatch(Runnable runnable);

    void dispatchAsync(Runnable runnable);

    Job createJob(String jobName, Consumer<JobContext> сonsumer, ICancellationToken cancellationToken);

    <T> Optional<T> dispatch(Supplier<? extends T> supplier, Duration timeout);
}
