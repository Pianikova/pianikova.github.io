/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.bm.core.IBmObject;
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
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.AccountTypeValue;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.DynamicListExtInfo;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormChoiceListDesTimeValue;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormParameter;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.MultiLanguageDataPath;
import com._1c.g5.v8.dt.form.model.PropertyInfo;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.form.model.ValueListExtInfo;
import com._1c.g5.v8.dt.form.service.datasourceinfo.IDataSourceInfoAssociationService;
import com._1c.g5.v8.dt.mcore.BinaryValue;
import com._1c.g5.v8.dt.mcore.BooleanValue;
import com._1c.g5.v8.dt.mcore.Border;
import com._1c.g5.v8.dt.mcore.BorderValue;
import com._1c.g5.v8.dt.mcore.Color;
import com._1c.g5.v8.dt.mcore.ColorValue;
import com._1c.g5.v8.dt.mcore.DateValue;
import com._1c.g5.v8.dt.mcore.Field;
import com._1c.g5.v8.dt.mcore.FieldSource;
import com._1c.g5.v8.dt.mcore.FixedArrayValue;
import com._1c.g5.v8.dt.mcore.Font;
import com._1c.g5.v8.dt.mcore.FontValue;
import com._1c.g5.v8.dt.mcore.IrresolvableReferenceValue;
import com._1c.g5.v8.dt.mcore.NullValue;
import com._1c.g5.v8.dt.mcore.NumberValue;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.ReferenceValue;
import com._1c.g5.v8.dt.mcore.StandardPeriod;
import com._1c.g5.v8.dt.mcore.StandardPeriodValue;
import com._1c.g5.v8.dt.mcore.StringValue;
import com._1c.g5.v8.dt.mcore.SysEnumValue;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeDescriptionValue;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.UndefinedValue;
import com._1c.g5.v8.dt.mcore.ValueList;
import com._1c.g5.v8.dt.metadata.common.AccountType;
import com._1c.g5.v8.dt.metadata.common.ChartLineType;
import com._1c.g5.v8.dt.metadata.common.ChartLineTypeValue;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicForm;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.BusinessProcess;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccountsPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypes;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypesPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypes;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypesPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.DataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.Enum;
import com._1c.g5.v8.dt.metadata.mdclass.EnumValue;
import com._1c.g5.v8.dt.metadata.mdclass.ExchangePlan;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalDataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalReport;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.Report;
import com._1c.g5.v8.dt.metadata.mdclass.ReportTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.StandardAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com._1c.g5.v8.dt.metadata.mdclass.Task;
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.e1c.edt.ai.context.DTO.AccountTypeEntity;
import com.e1c.edt.ai.context.DTO.AttributeEntity;
import com.e1c.edt.ai.context.DTO.BorderEntity;
import com.e1c.edt.ai.context.DTO.ChartLineTypeEntity;
import com.e1c.edt.ai.context.DTO.ColorEntity;
import com.e1c.edt.ai.context.DTO.DataType;
import com.e1c.edt.ai.context.DTO.DynamicListEntity;
import com.e1c.edt.ai.context.DTO.EnumValueEntity;
import com.e1c.edt.ai.context.DTO.FieldEntity;
import com.e1c.edt.ai.context.DTO.FontEntity;
import com.e1c.edt.ai.context.DTO.FormButtonEntity;
import com.e1c.edt.ai.context.DTO.FormEntity;
import com.e1c.edt.ai.context.DTO.FormFieldEntity;
import com.e1c.edt.ai.context.DTO.FormGroupEntity;
import com.e1c.edt.ai.context.DTO.FormParameterEntity;
import com.e1c.edt.ai.context.DTO.FormTableEntity;
import com.e1c.edt.ai.context.DTO.MetaEntity;
import com.e1c.edt.ai.context.DTO.MethodEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntityField;
import com.e1c.edt.ai.context.DTO.ObjectFormEntity;
import com.e1c.edt.ai.context.DTO.Parameter;
import com.e1c.edt.ai.context.DTO.PredefinedEntity;
import com.e1c.edt.ai.context.DTO.PropertyEntity;
import com.e1c.edt.ai.context.DTO.RegisterDimensionEntity;
import com.e1c.edt.ai.context.DTO.RegisterRecordEntity;
import com.e1c.edt.ai.context.DTO.RegisterResourceEntity;
import com.e1c.edt.ai.context.DTO.SignatureStructurized;
import com.e1c.edt.ai.context.DTO.StandardPeriodEntity;
import com.e1c.edt.ai.context.DTO.SubsystemEntity;
import com.e1c.edt.ai.context.DTO.TabularSectionEntity;
import com.e1c.edt.ai.context.DTO.TemplateEntity;
import com.e1c.edt.ai.context.DTO.ValueEntity;
import com.e1c.edt.ai.context.DTO.ValueListEntity;
import com.e1c.edt.ai.context.DTO.ValueType;
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
    private final IConfigurationProvider configurationProvider;
    private final IQualifiedNameFilePathConverter qualifiedNameFilePathConverter;

    @Inject
    public EntityFactory(IV8Model v8Model, IIdFactory idFactory, ICommentFactory commentFactory, IFormWalker formWalker,
        ICodePartsProvider codePartsProvider, IDataSourceInfoAssociationService dataSourceInfoAssociationService,
        IV8ProjectManager v8ProjectManager, IModuleProvider moduleProvider,
        IConfigurationProvider configurationProvider, IQualifiedNameFilePathConverter qualifiedNameFilePathConverter)
    {
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(commentFactory);
        Preconditions.checkNotNull(formWalker);
        Preconditions.checkNotNull(codePartsProvider);
        Preconditions.checkNotNull(dataSourceInfoAssociationService);
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(moduleProvider);
        Preconditions.checkNotNull(configurationProvider);
        Preconditions.checkNotNull(qualifiedNameFilePathConverter);
        this.v8Model = v8Model;
        this.idFactory = idFactory;
        this.commentFactory = commentFactory;
        this.formWalker = formWalker;
        this.codePartsProvider = codePartsProvider;
        this.dataSourceInfoAssociationService = dataSourceInfoAssociationService;
        this.v8ProjectManager = v8ProjectManager;
        this.moduleProvider = moduleProvider;
        this.configurationProvider = configurationProvider;
        this.qualifiedNameFilePathConverter = qualifiedNameFilePathConverter;
    }

    @Override
    public Optional<FormEntity> createFormEntity(Form form, ICancellationToken cancellationToken)
    {
        var formEntity = new FormEntity();
        formEntity.title = createMap(form.getTitle());
        formEntity.parameters = createFormParameters(form.getParameters());
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
        return Optional.of(formEntity);
    }

    private List<FormParameterEntity> createFormParameters(List<FormParameter> parameters)
    {
        if (parameters == null)
        {
            return null;
        }

        return parameters.stream().map(this::createFormParameter).collect(Collectors.toList());
    }

    private FormParameterEntity createFormParameter(FormParameter parameter)
    {
        var entity = new FormParameterEntity();
        entity.name = parameter.getName();
        entity.comment = parameter.getComment();
        entity.types = createTypes(parameter.getValueType());
        return entity;
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

        attr.title = createMap(attribute.getTitle());
        attr.types = createTypes(attribute.getValueType());
        if (!attribute.isMain())
        {
            try
            {
                var proprtyInfo = dataSourceInfoAssociationService.findPropertyInfo(form, attribute);
                if (proprtyInfo != null)
                {
                    fillProperty(attr, proprtyInfo, hasMainAttribute ? 1 : 2);
                }
            }
            catch (Exception e)
            {
                // ignore
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
                    valueList.itemTypes = createTypes(info.getItemValueType());
                }
            }
        }

        return attr;
    }

    private List<String> getDataPaths(MultiLanguageDataPath dataPath)
    {
        if (dataPath == null)
        {
            return null;
        }

        var paths = dataPath.getPaths();
        if (paths == null || paths.isEmpty())
        {
            return null;
        }

        return paths.stream().map(path -> path.toString()).collect(Collectors.toList());
    }

    private void fillProperty(PropertyEntity propery, PropertyInfo propertyInfo, int dept)
    {
        propery.name = propertyInfo.getName();
        propery.nameRu = propertyInfo.getNameRu();
        propery.description = propertyInfo.getStaticDescription();
        propery.dataPaths = getDataPaths(propertyInfo.getMultyLanguageDataPath());
        propery.types = createTypes(propertyInfo.getValueType());
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
        entity.toolTip = createMap(field.getToolTip());
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
        entity.title = createMap(group.getTitle());
        entity.toolTip = createMap(group.getToolTip());
        return entity;
    }

    private FormButtonEntity createButton(Button button)
    {
        var entity = new FormButtonEntity();
        entity.name = button.getName();
        entity.title = createMap(button.getTitle());
        var dataPath = button.getDataPath();
        if (dataPath != null)
        {
            entity.dataPath = dataPath.toString();
        }

        return entity;
    }

    private FormTableEntity createTable(Table table)
    {
        var entity = new FormTableEntity();
        entity.name = table.getName();
        entity.kind = table.getClass().getSimpleName();
        entity.title = createMap(table.getTitle());
        entity.toolTip = createMap(table.getToolTip());
        var dataPath = table.getDataPath();
        if (dataPath != null)
        {
            entity.dataPath = dataPath.toString();
        }

        entity.tableFields = createFields(table, field -> isPublishedField(field));
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
    public List<MetaEntity> createMetaEntity(List<IBmObject> objects, ICancellationToken cancellationToken)
    {
        var entities = new ArrayList<MetaEntity>();
        for (var bmObject : objects)
        {
            var entity = createAndFillMetaEntity(bmObject, false);
            if (entity != null)
            {
                entities.add(entity);
            }

            if (cancellationToken.isCanceled())
            {
                break;
            }
        }

        return entities;
    }

    private MetaEntity createAndFillMetaEntity(IBmObject bmObject, boolean brief)
    {
        var entity = brief ? new MetaEntity() : createMetaEntity(bmObject, brief);
        entity.type = getTypeName(bmObject);
        var namespace = bmObject.bmGetNamespace();
        if (namespace != null)
        {
            entity.namespace = namespace.getName();
            var top = bmObject.bmIsTop() ? bmObject : bmObject.bmGetTopObject();
            entity.fullQualifiedName = top.bmGetFqn();
            if (entity.fullQualifiedName != null)
            {
                entity.path = qualifiedNameFilePathConverter.getFilePath(entity.fullQualifiedName);
            }
        }

        if (bmObject instanceof MdObject)
        {
            var mdObject = (MdObject)bmObject;
            entity.name = mdObject.getName();
            entity.comment = mdObject.getComment();
            entity.synonym = createMap(mdObject.getSynonym());
        }

        return entity;
    }

    private String getTypeName(EObject eObject)
    {
        for (var metadataInterface : eObject.getClass().getInterfaces())
        {
            if (metadataInterface.getName().startsWith("com._1c.g5.v8.dt.metadata.mdclass.")) //$NON-NLS-1$
            {
                return metadataInterface.getSimpleName();
            }
        }

        return null;
    }

    private MetaEntity createMetaEntity(IBmObject bmObject, boolean brief)
    {
        if (bmObject instanceof AccountingRegister)
        {
            return createAccountingRegister((AccountingRegister)bmObject);
        }

        if (bmObject instanceof AccumulationRegister)
        {
            return createAccumulationRegister((AccumulationRegister)bmObject);
        }

        if (bmObject instanceof BusinessProcess)
        {
            return createBusinessProcess((BusinessProcess)bmObject);
        }

        if (bmObject instanceof CalculationRegister)
        {
            return createCalculationRegister((CalculationRegister)bmObject);
        }

        if (bmObject instanceof Catalog)
        {
            return createCatalog((Catalog)bmObject);
        }

        if (bmObject instanceof ChartOfAccounts)
        {
            return createChartOfAccounts((ChartOfAccounts)bmObject);
        }

        if (bmObject instanceof ChartOfCalculationTypes)
        {
            return createChartOfCalculationTypes((ChartOfCalculationTypes)bmObject);
        }

        if (bmObject instanceof ChartOfCharacteristicTypes)
        {
            return createChartOfCharacteristicTypes((ChartOfCharacteristicTypes)bmObject);
        }

        if (bmObject instanceof DataProcessor)
        {
            return createDataProcessor((DataProcessor)bmObject);
        }

        if (bmObject instanceof Document)
        {
            var meta = createDocument((Document)bmObject);
            return meta;
        }

        if (bmObject instanceof ExchangePlan)
        {
            return createExchangePlan((ExchangePlan)bmObject);
        }

        if (bmObject instanceof ExternalDataProcessor)
        {
            return createExternalDataProcessor((ExternalDataProcessor)bmObject);
        }

        if (bmObject instanceof ExternalReport)
        {
            return createExternalReport((ExternalReport)bmObject);
        }

        if (bmObject instanceof InformationRegister)
        {
            return createInformationRegister((InformationRegister)bmObject);
        }

        if (bmObject instanceof Report)
        {
            return createReport((Report)bmObject);
        }

        if (bmObject instanceof ReportTabularSection)
        {
            return createReportTabularSection((ReportTabularSection)bmObject);
        }

        if (bmObject instanceof Task)
        {
            return createTask((Task)bmObject);
        }

        if (bmObject instanceof com._1c.g5.v8.dt.metadata.mdclass.Enum)
        {
            return createEnum((com._1c.g5.v8.dt.metadata.mdclass.Enum)bmObject);
        }

        return new MetaEntity();
    }

    private MetaEntity createEnum(Enum bmObject)
    {
        var meta = new MetaEntity();
        var enumValues = bmObject.getEnumValues();
        if (enumValues != null)
        {
            meta.enumValues = enumValues.stream().map(this::createEnumValue).collect(Collectors.toList());
        }

        meta.objectForms = createForms(bmObject.getForms());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.subsystems = createSubsystems(bmObject);
        return meta;
    }

    private MetaEntity createTask(Task bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.objectForms = createForms(bmObject.getForms());
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createReportTabularSection(ReportTabularSection bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.subsystems = createSubsystems(bmObject);
        return meta;
    }

    private MetaEntity createReport(Report bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createInformationRegister(InformationRegister bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.registerResources = createRegisterResources(bmObject.getResources());
        meta.registerDimensions = createRegisterDimensions(bmObject.getDimensions());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createExternalReport(ExternalReport bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createExternalDataProcessor(ExternalDataProcessor bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createExchangePlan(ExchangePlan bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.objectForms = createForms(bmObject.getForms());
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createDocument(Document bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.fields = createFields(bmObject, field -> isPublishedField(field));
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.registerRecords = createRegisterRecords(bmObject.getRegisterRecords());
        meta.objectForms = createForms(bmObject.getForms());
        // Too much info:
        // meta.posting = bmObject.getPosting().getName();
        // meta.realTimePosting = bmObject.getRealTimePosting().getName();
        // meta.registerRecordsDeletion = bmObject.getRegisterRecordsDeletion().getName();
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createDataProcessor(DataProcessor bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createChartOfCharacteristicTypes(ChartOfCharacteristicTypes bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.objectForms = createForms(bmObject.getForms());
        meta.predefined =
            createChartOfCharacteristicTypesPredefinedItems(
                Optional.ofNullable(bmObject.getPredefined()).map(i -> i.getItems()).orElse(null));
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createChartOfCalculationTypes(ChartOfCalculationTypes bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.objectForms = createForms(bmObject.getForms());
        meta.predefined =
            createChartOfCalculationTypesPredefinedItems(
                Optional.ofNullable(bmObject.getPredefined()).map(i -> i.getItems()).orElse(null));
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createChartOfAccounts(ChartOfAccounts bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.objectForms = createForms(bmObject.getForms());
        meta.predefined =
            createChartOfAccountsPredefinedItems(
                Optional.ofNullable(bmObject.getPredefined()).map(i -> i.getItems()).orElse(null));
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createCatalog(Catalog bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.objectForms = createForms(bmObject.getForms());
        meta.predefined =
            createCatalogPredefinedItems(
                Optional.ofNullable(bmObject.getPredefined()).map(i -> i.getItems()).orElse(null));
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createCalculationRegister(CalculationRegister bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.registerResources = createRegisterResources(bmObject.getResources());
        meta.registerDimensions = createRegisterDimensions(bmObject.getDimensions());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createBusinessProcess(BusinessProcess bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.tabularSections = createTabularSections(bmObject.getTabularSections());
        meta.objectForms = createForms(bmObject.getForms());
        meta.basedOn = createBasedOn(bmObject.getBasedOn());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createAccumulationRegister(AccumulationRegister bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.registerResources = createRegisterResources(bmObject.getResources());
        meta.registerDimensions = createRegisterDimensions(bmObject.getDimensions());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private MetaEntity createAccountingRegister(AccountingRegister bmObject)
    {
        var meta = new MetaEntity();
        meta.attributes = createAttributes(bmObject.getAttributes());
        meta.standardAttributes = createStandardAttributes(bmObject.getStandardAttributes());
        meta.registerResources = createRegisterResources(bmObject.getResources());
        meta.registerDimensions = createRegisterDimensions(bmObject.getDimensions());
        meta.objectForms = createForms(bmObject.getForms());
        meta.subsystems = createSubsystems(bmObject);
        meta.templates = createTemplates(bmObject.getTemplates());
        return meta;
    }

    private List<MetaEntity> createBasedOn(List<MdObject> basedOn)
    {
        if (basedOn == null || basedOn.isEmpty())
        {
            return null;
        }

        return basedOn.stream()
            .filter(i -> i instanceof IBmObject)
            .map(i -> (IBmObject)i)
            .map(this::createBasedOn)
            .collect(Collectors.toList());
    }

    private <T extends BasicFeature> List<AttributeEntity> createAttributes(List<T> attributes)
    {
        if (attributes == null || attributes.isEmpty())
        {
            return null;
        }

        return attributes.stream().map(this::createAttribute).collect(Collectors.toList());
    }

    private <T extends StandardAttribute> List<AttributeEntity> createStandardAttributes(List<T> attributes)
    {
        if (attributes == null || attributes.isEmpty())
        {
            return null;
        }

        return attributes.stream().map(this::createStandardAttribute).collect(Collectors.toList());
    }

    private <T extends BasicForm> List<ObjectFormEntity> createForms(List<T> forms)
    {
        if (forms == null || forms.isEmpty())
        {
            return null;
        }

        return forms.stream().map(this::createForm).collect(Collectors.toList());
    }

    private <T extends RegisterResource> List<RegisterResourceEntity> createRegisterResources(List<T> resources)
    {
        if (resources == null || resources.isEmpty())
        {
            return null;
        }

        return resources.stream().map(this::createRegisterResource).collect(Collectors.toList());
    }

    private <T extends RegisterDimension> List<RegisterDimensionEntity> createRegisterDimensions(List<T> dimensions)
    {
        if (dimensions == null || dimensions.isEmpty())
        {
            return null;
        }

        return dimensions.stream().map(this::createRegisterDimension).collect(Collectors.toList());
    }

    private <T extends DbObjectTabularSection> List<TabularSectionEntity> createTabularSections(List<T> tabularSections)
    {
        if (tabularSections == null || tabularSections.isEmpty())
        {
            return null;
        }

        return tabularSections.stream().map(this::createTabularSection).collect(Collectors.toList());
    }

    private <T extends BasicRegister> List<RegisterRecordEntity> createRegisterRecords(List<T> registerRecords)
    {
        if (registerRecords == null || registerRecords.isEmpty())
        {
            return null;
        }

        return registerRecords.stream().map(this::createBasicRegister).collect(Collectors.toList());
    }

    private List<PredefinedEntity> createCatalogPredefinedItems(List<CatalogPredefinedItem> predefinedItems)
    {
        if (predefinedItems == null || predefinedItems.isEmpty())
        {
            return null;
        }

        return predefinedItems.stream().map(this::createCatalogPredefined).collect(Collectors.toList());
    }

    private List<PredefinedEntity> createChartOfCharacteristicTypesPredefinedItems(
        List<ChartOfCharacteristicTypesPredefinedItem> predefinedItems)
    {
        if (predefinedItems == null || predefinedItems.isEmpty())
        {
            return null;
        }

        return predefinedItems.stream()
            .map(this::createChartOfCharacteristicTypesPredefined)
            .collect(Collectors.toList());
    }

    private List<PredefinedEntity> createChartOfCalculationTypesPredefinedItems(
        List<ChartOfCalculationTypesPredefinedItem> predefinedItems)
    {
        if (predefinedItems == null || predefinedItems.isEmpty())
        {
            return null;
        }

        return predefinedItems.stream().map(this::createChartOfCalculationTypesPredefined).collect(Collectors.toList());
    }

    private List<PredefinedEntity> createChartOfAccountsPredefinedItems(
        List<ChartOfAccountsPredefinedItem> predefinedItems)
    {
        if (predefinedItems == null || predefinedItems.isEmpty())
        {
            return null;
        }

        return predefinedItems.stream().map(this::createChartOfAccountsPredefined).collect(Collectors.toList());
    }

    private <T extends Template> List<TemplateEntity> createTemplates(List<T> templates)
    {
        if (templates == null || templates.isEmpty())
        {
            return null;
        }

        return templates.stream().map(this::createTemplate).collect(Collectors.toList());
    }

    private List<SubsystemEntity> createSubsystems(MdObject mdObject)
    {
        var bmObject = (IBmObject)mdObject;
        var bmId = bmObject.bmGetId();
        var config = configurationProvider.getConfiguration(mdObject);
        if (config == null)
        {
            return null;
        }

        var result = config.getSubsystems()
            .stream()
            .filter(subsystem -> subsystem.getContent().stream().anyMatch(i -> ((IBmObject)mdObject).bmGetId() == bmId))
            .map(subsystem -> createSubsystem(subsystem, 0))
            .collect(Collectors.toList());

        if (result.isEmpty())
        {
            return null;
        }

        return result;
    }

    private SubsystemEntity createSubsystem(Subsystem subsystem, int level)
    {
        var entity = new SubsystemEntity();
        entity.name = subsystem.getName();
        entity.comment = subsystem.getComment();
        entity.synonym = createMap(subsystem.getSynonym());
        if (level < 16)
        {
            var subsystems = subsystem.getSubsystems();
            if (subsystems != null && !subsystems.isEmpty())
            {
                entity.subsystems =
                    subsystems.stream().map(s -> createSubsystem(s, level + 1)).collect(Collectors.toList());
            }
        }

        return entity;
    }

    private <T extends Field> List<FieldEntity> createFields(FieldSource fieldSource, Predicate<Field> filter)
    {
        if (fieldSource == null)
        {
            return null;
        }

        var result = new ArrayList<FieldEntity>();
        var sources = new Stack<FieldSource>();
        sources.push(fieldSource);
        while (!sources.isEmpty())
        {
            var source = sources.pop();
            var fields = source.getFields();
            if (fields != null)
            {
                for (var field : fields)
                {
                    if (!filter.test(field))
                    {
                        continue;
                    }

                    result.add(createField(field));
                }
            }

            sources.addAll(source.getRefFieldSources());
        }

        // var otherFields = FieldsSourceUtil.INSTANCE.getFields(fieldSource, f -> true);
        if (result.isEmpty())
        {
            return null;
        }

        return result;
    }

    private MetaEntity createBasedOn(IBmObject basedOn)
    {
        return createAndFillMetaEntity(basedOn, true);
    }

    private AttributeEntity createAttribute(BasicFeature attribute)
    {
        var entity = new AttributeEntity();
        entity.name = attribute.getName();
        entity.type = getTypeName(attribute);
        entity.comment = attribute.getComment();
        entity.toolTip = createMap(attribute.getToolTip());
        entity.synonym = createMap(attribute.getSynonym());
        entity.types = createTypes(attribute.getTypeDescription());
        entity.minValue = createValue(attribute.getMinValue());
        entity.maxValue = createValue(attribute.getMaxValue());
        return entity;
    }

    private AttributeEntity createStandardAttribute(StandardAttribute attribute)
    {
        var entity = new AttributeEntity();
        entity.name = attribute.getName();
        entity.type = getTypeName(attribute);
        entity.toolTip = createMap(attribute.getToolTip());
        entity.synonym = createMap(attribute.getSynonym());
        return entity;
    }

    private ObjectFormEntity createForm(BasicForm form)
    {
        var entity = new ObjectFormEntity();
        entity.name = form.getName();
        entity.type = getTypeName(form);
        entity.synonym = createMap(form.getSynonym());
        return entity;
    }

    private TabularSectionEntity createTabularSection(DbObjectTabularSection tabularSection)
    {
        var entity = new TabularSectionEntity();
        entity.name = tabularSection.getName();
        entity.type = getTypeName(tabularSection);
        entity.comment = tabularSection.getComment();
        entity.toolTip = createMap(tabularSection.getToolTip());
        entity.attributes = createAttributes(tabularSection.getAttributes());
        entity.fields = createFields(tabularSection, field -> isPublishedField(field));
        tabularSection.getRefFieldSources();
        return entity;
    }

    private RegisterResourceEntity createRegisterResource(RegisterResource registerResource)
    {
        var entity = new RegisterResourceEntity();
        entity.name = registerResource.getName();
        entity.type = getTypeName(registerResource);
        entity.comment = registerResource.getComment();
        entity.toolTip = createMap(registerResource.getToolTip());
        entity.synonym = createMap(registerResource.getSynonym());
        entity.types = createTypes(registerResource.getType());
        return entity;
    }

    private RegisterDimensionEntity createRegisterDimension(RegisterDimension registerDimension)
    {
        var entity = new RegisterDimensionEntity();
        entity.name = registerDimension.getName();
        entity.type = getTypeName(registerDimension);
        entity.comment = registerDimension.getComment();
        entity.toolTip = createMap(registerDimension.getToolTip());
        entity.synonym = createMap(registerDimension.getSynonym());
        entity.types = createTypes(registerDimension.getType());
        return entity;
    }

    private RegisterRecordEntity createBasicRegister(BasicRegister registerRecord)
    {
        var entity = new RegisterRecordEntity();
        entity.name = registerRecord.getName();
        entity.type = getTypeName(registerRecord);
        entity.comment = registerRecord.getComment();
        entity.synonym = createMap(registerRecord.getSynonym());
        // Too much info:
        // entity.fields = createFields(registerRecord, field -> isPublishedField(field));
        return entity;
    }

    private EnumValueEntity createEnumValue(EnumValue enumValue)
    {
        var entity = new EnumValueEntity();
        entity.name = enumValue.getName();
        entity.synonym = createMap(enumValue.getSynonym());
        return entity;
    }

    private PredefinedEntity createCatalogPredefined(CatalogPredefinedItem predefined)
    {
        var entity = new PredefinedEntity();
        entity.name = predefined.getName();
        entity.type = getTypeName(predefined);
        entity.description = predefined.getDescription();
        entity.value = createValue(predefined.getCode());
        entity.predefined = createCatalogPredefinedItems(predefined.getContent());
        return entity;
    }

    private PredefinedEntity createChartOfCharacteristicTypesPredefined(
        ChartOfCharacteristicTypesPredefinedItem predefined)
    {
        var entity = new PredefinedEntity();
        entity.name = predefined.getName();
        entity.type = getTypeName(predefined);
        entity.description = predefined.getDescription();
        entity.value = createValue(predefined.getCode());
        entity.predefined = createChartOfCharacteristicTypesPredefinedItems(predefined.getContent());
        return entity;
    }

    private PredefinedEntity createChartOfCalculationTypesPredefined(ChartOfCalculationTypesPredefinedItem predefined)
    {
        var entity = new PredefinedEntity();
        entity.name = predefined.getName();
        entity.type = getTypeName(predefined);
        entity.description = predefined.getDescription();
        entity.value = createValue(predefined.getCode());
        entity.displaced = createChartOfCalculationTypesPredefinedItems(predefined.getDisplaced());
        return entity;
    }

    private PredefinedEntity createChartOfAccountsPredefined(ChartOfAccountsPredefinedItem predefined)
    {
        var entity = new PredefinedEntity();
        entity.name = predefined.getName();
        entity.type = getTypeName(predefined);
        entity.description = predefined.getDescription();
        entity.value = createValue(predefined.getCode());
        entity.child = createChartOfAccountsPredefinedItems(predefined.getChildItems());
        return entity;
    }

    private TemplateEntity createTemplate(Template template)
    {
        var entity = new TemplateEntity();
        entity.name = template.getName();
        entity.type = getTypeName(template);
        entity.comment = template.getComment();
        entity.synonym = createMap(template.getSynonym());
        return entity;
    }

    private ValueEntity createValue(EObject valueObject)
    {
        var entity = new ValueEntity();
        if (valueObject == null)
        {
            entity.type = ValueType.NULL;
            return entity;
        }

        if (valueObject instanceof UndefinedValue)
        {
            entity.type = ValueType.UNDEFINED;
            return entity;
        }

        if (valueObject instanceof NullValue)
        {
            entity.type = ValueType.NULL;
            return entity;
        }

        if (valueObject instanceof BooleanValue)
        {
            entity.type = ValueType.BOOLEAN;
            entity.value = ((BooleanValue)valueObject).isValue();
            return entity;
        }

        if (valueObject instanceof NumberValue)
        {
            entity.type = ValueType.DECIMAL;
            entity.value = ((NumberValue)valueObject).getValue();
            return entity;
        }

        if (valueObject instanceof StringValue)
        {
            entity.type = ValueType.STRING;
            entity.value = ((StringValue)valueObject).getValue();
            return entity;
        }

        if (valueObject instanceof DateValue)
        {
            entity.type = ValueType.DATETIME;
            entity.value = ((DateValue)valueObject).getValue();
            return entity;
        }

        if (valueObject instanceof BinaryValue)
        {
            entity.type = ValueType.BINARY;
            entity.value = ((BinaryValue)valueObject).getValue();
            return entity;
        }

        if (valueObject instanceof ReferenceValue)
        {
            entity.type = ValueType.REFERENCE;
            final ReferenceValue refValueObject = (ReferenceValue)valueObject;
            entity.value = createValue(refValueObject.getValue());
        }

        if (valueObject instanceof IrresolvableReferenceValue)
        {
            entity.type = ValueType.IRRESORVABLE_REFERENCE;
            final IrresolvableReferenceValue referenceValue = (IrresolvableReferenceValue)valueObject;
            entity.value = String.format("%s.%s", referenceValue.getRefTypeId().toString(), //$NON-NLS-1$
                referenceValue.getInstanceId().toString());
            return entity;
        }

        if (valueObject instanceof ValueList)
        {
            entity.type = ValueType.LIST;
            entity.value =
                ((ValueList)valueObject).getValues().stream().map(this::createValue).collect(Collectors.toList());
            return entity;
        }

        if (valueObject instanceof FixedArrayValue)
        {
            entity.type = ValueType.ARRAY;
            entity.value =
                ((FixedArrayValue)valueObject).getValues().stream().map(this::createValue).collect(Collectors.toList());
            return entity;
        }

        if (valueObject instanceof TypeDescriptionValue)
        {
            entity.type = ValueType.TYPE;
            TypeDescription value = ((TypeDescriptionValue)valueObject).getValue();
            entity.value = createTypes(value);
            return entity;
        }

        if (valueObject instanceof StandardPeriodValue)
        {
            entity.type = ValueType.STANDARD_PERIOD;
            entity.value = createStandardPeriod(((StandardPeriodValue)valueObject).getValue());
            return entity;
        }

        if (valueObject instanceof FormChoiceListDesTimeValue)
        {
            entity.type = ValueType.FORM_CHOICE_LIST_DES_TIME;
            // entity.value = createStandardPeriod(((FormChoiceListDesTimeValue)valueObject).getValue());
            return entity;

            /*featureWriter.write(writer, (EObject)valueObject,
                FormPackage.Literals.FORM_CHOICE_LIST_DES_TIME_VALUE__PRESENTATION, writeEmpty, exportContext);
            featureWriter.write(writer, (EObject)valueObject,
                FormPackage.Literals.FORM_CHOICE_LIST_DES_TIME_VALUE__VALUE, writeEmpty, exportContext);*/
        }

        if (valueObject instanceof BorderValue)
        {
            entity.type = ValueType.BORDER;
            entity.value = createBorder(((BorderValue)valueObject).getValue());
            return entity;
        }

        if (valueObject instanceof ColorValue)
        {
            entity.type = ValueType.COLOR;
            entity.value = createColor(((ColorValue)valueObject).getValue());
            return entity;
        }

        if (valueObject instanceof FontValue)
        {
            entity.type = ValueType.FONT;
            entity.value = createFont(((FontValue)valueObject).getValue());
            return entity;
        }

        if (valueObject instanceof AccountTypeValue)
        {
            entity.type = ValueType.ACCOUNT_TYPE;
            entity.value = createAccountType(((AccountTypeValue)valueObject).getValue());
            return entity;
        }

        if (valueObject instanceof ChartLineTypeValue)
        {
            entity.type = ValueType.CHART_LINE_TYPE;
            entity.value = createChartLineType(((ChartLineTypeValue)valueObject).getValue());
            return entity;
        }

        if (valueObject instanceof EnumValue)
        {
            entity.type = ValueType.ENUM;
            entity.value = createEnumValue((EnumValue)valueObject);
            return entity;
        }

        if (valueObject instanceof SysEnumValue)
        {
            entity.type = ValueType.SYS_ENUM;
            SysEnumValue value = (SysEnumValue)valueObject;
            if (value.getValue() != null && value.getValue().indexOf('.') != -1)
            {
                String[] segments = value.getValue().split("\\."); //$NON-NLS-1$
                entity.value = new String[] { segments[0], segments[1] };
            }
        }

        entity.type = ValueType.UNKNOWN;
        return entity;
    }

    private ValueEntity createValue(String code)
    {
        var entity = new ValueEntity();
        entity.value = code;
        entity.type = ValueType.STRING;
        return entity;
    }

    private ChartLineTypeEntity createChartLineType(ChartLineType value)
    {
        var entity = new ChartLineTypeEntity();
        entity.name = value.getName();
        entity.literal = value.getLiteral();
        entity.value = value.getValue();
        return null;
    }

    private AccountTypeEntity createAccountType(AccountType value)
    {
        var entity = new AccountTypeEntity();
        entity.name = value.getName();
        entity.literal = value.getLiteral();
        entity.value = value.getValue();
        return entity;
    }

    private Object createFont(Font value)
    {
        var entity = new FontEntity();
        entity.bold = value.bold();
        entity.italic = value.italic();
        entity.underline = value.underline();
        entity.strikeout = value.strikeout();
        entity.faceName = value.faceName();
        entity.scale = value.scale();
        entity.height = value.height();
        return entity;
    }

    private Object createColor(Color value)
    {
        var entity = new ColorEntity();
        entity.red = value.red();
        entity.green = value.green();
        entity.blue = value.blue();
        return entity;
    }

    private Object createBorder(Border value)
    {
        var entity = new BorderEntity();
        entity.style = value.style().getName();
        entity.width = value.width();
        return entity;
    }

    private Object createStandardPeriod(StandardPeriod value)
    {
        var entity = new StandardPeriodEntity();
        entity.startDate = value.getStartDate();
        entity.endDate = value.getEndDate();
        return entity;
    }

    private boolean isPublishedField(Field field)
    {
        var name = field.getName();
        return name != null && !name.equals("Ref") && !name.equals("LineNumber"); //$NON-NLS-1$//$NON-NLS-2$
    }

    private Map<String, String> createMap(EMap<String, String> map)
    {
        if (map == null || map.isEmpty())
        {
            return null;
        }

        return map.map();
    }

    private List<DataType> createTypes(TypeDescription typeDescription)
    {
        if (typeDescription == null)
        {
            return null;
        }

        var types = typeDescription.getTypes();
        if (types == null || types.isEmpty())
        {
            return null;
        }

        return types.stream().map(this::createType).collect(Collectors.toList());
    }

    private DataType createType(TypeItem type)
    {
        var dataType = new DataType();
        dataType.type = type.getName();
        dataType.typeRu = type.getNameRu();
        return dataType;
    }

    private FieldEntity createField(Field field)
    {
        var entity = new FieldEntity();
        entity.name = field.getName();
        entity.nameRu = field.getNameRu();
        entity.types = createTypes(field.getType());
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
