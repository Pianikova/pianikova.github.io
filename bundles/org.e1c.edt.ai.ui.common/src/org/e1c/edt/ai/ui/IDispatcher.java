/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;
import java.util.function.Consumer;

import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Supplier;

public interface IDispatcher
{
    <T> Optional<T> dispatch(Supplier<? extends T> supplier);

    Boolean dispatch(Runnable runnable);

    void dispatchAsync(Runnable runnable);

    Job createJob(String jobName, Consumer<CancellationTokenSource> сonsumer, ICancellationToken cancellationToken);
}
