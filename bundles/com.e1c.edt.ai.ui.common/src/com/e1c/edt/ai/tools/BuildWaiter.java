package com.e1c.edt.ai.tools;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectBuilder;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class BuildWaiter implements IBuildWaiter
{
    private static final Object[] BUILD_FAMILIES =
        Arrays
            .asList(ResourcesPlugin.FAMILY_AUTO_BUILD, ResourcesPlugin.FAMILY_MANUAL_BUILD,
                ResourcesPlugin.FAMILY_AUTO_REFRESH, ResourcesPlugin.FAMILY_MANUAL_REFRESH)
            .toArray();
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IProjectBuilder builder;

    @Inject
    public BuildWaiter(Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IProjectBuilder builder)
    {
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(builder);

        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.builder = builder;
    }

    /**
     * Asynchronously waits for ongoing builds to complete
     *
     * @param cancellationToken Token to monitor for cancellation
     * @return CompletableFuture that completes when builds finish or fails on error
     */
    @Override
    @SuppressWarnings("nls")
    public CompletableFuture<Boolean> waitForBuilds(IProject project, ICancellationToken cancellationToken)
    {
        var jobManager = Job.getJobManager();

        // Check if there are any active jobs in the build families
        boolean hasActiveJobs = Arrays.stream(BUILD_FAMILIES)
            .flatMap(family -> Arrays.stream(jobManager.find(family)))
            .anyMatch(job -> job.getState() == Job.RUNNING || job.getState() == Job.WAITING);

        // Always invoke the builder: in EDT it waits for the background DD validation to settle even
        // when no Eclipse build jobs are active (1C validation is not an Eclipse Job). The Eclipse
        // job join is only needed when build-family jobs are actually running.
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                boolean complete = builder.build(project, cancellationToken);
                if (hasActiveJobs)
                {
                    var monitor = cancellationProgressMonitor.get();
                    monitor.setCancellationToken(cancellationToken);
                    jobManager.join(BUILD_FAMILIES, monitor);
                }
                return complete;
            }
            catch (OperationCanceledException e)
            {
                throw new CompletionException("Build waiting was cancelled", e);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new CompletionException("Build waiting was interrupted", e);
            }
            catch (CoreException e)
            {
                Thread.currentThread().interrupt();
                throw new CompletionException("Build failed", e);
            }
        });
    }
}