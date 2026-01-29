/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectBuilder;

public class ProjectBuilder
    implements IProjectBuilder
{
    @Override
    public void build(IProject project, ICancellationToken cancellationToken)
    {
        // skip builds
    }
}
