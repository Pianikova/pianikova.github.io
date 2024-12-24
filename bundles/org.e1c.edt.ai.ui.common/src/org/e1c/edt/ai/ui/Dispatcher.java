/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.ILog;
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
    private ILog log;

    @Inject
    public Dispatcher(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
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
        var vals = new ArrayList<T>();
        if(async)
        {
            Display.getDefault().asyncExec(() -> {
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

        if (vals.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.ofNullable(vals.get(0));
    }

    @Override
    public Job createJob(String jobName, Consumer<CancellationTokenSource> сonsumer,
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
                    сonsumer.accept(cancellationTokenSource);
                    return cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;                }
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

        return job;
    }
}
