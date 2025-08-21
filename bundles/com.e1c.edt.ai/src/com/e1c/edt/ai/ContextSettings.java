/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.concurrent.ExecutionException;

import com.e1c.edt.ai.assistent.IParametersService;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ContextSettings
    implements IContextSettings
{
    private static final int DEFAULT_PREFIX_LEN = 1000;
    private static final int DEFAULT_SUFFIX_LEN = 500;
    private final IParametersService parametersService;

    @Inject
    public ContextSettings(IParametersService parametersService)
    {
        Preconditions.checkNotNull(parametersService);
        this.parametersService = parametersService;
    }

    @Override
    public int getPrefixLength()
    {
        var params = parametersService.getParametersAsync(true);
        if (params.isDone())
        {
            try
            {
                return params.get().map(i -> i.prefixLength).orElse(DEFAULT_PREFIX_LEN);
            }
            catch (InterruptedException | ExecutionException e)
            {
                // ignored
            }
        }

        return DEFAULT_PREFIX_LEN;
    }

    @Override
    public int getSuffixLength()
    {
        var params = parametersService.getParametersAsync(true);
        if (params.isDone())
        {
            try
            {
                return params.get().map(i -> i.suffixLength).orElse(DEFAULT_SUFFIX_LEN);
            }
            catch (InterruptedException | ExecutionException e)
            {
                // ignored
            }
        }

        return DEFAULT_SUFFIX_LEN;
    }
}
