/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IInitializable;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JShellReflectionWarmUp
    implements IInitializable, IStateListener
{
    private final ILog log;
    private final Set<IJShellBindingProvider> bindingProviders;
    private final IJShellTypeIndex typeIndex;
    private final IDispatcher dispatcher;
    private final ISettings settings;
    private final IStateService stateService;
    private final AtomicBoolean scheduled = new AtomicBoolean();

    @Inject
    public JShellReflectionWarmUp(ILog log, Set<IJShellBindingProvider> bindingProviders,
        IJShellTypeIndex typeIndex, IDispatcher dispatcher, ISettings settings, IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(bindingProviders);
        Preconditions.checkNotNull(typeIndex);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(stateService);

        this.log = log;
        this.bindingProviders = bindingProviders;
        this.typeIndex = typeIndex;
        this.dispatcher = dispatcher;
        this.settings = settings;
        this.stateService = stateService;
    }

    @Override
    public void initialize()
    {
        stateService.addListener(this);
        scheduleWarmUpIfNeeded();
    }

    @Override
    public void onServiceStateChange(ServiceState serviceState)
    {
        scheduleWarmUpIfNeeded();
    }

    @Override
    public void onActionStateChange(ActionState actionState)
    {
        // Do nothing
    }

    private void scheduleWarmUpIfNeeded()
    {
        if (!settings.isEnabled() || !scheduled.compareAndSet(false, true))
        {
            return;
        }

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
