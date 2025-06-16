/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Procedure;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.bsl.util.BslUtil;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.DynamicListExtInfo;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.MultiLanguageDataPath;
import com._1c.g5.v8.dt.form.model.PropertyInfo;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.form.model.ValueListExtInfo;
import com._1c.g5.v8.dt.form.service.datasourceinfo.IDataSourceInfoAssociationService;
import com._1c.g5.v8.dt.mcore.Field;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.EnumValue;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterResource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.e1c.edt.ai.context.DTO.AttributeEntity;
import com.e1c.edt.ai.context.DTO.DataType;
import com.e1c.edt.ai.context.DTO.DynamicListEntity;
import com.e1c.edt.ai.context.DTO.EnumValueEntity;
import com.e1c.edt.ai.context.DTO.FieldEntity;
import com.e1c.edt.ai.context.DTO.FormButtonEntity;
import com.e1c.edt.ai.context.DTO.FormEntity;
import com.e1c.edt.ai.context.DTO.FormFieldEntity;
import com.e1c.edt.ai.context.DTO.FormGroupEntity;
import com.e1c.edt.ai.context.DTO.FormTableEntity;
import com.e1c.edt.ai.context.DTO.MetaEntity;
import com.e1c.edt.ai.context.DTO.MethodEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntityField;
import com.e1c.edt.ai.context.DTO.Parameter;
import com.e1c.edt.ai.context.DTO.PropertyEntity;
import com.e1c.edt.ai.context.DTO.RegisterDimensionEntity;
import com.e1c.edt.ai.context.DTO.RegisterRecordEntity;
import com.e1c.edt.ai.context.DTO.RegisterResourceEntity;
import com.e1c.edt.ai.context.DTO.SignatureStructurized;
import com.e1c.edt.ai.context.DTO.TabularSectionEntity;
import com.e1c.edt.ai.context.DTO.ValueListEntity;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class EntityFactory
    implements IEntityFactory
{
    private final IV8Model v8Model;
    private final IIdFactory idFactory;
    private final ICommentFactory commentFactory;
    private final IFormWalker formWalker;
    private final ICodePartsProvider codePartsProvider;
    private final IDataSourceInfoAssociationService dataSourceInfoAssociationService;
    private final IV8ProjectManager v8ProjectManager;
    private final IModuleProvider moduleProvider;

    @Inject
    public EntityFactory(IV8Model v8Model, IIdFactory idFactory, ICommentFactory commentFactory, IFormWalker formWalker,
        ICodePartsProvider codePartsProvider, IDataSourceInfoAssociationService dataSourceInfoAssociationService,
        IV8ProjectManager v8ProjectManager, IModuleProvider moduleProvider)
    {
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(commentFactory);
        Preconditions.checkNotNull(formWalker);
        Preconditions.checkNotNull(codePartsProvider);
        Preconditions.checkNotNull(dataSourceInfoAssociationService);
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(moduleProvider);
        this.v8Model = v8Model;
        this.idFactory = idFactory;
        this.commentFactory = commentFactory;
        this.formWalker = formWalker;
        this.codePartsProvider = codePartsProvider;
        this.dataSourceInfoAssociationService = dataSourceInfoAssociationService;
        this.v8ProjectManager = v8ProjectManager;
        this.moduleProvider = moduleProvider;
    }

    @Override
    public Optional<FormEntity> createFormEntity(Form form, ICancellationToken cancellationToken)
    {
        var formEntity = new FormEntity();
        var forms = new ArrayList<Form>();
        var groups = new HashMap<EObject, FormGroupEntity>();
        groups.put(form, formEntity);
        formWalker.walk(form, new FormVisitor()
        {
            @Override
            public void visitFormField(Optional<EObject> parent, FormField field)
            {
                parent.map(p -> groups.get(p)).ifPresent(group -> addField(group, createField(field)));
            }

            @Override
            public void visitButton(Optional<EObject> parent, Button button)
            {
                parent.map(p -> groups.get(p)).ifPresent(group -> addButton(group, createButton(button)));
            }

            @Override
            public void visitForm(Optional<EObject> parent, Form form)
            {
                forms.add(form);
                formEntity.title = getMap(form.getTitle());
                var attributes = form.getAttributes();
                if (attributes != null && !attributes.isEmpty())
                {
                    if (formEntity.attributes == null)
                    {
                        formEntity.attributes = new ArrayList<>();
                    }

                    var hasMainAttribute = false;
                    for (var attribute : attributes)
                    {
                        hasMainAttribute |= attribute.isMain();
                    }

                    for (var attribute : attributes)
                    {
                        formEntity.attributes.add(createAttribute(form, attribute, hasMainAttribute));
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
            public void visitTable(Optional<EObject> parent, Table table)
            {
                parent.map(p -> groups.get(p)).ifPresent(parentNode -> {
                    var node = createTable(table);
                    if (parentNode.groups == null)
                    {
                        parentNode.groups = new ArrayList<>();
                    }

                    parentNode.groups.add(node);
                    groups.put(table, node);
                });
            }
        }, cancellationToken);

        if (forms.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.of(formEntity);
    }

    private void addField(FormGroupEntity group, FormFieldEntity field)
    {
        if (group.fields == null)
        {
            group.fields = new ArrayList<>();
        }

        group.fields.add(field);
    }

    private void addButton(FormGroupEntity group, FormButtonEntity button)
    {
        if (group.buttons == null)
        {
            group.buttons = new ArrayList<>();
        }

        group.buttons.add(button);
    }

    private AttributeEntity createAttribute(Form form, FormAttribute attribute, boolean hasMainAttribute)
    {
        var attr = new AttributeEntity();
        attr.name = attribute.getName();
        if (attribute.isMain())
        {
            attr.isMain = true;
        }

        attr.title = getMap(attribute.getTitle());
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

        if (!attribute.isMain())
        {
            var proprtyInfo = dataSourceInfoAssociationService.findPropertyInfo(form, attribute);
            if (proprtyInfo != null)
            {
                fillProperty(attr, proprtyInfo, hasMainAttribute ? 1 : 2);
            }

            var extInfo = attribute.getExtInfo();
            if (extInfo != null)
            {
                if (extInfo instanceof DynamicListExtInfo)
                {
                    var info = (DynamicListExtInfo)extInfo;
                    var dynamicList = new DynamicListEntity();
                    attr.dynamicList = dynamicList;
                    dynamicList.query = info.getQueryText();
                    dynamicList.keyField = info.getKeyField();
                    var keyType = info.getKeyType();
                    if (keyType != null)
                    {
                        dynamicList.keyTypeName = keyType.getName();
                    }

                    var mainTable = info.getMainTable();
                    if (mainTable != null)
                    {
                        dynamicList.mainTableName = mainTable.getName();
                        dynamicList.mainTableNameRu = mainTable.getNameRu();
                    }
                }

                if (extInfo instanceof ValueListExtInfo)
                {
                    var info = (ValueListExtInfo)extInfo;
                    var valueList = new ValueListEntity();
                    attr.valueList = valueList;
                    valueList.itemTypes = getTypes(info.getItemValueType());
                }
            }
        }

        return attr;
    }

    private List<String> getDataPaths(MultiLanguageDataPath dataPath)
    {
        if (dataPath != null)
        {
            var paths = dataPath.getPaths();
            if (paths != null && !paths.isEmpty())
            {
                var result = new ArrayList<String>();
                for (var path : paths)
                {
                    result.add(path.toString());
                }

                return result;
            }
        }

        return null;
    }

    private void fillProperty(PropertyEntity propery, PropertyInfo propertyInfo, int dept)
    {
        propery.name = propertyInfo.getName();
        propery.nameRu = propertyInfo.getNameRu();
        propery.description = propertyInfo.getStaticDescription();
        propery.dataPaths = getDataPaths(propertyInfo.getMultyLanguageDataPath());
        propery.types = getTypes(propertyInfo.getValueType());
        if (dept <= 0 || "Ref".equals(propery.name)) //$NON-NLS-1$
        {
            return;
        }

        switch (propertyInfo.getType())
        {
        case COLUMN_TABLE_TYPE_PROPERTY:
        case COMMON_TABLE_TYPE_PROPERTY:
            dept = 0;
            break;

        default:
            dept--;
            break;
        }

        var propInfos = propertyInfo.getPropertyInfos();
        if (propInfos != null && !propInfos.isEmpty())
        {
            propery.properties = new ArrayList<>();
            for (var propInfo : propInfos)
            {
                var prop = new PropertyEntity();
                fillProperty(prop, propInfo, dept);
                propery.properties.add(prop);
            }
        }
    }

    private FormFieldEntity createField(FormField field)
    {
        var entity = new FormFieldEntity();
        entity.name = field.getName();
        entity.toolTip = getMap(field.getToolTip());
        var fiedType = field.getType();
        var dataPath = field.getDataPath();
        if (dataPath != null)
        {
            entity.dataPath = dataPath.toString();
        }

        if (fiedType != null)
        {
            entity.fieldType = fiedType.getName();
        }

        return entity;
    }

    private FormGroupEntity createGroup(Group group)
    {
        var entity = new FormGroupEntity();
        entity.name = group.getName();
        entity.kind = group.getClass().getSimpleName();
        entity.title = getMap(group.getTitle());
        entity.toolTip = getMap(group.getToolTip());
        return entity;
    }

    private FormButtonEntity createButton(Button button)
    {
        var entity = new FormButtonEntity();
        entity.name = button.getName();
        entity.title = getMap(button.getTitle());
        var dataPath = button.getDataPath();
        if (dataPath != null)
        {
            entity.dataPath = dataPath.toString();
        }

        button.getCommandName();
        // button.getCommandName()
        return entity;
    }

    private FormTableEntity createTable(Table table)
    {
        var entity = new FormTableEntity();
        entity.name = table.getName();
        entity.kind = table.getClass().getSimpleName();
        entity.title = getMap(table.getTitle());
        entity.toolTip = getMap(table.getToolTip());
        var dataPath = table.getDataPath();
        if (dataPath != null)
        {
            entity.dataPath = dataPath.toString();
        }

        var fields = table.getFields();
        if (!fields.isEmpty())
        {
            entity.fields = new ArrayList<>();
            for (var field : fields)
            {
                if (!isPublishedField(field))
                {
                    continue;
                }

                entity.tableFields.add(createField(field));
            }
        }

        return entity;
    }

    @Override
    public Optional<ObjectEntity> crateObjectEntity(Variable variable, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken)
    {
        var entity = new ObjectEntity();
        entity.name = variable.getName();
        if (detailed)
        {
            entity.start = node.getTotalOffset();
            entity.finish = node.getTotalEndOffset();
            entity.code = node.getText();
        }

        var comment = v8Model.getComment(variable);
        if (comment != null && !comment.isEmpty())
        {
            entity.comment = comment;
        }

        var types = v8Model.getTypes(variable.getTypeStateProvider(), node);
        fillType(variable, entity, types, cancellationToken);
        return Optional.of(entity);
    }

    @Override
    public Optional<ObjectEntity> crateObjectEntity(FeatureAccess featureAccess, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken)
    {
        var objectEntity = new ObjectEntity();
        objectEntity.name = featureAccess.getName();
        if (detailed)
        {
            objectEntity.start = node.getTotalOffset();
            objectEntity.finish = node.getTotalEndOffset();
            objectEntity.code = node.getText();
        }

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
    public Optional<MethodEntity> createMethodEntity(Invocation invocation, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken)
    {
        var methodAccess = invocation.getMethodAccess();
        var methodAccessFeatureOptional = v8Model.getMethodFeature(methodAccess, cancellationToken);
        var methodEntity = new MethodEntity();
        v8Model.getPath(methodAccess).ifPresent(path -> {
            methodEntity.path = path;
        });

        var hasData = false;
        var hasSignatureStructurized = false;
        if (methodAccessFeatureOptional.isPresent())
        {
            var methodAccessFeature = methodAccessFeatureOptional.get();
            var signatureStructurized = new SignatureStructurized();
            methodEntity.signatureStructurized = signatureStructurized;
            signatureStructurized.preprocess = new ArrayList<>();
            var parameters = new ArrayList<Parameter>();
            signatureStructurized.parameters = parameters;
            signatureStructurized.attributes = new ArrayList<>();
            if (methodAccessFeature instanceof Method)
            {
                var method = (Method)methodAccessFeature;
                var methodNode = NodeModelUtils.getNode(methodAccessFeature);
                fillMethod(methodEntity, method, detailed, methodNode, node);
                var returnTypes = v8Model.getTypesComputer().compute(invocation, v8Model.getEnvironments(invocation));
                signatureStructurized.returnTypes = createDataTypesFromTypeItemsSafety(returnTypes, true);
                hasSignatureStructurized = true;
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
                        parameter.types = createDataTypesFromTypeItemsSafety(param.getType(), true);
                    }
                }

                getAreas(method).ifPresent(areas -> methodEntity.areas = areas);
                var returnTypes = v8Model.getTypesComputer().compute(invocation, v8Model.getEnvironments(invocation));
                signatureStructurized.returnTypes = createDataTypesFromTypeItemsSafety(returnTypes, true);
                hasSignatureStructurized = true;
                if (method instanceof BslContextDefMethod)
                {
                    var defMethod = (BslContextDefMethod)method;
                    methodEntity.comment = defMethod.getCommentLines();
                    methodEntity.structurizedComment = commentFactory.create(v8Model.getComment(defMethod, true));
                }

                hasData = true;
            }
        }

        if (methodEntity.signatureStructurized == null || !hasSignatureStructurized)
        {
            var simpleStatement = EcoreUtil2.getContainerOfType(invocation, SimpleStatement.class);
            if (simpleStatement != null)
            {
                var target = simpleStatement.getLeft();
                if (target != null)
                {
                    var types = createDataTypesFromTypeItemsSafety(v8Model.getTypes(target), true);
                    if (types != null)
                    {
                        var signatureStructurized = new SignatureStructurized();
                        methodEntity.signatureStructurized = signatureStructurized;
                        signatureStructurized.returnTypes = types;
                        hasData = true;
                    }
                }
            }
        }

        if (!hasData)
        {
            return Optional.empty();
        }

        return Optional.of(methodEntity);
    }

    @Override
    public Optional<MethodEntity> createMethodEntity(Method method, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken)
    {
        var methodEntity = new MethodEntity();
        var signatureStructurized = new SignatureStructurized();
        methodEntity.signatureStructurized = signatureStructurized;
        signatureStructurized.preprocess = new ArrayList<>();
        signatureStructurized.parameters = new ArrayList<>();
        signatureStructurized.attributes = new ArrayList<>();
        fillMethod(methodEntity, method, detailed, node, node);
        var returnTypes = v8Model.getTypesComputer().compute(method, v8Model.getEnvironments(method));
        signatureStructurized.returnTypes = createDataTypesFromTypeItemsSafety(returnTypes, true);
        return Optional.of(methodEntity);
    }

    @Override
    public Optional<MetaEntity> createMetaEntity(List<BasicFeature> attributes,
        List<DbObjectTabularSection> tabularSections, List<RegisterResource> registerResources,
        List<RegisterDimension> registerDimensions, List<BasicRegister> registerRecords,
        ArrayList<EnumValue> enumValues, ICancellationToken cancellationToken)
    {
        var meta = new MetaEntity();
        if (!attributes.isEmpty())
        {
            meta.attributes = new ArrayList<>();
            for (var attribute : attributes)
            {
                var entity = new AttributeEntity();
                meta.attributes.add(entity);
                entity.name = attribute.getName();
                entity.toolTip = getMap(attribute.getToolTip());
                entity.types = getTypes(attribute.getTypeDescription());
            }
        }

        if (!tabularSections.isEmpty())
        {
            meta.tabularSections = new ArrayList<>();
            for (var tabularSection : tabularSections)
            {
                var entity = new TabularSectionEntity();
                meta.tabularSections.add(entity);
                entity.name = tabularSection.getName();
                entity.comment = tabularSection.getComment();
                entity.toolTip = getMap(tabularSection.getToolTip());
                var fields = tabularSection.getFields();
                if (!fields.isEmpty())
                {
                    entity.fields = new ArrayList<>();
                    for (var field : fields)
                    {
                        if (!isPublishedField(field))
                        {
                            continue;
                        }

                        entity.fields.add(createField(field));
                    }
                }
            }
        }

        if (!registerResources.isEmpty())
        {
            meta.registerResources = new ArrayList<>();
            for (var registerResource : registerResources)
            {
                var entity = new RegisterResourceEntity();
                meta.registerResources.add(entity);
                entity.name = registerResource.getName();
                entity.comment = registerResource.getComment();
                entity.toolTip = getMap(registerResource.getToolTip());
                entity.synonym = getMap(registerResource.getSynonym());
                entity.types = getTypes(registerResource.getType());
            }
        }

        if (!registerDimensions.isEmpty())
        {
            meta.registerDimensions = new ArrayList<>();
            for (var registerDimension : registerDimensions)
            {
                var entity = new RegisterDimensionEntity();
                meta.registerDimensions.add(entity);
                entity.name = registerDimension.getName();
                entity.comment = registerDimension.getComment();
                entity.toolTip = getMap(registerDimension.getToolTip());
                entity.synonym = getMap(registerDimension.getSynonym());
                entity.types = getTypes(registerDimension.getType());
            }
        }

        if (!registerRecords.isEmpty())
        {
            meta.registerRecords = new ArrayList<>();
            for (var registerRecord : registerRecords)
            {
                var entity = new RegisterRecordEntity();
                meta.registerRecords.add(entity);
                entity.name = registerRecord.getName();
                entity.comment = registerRecord.getComment();
                entity.synonym = getMap(registerRecord.getSynonym());
                var fields = registerRecord.getFields();
                if (!fields.isEmpty())
                {
                    entity.fields = new ArrayList<>();
                    for (var field : fields)
                    {
                        entity.fields.add(createField(field));
                    }
                }
            }
        }

        if (!enumValues.isEmpty())
        {
            meta.enumValues = new ArrayList<>();
            for (var enumValue : enumValues)
            {
                var entity = new EnumValueEntity();
                meta.enumValues.add(entity);
                entity.name = enumValue.getName();
                entity.synonym = getMap(enumValue.getSynonym());
            }
        }

        return Optional.of(meta);
    }

    private boolean isPublishedField(Field field)
    {
        var name = field.getName();
        return name != null && !name.equals("Ref") && !name.equals("LineNumber"); //$NON-NLS-1$//$NON-NLS-2$
    }

    private Map<String, String> getMap(EMap<String, String> map)
    {
        if (map == null || map.isEmpty())
        {
            return null;
        }

        return map.map();
    }

    private List<DataType> getTypes(TypeDescription typeDescription)
    {
        if (typeDescription == null)
        {
            return null;
        }

        List<DataType> result = new ArrayList<>();
        var types = typeDescription.getTypes();
        if (types != null && !types.isEmpty())
        {
            for (var type : types)
            {
                var dataType = new DataType();
                result.add(dataType);
                dataType.type = type.getName();
                dataType.typeRu = type.getNameRu();
            }
        }

        return distinct(result);
    }

    private FieldEntity createField(Field field)
    {
        var entity = new FieldEntity();
        entity.name = field.getName();
        entity.nameRu = field.getNameRu();
        entity.types = getTypes(field.getType());
        return entity;
    }

    private void fillMethod(MethodEntity methodEntity, Method method, boolean detailed, ICompositeNode methodNode,
        ICompositeNode node)
    {
        methodEntity.name = method.getName();
        if (detailed)
        {
            methodEntity.start = methodNode.getTotalOffset();
            methodEntity.finish = methodNode.getTotalEndOffset();

            // IDEAI-137
            var code = methodNode.getText();
            var length = code.length();
            code = code.stripLeading();
            methodEntity.start += (length - code.length());
            code = code.stripTrailing();
            methodEntity.finish -= (length - code.length());
            methodEntity.code = code;
        }

        if (method instanceof Function)
        {
            methodEntity.kind = BslUtil.isRussian(method, v8ProjectManager) ? "Функция" : "Function"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            if (method instanceof Procedure)
            {
                methodEntity.kind = BslUtil.isRussian(method, v8ProjectManager) ? "Процедура" : "Procedure"; //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        var signatureParts = codePartsProvider.getParts(methodNode)
            .filter(i -> i.getLocation() == CursorLocation.FunctionName
                || i.getLocation() == CursorLocation.FunctionArguments)
            .map(i -> i.getText())
            .collect(Collectors.toList());
        var signature = new StringBuilder();
        for (var signaturePart : signatureParts)
        {
            signature.append(signaturePart);
        }

        var signatureStr = signature.toString().trim();
        if (!signatureStr.isBlank())
        {
            methodEntity.signatureStr = signatureStr;
        }

        if (methodEntity.path != null)
        {
            methodEntity.uuid = idFactory.createNodeId(methodEntity.path, methodNode);
        }

        if (method.isAsync())
        {
            methodEntity.signatureStructurized.attributes
                .add(BslUtil.isRussian(method, v8ProjectManager) ? "Аcинх" : "Async"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (method.isExport())
        {
            methodEntity.signatureStructurized.attributes
                .add(BslUtil.isRussian(method, v8ProjectManager) ? "Экспорт" : "Export"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        for (var param : method.getFormalParams())
        {
            var parameter = new Parameter();
            methodEntity.signatureStructurized.parameters.add(parameter);
            parameter.name = param.getName();
            parameter.required = param.getDefaultValue() == null;
            parameter.types = createDataTypes(v8Model.getTypes(param.getTypeStateProvider(), node));
        }

        for (var pragma : method.getPragmas())
        {
            methodEntity.signatureStructurized.preprocess.add(pragma.getSymbol());
        }

        getEnvironments(method).ifPresent(areas -> methodEntity.environments = areas);
        getAreas(method).ifPresent(areas -> methodEntity.areas = areas);
        methodEntity.comment = v8Model.getComment(method);
        methodEntity.structurizedComment = commentFactory.create(v8Model.getComment(method, true));
    }

    @Override
    public Optional<List<String>> getEnvironments(EObject obj)
    {
        var environments = v8Model.getEnvironments(obj).toArray();
        if (environments.length == 0)
        {
            return Optional.empty();
        }

        var result = new ArrayList<String>();
        for (var environment : environments)
        {
            result.add(environment.name());
        }

        return Optional.of(result);
    }

    @Override
    public Optional<List<String>> getAreas(EObject obj)
    {
        EObject object = obj;
        var areas = new ArrayList<String>();
        do
        {
            var region = EcoreUtil2.getContainerOfType(object, RegionPreprocessor.class);
            if (region != null)
            {
                var it = region.eAllContents();
                var started = false;
                while (it.hasNext())
                {
                    var item = it.next();
                    if (!started)
                    {
                        if (region.getItem() == item)
                        {
                            started = true;
                        }
                    }
                    else
                    {
                        if (region.getItemAfter() == item)
                        {
                            break;
                        }
                    }

                    if (started && item == obj)
                    {
                        areas.add(0, region.getName());
                        break;
                    }
                }

                object = region.eContainer();
            }
            else
            {
                break;
            }
        }
        while (object != null);

        if (areas.size() == 0)
        {
            return Optional.empty();
        }

        return Optional.of(areas);
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

        return distinct(dataTypes);
    }

    private List<DataType> createDataTypesFromTypeItemsSafety(List<TypeItem> types, boolean distinct)
    {
        if (types == null || types.isEmpty())
        {
            return null;
        }

        var dataTypes = new ArrayList<DataType>();
        var iterator = types.iterator();
        while (iterator.hasNext())
        {
            TypeItem type;
            try
            {
                type = iterator.next();
            }
            catch (Exception ex)
            {
                continue;
            }

            var dataType = new DataType();
            dataTypes.add(dataType);
            dataType.type = type.getName();
            dataType.typeRu = type.getNameRu();
        }

        if (!distinct)
        {
            return dataTypes;
        }

        return distinct(dataTypes);
    }

    private static <T> List<T> distinct(List<T> source)
    {
        if (source == null || source.size() == 0)
        {
            return source;
        }

        return source.stream().distinct().collect(Collectors.toList());
    }

    private ObjectEntityField createField(Property prop, ICancellationToken cancellationToken)
    {
        var field = new ObjectEntityField();
        field.name = prop.getName();
        var types = prop.getTypes();
        field.types = createDataTypesFromTypeItemsSafety(types, false);
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
                        moduleProvider.getModule(null, path, cancellationToken);
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

        field.types = distinct(field.types);
        return field;
    }
}
