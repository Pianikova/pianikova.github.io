/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

interface IProjectTrackingWorkflow
{
    IProjectTrackingWorkflow initialize(IProject project, GlobalContextState state);

    String getId();

    Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken);

    void track(AIContext aiCtx);

    void reset();

    void saveState(GlobalContextState state);
}
