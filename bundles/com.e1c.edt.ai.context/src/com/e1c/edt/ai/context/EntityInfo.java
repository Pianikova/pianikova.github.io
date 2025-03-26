/**
  * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterResource;
import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.AIContextKind;
import com.e1c.edt.ai.DataType;
import com.e1c.edt.ai.Fields;
import com.e1c.edt.ai.FillAction;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IHashTools;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProgramingLanguage;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.StatisticsType;
import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.HashedValue;
import com.e1c.edt.ai.assistent.model.LocalContext;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.context.DTO.EntityInfoRequest;
import com.e1c.edt.ai.context.DTO.EntityInfoResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

class EntityInfo
    implements IEntityInfo, IContextEntities
{
    private final static String MethodNamePrefix = "#/_method/"; //$NON-NLS-1$
    private final static HashSet<CursorLocation> methodHashingParts = new HashSet<>();
    private final ILog log;
    private final IEntitiesWalker entitiesWalker;
    private final IIdFactory idFactory;
    private final IEntityFactory entityFactory;
    private final IUISettings uiSettings;
    private final IDispatcher dispatcher;
    private final IV8ProjectManager v8ProjectManager;
    private final IProgramingLanguage programingLanguage;
    private final Provider<MessageDigest> messageDigestProvider;
    private final IHashTools hashTools;
    private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;
    private final ICodePartsProvider codePartsProvider;
    private final IModuleProvider activeEditorResourceSetProvider;
    private final IModuleProvider baseResourceSetProvider;

    static
    {
        methodHashingParts.add(CursorLocation.Comment);
        methodHashingParts.add(CursorLocation.FunctionName);
        methodHashingParts.add(CursorLocation.FunctionArguments);
    }

    @Inject
    public EntityInfo(ILog log, IEntitiesWalker entitiesWalker, IIdFactory idFactory, IEntityFactory entityFactory,
        IUISettings uiSettings, IDispatcher dispatcher, IV8ProjectManager v8ProjectManager,
        IProgramingLanguage programingLanguage, Provider<MessageDigest> messageDigestProvider,
        IHashTools hashTools, IProjectFileSystemSupportProvider projectFileSystemSupportProvider,
        ICodePartsProvider codePartsProvider, IModuleProvider activeEditorResourceSetProvider,
        @Named("BaseModuleProvider") IModuleProvider baseResourceSetProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(entitiesWalker);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(entityFactory);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(programingLanguage);
        Preconditions.checkNotNull(messageDigestProvider);
        Preconditions.checkNotNull(hashTools);
        Preconditions.checkNotNull(projectFileSystemSupportProvider);
        Preconditions.checkNotNull(codePartsProvider);
        Preconditions.checkNotNull(activeEditorResourceSetProvider);
        Preconditions.checkNotNull(baseResourceSetProvider);
        this.log = log;
        this.entitiesWalker = entitiesWalker;
        this.idFactory = idFactory;
        this.entityFactory = entityFactory;
        this.uiSettings = uiSettings;
        this.dispatcher = dispatcher;
        this.v8ProjectManager = v8ProjectManager;
        this.programingLanguage = programingLanguage;
        this.messageDigestProvider = messageDigestProvider;
        this.hashTools = hashTools;
        this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
        this.codePartsProvider = codePartsProvider;
        this.activeEditorResourceSetProvider = activeEditorResourceSetProvider;
        this.baseResourceSetProvider = baseResourceSetProvider;
    }

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
        var result = entitiesWalker.walk(nodeId.getPath(), nodeId.getStart(), nodeId.getFinish(), activeEditorResourceSetProvider,
            new EntityVisitor()
        {
            @Override
            public boolean visitVariable(ModuleInfo moduleInfo, String nodeId, Variable variable, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var objectEntity = entityFactory.crateObjectEntity(variable, node, true, cancellationToken);
                response.object = objectEntity.orElse(null);
                return objectEntity.isPresent();
            }

            @Override
            public boolean visitFeatureAccess(ModuleInfo moduleInfo, String nodeId, FeatureAccess featureAccess,
                ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var objectEntity = entityFactory.crateObjectEntity(featureAccess, node, true, cancellationToken);
                response.object = objectEntity.orElse(null);
                return objectEntity.isPresent();
            }

            @Override
            public boolean visitInvocation(ModuleInfo moduleInfo, String nodeId, Invocation invocation,
                ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var methodEntity = entityFactory.createMethodEntity(invocation, node, true, cancellationToken);
                response.method = methodEntity.orElse(null);
                return methodEntity.isPresent();
            }
        }, IStatistics.Empty, cancellationToken);

        if (!result)
        {
            log.trace("Entity not found", () -> request.ref, Verbosity.DEFAULT); //$NON-NLS-1$
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Override
    public Duration fill(AIContext aiContext, LocalContext localContext, GlobalContext globalContext,
        Predicate<FillAction> actionFilter, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var curResourceSetProvider =
            aiContext.getKind() == AIContextKind.ActiveEditor ? activeEditorResourceSetProvider : baseResourceSetProvider;
        return fillInternal(aiContext, localContext, globalContext, curResourceSetProvider, statistics, actionFilter,
            cancellationToken);
    }

    private Duration fillInternal(AIContext aiContext, LocalContext localContext, GlobalContext globalContext,
        IModuleProvider resourceSetProvider,
        IStatistics statistics,
        Predicate<FillAction> actionFilter,
        ICancellationToken cancellationToken)
    {
        var buffer = CharBuffer.allocate(1024);
        var stopwatch = Stopwatch.createStarted();
        var filePath = aiContext.getPath();
        var start = aiContext.getStart();
        var offset = aiContext.getTextOffset();
        var finish = aiContext.getFinish();
        var sourceOffset = aiContext.getSourceOffset();
        var aiContextKind = aiContext.getKind();
        localContext.relatedObjects = new ArrayList<>();
        localContext.relatedFunctions = new ArrayList<>();
        globalContext.localFunctions = new HashMap<>();
        globalContext.localFunctionsEntities = new HashMap<>();
        var uuids = new HashSet<String>();
        var attributes = new ArrayList<BasicFeature>();
        var tabularSections = new ArrayList<DbObjectTabularSection>();
        var registerResources = new ArrayList<RegisterResource>();
        var registerDimensions = new ArrayList<RegisterDimension>();
        var registerRecords = new ArrayList<BasicRegister>();
        var actions = new ArrayList<Action>();
        var cursorObjects = new EObject[1];
        var owners = new ArrayList<IBmObject>();
        programingLanguage.getFromPath(filePath).ifPresent(lang -> localContext.programingLanguage = lang);
        entitiesWalker.walk(filePath, start, finish, resourceSetProvider, new EntityVisitor()
        {
            @Override
            public boolean visitModule(ModuleInfo moduleInfo)
            {
                if (!actionFilter.test(new FillAction(DataType.HASH, Fields.CONFIGURATION_NAME, null)))
                {
                    return false;
                }

                var module = moduleInfo.getModule();
                if (module == null)
                {
                    return false;
                }

                var project = v8ProjectManager.getProject(module);
                if (project != null)
                {
                    localContext.scriptLanguage = project.getScriptVariant().getName();
                    if (actionFilter.test(new FillAction(DataType.DATA, Fields.CONFIGURATION_NAME, ""))) //$NON-NLS-1$
                    {
                        if (project instanceof IExtensionProject)
                        {
                            var extensionProject = (IExtensionProject)project;
                            var parentProject = extensionProject.getParentProject();
                            globalContext.configurationName = parentProject.getName();
                        }
                    }
                }

                return false;
            }

            @Override
            public boolean visitNode(ModuleInfo moduleInfo, EObject eObject, ICompositeNode node)
            {
                var nodeStart = node.getTotalOffset();
                var nodeFinish = node.getTotalEndOffset();
                if (nodeStart <= offset && offset <= nodeFinish)
                {
                    cursorObjects[0] = eObject;
                }

                return false;
            }

            @Override
            public boolean visitOwner(ModuleInfo moduleInfo, IBmObject owner)
            {
                owners.add(owner);
                return false;
            }

            @Override
            public boolean visitOwnerAttribute(ModuleInfo moduleInfo, IBmObject owner, BasicFeature attribute)
            {
                if (caluclateMetadataHash(moduleInfo, attribute))
                {
                    return true;
                }

                if (actionFilter.test(new FillAction(DataType.DATA, Fields.META, globalContext.meta)))
                {
                    attributes.add(attribute);
                }

                return false;
            }

            @Override
            public boolean visitOwnerTabularSection(ModuleInfo moduleInfo, IBmObject owner,
                DbObjectTabularSection tabularSection)
            {
                if (caluclateMetadataHash(moduleInfo, tabularSection))
                {
                    return true;
                }

                if (actionFilter.test(new FillAction(DataType.DATA, Fields.META, globalContext.meta)))
                {
                    tabularSections.add(tabularSection);
                }

                return false;
            }

            @Override
            public boolean visitOwnerResource(ModuleInfo moduleInfo, IBmObject owner, RegisterResource resource)
            {
                if (caluclateMetadataHash(moduleInfo, resource))
                {
                    return true;
                }

                if (actionFilter.test(new FillAction(DataType.DATA, Fields.META, globalContext.meta)))
                {
                    registerResources.add(resource);
                }

                return false;
            }

            @Override
            public boolean visitOwnerDimension(ModuleInfo moduleInfo, IBmObject owner, RegisterDimension dimension)
            {
                if (caluclateMetadataHash(moduleInfo, dimension))
                {
                    return true;
                }

                if (actionFilter.test(new FillAction(DataType.DATA, Fields.META, globalContext.meta)))
                {
                    registerDimensions.add(dimension);
                }

                return false;
            }

            @Override
            public boolean visitOwnerRegisterRecord(ModuleInfo moduleInfo, IBmObject owner,
                BasicRegister registerRecord)
            {
                if (caluclateMetadataHash(moduleInfo, registerRecord))
                {
                    return true;
                }

                if (actionFilter.test(new FillAction(DataType.DATA, Fields.META, globalContext.meta)))
                {
                    registerRecords.add(registerRecord);
                }

                return false;
            }

            @Override
            public boolean visitForm(ModuleInfo moduleInfo, Form form)
            {
                try (var measurement = statistics.measureDuration(StatisticsType.FORM_DURATUION))
                {
                    if (!actionFilter.test(new FillAction(DataType.HASH, Fields.FORM, null)))
                    {
                        return false;
                    }

                    getFile(moduleInfo, form).map(file -> {
                        globalContext.formPath = file.getFullPath().makeRelative().toPortableString();
                        try
                        {
                            return hashTools.compute(file, buffer);
                        }
                        catch (Exception error)
                        {
                            log.logError(error);
                            return null;
                        }
                    }).ifPresent(hash -> globalContext.form = hashTools.format(hash, true));

                    if (actionFilter.test(new FillAction(DataType.DATA, Fields.FORM, globalContext.form)))
                    {
                        entityFactory.createFormEntity(form, cancellationToken)
                            .ifPresent(enity -> globalContext.formEntity = enity);
                    }
                }
                catch (Exception error)
                {
                    log.logError(error);
                }

                return false;
            }

            @Override
            public boolean visitVariable(ModuleInfo moduleInfo, String nodeId, Variable variable, ICompositeNode node)
            {
                if (!actionFilter.test(new FillAction(DataType.DATA, Fields.RELATED_OBJECTS, null)))
                {
                    return false;
                }

                if (!uuids.add(idFactory.createObjectId(filePath, variable, cancellationToken)))
                {
                    return false;
                }

                var action = new Action(node, offset, statistics, StatisticsType.RELATED_OBJECTS_DURATUION,
                    () -> entityFactory.crateObjectEntity(variable, node, false, cancellationToken)
                        .ifPresent(object -> localContext.relatedObjects.add(object)));
                actions.add(action);
                return false;
            }

            @Override
            public boolean visitFeatureAccess(ModuleInfo moduleInfo, String nodeId, FeatureAccess featureAccess,
                ICompositeNode node)
            {
                if (!actionFilter.test(new FillAction(DataType.DATA, Fields.RELATED_OBJECTS, null)))
                {
                    return false;
                }

                var action = new Action(node, offset, statistics, StatisticsType.RELATED_OBJECTS_DURATUION,
                    () -> entityFactory.crateObjectEntity(featureAccess, node, false, cancellationToken)
                        .ifPresent(object -> localContext.relatedObjects.add(object)));
                actions.add(action);
                return false;
            }

            @Override
            public boolean visitInvocation(ModuleInfo moduleInfo, String nodeId, Invocation invocation,
                ICompositeNode node)
            {
                if (!actionFilter.test(new FillAction(DataType.DATA, Fields.RELATED_FUNCTIONS, null)))
                {
                    return false;
                }

                var action = new Action(node, offset, statistics, StatisticsType.RELATED_FUNCTIONS_DURATUION,
                    () -> entityFactory.createMethodEntity(invocation, node, false, cancellationToken)
                        .ifPresent(method -> localContext.relatedFunctions.add(method)));
                actions.add(action);
                return false;
            }

            @Override
            public boolean visitMethod(ModuleInfo moduleInfo, String nodeId, Method method, ICompositeNode node)
            {
                if (aiContextKind != AIContextKind.ActiveEditor && !method.isExport())
                {
                    return false;
                }

                var uniqueName = method.getUniqueName();
                var prefixIndex = uniqueName.indexOf(MethodNamePrefix);
                if (prefixIndex >= 0)
                {
                    uniqueName = uniqueName.substring(prefixIndex + MethodNamePrefix.length());
                }

                final var methodName = uniqueName;
                var field = Fields.LOCAL_FUNCTIONS + '.' + methodName;
                if (aiContextKind == AIContextKind.ActiveEditor && sourceOffset >= node.getTotalOffset()
                    && sourceOffset <= node.getTotalEndOffset())
                {
                    localContext.currenMethodName = methodName;
                }

                if (!actionFilter.test(new FillAction(DataType.HASH, Fields.LOCAL_FUNCTIONS, null))
                    && !actionFilter.test(new FillAction(DataType.HASH, field, null)))
                {
                    return false;
                }

                var hash = messageDigestProvider.get();
                codePartsProvider.getParts(node)
                    .filter(part -> methodHashingParts.contains(part.getLocation()))
                    .flatMapToInt(i -> i.getText().codePoints())
                    .filter(ch -> !Character.isWhitespace(ch))
                    .forEach(ch -> hash.update((byte)ch));

                var hashStr = hashTools.format(hash, true);
                globalContext.localFunctions.put(methodName, hashStr);

                if (actionFilter.test(new FillAction(DataType.DATA, field, hashStr)))
                {
                    var action = new Action(node, offset, statistics, StatisticsType.LOCAL_FUNCTIONS_DURATUION,
                        () -> entityFactory.createMethodEntity(method, node, false, cancellationToken)
                            .ifPresent(
                                entity -> globalContext.localFunctionsEntities.put(methodName,
                                    new HashedValue<>(entity, hash))));
                    actions.add(action);
                }

                return false;
            }

            private boolean caluclateMetadataHash(ModuleInfo moduleInfo, EObject metadata)
            {
                if (globalContext.meta == null)
                {
                    if (!actionFilter.test(new FillAction(DataType.HASH, Fields.META, null)))
                    {
                        return true;
                    }

                    globalContext.meta =
                        getFile(moduleInfo, metadata).map(file -> {
                            globalContext.metaPath = file.getFullPath().makeRelative().toPortableString();
                            try
                            {
                                return hashTools.compute(file, buffer);
                            }
                            catch (Exception error)
                            {
                                log.logError(error);
                                return null;
                            }
                        }).map(hash -> hashTools.format(hash, true)).orElse(null);
                }

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
                    localContext.cursorObject = modelInterface.getSimpleName();
                    break;
                }
            }

            entityFactory.getEnvironments(cursorObject).ifPresent(areas -> localContext.cursorEnvironments = areas);
            entityFactory.getAreas(cursorObject).ifPresent(areas -> localContext.cursorAreas = areas);
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

        var unptocessedItems = actions.size();
        try
        {
            for (int i = 0; i < actions.size(); i++)
            {
                var action = actions.get(i);
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                action.apply();
                unptocessedItems--;
            }
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        statistics.registerInteger(StatisticsType.UNPROCESSED_ITEMS, unptocessedItems);

        if (!owners.isEmpty() && actionFilter.test(new FillAction(DataType.DATA, Fields.META, null)))
        {
            try (var measurement = statistics.measureDuration(StatisticsType.META_DURATUION))
            {
                entityFactory
                    .createMetaEntity(attributes, tabularSections, registerResources, registerDimensions,
                        registerRecords, cancellationToken)
                    .ifPresent(entity -> globalContext.metaEntity = entity);
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }

        return stopwatch.elapsed();
    }

    private Optional<IFile> getFile(ModuleInfo moduleInfo, EObject obj)
    {
        var module = moduleInfo.getModule();
        if (module == null)
        {
            return Optional.empty();
        }

        var project = v8ProjectManager.getProject(module);
        if (project == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(
            projectFileSystemSupportProvider.getProjectFileSystemSupport(project.getDtProject()).getFile(obj));
    }

    @Override
    public void fill(AIContext aiContext, ChatContext context, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var timeout = uiSettings.getTimeout();
        dispatcher.dispatch(() -> fillInternal(aiContext, context, statistics, cancellationToken), timeout);
    }

    private Boolean fillInternal(AIContext aiContext, ChatContext context, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var filePath = aiContext.getPath();
        var start = aiContext.getStart();
        var finish = aiContext.getFinish();
        programingLanguage.getFromPath(filePath).ifPresent(lang -> context.programingLanguage = lang);
        entitiesWalker.walk(filePath, start, finish, activeEditorResourceSetProvider, new EntityVisitor()
        {
            @Override
            public boolean visitModule(ModuleInfo moduleInfo)
            {
                var module = moduleInfo.getModule();
                var project = v8ProjectManager.getProject(module);
                if (project != null)
                {
                    context.scriptLanguage = project.getScriptVariant().getName();
                }

                return false;
            }
        }, statistics, cancellationToken);
        return null;
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
