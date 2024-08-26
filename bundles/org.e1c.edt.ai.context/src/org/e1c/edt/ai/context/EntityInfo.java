/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessorDeclareStatement;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EntityInfo
    implements IEntityInfo
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IEntitiesWalker entitiesWalker;
    private final IIdFactory idFactory;
    private final ICommentFactory commentFactory;

    @Inject
    public EntityInfo(ILog log, IV8Model v8Model, IEntitiesWalker entitiesWalker, IIdFactory idFactory,
        ICommentFactory commentFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(entitiesWalker);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(commentFactory);
        this.log = log;
        this.v8Model = v8Model;
        this.entitiesWalker = entitiesWalker;
        this.idFactory = idFactory;
        this.commentFactory = commentFactory;
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

        var nodeIdOptional = idFactory.paeNodeId(request.ref);
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
            public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var objectEntity = new ObjectEntity();
                response.object = objectEntity;
                objectEntity.name = variable.getName();
                objectEntity.start = node.getTotalOffset();
                objectEntity.finish = node.getTotalEndOffset();
                objectEntity.code = node.getText();
                var comment = v8Model.getComment(variable);
                if (comment != null && !comment.isEmpty())
                {
                    objectEntity.comment = comment;
                }

                v8Model.getLastType(variable.getTypeStateProvider())
                    .ifPresent(type -> fillType(variable, objectEntity, type));
                return true;
            }

            @Override
            public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var objectEntity = new ObjectEntity();
                response.object = objectEntity;
                objectEntity.name = featureAccess.getName();
                objectEntity.start = node.getTotalOffset();
                objectEntity.finish = node.getTotalEndOffset();
                objectEntity.code = node.getText();
                var comment = v8Model.getComment(featureAccess);
                if (comment != null && !comment.isEmpty())
                {
                    objectEntity.comment = comment;
                }

                v8Model.getType(featureAccess).ifPresent(type -> fillType(featureAccess, objectEntity, type));
                return true;
            }

            private void fillType(EObject eObject, ObjectEntity objectEntity, Type type)
            {
                var fields = new ArrayList<ObjectEntityField>();
                objectEntity.fields = fields;
                objectEntity.type = type.getName();
                objectEntity.typeRu = type.getNameRu();
                var resouce = eObject.eResource();
                if (resouce != null)
                {
                    var types = v8Model.getTypes(eObject);
                    for (var pair : v8Model.getProperties(types, resouce))
                    {
                        for (var dynamicProp : pair.getFirst())
                        {
                            var field = createField(dynamicProp);
                            objectEntity.fields.add(field);
                        }
                    }
                }
                else
                {
                    var contexDef = type.getContextDef();
                    if (contexDef != null)
                    {
                        for (var prop : contexDef.getProperties())
                        {
                            var field = createField(prop);
                            objectEntity.fields.add(field);
                        }
                    }
                }
            }

            private ObjectEntityField createField(Property prop)
            {
                var field = new ObjectEntityField();
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
                            var fieldNode = NodeModelUtils.getNode(featureAccess);
                            field.uuid = idFactory.createNodeId(path, fieldNode);
                        });
                    }

                    var comment = v8Model.getComment(featureAccess);
                    if (comment != null && !comment.isEmpty())
                    {
                        field.comment = comment;
                    }
                }

                return field;
            }

            @Override
            public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
            {
                if (request.ref == null || !request.ref.equals(nodeId))
                {
                    return false;
                }

                var methodAccess = invocation.getMethodAccess();
                var methodAccessFeatureOptional = v8Model.getMethodFeature(methodAccess);
                var methodEntity = new MethodEntity();
                response.method = methodEntity;
                v8Model.getPath(methodAccess).ifPresent(path -> {
                    methodEntity.path = path;
                });

                List<String> comment = null;
                BslDocumentationComment structurizedComment = null;
                if (methodAccessFeatureOptional.isPresent())
                {
                    var methodAccessFeature = methodAccessFeatureOptional.get();
                    var signatureStructurized = new SignatureStructurized();
                    methodEntity.signatureStructurized = signatureStructurized;
                    var preprocess = new ArrayList<String>();
                    signatureStructurized.preprocess = preprocess;
                    var parameters = new ArrayList<Parameter>();
                    signatureStructurized.parameters = parameters;
                    var attributes = new ArrayList<String>();
                    signatureStructurized.attributes = attributes;
                    if (methodAccessFeature instanceof Method)
                    {
                        var method = (Method)methodAccessFeature;
                        var methodNode = NodeModelUtils.getNode(methodAccessFeature);
                        methodEntity.name = method.getName();
                        methodEntity.start = methodNode.getTotalOffset();
                        methodEntity.finish = methodNode.getTotalEndOffset();
                        methodEntity.code = methodNode.getText();
                        if (methodEntity.path != null)
                        {
                            methodEntity.uuid = idFactory.createNodeId(methodEntity.path, methodNode);
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

                        comment = v8Model.getComment(method);
                        structurizedComment = v8Model.getComment(method, true);
                    }

                    if (methodAccessFeature instanceof com._1c.g5.v8.dt.mcore.Method)
                    {
                        var method = (com._1c.g5.v8.dt.mcore.Method)methodAccessFeature;
                        methodEntity.name = method.getName();
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

                        if (method instanceof BslContextDefMethod)
                        {
                            var defMethod = (BslContextDefMethod)method;
                            comment = defMethod.getCommentLines();
                            structurizedComment = v8Model.getComment(defMethod, true);
                        }
                    }
                }

                if (comment != null && !comment.isEmpty())
                {
                    methodEntity.comment = comment;
                }

                if (structurizedComment != null)
                {
                    methodEntity.structurizedСomment = commentFactory.create(structurizedComment);
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
            log.trace("Entity not found", request.ref);
            return Optional.empty();
        }

        return Optional.of(response);
    }
}
