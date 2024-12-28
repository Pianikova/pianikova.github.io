/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.v8.bm.core.IBmExternalUriResolver;
import com._1c.g5.v8.dt.bm.xtext.XtextBmLinkProvider;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ModuleProvider implements IModuleProvider
{
    private final IBmModelManager modelManager;
    private final IUISettings uiSettings;

    @Inject
    public ModuleProvider(IBmModelManager modelManager, IUISettings uiSettings)
    {
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(uiSettings);
        this.modelManager = modelManager;
        this.uiSettings = uiSettings;
    }

    @Override
    public synchronized Optional<ModuleInfo> getModule(String filePath, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(filePath);
        Preconditions.checkNotNull(cancellationToken);
        var root = ResourcesPlugin.getWorkspace().getRoot();
        for (var project : root.getProjects())
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            if (!project.isOpen())
            {
                continue;
            }

            return getModuleInfo(project, filePath, cancellationToken);
        }

        return Optional.empty();
    }

    @Override
    public Optional<ModuleInfo> getModuleInfo(IDocument document, ICancellationToken cancellationToken)
    {
        if (document instanceof BslXtextDocument)
        {
            IUnitOfWork<XtextResource, XtextResource> work = res -> res;
            var bslXtextDocument = ((BslXtextDocument)document).readOnlyDataModel(work);
            for (var content : bslXtextDocument.getContents())
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                if (content instanceof Module)
                {
                    return Optional.of(new ModuleInfo(analyzeModule((Module)content, cancellationToken), null));
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
            if (cancellationToken.isCanceled())
            {
                break;
            }

            if (provider instanceof XtextBmLinkProvider)
            {
                var moduleUri = URI.createPlatformResourceURI(filePath, true).appendFragment("/0"); //$NON-NLS-1$
                var currentModule = ((XtextBmLinkProvider)provider).getObject(moduleUri);
                if (currentModule != null && currentModule instanceof Module)
                {
                    return Optional
                        .of(new ModuleInfo(analyzeModule((Module)currentModule, cancellationToken), filePath));
                }
            }
        }

        return Optional.empty();
    }

    private Module analyzeModule(Module module, ICancellationToken cancellationToken)
    {
        if (!uiSettings.sendContext())
        {
            return module;
        }

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

        return module;
    }
}
