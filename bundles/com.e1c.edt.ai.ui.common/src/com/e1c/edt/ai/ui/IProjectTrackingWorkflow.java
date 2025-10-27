/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;

interface IProjectTrackingWorkflow
{
    IProjectTrackingWorkflow initialize(IProject project);

    IProject getProject();

    String getId();

    Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken);

    void track(AIContext aiCtx);
}
