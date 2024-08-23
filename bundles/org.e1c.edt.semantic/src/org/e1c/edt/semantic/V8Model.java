/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.CancelIndicator;

import com._1c.g5.v8.dt.bm.xtext.BmAwareResourceSetProvider;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslCommentUtils;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.BslContextDefPackage;
import com._1c.g5.v8.dt.bsl.model.BslPackage;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.SourceObjectLinkProvider;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.typesytem.TypeSystemMode;
import com._1c.g5.v8.dt.bsl.model.typesytem.VariableTypeStateProviderCollector;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.Environments;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class V8Model implements IV8Model
{
    private static final String RESOURCE_PREFIX = "/resource"; //$NON-NLS-1$
    private final ILog log;
    private final BslMultiLineCommentDocumentationProvider commentDocumentationProvider;

    @Inject
    public V8Model(ILog log, BslMultiLineCommentDocumentationProvider commentDocumentationProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(commentDocumentationProvider);
        this.log = log;
        this.commentDocumentationProvider = commentDocumentationProvider;
    }

    @Override
    public Optional<Module> getModule(String filePath)
    {
        Preconditions.checkNotNull(filePath);
        IPath path = new Path(filePath);
        var modules = new ArrayList<Module>();
        for (var project : getProjects())
        {
            var uriVisitor = new IFileVisitor()
            {
                @Override
                public void visit(IFile file)
                {
                    if (!modules.isEmpty())
                    {
                        return;
                    }

                    if (!path.equals(file.getFullPath()))
                    {
                        return;
                    }

                    var moduleUri =
                        URI.createPlatformResourceURI(file.getFullPath().toPortableString(), true).appendFragment("/0"); //$NON-NLS-1$
                    var resourceSetProvider = getResourceService(BmAwareResourceSetProvider.class);
                    var resourceSet = resourceSetProvider.get(file.getProject());
                    if (resourceSet == null)
                    {
                        return;
                    }

                    var module = resourceSet.getEObject(moduleUri, true);
                    if (module == null || !(module instanceof Module))
                    {
                        return;
                    }

                    var moduleResource = module.eResource();
                    if (moduleResource instanceof BslResource)
                    {
                        ((BslResource)moduleResource).setDeepAnalysis(true);
                    }

                    EcoreUtil2.resolveLazyCrossReferences(moduleResource, CancelIndicator.NullImpl);
                    modules.add((Module)module);
                }
            };

            try
            {
                walkFiles(project, uriVisitor);
            }
            catch (CoreException e)
            {
                log.logError(e);
            }

            if (!modules.isEmpty())
            {
                break;
            }
        }

        if (modules.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.of(modules.get(0));
    }

    @Override
    public Optional<Type> getLastType(VariableTypeStateProviderCollector typeStateProviders)
    {
        if (typeStateProviders == null)
        {
            return Optional.empty();
        }

        var typeStateProvider = typeStateProviders.get(TypeSystemMode.NORMAL);
        if (typeStateProvider == null)
        {
            return Optional.empty();
        }

        Type lastType = null;
        for (var state : typeStateProvider.getAll())
        {
            for (var type : state.getTypes())
            {
                if (type instanceof Type)
                {
                    lastType = (Type)type;
                }
            }
        }

        return Optional.ofNullable(lastType);
    }

    @Override
    public List<TypeItem> getTypes(EObject eObject)
    {
        return getResourceService(TypesComputer.class).computeTypes(eObject,
            getEnvironments(eObject));
    }

    @Override
    public List<FeatureEntry> getFeatureEntries(FeatureAccess featureAccess)
    {
        if (featureAccess != null)
        {
            if (featureAccess instanceof DynamicFeatureAccess)
            {
                var dynamicFeatureAccess = (DynamicFeatureAccess)featureAccess;
                var envs = EcoreUtil2.getContainerOfType(dynamicFeatureAccess, Environmental.class).environments();
                var dynamicComputer = getResourceService(DynamicFeatureAccessComputer.class);
                return dynamicComputer.getLastObject(dynamicFeatureAccess, envs, true);
            }

            if (featureAccess instanceof StaticFeatureAccess)
            {
                return ((StaticFeatureAccess)featureAccess).getFeatureEntries();
            }
        }

        return new ArrayList<>();
    }

    @Override
    public Optional<String> getPath(FeatureAccess featureAccess)
    {
        if (featureAccess == null)
        {
            return Optional.empty();
        }

        if (featureAccess instanceof DynamicFeatureAccess)
        {
            var dynamicFeatureAccess = (DynamicFeatureAccess)featureAccess;
            var envs = EcoreUtil2.getContainerOfType(dynamicFeatureAccess, Environmental.class).environments();
            var dynamicComputer = getResourceService(DynamicFeatureAccessComputer.class);
            var features = dynamicComputer.getLastObject(dynamicFeatureAccess, envs, true);
            return getPath(features);
        }

        if (featureAccess instanceof StaticFeatureAccess)
        {
            return getPath(((StaticFeatureAccess)featureAccess).getFeatureEntries());
        }

        return Optional.empty();
    }

    @Override
    public TypesComputer getTypesComputer()
    {
        return getResourceService(TypesComputer.class);
    }

    @Override
    public List<String> getComment(EObject eObject)
    {
        return commentDocumentationProvider.getCommentLines(eObject);
    }

    @Override
    public BslDocumentationComment getComment(Method method, boolean oldFormat)
    {
        return BslCommentUtils.parseTemplateComment(method, oldFormat, commentDocumentationProvider);
    }

    @Override
    public BslDocumentationComment getComment(BslContextDefMethod method, boolean oldFormat)
    {
        return BslCommentUtils.parseTemplateComment(method, oldFormat);
    }

    private Optional<String> getPath(List<FeatureEntry> features)
    {
        for (FeatureEntry uniqueFeature : features)
        {
            var feature = uniqueFeature.getFeature();
            if (feature == null)
            {
                continue;
            }

            var ePackage = feature.eClass().getEPackage();
            if (ePackage == BslPackage.eINSTANCE && !feature.eIsProxy())
            {
                return getPath(EcoreUtil.getURI(feature));
            }

            if (ePackage != BslContextDefPackage.eINSTANCE)
            {
                continue;
            }

            if (!(feature instanceof SourceObjectLinkProvider))
            {
                continue;
            }

            return getPath(((SourceObjectLinkProvider)feature).getSourceUri());
        }

        return Optional.empty();
    }

    private Optional<String> getPath(URI baseUri)
    {
        if (baseUri == null)
        {
            return Optional.empty();
        }

        var path = baseUri.path();
        if (path == null || path.isBlank())
        {
            return Optional.empty();
        }

        if (path.startsWith(RESOURCE_PREFIX))
        {
            path = path.substring(RESOURCE_PREFIX.length());
        }

        return Optional.ofNullable(path);
    }

    private IProject[] getProjects()
    {
        var root = ResourcesPlugin.getWorkspace().getRoot();
        return root.getProjects();
    }

    private void walkFiles(IProject project, IFileVisitor visitor) throws CoreException
    {
        project.accept(resource -> {
            if (resource instanceof IFile)
            {
                IFile file = (IFile)resource;
                if ("bsl".equalsIgnoreCase(file.getFileExtension())) //$NON-NLS-1$
                {
                    visitor.visit(file);
                }
            }
            else if (resource instanceof IFolder || resource instanceof IProject)
            {
                return true;
            }

            return false;
        });
    }

    private static <T> T getResourceService(Class<T> type)
    {
        IResourceServiceProvider resourceServiceProvider =
            IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(URI.createFileURI("*.bsl")); //$NON-NLS-1$
        return resourceServiceProvider.get(type);
    }

    @Override
    public Environments getEnvironments(EObject eObject)
    {
        var environmental = EcoreUtil2.getContainerOfType((Expression)eObject, Environmental.class);
        if (environmental != null)
        {
            return environmental.environments();
        }

        return Environments.EMPTY;
    }

    @Override
    public ICompositeNode getNode(EObject eObject)
    {
        var obj = eObject;
        while (obj != null)
        {
            var node = NodeModelUtils.getNode(obj);
            if (node != null)
            {
                return node;
            }

            obj = obj.eContainer();
        }

        return null;
    }

    @Override
    public Optional<Type> getType(FeatureAccess featureAccess)
    {
        if (featureAccess == null)
        {
            return Optional.empty();
        }

        return getType(featureAccess, getTypesComputer().compute(featureAccess, getEnvironments(featureAccess)));
    }

    private Optional<Type> getType(EObject contextObject, List<TypeItem> typeItems)
    {
        for (var typeItem : typeItems)
        {
            var type = getType(contextObject, typeItem);
            if (type.isPresent())
            {
                return type;
            }
        }

        return Optional.empty();
    }

    private Optional<Type> getType(EObject contextObject, TypeItem typeItem)
    {
        if (typeItem instanceof Type)
        {
            var type = (Type)typeItem;
            if (type.eIsProxy())
            {
                var proxy = (TypeItem)EcoreUtil.resolve(type, contextObject);
                return getType(type, proxy);
            }

            return Optional.ofNullable(type);
        }
        else
        {
            for (var ref : typeItem.eCrossReferences())
            {
                if (ref instanceof Type)
                {
                    Optional.ofNullable((Type)ref);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<EObject> getMethodFeature(FeatureAccess methodAccess)
    {
        if (methodAccess == null)
        {
            return Optional.empty();
        }

        var modules = new ArrayList<com._1c.g5.v8.dt.bsl.model.Module>();
        getPath(methodAccess).ifPresent(path -> {
            getModule(path).ifPresent(module -> modules.add(module));
        });

        for (var featureEntry : getFeatureEntries(methodAccess))
        {
            var feature = featureEntry.getFeature();
            if (feature instanceof SourceObjectLinkProvider)
            {
                var sourceLinkProvider = (SourceObjectLinkProvider)feature;
                if (!modules.isEmpty())
                {
                    var methodUri = sourceLinkProvider.getSourceUri().toString();
                    var module = modules.get(0);
                    for (var method : module.allMethods())
                    {
                        if (method.getUniqueName().equals(methodUri))
                        {
                            feature = method;
                            break;
                        }
                    }
                }
            }

            if (feature instanceof Method || feature instanceof com._1c.g5.v8.dt.mcore.Method)
            {
                return Optional.of(feature);
            }
        }

        return Optional.empty();
    }

    private static interface IFileVisitor
    {
        void visit(IFile file);
    }
}
