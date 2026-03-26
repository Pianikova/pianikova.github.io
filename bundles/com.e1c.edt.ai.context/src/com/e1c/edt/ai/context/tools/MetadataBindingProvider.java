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
}
