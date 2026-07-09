/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Set;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IInitializable;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JShellReflectionWarmUp
    implements IInitializable
{
    private final ILog log;
    private final Set<IJShellBindingProvider> bindingProviders;
    private final IJShellTypeIndex typeIndex;
    private final IDispatcher dispatcher;

    @Inject
    public JShellReflectionWarmUp(ILog log, Set<IJShellBindingProvider> bindingProviders,
        IJShellTypeIndex typeIndex, IDispatcher dispatcher)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(bindingProviders);
        Preconditions.checkNotNull(typeIndex);
        Preconditions.checkNotNull(dispatcher);

        this.log = log;
        this.bindingProviders = bindingProviders;
        this.typeIndex = typeIndex;
        this.dispatcher = dispatcher;
    }

    @Override
    public void initialize()
    {
        dispatcher.createJob(Messages.JShellReflectionIndexPreWarming, context -> {
            try
            {
                var significantClasses = new ArrayList<Class<?>>();
                for (var provider : bindingProviders)
                {
                    var classes = provider.getSignificantClasses();
                    if (classes != null)
                    {
                        significantClasses.addAll(classes);
                    }
                }
                typeIndex.warmUp(significantClasses, context.CancellationTokenSource);
            }
            catch (Exception e)
            {
                log.logError("Failed to warm up JShell reflection index: " + e.getMessage()); //$NON-NLS-1$
            }
        }, true, CancellationTokens.NONE).schedule();
    }
}
