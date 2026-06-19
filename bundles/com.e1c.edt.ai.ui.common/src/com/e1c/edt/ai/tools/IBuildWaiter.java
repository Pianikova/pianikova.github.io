/**
 *
 */
package com.e1c.edt.ai.tools;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ICancellationToken;

public interface IBuildWaiter
{
    /**
     * Triggers the project build and waits for it (and any background validation) to settle.
     *
     * @return a future completing with {@code true} if the build/validation finished (markers are
     *         complete), or {@code false} if it did not settle within the timeout, meaning markers
     *         read afterwards may be incomplete
     */
    CompletableFuture<Boolean> waitForBuilds(IProject project, ICancellationToken cancellationToken);
}
