/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.resource.ResourceSet;

public interface IResourceSetProvider
{
    ResourceSet getResourceSet(IProject project);
}
