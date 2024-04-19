/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Supplier;

public class Dispatcher implements IDispatcher
{
    private ILog log;

    public Dispatcher(ILog log)
    {
        this.log = log;
    }

    @Override
    public <T> Optional<T> dispatch(Supplier<T> supplier)
    {
        var vals = new ArrayList<T>();
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

        if (vals.isEmpty())
        {
            Optional.empty();
        }

        return Optional.of(vals.get(0));
    }

    @Override
    public Boolean dispatch(Runnable supplier)
    {
        return dispatch(() -> {
            supplier.run();
            return 0;
        }).isPresent();
    }
}
