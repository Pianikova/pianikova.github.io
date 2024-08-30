/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com._1c.g5.v8.dt.bm.xtext.BmAwareResourceSetProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResourceSetProvider implements IResourceSetProvider
{
    private final IV8Model v8Model;
    private final HashMap<IProject, ResourceSet> resources = new HashMap<>();

    @Inject
    public ResourceSetProvider(IV8Model v8Model)
    {
        Preconditions.checkNotNull(v8Model);
        this.v8Model = v8Model;
    }

    @Override
    public synchronized ResourceSet getResourceSet(IProject project)
    {
        Preconditions.checkNotNull(project);
        if (isLowMemory())
        {
            resources.clear();
        }

        var resourceSet = resources.computeIfAbsent(project,
            curProject -> v8Model.getResourceService(BmAwareResourceSetProvider.class).get(curProject));

        return resourceSet;
    }

    private boolean isLowMemory()
    {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return freeMemory < totalMemory / 100 * 0.5;
    }
}
