/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.context.DTO.RelatedEntitiesRequest;
import org.e1c.edt.ai.context.DTO.RelatedEntitiesResponse;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.mcore.AbstractMethod;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterResource;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class RelatedEntities implements IRelatedEntities
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IEntitiesWalker entitiesWalker;
    private final IIdFactory idFactory;
    private final IEntityFactory entityFactory;

    @Inject
    public RelatedEntities(ILog log, IV8Model v8Model, IEntitiesWalker entitiesWalker, IIdFactory idFactory,
        IEntityFactory entityFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(entitiesWalker);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(entityFactory);
        this.log = log;
        this.v8Model = v8Model;
        this.entitiesWalker = entitiesWalker;
        this.idFactory = idFactory;
        this.entityFactory = entityFactory;
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
        var attributes = new ArrayList<BasicFeature>();
        var tabularSections = new ArrayList<DbObjectTabularSection>();
        var registerResources = new ArrayList<RegisterResource>();
        var registerDimensions = new ArrayList<RegisterDimension>();
        var registerRecords = new ArrayList<BasicRegister>();
        var result = entitiesWalker.walk(request.path, request.start, request.finish, new EntityVisitor()
        {
            @Override
            public void visitOwnerAttribute(IBmObject owner, BasicFeature attribute)
            {
                attributes.add(attribute);
            }

            @Override
            public void visitOwnerTabularSection(IBmObject owner, DbObjectTabularSection tabularSection)
            {
                tabularSections.add(tabularSection);
            }

            @Override
            public void visitOwnerResource(IBmObject owner, RegisterResource resource)
            {
                registerResources.add(resource);
            }

            @Override
            public void visitOwnerDimension(IBmObject owner, RegisterDimension dimension)
            {
                registerDimensions.add(dimension);
            }

            @Override
            public void visitOwnerRegisterRecord(IBmObject owner, BasicRegister registerRecord)
            {
                registerRecords.add(registerRecord);
            }

            @Override
            public void visitForm(Form form)
            {
                entityFactory.createFormEntity(form, cancellationToken).ifPresent(i -> response.form = i);
            }

            @Override
            public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
            {
                var entity = createEntity(request.path, nodeId, variable, node, cancellationToken);
                if (!entities.add(entity))
                {
                    return false;
                }

                response.relatedObjects.add(entity);
                traceEntity("object", entity, variable, node);
                return false;
            }

            @Override
            public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
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
            public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
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

        entityFactory
            .createMetaEntity(attributes, tabularSections, registerResources, registerDimensions, registerRecords,
                cancellationToken)
            .ifPresent(meta -> response.meta = meta);

        entitiesWalker.walk(request.path, 0, Integer.MAX_VALUE, new EntityVisitor()
        {
            @Override
            public boolean visitMethod(String nodeId, Method method, ICompositeNode node)
            {
                entityFactory.createMethodEntity(method, node, cancellationToken)
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
        var sb = new StringBuilder();
        sb.append("Node type:");
        sb.append(eObject.getClass().getName());
        sb.append(System.lineSeparator());
        sb.append("Code:");
        sb.append(System.lineSeparator());
        sb.append(node.getText());
        log.trace(type + ": " + entity, sb.toString());
    }
}
