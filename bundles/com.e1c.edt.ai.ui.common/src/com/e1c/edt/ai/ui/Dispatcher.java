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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

class Dispatcher
    implements IDispatcher
{
    private static final StackTraceElement[] EmptyStackTrace = new StackTraceElement[0];

    /**
     * Lower bound for the UI watchdog timeout. A hung UI-thread call (e.g. {@code Job.join()} from a JShell
     * snippet) never completes, so the watchdog only needs to be generous enough not to interrupt legitimate
     * long-running metadata work — holding the UI thread longer than this is already a severe freeze.
     */
    private static final Duration UiWatchdogFloor = Duration.ofSeconds(120);

    private final ILog log;
    private final ISettings settings;
    private final IClock clock;
    private final ArrayList<Job> currentJobs = new ArrayList<>();
    private final ScheduledExecutorService uiWatchdogScheduler =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "ai-ui-watchdog"); //$NON-NLS-1$
            thread.setDaemon(true);
            return thread;
        });

    @Inject
    public Dispatcher(ILog log, ISettings settings, IClock clock)
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
            var stackTrace = getStack();
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
            if (Thread.currentThread() == Display.getDefault().getThread())
            {
                try
                {
                    vals.add(supplier.get());
                }
                catch (Exception ex)
                {
                    log.logError(ex);
                }
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
            }

            checkMicrofreeze("Sync call", startTime, () -> Thread.currentThread().getStackTrace()); //$NON-NLS-1$
        }

        if (vals.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.ofNullable(vals.get(0));
    }

    @SuppressWarnings("nls")
    @Override
    public <T> Optional<T> dispatchWithUiWatchdog(Supplier<? extends T> supplier)
    {
        Preconditions.checkNotNull(supplier);
        var display = Display.getDefault();

        // Already on the UI thread: a watchdog cannot interrupt the thread it runs on, so fall back to a
        // plain inline invocation. In practice unsafe callers (JShell) always dispatch from a worker thread.
        if (Thread.currentThread() == display.getThread())
        {
            return dispatch(supplier);
        }

        var startTime = clock.now();
        var vals = new ArrayList<T>();
        var uiThread = display.getThread();
        // Claimed either by the watchdog (when it fires) or by normal completion, whichever happens first.
        var settled = new AtomicBoolean(false);
        var watchdogFired = new AtomicBoolean(false);
        var timeout = computeUiWatchdogTimeout();

        ScheduledFuture<?> watchdog = uiWatchdogScheduler.schedule(() -> {
            if (settled.compareAndSet(false, true))
            {
                watchdogFired.set(true);
                // Capture the hung UI-thread stack before interrupting — this is what surfaces the offending
                // snippet in .metadata/.log.
                var uiStack = uiThread.getStackTrace();
                log.warning("UI watchdog", () -> {
                    var sb = new StringBuilder();
                    sb.append("A UI-thread operation exceeded ").append(timeout.toMillis()).append(" ms and was");
                    sb.append(" interrupted to keep the IDE responsive (likely a blocking wait such as Job.join()).");
                    sb.append(System.lineSeparator()).append("UI thread stack:");
                    for (StackTraceElement ste : uiStack)
                    {
                        sb.append(System.lineSeparator()).append('\t').append(ste);
                    }
                    return sb.toString();
                });
                // Breaks blocking waits (Job.join/Object.wait/sleep/park) running on the UI thread.
                uiThread.interrupt();
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);

        try
        {
            display.syncExec(() -> {
                try
                {
                    vals.add(supplier.get());
                }
                catch (Exception ex)
                {
                    log.logError(ex);
                }
            });
        }
        finally
        {
            // Normal completion wins the race → cancel the pending watchdog. If the watchdog already fired,
            // the CAS fails and we leave watchdogFired set.
            if (settled.compareAndSet(false, true))
            {
                watchdog.cancel(false);
            }
            checkMicrofreeze("UI watchdog call", startTime, () -> Thread.currentThread().getStackTrace());
        }

        if (watchdogFired.get())
        {
            return Optional.empty();
        }

        if (vals.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.ofNullable(vals.get(0));
    }

    private Duration computeUiWatchdogTimeout()
    {
        var configured = settings.getTimeout();
        if (configured == null || configured.compareTo(UiWatchdogFloor) < 0)
        {
            return UiWatchdogFloor;
        }
        return configured;
    }

    private StackTraceElement[] getStack()
    {
        return settings.getVerbosity().getLevel() >= Verbosity.TRACE.getLevel() ? Thread.currentThread().getStackTrace()
            : EmptyStackTrace;
    }

    @SuppressWarnings("nls")
    private void checkMicrofreeze(String description, LocalDateTime startTime,
        Supplier<StackTraceElement[]> stackTraceSupplier)
    {
        if (settings.getVerbosity().getLevel() < Verbosity.TRACE.getLevel())
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

    @SuppressWarnings("nls")
    @Override
    public Job createJob(String jobName, Consumer<JobContext> consumer, boolean isInfrastucture,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(jobName);
        Preconditions.checkNotNull(consumer);
        Preconditions.checkNotNull(cancellationToken);
        if (!settings.isEnabled() && !isInfrastucture)
        {
            log.warning(TracingSources.JOBS,
                () -> "Running non infrastructure job \"" + jobName + "\" while plugin is disabled.");
        }

        var isTracing = log.isTracingEnabled(TracingSources.JOBS);
        var resources = new ArrayList<AutoCloseable>();
        var jobs = new ArrayList<Job>();
        var job = new Job(jobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                var cancellationTokenSource = new JobCancellationTokenSource();
                cancellationTokenSource.attachMonitor(monitor);
                try
                {
                    var startTime = clock.now();
                    consumer.accept(new JobContext(jobs.get(0), monitor, cancellationTokenSource));
                    var duration = Duration.between(startTime, clock.now());
                    log.trace(TracingSources.JOBS, jobName, () -> "duration: " + duration);
                    return cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
                }
                catch (Throwable error)
                {
                    log.trace(TracingSources.JOBS, jobName, () -> "error: " + error);
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

        jobs.add(job);
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> job.cancel());
        synchronized (resources)
        {
            resources.add(attachToken);
        }

        if (isTracing)
        {
            synchronized (currentJobs)
            {
                currentJobs.add(job);
            }

            job.addJobChangeListener(new IJobChangeListener()
            {
                @Override
                public void aboutToRun(IJobChangeEvent event)
                {
                    log.trace(TracingSources.JOBS, jobName, () -> "about to run" + getJobsInfo());
                }

                @Override
                public void awake(IJobChangeEvent event)
                {
                    log.trace(TracingSources.JOBS, jobName, () -> "awake" + getJobsInfo());
                }

                @Override
                public void done(IJobChangeEvent event)
                {
                    synchronized (currentJobs)
                    {
                        currentJobs.remove(job);
                    }

                    log.trace(TracingSources.JOBS, jobName, () -> "done" + getJobsInfo());
                }

                @Override
                public void running(IJobChangeEvent event)
                {
                    log.trace(TracingSources.JOBS, jobName, () -> "running" + getJobsInfo());
                }

                @Override
                public void scheduled(IJobChangeEvent event)
                {
                    log.trace(TracingSources.JOBS, jobName, () -> "scheduled" + getJobsInfo());
                }

                @Override
                public void sleeping(IJobChangeEvent event)
                {
                    log.trace(TracingSources.JOBS, jobName, () -> "sleeping" + getJobsInfo());
                }
            });
        }

        return job;
    }

    @SuppressWarnings("nls")
    private String getJobsInfo()
    {
        synchronized (currentJobs)
        {
            return " jobs: "
                + String.join(", ", currentJobs.stream().map(i -> i.getName()).sorted().collect(Collectors.toList()));
        }
    }

    @Override
    public <T> Optional<T> dispatch(Supplier<? extends T> supplier, Duration timeout)
    {
        Preconditions.checkNotNull(supplier);
        Preconditions.checkNotNull(timeout);
        var executor = Executors.newCachedThreadPool();
        try
        {
            var result = executor.submit(() -> supplier.get()).get(timeout.toNanos(), TimeUnit.NANOSECONDS);
            return Optional.ofNullable(result);
        }
        catch (InterruptedException | ExecutionException | TimeoutException error)
        {
            log.warning("Dispatch", () -> error.toString()); //$NON-NLS-1$
            return Optional.empty();
        }
        finally
        {
            executor.shutdown();
        }
    }

    @SuppressWarnings("nls")
    @Override
    public boolean checkThread(boolean isUI, boolean showWarning)
    {
        var actualIsUI = Thread.currentThread() == Display.getDefault().getThread();
        if (settings.getVerbosity().getLevel() < Verbosity.TRACE.getLevel())
        {
            return actualIsUI;
        }

        if (actualIsUI == isUI)
        {
            return true;
        }

        if (!showWarning)
        {
            return false;
        }

        var stackTrace = getStack();
        log.warning(isUI ? "Execution in the UI thread is expected" : "Execution not in a UI thread is expected",
            () -> {
                var sb = new StringBuilder();
                sb.append("Stack:");
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

        return false;
    }
}
