/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextEntities;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.StatisticsType;
import org.e1c.edt.ai.assistent.model.LocalContext;
import org.e1c.edt.ai.context.DTO.EntityInfoRequest;
import org.e1c.edt.ai.context.DTO.EntityInfoResponse;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterResource;
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
    private final IUISettings uiSettings;
    private final Lock lock = new ReentrantLock(true);
    private final IDispatcher dispatcher;

    @Inject
    public EntityInfo(ILog log, IEntitiesWalker entitiesWalker, IIdFactory idFactory, IEntityFactory entityFactory,
        IUISettings uiSettings, IDispatcher dispatcher)
    {
        this.dispatcher = dispatcher;
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(entitiesWalker);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(entityFactory);
        Preconditions.checkNotNull(uiSettings);
        this.log = log;
        this.entitiesWalker = entitiesWalker;
        this.idFactory = idFactory;
        this.entityFactory = entityFactory;
        this.uiSettings = uiSettings;
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
        var result = entitiesWalker.walk(nodeId.getPath(), nodeId.getStart(), nodeId.getFinish(), new EntityVisitor()
        {
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
        }, IStatistics.Empty, cancellationToken);

        if (!result)
        {
            log.trace("Entity not found", request.ref);
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Override
    public Duration fill(AIContext aiContext, LocalContext context, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        lock.lock();
        try
        {
            var timeout = uiSettings.getTimeout();
            return dispatcher.dispatch(() -> fillInternal(aiContext, context, statistics, cancellationToken), timeout)
                .orElse(timeout);
        }
        finally
        {
            lock.unlock();
        }
    }

    private Duration fillInternal(AIContext aiContext, LocalContext context, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var stopwatch = Stopwatch.createStarted();
        var filePath = aiContext.getPath();
        var start = aiContext.getStart();
        var offset = aiContext.getTextOffset();
        var finish = aiContext.getFinish();
        context.relatedObjects = new ArrayList<>();
        context.relatedFunctions = new ArrayList<>();
        context.localFunctions = new ArrayList<>();
        var uuids = new HashSet<String>();
        var attributes = new ArrayList<BasicFeature>();
        var tabularSections = new ArrayList<DbObjectTabularSection>();
        var registerResources = new ArrayList<RegisterResource>();
        var registerDimensions = new ArrayList<RegisterDimension>();
        var registerRecords = new ArrayList<BasicRegister>();
        var actions = new ArrayList<Action>();
        var cursorObjects = new EObject[1];
        entitiesWalker.walk(filePath, start, finish, new EntityVisitor()
        {
            @Override
            public void visitNode(EObject eObject, ICompositeNode node)
            {
                var nodeStart = node.getTotalOffset();
                var nodeFinish = node.getTotalEndOffset();
                if (nodeStart <= offset && offset <= nodeFinish)
                {
                    cursorObjects[0] = eObject;
                }
            }

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
                try (var measurement = statistics.measureDuration(StatisticsType.FORM))
                {
                    context.form = entityFactory.createFormEntity(form, cancellationToken).orElse(null);
                }
                catch (Exception error)
                {
                    log.logError(error);
                }
            }

            @Override
            public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
            {
                if (!uiSettings.sendContext())
                {
                    return false;
                }

                if (!uuids.add(idFactory.createObjectId(filePath, variable, cancellationToken)))
                {
                    return false;
                }

                var action = new Action(node, offset, statistics, StatisticsType.RELATED_OBJECTS,
                    () -> entityFactory.crateObjectEntity(variable, node, cancellationToken)
                        .ifPresent(object -> context.relatedObjects.add(object)));
                actions.add(action);
                return false;
            }

            @Override
            public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
            {
                if (!uiSettings.sendContext())
                {
                    return false;
                }

                var action = new Action(node, offset, statistics, StatisticsType.RELATED_OBJECTS,
                    () -> entityFactory.crateObjectEntity(featureAccess, node, cancellationToken)
                        .ifPresent(object -> context.relatedObjects.add(object)));
                actions.add(action);
                return false;
            }

            @Override
            public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
            {
                if (!uiSettings.sendContext())
                {
                    return false;
                }

                var action = new Action(node, offset, statistics, StatisticsType.RELATED_FUNCTIONS,
                    () -> entityFactory.createMethodEntity(invocation, node, cancellationToken)
                        .ifPresent(method -> context.relatedFunctions.add(method)));
                actions.add(action);
                return false;
            }

            @Override
            public boolean visitMethod(String nodeId, Method method, ICompositeNode node)
            {
                var action = new Action(node, offset, statistics, StatisticsType.LOCAL_FUNCTIONS,
                    () -> entityFactory.createMethodEntity(method, node, cancellationToken)
                        .ifPresent(i -> context.localFunctions.add(i)));
                actions.add(action);
                return false;
            }
        }, statistics, cancellationToken);

        var cursorObject = cursorObjects[0];
        if (cursorObject != null)
        {
            var type = cursorObject.getClass();
            for (var modelInterface : type.getInterfaces())
            {
                if (modelInterface.getName().startsWith("com._1c.g5.v8.dt.bsl.model.")) //$NON-NLS-1$
                {
                    context.cursorObject = modelInterface.getSimpleName();
                    break;
                }
            }
        }

        Collections.sort(actions, new Comparator<Action>()
        {
            @Override
            public int compare(Action left, Action right)
            {
                return left.getPriority() > right.getPriority() ? 1
                    : (left.getPriority() < right.getPriority()) ? -1 : 0;
            }
        });

        try
        {
            for (var action : actions)
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                action.apply();
            }
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        try (var measurement = statistics.measureDuration(StatisticsType.META))
        {
            entityFactory
                .createMetaEntity(attributes, tabularSections, registerResources, registerDimensions, registerRecords,
                    cancellationToken)
                .ifPresent(meta -> context.meta = meta);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        return stopwatch.elapsed();
    }

    private class Action
    {
        private final int priority;
        private final IStatistics statistics;
        private final StatisticsType statisticsType;
        private final Runnable runnable;

        public Action(ICompositeNode node, int offset, IStatistics statistics, StatisticsType statisticsType,
            Runnable runnable)
        {
            this.statistics = statistics;
            this.statisticsType = statisticsType;
            this.runnable = runnable;
            var start = node.getTotalOffset();
            var finish = node.getTotalEndOffset();
            var pr = (offset - start) * (finish - offset);
            if (pr < 0)
            {
                pr *= -1;
            }

            priority = pr;
        }

        public int getPriority()
        {
            return priority;
        }

        public void apply() throws Exception
        {
            try (var measurement = statistics.measureDuration(statisticsType))
            {
                runnable.run();
            }
        }
    }
}
