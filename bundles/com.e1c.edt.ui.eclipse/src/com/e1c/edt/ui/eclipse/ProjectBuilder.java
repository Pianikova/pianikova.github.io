/**
 *
 */
package com.e1c.edt.ui.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectBuilder;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectBuilder
    implements IProjectBuilder
{
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;

    @Inject
    public ProjectBuilder(Provider<ICancellationProgressMonitor> cancellationProgressMonitor)
    {
        Preconditions.checkNotNull(cancellationProgressMonitor);
        this.cancellationProgressMonitor = cancellationProgressMonitor;
    }

    @Override
    public boolean build(IProject project, ICancellationToken cancellationToken) throws CoreException
    {
        var monitor = cancellationProgressMonitor.get();
        monitor.setCancellationToken(cancellationToken);
        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
        // Eclipse incremental build is synchronous, so markers read afterwards are complete.
        return true;
    }

}
