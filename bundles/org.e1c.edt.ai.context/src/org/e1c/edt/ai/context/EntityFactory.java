/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.DTO.DataType;
import org.e1c.edt.ai.context.DTO.FormAttr;
import org.e1c.edt.ai.context.DTO.FormBtn;
import org.e1c.edt.ai.context.DTO.FormEntity;
import org.e1c.edt.ai.context.DTO.FormFld;
import org.e1c.edt.ai.context.DTO.FormGrp;
import org.e1c.edt.ai.context.DTO.MethodEntity;
import org.e1c.edt.ai.context.DTO.ObjectEntity;
import org.e1c.edt.ai.context.DTO.ObjectEntityField;
import org.e1c.edt.ai.context.DTO.Parameter;
import org.e1c.edt.ai.context.DTO.SignatureStructurized;
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
import com._1c.g5.v8.dt.form.model.Addition;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.Decoration;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.mcore.Field;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EntityFactory implements IEntityFactory
{
    private final IV8Model v8Model;
    private final IIdFactory idFactory;
    private final ICommentFactory commentFactory;
    private final IFormWalker formWalker;

    @Inject
    public EntityFactory(IV8Model v8Model, IIdFactory idFactory, ICommentFactory commentFactory, IFormWalker formWalker)
    {
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(commentFactory);
        Preconditions.checkNotNull(formWalker);
        this.v8Model = v8Model;
        this.idFactory = idFactory;
        this.commentFactory = commentFactory;
        this.formWalker = formWalker;
    }

    @Override
    public Optional<FormEntity> createFormEntity(Form form, ICancellationToken cancellationToken)
    {
        var formEntity = new FormEntity();
        var groups = new HashMap<EObject, FormGrp>();
        groups.put(form, formEntity);
        formWalker.walk(form, new IFormVisitor()
        {
            @Override
            public void visitFormField(Optional<EObject> parent, FormField field)
            {
                parent.map(p -> groups.get(p)).ifPresent(group -> addField(group, createField(field)));
            }

            @Override
            public void visitField(Optional<EObject> parent, Field field)
            {
                //
            }

            @Override
            public void visitButton(Optional<EObject> parent, Button button)
            {
                parent.map(p -> groups.get(p)).ifPresent(group -> addButton(group, createButton(button)));
            }

            @Override
            public void visitTable(Optional<EObject> parent, Table table)
            {
                //
            }

            @Override
            public void visitAddition(Optional<EObject> parent, Addition addition)
            {
                //
            }

            @Override
            public void visitDecoration(Optional<EObject> parent, Decoration decoration)
            {
                //
            }

            @Override
            public void visitForm(Optional<EObject> parent, Form form)
            {
                formEntity.title = form.getTitle().map();
                var attributes = form.getAttributes();
                if (attributes != null && !attributes.isEmpty())
                {
                    if (formEntity.attributes == null)
                    {
                        formEntity.attributes = new ArrayList<>();
                    }

                    for (var attribute : attributes)
                    {
                        formEntity.attributes.add(createAttribute(attribute));
                    }
                }
            }

            @Override
            public void visitGroup(Optional<EObject> parent, Group group)
            {
                parent.map(p -> groups.get(p)).ifPresent(parentNode -> {
                    var node = createGroup(group);
                    if (parentNode.groups == null)
                    {
                        parentNode.groups = new ArrayList<>();
                    }

                    parentNode.groups.add(node);
                    groups.put(group, node);
                });
            }

            @Override
            public void visitFormItem(Optional<EObject> parent, FormItem formItem)
            {
                //
            }

            @Override
            public void visitEObject(Optional<EObject> parent, EObject eObject)
            {
                //
            }
        }, cancellationToken);

        return Optional.of(formEntity);
    }

    private void addField(FormGrp group, FormFld field)
    {
        if (group.fields == null)
        {
            group.fields = new ArrayList<>();
        }

        group.fields.add(field);
    }

    private void addButton(FormGrp group, FormBtn button)
    {
        if (group.buttons == null)
        {
            group.buttons = new ArrayList<>();
        }

        group.buttons.add(button);
    }

    private FormAttr createAttribute(FormAttribute attribute)
    {
        var attr = new FormAttr();
        attr.name = attribute.getName();
        var title = attribute.getTitle();
        if (title != null && !title.isEmpty())
        {
            attr.title = title.map();
        }

        var typeDescription = attribute.getValueType();
        if (typeDescription != null)
        {
            var types = typeDescription.getTypes();
            if (types != null && !types.isEmpty())
            {
                attr.types = new ArrayList<>();
                for(var type: types)
                {
                    var dataType = new DataType();
                    attr.types.add(dataType);
                    dataType.type = type.getName();
                    dataType.typeRu = type.getNameRu();
                }
            }
        }

        return attr;
    }

    private FormFld createField(FormField field)
    {
        var fld = new FormFld();
        fld.name = field.getName();
        var toolTip = field.getToolTip();
        if (toolTip != null && !toolTip.isEmpty())
        {
            fld.toolTip = toolTip.map();
        }

        var fiedType = field.getType();
        var dataPath = field.getDataPath();
        if (dataPath != null)
        {
            fld.dataPath = dataPath.toString();
        }

        if (fiedType != null)
        {
            fld.fieldType = fiedType.getName();
        }

        return fld;
    }

    private FormGrp createGroup(Group group)
    {
        var node = new FormGrp();
        node.name = group.getName();
        var title = group.getTitle();
        if (title != null && !title.isEmpty())
        {
            node.title = title.map();
        }

        var toolTip = group.getToolTip();
        if (toolTip != null && !toolTip.isEmpty())
        {
            node.toolTip = toolTip.map();
        }

        return node;
    }

    private FormBtn createButton(Button button)
    {
        var btn = new FormBtn();
        btn.name = button.getName();
        var title = button.getTitle();
        if (title != null && !title.isEmpty())
        {
            btn.title = title.map();
        }

        var dataPath = button.getDataPath();
        if (dataPath != null)
        {
            btn.dataPath = dataPath.toString();
        }

        button.getCommandName();
        // button.getCommandName()
        return btn;
    }

    @Override
    public Optional<ObjectEntity> crateObjectEntity(Variable variable, ICompositeNode node,
        ICancellationToken cancellationToken)
    {
        var objectEntity = new ObjectEntity();
        objectEntity.name = variable.getName();
        objectEntity.start = node.getTotalOffset();
        objectEntity.finish = node.getTotalEndOffset();
        objectEntity.code = node.getText();
        var comment = v8Model.getComment(variable);
        if (comment != null && !comment.isEmpty())
        {
            objectEntity.comment = comment;
        }

        var types = v8Model.getTypes(variable.getTypeStateProvider(), node);
        fillType(variable, objectEntity, types, cancellationToken);
        return Optional.of(objectEntity);
    }

    @Override
    public Optional<ObjectEntity> crateObjectEntity(FeatureAccess featureAccess, ICompositeNode node,
        ICancellationToken cancellationToken)
    {
        var objectEntity = new ObjectEntity();
        objectEntity.name = featureAccess.getName();
        objectEntity.start = node.getTotalOffset();
        objectEntity.finish = node.getTotalEndOffset();
        objectEntity.code = node.getText();
        var comment = v8Model.getComment(featureAccess);
        if (comment != null && !comment.isEmpty())
        {
            objectEntity.comment = comment;
        }


        var types = v8Model.getTypes(featureAccess);
        fillType(featureAccess, objectEntity, types, cancellationToken);
        return Optional.of(objectEntity);
    }

    @Override
    @SuppressWarnings("nls")
    public Optional<MethodEntity> createMethodEntity(Invocation invocation, ICompositeNode node,
        ICancellationToken cancellationToken)
    {
        var methodAccess = invocation.getMethodAccess();
        var methodAccessFeatureOptional = v8Model.getMethodFeature(methodAccess, cancellationToken);
        var methodEntity = new MethodEntity();
        v8Model.getPath(methodAccess).ifPresent(path -> {
            methodEntity.path = path;
        });

        var hasData = false;
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
                    parameter.types = createDataTypes(v8Model.getTypes(param.getTypeStateProvider(), node));
                }

                for (var pragma : method.getPragmas())
                {
                    preprocess.add(pragma.getSymbol());
                }

                var region = EcoreUtil2.getContainerOfType(method, RegionPreprocessorDeclareStatement.class);
                if (region != null)
                {
                    methodEntity.area = region.getName();
                }

                var returnTypes = v8Model.getTypesComputer().compute(invocation, v8Model.getEnvironments(invocation));
                signatureStructurized.returnTypes = createDataTypes2(returnTypes);
                comment = v8Model.getComment(method);
                structurizedComment = v8Model.getComment(method, true);
                hasData = true;
            }

            if (methodAccessFeature instanceof com._1c.g5.v8.dt.mcore.Method)
            {
                var method = (com._1c.g5.v8.dt.mcore.Method)methodAccessFeature;
                methodEntity.name = method.getName();
                var paramsSet = method.getParamSet();
                if (paramsSet != null && !paramsSet.isEmpty())
                {
                    var paramSet = paramsSet.get(paramsSet.size() - 1);
                    for (var param : paramSet.getParams())
                    {
                        var parameter = new Parameter();
                        parameters.add(parameter);
                        parameter.name = param.getName();
                        parameter.types = createDataTypes2(param.getType());
                    }
                }

                /*for (var pragma : method.getPragmas())
                {
                    preprocess.add(pragma.getSymbol());
                }*/

                var region = EcoreUtil2.getContainerOfType(method, RegionPreprocessorDeclareStatement.class);
                if (region != null)
                {
                    methodEntity.area = region.getName();
                }

                var returnTypes = v8Model.getTypesComputer().compute(invocation, v8Model.getEnvironments(invocation));
                signatureStructurized.returnTypes = createDataTypes2(returnTypes);
                if (method instanceof BslContextDefMethod)
                {
                    var defMethod = (BslContextDefMethod)method;
                    comment = defMethod.getCommentLines();
                    structurizedComment = v8Model.getComment(defMethod, true);
                }

                hasData = true;
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

        if (methodEntity.signatureStructurized == null)
        {
            var simpleStatement = EcoreUtil2.getContainerOfType(invocation, SimpleStatement.class);
            if (simpleStatement != null)
            {
                var target = simpleStatement.getLeft();
                if (target != null)
                {
                    var types = v8Model.getTypes(target);
                    var signatureStructurized = new SignatureStructurized();
                    methodEntity.signatureStructurized = signatureStructurized;
                    signatureStructurized.returnTypes = createDataTypes2(types);
                }
            }
        }

        if (!hasData)
        {
            return Optional.empty();
        }

        return Optional.of(methodEntity);
    }

    private void fillType(EObject eObject, ObjectEntity objectEntity, List<Type> types,
        ICancellationToken cancellationToken)
    {
        var fields = new ArrayList<ObjectEntityField>();

        objectEntity.fields = fields;
        objectEntity.types = createDataTypes(types);
        for (var type : types)
        {
            var contexDef = type.getContextDef();
            if (contexDef != null)
            {
                for (var prop : contexDef.getProperties())
                {
                    var field = createField(prop, cancellationToken);
                    objectEntity.fields.add(field);
                }
            }
        }

        var resouce = eObject.eResource();
        if (resouce != null)
        {
            var typeItems = v8Model.getTypes(eObject);
            for (var pair : v8Model.getProperties(typeItems, resouce))
            {
                for (var dynamicProp : pair.getFirst())
                {
                    var field = createField(dynamicProp, cancellationToken);
                    objectEntity.fields.add(field);
                }
            }
        }
    }

    private List<DataType> createDataTypes(List<Type> types)
    {
        if (types == null || types.isEmpty())
        {
            return null;
        }

        var dataTypes = new ArrayList<DataType>();
        for (var type : types)
        {
            var dataType = new DataType();
            dataTypes.add(dataType);
            dataType.type = type.getName();
            dataType.typeRu = type.getNameRu();
        }

        return dataTypes;
    }

    private List<DataType> createDataTypes2(List<TypeItem> types)
    {
        if (types == null || types.isEmpty())
        {
            return null;
        }

        var dataTypes = new ArrayList<DataType>();
        for (var type : types)
        {
            var dataType = new DataType();
            dataTypes.add(dataType);
            dataType.type = type.getName();
            dataType.typeRu = type.getNameRu();
        }

        return dataTypes;
    }

    private ObjectEntityField createField(Property prop, ICancellationToken cancellationToken)
    {
        var field = new ObjectEntityField();
        field.name = prop.getName();
        var types = prop.getTypes();
        field.types = createDataTypes2(types);
        if (types != null && !types.isEmpty())
        {
            for (var i = 0; i < types.size(); i++)
            {
                var propType = types.get(i);
                var propDataType = field.types.get(i);
                var featureAccess = EcoreUtil2.getContainerOfType(propType, FeatureAccess.class);
                if (featureAccess != null)
                {
                    v8Model.getPath(featureAccess).ifPresent(path -> {
                        v8Model.getModule(path, cancellationToken);
                        var fieldNode = NodeModelUtils.getNode(featureAccess);
                        propDataType.uuid = idFactory.createNodeId(path, fieldNode);
                    });
                }

                var comment = v8Model.getComment(featureAccess);
                if (comment != null && !comment.isEmpty())
                {
                    propDataType.comment = comment;
                }
            }
        }

        return field;
    }
}
