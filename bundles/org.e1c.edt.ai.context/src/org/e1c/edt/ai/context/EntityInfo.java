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
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.StatisticsType;
import org.e1c.edt.ai.assistent.model.LocalContext;
import org.e1c.edt.ai.context.DTO.EntityInfoRequest;
import org.e1c.edt.ai.context.DTO.EntityInfoResponse;
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

    @Inject
    public EntityInfo(ILog log, IEntitiesWalker entitiesWalker, IIdFactory idFactory, IEntityFactory entityFactory,
        IUISettings uiSettings)
    {
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
        var stopwatch = Stopwatch.createStarted();
        var filePath = aiContext.getPath();
        var start = aiContext.getStart();
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
        entitiesWalker.walk(filePath, start, finish, new EntityVisitor()
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

                try (var measurement = statistics.measureDuration(StatisticsType.RELATED_OBJECTS))
                {
                    if (!uuids.add(idFactory.createObjectId(filePath, variable, cancellationToken)))
                    {
                        return false;
                    }

                    entityFactory.crateObjectEntity(variable, node, cancellationToken)
                        .ifPresent(object -> context.relatedObjects.add(object));
                }
                catch (Exception error)
                {
                    log.logError(error);
                }

                return false;
            }

            @Override
            public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
            {
                if (!uiSettings.sendContext())
                {
                    return false;
                }

                try (var measurement = statistics.measureDuration(StatisticsType.RELATED_OBJECTS))
                {
                    if (!uuids.add(idFactory.createObjectId(filePath, featureAccess, cancellationToken)))
                    {
                        return false;
                    }

                    entityFactory.crateObjectEntity(featureAccess, node, cancellationToken)
                        .ifPresent(object -> context.relatedObjects.add(object));
                }
                catch (Exception error)
                {
                    log.logError(error);
                }

                return false;
            }

            @Override
            public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
            {
                if (!uiSettings.sendContext())
                {
                    return false;
                }

                try (var measurement = statistics.measureDuration(StatisticsType.RELATED_FUNCTIONS))
                {
                    if (!uuids.add(idFactory.createObjectId(filePath, invocation, cancellationToken)))
                    {
                        return false;
                    }

                    entityFactory.createMethodEntity(invocation, node, cancellationToken)
                        .ifPresent(method -> context.relatedFunctions.add(method));
                }
                catch (Exception error)
                {
                    log.logError(error);
                }
                return false;
            }

            @Override
            public boolean visitMethod(String nodeId, Method method, ICompositeNode node)
            {
                try (var measurement = statistics.measureDuration(StatisticsType.LOCAL_FUNCTIONS))
                {
                    entityFactory.createMethodEntity(method, node, cancellationToken)
                        .ifPresent(i -> context.localFunctions.add(i));
                }
                catch (Exception error)
                {
                    log.logError(error);
                }

                return false;
            }
        }, statistics, cancellationToken);

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
}
