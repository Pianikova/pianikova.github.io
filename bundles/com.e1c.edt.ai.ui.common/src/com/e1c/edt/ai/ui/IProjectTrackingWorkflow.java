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

    Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken);

    void track(AIContext aiCtx);

    /**
     * Requests a full re-sync of the project (re-hash and re-send every tracked file). The request is
     * recorded and applied on the tracking thread at the start of the next {@link #nextState} call, so it is
     * safe to invoke from any thread (e.g. a service-state listener). Used when the server session rotates
     * (settings changed / session expired) and the server has therefore lost the previously synced context.
     */
    void requestReset();
}
