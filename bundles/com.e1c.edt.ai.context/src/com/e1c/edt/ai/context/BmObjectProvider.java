/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;

import com._1c.g5.v8.bm.core.IBmExternalUriResolver;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Provider for getting BM objects from files
 */
public class BmObjectProvider implements IBmObjectProvider
{
    private final IBmModelManager modelManager;

    @Inject
    public BmObjectProvider(IBmModelManager modelManager)
    {
        Preconditions.checkNotNull(modelManager);
        this.modelManager = modelManager;
    }

    /**
     * Gets BM object from file
     * @param file the file to get BM object from
     * @return Optional containing BM object or empty if not found
     */
    public Optional<IBmObject> getObject(IFile file)
    {
        var project = file.getProject();
        var model = modelManager.getModel(project);
        if (model == null)
        {
            return Optional.empty();
        }

        var engine = model.getEngine();
        if (engine == null)
        {
            return Optional.empty();
        }

        var uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true).appendFragment("/0"); //$NON-NLS-1$
        for (IBmExternalUriResolver provider : engine.getExternalUriResolvers())
        {
            try
            {
                var obj = provider.getObject(uri);
                if (obj != null && obj instanceof IBmObject)
                {
                    return Optional.of((IBmObject)obj);
                }
            }
            catch (Exception e)
            {
                // ignore
            }
        }

        return Optional.empty();
    }
}