/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextEntities;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.assistent.model.LocalContext;
import org.e1c.edt.ai.context.DTO.EntityInfoRequest;
import org.e1c.edt.ai.context.DTO.EntityInfoResponse;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
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
    public Optional<EntityInfoResponse> getInfo(EntityInfoRequest request, ICancellationToken cancellationToken)
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
                response.form = entityFactory.createFormEntity(form, cancellationToken).orElse(null);
            }

            @Override
            public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var objectEntity = entityFactory.crateObjectEntity(variable, node, cancellationToken);
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

                var objectEntity = entityFactory.crateObjectEntity(featureAccess, node, cancellationToken);
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

                var methodEntity = entityFactory.createMethodEntity(invocation, node, cancellationToken);
                response.method = methodEntity.orElse(null);
                return methodEntity.isPresent();
            }

            @Override
            public boolean visitMethod(String nodeId, Method method, ICompositeNode node)
            {
                // TODO Auto-generated method stub
                return false;
            }
        }, cancellationToken);

        if (!result)
        {
            log.trace("Entity not found", request.ref);
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Override
    public Duration fill(AIContext aiContext, LocalContext context, ICancellationToken cancellationToken)
    {
        var stopwatch = Stopwatch.createStarted();
        var formStopwatch = Stopwatch.createUnstarted();
        var trace = new StringBuilder();
        try
        {
            var filePath = aiContext.getPath();
            var start = aiContext.getStart();
            var finish = aiContext.getFinish();
            context.relatedObjects = new ArrayList<>();
            context.relatedFunctions = new ArrayList<>();
            context.localFunctions = new ArrayList<>();
            var uuids = new HashSet<String>();
            entitiesWalker.walk(filePath, start, finish, new IEntityVisitor()
            {
                @Override
                public void visitForm(Form form)
                {
                    formStopwatch.start();
                    try
                    {
                        context.form = entityFactory.createFormEntity(form, cancellationToken).orElse(null);
                    }
                    finally
                    {
                        formStopwatch.stop();
                    }
                }

                @Override
                public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
                {
                    if (!uuids.add(idFactory.createObjectId(filePath, variable, cancellationToken)))
                    {
                        return false;
                    }

                    entityFactory.crateObjectEntity(variable, node, cancellationToken)
                        .ifPresent(object -> context.relatedObjects.add(object));
                    return false;
                }

                @Override
                public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
                {
                    if (!uuids.add(idFactory.createObjectId(filePath, featureAccess, cancellationToken)))
                    {
                        return false;
                    }

                    entityFactory.crateObjectEntity(featureAccess, node, cancellationToken)
                        .ifPresent(object -> context.relatedObjects.add(object));
                    return false;
                }

                @Override
                public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
                {
                    if (!uuids.add(idFactory.createObjectId(filePath, invocation, cancellationToken)))
                    {
                        return false;
                    }

                    entityFactory.createMethodEntity(invocation, node, cancellationToken)
                        .ifPresent(method -> context.relatedFunctions.add(method));
                    return false;
                }

                @Override
                public boolean visitMethod(String nodeId, Method method, ICompositeNode node)
                {
                    return false;
                }
            }, cancellationToken);
        }
        finally
        {
            stopwatch.stop(); // optional
            trace.append("objects count: "); //$NON-NLS-1$
            trace.append(context.relatedObjects.size());
            trace.append(System.lineSeparator());
            trace.append("methods count: "); //$NON-NLS-1$
            trace.append(context.relatedFunctions.size());
            trace.append(System.lineSeparator());
            trace.append("form duration: "); //$NON-NLS-1$
            trace.append(formStopwatch.elapsed());
            trace.append(System.lineSeparator());
            trace.append("total duration: "); //$NON-NLS-1$
            trace.append(stopwatch.elapsed());
            log.trace("AI context statistics " + cancellationToken, trace.toString()); //$NON-NLS-1$
        }

        return stopwatch.elapsed();
    }
}
