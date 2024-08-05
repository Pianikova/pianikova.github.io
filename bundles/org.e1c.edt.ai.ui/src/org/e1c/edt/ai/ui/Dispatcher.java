/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

public class Dispatcher implements IDispatcher
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
}
