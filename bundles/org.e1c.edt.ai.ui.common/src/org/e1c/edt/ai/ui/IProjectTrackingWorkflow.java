/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

interface IProjectTrackingWorkflow
{
    IProjectTrackingWorkflow initialize(IProject project, HashSet<String> hashes);

    Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken);

    List<String> getHashes();

    void track(AIContext aiCtx);
}
