/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.naming.ITopObjectFqnGenerator;
import com._1c.g5.v8.dt.core.platform.IDerivedDataManagerProvider;
import com._1c.g5.v8.dt.core.platform.IEditingLanguageManager;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.dcs.model.schema.DcsFactory;
import com._1c.g5.v8.dt.form.generator.FormType;
import com._1c.g5.v8.dt.form.generator.IFormFieldGenerator;
import com._1c.g5.v8.dt.form.generator.IFormGenerator;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.md.refactoring.core.IMdRefactoringService;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicForm;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.DataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.Report;
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
import com._1c.g5.v8.dt.moxel.sheet.SheetFactory;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IProjectBuilder;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.ToolErrorType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
final class MetadataMutationService
{
    private static final String IDENTIFIER_PATTERN = "[\\p{L}_][\\p{L}\\p{N}_]*"; //$NON-NLS-1$

    private final IBmModelManager modelManager;
    private final ITopObjectFqnGenerator fqnGenerator;
    private final IEditingSupport editingSupport;
    private final IMdRefactoringService refactoringService;
    private final MetadataTypeService typeService;
    private final IProjectBuilder projectBuilder;
    private final IDerivedDataManagerProvider derivedDataManagerProvider;
    private final ISettings settings;
    private final IV8ProjectManager v8ProjectManager;
    private final IFormGenerator formGenerator;
    private final IFormFieldGenerator formFieldGenerator;
    private final IEditingLanguageManager editingLanguageManager;

    @Inject
    MetadataMutationService(IBmModelManager modelManager, ITopObjectFqnGenerator fqnGenerator,
        IEditingSupport editingSupport, IMdRefactoringService refactoringService, MetadataTypeService typeService,
        IProjectBuilder projectBuilder, IDerivedDataManagerProvider derivedDataManagerProvider, ISettings settings,
        IV8ProjectManager v8ProjectManager, IFormGenerator formGenerator, IFormFieldGenerator formFieldGenerator,
        IEditingLanguageManager editingLanguageManager)
    {
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(fqnGenerator);
        Preconditions.checkNotNull(editingSupport);
        Preconditions.checkNotNull(refactoringService);
        Preconditions.checkNotNull(typeService);
        Preconditions.checkNotNull(projectBuilder);
        Preconditions.checkNotNull(derivedDataManagerProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(formGenerator);
        Preconditions.checkNotNull(formFieldGenerator);
        Preconditions.checkNotNull(editingLanguageManager);
        this.modelManager = modelManager;
        this.fqnGenerator = fqnGenerator;
        this.editingSupport = editingSupport;
        this.refactoringService = refactoringService;
        this.typeService = typeService;
        this.projectBuilder = projectBuilder;
        this.derivedDataManagerProvider = derivedDataManagerProvider;
        this.settings = settings;
        this.v8ProjectManager = v8ProjectManager;
        this.formGenerator = formGenerator;
        this.formFieldGenerator = formFieldGenerator;
        this.editingLanguageManager = editingLanguageManager;
    }

    synchronized MetadataResponse execute(MetadataRequest request, ICancellationToken cancellationToken)
    {
        checkCanceled(cancellationToken);
        var project = project(request.projectName);
        if ("inspectObject".equals(request.operation)) //$NON-NLS-1$
        {
            return inspectObject(project, request);
        }
        if (editingSupport.isReadOnly(project))
        {
            throw new ToolException("Project is read-only according to EDT editing rules: " + project.getName()); //$NON-NLS-1$
        }

        MetadataResponse response;
        switch (request.operation)
        {
        case "createObject": //$NON-NLS-1$
            response = createObject(project, request);
            break;
        case "setObjectProperty": //$NON-NLS-1$
            response = setObjectProperty(project, request);
            break;
        case "renameObject": //$NON-NLS-1$
            response = renameObject(project, request);
            break;
        case "removeObject": //$NON-NLS-1$
            response = removeObject(project, request);
            break;
        case "addObjectAttribute": //$NON-NLS-1$
            response = addFeature(project, request, "attributes", FeatureKind.ATTRIBUTE); //$NON-NLS-1$
            break;
        case "removeObjectAttribute": //$NON-NLS-1$
            response = removeFeature(project, request, "attributes", null, request.objectName); //$NON-NLS-1$
            break;
        case "addTabularSection": //$NON-NLS-1$
            response = addFeature(project, request, "tabularSections", FeatureKind.TABULAR_SECTION); //$NON-NLS-1$
            break;
        case "removeTabularSection": //$NON-NLS-1$
            response = removeFeature(project, request, "tabularSections", null, request.objectName); //$NON-NLS-1$
            break;
        case "addTabularSectionAttribute": //$NON-NLS-1$
            response = addFeature(project, request, "attributes", FeatureKind.TABULAR_SECTION_ATTRIBUTE); //$NON-NLS-1$
            break;
        case "removeTabularSectionAttribute": //$NON-NLS-1$
            response = removeTabularSectionAttribute(project, request);
            break;
        case "addEnumValue": //$NON-NLS-1$
            response = addFeature(project, request, "enumValues", FeatureKind.ENUM_VALUE); //$NON-NLS-1$
            break;
        case "removeEnumValue": //$NON-NLS-1$
            response = removeFeature(project, request, "enumValues", null, request.objectName); //$NON-NLS-1$
            break;
        case "addRegisterField": //$NON-NLS-1$
            response = addRegisterField(project, request);
            break;
        case "removeRegisterField": //$NON-NLS-1$
            response = removeRegisterField(project, request);
            break;
        case "setChildProperty": //$NON-NLS-1$
            response = setChildProperty(project, request);
            break;
        case "setChildType": //$NON-NLS-1$
            response = setChildType(project, request);
            break;
        case "renameChild": //$NON-NLS-1$
            response = renameChild(project, request);
            break;
        case "addDocumentRegister": //$NON-NLS-1$
            response = changeDocumentRegister(project, request, true);
            break;
        case "removeDocumentRegister": //$NON-NLS-1$
            response = changeDocumentRegister(project, request, false);
            break;
        case "createObjectForm": //$NON-NLS-1$
            response = createObjectForm(project, request);
            break;
        case "removeObjectForm": //$NON-NLS-1$
            response = removeObjectArtifact(project, request, "forms"); //$NON-NLS-1$
            break;
        case "createObjectTemplate": //$NON-NLS-1$
            response = createObjectTemplate(project, request);
            break;
        case "removeObjectTemplate": //$NON-NLS-1$
            response = removeObjectArtifact(project, request, "templates"); //$NON-NLS-1$
            break;
        default:
            throw new ToolException("Operation is not executable: " + request.operation); //$NON-NLS-1$
        }
        response.resourcePath = metadataResourcePath(project, response.target);
        response.markerPath = response.resourcePath;
        response.artifactPath = artifactPath(project, request);

        checkCanceled(cancellationToken);
        if (response.changed && !request.dryRun)
        {
            waitForPersistence(project, cancellationToken);
            long persistenceTimeoutMillis = persistenceTimeoutMillis();
            awaitResourceState(request, response, cancellationToken, persistenceTimeoutMillis);
            awaitArtifactState(request, response, cancellationToken, persistenceTimeoutMillis);
            refresh(project);
            boolean validationComplete = false;
            try
            {
                validationComplete = projectBuilder.build(project, cancellationToken);
            }
            catch (org.eclipse.core.runtime.CoreException e)
            {
                response.warnings.add("EDT validation failed to start: " + e.getMessage()); //$NON-NLS-1$
            }
            if (!validationComplete)
            {
                response.warnings.add("EDT validation did not settle before the configured timeout; marker results may be incomplete."); //$NON-NLS-1$
            }
        }
        if (!request.dryRun)
        {
            verifyResourceState(request, response);
            verifyArtifactState(request, response);
        }
        return response;
    }

    private long persistenceTimeoutMillis()
    {
        var timeout = settings.getTimeout();
        long configured = timeout != null ? timeout.toMillis() : 0L;
        return Math.max(configured, 120_000L);
    }

    private void waitForPersistence(IProject project, ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            return;
        }
        try
        {
            var manager = derivedDataManagerProvider.get(project);
            if (manager != null)
            {
                var timeout = settings.getTimeout();
                long timeoutMillis = timeout != null ? timeout.toMillis() : -1L;
                manager.waitComputation(timeoutMillis, false, "EXP_O", "EXP_B"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (Exception e)
        {
            // The concrete resource-state check below is the persistence source of truth.
        }
    }

    private static void awaitResourceState(MetadataRequest request, MetadataResponse response,
        ICancellationToken cancellationToken, long timeoutMillis)
    {
        if (!"createObject".equals(request.operation) && !"removeObject".equals(request.operation)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        boolean expectedExists = !"removeObject".equals(request.operation); //$NON-NLS-1$
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (Files.exists(Paths.get(response.resourcePath)) != expectedExists
            && System.currentTimeMillis() < deadline && !cancellationToken.isCanceled())
        {
            try
            {
                Thread.sleep(50L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void awaitArtifactState(MetadataRequest request, MetadataResponse response,
        ICancellationToken cancellationToken, long timeoutMillis)
    {
        if (response.artifactPath == null)
        {
            return;
        }
        boolean expectedExists = request.operation.startsWith("create"); //$NON-NLS-1$
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (Files.exists(Paths.get(response.artifactPath)) != expectedExists
            && System.currentTimeMillis() < deadline && !cancellationToken.isCanceled())
        {
            try
            {
                Thread.sleep(50L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void verifyArtifactState(MetadataRequest request, MetadataResponse response)
    {
        if (response.artifactPath == null || !response.changed)
        {
            return;
        }
        boolean expectedExists = request.operation.startsWith("create"); //$NON-NLS-1$
        if (Files.exists(Paths.get(response.artifactPath)) != expectedExists)
        {
            throw new ToolException("EDT did not persist the expected artifact state: " + response.artifactPath, //$NON-NLS-1$
                ToolErrorType.USER_VISIBLE);
        }
    }

    private static void verifyResourceState(MetadataRequest request, MetadataResponse response)
    {
        var resourceExists = Files.exists(Paths.get(response.resourcePath));
        if ("removeObject".equals(request.operation) && resourceExists) //$NON-NLS-1$
        {
            throw new ToolException("EDT removed the object from BM, but its metadata resource still exists: " //$NON-NLS-1$
                + response.resourcePath, ToolErrorType.USER_VISIBLE);
        }
        if (response.changed && !"removeObject".equals(request.operation) && !resourceExists) //$NON-NLS-1$
        {
            throw new ToolException("EDT changed BM, but the expected metadata resource was not persisted: " //$NON-NLS-1$
                + response.resourcePath, ToolErrorType.USER_VISIBLE);
        }
    }

    private MetadataResponse inspectObject(IProject project, MetadataRequest request)
    {
        var response = MetadataResponse.success(request, request.objectName, false);
        response.resourcePath = metadataResourcePath(project, request.objectName);
        response.markerPath = response.resourcePath;
        response.details = model(project).getGlobalContext().execute(new AbstractBmTask<Map<String, Object>>(
            "Inspect 1C metadata object") //$NON-NLS-1$
        {
            @Override
            public Map<String, Object> execute(IBmTransaction transaction,
                org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                return describeObject(requireObject(transaction, request.objectName));
            }
        });
        return response;
    }

    private static Map<String, Object> describeObject(MdObject object)
    {
        var result = new LinkedHashMap<String, Object>();
        result.put("class", object.eClass().getName()); //$NON-NLS-1$
        result.put("name", object.getName()); //$NON-NLS-1$
        result.put("synonym", object.getSynonym().get("ru")); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("properties", scalarProperties(object)); //$NON-NLS-1$
        var children = new LinkedHashMap<String, Object>();
        for (var featureName : List.of("attributes", "tabularSections", "enumValues", "dimensions", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "resources", "forms", "templates")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            var feature = object.eClass().getEStructuralFeature(featureName);
            if (feature != null && feature.isMany())
            {
                @SuppressWarnings("unchecked")
                var values = (List<Object>)object.eGet(feature);
                var descriptions = new java.util.ArrayList<Map<String, Object>>();
                for (var value : values)
                {
                    if (value instanceof MdObject)
                    {
                        descriptions.add(describeChild((MdObject)value));
                    }
                }
                children.put(featureName, descriptions);
            }
        }
        result.put("children", children); //$NON-NLS-1$
        if (object instanceof Document)
        {
            var registers = new java.util.ArrayList<String>();
            for (var register : ((Document)object).getRegisterRecords())
            {
                registers.add(register.eClass().getName() + "." + register.getName()); //$NON-NLS-1$
            }
            result.put("register_records", registers); //$NON-NLS-1$
        }
        return result;
    }

    private static Map<String, Object> describeChild(MdObject child)
    {
        var result = new LinkedHashMap<String, Object>();
        result.put("class", child.eClass().getName()); //$NON-NLS-1$
        result.put("name", child.getName()); //$NON-NLS-1$
        result.put("synonym", child.getSynonym().get("ru")); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("properties", scalarProperties(child)); //$NON-NLS-1$
        if (child instanceof BasicFeature)
        {
            var type = ((BasicFeature)child).getTypeDescription();
            var names = new java.util.ArrayList<String>();
            if (type != null)
            {
                for (var item : type.getTypes())
                {
                    names.add(item.getName());
                }
                var qualifiers = new LinkedHashMap<String, Object>();
                for (var qualifierName : List.of("stringQualifiers", "numberQualifiers", "dateQualifiers")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                {
                    var qualifier = type.eClass().getEStructuralFeature(qualifierName);
                    if (qualifier != null && type.eGet(qualifier) instanceof EObject)
                    {
                        qualifiers.put(qualifierName, scalarProperties((EObject)type.eGet(qualifier)));
                    }
                }
                result.put("type_qualifiers", qualifiers); //$NON-NLS-1$
            }
            result.put("types", names); //$NON-NLS-1$
        }
        var attributes = child.eClass().getEStructuralFeature("attributes"); //$NON-NLS-1$
        if (attributes != null && attributes.isMany())
        {
            @SuppressWarnings("unchecked")
            var values = (List<Object>)child.eGet(attributes);
            var descriptions = new java.util.ArrayList<Map<String, Object>>();
            for (var value : values)
            {
                if (value instanceof MdObject)
                {
                    descriptions.add(describeChild((MdObject)value));
                }
            }
            result.put("attributes", descriptions); //$NON-NLS-1$
        }
        return result;
    }

    private static Map<String, Object> scalarProperties(EObject object)
    {
        var result = new LinkedHashMap<String, Object>();
        for (var feature : object.eClass().getEAllStructuralFeatures())
        {
            if (!feature.isMany() && feature.getEType() instanceof EDataType && object.eIsSet(feature))
            {
                var value = object.eGet(feature);
                if (value != null)
                {
                    result.put(feature.getName(), String.valueOf(value));
                }
            }
        }
        return result;
    }

    private MetadataResponse setChildProperty(IProject project, MetadataRequest request)
    {
        if ("name".equalsIgnoreCase(request.propertyName)) //$NON-NLS-1$
        {
            throw new ToolException("Use `renameChild` to change a child name."); //$NON-NLS-1$
        }
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Set 1C metadata child property") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var child = requireChild(transaction, request);
                if ("synonym".equalsIgnoreCase(request.propertyName)) //$NON-NLS-1$
                {
                    var old = child.getSynonym().get("ru"); //$NON-NLS-1$
                    changed[0] = !java.util.Objects.equals(old, request.propertyValue);
                    if (changed[0] && !request.dryRun)
                    {
                        child.getSynonym().put("ru", request.propertyValue); //$NON-NLS-1$
                    }
                    return null;
                }
                changed[0] = setScalarProperty(child, request.propertyName, request.propertyValue, request.dryRun);
                return null;
            }
        });
        return MetadataResponse.success(request, childTarget(request), changed[0]);
    }

    private MetadataResponse setChildType(IProject project, MetadataRequest request)
    {
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Set 1C metadata child type") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var child = requireChild(transaction, request);
                if (!(child instanceof BasicFeature))
                {
                    throw new ToolException("Child does not have a configurable type: " + childTarget(request)); //$NON-NLS-1$
                }
                var newType = typeService.create(project, transaction, request);
                changed[0] = !EcoreUtil.equals(((BasicFeature)child).getTypeDescription(), newType);
                if (changed[0] && !request.dryRun)
                {
                    ((BasicFeature)child).setType(newType);
                }
                return null;
            }
        });
        return MetadataResponse.success(request, childTarget(request), changed[0]);
    }

    private MetadataResponse renameChild(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.newName, "new_name"); //$NON-NLS-1$
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Rename 1C metadata child") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var location = childLocation(transaction, request);
                var child = findNamed(location.children, request.name);
                if (child == null)
                {
                    throw new ToolException("Child metadata object not found: " + childTarget(request)); //$NON-NLS-1$
                }
                if (findNamed(location.children, request.newName) != null)
                {
                    throw new ToolException("A child with the new name already exists: " + request.newName); //$NON-NLS-1$
                }
                changed[0] = !request.newName.equals(child.getName());
                if (changed[0] && !request.dryRun)
                {
                    child.setName(request.newName);
                }
                return null;
            }
        });
        return MetadataResponse.success(request, request.objectName + "." + request.newName, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse changeDocumentRegister(IProject project, MetadataRequest request, boolean add)
    {
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Change document register records") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var owner = requireObject(transaction, request.objectName);
                var related = requireObject(transaction, request.relatedObjectName);
                if (!(owner instanceof Document))
                {
                    throw new ToolException("`object_name` must identify a Document."); //$NON-NLS-1$
                }
                if (!(related instanceof BasicRegister) || related instanceof InformationRegister)
                {
                    throw new ToolException("Only accumulation, accounting, or calculation registers can be document records."); //$NON-NLS-1$
                }
                var records = ((Document)owner).getRegisterRecords();
                boolean contains = records.contains(related);
                changed[0] = add ? !contains : contains;
                if (changed[0] && !request.dryRun)
                {
                    if (add)
                    {
                        records.add((BasicRegister)related);
                    }
                    else
                    {
                        records.remove(related);
                    }
                }
                return null;
            }
        });
        return MetadataResponse.success(request, request.objectName, changed[0]);
    }

    private MetadataResponse createObjectForm(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        final FormType formType;
        try
        {
            formType = FormType.valueOf(request.formType.toUpperCase(Locale.ROOT));
        }
        catch (RuntimeException e)
        {
            throw new ToolException("Invalid `form_type`. Valid values: OBJECT, LIST, RECORD, REPORT, CONSTANTS."); //$NON-NLS-1$
        }
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Create generated 1C object form") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var owner = requireObject(transaction, request.objectName);
                var forms = featureList(owner, "forms"); //$NON-NLS-1$
                if (findNamed(forms, request.name) != null)
                {
                    return null;
                }
                var formClass = MdClassPackage.eINSTANCE.getEClassifier(owner.eClass().getName() + "Form"); //$NON-NLS-1$
                if (!(formClass instanceof EClass))
                {
                    throw new ToolException("Object type does not support generated forms: " + owner.eClass().getName()); //$NON-NLS-1$
                }
                var created = MdClassFactory.eINSTANCE.create((EClass)formClass);
                if (!(created instanceof BasicForm))
                {
                    throw new ToolException("EDT form metadata class is not a BasicForm: " + formClass.getName()); //$NON-NLS-1$
                }
                var formMetadata = (BasicForm)created;
                formMetadata.setName(request.name);
                formMetadata.setUuid(UUID.randomUUID());
                if (request.title != null && !request.title.isBlank())
                {
                    formMetadata.getSynonym().put("ru", request.title); //$NON-NLS-1$
                }
                changed[0] = true;
                if (request.dryRun)
                {
                    return null;
                }
                forms.add(formMetadata);
                var v8Project = v8ProjectManager.getProject(project);
                if (v8Project == null)
                {
                    throw new ToolException("V8 project is not available: " + project.getName()); //$NON-NLS-1$
                }
                var scriptVariant = v8Project.getScriptVariant();
                var version = v8Project.getVersion();
                var languageCode = editingLanguageManager.getEditingLanguageCode(project);
                var rootField = formFieldGenerator.getFormGeneratorFields(owner, formType, scriptVariant, version);
                Form form = formGenerator.generateForm(owner, formMetadata, formType, scriptVariant,
                    languageCode, version, rootField, Integer.valueOf(1));
                formMetadata.setForm(form);
                form.setMdForm(formMetadata);
                var formReference = (org.eclipse.emf.ecore.EReference)formMetadata.eClass()
                    .getEStructuralFeature("form"); //$NON-NLS-1$
                var formFqn = fqnGenerator.generateExternalPropertyFqn(formMetadata, formReference);
                transaction.attachTopObject((IBmObject)form, formFqn);
                setDefaultForm(owner, formMetadata, formType);
                return null;
            }
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private static void setDefaultForm(MdObject owner, BasicForm form, FormType formType)
    {
        String featureName;
        switch (formType)
        {
        case OBJECT: featureName = "defaultObjectForm"; break; //$NON-NLS-1$
        case LIST: featureName = "defaultListForm"; break; //$NON-NLS-1$
        case RECORD: featureName = "defaultRecordForm"; break; //$NON-NLS-1$
        case REPORT: featureName = "defaultForm"; break; //$NON-NLS-1$
        case CONSTANTS: featureName = "defaultForm"; break; //$NON-NLS-1$
        default: return;
        }
        var feature = owner.eClass().getEStructuralFeature(featureName);
        if (feature != null && owner.eGet(feature) == null)
        {
            owner.eSet(feature, form);
        }
    }

    private MetadataResponse createObjectTemplate(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        final TemplateType templateType;
        try
        {
            templateType = TemplateType.valueOf(request.templateType.toUpperCase(Locale.ROOT));
        }
        catch (RuntimeException e)
        {
            throw new ToolException("Invalid `template_type`. Valid values: SPREADSHEET_DOCUMENT, DATA_COMPOSITION_SCHEMA."); //$NON-NLS-1$
        }
        if (templateType != TemplateType.SPREADSHEET_DOCUMENT
            && templateType != TemplateType.DATA_COMPOSITION_SCHEMA)
        {
            throw new ToolException("Only SPREADSHEET_DOCUMENT and DATA_COMPOSITION_SCHEMA are supported without source content."); //$NON-NLS-1$
        }
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Create 1C object template") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var owner = requireObject(transaction, request.objectName);
                var templates = featureList(owner, "templates"); //$NON-NLS-1$
                if (findNamed(templates, request.name) != null)
                {
                    return null;
                }
                var template = MdClassFactory.eINSTANCE.createTemplate();
                template.setName(request.name);
                template.setUuid(UUID.randomUUID());
                template.setTemplateType(templateType);
                if (request.title != null && !request.title.isBlank())
                {
                    template.getSynonym().put("ru", request.title); //$NON-NLS-1$
                }
                changed[0] = true;
                if (request.dryRun)
                {
                    return null;
                }
                templates.add(template);
                EObject body = templateType == TemplateType.SPREADSHEET_DOCUMENT
                    ? SheetFactory.createSpreadsheetDocument() : DcsFactory.eINSTANCE.createDataCompositionSchema();
                template.setTemplate(body);
                var contentFqn = fqnGenerator.generateExternalPropertyFqn(template,
                    MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
                transaction.attachTopObject((IBmObject)body, contentFqn);
                return null;
            }
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse removeObjectArtifact(IProject project, MetadataRequest request, String collection)
    {
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Remove 1C object artifact") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var owner = requireObject(transaction, request.objectName);
                var child = findNamed(featureList(owner, collection), request.name);
                if (child == null)
                {
                    return null;
                }
                changed[0] = true;
                if (request.dryRun)
                {
                    return null;
                }
                EObject body = null;
                if (child instanceof BasicForm)
                {
                    body = ((BasicForm)child).getForm();
                }
                else if (child instanceof Template)
                {
                    body = ((Template)child).getTemplate();
                }
                if (body instanceof IBmObject)
                {
                    transaction.detachTopObject((IBmObject)body);
                }
                EcoreUtil.delete(child, true);
                return null;
            }
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse createObject(IProject project, MetadataRequest request)
    {
        var name = objectParts(request.objectName)[1];
        validateIdentifier(name, "object_name"); //$NON-NLS-1$
        var model = model(project);
        boolean[] changed = { false };
        model.getGlobalContext().execute(new AbstractBmTask<Void>("Create 1C metadata object") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                if (transaction.getTopObjectByFqn(request.objectName) != null)
                {
                    return null;
                }
                var object = createTopObject(request.objectName);
                object.setName(name);
                object.setUuid(UUID.randomUUID());
                if (request.title != null && !request.title.isBlank())
                {
                    object.getSynonym().put("ru", request.title); //$NON-NLS-1$
                }
                if (request.dryRun)
                {
                    changed[0] = true;
                    return null;
                }
                var configuration = (Configuration)transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                if (configuration == null)
                {
                    throw new ToolException("Configuration top object is not available."); //$NON-NLS-1$
                }
                var fqn = fqnGenerator.generateStandaloneObjectFqn(object.eClass(), object.getName()).toString();
                transaction.attachTopObject((IBmObject)object, fqn);
                addToFeature(configuration, topCollection(request.objectName), object);
                changed[0] = true;
                return null;
            }
        });
        if (changed[0] && !request.dryRun)
        {
            readObject(project, request.objectName);
        }
        return MetadataResponse.success(request, request.objectName, changed[0]);
    }

    private MetadataResponse setObjectProperty(IProject project, MetadataRequest request)
    {
        if ("name".equalsIgnoreCase(request.propertyName)) //$NON-NLS-1$
        {
            throw new ToolException("Use `renameObject` to change an object name."); //$NON-NLS-1$
        }
        var model = model(project);
        boolean[] changed = { false };
        model.getGlobalContext().execute(new AbstractBmTask<Void>("Set 1C metadata property") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var object = requireObject(transaction, request.objectName);
                if ("synonym".equalsIgnoreCase(request.propertyName)) //$NON-NLS-1$
                {
                    var old = object.getSynonym().get("ru"); //$NON-NLS-1$
                    changed[0] = !java.util.Objects.equals(old, request.propertyValue);
                    if (changed[0] && !request.dryRun)
                    {
                        object.getSynonym().put("ru", request.propertyValue); //$NON-NLS-1$
                    }
                    return null;
                }

                var feature = object.eClass().getEStructuralFeature(request.propertyName);
                if (feature == null || feature.isMany() || !(feature.getEType() instanceof EDataType))
                {
                    throw new ToolException("Unsupported scalar property `" + request.propertyName //$NON-NLS-1$
                        + "` for " + request.objectName + "."); //$NON-NLS-1$ //$NON-NLS-2$
                }
                var value = EcoreUtil.createFromString((EDataType)feature.getEType(), request.propertyValue);
                changed[0] = !java.util.Objects.equals(object.eGet(feature), value);
                if (changed[0] && !request.dryRun)
                {
                    object.eSet(feature, value);
                }
                return null;
            }
        });
        return MetadataResponse.success(request, request.objectName, changed[0]);
    }

    private MetadataResponse renameObject(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.newName, "new_name"); //$NON-NLS-1$
        var oldObject = readObject(project, request.objectName);
        var parts = objectParts(request.objectName);
        var newFqn = parts[0] + "." + request.newName; //$NON-NLS-1$
        if (readObjectOrNull(project, newFqn) != null)
        {
            throw new ToolException("Target object already exists: " + newFqn); //$NON-NLS-1$
        }
        if (!request.dryRun)
        {
            Collection<com._1c.g5.v8.dt.refactoring.core.IRefactoring> refactorings =
                refactoringService.createMdObjectRenameRefactoring(oldObject, request.newName);
            for (var refactoring : refactorings)
            {
                refactoring.perform();
            }
            refresh(project);
            if (readObjectOrNull(project, request.objectName) != null || readObjectOrNull(project, newFqn) == null)
            {
                throw new ToolException("EDT rename did not produce the expected metadata object: " + newFqn); //$NON-NLS-1$
            }
        }
        return MetadataResponse.success(request, newFqn, true);
    }

    private MetadataResponse removeObject(IProject project, MetadataRequest request)
    {
        var object = readObjectOrNull(project, request.objectName);
        if (object == null)
        {
            return MetadataResponse.success(request, request.objectName, false);
        }
        if (!request.dryRun)
        {
            refactoringService.createMdObjectDeleteRefactoring(List.of(object)).perform();
            refresh(project);
            if (readObjectOrNull(project, request.objectName) != null)
            {
                throw new ToolException("EDT delete did not remove the metadata object: " + request.objectName); //$NON-NLS-1$
            }
        }
        return MetadataResponse.success(request, request.objectName, true);
    }

    private MetadataResponse addFeature(IProject project, MetadataRequest request, String featureName, FeatureKind kind)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        var ownerFqn = request.objectName;
        String tabularSectionName = null;
        if (kind == FeatureKind.TABULAR_SECTION_ATTRIBUTE)
        {
            var parts = tabularSectionParts(request.objectName);
            ownerFqn = parts[0];
            tabularSectionName = parts[1];
        }
        final String finalOwnerFqn = ownerFqn;
        final String finalTabularSectionName = tabularSectionName;
        var model = model(project);
        boolean[] changed = { false };
        String[] target = { finalOwnerFqn + "." + request.name }; //$NON-NLS-1$
        model.getGlobalContext().execute(new AbstractBmTask<Void>("Add 1C metadata child") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                MdObject owner = requireObject(transaction, finalOwnerFqn);
                if (kind == FeatureKind.TABULAR_SECTION_ATTRIBUTE)
                {
                    owner = requireNamedChild(owner, "tabularSections", finalTabularSectionName); //$NON-NLS-1$
                    target[0] = finalOwnerFqn + "." + finalTabularSectionName + "." + request.name; //$NON-NLS-1$ //$NON-NLS-2$
                }
                var list = featureList(owner, featureName);
                if (findNamed(list, request.name) != null)
                {
                    return null;
                }
                var child = createChild(owner, kind, featureName);
                child.setName(request.name);
                child.setUuid(UUID.randomUUID());
                if (request.title != null && !request.title.isBlank())
                {
                    child.getSynonym().put("ru", request.title); //$NON-NLS-1$
                }
                if (child instanceof BasicFeature)
                {
                    ((BasicFeature)child).setType(typeService.create(project, transaction, request));
                }
                changed[0] = true;
                if (!request.dryRun)
                {
                    list.add(child);
                }
                return null;
            }
        });
        return MetadataResponse.success(request, target[0], changed[0]);
    }

    private MetadataResponse removeFeature(IProject project, MetadataRequest request, String featureName,
        String tabularSectionName, String ownerFqn)
    {
        var model = model(project);
        boolean[] changed = { false };
        String[] target = { ownerFqn + "." + request.name }; //$NON-NLS-1$
        model.getGlobalContext().execute(new AbstractBmTask<Void>("Remove 1C metadata child") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                MdObject owner = requireObject(transaction, ownerFqn);
                if (tabularSectionName != null)
                {
                    owner = requireNamedChild(owner, "tabularSections", tabularSectionName); //$NON-NLS-1$
                    target[0] = ownerFqn + "." + tabularSectionName + "." + request.name; //$NON-NLS-1$ //$NON-NLS-2$
                }
                var child = findNamed(featureList(owner, featureName), request.name);
                if (child == null)
                {
                    return null;
                }
                changed[0] = true;
                if (!request.dryRun)
                {
                    EcoreUtil.delete((EObject)child, true);
                }
                return null;
            }
        });
        return MetadataResponse.success(request, target[0], changed[0]);
    }

    private MetadataResponse addRegisterField(IProject project, MetadataRequest request)
    {
        var feature = registerFeature(request.fieldKind);
        return addFeature(project, request, feature, FeatureKind.REGISTER_FIELD);
    }

    private MetadataResponse removeRegisterField(IProject project, MetadataRequest request)
    {
        return removeFeature(project, request, registerFeature(request.fieldKind), null, request.objectName);
    }

    private MetadataResponse removeTabularSectionAttribute(IProject project, MetadataRequest request)
    {
        var parts = tabularSectionParts(request.objectName);
        return removeFeature(project, request, "attributes", parts[1], parts[0]); //$NON-NLS-1$
    }

    private MdObject createTopObject(String fqn)
    {
        switch (objectParts(fqn)[0])
        {
        case "Catalog": return MdClassFactory.eINSTANCE.createCatalog(); //$NON-NLS-1$
        case "Document": return MdClassFactory.eINSTANCE.createDocument(); //$NON-NLS-1$
        case "Enum": return MdClassFactory.eINSTANCE.createEnum(); //$NON-NLS-1$
        case "InformationRegister": return MdClassFactory.eINSTANCE.createInformationRegister(); //$NON-NLS-1$
        case "AccumulationRegister": return MdClassFactory.eINSTANCE.createAccumulationRegister(); //$NON-NLS-1$
        case "Report": return MdClassFactory.eINSTANCE.createReport(); //$NON-NLS-1$
        case "DataProcessor": return MdClassFactory.eINSTANCE.createDataProcessor(); //$NON-NLS-1$
        case "CommonModule": return MdClassFactory.eINSTANCE.createCommonModule(); //$NON-NLS-1$
        case "Subsystem": return MdClassFactory.eINSTANCE.createSubsystem(); //$NON-NLS-1$
        case "Constant": return MdClassFactory.eINSTANCE.createConstant(); //$NON-NLS-1$
        default:
            throw new ToolException("Unsupported object type `" + objectParts(fqn)[0] //$NON-NLS-1$
                + "`. Supported: Catalog, Document, Enum, InformationRegister, AccumulationRegister, Report, DataProcessor, CommonModule, Subsystem, Constant."); //$NON-NLS-1$
        }
    }

    private static String topCollection(String fqn)
    {
        switch (objectParts(fqn)[0])
        {
        case "Catalog": return "catalogs"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Document": return "documents"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Enum": return "enums"; //$NON-NLS-1$ //$NON-NLS-2$
        case "InformationRegister": return "informationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
        case "AccumulationRegister": return "accumulationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Report": return "reports"; //$NON-NLS-1$ //$NON-NLS-2$
        case "DataProcessor": return "dataProcessors"; //$NON-NLS-1$ //$NON-NLS-2$
        case "CommonModule": return "commonModules"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Subsystem": return "subsystems"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Constant": return "constants"; //$NON-NLS-1$ //$NON-NLS-2$
        default: throw new ToolException("Unsupported object type: " + objectParts(fqn)[0]); //$NON-NLS-1$
        }
    }

    private static String metadataResourcePath(IProject project, String target)
    {
        var relativePath = metadataRelativePath(target);
        var location = project.getFile(relativePath).getLocation();
        return location != null ? location.toOSString() : relativePath;
    }

    static String metadataRelativePath(String target)
    {
        var parts = target != null ? target.split("\\.", -1) : new String[0]; //$NON-NLS-1$
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank())
        {
            throw new ToolException("Cannot derive metadata resource path from target: " + target); //$NON-NLS-1$
        }
        var folder = topFolder(parts[0]);
        return "src/" + folder + "/" + parts[1] + "/" + parts[1] + ".mdo"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    private static String topFolder(String type)
    {
        switch (type)
        {
        case "Catalog": return "Catalogs"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Document": return "Documents"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Enum": return "Enums"; //$NON-NLS-1$ //$NON-NLS-2$
        case "InformationRegister": return "InformationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
        case "AccumulationRegister": return "AccumulationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Report": return "Reports"; //$NON-NLS-1$ //$NON-NLS-2$
        case "DataProcessor": return "DataProcessors"; //$NON-NLS-1$ //$NON-NLS-2$
        case "CommonModule": return "CommonModules"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Subsystem": return "Subsystems"; //$NON-NLS-1$ //$NON-NLS-2$
        case "Constant": return "Constants"; //$NON-NLS-1$ //$NON-NLS-2$
        default: throw new ToolException("Unsupported metadata resource type: " + type); //$NON-NLS-1$
        }
    }

    private MdObject createChild(MdObject owner, FeatureKind kind, String featureName)
    {
        if (kind == FeatureKind.ENUM_VALUE && owner instanceof com._1c.g5.v8.dt.metadata.mdclass.Enum)
        {
            return MdClassFactory.eINSTANCE.createEnumValue();
        }
        if (kind == FeatureKind.ATTRIBUTE)
        {
            if (owner instanceof Catalog) return MdClassFactory.eINSTANCE.createCatalogAttribute();
            if (owner instanceof Document) return MdClassFactory.eINSTANCE.createDocumentAttribute();
            if (owner instanceof Report) return MdClassFactory.eINSTANCE.createReportAttribute();
            if (owner instanceof DataProcessor) return MdClassFactory.eINSTANCE.createDataProcessorAttribute();
        }
        if (kind == FeatureKind.TABULAR_SECTION)
        {
            if (owner instanceof Catalog) return MdClassFactory.eINSTANCE.createCatalogTabularSection();
            if (owner instanceof Document) return MdClassFactory.eINSTANCE.createDocumentTabularSection();
            if (owner instanceof Report) return MdClassFactory.eINSTANCE.createReportTabularSection();
            if (owner instanceof DataProcessor) return MdClassFactory.eINSTANCE.createDataProcessorTabularSection();
        }
        if (kind == FeatureKind.TABULAR_SECTION_ATTRIBUTE)
        {
            var className = owner.eClass().getName();
            if ("CatalogTabularSection".equals(className)) return MdClassFactory.eINSTANCE.createTabularSectionAttribute(); //$NON-NLS-1$
            if ("DocumentTabularSection".equals(className)) return MdClassFactory.eINSTANCE.createTabularSectionAttribute(); //$NON-NLS-1$
            if ("ReportTabularSection".equals(className)) return MdClassFactory.eINSTANCE.createReportTabularSectionAttribute(); //$NON-NLS-1$
            if ("DataProcessorTabularSection".equals(className)) return MdClassFactory.eINSTANCE.createDataProcessorTabularSectionAttribute(); //$NON-NLS-1$
        }
        if (kind == FeatureKind.REGISTER_FIELD)
        {
            return createRegisterChild(owner, featureName);
        }
        throw new ToolException("Operation is not supported for object type: " + owner.eClass().getName()); //$NON-NLS-1$
    }

    private MdObject createRegisterChild(MdObject owner, String feature)
    {
        if (owner instanceof InformationRegister)
        {
            if ("dimensions".equals(feature)) return MdClassFactory.eINSTANCE.createInformationRegisterDimension(); //$NON-NLS-1$
            if ("resources".equals(feature)) return MdClassFactory.eINSTANCE.createInformationRegisterResource(); //$NON-NLS-1$
            return MdClassFactory.eINSTANCE.createInformationRegisterAttribute();
        }
        if (owner instanceof com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister)
        {
            if ("dimensions".equals(feature)) return MdClassFactory.eINSTANCE.createAccumulationRegisterDimension(); //$NON-NLS-1$
            if ("resources".equals(feature)) return MdClassFactory.eINSTANCE.createAccumulationRegisterResource(); //$NON-NLS-1$
            return MdClassFactory.eINSTANCE.createAccumulationRegisterAttribute();
        }
        if (owner instanceof AccountingRegister)
        {
            if ("dimensions".equals(feature)) return MdClassFactory.eINSTANCE.createAccountingRegisterDimension(); //$NON-NLS-1$
            if ("resources".equals(feature)) return MdClassFactory.eINSTANCE.createAccountingRegisterResource(); //$NON-NLS-1$
            return MdClassFactory.eINSTANCE.createAccountingRegisterAttribute();
        }
        if (owner instanceof CalculationRegister)
        {
            if ("dimensions".equals(feature)) return MdClassFactory.eINSTANCE.createCalculationRegisterDimension(); //$NON-NLS-1$
            if ("resources".equals(feature)) return MdClassFactory.eINSTANCE.createCalculationRegisterResource(); //$NON-NLS-1$
            return MdClassFactory.eINSTANCE.createCalculationRegisterAttribute();
        }
        throw new ToolException("Object is not a supported register: " + owner.getName()); //$NON-NLS-1$
    }

    private static String registerFeature(String kind)
    {
        if (kind == null) throw new ToolException("Parameter `field_kind` is required."); //$NON-NLS-1$
        switch (kind.toLowerCase(java.util.Locale.ROOT))
        {
        case "dimension": return "dimensions"; //$NON-NLS-1$ //$NON-NLS-2$
        case "resource": return "resources"; //$NON-NLS-1$ //$NON-NLS-2$
        case "attribute": return "attributes"; //$NON-NLS-1$ //$NON-NLS-2$
        default: throw new ToolException("Invalid `field_kind`. Valid values: dimension, resource, attribute."); //$NON-NLS-1$
        }
    }

    private static boolean setScalarProperty(EObject object, String propertyName, String propertyValue,
        boolean dryRun)
    {
        var feature = object.eClass().getEStructuralFeature(propertyName);
        if (feature == null || feature.isMany() || !(feature.getEType() instanceof EDataType))
        {
            throw new ToolException("Unsupported scalar property `" + propertyName + "` for " //$NON-NLS-1$ //$NON-NLS-2$
                + object.eClass().getName() + "."); //$NON-NLS-1$
        }
        var value = EcoreUtil.createFromString((EDataType)feature.getEType(), propertyValue);
        boolean changed = !java.util.Objects.equals(object.eGet(feature), value);
        if (changed && !dryRun)
        {
            object.eSet(feature, value);
        }
        return changed;
    }

    private static MdObject requireChild(IBmTransaction transaction, MetadataRequest request)
    {
        var child = findNamed(childLocation(transaction, request).children, request.name);
        if (child == null)
        {
            throw new ToolException("Child metadata object not found: " + childTarget(request)); //$NON-NLS-1$
        }
        return child;
    }

    private static ChildLocation childLocation(IBmTransaction transaction, MetadataRequest request)
    {
        if (request.childKind == null)
        {
            throw new ToolException("Parameter `child_kind` is required."); //$NON-NLS-1$
        }
        String ownerFqn = request.objectName;
        String collection;
        String section = null;
        switch (request.childKind.toLowerCase(Locale.ROOT))
        {
        case "object_attribute": collection = "attributes"; break; //$NON-NLS-1$ //$NON-NLS-2$
        case "tabular_section": collection = "tabularSections"; break; //$NON-NLS-1$ //$NON-NLS-2$
        case "tabular_section_attribute": //$NON-NLS-1$
            var parts = tabularSectionParts(request.objectName);
            ownerFqn = parts[0];
            section = parts[1];
            collection = "attributes"; //$NON-NLS-1$
            break;
        case "enum_value": collection = "enumValues"; break; //$NON-NLS-1$ //$NON-NLS-2$
        case "dimension": collection = "dimensions"; break; //$NON-NLS-1$ //$NON-NLS-2$
        case "resource": collection = "resources"; break; //$NON-NLS-1$ //$NON-NLS-2$
        case "register_attribute": collection = "attributes"; break; //$NON-NLS-1$ //$NON-NLS-2$
        case "form": collection = "forms"; break; //$NON-NLS-1$ //$NON-NLS-2$
        case "template": collection = "templates"; break; //$NON-NLS-1$ //$NON-NLS-2$
        default:
            throw new ToolException("Invalid `child_kind`. Valid values: object_attribute, tabular_section, " //$NON-NLS-1$
                + "tabular_section_attribute, enum_value, dimension, resource, register_attribute, form, template."); //$NON-NLS-1$
        }
        MdObject owner = requireObject(transaction, ownerFqn);
        if (section != null)
        {
            owner = requireNamedChild(owner, "tabularSections", section); //$NON-NLS-1$
        }
        return new ChildLocation(featureList(owner, collection));
    }

    private static String childTarget(MetadataRequest request)
    {
        return request.objectName + "." + request.name; //$NON-NLS-1$
    }

    private static String artifactPath(IProject project, MetadataRequest request)
    {
        if (request.dryRun || request.name == null)
        {
            return null;
        }
        String base = project.getLocation().append(metadataOwnerFolder(request.objectName)).toOSString();
        if ("createObjectForm".equals(request.operation) || "removeObjectForm".equals(request.operation)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return "removeObjectForm".equals(request.operation) //$NON-NLS-1$
                ? Paths.get(base, "Forms", request.name).toString() //$NON-NLS-1$
                : Paths.get(base, "Forms", request.name, "Form.form").toString(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ("createObjectTemplate".equals(request.operation)) //$NON-NLS-1$
        {
            String file = "DATA_COMPOSITION_SCHEMA".equalsIgnoreCase(request.templateType) //$NON-NLS-1$
                ? "Template.dcs" : "Template.mxlx"; //$NON-NLS-1$ //$NON-NLS-2$
            return Paths.get(base, "Templates", request.name, file).toString(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ("removeObjectTemplate".equals(request.operation)) //$NON-NLS-1$
        {
            return Paths.get(base, "Templates", request.name).toString(); //$NON-NLS-1$
        }
        return null;
    }

    private static String metadataOwnerFolder(String target)
    {
        var parts = target.split("\\.", -1); //$NON-NLS-1$
        if (parts.length < 2)
        {
            throw new ToolException("Object FQN must contain type and name: " + target); //$NON-NLS-1$
        }
        return "src/" + topFolder(parts[0]) + "/" + parts[1]; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @SuppressWarnings("unchecked")
    private static EList<MdObject> featureList(MdObject owner, String name)
    {
        var feature = owner.eClass().getEStructuralFeature(name);
        if (feature == null || !feature.isMany())
        {
            throw new ToolException("Object type `" + owner.eClass().getName() + "` has no collection `" + name + "`."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        return (EList<MdObject>)owner.eGet(feature);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void addToFeature(EObject owner, String name, EObject value)
    {
        var feature = owner.eClass().getEStructuralFeature(name);
        if (feature == null || !feature.isMany())
        {
            throw new ToolException("EDT model collection is not available: " + name); //$NON-NLS-1$
        }
        ((EList)owner.eGet(feature)).add(value);
    }

    private static MdObject findNamed(EList<MdObject> list, String name)
    {
        for (var item : list)
        {
            if (name.equalsIgnoreCase(item.getName())) return item;
        }
        return null;
    }

    private static MdObject requireNamedChild(MdObject owner, String feature, String name)
    {
        var result = findNamed(featureList(owner, feature), name);
        if (result == null) throw new ToolException("Child metadata object not found: " + name); //$NON-NLS-1$
        return result;
    }

    private MdObject readObject(IProject project, String fqn)
    {
        var result = readObjectOrNull(project, fqn);
        if (result == null) throw new ToolException("Metadata object not found: " + fqn); //$NON-NLS-1$
        return result;
    }

    private MdObject readObjectOrNull(IProject project, String fqn)
    {
        return model(project).getGlobalContext().execute(new AbstractBmTask<MdObject>("Read 1C metadata object") //$NON-NLS-1$
        {
            @Override
            public MdObject execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var value = transaction.getTopObjectByFqn(fqn);
                return value instanceof MdObject ? (MdObject)value : null;
            }
        });
    }

    private static MdObject requireObject(IBmTransaction transaction, String fqn)
    {
        var object = transaction.getTopObjectByFqn(fqn);
        if (!(object instanceof MdObject)) throw new ToolException("Metadata object not found: " + fqn); //$NON-NLS-1$
        return (MdObject)object;
    }

    private com._1c.g5.v8.bm.integration.IBmModel model(IProject project)
    {
        var model = modelManager.getModel(project);
        if (model == null) throw new ToolException("BM model is not available for project: " + project.getName()); //$NON-NLS-1$
        return model;
    }

    private static IProject project(String name)
    {
        var project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
        if (project == null || !project.exists() || !project.isAccessible())
        {
            throw new ToolException("Project not found or not accessible: " + name); //$NON-NLS-1$
        }
        return project;
    }

    private static String[] objectParts(String fqn)
    {
        if (fqn == null) throw new ToolException("Parameter `object_name` is required."); //$NON-NLS-1$
        var parts = fqn.split("\\.", -1); //$NON-NLS-1$
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank())
        {
            throw new ToolException("`object_name` must be a top-level FQN such as `Catalog.Products`."); //$NON-NLS-1$
        }
        return parts;
    }

    private static String[] tabularSectionParts(String objectName)
    {
        var parts = objectName != null ? objectName.split("\\.", -1) : new String[0]; //$NON-NLS-1$
        if (parts.length != 3)
        {
            throw new ToolException("For a tabular-section attribute, `object_name` must be `Type.Object.TabularSection`."); //$NON-NLS-1$
        }
        return new String[] { parts[0] + "." + parts[1], parts[2] }; //$NON-NLS-1$
    }

    private static void validateIdentifier(String value, String parameter)
    {
        if (value == null || !value.matches(IDENTIFIER_PATTERN))
        {
            throw new ToolException("Parameter `" + parameter + "` is not a valid 1C identifier: " + value); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static void checkCanceled(ICancellationToken token)
    {
        if (token.isCanceled()) throw new ToolException("Operation was cancelled."); //$NON-NLS-1$
    }

    private static void refresh(IProject project)
    {
        try
        {
            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        }
        catch (org.eclipse.core.runtime.CoreException e)
        {
            throw new ToolException("Cannot refresh project after refactoring.", e, ToolErrorType.RETRYABLE); //$NON-NLS-1$
        }
    }

    private enum FeatureKind
    {
        ATTRIBUTE,
        TABULAR_SECTION,
        TABULAR_SECTION_ATTRIBUTE,
        ENUM_VALUE,
        REGISTER_FIELD
    }

    private static final class ChildLocation
    {
        final EList<MdObject> children;

        ChildLocation(EList<MdObject> children)
        {
            this.children = children;
        }
    }
}
