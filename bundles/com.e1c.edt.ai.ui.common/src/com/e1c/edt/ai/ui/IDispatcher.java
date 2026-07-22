/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Supplier;

public interface IDispatcher
{
    <T> Optional<T> dispatch(Supplier<? extends T> supplier);

    /**
     * Executes {@code supplier} on the UI thread, guarded by a watchdog.
     * <p>
     * Unlike {@link #dispatch(Supplier)} (which blocks the caller forever via {@code syncExec}), this
     * variant interrupts the UI thread if the supplier does not return within the watchdog timeout. It is
     * meant for potentially unsafe UI-thread work — e.g. running LLM-authored JShell snippets that may call
     * a blocking wait ({@code Job.join()}, {@code Future.get()}, {@code Thread.sleep()}), which would
     * otherwise freeze or deadlock the IDE. On watchdog interruption an empty {@link Optional} is returned.
     *
     * @param supplier work to run on the UI thread, never {@code null}
     * @return the produced value, or {@link Optional#empty()} if the work failed or was interrupted by the watchdog
     */
    <T> Optional<T> dispatchWithUiWatchdog(Supplier<? extends T> supplier);

    Boolean dispatch(Runnable runnable);

    void dispatchAsync(Runnable runnable);

    Job createJob(String jobName, Consumer<JobContext> consumer, boolean isInfrastucture,
        ICancellationToken cancellationToken);

    <T> Optional<T> dispatch(Supplier<? extends T> supplier, Duration timeout);

    boolean checkThread(boolean isUI, boolean showWarning);
}
