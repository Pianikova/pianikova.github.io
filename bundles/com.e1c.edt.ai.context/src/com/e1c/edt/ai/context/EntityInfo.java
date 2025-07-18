/**
  * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.AIContext;
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
        IProgramingLanguage programingLanguage, Provider<MessageDigest> messageDigestProvider, IHashTools hashTools,
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
        var result = entitiesWalker.walk(null, nodeId.getPath(), nodeId.getStart(), nodeId.getFinish(),
            activeEditorResourceSetProvider,
            new EntityVisitor()
        {
            @Override
                public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node)
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
                public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess,
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
                public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation,
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
            log.warning("Entity not found", () -> request.ref); //$NON-NLS-1$
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
            aiContext.getDocument() != null ? activeEditorResourceSetProvider : baseResourceSetProvider;
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
        localContext.relatedObjects = new ArrayList<>();
        localContext.relatedFunctions = new ArrayList<>();
        globalContext.localFunctions = new HashMap<>();
        globalContext.localFunctionsEntities = new HashMap<>();
        var uuids = new HashSet<String>();
        var cursorObjects = new EObject[1];
        var objects = new ArrayList<IBmObject>();
        var document = aiContext.getDocument();
        programingLanguage.getFromPath(filePath).ifPresent(lang -> localContext.programingLanguage = lang);
        entitiesWalker.walk(document, filePath, start, finish, resourceSetProvider, new EntityVisitor()
        {
            @Override
            public boolean visitModule(BmRoot root, Module module)
            {
                if (!actionFilter.test(new FillAction(DataType.HASH, Fields.CONFIGURATION_NAME, null)))
                {
                    return false;
                }

                var file = root.getFile(module).orElse(null);
                globalContext.moduleHash =
                    hashTools.hashOf(document, file).map(hash -> hashTools.format(hash, true)).orElse(null);

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

                        if (globalContext.configurationName == null)
                        {
                            globalContext.configurationName = ""; //$NON-NLS-1$
                        }
                    }
                }

                return false;
            }

            @Override
            public boolean visitNode(BmRoot root, EObject eObject, ICompositeNode node)
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
            public boolean visitBmObject(BmRoot root, IBmObject owner)
            {
                objects.add(owner);
                return false;
            }

            @Override
            public boolean visitForm(BmRoot root, Form form)
            {
                try (var measurement = statistics.measureDuration(StatisticsType.FORM_DURATUION))
                {
                    if (!actionFilter.test(new FillAction(DataType.HASH, Fields.FORM, null)))
                    {
                        return false;
                    }

                    root.getFile(form).map(file -> {
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
                    }).ifPresent(hash -> globalContext.formHash = hashTools.format(hash, true));

                    if (actionFilter.test(new FillAction(DataType.DATA, Fields.FORM, globalContext.formHash)))
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
            public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node)
            {
                if (!actionFilter.test(new FillAction(DataType.DATA, Fields.RELATED_OBJECTS, null)))
                {
                    return false;
                }

                if (!uuids.add(idFactory.createObjectId(filePath, variable, cancellationToken)))
                {
                    return false;
                }

                entityFactory.crateObjectEntity(variable, node, false, cancellationToken)
                    .ifPresent(object -> localContext.relatedObjects.add(object));
                return false;
            }

            @Override
            public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess,
                ICompositeNode node)
            {
                if (!actionFilter.test(new FillAction(DataType.DATA, Fields.RELATED_OBJECTS, null)))
                {
                    return false;
                }

                entityFactory.crateObjectEntity(featureAccess, node, false, cancellationToken)
                    .ifPresent(object -> localContext.relatedObjects.add(object));
                return false;
            }

            @Override
            public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation,
                ICompositeNode node)
            {
                if (!actionFilter.test(new FillAction(DataType.DATA, Fields.RELATED_FUNCTIONS, null)))
                {
                    return false;
                }

                entityFactory.createMethodEntity(invocation, node, false, cancellationToken)
                    .ifPresent(method -> localContext.relatedFunctions.add(method));
                return false;
            }

            @Override
            public boolean visitMethod(BmRoot root, String nodeId, Method method, ICompositeNode node)
            {
                if (document == null && !method.isExport())
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
                if (document != null && sourceOffset >= node.getTotalOffset()
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
                    entityFactory.createMethodEntity(method, node, false, cancellationToken)
                        .ifPresent(entity -> globalContext.localFunctionsEntities.put(methodName,
                            new HashedValue<>(entity, hashStr)));
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

        if (!objects.isEmpty() && actionFilter.test(new FillAction(DataType.DATA, Fields.META, null)))
        {
            try (var measurement = statistics.measureDuration(StatisticsType.META_DURATUION))
            {
                var meta = entityFactory.createMetaEntity(objects, cancellationToken);
                if (!meta.isEmpty())
                {
                    globalContext.metaEntity = meta.get(0);
                }
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }

        return stopwatch.elapsed();
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
        entitiesWalker.walk(aiContext.getDocument(), filePath, start, finish, activeEditorResourceSetProvider,
            new EntityVisitor()
        {
            @Override
                public boolean visitModule(BmRoot root, Module module)
            {
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
}
