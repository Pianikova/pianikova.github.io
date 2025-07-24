/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.mcore.AbstractMethod;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.context.DTO.Entity;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesRequest;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class RelatedEntities implements IRelatedEntities
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IEntitiesWalker entitiesWalker;
    private final IIdFactory idFactory;
    private final IEntityFactory entityFactory;
    private final IV8ProjectManager v8ProjectManager;
    private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;
    private final IModuleProvider resourceSetProvider;

    @Inject
    public RelatedEntities(ILog log, IV8Model v8Model, IEntitiesWalker entitiesWalker, IIdFactory idFactory,
        IEntityFactory entityFactory, IV8ProjectManager v8ProjectManager,
        IProjectFileSystemSupportProvider projectFileSystemSupportProvider, IModuleProvider resourceSetProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(entitiesWalker);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(entityFactory);
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(projectFileSystemSupportProvider);
        Preconditions.checkNotNull(resourceSetProvider);
        this.log = log;
        this.v8Model = v8Model;
        this.entitiesWalker = entitiesWalker;
        this.idFactory = idFactory;
        this.entityFactory = entityFactory;
        this.v8ProjectManager = v8ProjectManager;
        this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
        this.resourceSetProvider = resourceSetProvider;
    }

    @SuppressWarnings("nls")
    @Override
    public Optional<RelatedEntitiesResponse> getRelatedEntities(RelatedEntitiesRequest request,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(request);
        if (request.path == null || request.path.isBlank())
        {
            return Optional.empty();
        }

        var response = new RelatedEntitiesResponse();
        response.relatedObjects = new ArrayList<>();
        response.relatedFunctions = new ArrayList<>();
        response.localFunctions = new ArrayList<>();
        var entities = new HashSet<Entity>();
        var objects = new ArrayList<IBmObject>();
        var result =
            entitiesWalker.walk(null, request.path, request.start, request.finish, resourceSetProvider,
                new EntityVisitor()
                {
                    @Override
                    public boolean visitModule(BmRoot root, Module module)
            {
                var project = v8ProjectManager.getProject(module);
                if (project == null)
                {
                    return false;
                }

                var fileSystemSupport =
                    projectFileSystemSupportProvider.getProjectFileSystemSupport(project.getDtProject());
                var moduleFile = fileSystemSupport.getFile(module);
                try (var reader =
                    new BufferedReader(new InputStreamReader(moduleFile.getContents(), moduleFile.getCharset())))
                {
                    var code = new StringBuilder();
                    var charBuffer = CharBuffer.allocate(1024);
                    int size;
                    do
                    {
                        size = reader.read(charBuffer);
                        if (size <= 0)
                        {
                            break;
                        }

                        code.append(charBuffer.array(), 0, size);
                        charBuffer.clear();
                    }
                    while (true);
                    response.code = code.toString();
                }
                catch (Exception error)
                {
                    log.logError(error);
                }

                return false;
            }

                    @Override
                    public boolean visitBmObject(BmRoot root, IBmObject owner)
                    {
                        objects.add(owner);
                        return false;
                    }

            @Override
                    public boolean visitForm(BmRoot root, Form form)
            {
                entityFactory.createFormEntity(form, cancellationToken).ifPresent(i -> response.form = i);
                return false;
            }

            @Override
                    public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node)
            {
                var entity = createEntity(request.path, nodeId, variable, node, cancellationToken);
                if (!entities.add(entity))
                {
                    return false;
                }

                response.relatedObjects.add(entity);
                traceEntity("object", entity, variable, node); //$NON-NLS-1$
                return false;
            }

            @Override
                    public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess,
                ICompositeNode node)
            {
                var entity = createEntity(request.path, nodeId, featureAccess, node, cancellationToken);
                if (!entities.add(entity))
                {
                    return false;
                }

                for (var featureEntry : v8Model.getFeatureEntries(featureAccess))
                {
                    if (cancellationToken.isCanceled())
                    {
                        break;
                    }

                    var feature = featureEntry.getFeature();
                    if (feature instanceof AbstractMethod)
                    {
                        return false;
                    }

                    if (feature instanceof Method)
                    {
                        return false;
                    }
                }

                response.relatedObjects.add(entity);
                traceEntity("object", entity, featureAccess, node);
                return false;
            }

            @Override
                    public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation,
                ICompositeNode node)
            {
                var entity = createEntity(request.path, nodeId, invocation, node, cancellationToken);
                if (!entities.add(entity))
                {
                    return false;
                }

                response.relatedFunctions.add(entity);
                traceEntity("function", entity, invocation, node);
                return false;
            }
        }, IStatistics.Empty, cancellationToken);

        var meta = entityFactory.createMetaEntity(objects, cancellationToken);
        if (!meta.isEmpty())
        {
            response.meta = meta.get(meta.size() - 1);
        }
        else
        {
            response.meta = null;
        }

        entitiesWalker.walk(null, request.path, 0, Integer.MAX_VALUE, resourceSetProvider, new EntityVisitor()
        {
            @Override
            public boolean visitMethod(BmRoot root, String nodeId, Method method, ICompositeNode node)
            {
                entityFactory.createMethodEntity(method, node, true, cancellationToken)
                    .ifPresent(i -> response.localFunctions.add(i));
                return false;
            }
        }, IStatistics.Empty, cancellationToken);

        if (!result)
        {
            return Optional.empty();
        }

        return Optional.of(response);
    }

    private Entity createEntity(String path, String nodeId, EObject eObject, ICompositeNode node,
        ICancellationToken cancellationToken)
    {
        var entity = new Entity();
        entity.uuid = idFactory.createObjectId(path, eObject, cancellationToken);
        entity.ref = nodeId;
        entity.start = node.getTotalOffset();
        entity.finish = node.getTotalEndOffset();
        return entity;
    }

    @SuppressWarnings("nls")
    private void traceEntity(String type, Entity entity, EObject eObject, ICompositeNode node)
    {
        log.trace(type + ": " + entity, () -> {
            var sb = new StringBuilder();
            sb.append("Node type:");
            sb.append(eObject.getClass().getName());
            sb.append(System.lineSeparator());
            sb.append("Code:");
            sb.append(System.lineSeparator());
            sb.append(node.getText());
            return sb.toString();
        });
    }
}
