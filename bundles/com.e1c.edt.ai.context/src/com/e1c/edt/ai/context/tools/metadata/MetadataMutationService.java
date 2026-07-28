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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.bsl.model.BslPackage;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
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
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.md.model.IMdObjectInitializer;
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
import com.e1c.edt.ai.assistent.model.MarkerInfo;
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
    private static final String EXTERNAL_OBJECTS_NATURE = "com._1c.g5.v8.dt.core.V8ExternalObjectsNature"; //$NON-NLS-1$

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
    private final IProjectFileSystemSupportProvider fileSystemSupportProvider;
    private final java.util.Set<com.e1c.edt.ai.IMarkersProvider> markersProviders;

    @Inject
    MetadataMutationService(IBmModelManager modelManager, ITopObjectFqnGenerator fqnGenerator,
        IEditingSupport editingSupport, IMdRefactoringService refactoringService, MetadataTypeService typeService,
        IProjectBuilder projectBuilder, IDerivedDataManagerProvider derivedDataManagerProvider, ISettings settings,
        IV8ProjectManager v8ProjectManager, IFormGenerator formGenerator, IFormFieldGenerator formFieldGenerator,
        IEditingLanguageManager editingLanguageManager, IProjectFileSystemSupportProvider fileSystemSupportProvider,
        java.util.Set<com.e1c.edt.ai.IMarkersProvider> markersProviders)
    {
        Preconditions.checkNotNull(markersProviders);
        this.markersProviders = markersProviders;
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
        Preconditions.checkNotNull(fileSystemSupportProvider);
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
        this.fileSystemSupportProvider = fileSystemSupportProvider;
    }

    synchronized MetadataResponse execute(MetadataRequest request, ICancellationToken cancellationToken)
    {
        checkCanceled(cancellationToken);
        // Configuration-level lifecycle operates on the workspace project itself (create/delete), so it
        // runs before the "project must already exist" resolution used by object-level operations.
        if ("createConfiguration".equals(request.operation)) //$NON-NLS-1$
        {
            return createConfiguration(request, cancellationToken);
        }
        if ("removeConfiguration".equals(request.operation)) //$NON-NLS-1$
        {
            return removeConfiguration(request);
        }
        var project = project(request.projectName);
        if ("inspectObject".equals(request.operation)) //$NON-NLS-1$
        {
            return inspectObject(project, request);
        }
        if ("listModules".equals(request.operation)) //$NON-NLS-1$
        {
            return listModules(project, request);
        }
        if (editingSupport.isReadOnly(project))
        {
            // The user must switch the configuration off vendor support first: retrying cannot help.
            throw new ToolException("Configuration is on full vendor support, so it must not be modified: " //$NON-NLS-1$
                + project.getName() + ". Ask the user to enable editing before changing anything.", //$NON-NLS-1$
                ToolErrorType.USER_VISIBLE);
        }
        // Per-object vendor-support rule. isReadOnly above only covers a configuration on *full*
        // support; with "editable with preservation" the project is writable while individual adopted
        // objects still must not change, and only this check catches that.
        requireEditable(project, request);

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
        case "addObjectReference": //$NON-NLS-1$
            response = changeObjectReference(project, request, true);
            break;
        case "removeObjectReference": //$NON-NLS-1$
            response = changeObjectReference(project, request, false);
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
        case "addSubordinateObject": //$NON-NLS-1$
            response = addSubordinateObject(project, request);
            break;
        case "removeSubordinateObject": //$NON-NLS-1$
            response = removeSubordinateObject(project, request);
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
            if (request.verifyEnabled())
            {
                collectMarkers(project, response, cancellationToken, validationComplete, persistenceTimeoutMillis);
                // Deletions and renames can invalidate references held by other objects and modules,
                // which a check scoped to the changed resource cannot see.
                if (request.operation.startsWith("remove") || request.operation.startsWith("rename")) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    response.warnings.add("This operation can break references in other objects." //$NON-NLS-1$
                        + " Run a project-wide GetMarkers check (marker_type=1c) for project_name=" //$NON-NLS-1$
                        + project.getName() + "."); //$NON-NLS-1$
                }
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
        // renameObject belongs here too: EDT writes the renamed resource asynchronously, so without a
        // wait the check below fired on a rename that had in fact succeeded. That false failure also made
        // the model abandon the rest of the user's request.
        if (!"createObject".equals(request.operation) && !"removeObject".equals(request.operation) //$NON-NLS-1$ //$NON-NLS-2$
            && !"renameObject".equals(request.operation)) //$NON-NLS-1$
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

    /** Type-description settings that {@code setChildProperty} must redirect to {@code setChildType}. */
    private static final java.util.Set<String> TYPE_DESCRIPTION_PROPERTIES =
        java.util.Set.of("type", "length", "precision", "datefractions", "date_fractions", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "stringqualifiers", "numberqualifiers", "datequalifiers"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /** Operations that remove metadata, and therefore need the delete permission, not the edit one. */
    private static final java.util.Set<String> DELETING_OPERATIONS = java.util.Set.of("removeObject"); //$NON-NLS-1$

    /**
     * Refuses the operation when vendor-support rules forbid changing the target object.
     * <p>
     * The question is delegated to EDT ({@code IModelEditingSupport}), the same authority the editors
     * use, so support rules are never reinterpreted here. Objects that cannot be resolved yet (a
     * {@code createObject} target, for instance) are permitted: the configuration root is checked
     * instead, since adding a new own object to a supported configuration is legitimate.
     */
    private void requireEditable(IProject project, MetadataRequest request)
    {
        var fqn = request.objectName;
        boolean deleting = DELETING_OPERATIONS.contains(request.operation);
        var target = "createObject".equals(request.operation) ? "Configuration" //$NON-NLS-1$ //$NON-NLS-2$
            : topObjectFqn(fqn);
        if (target == null)
        {
            return;
        }
        var verdict = model(project).getGlobalContext().execute(new AbstractBmTask<Boolean>("Check 1C support rules") //$NON-NLS-1$
        {
            @Override
            public Boolean execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var object = transaction.getTopObjectByFqn(normalizeConfigurationFqn(target));
                if (object == null)
                {
                    return Boolean.TRUE;
                }
                return Boolean.valueOf(deleting ? editingSupport.canDelete(object) : editingSupport.canEdit(object));
            }
        });
        if (Boolean.FALSE.equals(verdict))
        {
            throw new ToolException("Object `" + target //$NON-NLS-1$
                + "` is on vendor support and its support rule forbids " //$NON-NLS-1$
                + (deleting ? "deletion" : "modification") //$NON-NLS-1$ //$NON-NLS-2$
                + ". Do not retry: only the user can change the support rule in EDT.", //$NON-NLS-1$
                ToolErrorType.USER_VISIBLE);
        }
    }

    /** The {@code Type.Name} part of an FQN, or {@code null} when there is nothing to check. */
    private static String topObjectFqn(String fqn)
    {
        if (fqn == null || fqn.isBlank())
        {
            return null;
        }
        var parts = fqn.split("\\.", -1); //$NON-NLS-1$
        return parts.length >= 2 ? parts[0] + "." + parts[1] : fqn; //$NON-NLS-1$
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
                var object = requireObject(transaction, request.objectName);
                var described = describeObject(object);
                described.put("vendor_support", describeSupport(project, object)); //$NON-NLS-1$
                return described;
            }
        });
        return response;
    }

    /**
     * Vendor-support state of an object, so the model learns about the restriction before attempting a
     * change instead of from a refusal. {@code editable}/{@code deletable} are the same verdicts the
     * mutations enforce, and {@code object_belonging} tells adopted objects from own ones.
     */
    private Map<String, Object> describeSupport(IProject project, MdObject object)
    {
        var result = new LinkedHashMap<String, Object>();
        var feature = object.eClass().getEStructuralFeature("objectBelonging"); //$NON-NLS-1$
        if (feature != null && !feature.isMany())
        {
            var value = object.eGet(feature);
            if (value != null)
            {
                result.put("object_belonging", String.valueOf(value)); //$NON-NLS-1$
            }
        }
        boolean projectReadOnly = editingSupport.isReadOnly(project);
        result.put("configuration_on_full_support", Boolean.valueOf(projectReadOnly)); //$NON-NLS-1$
        result.put("editable", Boolean.valueOf(!projectReadOnly && editingSupport.canEdit(object))); //$NON-NLS-1$
        result.put("deletable", Boolean.valueOf(!projectReadOnly && editingSupport.canDelete(object))); //$NON-NLS-1$
        if (Boolean.FALSE.equals(result.get("editable"))) //$NON-NLS-1$
        {
            result.put("note", //$NON-NLS-1$
                "This object must not be changed; only the user can lift the vendor-support restriction."); //$NON-NLS-1$
        }
        return result;
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
        // Length, precision and the like live in the type description, not among the child's own
        // properties. Without this hint the model hit "unsupported property" and reached for a direct
        // .mdo edit instead of the operation that exists.
        if (TYPE_DESCRIPTION_PROPERTIES.contains(request.propertyName.toLowerCase(Locale.ROOT)))
        {
            throw new ToolException("`" + request.propertyName //$NON-NLS-1$
                + "` belongs to the type of a child, not to the child itself: use `setChildType` with type," //$NON-NLS-1$
                + " length, precision or date_fractions. For example type=String and length=50."); //$NON-NLS-1$
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
        // True for a child whose body is an external resource (form, template): renaming it needs the
        // refactoring service, not a plain setName.
        boolean[] external = { false };
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
                external[0] = child instanceof BasicForm || child instanceof Template;
                if (changed[0] && !request.dryRun && !external[0])
                {
                    child.setName(request.newName);
                }
                return null;
            }
        });
        // A form or a template keeps its body in a folder named after the child, so renaming it in the
        // model alone leaves the body behind and EDT then looks for a file that does not exist: the
        // artifact silently disappears from the configuration without producing a single marker.
        // Renaming through the EDT refactoring service is what moves the resources as well.
        if (changed[0] && !request.dryRun && external[0])
        {
            renameExternalChild(project, request);
        }
        return MetadataResponse.success(request, request.objectName + "." + request.newName, changed[0]); //$NON-NLS-1$
    }

    /**
     * Renames a child that owns an external body (form, template) through the EDT refactoring service,
     * then verifies the body really moved. Fails loudly rather than leaving a dangling artifact.
     */
    private void renameExternalChild(IProject project, MetadataRequest request)
    {
        var oldBody = externalBodyFolder(project, request, request.name);
        var child = model(project).getGlobalContext().execute(new AbstractBmTask<MdObject>("Read 1C child to rename") //$NON-NLS-1$
        {
            @Override
            public MdObject execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                return findNamed(childLocation(transaction, request).children, request.name);
            }
        });
        if (child == null)
        {
            throw new ToolException("Child metadata object not found: " + childTarget(request)); //$NON-NLS-1$
        }
        for (var refactoring : refactoringService.createMdObjectRenameRefactoring(child, request.newName))
        {
            refactoring.perform();
        }
        refresh(project);
        var newBody = externalBodyFolder(project, request, request.newName);
        if (newBody == null || oldBody == null)
        {
            return;
        }
        // The refactoring moves the resources asynchronously, so poll instead of checking once: a single
        // immediate check reported a broken artifact for a rename that had in fact completed.
        var target = Paths.get(newBody);
        long deadline = System.currentTimeMillis() + persistenceTimeoutMillis();
        while (!Files.exists(target) && System.currentTimeMillis() < deadline)
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
            refresh(project);
        }
        if (!Files.exists(target))
        {
            throw new ToolException("Renaming `" + request.name + "` to `" + request.newName //$NON-NLS-1$ //$NON-NLS-2$
                + "` did not move its body folder, so the artifact would be broken. Remove the child and create it" //$NON-NLS-1$
                + " again under the wanted name instead.", ToolErrorType.USER_VISIBLE); //$NON-NLS-1$
        }
    }

    /** Absolute path of a form/template body folder for the given child name, or {@code null}. */
    private static String externalBodyFolder(IProject project, MetadataRequest request, String childName)
    {
        String collection = "template".equalsIgnoreCase(request.childKind) ? "Templates" : "Forms"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        var location = project.getLocation();
        if (location == null)
        {
            return null;
        }
        return Paths.get(location.toOSString(), metadataOwnerFolder(request.objectName), collection, childName)
            .toString();
    }

    private static boolean isSingleReference(org.eclipse.emf.ecore.EStructuralFeature feature)
    {
        return feature instanceof EReference && !feature.isMany() && feature.isChangeable()
            && feature.getEType() instanceof EClass;
    }

    /**
     * Adds or removes an object in a reference collection, for example the documents registered by a
     * DocumentJournal. Such collections are mandatory for some object types, and no scalar operation can
     * fill them.
     */
    private MetadataResponse changeObjectReference(IProject project, MetadataRequest request, boolean add)
    {
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Change 1C reference collection") //$NON-NLS-1$
        {
            @Override
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var object = requireObject(transaction, request.objectName);
                var feature = object.eClass().getEStructuralFeature(request.propertyName);
                if (!(feature instanceof EReference) || !feature.isMany())
                {
                    throw new ToolException("Property `" + request.propertyName + "` of " + request.objectName //$NON-NLS-1$ //$NON-NLS-2$
                        + " is not a reference collection." + propertySuggestion(object, request.propertyName)); //$NON-NLS-1$
                }
                var target = requireObject(transaction, request.relatedObjectName);
                var list = (EList)object.eGet(feature);
                boolean contains = list.contains(target);
                changed[0] = add ? !contains : contains;
                if (changed[0] && !request.dryRun)
                {
                    if (add)
                    {
                        list.add(target);
                    }
                    else
                    {
                        list.remove(target);
                    }
                }
                return null;
            }
        });
        return MetadataResponse.success(request, request.objectName, changed[0]);
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

    private void registerExternalObject(IProject project, MdObject object)
    {
        var v8Project = v8ProjectManager.getProject(project);
        if (v8Project == null)
        {
            throw new ToolException("V8 project is not available: " + project.getName()); //$NON-NLS-1$
        }
        for (var method : v8Project.getClass().getMethods())
        {
            if ("addExternalObject".equals(method.getName()) && method.getParameterCount() == 1) //$NON-NLS-1$
            {
                try
                {
                    method.setAccessible(true);
                    method.invoke(v8Project, object);
                    return;
                }
                catch (ReflectiveOperationException e)
                {
                    throw new ToolException("Cannot register external object in EDT project: " + e.getMessage()); //$NON-NLS-1$
                }
            }
        }
        throw new ToolException("EDT project does not expose external object registration."); //$NON-NLS-1$
    }

    private static boolean hasNature(IProject project, String nature)
    {
        try
        {
            return project.hasNature(nature);
        }
        catch (org.eclipse.core.runtime.CoreException e)
        {
            throw new ToolException("Cannot inspect EDT project nature: " + e.getMessage()); //$NON-NLS-1$
        }
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
            throw new ToolException("Invalid `form_type`. Valid values: " + formTypeNames() + "."); //$NON-NLS-1$ //$NON-NLS-2$
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

    /** Comma-separated list of every generated form type EDT supports. */
    static String formTypeNames()
    {
        var names = new java.util.ArrayList<String>();
        for (var value : FormType.values())
        {
            names.add(value.name());
        }
        return String.join(", ", names); //$NON-NLS-1$
    }

    /**
     * Candidate "default form" features per form type, most specific first. A type may have no slot at
     * all (GENERIC), and a slot may live on the configuration rather than on the owner, so candidates
     * are only probed: the first feature that exists on this owner and is still unset gets the form.
     */
    private static final Map<FormType, List<String>> DEFAULT_FORM_FEATURES = createDefaultFormFeatures();

    @SuppressWarnings("nls")
    private static Map<FormType, List<String>> createDefaultFormFeatures()
    {
        Map<FormType, List<String>> result = new LinkedHashMap<>();
        result.put(FormType.OBJECT, List.of("defaultObjectForm"));
        result.put(FormType.FOLDER, List.of("defaultFolderForm"));
        result.put(FormType.LIST, List.of("defaultListForm"));
        result.put(FormType.CHOICE, List.of("defaultChoiceForm"));
        result.put(FormType.FOLDER_CHOICE, List.of("defaultFolderChoiceForm"));
        result.put(FormType.RECORD, List.of("defaultRecordForm"));
        result.put(FormType.RECORD_SET, List.of("defaultRecordForm", "defaultListForm"));
        result.put(FormType.CONSTANTS, List.of("defaultConstantsForm", "defaultForm"));
        result.put(FormType.SEARCH, List.of("defaultSearchForm"));
        result.put(FormType.REPORT, List.of("defaultForm", "defaultReportForm"));
        result.put(FormType.REPORT_SETTINGS, List.of("defaultSettingsForm", "defaultReportSettingsForm"));
        result.put(FormType.REPORT_VARIANT, List.of("defaultVariantForm", "defaultReportVariantForm"));
        result.put(FormType.SAVE, List.of("defaultSaveForm"));
        result.put(FormType.LOAD, List.of("defaultLoadForm"));
        result.put(FormType.DYNAMIC_LIST, List.of("defaultDynamicListSettingsForm"));
        result.put(FormType.CHANGE_HISTORY, List.of("defaultDataHistoryChangeHistoryForm"));
        result.put(FormType.VERSION_DATA, List.of("defaultDataHistoryVersionDataForm"));
        result.put(FormType.VERSION_DIFFERENCES, List.of("defaultDataHistoryVersionDifferencesForm"));
        // GENERIC is an arbitrary form: it is never anybody's default.
        return result;
    }

    private static void setDefaultForm(MdObject owner, BasicForm form, FormType formType)
    {
        for (var featureName : DEFAULT_FORM_FEATURES.getOrDefault(formType, List.of()))
        {
            var feature = owner.eClass().getEStructuralFeature(featureName);
            if (feature != null && !feature.isMany() && owner.eGet(feature) == null)
            {
                owner.eSet(feature, form);
                return;
            }
        }
    }

    private MetadataResponse createObjectTemplate(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        var templateType = templateTypeOf(request.templateType);
        if (TEMPLATE_TYPES_NEEDING_CONTENT.contains(templateType))
        {
            // Verified empirically: these bodies wrap external content (text file, binary blob, add-in
            // archive, embedded document), and EDT writes no file for an empty one. Creating the
            // metadata anyway leaves a template registered in the .mdo with no body, so refuse up front
            // instead of producing broken metadata.
            throw new ToolException("`template_type` " + templateType.name() //$NON-NLS-1$
                + " cannot be created empty: its body wraps external content that this operation cannot" //$NON-NLS-1$
                + " invent. Import such a template through the EDT UI, or use one of: " //$NON-NLS-1$
                + supportedTemplateTypeNames() + "."); //$NON-NLS-1$
        }
        if (!TEMPLATE_BODY_FACTORIES.containsKey(templateType))
        {
            throw new ToolException("`template_type` " + templateType.name() //$NON-NLS-1$
                + " cannot be created empty: this platform has no model for its body. Supported: " //$NON-NLS-1$
                + supportedTemplateTypeNames() + "."); //$NON-NLS-1$
        }
        boolean[] changed = { false };
        // The body file extension is derived by EDT from the body object type, so the authoritative
        // path is captured here instead of being guessed from the template type.
        String[] bodyPath = { null };
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
                final EObject body;
                try
                {
                    body = TEMPLATE_BODY_FACTORIES.get(templateType).get();
                }
                catch (LinkageError e)
                {
                    // The body model bundles are optional dependencies: if one is absent from this EDT
                    // installation, fail this template type only instead of breaking the whole plugin.
                    throw new ToolException("This EDT installation has no model bundle for template_type " //$NON-NLS-1$
                        + templateType.name() + ", so it cannot be created. Supported here: " //$NON-NLS-1$
                        + supportedTemplateTypeNames() + "."); //$NON-NLS-1$
                }
                template.setTemplate(body);
                var contentFqn = fqnGenerator.generateExternalPropertyFqn(template,
                    MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
                transaction.attachTopObject((IBmObject)body, contentFqn);
                var file = fileSystemSupportProvider.getProjectFileSystemSupport(project)
                    .getFile(template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
                if (file != null)
                {
                    var location = file.getLocation();
                    bodyPath[0] = location != null ? location.toOSString() : file.getFullPath().toString();
                }
                return null;
            }
        });
        var response = MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
        if (bodyPath[0] != null)
        {
            var details = new LinkedHashMap<String, Object>();
            details.put("template_type", templateType.name()); //$NON-NLS-1$
            details.put("body_path", bodyPath[0]); //$NON-NLS-1$
            response.details = details;
        }
        return response;
    }

    /**
     * Resolves {@code template_type}. {@link TemplateType} is an EMF enum, so its Java constant
     * ({@code SPREADSHEET_DOCUMENT}) differs from the name serialized in {@code .mdo}
     * ({@code SpreadsheetDocument}); both spellings are accepted because the model legitimately sees
     * the latter when reading metadata files.
     */
    private static TemplateType templateTypeOf(String value)
    {
        if (value != null && !value.isBlank())
        {
            var normalized = value.trim();
            for (var candidate : TemplateType.values())
            {
                if (candidate.name().equalsIgnoreCase(normalized)
                    || candidate.getName().equalsIgnoreCase(normalized)
                    || candidate.getLiteral().equalsIgnoreCase(normalized))
                {
                    return candidate;
                }
            }
        }
        throw new ToolException("Invalid `template_type` `" + value + "`. Valid values: " //$NON-NLS-1$ //$NON-NLS-2$
            + templateTypeNames() + "."); //$NON-NLS-1$
    }

    /** Comma-separated list of every template type EDT declares, as accepted by {@code template_type}. */
    static String templateTypeNames()
    {
        var names = new java.util.ArrayList<String>();
        for (var value : TemplateType.values())
        {
            names.add(value.name());
        }
        return String.join(", ", names); //$NON-NLS-1$
    }

    static String supportedTemplateTypeNames()
    {
        var names = new java.util.ArrayList<String>();
        for (var type : TEMPLATE_BODY_FACTORIES.keySet())
        {
            names.add(type.name());
        }
        return String.join(", ", names); //$NON-NLS-1$
    }

    /**
     * Body factories per template type. The body is an {@code EObject} attached as its own BM top
     * object; EDT derives the body file name (Template.mxlx, Template.txt, ...) from the body's type,
     * so no extension is hardcoded here. GRAPHICAL_SCHEMA and GEOGRAPHICAL_SCHEMA are absent on
     * purpose: this platform ships no model for them, so an empty one cannot be constructed.
     */
    private static final Map<TemplateType, java.util.function.Supplier<EObject>> TEMPLATE_BODY_FACTORIES =
        createTemplateBodyFactories();

    private static Map<TemplateType, java.util.function.Supplier<EObject>> createTemplateBodyFactories()
    {
        Map<TemplateType, java.util.function.Supplier<EObject>> result = new LinkedHashMap<>();
        result.put(TemplateType.SPREADSHEET_DOCUMENT, SheetFactory::createSpreadsheetDocument);
        result.put(TemplateType.DATA_COMPOSITION_SCHEMA, () -> DcsFactory.eINSTANCE.createDataCompositionSchema());
        result.put(TemplateType.DATA_COMPOSITION_APPEARANCE_TEMPLATE,
            () -> com._1c.g5.v8.dt.dcs.model.appearancetemplate.DcsFactory.eINSTANCE
                .createDataCompositionAppearanceTemplate());
        result.put(TemplateType.HTML_DOCUMENT,
            () -> com._1c.g5.v8.dt.htmldocument.model.HtmlDocumentFactory.eINSTANCE.createHtmlDocument());
        return result;
    }

    /**
     * Types whose body is a wrapper around external content. Their EMF model exists, but EDT persists
     * no file for an empty instance (verified on a real project: the template ended up registered in the
     * .mdo without any body file), so they are refused rather than created broken.
     */
    private static final java.util.Set<TemplateType> TEMPLATE_TYPES_NEEDING_CONTENT =
        java.util.Set.of(TemplateType.TEXT_DOCUMENT, TemplateType.BINARY_DATA, TemplateType.ADD_IN,
            TemplateType.ACTIVE_DOCUMENT);

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
                var descriptor = MetadataObjectTypeRegistry.get(objectParts(request.objectName)[0]);
                boolean externalProject = hasNature(project, EXTERNAL_OBJECTS_NATURE);
                if (descriptor.external != externalProject)
                {
                    throw new ToolException(descriptor.external
                        ? "ExternalDataProcessor and ExternalReport require an EDT external-objects project." //$NON-NLS-1$
                        : "Configuration metadata objects cannot be created in an EDT external-objects project."); //$NON-NLS-1$
                }
                var object = createTopObject(project, request.objectName);
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
                if (descriptor.external)
                {
                    transaction.attachTopObject((IBmObject)object, request.objectName);
                    registerExternalObject(project, object);
                }
                else
                {
                    var configuration = (Configuration)transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                    if (configuration == null)
                    {
                        throw new ToolException("Configuration top object is not available."); //$NON-NLS-1$
                    }
                    var fqn = fqnGenerator.generateStandaloneObjectFqn(object.eClass(), object.getName()).toString();
                    transaction.attachTopObject((IBmObject)object, fqn);
                    addToFeature(configuration, descriptor.collection, object);
                    if ("CommonForm".equals(descriptor.name)) //$NON-NLS-1$
                    {
                        attachFormBody(project, transaction, (BasicForm)object);
                    }
                }
                changed[0] = true;
                return null;
            }
        });
        if (changed[0] && !request.dryRun)
        {
            if (MetadataObjectTypeRegistry.get(objectParts(request.objectName)[0]).inlineInConfiguration)
            {
                requireInlineObject(project, request.objectName);
            }
            else
            {
                readObject(project, request.objectName);
            }
        }
        return MetadataResponse.success(request, request.objectName, changed[0]);
    }

    private static final String DEFAULT_PLATFORM_VERSION = "8.3.24"; //$NON-NLS-1$
    private static final String XTEXT_NATURE = "org.eclipse.xtext.ui.shared.xtextNature"; //$NON-NLS-1$
    private static final String CONFIGURATION_NATURE = "com._1c.g5.v8.dt.core.V8ConfigurationNature"; //$NON-NLS-1$

    // Fixed system class ids for the seven mandatory containedObjects of an empty configuration,
    // matching what EDT's ConfigurationInitializer produces; only their object ids are per-configuration.
    private static final String[] CONFIGURATION_SYSTEM_CLASS_IDS = {
        "9cd510cd-abfc-11d4-9434-004095e12fc7", "9fcd25a0-4822-11d4-9414-008048da11f9", //$NON-NLS-1$ //$NON-NLS-2$
        "e3687481-0a87-462c-a166-9f34594f9bba", "9de14907-ec23-4a07-96f0-85521cb6b53b", //$NON-NLS-1$ //$NON-NLS-2$
        "51f2d5d8-ea4d-4064-8892-82951750031e", "e68182ea-4237-4383-967f-90c1e3370bc7", //$NON-NLS-1$ //$NON-NLS-2$
        "fb282519-d103-4dd3-bc12-cb271d631dfc" }; //$NON-NLS-1$

    private MetadataResponse createConfiguration(MetadataRequest request, ICancellationToken cancellationToken)
    {
        var name = request.projectName;
        if (name == null || name.isBlank())
        {
            throw new ToolException("`project_name` is required for createConfiguration."); //$NON-NLS-1$
        }
        validateIdentifier(name, "project_name"); //$NON-NLS-1$
        var workspace = ResourcesPlugin.getWorkspace();
        var project = workspace.getRoot().getProject(name);
        if (project.exists())
        {
            throw new ToolException("A project with this name already exists: " + name); //$NON-NLS-1$
        }
        validateConfigurationParameters(request);
        var platformVersion = request.platformVersion != null && !request.platformVersion.isBlank()
            ? request.platformVersion : DEFAULT_PLATFORM_VERSION;
        var configurationMdo = configurationMdo(name, request, platformVersion);
        try
        {
            var description = workspace.newProjectDescription(name);
            workspace.run(monitor -> {
                project.create(description, monitor);
                project.open(monitor);
                project.setDefaultCharset(java.nio.charset.StandardCharsets.UTF_8.name(), monitor);
                writeProjectManifest(project, platformVersion, monitor);
                // Write a valid Configuration.mdo BEFORE enabling the configuration nature: an empty
                // configuration folder makes the EDT project context fail RESOURCE_LOADING, and seeding
                // the root through the BM afterwards is racy. Enabling the nature last starts the context
                // with the configuration already present.
                writeConfigurationMdo(project, configurationMdo, monitor);
                description.setNatureIds(new String[] { CONFIGURATION_NATURE, XTEXT_NATURE });
                project.setDescription(description, IResource.FORCE, monitor);
            }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, null);
        }
        catch (org.eclipse.core.runtime.CoreException e)
        {
            throw new ToolException("Failed to create configuration project " + name + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        var response = MetadataResponse.success(request, "Configuration", true); //$NON-NLS-1$
        response.resourcePath = configurationResourcePath(project);
        response.markerPath = response.resourcePath;
        refresh(project);
        try
        {
            projectBuilder.build(project, cancellationToken);
        }
        catch (org.eclipse.core.runtime.CoreException e)
        {
            response.warnings.add("EDT validation failed to start: " + e.getMessage()); //$NON-NLS-1$
        }
        return response;
    }

    @SuppressWarnings("nls")
    private static String configurationMdo(String name, MetadataRequest request, String platformVersion)
    {
        var compatibility = request.compatibilityMode != null && !request.compatibilityMode.isBlank()
            ? request.compatibilityMode : platformVersion;
        var languageName = resolveLanguageName(request);
        var languageCode = request.defaultLanguageCode;
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<mdclass:Configuration xmlns:mdclass=\"http://g5.1c.ru/v8/dt/metadata/mdclass\" uuid=\"")
            .append(UUID.randomUUID()).append("\">\n");
        sb.append("  <name>").append(name).append("</name>\n");
        if (request.title != null && !request.title.isBlank())
        {
            var key = languageCode != null && !languageCode.isBlank() ? languageCode : "ru";
            sb.append("  <synonym>\n    <key>").append(xml(key)).append("</key>\n    <value>")
                .append(xml(request.title)).append("</value>\n  </synonym>\n");
        }
        for (var classId : CONFIGURATION_SYSTEM_CLASS_IDS)
        {
            sb.append("  <containedObjects classId=\"").append(classId).append("\" objectId=\"")
                .append(UUID.randomUUID()).append("\"/>\n");
        }
        sb.append("  <defaultRunMode>ManagedApplication</defaultRunMode>\n");
        sb.append("  <usePurposes>PersonalComputer</usePurposes>\n");
        if (request.scriptVariant != null && !request.scriptVariant.isBlank())
        {
            sb.append("  <scriptVariant>").append(request.scriptVariant).append("</scriptVariant>\n");
        }
        if (request.vendor != null && !request.vendor.isBlank())
        {
            sb.append("  <vendor>").append(xml(request.vendor)).append("</vendor>\n");
        }
        if (request.version != null && !request.version.isBlank())
        {
            sb.append("  <version>").append(xml(request.version)).append("</version>\n");
        }
        if (languageName != null)
        {
            sb.append("  <defaultLanguage>Language.").append(languageName).append("</defaultLanguage>\n");
        }
        sb.append("  <dataLockControlMode>Managed</dataLockControlMode>\n");
        sb.append("  <objectAutonumerationMode>NotAutoFree</objectAutonumerationMode>\n");
        sb.append("  <modalityUseMode>DontUse</modalityUseMode>\n");
        sb.append("  <synchronousPlatformExtensionAndAddInCallUseMode>DontUse")
            .append("</synchronousPlatformExtensionAndAddInCallUseMode>\n");
        sb.append("  <compatibilityMode>").append(compatibility).append("</compatibilityMode>\n");
        if (languageName != null)
        {
            sb.append("  <languages uuid=\"").append(UUID.randomUUID()).append("\">\n");
            sb.append("    <name>").append(languageName).append("</name>\n");
            sb.append("    <synonym>\n      <key>").append(xml(languageCode)).append("</key>\n      <value>")
                .append(xml(languageName)).append("</value>\n    </synonym>\n");
            sb.append("    <languageCode>").append(xml(languageCode)).append("</languageCode>\n");
            sb.append("  </languages>\n");
        }
        sb.append("</mdclass:Configuration>\n");
        return sb.toString();
    }

    /** Resolves the default language object name, or {@code null} when no language was requested. */
    private static String resolveLanguageName(MetadataRequest request)
    {
        var code = request.defaultLanguageCode;
        if (code == null || code.isBlank())
        {
            return null;
        }
        if (request.defaultLanguageName != null && !request.defaultLanguageName.isBlank())
        {
            return request.defaultLanguageName;
        }
        switch (code.toLowerCase(Locale.ROOT))
        {
        case "ru": return "Русский"; //$NON-NLS-1$ //$NON-NLS-2$
        case "en": return "English"; //$NON-NLS-1$ //$NON-NLS-2$
        default: return code;
        }
    }

    private static void validateConfigurationParameters(MetadataRequest request)
    {
        if (request.compatibilityMode != null && !request.compatibilityMode.isBlank())
        {
            validateEnum(MdClassPackage.eINSTANCE.getCompatibilityMode(), request.compatibilityMode,
                "compatibility_mode"); //$NON-NLS-1$
        }
        if (request.scriptVariant != null && !request.scriptVariant.isBlank())
        {
            validateEnum(MdClassPackage.eINSTANCE.getScriptVariant(), request.scriptVariant, "script_variant"); //$NON-NLS-1$
        }
        var languageName = resolveLanguageName(request);
        if (languageName != null)
        {
            validateIdentifier(languageName, "default_language_name"); //$NON-NLS-1$
            if (!request.defaultLanguageCode.matches("[A-Za-z]{1,10}")) //$NON-NLS-1$
            {
                throw new ToolException(
                    "`default_language_code` must be a short language code such as `ru` or `en`."); //$NON-NLS-1$
            }
        }
        else if (request.defaultLanguageName != null && !request.defaultLanguageName.isBlank())
        {
            throw new ToolException("`default_language_name` requires `default_language_code`."); //$NON-NLS-1$
        }
    }

    private static void validateEnum(org.eclipse.emf.ecore.EEnum eenum, String value, String parameter)
    {
        if (eenum.getEEnumLiteralByLiteral(value) != null || eenum.getEEnumLiteral(value) != null)
        {
            return;
        }
        var valid = new java.util.ArrayList<String>();
        for (var literal : eenum.getELiterals())
        {
            valid.add(literal.getLiteral());
        }
        throw new ToolException("Invalid `" + parameter + "` value `" + value + "`. Valid values: " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + String.join(", ", valid) + "."); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String xml(String value)
    {
        if (value == null)
        {
            return ""; //$NON-NLS-1$
        }
        return value.replace("&", "&amp;").replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            .replace(">", "&gt;").replace("\"", "&quot;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    private void writeConfigurationMdo(IProject project, String content,
        org.eclipse.core.runtime.IProgressMonitor monitor) throws org.eclipse.core.runtime.CoreException
    {
        var src = project.getFolder("src"); //$NON-NLS-1$
        if (!src.exists())
        {
            src.create(true, true, monitor);
        }
        var configurationFolder = src.getFolder("Configuration"); //$NON-NLS-1$
        if (!configurationFolder.exists())
        {
            configurationFolder.create(true, true, monitor);
        }
        var file = configurationFolder.getFile("Configuration.mdo"); //$NON-NLS-1$
        file.create(new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)), true,
            monitor);
    }

    private MetadataResponse removeConfiguration(MetadataRequest request)
    {
        var name = request.projectName;
        if (name == null || name.isBlank())
        {
            throw new ToolException("`project_name` is required for removeConfiguration."); //$NON-NLS-1$
        }
        var project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
        boolean existed = project.exists();
        if (existed && editingSupport.isReadOnly(project))
        {
            // Reached before the shared read-only guard, so it is checked explicitly here.
            throw new ToolException("Configuration `" + name //$NON-NLS-1$
                + "` is on full vendor support and must not be removed. Ask the user to do it in EDT.", //$NON-NLS-1$
                ToolErrorType.USER_VISIBLE);
        }
        if (existed && !request.dryRun)
        {
            try
            {
                // Remove the project from the workspace but keep files on disk (non-destructive).
                project.delete(false, true, new NullProgressMonitor());
            }
            catch (org.eclipse.core.runtime.CoreException e)
            {
                throw new ToolException("Failed to remove configuration project " + name + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        var response = MetadataResponse.success(request, name, existed);
        if (existed)
        {
            response.warnings.add("Project removed from workspace; source files remain on disk."); //$NON-NLS-1$
        }
        return response;
    }

    @SuppressWarnings("deprecation")
    private void writeProjectManifest(IProject project, String version,
        org.eclipse.core.runtime.IProgressMonitor monitor) throws org.eclipse.core.runtime.CoreException
    {
        var headers = new java.util.HashMap<String, String>();
        headers.put(com._1c.g5.v8.dt.core.platform.ProjectManifest.MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
        headers.put(com._1c.g5.v8.dt.core.platform.ProjectManifest.RUNTIME_VERSION, version);
        try (var out = new java.io.ByteArrayOutputStream())
        {
            var dtInf = project.getFolder(com._1c.g5.v8.dt.core.platform.ProjectManifest.DT_INF_FOLDER);
            if (!dtInf.exists())
            {
                dtInf.create(true, true, monitor);
            }
            com._1c.g5.v8.dt.core.platform.ProjectManifest.writeProjectManifest(out, headers);
            var manifestFile = project.getFile(com._1c.g5.v8.dt.core.platform.ProjectManifest.DT_PROJECT_MANIFEST);
            manifestFile.create(new java.io.ByteArrayInputStream(out.toByteArray()), true, monitor);
        }
        catch (java.io.IOException e)
        {
            throw new ToolException("Failed to write project manifest: " + e.getMessage()); //$NON-NLS-1$
        }
    }

    private static String configurationResourcePath(IProject project)
    {
        var location = project.getFile("src/Configuration/Configuration.mdo").getLocation(); //$NON-NLS-1$
        return location != null ? location.toOSString() : "src/Configuration/Configuration.mdo"; //$NON-NLS-1$
    }

    // ===== Post-mutation marker auto-check =====

    /** Markers returned inline; a small page keeps mutation responses compact. */
    private static final int MAX_INLINE_MARKERS = 10;
    /** Interval between marker reads while waiting for the set to stabilize. */
    private static final long MARKER_POLL_INTERVAL_MS = 200L;
    /** Upper bound on stabilization polls, so a churning marker set cannot stall a mutation. */
    private static final int MAX_MARKER_POLLS = 15;
    /** Wall-clock budget for stabilization, independent of the (much larger) persistence timeout. */
    private static final long MARKER_STABILIZE_BUDGET_MS = 5_000L;

    /**
     * Reads the markers of the changed resource and puts them into the response.
     * <p>
     * EDT produces markers asynchronously (Derived Data pipeline) and flushes them in batches, so a
     * single read can return a partial snapshot even after {@link IProjectBuilder#build} reported
     * completion. Therefore the set is polled until two consecutive reads agree, bounded by
     * {@link #MAX_MARKER_POLLS} and {@link #MARKER_STABILIZE_BUDGET_MS}. This does not depend on
     * knowing internal DD segment ids, so it keeps working even if those ever change.
     * <p>
     * Scope is the changed resource only. Operations that can break references elsewhere (remove,
     * rename) therefore also get an explicit hint to run a project-wide check.
     */
    private void collectMarkers(IProject project, MetadataResponse response, ICancellationToken cancellationToken,
        boolean validationComplete, long timeoutMillis)
    {
        if (response.markerPath == null || markersProviders.isEmpty())
        {
            return;
        }
        try
        {
            var file = fileForPath(project, response.markerPath);
            if (file == null || !file.exists())
            {
                return;
            }
            // Typical case: the second read equals the first, so this costs one extra read + one
            // interval. The poll cap bounds the pathological case (markers still churning) so a long
            // build of hundreds of mutations cannot stall on any single one.
            long deadline = System.currentTimeMillis() + Math.min(timeoutMillis, MARKER_STABILIZE_BUDGET_MS);
            var markers = readMarkers(project, file);
            boolean stable = false;
            for (int poll = 0; poll < MAX_MARKER_POLLS; poll++)
            {
                if (System.currentTimeMillis() >= deadline || cancellationToken.isCanceled())
                {
                    break;
                }
                Thread.sleep(MARKER_POLL_INTERVAL_MS);
                var next = readMarkers(project, file);
                stable = sameMarkers(markers, next);
                markers = next;
                if (stable)
                {
                    break;
                }
            }

            var counts = new LinkedHashMap<String, Integer>();
            int errors = 0;
            int warnings = 0;
            int infos = 0;
            for (var marker : markers)
            {
                if (MarkerInfo.SEVERITY_ERROR.equals(marker.severity))
                {
                    errors++;
                }
                else if (MarkerInfo.SEVERITY_WARNING.equals(marker.severity))
                {
                    warnings++;
                }
                else
                {
                    infos++;
                }
            }
            counts.put("errors", Integer.valueOf(errors)); //$NON-NLS-1$
            counts.put("warnings", Integer.valueOf(warnings)); //$NON-NLS-1$
            counts.put("infos", Integer.valueOf(infos)); //$NON-NLS-1$
            counts.put("total", Integer.valueOf(markers.size())); //$NON-NLS-1$
            response.markerCount = counts;
            response.markers = markers.size() > MAX_INLINE_MARKERS
                ? new java.util.ArrayList<>(markers.subList(0, MAX_INLINE_MARKERS)) : markers;
            if (markers.size() > MAX_INLINE_MARKERS || !validationComplete || !stable)
            {
                response.markersIncomplete = Boolean.TRUE;
            }
            if (errors > 0)
            {
                response.warnings.add("The changed resource has " + errors //$NON-NLS-1$
                    + " error marker(s). Fix them before reporting success."); //$NON-NLS-1$
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (RuntimeException e)
        {
            // The auto-check must never fail a successful mutation; GetMarkers remains available.
            response.warnings.add("Marker auto-check failed: " + e.getMessage()); //$NON-NLS-1$
        }
    }

    /** Markers of one file, most important first (error > warning > info, then priority). */
    private List<MarkerInfo> readMarkers(IProject project, IFile file)
    {
        var result = new java.util.ArrayList<MarkerInfo>();
        for (var provider : markersProviders)
        {
            try (var stream = provider.getMarkers(project, file))
            {
                stream.forEach(result::add);
            }
        }
        result.sort(new com.e1c.edt.ai.tools.MarkerInfoComparator());
        return result;
    }

    private static boolean sameMarkers(List<MarkerInfo> left, List<MarkerInfo> right)
    {
        if (left.size() != right.size())
        {
            return false;
        }
        for (int i = 0; i < left.size(); i++)
        {
            var a = left.get(i);
            var b = right.get(i);
            if (!java.util.Objects.equals(a.severity, b.severity)
                || !java.util.Objects.equals(a.message, b.message)
                || !java.util.Objects.equals(a.startLine, b.startLine))
            {
                return false;
            }
        }
        return true;
    }

    /** Resolves a workspace file from an absolute OS path inside the project. */
    private static IFile fileForPath(IProject project, String absolutePath)
    {
        var projectLocation = project.getLocation();
        if (projectLocation == null)
        {
            return null;
        }
        var path = new org.eclipse.core.runtime.Path(absolutePath);
        if (!projectLocation.isPrefixOf(path))
        {
            return null;
        }
        return project.getFile(path.removeFirstSegments(projectLocation.segmentCount()).makeRelative());
    }

    // ===== Code module (.bsl) operations =====
    // Modules are transient references in the metadata model; the .bsl file on disk is the source of
    // truth and its path is derived from (owner object, module reference) by EDT's file-system support.
    // These operations create or delete that file. The BSL text itself is edited with the Edit tool.

    /** Public module_kind -> transient mdclass module EReference feature name. */
    private static final Map<String, String> MODULE_KINDS = createModuleKinds();

    private static Map<String, String> createModuleKinds()
    {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("object_module", "objectModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("manager_module", "managerModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("record_set_module", "recordSetModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("value_manager_module", "valueManagerModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("command_module", "commandModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("module", "module"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("managed_application_module", "managedApplicationModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("ordinary_application_module", "ordinaryApplicationModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("external_connection_module", "externalConnectionModule"); //$NON-NLS-1$ //$NON-NLS-2$
        result.put("session_module", "sessionModule"); //$NON-NLS-1$ //$NON-NLS-2$
        return result;
    }

    private static String moduleKindForFeature(String featureName)
    {
        for (var entry : MODULE_KINDS.entrySet())
        {
            if (entry.getValue().equals(featureName))
            {
                return entry.getKey();
            }
        }
        return featureName;
    }

    private static boolean isModuleReference(org.eclipse.emf.ecore.EStructuralFeature feature)
    {
        return feature instanceof EReference && !feature.isMany()
            && feature.getEType() == BslPackage.eINSTANCE.getModule();
    }

    /**
     * Returns the object that actually declares the module references. For a form metadata object the
     * module lives on the form body (AbstractForm.module), reached through BasicForm.form, not on the
     * form object itself.
     */
    private static EObject moduleHolder(MdObject owner)
    {
        if (owner instanceof BasicForm)
        {
            var body = ((BasicForm)owner).getForm();
            if (body != null)
            {
                return body;
            }
        }
        return owner;
    }

    private MetadataResponse listModules(IProject project, MetadataRequest request)
    {
        var modules = model(project).getGlobalContext().execute(
            new AbstractBmTask<List<Map<String, Object>>>("List 1C code modules") //$NON-NLS-1$
            {
                @Override
                public List<Map<String, Object>> execute(IBmTransaction transaction,
                    org.eclipse.core.runtime.IProgressMonitor monitor)
                {
                    var owner = requireObject(transaction, request.objectName);
                    var holder = moduleHolder(owner);
                    var support = fileSystemSupportProvider.getProjectFileSystemSupport(project);
                    var result = new java.util.ArrayList<Map<String, Object>>();
                    for (var feature : holder.eClass().getEAllStructuralFeatures())
                    {
                        if (!isModuleReference(feature))
                        {
                            continue;
                        }
                        var file = support.getFile(holder, (EReference)feature);
                        var item = new LinkedHashMap<String, Object>();
                        item.put("module_kind", moduleKindForFeature(feature.getName())); //$NON-NLS-1$
                        item.put("relative_path", file.getProjectRelativePath().toString()); //$NON-NLS-1$
                        var location = file.getLocation();
                        item.put("path", location != null ? location.toOSString() : file.getFullPath().toString()); //$NON-NLS-1$
                        item.put("exists", Boolean.valueOf(file.exists())); //$NON-NLS-1$
                        result.add(item);
                    }
                    return result;
                }
            });
        var response = MetadataResponse.success(request, request.objectName, false);
        response.resourcePath = metadataResourcePath(project, request.objectName);
        response.markerPath = response.resourcePath;
        var details = new LinkedHashMap<String, Object>();
        details.put("object", request.objectName); //$NON-NLS-1$
        details.put("modules", modules); //$NON-NLS-1$
        details.put("edit_hint", "Edit module text with the Edit tool using the module `path`."); //$NON-NLS-1$ //$NON-NLS-2$
        response.details = details;
        return response;
    }

    private void requireInlineObject(IProject project, String fqn)
    {
        var parts = objectParts(fqn);
        var descriptor = MetadataObjectTypeRegistry.get(parts[0]);
        var found = model(project).getGlobalContext().execute(new AbstractBmTask<Boolean>("Read inline 1C object") //$NON-NLS-1$
        {
            @Override
            public Boolean execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var configuration = transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                return Boolean.valueOf(configuration instanceof MdObject
                    && findNamed(featureList((MdObject)configuration, descriptor.collection), parts[1]) != null);
            }
        });
        if (!Boolean.TRUE.equals(found))
        {
            throw new ToolException("Metadata object not found: " + fqn); //$NON-NLS-1$
        }
    }

    /**
     * Subordinate (nested) object descriptor: a metadata object contained in another object's
     * collection, e.g. a Recalculation inside a CalculationRegister.
     */
    private static final class Subordinate
    {
        final String kind;
        final String parentType;
        final String subType;
        final String feature;

        Subordinate(String kind, String parentType, String subType, String feature)
        {
            this.kind = kind;
            this.parentType = parentType;
            this.subType = subType;
            this.feature = feature;
        }

        String initializer()
        {
            return "com._1c.g5.v8.dt.md.model." + subType + "Initializer"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static final Map<String, Subordinate> SUBORDINATES = createSubordinates();

    private static Map<String, Subordinate> createSubordinates()
    {
        Map<String, Subordinate> result = new LinkedHashMap<>();
        result.put("recalculation", //$NON-NLS-1$
            new Subordinate("recalculation", "CalculationRegister", "Recalculation", "recalculations")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        result.put("integration_service_channel", //$NON-NLS-1$
            new Subordinate("integration_service_channel", "IntegrationService", "IntegrationServiceChannel", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "integrationServiceChannels")); //$NON-NLS-1$
        return result;
    }

    private static Subordinate subordinate(String kind)
    {
        var result = SUBORDINATES.get(kind);
        if (result == null)
        {
            throw new ToolException("Unsupported subordinate_kind `" + kind + "`. Valid values: " //$NON-NLS-1$ //$NON-NLS-2$
                + String.join(", ", SUBORDINATES.keySet()) + "."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return result;
    }

    private MetadataResponse addSubordinateObject(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        var sub = subordinate(request.subordinateKind);
        var parentParts = objectParts(request.objectName);
        if (!sub.parentType.equals(parentParts[0]))
        {
            throw new ToolException("subordinate_kind `" + sub.kind + "` requires object_name of type " //$NON-NLS-1$ //$NON-NLS-2$
                + sub.parentType + ", got " + parentParts[0] + "."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        var classifier = MdClassPackage.eINSTANCE.getEClassifier(sub.subType);
        if (!(classifier instanceof EClass))
        {
            throw new ToolException("EDT metadata class is not available: " + sub.subType); //$NON-NLS-1$
        }
        var model = model(project);
        boolean[] changed = { false };
        String target = request.objectName + "." + sub.subType + "." + request.name; //$NON-NLS-1$ //$NON-NLS-2$
        model.getGlobalContext().execute(new AbstractBmTask<Void>("Add 1C subordinate object") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                MdObject owner = requireObject(transaction, request.objectName);
                var list = featureList(owner, sub.feature);
                if (findNamed(list, request.name) != null)
                {
                    return null;
                }
                var v8Project = v8ProjectManager.getProject(project);
                if (v8Project == null)
                {
                    throw new ToolException("V8 project is not available: " + project.getName()); //$NON-NLS-1$
                }
                MdObject child = createViaInitializer(sub.initializer(), v8Project);
                if (child == null)
                {
                    child = (MdObject)MdClassFactory.eINSTANCE.create((EClass)classifier);
                }
                child.setName(request.name);
                child.setUuid(UUID.randomUUID());
                if (request.title != null && !request.title.isBlank())
                {
                    child.getSynonym().put("ru", request.title); //$NON-NLS-1$
                }
                changed[0] = true;
                if (!request.dryRun)
                {
                    list.add(child);
                }
                return null;
            }
        });
        if (changed[0] && !request.dryRun)
        {
            requireSubordinate(project, request.objectName, sub, request.name);
        }
        return MetadataResponse.success(request, target, changed[0]);
    }

    private MetadataResponse removeSubordinateObject(IProject project, MetadataRequest request)
    {
        var sub = subordinate(request.subordinateKind);
        var model = model(project);
        boolean[] changed = { false };
        String target = request.objectName + "." + sub.subType + "." + request.name; //$NON-NLS-1$ //$NON-NLS-2$
        model.getGlobalContext().execute(new AbstractBmTask<Void>("Remove 1C subordinate object") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                MdObject owner = requireObject(transaction, request.objectName);
                var child = findNamed(featureList(owner, sub.feature), request.name);
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
        return MetadataResponse.success(request, target, changed[0]);
    }

    private void requireSubordinate(IProject project, String parentFqn, Subordinate sub, String name)
    {
        var found = model(project).getGlobalContext().execute(new AbstractBmTask<Boolean>("Read 1C subordinate") //$NON-NLS-1$
        {
            @Override
            public Boolean execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var owner = transaction.getTopObjectByFqn(parentFqn);
                return Boolean.valueOf(owner instanceof MdObject
                    && findNamed(featureList((MdObject)owner, sub.feature), name) != null);
            }
        });
        if (!Boolean.TRUE.equals(found))
        {
            throw new ToolException("Subordinate object was not created: " + parentFqn + "." + sub.subType //$NON-NLS-1$ //$NON-NLS-2$
                + "." + name); //$NON-NLS-1$
        }
    }

    /**
     * Attaches a body to a standalone form (a CommonForm). An empty {@link FormFactory} form leaves
     * mandatory features unset ({@code commandInterface}, {@code commandBar}, {@code navigationPanel}),
     * so EDT reports the new form as invalid; the generator produces a complete one. A CommonForm is at
     * once the owner and the form, hence it is passed as both. Falls back to the bare body if generation
     * is unavailable, which is no worse than before.
     */
    private void attachFormBody(IProject project, IBmTransaction transaction, BasicForm formMetadata)
    {
        try
        {
            var v8Project = v8ProjectManager.getProject(project);
            if (v8Project != null && formMetadata instanceof MdObject)
            {
                var owner = (MdObject)formMetadata;
                var scriptVariant = v8Project.getScriptVariant();
                var version = v8Project.getVersion();
                var languageCode = editingLanguageManager.getEditingLanguageCode(project);
                var rootField =
                    formFieldGenerator.getFormGeneratorFields(owner, FormType.GENERIC, scriptVariant, version);
                Form generated = formGenerator.generateForm(owner, formMetadata, FormType.GENERIC, scriptVariant,
                    languageCode, version, rootField, Integer.valueOf(1));
                if (generated != null)
                {
                    formMetadata.setForm(generated);
                    generated.setMdForm(formMetadata);
                    attachFormResource(transaction, formMetadata, generated);
                    return;
                }
            }
        }
        catch (RuntimeException | Error e)
        {
            // Generation unavailable for this form: fall through to the bare body below.
        }
        attachEmptyFormBody(transaction, formMetadata);
    }

    private void attachFormResource(IBmTransaction transaction, BasicForm formMetadata, Form form)
    {
        var formReference = (org.eclipse.emf.ecore.EReference)formMetadata.eClass().getEStructuralFeature("form"); //$NON-NLS-1$
        transaction.attachTopObject((IBmObject)form, fqnGenerator.generateExternalPropertyFqn(formMetadata,
            formReference));
    }

    private void attachEmptyFormBody(IBmTransaction transaction, BasicForm formMetadata)
    {
        Form form = FormFactory.eINSTANCE.createForm();
        formMetadata.setForm(form);
        form.setMdForm(formMetadata);
        var formReference = (org.eclipse.emf.ecore.EReference)formMetadata.eClass().getEStructuralFeature("form"); //$NON-NLS-1$
        var formFqn = fqnGenerator.generateExternalPropertyFqn(formMetadata, formReference);
        transaction.attachTopObject((IBmObject)form, formFqn);
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
                // A single-valued reference property (BusinessProcess.task, CalculationRegister
                // .chartOfCalculationTypes, ...) is mandatory for some object types, and leaving it unset
                // keeps the object permanently invalid. Its value is given as the target FQN.
                if (isSingleReference(feature))
                {
                    var target = transaction.getTopObjectByFqn(normalizeConfigurationFqn(request.propertyValue));
                    if (!(target instanceof MdObject))
                    {
                        throw new ToolException("Referenced metadata object not found: " + request.propertyValue //$NON-NLS-1$
                            + ". Property `" + request.propertyName + "` expects the FQN of an existing object," //$NON-NLS-1$ //$NON-NLS-2$
                            + " for example Task.MyTask."); //$NON-NLS-1$
                    }
                    changed[0] = object.eGet(feature) != target;
                    if (changed[0] && !request.dryRun)
                    {
                        object.eSet(feature, target);
                    }
                    return null;
                }
                if (feature == null || feature.isMany() || !(feature.getEType() instanceof EDataType))
                {
                    throw new ToolException("Unsupported property `" + request.propertyName //$NON-NLS-1$
                        + "` for " + request.objectName + "." //$NON-NLS-1$ //$NON-NLS-2$
                        + propertySuggestion(object, request.propertyName)
                        + (feature != null && feature.isMany()
                            ? " That property holds a collection: use addObjectReference/removeObjectReference." //$NON-NLS-1$
                            : "") //$NON-NLS-1$
                        + " Valid scalar properties: " + scalarPropertyNames(object) + "."); //$NON-NLS-1$ //$NON-NLS-2$
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
        if (MetadataObjectTypeRegistry.get(objectParts(request.objectName)[0]).inlineInConfiguration)
        {
            return removeInlineObject(project, request);
        }
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

    private MetadataResponse removeInlineObject(IProject project, MetadataRequest request)
    {
        var parts = objectParts(request.objectName);
        var descriptor = MetadataObjectTypeRegistry.get(parts[0]);
        boolean[] changed = { false };
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Remove inline 1C object") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                var configuration = transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                if (!(configuration instanceof MdObject))
                {
                    return null;
                }
                var child = findNamed(featureList((MdObject)configuration, descriptor.collection), parts[1]);
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
        return MetadataResponse.success(request, request.objectName, changed[0]);
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
                var child = createChild(project, owner, kind, featureName);
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

    private MdObject createTopObject(IProject project, String fqn)
    {
        var descriptor = MetadataObjectTypeRegistry.get(objectParts(fqn)[0]);
        var v8Project = v8ProjectManager.getProject(project);
        if (v8Project == null)
        {
            throw new ToolException("V8 project is not available: " + project.getName()); //$NON-NLS-1$
        }
        var classifier = MdClassPackage.eINSTANCE.getEClassifier(descriptor.name);
        if (!(classifier instanceof EClass))
        {
            throw new ToolException("EDT metadata class is not available: " + descriptor.name); //$NON-NLS-1$
        }
        if (descriptor.initializer != null)
        {
            var initialized = createViaInitializer(descriptor.initializer, v8Project);
            if (initialized != null)
            {
                return initialized;
            }
        }
        // No initializer, or it failed: a plain object is still a valid top object for such types.
        return (MdObject)MdClassFactory.eINSTANCE.create((EClass)classifier);
    }

    /**
     * Runs an official EDT {@link IMdObjectInitializer} to build a complete object (produced types,
     * default type descriptions, ...). The initializer is normally Guice-managed; here it is created
     * directly, so its {@code mdTypeUtil} collaborator (dereferenced in {@code create()} by e.g.
     * ChartOfCharacteristicTypes and AccountingRegister) is supplied reflectively. Returns {@code null}
     * on any failure so the caller can fall back to a bare {@link MdClassFactory} object.
     */
    private MdObject createViaInitializer(String className, com._1c.g5.v8.dt.core.platform.IV8Project v8Project)
    {
        try
        {
            var initializer = newInitializer(className);
            var created = initializer.create(v8Project, v8Project.getVersion());
            return created instanceof MdObject ? (MdObject)created : null;
        }
        catch (RuntimeException | Error | ReflectiveOperationException e)
        {
            return null;
        }
    }

    private static IMdObjectInitializer<?> newInitializer(String className) throws ReflectiveOperationException
    {
        var instance = Class.forName(className).getDeclaredConstructor().newInstance();
        if (!(instance instanceof IMdObjectInitializer<?>))
        {
            throw new ToolException("EDT initializer has an unexpected type: " + className); //$NON-NLS-1$
        }
        injectInitializerCollaborators(instance);
        return (IMdObjectInitializer<?>)instance;
    }

    /**
     * Best-effort: sets the initializer's inherited {@code mdTypeUtil} field to a fresh
     * {@code com._1c.g5.v8.dt.md.resource.MdTypeUtil}. That class has a usable no-argument constructor,
     * and the type-description helpers used during create() do not need its injected scope provider.
     */
    private static void injectInitializerCollaborators(Object initializer) throws ReflectiveOperationException
    {
        var field = findField(initializer.getClass(), "mdTypeUtil"); //$NON-NLS-1$
        if (field == null)
        {
            return;
        }
        var mdTypeUtil = Class.forName("com._1c.g5.v8.dt.md.resource.MdTypeUtil") //$NON-NLS-1$
            .getDeclaredConstructor().newInstance();
        field.setAccessible(true);
        field.set(initializer, mdTypeUtil);
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name)
    {
        for (var current = type; current != null; current = current.getSuperclass())
        {
            try
            {
                return current.getDeclaredField(name);
            }
            catch (NoSuchFieldException e)
            {
                // try superclass
            }
        }
        return null;
    }

    private static String childInitializerName(MdObject owner)
    {
        var className = owner.eClass().getName();
        if (className.endsWith("TabularSection")) //$NON-NLS-1$
        {
            return "com._1c.g5.v8.dt.md.model." + className + "Initializer"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        try
        {
            return MetadataObjectTypeRegistry.get(className).initializer;
        }
        catch (ToolException e)
        {
            return null;
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
        if ("Configuration".equals(target) || (target != null && target.startsWith("Configuration."))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            // The configuration root object itself (addressable via inspectObject/setObjectProperty),
            // including the model's frequent "Configuration.<ProjectName>" form.
            return "src/Configuration/Configuration.mdo"; //$NON-NLS-1$
        }
        var parts = target != null ? target.split("\\.", -1) : new String[0]; //$NON-NLS-1$
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank())
        {
            throw new ToolException("Cannot derive metadata resource path from target: " + target); //$NON-NLS-1$
        }
        var descriptor = MetadataObjectTypeRegistry.get(parts[0]);
        if (descriptor.inlineInConfiguration)
        {
            // Inline types (e.g. Language) live inside Configuration.mdo, not their own resource.
            return "src/Configuration/Configuration.mdo"; //$NON-NLS-1$
        }
        return "src/" + descriptor.folder + "/" + parts[1] + "/" + parts[1] + ".mdo"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    private static String topFolder(String type)
    {
        return MetadataObjectTypeRegistry.get(type).folder;
    }

    private MdObject createChild(IProject project, MdObject owner, FeatureKind kind, String featureName)
    {
        var feature = owner.eClass().getEStructuralFeature(featureName);
        if (feature != null && feature.getEType() instanceof EClass)
        {
            var initializerName = childInitializerName(owner);
            if (initializerName != null)
            {
                var v8Project = v8ProjectManager.getProject(project);
                if (v8Project == null)
                {
                    throw new ToolException("V8 project is not available: " + project.getName()); //$NON-NLS-1$
                }
                try
                {
                    var initialized = newInitializer(initializerName)
                        .createChildObject((EClass)feature.getEType(), owner, v8Project.getVersion());
                    if (initialized instanceof MdObject)
                    {
                        return (MdObject)initialized;
                    }
                }
                catch (RuntimeException | Error | ReflectiveOperationException e)
                {
                    // Best effort: fall through to the explicit MdClassFactory fallbacks below.
                }
            }
        }
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

    /** Sorted names of an object's writable scalar properties, for helpful "unsupported property" errors. */
    private static String scalarPropertyNames(EObject object)
    {
        var names = scalarPropertyNameSet(object);
        return names.isEmpty() ? "(none)" : String.join(", ", names); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static java.util.SortedSet<String> scalarPropertyNameSet(EObject object)
    {
        var names = new java.util.TreeSet<String>();
        for (var feature : object.eClass().getEAllStructuralFeatures())
        {
            if (!feature.isMany() && feature.getEType() instanceof EDataType && feature.isChangeable())
            {
                names.add(feature.getName());
            }
        }
        return names;
    }

    /**
     * "Did you mean" prefix for an unsupported property name. Models routinely shorten real names (for
     * example {@code client} instead of {@code clientManagedApplication}), so the closest candidates are
     * offered before the full list to cut the retry loop.
     */
    private static String propertySuggestion(EObject object, String requested)
    {
        if (requested == null || requested.isBlank())
        {
            return ""; //$NON-NLS-1$
        }
        var needle = requested.toLowerCase(Locale.ROOT);
        var matches = new java.util.ArrayList<String>();
        for (var name : scalarPropertyNameSet(object))
        {
            var candidate = name.toLowerCase(Locale.ROOT);
            if (candidate.startsWith(needle) || candidate.contains(needle))
            {
                matches.add(name);
            }
        }
        return matches.isEmpty() ? "" : " Did you mean: " + String.join(", ", matches) + "?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    private static boolean setScalarProperty(EObject object, String propertyName, String propertyValue,
        boolean dryRun)
    {
        var feature = object.eClass().getEStructuralFeature(propertyName);
        if (feature == null || feature.isMany() || !(feature.getEType() instanceof EDataType))
        {
            throw new ToolException("Unsupported scalar property `" + propertyName + "` for " //$NON-NLS-1$ //$NON-NLS-2$
                + object.eClass().getName() + "." + propertySuggestion(object, propertyName) //$NON-NLS-1$
                + " Valid scalar properties: " + scalarPropertyNames(object) + "."); //$NON-NLS-1$ //$NON-NLS-2$
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
            // The body file name depends on the template type (Template.mxlx, Template.txt,
            // Template.htmldoc, ...), so it is not guessed here: the folder is verified instead, and
            // createObjectTemplate reports the exact body path from EDT in details.body_path.
            return Paths.get(base, "Templates", request.name).toString(); //$NON-NLS-1$
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
        var object = transaction.getTopObjectByFqn(normalizeConfigurationFqn(fqn));
        if (!(object instanceof MdObject)) throw new ToolException("Metadata object not found: " + fqn); //$NON-NLS-1$
        return (MdObject)object;
    }

    /**
     * The single configuration root is always addressed by the FQN {@code Configuration}, but the model
     * often writes {@code Configuration.<ProjectName>} (e.g. for application-level modules or properties).
     * No real top object is named {@code Configuration.*}, so normalizing it to {@code Configuration} is
     * safe and makes those calls resolve.
     */
    private static String normalizeConfigurationFqn(String fqn)
    {
        if (fqn != null && (fqn.equals("Configuration") || fqn.startsWith("Configuration."))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return "Configuration"; //$NON-NLS-1$
        }
        return fqn;
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
