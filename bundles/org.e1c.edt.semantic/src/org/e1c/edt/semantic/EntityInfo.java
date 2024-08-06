/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessorDeclareStatement;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.SourceObjectLinkProvider;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.mcore.Type;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.inject.Inject;

public class EntityInfo
    implements IEntityInfo
{
    private static String MAX_INT = Integer.toString(Integer.MAX_VALUE);
    private final ILog log;
    private final IV8Model v8Model;
    private final IEntitiesWalker entitiesWalker;
    private final IIdFactory idFactory;

    @Inject
    public EntityInfo(ILog log, IV8Model v8Model, IEntitiesWalker entitiesWalker, IIdFactory idFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(entitiesWalker);
        Preconditions.checkNotNull(idFactory);
        this.log = log;
        this.v8Model = v8Model;
        this.entitiesWalker = entitiesWalker;
        this.idFactory = idFactory;
    }

    @SuppressWarnings("nls")
    @Override
    public Optional<EntityInfoResponse> geInfo(EntityInfoRequest request)
    {
        Preconditions.checkNotNull(request);
        if (request.uuid == null || request.uuid.isBlank())
        {
            return Optional.empty();
        }

        URL url;
        try
        {
            url = new URL(request.uuid);
        }
        catch (MalformedURLException e)
        {
            return Optional.empty();
        }

        var path = url.getPath();
        var params = Splitter.on('&').trimResults().withKeyValueSeparator('=').split(url.getQuery());
        var span = new ArrayList<Integer>();
        span.add(Integer.parseInt(params.getOrDefault("start", "0")));
        span.add(Integer.parseInt(params.getOrDefault("finish", MAX_INT)));
        var response = new EntityInfoResponse();
        var result = entitiesWalker.walk(path, span, new IEntityVisitor()
        {
            @Override
            public boolean visitVariable(String id, Variable variable, ICompositeNode node)
            {
                if (!request.uuid.equals(id))
                {
                    return false;
                }

                var objectEntity = new ObjectEntity();
                response.object = objectEntity;
                objectEntity.name = variable.getName();
                objectEntity.start = node.getTotalOffset();
                objectEntity.finish = node.getTotalEndOffset();
                objectEntity.code = node.getText();
                v8Model.getLastType(variable.getTypeStateProvider()).ifPresent(type -> fillType(objectEntity, type));
                return true;
            }

            @Override
            public boolean visitFeatureAccess(String id, FeatureAccess featureAccess, ICompositeNode node)
            {
                if (!request.uuid.equals(id))
                {
                    return false;
                }

                var objectEntity = new ObjectEntity();
                response.object = objectEntity;
                objectEntity.name = featureAccess.getName();
                objectEntity.start = node.getTotalOffset();
                objectEntity.finish = node.getTotalEndOffset();
                objectEntity.code = node.getText();
                var hasType = false;
                for (var type : v8Model.getTypesComputer()
                    .compute(featureAccess, v8Model.getEnvironments(featureAccess)))
                {
                    if (type instanceof Type)
                    {
                        fillType(objectEntity, (Type)type);
                    }
                    else
                    {
                        for (var ref : type.eCrossReferences())
                        {
                            if (ref instanceof Type)
                            {
                                fillType(objectEntity, (Type)ref);
                                hasType = true;
                                break;
                            }
                        }

                        if (hasType)
                        {
                            break;
                        }
                    }
                }

                return true;
            }

            private void fillType(ObjectEntity objectEntity, Type type)
            {
                objectEntity.type = type.getName();
                objectEntity.typeRu = type.getNameRu();
                var fields = new ArrayList<ObjectEntityField>();
                objectEntity.fields = fields;
                var contexDef = type.getContextDef();
                if (contexDef != null)
                {
                    for (var prop : contexDef.getProperties())
                    {
                        var field = new ObjectEntityField();
                        fields.add(field);
                        field.name = prop.getName();
                        var types = prop.getTypes();
                        if (!types.isEmpty())
                        {
                            var propType = types.get(types.size() - 1);
                            field.type = propType.getName();
                            field.typeRu = propType.getNameRu();
                            var featureAccess = EcoreUtil2.getContainerOfType(propType, FeatureAccess.class);
                            if (featureAccess != null)
                            {
                                v8Model.getPath(featureAccess).ifPresent(path -> {
                                    v8Model.getModule(path);
                                    try
                                    {
                                        var fieldNode = NodeModelUtils.getNode(featureAccess);
                                        field.uuid = idFactory.create(path, fieldNode);
                                    }
                                    catch (MalformedURLException e)
                                    {
                                        //
                                    }
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public boolean visitInvocation(String id, Invocation invocation, ICompositeNode node)
            {
                if (!request.uuid.equals(id))
                {
                    return false;
                }

                var methodEntity = new MethodEntity();
                response.method = methodEntity;
                var methodAccess = invocation.getMethodAccess();
                var modules = new ArrayList<com._1c.g5.v8.dt.bsl.model.Module>();
                v8Model.getPath(methodAccess).ifPresent(path -> {
                    v8Model.getModule(path).ifPresent(module -> modules.add(module));
                    methodEntity.path = path;
                });

                for (var featureEntry : v8Model.getFeatureEntries(methodAccess))
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

                    var signatureStructurized = new SignatureStructurized();
                    methodEntity.signatureStructurized = signatureStructurized;
                    var preprocess = new ArrayList<String>();
                    signatureStructurized.preprocess = preprocess;
                    var parameters = new ArrayList<Parameter>();
                    signatureStructurized.parameters = parameters;
                    var attributes = new ArrayList<String>();
                    signatureStructurized.attributes = attributes;
                    if (feature instanceof Method)
                    {
                        var method = (Method)feature;
                        var methodNode = NodeModelUtils.getNode(feature);
                        methodEntity.start = methodNode.getTotalOffset();
                        methodEntity.finish = methodNode.getTotalEndOffset();
                        methodEntity.code = methodNode.getText();
                        if (methodEntity.path != null)
                        {
                            try
                            {
                                methodEntity.uuid = idFactory.create(methodEntity.path, methodNode);
                            }
                            catch (MalformedURLException e)
                            {
                                //
                            }
                        }

                        if (method.isAsync())
                        {
                            attributes.add("Async");
                        }

                        if (method.isExport())
                        {
                            attributes.add("Export");
                        }

                        for (var param : method.getFormalParams())
                        {
                            var parameter = new Parameter();
                            parameters.add(parameter);
                            parameter.name = param.getName();
                            parameter.required = param.getDefaultValue() == null;
                            v8Model.getLastType(param.getTypeStateProvider()).ifPresent(type -> {
                                parameter.type = type.getName();
                                parameter.typeRu = type.getNameRu();
                            });
                        }

                        for (var pragma : method.getPragmas())
                        {
                            preprocess.add(pragma.getSymbol());
                        }

                        var region =
                            EcoreUtil2.getContainerOfType(method, RegionPreprocessorDeclareStatement.class);
                        if (region != null)
                        {
                            methodEntity.area = region.getName();
                        }

                        var returnTypes =
                            v8Model.getTypesComputer().compute(invocation, v8Model.getEnvironments(invocation));

                        if (!returnTypes.isEmpty())
                        {
                            var returnType = returnTypes.get(0);
                            signatureStructurized.returnType = returnType.getName();
                            signatureStructurized.returnTypeRu = returnType.getNameRu();
                        }
                    }

                    if (feature instanceof com._1c.g5.v8.dt.mcore.Method)
                    {
                        var method = (com._1c.g5.v8.dt.mcore.Method)feature;
                        var paramsSet = method.getParamSet();
                        if (!paramsSet.isEmpty())
                        {
                            var paramSet = paramsSet.get(paramsSet.size() - 1);
                            for (var param : paramSet.getParams())
                            {
                                var parameter = new Parameter();
                                parameters.add(parameter);
                                parameter.name = param.getName();
                                var paramTypes = param.getType();
                                if (!paramTypes.isEmpty())
                                {
                                    var paramType = paramTypes.get(paramTypes.size() - 1);
                                    // parameter.required = param.getDefaultValue() == null;
                                    parameter.type = paramType.getName();
                                    parameter.typeRu = paramType.getNameRu();
                                }
                            }
                        }

                        /*for (var pragma : method.getPragmas())
                        {
                            preprocess.add(pragma.getSymbol());
                        }*/

                        var region =
                            EcoreUtil2.getContainerOfType(method, RegionPreprocessorDeclareStatement.class);
                        if (region != null)
                        {
                            methodEntity.area = region.getName();
                        }

                        var returnTypes =
                            v8Model.getTypesComputer().compute(invocation, v8Model.getEnvironments(invocation));

                        if (!returnTypes.isEmpty())
                        {
                            var returnType = returnTypes.get(0);
                            signatureStructurized.returnType = returnType.getName();
                            signatureStructurized.returnTypeRu = returnType.getNameRu();
                        }
                    }
                }

                var simpleStatement = EcoreUtil2.getContainerOfType(invocation, SimpleStatement.class);
                if (simpleStatement != null)
                {
                    var target = simpleStatement.getLeft();
                    var types = v8Model.getTypes(target);
                    if (types.isEmpty())
                    {
                        return false;
                    }
                }

                return true;
            }
        });

        if (!result)
        {
            log.trace("Entity not found", request.uuid);
            return Optional.empty();
        }

        return Optional.of(response);
    }
}
