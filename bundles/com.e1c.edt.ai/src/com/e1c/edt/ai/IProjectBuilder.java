/**
 *
 */
package com.e1c.edt.ai;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public interface IProjectBuilder
{
    void build(IProject projectь, ICancellationToken cancellationToken) throws CoreException;
}
