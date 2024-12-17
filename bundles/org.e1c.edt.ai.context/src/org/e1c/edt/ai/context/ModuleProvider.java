/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.ILog;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.v8.bm.core.IBmExternalUriResolver;
import com._1c.g5.v8.dt.bm.xtext.XtextBmLinkProvider;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ModuleProvider implements IModuleProvider
{
    private final ILog log;
    private final IBmModelManager modelManager;

    @Inject
    public ModuleProvider(ILog log, IBmModelManager modelManager)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(modelManager);
        this.log = log;
        this.modelManager = modelManager;
    }

    @Override
    public synchronized Optional<ModuleInfo> getModule(String filePath, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(filePath);
        Preconditions.checkNotNull(cancellationToken);
        var root = ResourcesPlugin.getWorkspace().getRoot();
        for (var project : root.getProjects())
        {
            if (!project.isOpen())
            {
                continue;
            }

            return getModuleInfo(project, filePath, cancellationToken);
        }

        return Optional.empty();
    }

    @Override
    public Optional<ModuleInfo> getModuleInfo(IDocument document)
    {
        if (document instanceof BslXtextDocument)
        {
            IUnitOfWork<XtextResource, XtextResource> work = res -> res;
            var bslXtextDocument = ((BslXtextDocument)document).readOnlyDataModel(work);
            for (var content : bslXtextDocument.getContents())
            {
                if (content instanceof Module)
                {
                    var module = (Module)content;
                    return Optional.of(new ModuleInfo((Module)content, null));
                }
            }
        }

        return Optional.empty();
    }


    @SuppressWarnings("deprecation")
    private Optional<ModuleInfo> getModuleInfo(IProject project, String filePath, ICancellationToken cancellationToken)
    {
        var bmModel = modelManager.getModel(project);
        if (bmModel == null)
        {
            return Optional.empty();
        }

        for (IBmExternalUriResolver provider : bmModel.getEngine().getExternalUriResolvers())
        {
            if (provider instanceof XtextBmLinkProvider)
            {
                var moduleUri = URI.createPlatformResourceURI(filePath, true).appendFragment("/0"); //$NON-NLS-1$
                var currentModule = ((XtextBmLinkProvider)provider).getObject(moduleUri);
                if (currentModule != null && currentModule instanceof Module)
                {
                    return Optional.of(new ModuleInfo((Module)currentModule, filePath));
                }
            }
        }

        return Optional.empty();
    }
}
