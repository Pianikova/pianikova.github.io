/**
 *
 */
package com.e1c.edt.ai;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public interface IProjectBuilder
{
    /**
     * Builds the project and waits for background validation to settle so that markers can be read
     * afterwards.
     *
     * @param project the project to build, never {@code null}
     * @param cancellationToken cancellation token, never {@code null}
     * @return {@code true} if the build and any background validation finished (markers are
     *         complete); {@code false} if it did not settle within the timeout, meaning the marker
     *         set read afterwards may be incomplete
     * @throws CoreException if the build failed
     */
    boolean build(IProject project, ICancellationToken cancellationToken) throws CoreException;
}
