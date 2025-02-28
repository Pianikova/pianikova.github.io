/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IUISettings;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

class Dispatcher
    implements IDispatcher
{
    private static final StackTraceElement[] EmptyStackTrace = new StackTraceElement[0];
    private final ILog log;
    private final IUISettings settings;
    private final IClock clock;

    @Inject
    public Dispatcher(ILog log, IUISettings settings, IClock clock)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(clock);
        this.settings = settings;
        this.log = log;
        this.clock = clock;
    }

    @Override
    public <T> Optional<T> dispatch(Supplier<? extends T> supplier)
    {
        Preconditions.checkNotNull(supplier);
        return dispatch(supplier, false);
    }

    @Override
    public Boolean dispatch(Runnable runnable)
    {
        Preconditions.checkNotNull(runnable);
        return dispatch(() -> {
            runnable.run();
            return 0;
        }, false).isPresent();
    }

    @Override
    public void dispatchAsync(Runnable runnable)
    {
        Preconditions.checkNotNull(runnable);
        dispatch(() -> {
            runnable.run();
            return 0;
        }, true);
    }

    private <T> Optional<T> dispatch(Supplier<? extends T> supplier, boolean async)
    {
        Preconditions.checkNotNull(supplier);
        var startTime = clock.now();
        var vals = new ArrayList<T>();
        if(async)
        {
            StackTraceElement[] stackTrace =
                settings.traceMode() ? Thread.currentThread().getStackTrace() : EmptyStackTrace;
            Display.getDefault().asyncExec(() -> {
                try
                {
                    vals.add(supplier.get());
                    checkMicrofreeze("Async call", startTime, () -> stackTrace); //$NON-NLS-1$
                }
                catch (Exception ex)
                {
                    log.logError(ex);
                }
            });
        }
        else
        {
            Display.getDefault().syncExec(() -> {
                try
                {
                    vals.add(supplier.get());
                }
                catch (Exception ex)
                {
                    log.logError(ex);
                }
            });

            checkMicrofreeze("Sync call", startTime, () -> Thread.currentThread().getStackTrace()); //$NON-NLS-1$
        }

        if (vals.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.ofNullable(vals.get(0));
    }

    @SuppressWarnings("nls")
    private void checkMicrofreeze(String description, LocalDateTime startTime,
        Supplier<StackTraceElement[]> stackTraceSupplier)
    {
        if (!settings.traceMode())
        {
            return;
        }

        var duration = Duration.between(startTime, clock.now());
        if (duration.toMillis() > settings.getMinRequestDelay().toMillis())
        {
            log.warning("Microfreeze UI", () -> {
                var sb = new StringBuilder();
                sb.append("Description: ");
                sb.append(description);

                sb.append(System.lineSeparator());
                sb.append("Duration: ");
                sb.append(duration.toMillis());
                sb.append(" ms");

                sb.append(System.lineSeparator());
                sb.append("Stack:");
                var stackTrace = stackTraceSupplier.get();
                if (stackTrace.length > 0)
                {
                    sb.append(System.lineSeparator());
                    for (StackTraceElement ste : stackTrace)
                    {
                        sb.append(System.lineSeparator());
                        sb.append(ste);
                    }
                }
                else
                {
                    sb.append(" empty");
                }

                return sb.toString();
            });
        }
    }

    @Override
    public Job createJob(String jobName, Consumer<JobContext> сonsumer,
        ICancellationToken cancellationToken)
    {
        var resources = new ArrayList<AutoCloseable>();

        var job = new Job(jobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                var cancellationTokenSource = new JobCancellationTokenSource();
                cancellationTokenSource.attachMonitor(monitor);
                try
                {
                    сonsumer.accept(new JobContext(monitor, cancellationTokenSource));
                    return cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
                }
                catch (Throwable error)
                {
                    return Status.error(jobName, error);
                }
                finally
                {
                    synchronized (resources)
                    {
                        for (var resource : resources)
                        {
                            try
                            {
                                resource.close();
                            }
                            catch (Exception error)
                            {
                                log.logError(error);
                            }
                        }
                    }
                }
            }
        };

        if (cancellationToken != null)
        {
            var attachToken = CancellationTokenSource.attach(cancellationToken, () -> job.cancel());
            synchronized (resources)
            {
                resources.add(attachToken);
            }
        }

        job.setSystem(!settings.traceMode());
        return job;
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
