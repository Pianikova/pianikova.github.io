/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.v8.bm.core.BmObject;
import com._1c.g5.v8.bm.core.IBmExternalUriResolver;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class BmPovider implements IBmPovider
{
    private final IResourceLookup resourceLookup;
    private final IBmModelManager modelManager;
    private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;

    @Inject
    public BmPovider(IResourceLookup resourceLookup,
        IBmModelManager modelManager, IProjectFileSystemSupportProvider projectFileSystemSupportProvider)
    {
        Preconditions.checkNotNull(resourceLookup);
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(projectFileSystemSupportProvider);
        this.resourceLookup = resourceLookup;
        this.modelManager = modelManager;
        this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
    }

    @Override
    public Optional<BmRoot> getRoot(IDocument document, String filePath, ICancellationToken cancellationToken)
    {
        if (filePath == null || filePath.isBlank())
        {
            return Optional.empty();
        }

        var uri = getURI(filePath);
        var project = resourceLookup.getProject(uri);
        if (project == null)
        {
            return Optional.empty();
        }

        var model = modelManager.getModel(project);
        if (model == null)
        {
            return Optional.empty();
        }

        var dtProject = modelManager.getDtProject(model);
        if (dtProject == null)
        {
            return Optional.empty();
        }

        var engine = model.getEngine();
        if (engine == null)
        {
            return Optional.empty();
        }

        IBmObject bmObject = null;
        if (document != null && document instanceof BslXtextDocument)
        {
            IUnitOfWork<XtextResource, XtextResource> work = res -> res;
            var bslXtextDocument = ((BslXtextDocument)document).readOnlyDataModel(work);
            if(bslXtextDocument != null)
            {
                for (var content : bslXtextDocument.getContents())
                {
                    if (content instanceof Module)
                    {
                        var module = (Module)content;
                        var moduleResource = module.eResource();
                        if (moduleResource instanceof BslResource)
                        {
                            ((BslResource)moduleResource).setDeepAnalysis(true);
                            EcoreUtil2.resolveLazyCrossReferences(moduleResource, new CancelIndicator()
                            {
                                @Override
                                public boolean isCanceled()
                                {
                                    return cancellationToken.isCanceled();
                                }
                            });
                        }

                        if (module instanceof BmObject)
                        {
                            bmObject = (BmObject)module;
                            break;
                        }
                    }
                }
            }
        }

        if (bmObject == null)
        {
            for (IBmExternalUriResolver provider : engine.getExternalUriResolvers())
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                var obj = provider.getObject(uri);
                if (obj != null && obj instanceof IBmObject)
                {
                    bmObject = (IBmObject)obj;
                    break;
                }
            }
        }

        if (bmObject == null)
        {
            return Optional.empty();
        }

        return Optional.of(
            new BmRoot(filePath, uri, project, model, dtProject, engine, bmObject, projectFileSystemSupportProvider));
    }

    private URI getURI(String filePath)
    {
        Preconditions.checkNotNull(filePath);
        return URI.createPlatformResourceURI(filePath, true).appendFragment("/0"); //$NON-NLS-1$
    }
}
