/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import com._1c.g5.v8.bm.core.IBmNamespace;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmEditingContext;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.bm.integration.IBmTask;
import com._1c.g5.v8.dt.core.model.IModelObjectFactory;
import com._1c.g5.v8.dt.core.naming.ITopObjectFqnGenerator;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AddressingAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Bot;
import com._1c.g5.v8.dt.metadata.mdclass.BusinessProcess;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypes;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypes;
import com._1c.g5.v8.dt.metadata.mdclass.Column;
import com._1c.g5.v8.dt.metadata.mdclass.CommonAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.CommonCommand;
import com._1c.g5.v8.dt.metadata.mdclass.CommonForm;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.CommonPicture;
import com._1c.g5.v8.dt.metadata.mdclass.CommonTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Constant;
import com._1c.g5.v8.dt.metadata.mdclass.DataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.Dimension;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentJournal;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentNumerator;
import com._1c.g5.v8.dt.metadata.mdclass.Enum;
import com._1c.g5.v8.dt.metadata.mdclass.EnumValue;
import com._1c.g5.v8.dt.metadata.mdclass.EventSubscription;
import com._1c.g5.v8.dt.metadata.mdclass.ExchangePlan;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalDataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalDataSource;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalReport;
import com._1c.g5.v8.dt.metadata.mdclass.Field;
import com._1c.g5.v8.dt.metadata.mdclass.FilterCriterion;
import com._1c.g5.v8.dt.metadata.mdclass.Function;
import com._1c.g5.v8.dt.metadata.mdclass.FunctionalOption;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Language;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.Operation;
import com._1c.g5.v8.dt.metadata.mdclass.Recalculation;
import com._1c.g5.v8.dt.metadata.mdclass.Report;
import com._1c.g5.v8.dt.metadata.mdclass.Role;
import com._1c.g5.v8.dt.metadata.mdclass.ScheduledJob;
import com._1c.g5.v8.dt.metadata.mdclass.Sequence;
import com._1c.g5.v8.dt.metadata.mdclass.SessionParameter;
import com._1c.g5.v8.dt.metadata.mdclass.StandardAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.StandardCommand;
import com._1c.g5.v8.dt.metadata.mdclass.StyleItem;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com._1c.g5.v8.dt.metadata.mdclass.Task;
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com.e1c.edt.ai.tools.IJShellBindingProvider;
import com.e1c.edt.ai.tools.JShellBindingDescription;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provides JShell bindings for 1C metadata creation and editing operations.
 * This provider enables LLM to programmatically create, modify, and manage metadata objects
 * in 1C:Enterprise configurations through EDT API.
 *
 * Key Concepts:
 * - All metadata changes must be performed within BM transactions
 * - Use MdClassFactory.eINSTANCE to create metadata objects
 * - Use IBmEditingContext to execute transactions
 * - Use ITopObjectFqnGenerator to generate FQNs for top-level objects
 * - Use IModelObjectFactory to create objects with proper initialization
 *
 * @author 1C AI Team
 */
@Singleton
public class MetadataBindingProvider
    implements IJShellBindingProvider
{
    @Inject
    private IV8ProjectManager v8projectManager;

    @Inject
    private IBmModelManager modelManager;

    @Inject
    private ITopObjectFqnGenerator topObjectFqnGenerator;

    @Inject
    private IResourceLookup resourceLookup;

    @Inject
    private IModelObjectFactory modelObjectFactory;

    @SuppressWarnings("nls")
    @Override
    public Map<String, Object> getBindings()
    {
        var bindings = new HashMap<String, Object>();

        // Core factories and generators
        bindings.put("mdFactory", MdClassFactory.eINSTANCE);
        bindings.put("fqnGenerator", topObjectFqnGenerator);
        bindings.put("modelFactory", modelObjectFactory);
        bindings.put("projectManager", v8projectManager);
        bindings.put("modelManager", modelManager);
        bindings.put("resourceLookup", resourceLookup);

        // Workspace access
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        bindings.put("workspaceRoot", root);

        return bindings;
    }

    @SuppressWarnings("nls")
    @Override
    public Map<String, JShellBindingDescription> getBindingDescriptions()
    {
        var infos = new HashMap<String, JShellBindingDescription>();

        // MdClassFactory
        infos.put("mdFactory", new JShellBindingDescription(
            "Factory for creating 1C metadata objects (Catalogs, Documents, Reports, etc.)",
            buildMdFactoryDescription()));

        // FQN Generator
        infos.put("fqnGenerator", new JShellBindingDescription(
            "Generates FQNs (Fully Qualified Names) for metadata objects",
            buildFqnGeneratorDescription()));

        // Model Factory
        infos.put("modelFactory", new JShellBindingDescription(
            "Factory for creating model objects with proper initialization and context",
            buildModelFactoryDescription()));

        // Project Manager
        infos.put("projectManager", new JShellBindingDescription(
            "Manages 1C projects (configurations) in the workspace",
            buildProjectManagerDescription()));

        // Model Manager
        infos.put("modelManager", new JShellBindingDescription(
            "Provides access to BM (Business Model) for 1C projects",
            buildModelManagerDescription()));

        // Resource Lookup
        infos.put("resourceLookup", new JShellBindingDescription(
            "Looks up Eclipse resources (IProject, IFile) for 1C objects",
            buildResourceLookupDescription()));

        // Workspace Root
        infos.put("workspaceRoot", new JShellBindingDescription(
            "Eclipse workspace root for accessing all projects",
            buildWorkspaceRootDescription()));

        // Metadata Types and Features
        infos.put("metadataTypes", new JShellBindingDescription(
            "Complete guide to metadata types and their features",
            buildMetadataTypesAndFeatures()));

        // Complete Examples
        infos.put("completeExamples", new JShellBindingDescription(
            "Full examples of creating, editing, and deleting metadata",
            buildCompleteExamples()));

        // Workflows
        infos.put("workflows", new JShellBindingDescription(
            "Detailed workflows for metadata operations",
            buildWorkflows()));

        return infos;
    }

    @Override
    public String getDescription()
    {
        return "1C metadata API (factories, project manager, BM model)";
    }

    @Override
    public Collection<Class<?>> getSignificantClasses()
    {
        return List.of(
            // Core metadata classes
            MdClassFactory.class,
            MdObject.class,
            Configuration.class,

            // Metadata object types
            Catalog.class,
            Document.class,
            Report.class,
            BasicFeature.class,

            // Registers
            InformationRegister.class,
            AccumulationRegister.class,
            BasicRegister.class,
            Recalculation.class,

            // Common metadata
            CommonModule.class,
            Subsystem.class,
            Enum.class,
            EnumValue.class,

            // Business objects
            DataProcessor.class,
            Constant.class,
            ExchangePlan.class,
            DocumentJournal.class,

            // System objects
            FunctionalOption.class,
            ScheduledJob.class,
            EventSubscription.class,
            Role.class,
            SessionParameter.class,

            // Integration
            // XDTOPackage class removed - does not exist in mdclass package

            // UI/Resources
            StyleItem.class,
            CommonPicture.class,
            Form.class,
            Template.class,
            CommonForm.class, CommonTemplate.class,

            // Framework objects
            Language.class,
            CommonAttribute.class,
            CommonCommand.class,
            StandardCommand.class,

            // Registers details
            Dimension.class,

            // Advanced objects
            Bot.class,
            DocumentNumerator.class,
            Sequence.class,
            BusinessProcess.class,
            Task.class,

            // Accounting
            CalculationRegister.class,
            AccountingRegister.class,
            ChartOfAccounts.class,
            ChartOfCalculationTypes.class,
            ChartOfCharacteristicTypes.class,

            // Other
            ExternalDataSource.class,
            ExternalDataProcessor.class, ExternalReport.class,
            FilterCriterion.class,

            // Object components
            Field.class,
            Column.class,
            Operation.class,
            Function.class,
            AddressingAttribute.class,
            StandardAttribute.class,

            // BM API
            IBmNamespace.class,
            IBmTransaction.class,
            IBmModel.class,
            IBmEditingContext.class,
            IBmTask.class,

            // EDT platform
            IV8Project.class,
            IV8ProjectManager.class,
            ITopObjectFqnGenerator.class,
            IModelObjectFactory.class,

            // Eclipse resources
            IProject.class,
            IWorkspaceRoot.class
        );
    }

    @SuppressWarnings("nls")
    @Override
    public Collection<String> getImports()
    {
        // @formatter:off
        return List.of(
            // Core metadata factory
            "import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;",

            // Metadata objects
            "import com._1c.g5.v8.dt.metadata.mdclass.*;",

            // BM (Business Model) API
            "import com._1c.g5.v8.bm.core.*;",
            "import com._1c.g5.v8.bm.integration.*;",

            // EDT platform
            "import com._1c.g5.v8.dt.core.model.*;",
            "import com._1c.g5.v8.dt.core.naming.*;",
            "import com._1c.g5.v8.dt.core.platform.*;",

            // Eclipse resources
            "import org.eclipse.core.resources.*;",
            "import org.eclipse.core.runtime.*;"
        );
        // @formatter:on
    }

    // ========== Binding Description Builders ==========

    @SuppressWarnings("nls")
    private String buildMdFactoryDescription()
    {
        var desc = new StringBuilder();
        desc.append("## MdClassFactory - Metadata Object Factory\n\n");
        desc.append("Use `MdClassFactory.eINSTANCE` to create new metadata objects.\n\n");
        desc.append("### Creating Catalog\n");
        desc.append("```java\n");
        desc.append("// Create a new catalog object\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"MyCatalog\");\n");
        desc.append("catalog.getSynonym().setContent(\"ru\", \"Мой справочник\");\n");
        desc.append("```\n\n");

        desc.append("### Creating Document\n");
        desc.append("```java\n");
        desc.append("// Create a new document object\n");
        desc.append("Document document = mdFactory.createDocument();\n");
        desc.append("document.setName(\"MyDocument\");\n");
        desc.append("document.getSynonym().setContent(\"ru\", \"Мой документ\");\n");
        desc.append("document.setPostInChangeList(true);\n");
        desc.append("```\n\n");

        desc.append("### Creating Report\n");
        desc.append("```java\n");
        desc.append("// Create a new report object\n");
        desc.append("Report report = mdFactory.createReport();\n");
        desc.append("report.setName(\"MyReport\");\n");
        desc.append("report.getSynonym().setContent(\"ru\", \"Мой отчет\");\n");
        desc.append("```\n\n");

        desc.append("### Creating Information Register\n");
        desc.append("```java\n");
        desc.append("// Create a new information register\n");
        desc.append("InformationRegister register = mdFactory.createInformationRegister();\n");
        desc.append("register.setName(\"MyRegister\");\n");
        desc.append("register.setWriteMode(InformationRegisterWriteMode.OVERWRITE);\n");
        desc.append("```\n\n");

        desc.append("### Creating Accumulation Register\n");
        desc.append("```java\n");
        desc.append("// Create a new accumulation register\n");
        desc.append("AccumulationRegister register = mdFactory.createAccumulationRegister();\n");
        desc.append("register.setName(\"MyAccumulationRegister\");\n");
        desc.append("register.setRegisterType(AccumulationRegisterType.BALANCE);\n");
        desc.append("```\n\n");

        desc.append("### Creating Attribute\n");
        desc.append("```java\n");
        desc.append("// Create an attribute for catalog/document/register\n");
        desc.append("BasicFeature attribute = mdFactory.createBasicFeature();\n");
        desc.append("attribute.setName(\"MyAttribute\");\n");
        desc.append("attribute.setType(mdFactory.createTypeDescription(\"String.100\"));\n");
        desc.append("```\n\n");

        desc.append("### Available Factory Methods:\n");
        desc.append("- `createCatalog()` - Creates Catalog object\n");
        desc.append("- `createDocument()` - Creates Document object\n");
        desc.append("- `createReport()` - Creates Report object\n");
        desc.append("- `createInformationRegister()` - Creates InformationRegister\n");
        desc.append("- `createAccumulationRegister()` - Creates AccumulationRegister\n");
        desc.append("- `createBasicFeature()` - Creates BasicFeature (attribute)\n");
        desc.append("- `createCommonModule()` - Creates CommonModule\n");
        desc.append("- `createSubsystem()` - Creates Subsystem\n");
        desc.append("- `createEnum()` - Creates Enum\n");
        desc.append("- `createDataProcessor()` - Creates DataProcessor\n");
        desc.append("- `createConstant()` - Creates Constant\n");
        desc.append("- `createBusinessProcess()` - Creates BusinessProcess\n");
        desc.append("- `createTask()` - Creates Task\n");
        desc.append("- `createExchangePlan()` - Creates ExchangePlan\n");
        desc.append("- `createScheduledJob()` - Creates ScheduledJob\n");
        desc.append("- `createEventSubscription()` - Creates EventSubscription\n");
        desc.append("- `createRole()` - Creates Role\\n");
        desc.append("- `createExternalDataSource()` - Creates ExternalDataSource\\n");
        desc.append("- `createDocumentJournal()` - Creates DocumentJournal\\n");
        desc.append("- `createDocumentNumerator()` - Creates DocumentNumerator\n");
        desc.append("- `createSequence()` - Creates Sequence\n");
        desc.append("- `createRecalculation()` - Creates Recalculation\n");
        desc.append("- `createCalculationRegister()` - Creates CalculationRegister\n");
        desc.append("- `createAccountingRegister()` - Creates AccountingRegister\n");
        desc.append("- `createChartOfAccounts()` - Creates ChartOfAccounts\n");
        desc.append("- `createChartOfCalculationTypes()` - Creates ChartOfCalculationTypes\n");
        desc.append("- `createChartOfCharacteristicTypes()` - Creates ChartOfCharacteristicTypes\n");
        desc.append("- `createFilterCriterion()` - Creates FilterCriterion\n");
        desc.append("- `createFunctionalOption()` - Creates FunctionalOption\n");
        desc.append("- `createBot()` - Creates Bot\n");
        desc.append("- `createCommonPicture()` - Creates CommonPicture\n");
        desc.append("- `createCommonCommand()` - Creates CommonCommand\n");
        desc.append("- `createCommonAttribute()` - Creates CommonAttribute\n");
        desc.append("- And many more...\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildFqnGeneratorDescription()
    {
        var desc = new StringBuilder();
        desc.append("## ITopObjectFqnGenerator - FQN Generator\n\n");
        desc.append("Generates Fully Qualified Names for metadata objects.\n\n");
        desc.append("### Generate FQN for Catalog\n");
        desc.append("```java\n");
        desc.append("// Generate FQN for a catalog\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"MyCatalog\");\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("// Result: \"Catalog.MyCatalog\"\n");
        desc.append("```\n\n");

        desc.append("### Generate FQN for Document\n");
        desc.append("```java\n");
        desc.append("// Generate FQN for a document\n");
        desc.append("Document document = mdFactory.createDocument();\n");
        desc.append("document.setName(\"MyDocument\");\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();\n");
        desc.append("// Result: \"Document.MyDocument\"\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildModelFactoryDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IModelObjectFactory - Model Object Factory\n\n");
        desc.append("Creates metadata objects with proper initialization and context.\n\n");
        desc.append("### Create Catalog in Project\n");
        desc.append("```java\n");
        desc.append("// Get the active project\n");
        desc.append("IProject eclipseProject = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(eclipseProject);\n");
        desc.append("\n");
        desc.append("// Create catalog with project context\n");
        desc.append("Catalog catalog = modelFactory.create(Catalog.class, v8project);\n");
        desc.append("catalog.setName(\"MyCatalog\");\n");
        desc.append("```\n\n");

        desc.append("### Create Document in Configuration\\n");
        desc.append("```java\\n");
        desc.append("// Get configuration object\\n");
        desc.append("IProject eclipseProject = workspaceRoot.getProject(\\\"MyProject\\\");\\n");
        desc.append("IBmModel bmModel = modelManager.getModel(eclipseProject);\\n");
        desc.append("IV8Project v8project = projectManager.getProject(eclipseProject);\\n");
        desc.append("Configuration config = (Configuration) bmModel.getGlobalContext().getObject(\\\".Configuration\\\");\\n");
        desc.append("\\n");
        desc.append("// Create document with parent and version context\\n");
        desc.append("Document document = modelFactory.create(Document.class, config, v8project.getRuntimeVersion());\\n");
        desc.append("document.setName(\"MyDocument\");\n");
        desc.append("```\n\n");

        desc.append("### Fill Default References\n");
        desc.append("```java\n");
        desc.append("// After creating an object, fill default references\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"MyCatalog\");\n");
        desc.append("modelFactory.fillDefaultReferences(catalog);\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildProjectManagerDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IV8ProjectManager - Project Manager\n\n");
        desc.append("Manages 1C projects in the workspace.\n\n");
        desc.append("### Get Project by Name\n");
        desc.append("```java\n");
        desc.append("// Get project by name\n");
        desc.append("IProject eclipseProject = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(eclipseProject);\n");
        desc.append("if (v8project != null) {\n");
        desc.append("    System.out.println(\"Project found: \" + v8project.getName());\n");
        desc.append("    System.out.println(\"Runtime version: \" + v8project.getRuntimeVersion());\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### Get All Projects\n");
        desc.append("```java\n");
        desc.append("// Get all V8 projects in workspace\n");
        desc.append("IProject[] eclipseProjects = workspaceRoot.getProjects();\n");
        desc.append("for (IProject project : eclipseProjects) {\n");
        desc.append("    IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("    if (v8project != null) {\n");
        desc.append("        System.out.println(\"V8 Project: \" + v8project.getName());\n");
        desc.append("    }\n");
        desc.append("}\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildModelManagerDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IBmModelManager - BM Model Manager\n\n");
        desc.append("Provides access to Business Model (BM) for 1C projects.\n\n");
        desc.append("### Get BM Model for Project\n");
        desc.append("```java\n");
        desc.append("// Get BM model for a project\n");
        desc.append("IProject eclipseProject = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(eclipseProject);\n");
        desc.append("if (bmModel != null) {\n");
        desc.append("    System.out.println(\"BM Model loaded\");\n");
        desc.append("    IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### Get Global Editing Context\n");
        desc.append("```java\n");
        desc.append("// Get global editing context for read-only operations\n");
        desc.append("IProject eclipseProject = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(eclipseProject);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("// Execute read-only task\n");
        desc.append("IBmObject result = globalContext.executeReadonlyTask(new IBmTask<IBmObject>() {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        return transaction.getObjectById(someObjectId);\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"taskId\"; }\n");
        desc.append("    public String getName() { return \"Task Name\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildResourceLookupDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IResourceLookup - Resource Lookup\n\n");
        desc.append("Looks up Eclipse resources (IProject, IFile) for 1C objects.\n\n");
        desc.append("### Get Project from Metadata Object\n");
        desc.append("```java\n");
        desc.append("// Get Eclipse IProject from metadata object\n");
        desc.append("Catalog catalog = ...; // existing catalog object\n");
        desc.append("IProject project = resourceLookup.getProject(catalog);\n");
        desc.append("if (project != null) {\n");
        desc.append("    System.out.println(\"Project: \" + project.getName());\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### Get File from Metadata Object\n");
        desc.append("```java\n");
        desc.append("// Get Eclipse IFile from metadata object\n");
        desc.append("Catalog catalog = ...; // existing catalog object\n");
        desc.append("IFile file = resourceLookup.getFile(catalog);\n");
        desc.append("if (file != null) {\n");
        desc.append("    System.out.println(\"File: \" + file.getFullPath());\n");
        desc.append("}\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildWorkspaceRootDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IWorkspaceRoot - Workspace Root\n\n");
        desc.append("Eclipse workspace root for accessing all projects.\n\n");
        desc.append("### Get Project by Name\n");
        desc.append("```java\n");
        desc.append("// Get project by name\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("if (project.exists()) {\n");
        desc.append("    System.out.println(\"Project exists: \" + project.getName());\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### Get All Projects\n");
        desc.append("```java\n");
        desc.append("// Get all projects in workspace\n");
        desc.append("IProject[] projects = workspaceRoot.getProjects();\n");
        desc.append("for (IProject project : projects) {\n");
        desc.append("    System.out.println(\"Project: \" + project.getName());\n");
        desc.append("}\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildMetadataTypesAndFeatures()
    {
        var desc = new StringBuilder();
        desc.append("## Metadata Types and Features\n\n");
        desc.append("### Overview of 1C:Enterprise Metadata Types\n\n");

        desc.append("#### 1. **Catalogs (Справочники)**\n");
        desc.append("**Purpose**: Store hierarchical or tabular data (goods, employees, clients)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Hierarchical structure (subdirectories)\n");
        desc.append("- Support for subordination (groups and items)\n");
        desc.append("- Can have multiple tabular parts\n");
        desc.append("- Automatic code/name numbering\n");
        desc.append("- Forms: list form, item form, choice form\n\n");
        desc.append("**Key Properties:**\n");
        desc.append("- `setHierarchyType(HierarchyType.HIERARCHY | HIERarchY | GROUPS)` - Hierarchy mode\n");
        desc.append("- `setHierarchyCatalog()` - Parent catalog for hierarchy\n");
        desc.append("- `setCodeLength()`, `setNameLength()` - Code/name lengths\n");
        desc.append("- `setDescriptionLength()` - Description length\n");
        desc.append("- `setCodeSeries(CodeSeries.BY_PARENT | ACROSS_PARENTS)` - Code series\n\n");

        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.setHierarchyType(HierarchyType.GROUPS);\n");
        desc.append("catalog.setCodeLength(9);\n");
        desc.append("catalog.setNameLength(150);\n");
        desc.append("```\\n\\n");

        desc.append("#### 2. **Documents (Документы)**\n");
        desc.append("**Purpose**: Store business transactions (sales, purchases, payments)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Movement registration (movements in registers)\n");
        desc.append("- Post in change list control\n");
        desc.append("- Numbering with periodicity\n");
        desc.append("- Can be posted or unposted\n");
        desc.append("- Multiple tabular parts\n");
        desc.append("- Document journals for grouping\n\n");
        desc.append("**Key Properties:**\n");
        desc.append("- `setPostInChangeList(true)` - Post in change list\n");
        desc.append("- `setNumbering(Numbering.NONUMBER | PERIOD | NONUMBER_PERIOD)` - Numbering mode\n");
        desc.append("- `setNumberPeriodicity(NumberingPeriodicity.DAY | MONTH | QUARTER | YEAR)` - Periodicity\n");
        desc.append("- `setNumberLength()` - Number length\n");
        desc.append("- `setDateInNumbering()` - Include date in number\n");
        desc.append("- `setJournal()` - Document journal\n\n");

        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("Document document = mdFactory.createDocument();\n");
        desc.append("document.setName(\"SalesInvoice\");\n");
        desc.append("document.setPostInChangeList(true);\n");
        desc.append("document.setNumbering(Numbering.PERIOD);\n");
        desc.append("document.setNumberPeriodicity(NumberingPeriodicity.MONTH);\n");
        desc.append("document.setNumberLength(10);\n");
        desc.append("```\\n\\n");

        desc.append("#### 3. **Reports (Отчеты)**\n");
        desc.append("**Purpose**: Output and analysis data (SalesReport, InventoryReport)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Based on Data Composition System (СКД)\n");
        desc.append("- Optional connection to forms\n");
        desc.append("- Can have templates\n");
        desc.append("- Parameters for filtering\n\n");
        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("Report report = mdFactory.createReport();\n");
        desc.append("report.setName(\"SalesByDate\");\n");
        desc.append("```\\n\\n");

        desc.append("#### 4. **Information Registers (РегистрыСведений)**\n");
        desc.append("**Purpose**: Store current state data (prices, settings)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Store current state at specific moment\n");
        desc.append("- Write modes: overwrite, append, subordinated\n");
        desc.append("- Periodicity: within day, day, month, quarter, year, position\n");
        desc.append("- Recorders: documents that write to register\n");
        desc.append("- Dimensions and resources\n\n");
        desc.append("**Key Properties:**\n");
        desc.append("- `setWriteMode(InformationRegisterWriteMode.OVERWRITE | APPEND | SUBORDINATE)` - Write mode\n");
        desc.append("- `setInformationRegisterPeriodicity()` - Periodicity\n");
        desc.append("- `setStandardAttributes()` - Standard attributes (Active, LineNumber)\n\n");

        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("InformationRegister register = mdFactory.createInformationRegister();\n");
        desc.append("register.setName(\"Prices\");\n");
        desc.append("register.setWriteMode(InformationRegisterWriteMode.OVERWRITE);\n");
        desc.append("register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.POSITION);\n");
        desc.append("```\\n\\n");

        desc.append("#### 5. **Accumulation Registers (РегистрыНакопления)**\n");
        desc.append("**Purpose**: Track balances and turnovers (goods, cash)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Two types: Balance (balances) and Turnovers (movements)\n");
        desc.append("- Write mode: accumulate, overwrite\n");
        desc.append("- Recorders: documents that write to register\n");
        desc.append("- Splitting for complex calculations\n");
        desc.append("- Dimensions, resources, attributes\n\n");
        desc.append("**Key Properties:**\n");
        desc.append("- `setRegisterType(AccumulationRegisterType.BALANCE | TURNOVERS)` - Register type\n");
        desc.append("- `setRegisterWrite(AccumulationRegisterWriteMode.ACCUMULATE | OVERWRITE)` - Write mode\n\n");

        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("AccumulationRegister register = mdFactory.createAccumulationRegister();\n");
        desc.append("register.setName(\"GoodsInStock\");\n");
        desc.append("register.setRegisterType(AccumulationRegisterType.BALANCE);\n");
        desc.append("register.setRegisterWrite(AccumulationRegisterWriteMode.ACCUMULATE);\n");
        desc.append("```\\n\\n");

        desc.append("#### 6. **Common Modules (ОбщиеМодули)**\n");
        desc.append("**Purpose**: Store common functions and procedures\n\n");
        desc.append("**Features:**\n");
        desc.append("- Execution contexts: client, server, external connection\n");
        desc.append("- Can be global (accessible everywhere)\n");
        desc.append("- Can be privileged (bypass rights)\n");
        desc.append("- Can be for forms only\n");
        desc.append("- Can be cached for performance\n\n");
        desc.append("**Key Properties:**\n");
        desc.append("- `setClientManagedApplication(true)` - Available on client\n");
        desc.append("- `setServer(true)` - Available on server\n");
        desc.append("- `setExternalConnection(true)` - Available in external connection\n");
        desc.append("- `setGlobal(true)` - Global (accessible without prefix)\n");
        desc.append("- `setPrivileged(true)` - Privileged (bypass rights)\n");
        desc.append("- `setClientOrdinaryApplication(true)` - Available in managed client\n\n");

        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("CommonModule module = mdFactory.createCommonModule();\n");
        desc.append("module.setName(\"GeneralPurpose\");\n");
        desc.append("module.setServer(true);\n");
        desc.append("module.setClientManagedApplication(true);\n");
        desc.append("module.setGlobal(true);\n");
        desc.append("```\\n\\n");

        desc.append("#### 7. **Enumerations (Перечисления)**\n");
        desc.append("**Purpose**: Store predefined values (order status, document type)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Fixed set of values\n");
        desc.append("- Can be hierarchical\n");
        desc.append("- Can have synonyms\n\n");
        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("Enum enumType = mdFactory.createEnum();\n");
        desc.append("enumType.setName(\"OrderStatus\");\n");
        desc.append("\\n");
        desc.append("// Create enum values\n");
        desc.append("EnumValue value1 = mdFactory.createEnumValue();\n");
        desc.append("value1.setName(\"New\");\n");
        desc.append("value1.getSynonym().setContent(\"ru\", \"Новый\");\n");
        desc.append("enumType.getValues().add(value1);\n");
        desc.append("```\\n\\n");

        desc.append("#### 8. **Subsystems (Подсистемы)**\n");
        desc.append("**Purpose**: Organize metadata into functional areas\n\n");
        desc.append("**Features:**\n");
        desc.append("- Hierarchical structure\n");
        desc.append("- Contain metadata objects\n");
        desc.append("- Have command interfaces\n\n");
        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("Subsystem subsystem = mdFactory.createSubsystem();\n");
        desc.append("subsystem.setName(\"Inventory\");\n");
        desc.append("subsystem.getSynonym().setContent(\"ru\", \"Склад\");\n");
        desc.append("```\\n\\n");

        desc.append("#### 9. **Constants (Константы)**\n");
        desc.append("**Purpose**: Store system-wide values (tax rate, company name)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Single value per constant\n");
        desc.append("- Can have different types\n\n");
        desc.append("**Example:**\n");
        desc.append("```java\n");
        desc.append("Constant constant = mdFactory.createConstant();\n");
        desc.append("constant.setName(\"DefaultTaxRate\");\n");
        desc.append("constant.setType(mdFactory.createTypeDescription(\"Number.10:2\"));\n");
        desc.append("```\\n\\n");

        desc.append("#### 10. **Accounting Registers (РегистрыБухгалтерии)**\n");
        desc.append("**Purpose**: Double-entry bookkeeping\n\n");
        desc.append("**Features:**\n");
        desc.append("- Chart of accounts\n");
        desc.append("- Debits and credits\n");
        desc.append("- Accounting flags\n\n");

        desc.append("#### 11. **Calculation Registers (РегистрыРасчета)**\n");
        desc.append("**Purpose**: Payroll and other calculations\n\n");
        desc.append("**Features:**\n");
        desc.append("- Base periods and periods\n");
        desc.append("- Recalculation registers\n");
        desc.append("- Chart of calculation types\n\n");

        desc.append("#### 12. **Document Journals (ЖурналыДокументов)**\n");
        desc.append("**Purpose**: Group documents for unified viewing\n\n");
        desc.append("**Features:**\n");
        desc.append("- Multiple documents\n");
        desc.append("- Common list form\n\n");

        desc.append("#### 13. **Business Processes (БизнесПроцессы)**\n");
        desc.append("**Purpose**: Workflow automation\n\n");
        desc.append("**Features:**\n");
        desc.append("- Tasks and routes\n");
        desc.append("- Interprocess points\n\n");

        desc.append("#### 14. **Tasks (Задачи)**\n");
        desc.append("**Purpose**: Task management\n\n");
        desc.append("**Features:**\n");
        desc.append("- Execution and addresses\n");
        desc.append("- Business process integration\n\n");

        desc.append("#### 15. **Data Processors (Обработки)**\n");
        desc.append("**Purpose**: Interactive data processing tools\n\n");
        desc.append("**Features:**\n");
        desc.append("- Forms and templates\n");
        desc.append("- Can be called from anywhere\n\n");

        desc.append("#### 16. **Common Forms (ОбщиеФормы)**\n");
        desc.append("**Purpose**: Reusable forms\n\n");
        desc.append("**Features:**\n");
        desc.append("- Can be called programmatically\n");
        desc.append("- Shared across metadata\n\n");

        desc.append("#### 17. **Common Templates (ОбщиеМакеты)**\n");
        desc.append("**Purpose**: Reusable templates (reports, data processors)\n\n");
        desc.append("**Features:**\n");
        desc.append("- Data composition schemas\n");
        desc.append("- Print forms\n");
        desc.append("- Excel spreadsheets\n\n");

        desc.append("#### 18. **Functional Options (ФункциональныеОпции)**\n");
        desc.append("**Purpose**: Conditional feature enabling\n\n");
        desc.append("**Features:**\n");
        desc.append("- Enable/disable metadata\n");
        desc.append("- Based on conditions\n\n");

        desc.append("#### 19. **Scheduled Jobs (РегламентныеЗадания)**\n");
        desc.append("**Purpose**: Scheduled background tasks\n\n");
        desc.append("**Features:**\n");
        desc.append("- Schedule configuration\n");
        desc.append("- Methods to call\n\n");

        desc.append("#### 20. **Event Subscriptions (ПодпискаНаСобытие)**\n");
        desc.append("**Purpose**: React to metadata events\n\n");
        desc.append("**Features:**\n");
        desc.append("- Before write, after write, on deletion\n");
        desc.append("- Specific handlers\n\n");

        desc.append("#### 21. **Roles (Роли)**\n");
        desc.append("**Purpose**: Access control\n\n");
        desc.append("**Features:**\n");
        desc.append("- Rights assignment\n");
        desc.append("- Role separation\n\n");

        desc.append("#### 22. **External Data Sources (ВнешниеИсточникиДанных)**\n");
        desc.append("**Purpose**: Connect to external databases\n\n");
        desc.append("**Features:**\n");
        desc.append("- Tables, views, queries\n");
        desc.append("- SQL integration\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCompleteExamples()
    {
        var desc = new StringBuilder();
        desc.append("## Complete Examples: Create, Edit, Delete Metadata\n\n");
        desc.append("### CREATE: Full Catalog with Attributes and Tabular Part\n\n");
        desc.append("```java\n");
        desc.append("// Step 1: Get project and context\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n\n");
        desc.append("// Step 2: Execute in transaction\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Create Catalog\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Step 3: Get configuration\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n\n");
        desc.append("        // Step 4: Create catalog\n");
        desc.append("        Catalog catalog = modelFactory.create(Catalog.class, config, v8project.getRuntimeVersion());\n");
        desc.append("        catalog.setName(\"Products\");\n");
        desc.append("        catalog.getSynonym().setContent(\"ru\", \"Товары\");\n");
        desc.append("        catalog.setHierarchyType(HierarchyType.GROUPS);\n");
        desc.append("        catalog.setCodeLength(9);\n");
        desc.append("        catalog.setNameLength(150);\n");
        desc.append("        catalog.setCodeSeries(CodeSeries.BY_PARENT);\n");
        desc.append("        catalog.setDescriptionLength(100);\n");
        desc.append("\n");
        desc.append("        // Step 5: Create attributes\n");
        desc.append("        BasicFeature attributeCode = mdFactory.createBasicFeature();\n");
        desc.append("        attributeCode.setName(\"ProductCode\");\n");
        desc.append("        attributeCode.getSynonym().setContent(\"ru\", \"Артикул\");\n");
        desc.append("        attributeCode.setType(mdFactory.createTypeDescription(\"String.50\"));\n");
        desc.append("        catalog.getAttributes().add(attributeCode);\n");
        desc.append("\n");
        desc.append("        BasicFeature attributePrice = mdFactory.createBasicFeature();\n");
        desc.append("        attributePrice.setName(\"Price\");\n");
        desc.append("        attributePrice.getSynonym().setContent(\"ru\", \"Цена\");\n");
        desc.append("        attributePrice.setType(mdFactory.createTypeDescription(\"Number.15:2\"));\n");
        desc.append("        catalog.getAttributes().add(attributePrice);\n");
        desc.append("\n");
        desc.append("        BasicFeature attributeWeight = mdFactory.createBasicFeature();\n");
        desc.append("        attributeWeight.setName(\"Weight\");\n");
        desc.append("        attributeWeight.getSynonym().setContent(\"ru\", \"Вес\");\n");
        desc.append("        attributeWeight.setType(mdFactory.createTypeDescription(\"Number.10:3\"));\n");
        desc.append("        catalog.getAttributes().add(attributeWeight);\n");
        desc.append("\n");
        desc.append("        // Step 6: Create tabular part\n");
        desc.append("        CatalogTabularSection tabularPart = mdFactory.createCatalogTabularSection();\n");
        desc.append("        tabularPart.setName(\"Specifications\");\n");
        desc.append("        tabularPart.getSynonym().setContent(\"ru\", \"Спецификации\");\n");
        desc.append("\n");
        desc.append("        // Add attributes to tabular part\n");
        desc.append("        BasicFeature tpAttribute1 = mdFactory.createBasicFeature();\n");
        desc.append("        tpAttribute1.setName(\"Specification\");\n");
        desc.append("        tpAttribute1.getSynonym().setContent(\"ru\", \"Спецификация\");\n");
        desc.append("        tpAttribute1.setType(mdFactory.createTypeDescription(\"String.100\"));\n");
        desc.append("        tabularPart.getAttributes().add(tpAttribute1);\n");
        desc.append("\n");
        desc.append("        BasicFeature tpAttribute2 = mdFactory.createBasicFeature();\n");
        desc.append("        tpAttribute2.setName(\"Quantity\");\n");
        desc.append("        tpAttribute2.getSynonym().setContent(\"ru\", \"Количество\");\n");
        desc.append("        tpAttribute2.setType(mdFactory.createTypeDescription(\"Number.10:0\"));\n");
        desc.append("        tabularPart.getAttributes().add(tpAttribute2);\n");
        desc.append("\n");
        desc.append("        catalog.getTabularSections().add(tabularPart);\n");
        desc.append("\n");
        desc.append("        // Step 7: Generate FQN and attach\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("            catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("\n");
        desc.append("        // Step 8: Link to parent\n");
        desc.append("        config.getCatalogs().add(catalog);\n");
        desc.append("\n");
        desc.append("        // Step 9: Fill defaults\n");
        desc.append("        modelFactory.fillDefaultReferences(catalog);\n");
        desc.append("\n");
        desc.append("        return catalog;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"createCatalogTask\"; }\n");
        desc.append("    public String getName() { return \"Create Catalog Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### CREATE: Full Document with Movement\n\n");
        desc.append("```java\n");
        desc.append("// Create sales invoice document\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Create Document\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n\n");
        desc.append("        // Get existing catalog for reference\n");
        desc.append("        Catalog productsCatalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n\n");
        desc.append("        // Create document\n");
        desc.append("        Document document = modelFactory.create(Document.class, config, v8project.getRuntimeVersion());\n");
        desc.append("        document.setName(\"SalesInvoice\");\n");
        desc.append("        document.getSynonym().setContent(\"ru\", \"РеализацияТоваров\");\n");
        desc.append("        document.setPostInChangeList(true);\n");
        desc.append("        document.setNumbering(Numbering.PERIOD);\n");
        desc.append("        document.setNumberPeriodicity(NumberingPeriodicity.MONTH);\n");
        desc.append("        document.setNumberLength(10);\n");
        desc.append("        document.setDateInNumbering(true);\n");
        desc.append("        document.setUseObjectForPresentation(true);\n");
        desc.append("\n");
        desc.append("        // Create attributes\n");
        desc.append("        BasicFeature attributeClient = mdFactory.createBasicFeature();\n");
        desc.append("        attributeClient.setName(\"Client\");\n");
        desc.append("        attributeClient.getSynonym().setContent(\"ru\", \"Клиент\");\n");
        desc.append("        attributeClient.setType(mdFactory.createCatalogRef(productsCatalog));\n");
        desc.append("        document.getAttributes().add(attributeClient);\n");
        desc.append("\n");
        desc.append("        BasicFeature attributeDate = mdFactory.createBasicFeature();\n");
        desc.append("        attributeDate.setName(\"InvoiceDate\");\n");
        desc.append("        attributeDate.getSynonym().setContent(\"ru\", \"Дата\");\n");
        desc.append("        attributeDate.setType(mdFactory.createTypeDescription(\"Date\"));\n");
        desc.append("        document.getAttributes().add(attributeDate);\n");
        desc.append("\n");
        desc.append("        BasicFeature attributeSum = mdFactory.createBasicFeature();\n");
        desc.append("        attributeSum.setName(\"TotalSum\");\n");
        desc.append("        attributeSum.getSynonym().setContent(\"ru\", \"Сумма\");\n");
        desc.append("        attributeSum.setType(mdFactory.createTypeDescription(\"Number.15:2\"));\n");
        desc.append("        document.getAttributes().add(attributeSum);\n");
        desc.append("\n");
        desc.append("        // Create tabular part\n");
        desc.append("        DocumentTabularSection tabularPart = mdFactory.createDocumentTabularSection();\n");
        desc.append("        tabularPart.setName(\"Products\");\n");
        desc.append("        tabularPart.getSynonym().setContent(\"ru\", \"Товары\");\n");
        desc.append("\n");
        desc.append("        BasicFeature tpProduct = mdFactory.createBasicFeature();\n");
        desc.append("        tpProduct.setName(\"Product\");\n");
        desc.append("        tpProduct.getSynonym().setContent(\"ru\", \"Товар\");\n");
        desc.append("        tpProduct.setType(mdFactory.createCatalogRef(productsCatalog));\n");
        desc.append("        tabularPart.getAttributes().add(tpProduct);\n");
        desc.append("\n");
        desc.append("        BasicFeature tpQuantity = mdFactory.createBasicFeature();\n");
        desc.append("        tpQuantity.setName(\"Quantity\");\n");
        desc.append("        tpQuantity.getSynonym().setContent(\"ru\", \"Количество\");\n");
        desc.append("        tpQuantity.setType(mdFactory.createTypeDescription(\"Number.10:3\"));\n");
        desc.append("        tabularPart.getAttributes().add(tpQuantity);\n");
        desc.append("\n");
        desc.append("        BasicFeature tpPrice = mdFactory.createBasicFeature();\n");
        desc.append("        tpPrice.setName(\"Price\");\n");
        desc.append("        tpPrice.getSynonym().setContent(\"ru\", \"Цена\");\n");
        desc.append("        tpPrice.setType(mdFactory.createTypeDescription(\"Number.15:2\"));\n");
        desc.append("        tabularPart.getAttributes().add(tpPrice);\n");
        desc.append("\n");
        desc.append("        BasicFeature tpSum = mdFactory.createBasicFeature();\n");
        desc.append("        tpSum.setName(\"Sum\");\n");
        desc.append("        tpSum.getSynonym().setContent(\"ru\", \"Сумма\");\n");
        desc.append("        tpSum.setType(mdFactory.createTypeDescription(\"Number.15:2\"));\n");
        desc.append("        tabularPart.getAttributes().add(tpSum);\n");
        desc.append("\n");
        desc.append("        document.getTabularSections().add(tabularPart);\n");
        desc.append("\n");
        desc.append("        // Attach and link\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("            document.eClass(), document.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)document, fqn);\n");
        desc.append("        config.getDocuments().add(document);\n");
        desc.append("        modelFactory.fillDefaultReferences(document);\n");
        desc.append("\n");
        desc.append("        return document;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"createDocumentTask\"; }\n");
        desc.append("    public String getName() { return \"Create Document Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### EDIT: Modify Existing Metadata\n\n");
        desc.append("```java\n");
        desc.append("// Modify existing catalog\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Edit Catalog\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Get existing catalog by FQN\n");
        desc.append("        Catalog catalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n\n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            // Modify properties\n");
        desc.append("            catalog.setCodeLength(12);  // Change from 9 to 12\n");
        desc.append("            catalog.setNameLength(200);  // Change from 150 to 200\n");
        desc.append("            catalog.getComment().setContent(\"ru\", \"Модифицирован программно\");\n");
        desc.append("\n");
        desc.append("            // Add new attribute\n");
        desc.append("            BasicFeature newAttribute = mdFactory.createBasicFeature();\n");
        desc.append("            newAttribute.setName(\"Barcode\");\n");
        desc.append("            newAttribute.getSynonym().setContent(\"ru\", \"Штрихкод\");\n");
        desc.append("            newAttribute.setType(mdFactory.createTypeDescription(\"String.13\"));\n");
        desc.append("            catalog.getAttributes().add(newAttribute);\n");
        desc.append("\n");
        desc.append("            // Modify existing attribute\n");
        desc.append("            for (BasicFeature attr : catalog.getAttributes()) {\n");
        desc.append("                if (\"Price\".equals(attr.getName())) {\n");
        desc.append("                    attr.setType(mdFactory.createTypeDescription(\"Number.18:2\"));\n");
        desc.append("                }\n");
        desc.append("            }\n");
        desc.append("\n");
        desc.append("            // Remove attribute by name\n");
        desc.append("            catalog.getAttributes().removeIf(attr -> \"Weight\".equals(attr.getName()));\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        return catalog;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"editCatalogTask\"; }\n");
        desc.append("    public String getName() { return \"Edit Catalog Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### EDIT: Update Document Properties\n\n");
        desc.append("```java\n");
        desc.append("// Update document numbering settings\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Edit Document\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Document document = (Document) transaction.getTopObjectByFqn(\"Document.SalesInvoice\");\n\n");
        desc.append("        if (document != null) {\n");
        desc.append("            // Change numbering\n");
        desc.append("            document.setNumberLength(12);  // Increase length\n");
        desc.append("            document.setDateInNumbering(false);  // Remove date from number\n");
        desc.append("\n");
        desc.append("            // Add new tabular part\n");
        desc.append("            DocumentTabularSection newTP = mdFactory.createDocumentTabularSection();\n");
        desc.append("            newTP.setName(\"Services\");\n");
        desc.append("            newTP.getSynonym().setContent(\"ru\", \"Услуги\");\n");
        desc.append("            document.getTabularSections().add(newTP);\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        return document;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"editDocumentTask\"; }\n");
        desc.append("    public String getName() { return \"Edit Document Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### DELETE: Remove Metadata Object\n\n");
        desc.append("```java\n");
        desc.append("// Delete a catalog\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Delete Catalog\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Get configuration\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n\n");
        desc.append("        // Get catalog to delete\n");
        desc.append("        Catalog catalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n\n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            // Detach from BM\n");
        desc.append("            transaction.detachTopObject((IBmObject)catalog);\n");
        desc.append("\n");
        desc.append("            // Remove from configuration\n");
        desc.append("            config.getCatalogs().remove(catalog);\n");
        desc.append("\n");
        desc.append("            System.out.println(\"Deleted catalog: \" + catalog.getName());\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"deleteCatalogTask\"; }\n");
        desc.append("    public String getName() { return \"Delete Catalog Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### DELETE: Remove Document\n\n");
        desc.append("```java\n");
        desc.append("// Delete a document\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Delete Document\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        Document document = (Document) transaction.getTopObjectByFqn(\"Document.SalesInvoice\");\n\n");
        desc.append("        if (document != null) {\n");
        desc.append("            // Detach and remove\n");
        desc.append("            transaction.detachTopObject((IBmObject)document);\n");
        desc.append("            config.getDocuments().remove(document);\n");
        desc.append("\n");
        desc.append("            System.out.println(\"Deleted document: \" + document.getName());\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"deleteDocumentTask\"; }\n");
        desc.append("    public String getName() { return \"Delete Document Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### CREATE: Information Register with Dimensions\n\n");
        desc.append("```java\n");
        desc.append("// Create information register for prices\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Create Register\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        Catalog productsCatalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n\n");
        desc.append("        InformationRegister register = mdFactory.createInformationRegister();\n");
        desc.append("        register.setName(\"Prices\");\n");
        desc.append("        register.getSynonym().setContent(\"ru\", \"Цены\");\n");
        desc.append("        register.setWriteMode(InformationRegisterWriteMode.OVERWRITE);\n");
        desc.append("        register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.POSITION);\n");
        desc.append("\n");
        desc.append("        // Create dimension (Product)\n");
        desc.append("        Dimension dimensionProduct = mdFactory.createDimension();\n");
        desc.append("        dimensionProduct.setName(\"Product\");\n");
        desc.append("        dimensionProduct.getSynonym().setContent(\"ru\", \"Товар\");\n");
        desc.append("        dimensionProduct.setType(mdFactory.createCatalogRef(productsCatalog));\n");
        desc.append("        dimensionProduct.setBalance(true);\n");
        desc.append("        register.getDimensions().add(dimensionProduct);\n");
        desc.append("\n");
        desc.append("        // Create dimension (Date)\n");
        desc.append("        Dimension dimensionDate = mdFactory.createDimension();\n");
        desc.append("        dimensionDate.setName(\"Date\");\n");
        desc.append("        dimensionDate.getSynonym().setContent(\"ru\", \"Дата\");\n");
        desc.append("        dimensionDate.setType(mdFactory.createTypeDescription(\"Date\"));\n");
        desc.append("        dimensionDate.setBalance(true);\n");
        desc.append("        register.getDimensions().add(dimensionDate);\n");
        desc.append("\n");
        desc.append("        // Create resource (Price)\n");
        desc.append("        BasicFeature resourcePrice = mdFactory.createBasicFeature();\n");
        desc.append("        resourcePrice.setName(\"Price\");\n");
        desc.append("        resourcePrice.getSynonym().setContent(\"ru\", \"Цена\");\n");
        desc.append("        resourcePrice.setType(mdFactory.createTypeDescription(\"Number.15:2\"));\n");
        desc.append("        register.getResources().add(resourcePrice);\n");
        desc.append("\n");
        desc.append("        // Attach and link\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("            register.eClass(), register.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)register, fqn);\n");
        desc.append("        config.getInformationRegisters().add(register);\n");
        desc.append("        modelFactory.fillDefaultReferences(register);\n");
        desc.append("\n");
        desc.append("        return register;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"createRegisterTask\"; }\n");
        desc.append("    public String getName() { return \"Create Register Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### CREATE: Accumulation Register\n\n");
        desc.append("```java\n");
        desc.append("// Create accumulation register for goods balance\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Create Accum Register\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        Catalog productsCatalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("        Catalog warehousesCatalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Warehouses\");\n\n");
        desc.append("        AccumulationRegister register = mdFactory.createAccumulationRegister();\n");
        desc.append("        register.setName(\"GoodsInStock\");\n");
        desc.append("        register.getSynonym().setContent(\"ru\", \"ТоварыНаСкладах\");\n");
        desc.append("        register.setRegisterType(AccumulationRegisterType.BALANCE);\n");
        desc.append("        register.setRegisterWrite(AccumulationRegisterWriteMode.ACCUMULATE);\n");
        desc.append("\n");
        desc.append("        // Dimension: Product\n");
        desc.append("        Dimension dimProduct = mdFactory.createDimension();\n");
        desc.append("        dimProduct.setName(\"Product\");\n");
        desc.append("        dimProduct.getSynonym().setContent(\"ru\", \"Товар\");\n");
        desc.append("        dimProduct.setType(mdFactory.createCatalogRef(productsCatalog));\n");
        desc.append("        dimProduct.setBalance(true);\n");
        desc.append("        register.getDimensions().add(dimProduct);\n");
        desc.append("\n");
        desc.append("        // Dimension: Warehouse\n");
        desc.append("        Dimension dimWarehouse = mdFactory.createDimension();\n");
        desc.append("        dimWarehouse.setName(\"Warehouse\");\n");
        desc.append("        dimWarehouse.getSynonym().setContent(\"ru\", \"Склад\");\n");
        desc.append("        dimWarehouse.setType(mdFactory.createCatalogRef(warehousesCatalog));\n");
        desc.append("        dimWarehouse.setBalance(true);\n");
        desc.append("        register.getDimensions().add(dimWarehouse);\n");
        desc.append("\n");
        desc.append("        // Resource: Quantity\n");
        desc.append("        BasicFeature resQuantity = mdFactory.createBasicFeature();\n");
        desc.append("        resQuantity.setName(\"Quantity\");\n");
        desc.append("        resQuantity.getSynonym().setContent(\"ru\", \"Количество\");\n");
        desc.append("        resQuantity.setType(mdFactory.createTypeDescription(\"Number.15:3\"));\n");
        desc.append("        register.getResources().add(resQuantity);\n");
        desc.append("\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("            register.eClass(), register.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)register, fqn);\n");
        desc.append("        config.getAccumulationRegisters().add(register);\n");
        desc.append("        modelFactory.fillDefaultReferences(register);\n");
        desc.append("\n");
        desc.append("        return register;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"createAccumRegisterTask\"; }\n");
        desc.append("    public String getName() { return \"Create Accumulation Register Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### CREATE: Subsystem\n\n");
        desc.append("```java\n");
        desc.append("// Create subsystem\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Create Subsystem\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n\n");
        desc.append("        Subsystem subsystem = mdFactory.createSubsystem();\n");
        desc.append("        subsystem.setName(\"Inventory\");\n");
        desc.append("        subsystem.getSynonym().setContent(\"ru\", \"Склад\");\n");
        desc.append("        subsystem.getComment().setContent(\"ru\", \"Учет товаров и складов\");\n");
        desc.append("\n");
        desc.append("        // Add existing objects to subsystem\n");
        desc.append("        Catalog products = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("        Catalog warehouses = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Warehouses\");\n");
        desc.append("        Document salesInvoice = (Document) transaction.getTopObjectByFqn(\"Document.SalesInvoice\");\n");
        desc.append("        AccumulationRegister goodsInStock = (AccumulationRegister) transaction.getTopObjectByFqn(\"AccumulationRegister.GoodsInStock\");\n");
        desc.append("\n");
        desc.append("        if (products != null) subsystem.getMetadata().add(products);\n");
        desc.append("        if (warehouses != null) subsystem.getMetadata().add(warehouses);\n");
        desc.append("        if (salesInvoice != null) subsystem.getMetadata().add(salesInvoice);\n");
        desc.append("        if (goodsInStock != null) subsystem.getMetadata().add(goodsInStock);\n");
        desc.append("\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("            subsystem.eClass(), subsystem.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)subsystem, fqn);\n");
        desc.append("        config.getSubsystems().add(subsystem);\n");
        desc.append("        modelFactory.fillDefaultReferences(subsystem);\n");
        desc.append("\n");
        desc.append("        return subsystem;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"createSubsystemTask\"; }\n");
        desc.append("    public String getName() { return \"Create Subsystem Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("### CREATE: Enum\n\n");
        desc.append("```java\n");
        desc.append("// Create enum for order status\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Create Enum\") {\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n\n");
        desc.append("        Enum enumType = mdFactory.createEnum();\n");
        desc.append("        enumType.setName(\"OrderStatus\");\n");
        desc.append("        enumType.getSynonym().setContent(\"ru\", \"СтатусЗаказа\");\n");
        desc.append("\n");
        desc.append("        // Create enum values\n");
        desc.append("        EnumValue value1 = mdFactory.createEnumValue();\n");
        desc.append("        value1.setName(\"New\");\n");
        desc.append("        value1.getSynonym().setContent(\"ru\", \"Новый\");\n");
        desc.append("        enumType.getValues().add(value1);\n");
        desc.append("\n");
        desc.append("        EnumValue value2 = mdFactory.createEnumValue();\n");
        desc.append("        value2.setName(\"InProcessing\");\n");
        desc.append("        value2.getSynonym().setContent(\"ru\", \"ВОбработке\");\n");
        desc.append("        enumType.getValues().add(value2);\n");
        desc.append("\n");
        desc.append("        EnumValue value3 = mdFactory.createEnumValue();\n");
        desc.append("        value3.setName(\"Completed\");\n");
        desc.append("        value3.getSynonym().setContent(\"ru\", \"Выполнен\");\n");
        desc.append("        enumType.getValues().add(value3);\n");
        desc.append("\n");
        desc.append("        EnumValue value4 = mdFactory.createEnumValue();\n");
        desc.append("        value4.setName(\"Cancelled\");\n");
        desc.append("        value4.getSynonym().setContent(\"ru\", \"Отменен\");\n");
        desc.append("        enumType.getValues().add(value4);\n");
        desc.append("\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("            enumType.eClass(), enumType.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)enumType, fqn);\n");
        desc.append("        config.getEnums().add(enumType);\n");
        desc.append("        modelFactory.fillDefaultReferences(enumType);\n");
        desc.append("\n");
        desc.append("        return enumType;\n");
        desc.append("    }\n");
        desc.append("    public Object getId() { return \"createEnumTask\"; }\n");
        desc.append("    public String getName() { return \"Create Enum Task\"; }\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildWorkflows()
    {
        var desc = new StringBuilder();
        desc.append("## Detailed Workflows for Metadata Operations\n\n");

        desc.append("### Workflow: CREATE New Metadata Object\n\n");
        desc.append("**Complete step-by-step workflow for creating any metadata object:**\n\n");
        desc.append("#### Step 1: Initialize Context\n");
        desc.append("```java\n");
        desc.append("// Get the Eclipse project\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("\n");
        desc.append("// Get V8 project wrapper\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("\n");
        desc.append("// Get BM (Business Model) for the project\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("\n");
        desc.append("// Get global editing context (for read operations)\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 2: Execute Transaction\n");
        desc.append("```java\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Task Name\") {\n");
        desc.append("    @Override\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // All metadata operations go here\n");
        desc.append("    }\n");
        desc.append("    @Override\n");
        desc.append("    public Object getId() { return \"uniqueTaskId\"; }\n");
        desc.append("    @Override\n");
        desc.append("    public String getName() { return \"Task Display Name\"; }\n");
        desc.append("    @Override\n");
        desc.append("    public Object getServiceId() { return \"ServiceId\"; }\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 3: Get Parent Object\n");
        desc.append("```java\n");
        desc.append("// Get configuration (parent for all metadata)\n");
        desc.append("Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("// Or get specific parent (e.g., Subsystem)\n");
        desc.append("Subsystem subsystem = (Subsystem) transaction.getTopObjectByFqn(\"Subsystem.Inventory\");\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 4: Create New Object\n");
        desc.append("```java\n");
        desc.append("// Method A: Create with MdClassFactory (simple)\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"MyCatalog\");\n");
        desc.append("\n");
        desc.append("// Method B: Create with ModelFactory (with context - recommended)\n");
        desc.append("Catalog catalog = modelFactory.create(\n");
        desc.append("    Catalog.class,                    // Class\n");
        desc.append("    config,                          // Parent\n");
        desc.append("    v8project.getRuntimeVersion()    // Version context\n");
        desc.append(");\n");
        desc.append("catalog.setName(\"MyCatalog\");\n");
        desc.append("catalog.getSynonym().setContent(\"ru\", \"МойСправочник\");\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 5: Configure Object Properties\n");
        desc.append("```java\n");
        desc.append("// Set basic properties\n");
        desc.append("catalog.setHierarchyType(HierarchyType.GROUPS);\n");
        desc.append("catalog.setCodeLength(9);\n");
        desc.append("catalog.setNameLength(150);\n");
        desc.append("catalog.setCodeSeries(CodeSeries.BY_PARENT);\n");
        desc.append("\n");
        desc.append("// Set presentation properties\n");
        desc.append("catalog.getSynonym().setContent(\"ru\", \"Товары\");\n");
        desc.append("catalog.getSynonym().setContent(\"en\", \"Products\");\n");
        desc.append("catalog.getComment().setContent(\"ru\", \"Справочник товаров\");\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 6: Add Attributes (Requisites)\n");
        desc.append("```java\n");
        desc.append("// Create attribute\n");
        desc.append("BasicFeature attribute = mdFactory.createBasicFeature();\n");
        desc.append("attribute.setName(\"AttributeName\");\n");
        desc.append("attribute.getSynonym().setContent(\"ru\", \"ИмяАтрибута\");\n");
        desc.append("\n");
        desc.append("// Set type\n");
        desc.append("attribute.setType(mdFactory.createTypeDescription(\"String.100\"));\n");
        desc.append("// Or:\n");
        desc.append("attribute.setType(mdFactory.createTypeDescription(\"Number.15:2\"));\n");
        desc.append("// Or:\n");
        desc.append("attribute.setType(mdFactory.createCatalogRef(productsCatalog));\n");
        desc.append("\n");
        desc.append("// Add to object\n");
        desc.append("catalog.getAttributes().add(attribute);\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 7: Add Tabular Parts (if applicable)\n");
        desc.append("```java\n");
        desc.append("// Create tabular part\n");
        desc.append("CatalogTabularSection tabularPart = mdFactory.createCatalogTabularSection();\n");
        desc.append("tabularPart.setName(\"TabularPartName\");\n");
        desc.append("tabularPart.getSynonym().setContent(\"ru\", \"ИмяТабличнойЧасти\");\n");
        desc.append("\n");
        desc.append("// Add attributes to tabular part\n");
        desc.append("BasicFeature tpAttribute = mdFactory.createBasicFeature();\n");
        desc.append("tpAttribute.setName(\"TPAttributeName\");\n");
        desc.append("tpAttribute.setType(mdFactory.createTypeDescription(\"String.50\"));\n");
        desc.append("tabularPart.getAttributes().add(tpAttribute);\n");
        desc.append("\n");
        desc.append("// Add tabular part to object\n");
        desc.append("catalog.getTabularSections().add(tabularPart);\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 8: Generate FQN\n");
        desc.append("```java\n");
        desc.append("// Generate FQN (Fully Qualified Name)\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("    catalog.eClass(),      // EClass of the object\n");
        desc.append("    catalog.getName()     // Object name\n");
        desc.append(").toString();\n");
        desc.append("// Result: \"Catalog.MyCatalog\"\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 9: Attach to BM (Business Model)\n");
        desc.append("```java\n");
        desc.append("// Attach as top object - makes it persistent\n");
        desc.append("transaction.attachTopObject(\n");
        desc.append("    (IBmObject)catalog,   // Object to attach\n");
        desc.append("    fqn                  // FQN for identification\n");
        desc.append(");\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 10: Link to Parent\n");
        desc.append("```java\n");
        desc.append("// Add to configuration's catalog collection\n");
        desc.append("config.getCatalogs().add(catalog);\n");
        desc.append("\n");
        desc.append("// Or add to subsystem\n");
        desc.append("subsystem.getMetadata().add(catalog);\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 11: Fill Default References\n");
        desc.append("```java\n");
        desc.append("// Fill default cross-references\n");
        desc.append("modelFactory.fillDefaultReferences(catalog);\n");
        desc.append("// This ensures proper linking with system objects\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 12: Return and Complete\n");
        desc.append("```java\n");
        desc.append("return catalog;  // Return created object\n");
        desc.append("```\\n\\n");

        desc.append("---\\n\\n");

        desc.append("### Workflow: EDIT Existing Metadata Object\n\n");
        desc.append("**Complete workflow for modifying existing metadata:**\n\n");
        desc.append("#### Step 1: Get Context (same as CREATE)\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 2: Execute Transaction\n");
        desc.append("```java\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Edit Object\") {\n");
        desc.append("    @Override\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Edit operations here\n");
        desc.append("    }\n");
        desc.append("    // ... implement getId(), getName(), getServiceId()\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 3: Retrieve Object by FQN\n");
        desc.append("```java\n");
        desc.append("// Get existing object\n");
        desc.append("Catalog catalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("\n");
        desc.append("// Check if object exists\n");
        desc.append("if (catalog == null) {\n");
        desc.append("    System.out.println(\"Catalog not found\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 4: Modify Properties\n");
        desc.append("```java\n");
        desc.append("// Modify simple properties\n");
        desc.append("catalog.setCodeLength(12);  // Change code length\n");
        desc.append("catalog.setNameLength(200);  // Change name length\n");
        desc.append("catalog.getSynonym().setContent(\"ru\", \"ТоварыНоменклатура\");\n");
        desc.append("\n");
        desc.append("// Modify comment\n");
        desc.append("catalog.getComment().setContent(\"ru\", \"Модифицирован\");\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 5: Add New Attribute\n");
        desc.append("```java\n");
        desc.append("// Create and add new attribute\n");
        desc.append("BasicFeature newAttr = mdFactory.createBasicFeature();\n");
        desc.append("newAttr.setName(\"NewAttribute\");\n");
        desc.append("newAttr.setType(mdFactory.createTypeDescription(\"String.50\"));\n");
        desc.append("catalog.getAttributes().add(newAttr);\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 6: Modify Existing Attribute\n");
        desc.append("```java\n");
        desc.append("// Find and modify attribute\n");
        desc.append("for (BasicFeature attr : catalog.getAttributes()) {\n");
        desc.append("    if (\"Price\".equals(attr.getName())) {\n");
        desc.append("        // Modify type\n");
        desc.append("        attr.setType(mdFactory.createTypeDescription(\"Number.18:2\"));\n");
        desc.append("        // Modify synonym\n");
        desc.append("        attr.getSynonym().setContent(\"ru\", \"ЦенаПродажи\");\n");
        desc.append("        break;\n");
        desc.append("    }\n");
        desc.append("}\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 7: Remove Attribute\n");
        desc.append("```java\n");
        desc.append("// Remove by name\n");
        desc.append("catalog.getAttributes().removeIf(attr -> \"OldAttribute\".equals(attr.getName()));\n");
        desc.append("\n");
        desc.append("// Or remove by reference\n");
        desc.append("BasicFeature attrToRemove = ...;\n");
        desc.append("catalog.getAttributes().remove(attrToRemove);\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 8: Add/Modify Tabular Parts\n");
        desc.append("```java\n");
        desc.append("// Add new tabular part\n");
        desc.append("CatalogTabularSection newTP = mdFactory.createCatalogTabularSection();\n");
        desc.append("newTP.setName(\"NewTabularPart\");\n");
        desc.append("catalog.getTabularSections().add(newTP);\n");
        desc.append("\n");
        desc.append("// Modify existing tabular part\n");
        desc.append("for (CatalogTabularSection tp : catalog.getTabularSections()) {\n");
        desc.append("    if (\"Specifications\".equals(tp.getName())) {\n");
        desc.append("        // Add attribute to tabular part\n");
        desc.append("        BasicFeature tpAttr = mdFactory.createBasicFeature();\n");
        desc.append("        tpAttr.setName(\"NewColumn\");\n");
        desc.append("        tp.getAttributes().add(tpAttr);\n");
        desc.append("        break;\n");
        desc.append("    }\n");
        desc.append("}\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 9: Remove Tabular Part\n");
        desc.append("```java\n");
        desc.append("// Remove tabular part by name\n");
        desc.append("catalog.getTabularSections().removeIf(tp -> \"OldTP\".equals(tp.getName()));\n");
        desc.append("```\\n\\n");

        desc.append("---\\n\\n");

        desc.append("### Workflow: DELETE Metadata Object\n\n");
        desc.append("**Complete workflow for deleting metadata:**\n\n");
        desc.append("#### Step 1: Get Context (same as CREATE)\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 2: Execute Transaction\n");
        desc.append("```java\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Delete Object\") {\n");
        desc.append("    @Override\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Delete operations here\n");
        desc.append("    }\n");
        desc.append("    // ... implement getId(), getName(), getServiceId()\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 3: Retrieve Object to Delete\n");
        desc.append("```java\n");
        desc.append("// Get object\n");
        desc.append("Catalog catalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("\n");
        desc.append("// Check if exists\n");
        desc.append("if (catalog == null) {\n");
        desc.append("    System.out.println(\"Object not found\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 4: Remove from Parent Collections\n");
        desc.append("```java\n");
        desc.append("// Get configuration\n");
        desc.append("Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("// Remove from configuration\n");
        desc.append("config.getCatalogs().remove(catalog);\n");
        desc.append("\n");
        desc.append("// Also remove from subsystems if needed\n");
        desc.append("for (Subsystem subsystem : config.getSubsystems()) {\n");
        desc.append("    subsystem.getMetadata().removeIf(md -> md == catalog);\n");
        desc.append("}\n");
        desc.append("```\\n\\n");

        desc.append("#### Step 5: Detach from BM\n");
        desc.append("```java\n");
        desc.append("// Detach from Business Model\n");
        desc.append("transaction.detachTopObject((IBmObject)catalog);\n");
        desc.append("\n");
        desc.append("System.out.println(\"Deleted: \" + catalog.getName());\n");
        desc.append("```\\n\\n");

        desc.append("---\\n\\n");

        desc.append("### Workflow: FIND and LIST Metadata\n\n");
        desc.append("**Workflow for finding and listing metadata objects:**\n\n");
        desc.append("#### Find by FQN\n");
        desc.append("```java\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Find Object\") {\n");
        desc.append("    @Override\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Find specific object\n");
        desc.append("        Catalog catalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("\n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            System.out.println(\"Found: \" + catalog.getName());\n");
        desc.append("            System.out.println(\"Attributes: \" + catalog.getAttributes().size());\n");
        desc.append("            System.out.println(\"Tabular Parts: \" + catalog.getTabularSections().size());\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        return catalog;\n");
        desc.append("    }\n");
        desc.append("    // ... implement getId(), getName(), getServiceId()\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("#### List All Catalogs\n");
        desc.append("```java\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"List Catalogs\") {\n");
        desc.append("    @Override\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        // Iterate all catalogs\n");
        desc.append("        for (Catalog catalog : config.getCatalogs()) {\n");
        desc.append("            System.out.println(\"Catalog: \" + catalog.getName());\n");
        desc.append("            System.out.println(\"  Synonym: \" + catalog.getSynonym().getContent(\"ru\"));\n");
        desc.append("            System.out.println(\"  Hierarchy: \" + catalog.getHierarchyType());\n");
        desc.append("            System.out.println(\"  Attributes: \" + catalog.getAttributes().size());\n");
        desc.append("\n");
        desc.append("            // List attributes\n");
        desc.append("            for (BasicFeature attr : catalog.getAttributes()) {\n");
        desc.append("                System.out.println(\"    - \" + attr.getName() + \": \" + attr.getType().getTypeName());\n");
        desc.append("            }\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("    // ... implement getId(), getName(), getServiceId()\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("#### List All Documents\n");
        desc.append("```java\n");
        desc.append("// List all documents\n");
        desc.append("Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("for (Document document : config.getDocuments()) {\n");
        desc.append("    System.out.println(\"Document: \" + document.getName());\n");
        desc.append("    System.out.println(\"  Post in change list: \" + document.isPostInChangeList());\n");
        desc.append("    System.out.println(\"  Numbering: \" + document.getNumbering());\n");
        desc.append("    System.out.println(\"  Attributes: \" + document.getAttributes().size());\n");
        desc.append("    System.out.println(\"  Tabular Parts: \" + document.getTabularSections().size());\n");
        desc.append("}\n");
        desc.append("```\\n\\n");

        desc.append("---\\n\\n");

        desc.append("### Workflow: WORK WITH SUBSYSTEMS\n\n");
        desc.append("```java\n");
        desc.append("// Create subsystem and add objects\n");
        desc.append("globalContext.executeReadonlyTask(new AbstractBmTask<IBmObject>(\"Manage Subsystem\") {\n");
        desc.append("    @Override\n");
        desc.append("    public IBmObject execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration) transaction.getTopObjectByFqn(\"Configuration\");\n\n");
        desc.append("        // Find or create subsystem\n");
        desc.append("        Subsystem subsystem = (Subsystem) transaction.getTopObjectByFqn(\"Subsystem.Inventory\");\n");
        desc.append("        if (subsystem == null) {\n");
        desc.append("            subsystem = mdFactory.createSubsystem();\n");
        desc.append("            subsystem.setName(\"Inventory\");\n");
        desc.append("            subsystem.getSynonym().setContent(\"ru\", \"Склад\");\n");
        desc.append("            \n");
        desc.append("            String fqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("                subsystem.eClass(), subsystem.getName()).toString();\n");
        desc.append("            transaction.attachTopObject((IBmObject)subsystem, fqn);\n");
        desc.append("            config.getSubsystems().add(subsystem);\n");
        desc.append("            modelFactory.fillDefaultReferences(subsystem);\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        // Add objects to subsystem\n");
        desc.append("        Catalog catalog = (Catalog) transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("        Document document = (Document) transaction.getTopObjectByFqn(\"Document.SalesInvoice\");\n\n");
        desc.append("        if (catalog != null) subsystem.getMetadata().add(catalog);\n");
        desc.append("        if (document != null) subsystem.getMetadata().add(document);\n");
        desc.append("\n");
        desc.append("        return subsystem;\n");
        desc.append("    }\n");
        desc.append("    // ... implement getId(), getName(), getServiceId()\n");
        desc.append("});\n");
        desc.append("```\\n\\n");

        desc.append("---\\n\\n");

        desc.append("### Best Practices and Common Patterns\n\n");
        desc.append("**1. Always use transactions for metadata operations**\n");
        desc.append("- Never modify metadata outside of transactions\n");
        desc.append("- Use `executeReadonlyTask` for read operations\n");
        desc.append("- For write operations, use local editing context (requires save)\n\n");
        desc.append("**2. Generate FQN before attaching to BM**\n");
        desc.append("- FQN must be unique\n");
        desc.append("- Format: `Catalog.MyCatalog`, `Document.MyDocument`, etc.\n\n");
        desc.append("**3. Always fill default references**\n");
        desc.append("- Ensures proper linking with system objects\n");
        desc.append("- Call `modelFactory.fillDefaultReferences(object)` after creation\n\n");
        desc.append("**4. Check for null before operations**\n");
        desc.append("- Objects might not exist\n");
        desc.append("- Always check `if (object != null)`\n\n");
        desc.append("**5. Use ModelFactory instead of MdClassFactory when possible**\n");
        desc.append("- Provides proper initialization\n");
        desc.append("- Sets version context correctly\n\n");
        desc.append("**6. Set synonyms and comments**\n");
        desc.append("- Good practice for multilingual support\n");
        desc.append("- Improves user experience\n\n");
        desc.append("**7. Handle errors gracefully**\n");
        desc.append("- Try-catch around critical operations\n");
        desc.append("- Log errors for debugging\n\n");

        return desc.toString();
    }
}
