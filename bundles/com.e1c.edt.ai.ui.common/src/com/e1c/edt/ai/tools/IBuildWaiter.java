/**
 *
 */
package com.e1c.edt.ai.tools;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ICancellationToken;

public interface IBuildWaiter
{
    CompletableFuture<Void> waitForBuilds(IProject project, ICancellationToken cancellationToken);
}
