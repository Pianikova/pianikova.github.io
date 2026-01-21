/**
 *
 */
package com.e1c.edt.ai;

import java.util.Map;

import org.eclipse.core.resources.IProject;

public interface IProjectDetailsProvider
{
    void fill(IProject project, Map<String, Object> details);
}
