/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.builder.IXtextBuilderParticipant;

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;

public class BuildTrackingParticipant
    implements IXtextBuilderParticipant
{
    @Inject
    public ILog log;
    @Inject
    public IGlobalContextTracker globalContextTracker;

    public BuildTrackingParticipant()
    {
        Activator.injectMembers(this);
    }

    @SuppressWarnings("nls")
    @Override
    public void build(IBuildContext context, IProgressMonitor monitor) throws CoreException
    {
        var project = context.getBuiltProject();
        if (project == null)
        {
            return;
        }

        log.trace("Building", () -> "The building was registered for project " + project.getName());
        globalContextTracker.track(project);
    }
}
