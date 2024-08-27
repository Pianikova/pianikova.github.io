/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.IContextEntities;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.assistent.model.LocalContext;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EntityInfo
    implements IEntityInfo, IContextEntities
{
    private final ILog log;
    private final IEntitiesWalker entitiesWalker;
    private final IIdFactory idFactory;
    private final IEntityFactory entityFactory;

    @Inject
    public EntityInfo(ILog log, IEntitiesWalker entitiesWalker, IIdFactory idFactory, IEntityFactory entityFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(entitiesWalker);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(entityFactory);
        this.log = log;
        this.entitiesWalker = entitiesWalker;
        this.idFactory = idFactory;
        this.entityFactory = entityFactory;
    }

    @SuppressWarnings("nls")
    @Override
    public Optional<EntityInfoResponse> geInfo(EntityInfoRequest request)
    {
        Preconditions.checkNotNull(request);
        if (request.ref == null || request.ref.isBlank())
        {
            return Optional.empty();
        }

        var nodeIdOptional = idFactory.getNodeId(request.ref);
        if (nodeIdOptional.isEmpty())
        {
            return Optional.empty();
        }

        var nodeId = nodeIdOptional.get();
        var response = new EntityInfoResponse();
        response.ref = request.ref;
        var result = entitiesWalker.walk(nodeId.getPath(), nodeId.getStart(), nodeId.getFinish(), new IEntityVisitor()
        {
            @Override
            public void visitForm(Form form)
            {
                response.form = entityFactory.createFormEntity(form).orElse(null);
            }

            @Override
            public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var objectEntity = entityFactory.crateObjectEntity(variable, node);
                response.object = objectEntity.orElse(null);
                return objectEntity.isPresent();
            }

            @Override
            public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var objectEntity = entityFactory.crateObjectEntity(featureAccess, node);
                response.object = objectEntity.orElse(null);
                return objectEntity.isPresent();
            }

            @Override
            public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var methodEntity = entityFactory.createMethodEntity(invocation);
                response.method = methodEntity.orElse(null);
                return methodEntity.isPresent();
            }
        });

        if (!result)
        {
            log.trace("Entity not found", request.ref);
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Override
    public void fill(AIContext aiContext, LocalContext context)
    {
        var filePath = aiContext.getPath();
        var start = aiContext.getStart();
        var finish = aiContext.getFinish();
        context.relatedObjects = new ArrayList<>();
        context.relatedFunctions = new ArrayList<>();
        var uuids = new HashSet<String>();
        entitiesWalker.walk(filePath, start, finish, new IEntityVisitor()
        {
            @Override
            public void visitForm(Form form)
            {
                context.form = entityFactory.createFormEntity(form).orElse(null);
            }

            @Override
            public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
            {
                if (!uuids.add(idFactory.createObjectId(filePath, variable)))
                {
                    return false;
                }

                entityFactory.crateObjectEntity(variable, node).ifPresent(object -> context.relatedObjects.add(object));
                return false;
            }

            @Override
            public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
            {
                if (!uuids.add(idFactory.createObjectId(filePath, featureAccess)))
                {
                    return false;
                }

                entityFactory.crateObjectEntity(featureAccess, node)
                    .ifPresent(object -> context.relatedObjects.add(object));
                return false;
            }

            @Override
            public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
            {
                if (!uuids.add(idFactory.createObjectId(filePath, invocation)))
                {
                    return false;
                }

                entityFactory.createMethodEntity(invocation).ifPresent(method -> context.relatedFunctions.add(method));
                return false;
            }
        });
    }
}
