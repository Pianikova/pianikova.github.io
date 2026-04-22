/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.emf.ecore.util.EcoreUtil;

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
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterType;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterPeriodicity;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypes;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.Recalculation;
import com._1c.g5.v8.dt.metadata.mdclass.Report;
import com._1c.g5.v8.dt.metadata.mdclass.TabularSectionAttribute;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
import com.e1c.edt.ai.tools.IJShellBindingProvider;
import com.e1c.edt.ai.tools.IJShellManualProvider;
import com.e1c.edt.ai.tools.JShellBindingDescription;
import com.e1c.edt.ai.tools.JShellManualEntry;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provides JShell bindings for 1C metadata creation, editing, and deletion operations.
 * <p>
 * This provider offers access to:
 * <ul>
 *   <li>{@link MdClassFactory} - Factory for creating metadata objects</li>
 *   <li>{@link ITopObjectFqnGenerator} - Generates FQNs for top-level objects</li>
 *   <li>{@link IModelObjectFactory} - Creates model objects in project context</li>
 *   <li>{@link IV8ProjectManager} - Resolves IV8Project from Eclipse projects</li>
 *   <li>{@link IBmModelManager} - Provides BM model and editing contexts for read/write operations</li>
 *   <li>{@link IResourceLookup} - Maps metadata objects to Eclipse resources</li>
 * </ul>
 * <p>
 * <b>Key Operations:</b>
 * <ul>
 *   <li>Create new metadata objects (Catalog, Document, Register, etc.)</li>
 *   <li>Edit existing metadata objects</li>
 *   <li>Delete metadata objects by removing from parent collection and detaching from transaction</li>
 *   <li>Manage UUIDs for new objects</li>
 *   <li>Create TypeDescription for attributes using {@link TypeDescriptionBuilder}</li>
 *   <li>Transaction-based modifications</li>
 * </ul>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 *   <li>Always use BM transactions ({@link IBmGlobalEditingContext#execute()}) for modifications</li>
 *   <li>For deletion: remove from parent collection and detach from transaction - NEVER use collection {@code remove()} alone or {@code EcoreUtil.delete()} for top-level objects</li>
 *   <li>New objects MUST have UUIDs set via {@code object.setUuid(UUID.randomUUID())}</li>
 *   <li>{@code mdFactory} can only be used inside BM transaction</li>
 *   <li>Use {@link TypeDescriptionBuilder} and {@link IEObjectProvider} for type handling - see TypeDescription Handling Guide</li>
 * </ul>
 */
@Singleton
public class MetadataBindingProvider
    implements IJShellBindingProvider, IJShellManualProvider
{
    private final IV8ProjectManager v8projectManager;
    private final IBmModelManager modelManager;
    private final ITopObjectFqnGenerator topObjectFqnGenerator;
    private final IResourceLookup resourceLookup;
    private final IModelObjectFactory modelObjectFactory;
    private final IMethodListProvider methodListProvider;

    @Inject
    public MetadataBindingProvider(IV8ProjectManager v8projectManager, IBmModelManager modelManager,
        ITopObjectFqnGenerator topObjectFqnGenerator, IResourceLookup resourceLookup,
        IModelObjectFactory modelObjectFactory, IMethodListProvider methodListProvider)
    {
        Preconditions.checkNotNull(v8projectManager);
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(topObjectFqnGenerator);
        Preconditions.checkNotNull(resourceLookup);
        Preconditions.checkNotNull(modelObjectFactory);
        Preconditions.checkNotNull(methodListProvider);

        this.v8projectManager = v8projectManager;
        this.modelManager = modelManager;
        this.topObjectFqnGenerator = topObjectFqnGenerator;
        this.resourceLookup = resourceLookup;
        this.modelObjectFactory = modelObjectFactory;
        this.methodListProvider = methodListProvider;
    }

    @SuppressWarnings("nls")
    @Override
    public Map<String, JShellBindingDescription> getBindings()
    {
        var bindings = new HashMap<String, JShellBindingDescription>();

        var mdClassFactory = MdClassFactory.eINSTANCE;
        bindings.put("mdFactory", new JShellBindingDescription("Factory for creating 1C metadata objects",
            buildMdFactoryDescription(), mdClassFactory, MdClassFactory.class,
            "**⚠️ RESTRICTION: Cannot be used outside BM transaction.** Use `mdFactory` ONLY in "
                + "AbstractBmTask.execute() body, where IBmTransaction is available. Do not use attachTopObject() for existing objects. "
                + "**IMPORTANT**: Objects created with mdFactory MUST have UUIDs set via "
                + "manual assignment: `object.setUuid(UUID.randomUUID())`. "
                + "NOTE: `modelFactory.fillDefaultReferences()` may timeout in JShell due to OSGi service limitations."));

        bindings.put("fqnGenerator", new JShellBindingDescription(
            "Generates FQNs (Fully Qualified Names) for top-level metadata objects. Required before attachTopObject().",
            buildFqnGeneratorDescription(),
            topObjectFqnGenerator,
            ITopObjectFqnGenerator.class));

        bindings.put("modelFactory", new JShellBindingDescription(
            "Creates model objects in project/version context",
            buildModelFactoryDescription(),
            modelObjectFactory,
            IModelObjectFactory.class));

        bindings.put("projectManager", new JShellBindingDescription(
            "Resolves IV8Project from Eclipse projects",
            buildProjectManagerDescription(),
            v8projectManager,
            IV8ProjectManager.class));

        bindings.put("modelManager", new JShellBindingDescription(
            "Provides BM model and editing contexts. Use for read/write operations with transactions.",
            buildModelManagerDescription(),
            modelManager,
            IBmModelManager.class));

        bindings.put("resourceLookup", new JShellBindingDescription(
            "Maps metadata/model objects to Eclipse resources",
            buildResourceLookupDescription(),
            resourceLookup,
            IResourceLookup.class));

        return bindings;
    }

    @SuppressWarnings("nls")
    @Override
    public String getUseCases()
    {
        return "- Create and edit 1C metadata objects in BM transactions"
            + "\n- Resolve IV8Project, BM model, and all top-level configuration entities"
            + "\n- Build TypeDescription for attributes, dimensions, and resources"
            + "\n- Attach new top-level objects with generated FQN"
            + "\n- Remove existing objects through parent collections and transaction detach"
            + "\n- For detailed workflows and templates, use `JShellManual`"
            + "\n- Use enhanced methods with object existence checks and error handling";
    }

    @SuppressWarnings("nls")
    @Override
    public String getDescription()
    {
        return "1C metadata API: Create, edit, and delete top-level 1C metadata entities and common child objects. "
                + "Includes factories (mdFactory, modelFactory), FQN generator, BM model with transactions, "
                + "and resource lookup for Eclipse integration.";
    }

    @Override
    public Collection<JShellManualEntry> getManualEntries()
    {
        var entries = new ArrayList<JShellManualEntry>();

        entries.add(new JShellManualEntry(
            "edt_overview", //$NON-NLS-1$
            "edt", //$NON-NLS-1$
            "EDT Metadata Overview", //$NON-NLS-1$
            "Core EDT transaction, type, validation, and pitfall guidance before writing metadata code.", //$NON-NLS-1$
            buildOverviewManual(),
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("edt", "metadata", "transaction", "types", "pitfalls"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        entries.add(new JShellManualEntry("create_configuration_project", "edt", "Create Configuration Project", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create a new 1C configuration project in the Eclipse workspace with the required project structure.", //$NON-NLS-1$
            buildCreateConfigurationWorkflow(),
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("configuration", "configuration project", "create configuration", "create project"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("delete_configuration_project", "edt", "Delete Configuration Project", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Delete an existing 1C configuration project from the workspace and clean up associated resources.", //$NON-NLS-1$
            buildDeleteConfigurationWorkflow(),
            List.of("workspaceRoot", "projectManager"), //$NON-NLS-1$ //$NON-NLS-2$
            List.of("configuration", "configuration project", "delete configuration", "delete project"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        entries.add(new JShellManualEntry("create_catalog", "edt", "Create Catalog", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create a top-level Catalog safely inside a BM transaction.", buildSafeCatalogWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("catalog", "create catalog", "metadata object"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_document", "edt", "Create Document", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create a Document with safe defaults and BM transaction rules.", buildDocumentWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("document", "create document"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_information_register", "edt", "Create Information Register", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create information register with dimensions, resources, and periodicity.", buildInformationRegisterWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("information register", "register", "create information register"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_enum", "edt", "Create Enum", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create enumeration and initial enum values.", buildEnumWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("enum", "enumeration", "enum value"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_common_module", "edt", "Create Common Module", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create CommonModule metadata and align it with Module.bsl.", buildCommonModuleWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator", "resourceLookup"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            List.of("common module", "module", "create common module"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_chart_of_accounts", "edt", "Create Chart Of Accounts", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create ChartOfAccounts as a top-level accounting metadata object.", buildChartOfAccountsWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("chart of accounts", "accounts", "accounting"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_chart_of_calculation_types", "edt", "Create Chart Of Calculation Types", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create ChartOfCalculationTypes for calculation registers.", buildChartOfCalculationTypesWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("chart of calculation types", "calculation types", "payroll"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_business_process", "edt", "Create Business Process", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create BusinessProcess metadata with standard top-level workflow.", buildBusinessProcessWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("business process", "bp", "process"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_task", "edt", "Create Task", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Task metadata with attributes and standard top-level workflow.", buildTaskWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("task", "create task"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_exchange_plan", "edt", "Create Exchange Plan", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create ExchangePlan metadata with safe top-level attach flow.", buildExchangePlanWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("exchange plan", "replication", "exchange"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_constant", "edt", "Create Constant", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Constant metadata with mandatory TypeDescription.", buildConstantWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("constant", "settings", "type description"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_defined_type", "edt", "Create Defined Type", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create DefinedType with a precise TypeDescription.", buildDefinedTypeWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("defined type", "type alias", "type description"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_document_journal", "edt", "Create Document Journal", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create DocumentJournal metadata with safe top-level attach flow.", buildDocumentJournalWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("document journal", "journal"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_document_numerator", "edt", "Create Document Numerator", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create DocumentNumerator metadata and connect documents to it later.", buildDocumentNumeratorWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("document numerator", "numerator", "numbering"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_report", "edt", "Create Report", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Report metadata; forms, module, and layouts can be added later.", buildReportWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("report", "analytics"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_data_processor", "edt", "Create Data Processor", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create DataProcessor metadata; forms and module content are separate follow-up steps.", buildDataProcessorWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("data processor", "processor", "обработка"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_web_service", "edt", "Create Web Service", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create WebService metadata and then add operations and parameters.", buildWebServiceWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("web service", "soap", "operation"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_http_service", "edt", "Create HTTP Service", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create HTTPService metadata and then add URL templates and methods.", buildHttpServiceWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("http service", "rest", "url template"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_integration_service", "edt", "Create Integration Service", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create IntegrationService metadata and then add channels.", buildIntegrationServiceWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("integration service", "service channel", "integration"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_common_form", "edt", "Create Common Form", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create CommonForm metadata and attach it as a top-level object.", buildCommonFormWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("common form", "form"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_common_command", "edt", "Create Common Command", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create CommonCommand metadata and add it to configuration command collections.", buildCommonCommandWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("common command", "command"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_common_attribute", "edt", "Create Common Attribute", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create CommonAttribute metadata with mandatory TypeDescription.", buildCommonAttributeWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("common attribute", "attribute", "type description"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_common_template", "edt", "Create Common Template", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create CommonTemplate metadata and fill template content afterwards.", buildCommonTemplateWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("common template", "template", "макет"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_common_picture", "edt", "Create Common Picture", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create CommonPicture metadata and assign picture content later.", buildCommonPictureWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("common picture", "picture", "image"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_interface", "edt", "Create Interface", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Interface metadata and configure command interface content afterwards.", buildInterfaceWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("interface", "ui interface"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_session_parameter", "edt", "Create Session Parameter", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create SessionParameter metadata with mandatory TypeDescription.", buildSessionParameterWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("session parameter", "parameter", "type description"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_ws_reference", "edt", "Create WS Reference", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create WSReference metadata and configure service connection details later.", buildWSReferenceWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("ws reference", "soap client", "reference"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_event_subscription", "edt", "Create Event Subscription", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create EventSubscription metadata and fill event handler references explicitly.", buildEventSubscriptionWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("event subscription", "handler", "subscription"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_scheduled_job", "edt", "Create Scheduled Job", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create ScheduledJob metadata and define job parameters afterwards.", buildScheduledJobWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("scheduled job", "regламентное задание", "job"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_filter_criterion", "edt", "Create Filter Criterion", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create FilterCriterion metadata and set its TypeDescription carefully.", buildFilterCriterionWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("filter criterion", "criterion", "type description"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_settings_storage", "edt", "Create Settings Storage", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create SettingsStorage metadata and bind configuration references to it later.", buildSettingsStorageWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("settings storage", "storage"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_functional_option", "edt", "Create Functional Option", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create FunctionalOption metadata and bind controlled objects later.", buildFunctionalOptionWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("functional option", "feature flag", "option"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_functional_options_parameter", "edt", "Create Functional Options Parameter", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create FunctionalOptionsParameter metadata and connect it to functional options afterwards.", buildFunctionalOptionsParameterWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("functional options parameter", "feature parameter", "parameter"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_subsystem", "edt", "Create Subsystem", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Subsystem metadata and then link objects through references or command interfaces.", buildSubsystemWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("subsystem", "section"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_role", "edt", "Create Role", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Role metadata and then configure rights separately.", buildRoleWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("role", "rights", "security"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_command_group", "edt", "Create Command Group", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create CommandGroup metadata and then place commands in it.", buildCommandGroupWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("command group", "group", "commands"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_language", "edt", "Create Language", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Language metadata and then configure default language references in configuration.", buildLanguageWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("language", "localization"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_style", "edt", "Create Style", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Style metadata and then add style items or assign it as default style.", buildStyleWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("style", "theme", "ui style"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_style_item", "edt", "Create Style Item", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create StyleItem metadata and configure its visual type afterwards.", buildStyleItemWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("style item", "style element"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_external_data_source", "edt", "Create External Data Source", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create ExternalDataSource metadata and then add tables, cubes, and fields.", buildExternalDataSourceWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("external data source", "data source", "integration"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_sequence", "edt", "Create Sequence", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Sequence metadata and then add dimension rules for ordering logic.", buildSequenceWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("sequence", "ordering", "последовательность"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_xdto_package", "edt", "Create XDTO Package", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create XDTOPackage metadata and then add package content or schema definitions.", buildXDTOPackageWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("xdto package", "xdto", "schema"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_chart_of_characteristic_types", "edt", "Create Chart Of Characteristic Types", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create ChartOfCharacteristicTypes metadata with code, description, and value type settings.", buildChartOfCharacteristicTypesWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("chart of characteristic types", "characteristics", "plan видов характеристик"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_bot", "edt", "Create Bot", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Bot metadata and configure its conversation logic afterwards.", buildBotWorkflowSafe(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("bot", "assistant", "chat bot"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_table", "edt", "Create Table", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Table inside ExternalDataSource with fields and command sources added afterwards.", buildTableWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            List.of("table", "external data source table"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("create_cube", "edt", "Create Cube", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create Cube inside ExternalDataSource and then add dimensions, resources, and functions.", buildCubeWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            List.of("cube", "olap", "analytics"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        entries.add(new JShellManualEntry("add_tabular_section", "edt", "Add Tabular Section", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Add a tabular section and its attributes to an existing metadata object.", buildTabularSectionWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            List.of("tabular section", "document tabular section", "catalog tabular section"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("add_document_registers", "edt", "Add Document Registers", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Configure which registers a document can write to (set up registrars on document side).", buildDocumentRegistersWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("document register", "registrar", "register records"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        entries.add(new JShellManualEntry("create_attribute_for_entity", "edt", "Create Attribute For Entity", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create entity-specific attributes with the correct child type and TypeDescription.", buildCreateAttributeForEntityWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            List.of("attribute", "catalog attribute", "document attribute", "register attribute"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("add_registered_documents_to_journal", "edt", "Add Registered Documents To Journal", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Configure which documents appear in a document journal (set up on journal side).", buildDocumentJournalDocumentsWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("document journal", "registered documents", "journal"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        entries.add(new JShellManualEntry("create_type_description", "edt", "Create TypeDescription", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Build primitive, reference, union, and qualified TypeDescription values.", buildCreateTypeDescriptionWorkflow(), //$NON-NLS-1$
            List.of("modelManager"), //$NON-NLS-1$
            List.of("type description", "typedescription", "qualifiers", "reference type"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("set_exchange_plan_thisnode", "edt", "Set Exchange Plan ThisNode", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Set the current node UUID for an exchange plan to participate in replication.", buildExchangePlanThisNodeWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("exchange plan", "this node", "replication", "exchange"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        entries.add(new JShellManualEntry("resolve_top_object_and_parent_collection", "edt", "Resolve Top Object And Parent Collection", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Resolve top objects by FQN and choose the correct parent collection before mutation.", buildResolveTopObjectWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            List.of("top object", "fqn", "parent collection", "containment"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        entries.add(new JShellManualEntry("edit_existing_metadata", "edt", "Edit Existing Metadata", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Generic workflow for editing any existing metadata object correctly.", buildEditExistingMetadataWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("edit", "existing object", "update metadata", "modification"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        entries.add(new JShellManualEntry("edit_existing_object", "edt", "Edit Existing Object", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Modify an existing metadata object without reattaching it as a new top object.", buildEditExistingObjectWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("edit", "existing object", "update metadata"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("edit_information_register", "edt", "Edit Information Register", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Edit InformationRegister by loading it from BM transaction and changing only existing features.", //$NON-NLS-1$
            buildGenericEditWorkflow("InformationRegister", "InformationRegister.Prices", //$NON-NLS-1$ //$NON-NLS-2$
                "register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY);\n        register.setUseStandardCommands(true);", //$NON-NLS-1$
                "Do not call attachTopObject() for an existing register. Add or remove child objects through the existing collections."), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("edit information register", "information register"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("edit_enum", "edt", "Edit Enum", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Edit Enum and its enum values by loading the existing top-level object.", //$NON-NLS-1$
            buildGenericEditWorkflow("Enum", "Enum.Statuses", //$NON-NLS-1$ //$NON-NLS-2$
                "enumObject.getEnumValues().get(0).setName(\"Active\");", //$NON-NLS-1$
                "Prefer changing value names/descriptions in-place. For deleted values, remove them from enumObject.getEnumValues()."), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("edit enum", "enum value"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("edit_common_module", "edt", "Edit Common Module", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Edit CommonModule flags and metadata separately from Module.bsl content.", //$NON-NLS-1$
            buildGenericEditWorkflow("CommonModule", "CommonModule.WorkingWithData", //$NON-NLS-1$ //$NON-NLS-2$
                "commonModule.setServer(true);\n        commonModule.setServerCall(true);", //$NON-NLS-1$
                "Metadata flags and BSL source are separate concerns. Update Module.bsl through file tools, not through mdFactory."), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "resourceLookup"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            List.of("edit common module", "module flags"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("edit_chart_of_accounts", "edt", "Edit Chart Of Accounts", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Edit ChartOfAccounts by loading existing object and mutating supported features.", //$NON-NLS-1$
            buildGenericEditWorkflow("ChartOfAccounts", "ChartOfAccounts.MainChart", //$NON-NLS-1$ //$NON-NLS-2$
                "chart.setCodeLength(10);\\n        chart.setDescriptionLength(100);", //$NON-NLS-1$
                "Be careful with references from AccountingRegister and dependent objects when renaming or restructuring the chart."), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("edit chart of accounts", "accounts chart"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("edit_business_process", "edt", "Edit Business Process", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Edit BusinessProcess in BM transaction and keep child objects in the right collections.", //$NON-NLS-1$
            buildGenericEditWorkflow("BusinessProcess", "BusinessProcess.Approval", //$NON-NLS-1$ //$NON-NLS-2$
                "businessProcess.setNumberLength(11);\n        businessProcess.setAutonumbering(true);", //$NON-NLS-1$
                "Add attributes and tabular sections through their dedicated collections and set TypeDescription on new features."), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("edit business process", "business process"))); //$NON-NLS-1$ //$NON-NLS-2$
        entries.add(new JShellManualEntry("edit_task", "edt", "Edit Task", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Edit Task metadata in BM transaction without recreating the object.", //$NON-NLS-1$
            buildGenericEditWorkflow("Task", "Task.SupportRequest", //$NON-NLS-1$ //$NON-NLS-2$
                "InformationRegister addressing = (InformationRegister)transaction.getTopObjectByFqn(\"InformationRegister.TaskAddressing\");\n        task.setAddressing(addressing);\n        task.setAutonumbering(true);", //$NON-NLS-1$
                "Use the generic attribute/tabular-section scenarios when you need to modify child collections."), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("edit task", "task metadata"))); //$NON-NLS-1$ //$NON-NLS-2$

        entries.add(new JShellManualEntry("rename_object", "edt", "Rename Metadata Object", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Rename a metadata object inside BM transaction with correct lookup flow.", buildRenameObjectWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("rename", "metadata rename", "object name"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("delete_attribute", "edt", "Delete Attribute", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Delete attribute-like children safely by removing them from parent collections.", buildDeleteAttributeWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("delete attribute", "remove attribute", "delete field"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("delete_metadata_object", "edt", "Delete Metadata Object", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Delete top-level metadata objects through the parent collection and transaction detach.", buildDeleteMetadataObjectWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("delete metadata", "detach top object", "remove object"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        entries.add(new JShellManualEntry("create_accumulation_register", "edt", "Create Accumulation Register", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create accumulation register with dimensions, resources, and type setup.", buildAccumulationRegisterWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("accumulation register", "register", "create register"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_accounting_register", "edt", "Create Accounting Register", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create accounting register with chart of accounts dependencies.", buildAccountingRegisterWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("accounting register", "chart of accounts", "register"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("create_calculation_register", "edt", "Create Calculation Register", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create calculation register with schedule and calculation-specific references.", buildCalculationRegisterWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("calculation register", "register", "schedule"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entries.add(new JShellManualEntry("validation_errors", "edt", "Metadata Validation Errors", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Interpret common metadata validation failures and missing required fields.", buildMetadataValidationErrors(), //$NON-NLS-1$
            List.of("modelManager", "mdFactory"), //$NON-NLS-1$ //$NON-NLS-2$
            List.of("validation", "error", "metadata validation"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        entries.add(new JShellManualEntry("enhanced_catalog_creation", "edt", "Enhanced Catalog Creation", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create catalog with existence check and UUID error handling.", buildEnhancedCatalogCreationWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("enhanced catalog", "safe creation", "existence check", "UUID handling"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("safe_uuid_assignment", "edt", "Safe UUID Assignment", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Safe UUID assignment patterns for all metadata objects and child elements.", buildSafeUuidAssignmentWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            List.of("UUID", "SU45 error", "child objects UUID", "UUID assignment"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("typedescription_best_practices", "edt", "TypeDescriptionBuilder Best Practices", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Best practices for using TypeDescriptionBuilder inside transactions.", buildTypeDescriptionBuilderBestPractices(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            List.of("TypeDescriptionBuilder", "transaction context", "IEObjectProvider", "best practices"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("enhanced_document_creation", "edt", "Enhanced Document Creation", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create document with full validation and UUID error handling.", buildEnhancedDocumentCreationWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("enhanced document", "safe creation", "UUID handling", "tabular sections"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("enhanced_register_creation", "edt", "Enhanced Register Creation", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Create registers (Information, Accumulation, Accounting, Calculation) with full validation.", buildEnhancedRegisterCreationWorkflow(), //$NON-NLS-1$
            List.of("workspaceRoot", "projectManager", "modelManager", "mdFactory", "fqnGenerator"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            List.of("enhanced register", "safe creation", "UUID handling", "dimensions resources"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entries.add(new JShellManualEntry("child_elements_uuid_importance", "edt", "Child Elements UUID Importance", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "CRITICAL: Importance of UUIDs for all child elements (attributes, tabular sections, etc.).", buildChildElementsUuidImportance(), //$NON-NLS-1$
            List.of("mdFactory"), //$NON-NLS-1$
            List.of("UUID importance", "child objects", "SU45 error", "UUID assignment"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        return entries;
    }

    @SuppressWarnings("nls")
    private String buildOverviewManual()
    {
        var desc = new StringBuilder();
        desc.append(buildApiCompatibilityNotes());
        desc.append("\n\n");
        desc.append(buildTypeDescriptionHandling());
        desc.append("\n\n");
        desc.append(buildTransactionManagementScenarios());
        desc.append("\n\n");
        desc.append(buildCommonPitfalls());
        desc.append("\n\n");
        desc.append(buildMetadataValidationErrors());
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildInformationRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create InformationRegister\n\n");
        desc.append("### Recommended bindings\n");
        desc.append("- `workspaceRoot`, `projectManager`, `modelManager`, `mdFactory`, `fqnGenerator`\n\n");
        desc.append("### Example\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create information register\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        InformationRegister register = mdFactory.createInformationRegister();\n");
        desc.append("        register.setName(\"Prices\");\n");
        desc.append("        register.getSynonym().put(\"ru\", \"Prices\");\n");
        desc.append("        register.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("        InformationRegisterDimension product = mdFactory.createInformationRegisterDimension();\n");
        desc.append("        product.setName(\"Product\");\n");
        desc.append("        product.setUuid(UUID.randomUUID());\n");
        desc.append("        ").append(buildCatalogRefTypeDescription().replace("\n", "\n        ")).append("\n");
        desc.append("        product.setType(typeDesc);\n");
        desc.append("        register.getDimensions().add(product);\n");
        desc.append("\n");
        desc.append("        InformationRegisterResource price = mdFactory.createInformationRegisterResource();\n");
        desc.append("        price.setName(\"Price\");\n");
        desc.append("        price.setUuid(UUID.randomUUID());\n");
        desc.append("        ").append(buildNumberTypeDescription().replace("\n", "\n        ")).append("\n");
        desc.append("        price.setType(typeDesc);\n");
        desc.append("        register.getResources().add(price);\n");
        desc.append("\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)register, fqn);\n");
        desc.append("        configuration.getInformationRegisters().add(register);\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Notes\n");
        desc.append("- InformationRegister usually needs at least one dimension and often one resource\n");
        desc.append("- Every new feature derived from BasicFeature must have `setType(...)`\n");
        desc.append("- Use a specific reference type like `Catalog.Products` when you need a strict typed dimension\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildEnumWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create Enum\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create enum\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        com._1c.g5.v8.dt.metadata.mdclass.Enum enumObject = mdFactory.createEnum();\n");
        desc.append("        enumObject.setName(\"Statuses\");\n");
        desc.append("        enumObject.setUuid(UUID.randomUUID());\n");
        desc.append("        com._1c.g5.v8.dt.metadata.mdclass.EnumValue active = mdFactory.createEnumValue();\n");
        desc.append("        active.setName(\"Active\");\n");
        desc.append("        active.setUuid(UUID.randomUUID());\n");
        desc.append("        enumObject.getEnumValues().add(active);\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(enumObject.eClass(), enumObject.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)enumObject, fqn);\n");
        desc.append("        configuration.getEnums().add(enumObject);\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Notes\n");
        desc.append("- Add at least one `EnumValue` immediately to reduce validation issues\n");
        desc.append("- Set UUID on the enum and on each enum value\n");
        desc.append("- For edits, mutate `enumObject.getEnumValues()` directly instead of recreating the enum\n");
        return desc.toString();
    }

    private String buildCommonModuleWorkflow()
    {
        var desc = new StringBuilder();
        desc.append(buildTopLevelCreateWorkflow(
            "CommonModule", "CommonModule", "commonModule", "createCommonModule()", "getCommonModules()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "commonModule.setServer(true);\n        commonModule.setServerCall(true);", //$NON-NLS-1$
            "After metadata creation, create or update the corresponding Module.bsl file through file tools. Metadata flags and BSL text are separate layers.")); //$NON-NLS-1$
        return desc.toString();
    }

    private String buildChartOfAccountsWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "ChartOfAccounts", "ChartOfAccounts", "chart", "createChartOfAccounts()", "getChartsOfAccounts()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "chart.setCodeLength(10);\n        chart.setDescriptionLength(100);", //$NON-NLS-1$
            "ChartOfAccounts is commonly referenced by AccountingRegister. Create the chart first if registers depend on it."); //$NON-NLS-1$
    }

    private String buildChartOfCalculationTypesWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "ChartOfCalculationTypes", "ChartOfCalculationTypes", "chart", "createChartOfCalculationTypes()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "getChartsOfCalculationTypes()", "chart.setCodeLength(10);\n        chart.setDescriptionLength(100);", //$NON-NLS-1$ //$NON-NLS-2$
            "CalculationRegister frequently depends on ChartOfCalculationTypes. Create the chart before assigning register references."); //$NON-NLS-1$
    }

    private String buildBusinessProcessWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "BusinessProcess", "BusinessProcess", "businessProcess", "createBusinessProcess()", "getBusinessProcesses()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "businessProcess.setAutonumbering(true);", //$NON-NLS-1$
            "Add attributes and tabular sections using the generic attribute and tabular section scenarios."); //$NON-NLS-1$
    }

    private String buildTaskWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Task", "Task", "task", "createTask()", "getTasks()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "task.setAutonumbering(true);", //$NON-NLS-1$
            "Tasks often mirror BusinessProcess patterns. Use TaskAttribute/TaskTabularSection for child objects."); //$NON-NLS-1$
    }

    private String buildExchangePlanWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "ExchangePlan", "ExchangePlan", "exchangePlan", "createExchangePlan()", "getExchangePlans()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "exchangePlan.setCodeLength(9);", //$NON-NLS-1$
            "Exchange plans are top-level objects; node definitions and related metadata can be added afterwards."); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String buildConstantWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Constant", "Constant", "constant", "createConstant()", "getConstants()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            buildStringTypeDescription().replace("\n", "\n        ") + "\n        constant.setType(typeDesc);", //$NON-NLS-1$
            "Constants implement TypeDescriptionProvider, so `setType(...)` is mandatory."); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String buildDefinedTypeWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "DefinedType", "DefinedType", "definedType", "createDefinedType()", "getDefinedTypes()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            buildStringTypeWithQualifiersDescription().replace("\n", "\n        ") + "\n        definedType.setType(typeDesc);", //$NON-NLS-1$
            "DefinedType is usually used as a reusable alias; choose qualifiers deliberately to avoid broad runtime types."); //$NON-NLS-1$
    }

    private String buildDocumentJournalWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "DocumentJournal", "DocumentJournal", "journal", "createDocumentJournal()", "getDocumentJournals()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "journal.setName(\"DocumentsJournal\");", //$NON-NLS-1$
            "DocumentJournal collects document views; related columns and commands can be configured later."); //$NON-NLS-1$
    }

    private String buildDocumentNumeratorWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "DocumentNumerator", "DocumentNumerator", "numerator", "createDocumentNumerator()", "getDocumentNumerators()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "numerator.setName(\"MainNumerator\");", //$NON-NLS-1$
            "Attach documents to the numerator later through document properties inside a BM transaction."); //$NON-NLS-1$
    }

    private String buildReportWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Report", "Report", "report", "createReport()", "getReports()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "report.setName(\"SalesAnalysis\");", //$NON-NLS-1$
            "Metadata object creation does not create layouts, forms, or module code automatically."); //$NON-NLS-1$
    }

    private String buildDataProcessorWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "DataProcessor", "DataProcessor", "processor", "createDataProcessor()", "getDataProcessors()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "processor.setName(\"DataMaintenance\");", //$NON-NLS-1$
            "For executable behavior, add forms or module content separately after metadata creation."); //$NON-NLS-1$
    }

    private String buildWebServiceWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "WebService", "WebService", "service", "createWebService()", "getWebServices()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "service.setName(\"OrderService\");", //$NON-NLS-1$
            "Operations and parameters are child objects. Create the service first, then add `Operation` and `Parameter` children."); //$NON-NLS-1$
    }

    private String buildHttpServiceWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "HTTPService", "HTTPService", "service", "createHTTPService()", "getHttpServices()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "service.setName(\"OrdersApi\");", //$NON-NLS-1$
            "URL templates and HTTP methods are child objects. Keep routing details in follow-up steps."); //$NON-NLS-1$
    }

    private String buildIntegrationServiceWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "IntegrationService", "IntegrationService", "service", "createIntegrationService()", "getIntegrationServices()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "service.setName(\"ERPIntegration\");", //$NON-NLS-1$
            "Add `IntegrationServiceChannel` objects after the parent service is attached to configuration."); //$NON-NLS-1$
    }

    private String buildCommonFormWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "CommonForm", "CommonForm", "form", "createCommonForm()", "getCommonForms()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "form.setName(\"UniversalSearchForm\");", //$NON-NLS-1$
            "Form structure and controls are separate layers. Start by creating the top-level metadata object."); //$NON-NLS-1$
    }

    private String buildCommonCommandWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "CommonCommand", "CommonCommand", "command", "createCommonCommand()", "getCommonCommands()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "command.setName(\"OpenDashboard\");", //$NON-NLS-1$
            "Command groups and UI placement are configured separately after command creation."); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String buildCommonAttributeWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "CommonAttribute", "CommonAttribute", "attribute", "createCommonAttribute()", "getCommonAttributes()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            buildStringTypeDescription().replace("\n", "\n        ") + "\n        attribute.setType(typeDesc);", //$NON-NLS-1$
            "CommonAttribute extends BasicFeature, so assign `TypeDescription` before adding it to configuration."); //$NON-NLS-1$
    }

    private String buildCommonTemplateWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "CommonTemplate", "CommonTemplate", "template", "createCommonTemplate()", "getCommonTemplates()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "template.setName(\"PrintLayout\");", //$NON-NLS-1$
            "Template binary or text content is maintained separately after the metadata object exists."); //$NON-NLS-1$
    }

    private String buildCommonPictureWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "CommonPicture", "CommonPicture", "picture", "createCommonPicture()", "getCommonPictures()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "picture.setName(\"Logo\");", //$NON-NLS-1$
            "After creation, assign the actual picture resource or content through the corresponding picture APIs."); //$NON-NLS-1$
    }

    private String buildInterfaceWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Interface", "Interface", "interfaceObject", "createInterface()", "getInterfaces()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "interfaceObject.setName(\"MainInterface\");", //$NON-NLS-1$
            "Configure sections, command interface, and navigation structure after the top-level interface is attached."); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String buildSessionParameterWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "SessionParameter", "SessionParameter", "parameter", "createSessionParameter()", "getSessionParameters()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            buildStringTypeDescription().replace("\n", "\n        ") + "\n        parameter.setType(typeDesc);", //$NON-NLS-1$
            "SessionParameter implements TypeDescriptionProvider, so set `type` before the object is used in session logic."); //$NON-NLS-1$
    }

    private String buildWSReferenceWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "WSReference", "WSReference", "reference", "createWSReference()", "getWsReferences()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "reference.setName(\"ExternalSoapService\");", //$NON-NLS-1$
            "After creation, fill endpoint and service metadata according to the referenced WSDL contract."); //$NON-NLS-1$
    }

    private String buildEventSubscriptionWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "EventSubscription", "EventSubscription", "subscription", "createEventSubscription()", "getEventSubscriptions()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "subscription.setName(\"OnDocumentPost\");", //$NON-NLS-1$
            "After creation, configure source object, event, and handler module explicitly."); //$NON-NLS-1$
    }

    private String buildScheduledJobWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "ScheduledJob", "ScheduledJob", "job", "createScheduledJob()", "getScheduledJobs()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "job.setName(\"NightlyCleanup\");", //$NON-NLS-1$
            "Add schedule details and called method configuration after the top-level job object exists."); //$NON-NLS-1$
    }

    private String buildFilterCriterionWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "FilterCriterion", "FilterCriterion", "criterion", "createFilterCriterion()", "getFilterCriteria()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            buildCatalogRefTypeDescription().replace("\\n", "\\n        ") + "\\n        criterion.setType(typeDesc);", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "FilterCriterion implements TypeDescriptionProvider. Make the type narrow and intentional."); //$NON-NLS-1$
    }

    private String buildSettingsStorageWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "SettingsStorage", "SettingsStorage", "storage", "createSettingsStorage()", "getSettingsStorages()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "storage.setName(\"CommonSettingsStorage\");", //$NON-NLS-1$
            "Configuration references such as commonSettingsStorage or reportsVariantsStorage should point to this object afterwards."); //$NON-NLS-1$
    }

    private String buildFunctionalOptionWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "FunctionalOption", "FunctionalOption", "option", "createFunctionalOption()", "getFunctionalOptions()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "option.setName(\"UseAdvancedPricing\");", //$NON-NLS-1$
            "Bind commands, forms, or object availability to the option after it is created."); //$NON-NLS-1$
    }

    private String buildFunctionalOptionsParameterWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "FunctionalOptionsParameter", "FunctionalOptionsParameter", "parameter", "createFunctionalOptionsParameter()", "getFunctionalOptionsParameters()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "parameter.setName(\"CurrentCompany\");", //$NON-NLS-1$
            "After creation, link the parameter to functional options and configure dependent expressions."); //$NON-NLS-1$
    }

    private String buildSubsystemWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Subsystem", "Subsystem", "subsystem", "createSubsystem()", "getSubsystems()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "subsystem.setName(\"Sales\");", //$NON-NLS-1$
            "Configuration keeps subsystem references; command interfaces and object composition are follow-up steps."); //$NON-NLS-1$
    }

    private String buildRoleWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Role", "Role", "role", "createRole()", "getRoles()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "role.setName(\"PowerUser\");", //$NON-NLS-1$
            "Rights matrices and permissions are configured after the role object exists."); //$NON-NLS-1$
    }

    private String buildCommandGroupWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "CommandGroup", "CommandGroup", "group", "createCommandGroup()", "getCommandGroups()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "group.setName(\"SalesCommands\");", //$NON-NLS-1$
            "Use command groups to organize common commands and UI navigation after the group object exists."); //$NON-NLS-1$
    }

    private String buildLanguageWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Language", "Language", "language", "createLanguage()", "getLanguages()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "language.setName(\"English\");", //$NON-NLS-1$
            "After creation, set configuration default language and local string entries appropriately."); //$NON-NLS-1$
    }

    private String buildExternalDataSourceWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "ExternalDataSource", "ExternalDataSource", "source", "createExternalDataSource()", "getExternalDataSources()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "source.setName(\"WarehouseDwh\");", //$NON-NLS-1$
            "Tables, cubes, fields, dimensions, and resources are child objects created after the source."); //$NON-NLS-1$
    }

    private String buildStyleWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Style", "Style", "style", "createStyle()", "getStyles()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "style.setName(\"CorporateStyle\");", //$NON-NLS-1$
            "After creation, assign the style in configuration defaults or reference it from forms and UI elements."); //$NON-NLS-1$
    }

    private String buildStyleItemWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "StyleItem", "StyleItem", "styleItem", "createStyleItem()", "getStyleItems()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "styleItem.setName(\"PrimaryButton\");", //$NON-NLS-1$
            "StyleItem visual type and concrete appearance settings should be configured explicitly after creation."); //$NON-NLS-1$
    }

    private String buildSequenceWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "Sequence", "Sequence", "sequence", "createSequence()", "getSequences()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "sequence.setName(\"DocumentsSequence\");", //$NON-NLS-1$
            "After sequence creation, add dimensions and connect participating objects through sequence settings."); //$NON-NLS-1$
    }

    private String buildXDTOPackageWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "XDTOPackage", "XDTOPackage", "packageObject", "createXDTOPackage()", "getXDTOPackages()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "packageObject.setName(\"CommonSchema\");", //$NON-NLS-1$
            "Add XDTO package content and imported types after the package metadata object exists."); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String buildChartOfCharacteristicTypesWorkflow()
    {
        return buildTopLevelCreateWorkflow(
            "ChartOfCharacteristicTypes", "ChartOfCharacteristicTypes", "chart", "createChartOfCharacteristicTypes()", "getChartsOfCharacteristicTypes()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "chart.setCodeLength(10);\n        chart.setDescriptionLength(100);\n        "
                + buildStringTypeDescription().replace("\n", "\n        ") + "\n        chart.setType(typeDesc);", //$NON-NLS-1$
            "ChartOfCharacteristicTypes requires a value `TypeDescription`; replace the sample string type with the actual allowed characteristic value types."); //$NON-NLS-1$
    }

    private String buildBotWorkflowSafe()
    {
        return buildTopLevelCreateWorkflow(
            "Bot", "Bot", "bot", "createBot()", "getBots()", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "bot.setName(\"SupportBot\");", //$NON-NLS-1$
            "Conversation scenarios, commands, and integration endpoints are configured after the bot metadata object is attached."); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String buildTableWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create Table inside ExternalDataSource\n\n");
        desc.append("### Parent resolution\n");
        desc.append("- Load the parent `ExternalDataSource` from transaction by FQN\n");
        desc.append("- Create `Table` inside the same BM transaction\n");
        desc.append("- Add it to `externalDataSource.getTables()`\n\n");
        desc.append("### Example\n");
        desc.append("```java\n");
        desc.append("ExternalDataSource source = (ExternalDataSource)transaction.getTopObjectByFqn(\"ExternalDataSource.WarehouseDwh\");\n");
        desc.append("Table table = mdFactory.createTable();\n");
        desc.append("table.setName(\"Products\");\n");
        desc.append("table.setUuid(UUID.randomUUID());\n");
        desc.append("source.getTables().add(table);\n");
        desc.append("```\n\n");
        desc.append("### Notes\n");
        desc.append("- Child objects inside ExternalDataSource are usually not attached as standalone top-level objects\n");
        desc.append("- Add fields and commands after the table exists\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCubeWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create Cube inside ExternalDataSource\n\n");
        desc.append("### Parent resolution\n");
        desc.append("- Load the parent `ExternalDataSource` from transaction by FQN\n");
        desc.append("- Create `Cube` inside the same BM transaction\n");
        desc.append("- Add it to `externalDataSource.getCubes()`\n\n");
        desc.append("### Example\n");
        desc.append("```java\n");
        desc.append("ExternalDataSource source = (ExternalDataSource)transaction.getTopObjectByFqn(\"ExternalDataSource.WarehouseDwh\");\n");
        desc.append("Cube cube = mdFactory.createCube();\n");
        desc.append("cube.setName(\"SalesCube\");\n");
        desc.append("cube.setUuid(UUID.randomUUID());\n");
        desc.append("source.getCubes().add(cube);\n");
        desc.append("```\n\n");
        desc.append("### Notes\n");
        desc.append("- Add dimensions, resources, functions, and dimension tables after cube creation\n");
        desc.append("- Keep TypeDescription on child objects explicit to avoid validation noise\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCreateAttributeForEntityWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Scenario: Create Attribute For Entity\n\n");
        desc.append("### Correct child types\n");
        desc.append("- `Catalog` -> `CatalogAttribute`\n");
        desc.append("- `Document` -> `DocumentAttribute`\n");
        desc.append("- `BusinessProcess` -> `BusinessProcessAttribute`\n");
        desc.append("- `Task` -> `TaskAttribute`\n");
        desc.append("- registers -> specific register attribute class\n");
        desc.append("- tabular section -> `TabularSectionAttribute`\n\n");
        desc.append("### Example pattern\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("CatalogAttribute article = mdFactory.createCatalogAttribute();\n");
        desc.append("article.setName(\"Article\");\n");
        desc.append("article.setUuid(UUID.randomUUID());\n");
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n")).append("\n");
        desc.append("article.setType(typeDesc);\n");
        desc.append("catalog.getAttributes().add(article);\n");
        desc.append("```\n\n");
        desc.append("### Safe checklist\n");
        desc.append("1. Choose the exact child class for the parent (`CatalogAttribute`, `DocumentAttribute`, `TabularSectionAttribute`, ...)\n");
        desc.append("2. Set `name` and `uuid` on the new child object\n");
        desc.append("3. Resolve `IEObjectProvider` INSIDE the current transaction\n");
        desc.append("4. Build `TypeDescription` BEFORE adding the object to the parent collection\n");
        desc.append("5. Call `setType(typeDesc)` on every object derived from `BasicFeature`\n");
        desc.append("6. Only after `setType(...)` add the object to `getAttributes()` / `getDimensions()` / `getResources()`\n\n");
        desc.append("### Wrong vs correct\n");
        desc.append("```java\n");
        desc.append("// WRONG: adding BasicFeature child without type\n");
        desc.append("DocumentAttribute counterparty = mdFactory.createDocumentAttribute();\n");
        desc.append("counterparty.setName(\"Counterparty\");\n");
        desc.append("counterparty.setUuid(UUID.randomUUID());\n");
        desc.append("document.getAttributes().add(counterparty); // md-legacy-emf-check: type is required\n\n");
        desc.append("// CORRECT: build and assign TypeDescription first\n");
        desc.append("DocumentAttribute counterparty = mdFactory.createDocumentAttribute();\n");
        desc.append("counterparty.setName(\"Counterparty\");\n");
        desc.append("counterparty.setUuid(UUID.randomUUID());\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("TypeDescription counterpartyType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(catalogRefType)\n");
        desc.append("    .build();\n");
        desc.append("counterparty.setType(counterpartyType);\n");
        desc.append("document.getAttributes().add(counterparty);\n");
        desc.append("```\n\n");
        desc.append("### Tabular section example\n");
        desc.append("```java\n");
        desc.append("TabularSectionAttribute quantity = mdFactory.createTabularSectionAttribute();\n");
        desc.append("quantity.setName(\"Quantity\");\n");
        desc.append("quantity.setUuid(UUID.randomUUID());\n");
        desc.append("TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("TypeDescription quantityType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(numberType)\n");
        desc.append("    .build();\n");
        desc.append("quantity.setType(quantityType);\n");
        desc.append("products.getAttributes().add(quantity);\n");
        desc.append("```\n\n");
        desc.append("### Recovery pattern for real error (`Catalog.Авторы` -> `Страна`)\n");
        desc.append("```java\n");
        desc.append("Catalog authors = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Авторы\");\n");
        desc.append("if (authors != null) {\n");
        desc.append("    IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("        .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("    CatalogAttribute country = authors.getAttributes().stream()\n");
        desc.append("        .filter(a -> \"Страна\".equals(a.getName()))\n");
        desc.append("        .findFirst()\n");
        desc.append("        .orElse(null);\n");
        desc.append("    if (country == null) {\n");
        desc.append("        country = mdFactory.createCatalogAttribute();\n");
        desc.append("        country.setName(\"Страна\");\n");
        desc.append("        country.setUuid(UUID.randomUUID());\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription countryType = new TypeDescriptionBuilder().addType(stringType).build();\n");
        desc.append("        country.setType(countryType);\n");
        desc.append("        authors.getAttributes().add(country);\n");
        desc.append("    } else if (country.getType() == null || country.getType().getTypes().isEmpty()) {\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription countryType = new TypeDescriptionBuilder().addType(stringType).build();\n");
        desc.append("        country.setType(countryType);\n");
        desc.append("    }\n");
        desc.append("}\n");
        desc.append("```\n\n");
        desc.append("### Rules\n");
        desc.append("- Always choose the child class that matches the parent entity\n");
        desc.append("- Every attribute derived from `BasicFeature` must have `setType(...)` before it is added to the parent collection\n");
        desc.append("- `CatalogAttribute`, `DocumentAttribute`, and `TabularSectionAttribute` are the most common sources of `md-legacy-emf-check` when `type` is omitted\n");
        desc.append("- For child objects, UUID is still recommended in JShell\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCreateTypeDescriptionWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Scenario: Create TypeDescription\n\n");
        desc.append("### String\n");
        desc.append(buildStringTypeDescription()).append("\n");
        desc.append("### Number\n");
        desc.append(buildNumberTypeDescription()).append("\n");
        desc.append("### String with qualifiers\n");
        desc.append(buildStringTypeWithQualifiersDescription()).append("\n");
        desc.append("### Number with qualifiers\n");
        desc.append(buildNumberTypeWithQualifiersDescription()).append("\n");
        desc.append("### Catalog reference\n");
        desc.append(buildCatalogRefTypeDescription()).append("\n");
        desc.append("### Document reference\n");
        desc.append(buildDocumentRefTypeDescription()).append("\n");
        desc.append("### Enum reference\n");
        desc.append(buildEnumRefTypeDescription()).append("\n");
        desc.append("### Validate proxy before addType\n");
        desc.append("```java\n");
        desc.append("TypeItem unitsRef = (TypeItem)typeProvider.getProxy(\"Catalog.Units\");\n");
        desc.append("if (unitsRef == null) {\n");
        desc.append("    System.err.println(\"ERROR: Cannot resolve type proxy Catalog.Units\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("TypeDescription unitsType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(unitsRef)\n");
        desc.append("    .build();\n");
        desc.append("```\n");
        desc.append("### Rules\n");
        desc.append("- Prefer a specific proxy like `Catalog.Products` when the business rule is narrow\n");
        desc.append("- Use generic IEObjectTypeNames only when polymorphism is desired\n");
        desc.append("- Build the type before assigning it to attributes, dimensions, resources, constants, or defined types\n");
        desc.append("- Always validate `typeProvider.getProxy(...)` before `addType(...)`; `null` causes `IllegalArgumentException`\n");
        desc.append("- Specific references only work for metadata objects that already exist and are visible to the current transaction\n");
        desc.append("- When a specific proxy is unavailable, fall back to a generic type like `IEObjectTypeNames.CATALOG_REF`\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildResolveTopObjectWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Scenario: Resolve Top Object And Parent Collection\n\n");
        desc.append("### Top-level lookup pattern\n");
        desc.append("```java\n");
        desc.append("Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("```\n\n");
        desc.append("### Parent collection rules\n");
        desc.append("- top-level objects go to `Configuration` collections like `getCatalogs()` or `getDocuments()`\n");
        desc.append("- child objects go to the owning object collection like `catalog.getAttributes()`\n");
        desc.append("- do not attach child objects as top-level objects\n\n");
        desc.append("### FQN rules\n");
        desc.append("- new top-level objects: generate FQN with `fqnGenerator`\n");
        desc.append("- existing objects: load by known FQN and mutate in-place\n");
        desc.append("- do not call `attachTopObject()` on objects already present in the model\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteMetadataObjectWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Scenario: Delete Metadata Object\n\n");
        desc.append("### Safe pattern\n");
        desc.append("```java\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete object\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            configuration.getCatalogs().remove(catalog);\n");
        desc.append("            transaction.detachTopObject((IBmObject)catalog);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Rules\n");
        desc.append("- remove top-level objects from the correct `Configuration` collection first\n");
        desc.append("- then call `transaction.detachTopObject(...)`\n");
        desc.append("- do not use `EcoreUtil.delete()` for top-level metadata objects\n");
        desc.append("- for child objects, remove them from the owning collection instead of detaching top-level state\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildGenericEditWorkflow(String typeName, String sampleFqn, String editSnippet, String notes)
    {
        var varName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Edit ").append(typeName).append("\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Edit ").append(typeName).append("\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        ").append(typeName).append(" ").append(varName).append(" = (").append(typeName)
            .append(")transaction.getTopObjectByFqn(\"").append(sampleFqn).append("\");\n");
        desc.append("        if (").append(varName).append(" != null) {\n");
        desc.append("        ").append(editSnippet.replace("\n", "\n        ")).append("\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Notes\n");
        desc.append("- Load the existing object by FQN from the transaction\n");
        desc.append("- Do not recreate or reattach the object\n");
        desc.append("- ").append(notes).append("\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildTopLevelCreateWorkflow(String title, String typeName, String variableName, String createMethod,
        String configurationCollectionAccessor, String setupSnippet, String notes)
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create ").append(title).append("\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create ").append(title).append("\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        ").append(typeName).append(" ").append(variableName).append(" = mdFactory.")
            .append(createMethod).append(";\n");
        desc.append("        ").append(variableName).append(".setName(\"").append(title).append("Sample\");\n");
        desc.append("        ").append(variableName).append(".setUuid(UUID.randomUUID());\n");
        if (setupSnippet != null && !setupSnippet.isBlank())
        {
            desc.append("        ").append(setupSnippet.replace("\n", "\n        ")).append("\n");
        }
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(").append(variableName)
            .append(".eClass(), ").append(variableName).append(".getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)").append(variableName).append(", fqn);\n");
        desc.append("        configuration.").append(configurationCollectionAccessor).append(".add(").append(variableName)
            .append(");\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Rules\n");
        desc.append("- Create the object only inside BM transaction\n");
        desc.append("- Set UUID before attach\n");
        desc.append("- Generate FQN with `fqnGenerator`\n");
        desc.append("- Add the object to `Configuration.").append(configurationCollectionAccessor).append("`\n\n");
        desc.append("### Notes\n");
        desc.append("- ").append(notes).append("\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildAccumulationRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create AccumulationRegister\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append(
            "AccumulationRegister register = globalContext.execute(new AbstractBmTask<AccumulationRegister>(\"Create register\") {\n");
        desc.append("    @Override\n");
        desc.append(
            "    public AccumulationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append(
            "        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        AccumulationRegister register = mdFactory.createAccumulationRegister();\n");
        desc.append("        register.setName(\"GoodsInStock\");\n");
        desc.append("        register.getSynonym().put(\"ru\", \"Goods In Stock\");\n");
        desc.append("        register.setRegisterType(AccumulationRegisterType.BALANCE);\n");
        desc.append("\n");
        desc.append("        // Add dimension\n");
        desc.append(
            "        AccumulationRegisterDimension warehouse = mdFactory.createAccumulationRegisterDimension();\n");
        desc.append("        warehouse.setName(\"Warehouse\");\n");
        desc.append("        warehouse.getSynonym().put(\"ru\", \"Warehouse\");\n");
        desc.append("\n");
        desc.append("        ").append(buildCatalogRefTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        warehouse.setType(typeDesc);\n");
        desc.append("        register.getDimensions().add(warehouse);\n");
        desc.append("\n");
        desc.append("        // Add resource\n");
        desc.append(
            "        AccumulationRegisterResource quantity = mdFactory.createAccumulationRegisterResource();\n");
        desc.append("        quantity.setName(\"Quantity\");\n");
        desc.append("        quantity.getSynonym().put(\"ru\", \"Quantity\");\n");
        desc.append("\n");
        desc.append("        // Set numeric type for resource\n");
        desc.append("        ").append(buildNumberTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        quantity.setType(typeDesc);\n");
        desc.append("        register.getResources().add(quantity);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs manually (RECOMMENDED for JShell)\n");
        desc.append("        register.setUuid(UUID.randomUUID());\n");
        desc.append("        warehouse.setUuid(UUID.randomUUID());\n");
        desc.append("        quantity.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append(
            "        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)register, fqn);\n");
        desc.append("        configuration.getAccumulationRegisters().add(register);\n");
        desc.append("        return register;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**Note:** Registers require at least one Dimension. Resources are optional but recommended.\n");
        desc.append("**Note:** `AccumulationRegisterDimension` does not have `setBalance(...)`; do not call it in JShell examples.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildAccountingRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create AccountingRegister\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append(
            "AccountingRegister register = globalContext.execute(new AbstractBmTask<AccountingRegister>(\"Create register\") {\n");
        desc.append("    @Override\n");
        desc.append("    public AccountingRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append(
            "        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        AccountingRegister register = mdFactory.createAccountingRegister();\n");
        desc.append("        register.setName(\"Accounting\");\n");
        desc.append("        register.getSynonym().put(\"ru\", \"Accounting\");\n");
        desc.append("        register.setCorrespondence(true);\n");
        desc.append("        register.setPeriodAdjustmentLength(2);\n");
        desc.append("\n");
        desc.append("        // Set ChartOfAccounts reference\n");
        desc.append(
            "        ChartOfAccounts chartOfAccounts = (ChartOfAccounts)transaction.getTopObjectByFqn(\"ChartOfAccounts.ПланСчетов\");\n");
        desc.append("        register.setChartOfAccounts(chartOfAccounts);\n");
        desc.append("\n");
        desc.append("        // Add dimension\n");
        desc.append("        AccountingRegisterDimension account = mdFactory.createAccountingRegisterDimension();\n");
        desc.append("        account.setName(\"Account\");\n");
        desc.append("        account.getSynonym().put(\"ru\", \"Account\");\n");
        desc.append("        account.setBalance(true);\n");
        desc.append("\n");
        desc.append("        ").append(buildChartOfAccountsRefTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        account.setType(typeDesc);\n");
        desc.append("        register.getDimensions().add(account);\n");
        desc.append("\n");
        desc.append("        // Add resource\n");
        desc.append("        AccountingRegisterResource amount = mdFactory.createAccountingRegisterResource();\n");
        desc.append("        amount.setName(\"Amount\");\n");
        desc.append("        amount.getSynonym().put(\"ru\", \"Amount\");\n");
        desc.append("        amount.setBalance(true);\n");
        desc.append("\n");
        desc.append("        ").append(buildNumberTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        amount.setType(typeDesc);\n");
        desc.append("        register.getResources().add(amount);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs manually (RECOMMENDED for JShell)\n");
        desc.append("        register.setUuid(UUID.randomUUID());\n");
        desc.append("        account.setUuid(UUID.randomUUID());\n");
        desc.append("        amount.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append(
            "        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)register, fqn);\n");
        desc.append("        configuration.getAccountingRegisters().add(register);\n");
        desc.append("        return register;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append(
            "**Note:** AccountingRegister requires ChartOfAccounts reference and at least one Dimension with account reference type.\n");
        desc.append("**Note:** `AccountingRegisterDimension` and `AccountingRegisterResource` support `setBalance(boolean)` in EDT API.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCalculationRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create CalculationRegister\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append(
            "CalculationRegister register = globalContext.execute(new AbstractBmTask<CalculationRegister>(\"Create register\") {\n");
        desc.append("    @Override\n");
        desc.append(
            "    public CalculationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append(
            "        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        CalculationRegister register = mdFactory.createCalculationRegister();\n");
        desc.append("        register.setName(\"SalaryCalculation\");\n");
        desc.append("        register.getSynonym().put(\"ru\", \"Salary Calculation\");\n");
        desc.append("        register.setPeriodicity(CalculationRegisterPeriodicity.MONTH);\n");
        desc.append("        register.setActionPeriod(true);\n");
        desc.append("        register.setBasePeriod(false);\n");
        desc.append("\n");
        desc.append("        // Set ChartOfCalculationTypes reference\n");
        desc.append(
            "        ChartOfCalculationTypes chart = (ChartOfCalculationTypes)transaction.getTopObjectByFqn(\"ChartOfCalculationTypes.ВидыРасчетов\");\n");
        desc.append("        register.setChartOfCalculationTypes(chart);\n");
        desc.append("\n");
        desc.append("        // Add dimension (base dimension)\n");
        desc.append(
            "        CalculationRegisterDimension employee = mdFactory.createCalculationRegisterDimension();\n");
        desc.append("        employee.setName(\"Employee\");\n");
        desc.append("        employee.getSynonym().put(\"ru\", \"Employee\");\n");
        desc.append("        employee.setBaseDimension(true);\n");
        desc.append("\n");
        desc.append("        ").append(buildCatalogRefTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        employee.setType(typeDesc);\n");
        desc.append("        register.getDimensions().add(employee);\n");
        desc.append("\n");
        desc.append("        // Add resource\n");
        desc.append("        CalculationRegisterResource amount = mdFactory.createCalculationRegisterResource();\n");
        desc.append("        amount.setName(\"Amount\");\n");
        desc.append("        amount.getSynonym().put(\"ru\", \"Amount\");\n");
        desc.append("\n");
        desc.append("        ").append(buildNumberTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        amount.setType(typeDesc);\n");
        desc.append("        register.getResources().add(amount);\n");
        desc.append("\n");
        desc.append("        // Add recalculation rule\n");
        desc.append("        Recalculation recalculation = mdFactory.createRecalculation();\n");
        desc.append("        recalculation.setName(\"Recalculation\");\n");
        desc.append("        register.getRecalculations().add(recalculation);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs manually (RECOMMENDED for JShell)\n");
        desc.append("        register.setUuid(UUID.randomUUID());\n");
        desc.append("        employee.setUuid(UUID.randomUUID());\n");
        desc.append("        amount.setUuid(UUID.randomUUID());\n");
        desc.append("        recalculation.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append(
            "        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)register, fqn);\n");
        desc.append("        configuration.getCalculationRegisters().add(register);\n");
        desc.append("        return register;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append(
            "**Note:** CalculationRegister requires ChartOfCalculationTypes reference and at least one base Dimension.\n");
        desc.append("**Note:** Use a numeric type for calculation resources such as amount; do not reuse a reference type from a dimension.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildApiCompatibilityNotes()
    {
        var desc = new StringBuilder();
        desc.append("## API Compatibility Notes\n\n");
        desc.append("### ⚠️ CRITICAL RULES - Follow These to Avoid Failures\n\n");

        desc.append("#### Transaction Management\n\n");
        desc.append("**✅ REQUIRED:**\n");
        desc.append("- Use `globalContext.execute(new AbstractBmTask<...>(\"Task name\") { ... })` for ALL read/write operations\n");
        desc.append("- Access `IBmTransaction` parameter in `execute()` method for metadata operations\n");
        desc.append("- Use `getTopObjectByFqn()` to READ existing objects\n");
        desc.append("- Use `mdFactory.createXxx()` + `attachTopObject()` to CREATE new objects\n");
        desc.append("- Modify existing objects directly (no `attachTopObject()` needed)\n\n");

        desc.append("**❌ PROHIBITED:**\n");
        desc.append("- Do NOT use `executeReadonlyTask(...)` for metadata creation/modification\n");
        desc.append("- Do NOT override final methods `getId()` / `getServiceId()` in `AbstractBmTask`\n");
        desc.append("- Do NOT use `attachTopObject()` on existing objects (causes `BmFqnAlreadyInUseException`)\n\n");

        desc.append("#### Version and Type Handling\n\n");
        desc.append("- Use `v8project.getVersion()` (returns `Version` object), NOT `getRuntimeVersion()`\n");
        desc.append("- Localized fields (`synonym`, `comment`, `toolTip`) are `EMap<String, String>`: use `put(\"ru\", \"...\")`\n");
        desc.append("- Type qualifiers (StringQualifiers, NumberQualifiers) are ABSTRACT classes - CANNOT instantiate directly\n");
        desc.append("- For type handling: use `TypeDescriptionBuilder` WITHOUT qualifiers or use default types\n\n");
        desc.append("- If the new child object extends `BasicFeature`, treat `setType(...)` as mandatory, not optional\n\n");
        desc.append("- Before finishing transaction, verify every new `BasicFeature` has non-null/non-empty `type` to avoid `MdTypeSetInferrer` NPE during derived rebuild\n\n");

        desc.append("#### Metadata Object-Specific Rules\n\n");
        desc.append("**Catalog (Справочник):**\n");
        desc.append("- Use `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS` or `HierarchyType.HIERARCHY_OF_ITEMS` (correct enum constants)\\n");
        desc.append("- Use `setDescriptionLength(...)` - `setNameLength(...)` is NOT AVAILABLE\\n");
        desc.append("- For attributes: use `CatalogAttribute` via `createCatalogAttribute()` or `modelFactory` + EClass\\n");
        desc.append("- Supports: hierarchical, codeType (Number/String), checkUnique, autonumbering\\n\\n");

        desc.append("**Document (Документ):**\n");
        desc.append("- Use `DocumentNumberType.NUMBER` or `DocumentNumberType.STRING`\\n");
        desc.append("- Use `DocumentNumberPeriodicity.NONPERIODICAL` (correct enum constant)\\n");
        desc.append("- Supports: realTimePosting, registerRecordsDeletion, sequenceFilling\\n");
        desc.append("- Do not use `setPosted(...)` - this setter is not present in EDT API\\n\\n");
        desc.append("- May reference: numerator, registerRecords (array of BasicRegister)\\n\\n");

        desc.append("**InformationRegister (РегистрСведений):**\n");
        desc.append("- Use `InformationRegisterPeriodicity.NONPERIODICAL`, `SECOND`, `DAY`, `MONTH`, `QUARTER`, `YEAR`, `RECORDER_POSITION`\n");
        desc.append("- Use setter `setInformationRegisterPeriodicity(...)` (not `setPeriodicity(...)`)\n");
        desc.append("- Use `RegisterWriteMode.INDEPENDENT` or `RegisterWriteMode.RECORDER_SUBORDINATE`\n");
        desc.append("- Use specific child factories (`createInformationRegisterDimension/Resource`); generic `createRegisterDimension/Resource` is not valid in mdFactory\n");
        desc.append("- Contains: resources, attributes, dimensions (all require types)\n\n");

        desc.append("**AccumulationRegister (РегистрНакопления):**\n");
        desc.append("- Use `AccumulationRegisterType.BALANCE` or `AccumulationRegisterType.TURNOVERS`\n");
        desc.append("- Do NOT use non-existing constants like `REMAINS` or `TURNOVER`\n");
        desc.append("- Use specific child factories: `createAccumulationRegisterDimension()` and `createAccumulationRegisterResource()`\n\n");

        desc.append("**Enum (Перечисление):**\n");
        desc.append("- Contains `EnumValue[] enumValues` - create with `createEnumValue()`\n");
        desc.append("- Use `enumObject.getEnumValues()` for collection access; do NOT use `getValues()`\n");
        desc.append("- In JShell snippets prefer fully-qualified `com._1c.g5.v8.dt.metadata.mdclass.Enum` and `EnumValue` to avoid ambiguous imports\n");
        desc.append("- Each EnumValue has: name, description, color (since 8.5.1)\n");
        desc.append("- NO attributes or tabular sections\n\n");

        desc.append("**CommonModule (ОбщийМодуль):**\n");
        desc.append("- Safe baseline flags: `setServer(true)` and `setServerCall(true)`\n");
        desc.append("- Do NOT use `setClient(...)` in EDT API snippets unless you verified the exact method exists in this version\n\n");

        desc.append("**ChartOfCharacteristicTypes (ПланВидовХарактеристик):**\n");
        desc.append("- Has its own type (TypeDescription) for characteristic values\n");
        desc.append("- Supports: hierarchical, codeSeries, checkUnique, autonumbering\n");
        desc.append("- May reference: characteristicExtValues (Catalog)\n\n");

        desc.append("**Tabular Sections:**\n");
        desc.append("- Use `TabularSectionAttribute` with `createTabularSectionAttribute()`\n");
        desc.append("- Attributes: name, synonym, type (TypeDescription), indexing, fillChecking\n");
        desc.append("- Line number length configurable (since 8.3.27)\n\n");

        desc.append("#### Factory Selection Guidelines\n\n");
        desc.append("**Prefer `mdFactory` for most operations:**\n");
        desc.append("- More reliable in JShell context (no OSGi timeout issues)\n");
        desc.append("- Simpler API for creating metadata objects\n");
        desc.append("- Consistent with 1C metadata creation patterns\n\n");

        desc.append("**Use `modelFactory` when needed:**\n");
        desc.append("- For project/version context operations\n");
        desc.append("- NOTE: `fillDefaultReferences()` may timeout in JShell due to OSGi service limitations\n");
        desc.append("- **RECOMMENDED for JShell:** Use manual UUID assignment: `object.setUuid(UUID.randomUUID())`\n\n");

        desc.append("#### Object Creation Workflow (Required Order)\n\n");
        desc.append("1. Create object with `mdFactory.createXxx()`\n");
        desc.append("2. Set required properties: name, synonym, type-specific settings\n");
        desc.append("3. Add children (attributes, tabular sections, etc.) if needed\n");
        desc.append("4. **CRITICAL for JShell:** Set UUID manually: `object.setUuid(UUID.randomUUID())`\n");
        desc.append("   - For children, set UUIDs: `childObject.setUuid(UUID.randomUUID())`\n");
        desc.append("   - NOTE: `modelFactory.fillDefaultReferences()` may timeout in JShell\n");
        desc.append("5. Generate FQN: `fqnGenerator.generateStandaloneObjectFqn(eClass(), name)`\n");
        desc.append("6. Attach: `transaction.attachTopObject((IBmObject)object, fqn)`\n");
        desc.append("7. Add to parent collection: `configuration.getXxxs().add(object)`\n\n");

        desc.append("#### Common Property Setting Patterns\n\n");
        desc.append("**Names and Synonyms:**\n");
        desc.append("```java\n");
        desc.append("object.setName(\"ObjectName\");\n");
        desc.append("object.getSynonym().put(\"ru\", \"Объект\");\n");
        desc.append("object.getComment().put(\"ru\", \"Комментарий\");\n");
        desc.append("```\n\n");

        desc.append("**Setting Types:**\n");
        desc.append("```java\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .build();\n");
        desc.append("attribute.setType(typeDesc);\n");
        desc.append("```\n\n");

        desc.append("**UUID Handling (CRITICAL):**\n");
        desc.append("```java\n");
        desc.append("// Option 1: RECOMMENDED for JShell - manual UUID assignment\n");
        desc.append("object.setUuid(UUID.randomUUID());\n");
        desc.append("// For children, also set UUIDs:\n");
        desc.append("childObject.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("// Option 2: auto-generate all UUIDs (may timeout in JShell)\n");
        desc.append("// modelFactory.fillDefaultReferences(object);\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildTypeDescriptionHandling()
    {
        var desc = new StringBuilder();
        desc.append("## TypeDescription Handling Guide\n\n");

        desc.append("### Overview\n");
        desc.append(
            "TypeDescription is the EDT API representation of 1C types for metadata attributes and properties. ");
        desc.append(
            "Use `TypeDescriptionBuilder` from `com._1c.g5.v8.dt.platform.core.typeinfo` package to create type descriptions.\n\n");

        desc.append("### Key Components\n\n");
        desc.append("**1. IEObjectProvider Registry**\n");
        desc.append("Access platform type registry for current project version:\n");
        desc.append("```java\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("```\n\n");

        desc.append("**⚠️ CRITICAL: Context Requirements**\n");
        desc.append("**MUST be created INSIDE BM transaction context:**\n");
        desc.append("- IEObjectProvider MUST use `v8project.getVersion()` from a properly initialized IV8Project\n");
        desc.append("- TypeItem proxies MUST be obtained and used within the SAME IBmTransaction\n");
        desc.append("- It is safe to reuse the already resolved `v8project`, but resolve `TypeItem` and build `TypeDescription` inside the current transaction\n");
        desc.append("- DO NOT reuse TypeDescription created in a different transaction context\n\n");
        desc.append("**Correct usage pattern:**\n");
        desc.append("```java\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create metadata\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Get typeProvider INSIDE transaction\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        \n");
        desc.append("        // Get TypeItem INSIDE transaction\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(stringType)\n");
        desc.append("            .build();\n");
        desc.append("        \n");
        desc.append("        // Set TypeDescription to attribute INSIDE transaction\n");
        desc.append("        attribute.setType(typeDesc);\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("**❌ WRONG Pattern - causes NullPointerException:**\n");
        desc.append("```java\n");
        desc.append("// Getting typeProvider OUTSIDE transaction\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create metadata\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Using TypeItem created OUTSIDE transaction context\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(stringType)\n");
        desc.append("            .build();\n");
        desc.append("        attribute.setType(typeDesc); // ❌ May cause NullPointerException\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("**2. TypeItem Proxy Retrieval**\n");
        desc.append("Get type proxy by name from `IEObjectTypeNames`:\n");
        desc.append("```java\n");
        desc.append("// Basic types\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);\n");
        desc.append("TypeItem booleanType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BOOLEAN);\n");
        desc.append("TypeItem undefinedType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UNDEFINED);\n");
        desc.append("TypeItem valueType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.VALUESTORAGE);\n");
        desc.append("TypeItem uuidType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UUID);\n");
        desc.append("\n");
        desc.append("// Primary metadata reference types\n");
        desc.append("TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("TypeItem documentRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DOCUMENT_REF);\n");
        desc.append("TypeItem enumRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ENUM_REF);\n");
        desc.append(
            "TypeItem businessProcessRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BUSINESS_PROCESS_REF);\n");
        desc.append("TypeItem taskRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.TASK_REF);\n");
        desc.append("\n");
        desc.append("// Register reference types\n");
        desc.append(
            "TypeItem accumulationRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCUMULATION_REGISTER_REF);\n");
        desc.append(
            "TypeItem accountingRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCOUNTING_REGISTER_REF);\n");
        desc.append(
            "TypeItem informationRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.INFORMATION_REGISTER_REF);\n");
        desc.append(
            "TypeItem calculationRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CALCULATION_REGISTER_REF);\n");
        desc.append("\n");
        desc.append("// Chart/Plan reference types\n");
        desc.append(
            "TypeItem chartOfAccountsRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_ACCOUNTS_REF);\n");
        desc.append(
            "TypeItem chartOfCalcTypesRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CALCULATION_TYPES_REF);\n");
        desc.append(
            "TypeItem chartOfCharTypesRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CHARACTERISTIC_TYPES_REF);\n");
        desc.append(
            "TypeItem exchangePlanRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.EXCHANGE_PLAN_REF);\n");
        desc.append("\n");
        desc.append("// Special types\n");
        desc.append("TypeItem anyRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ANY_REF);\n");
        desc.append("TypeItem characteristic = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHARACTERISTIC);\n");
        desc.append("```\n\n");

        desc.append("**3. TypeDescriptionBuilder**\n");
        desc.append("Builder pattern for creating TypeDescription:\n");
        desc.append("```java\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Multiple types (composite type)\n");
        desc.append("TypeDescription compositeType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .addType(numberType)\n");
        desc.append("    .build();\n");
        desc.append("```\n\n");

        desc.append("### Common Type Patterns\n\n");

        desc.append("**Basic Types:**\n");
        desc.append("```java\n");
        desc.append("// String type\n");
        desc.append(buildStringTypeDescription());
        desc.append("\n");

        desc.append("// String type with length qualifier (recommended for INN, codes, etc.)\n");
        desc.append(buildStringTypeWithQualifiersDescription());
        desc.append("\n");

        desc.append("// Number type\n");
        desc.append(buildNumberTypeDescription());
        desc.append("\n");

        desc.append("// Number type with precision and scale (recommended for amounts, prices, etc.)\n");
        desc.append(buildNumberTypeWithQualifiersDescription());
        desc.append("\n");

        desc.append("// Date type\n");
        desc.append(buildDateTypeDescription());
        desc.append("\n");

        desc.append("// Boolean type\n");
        desc.append(buildBooleanTypeDescription());
        desc.append("```\n\n");

        desc.append("**Reference Types:**\n");
        desc.append("```java\n");
        desc.append("// Catalog reference (generic)\n");
        desc.append(buildCatalogRefTypeDescription());
        desc.append("\n");
        desc.append("// Specific catalog reference (requires existing catalog)\n");
        desc.append("TypeItem catalogType = (TypeItem)typeProvider.getProxy(\"Catalog.Products\");\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(catalogType)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Document reference (generic)\n");
        desc.append(buildDocumentRefTypeDescription());
        desc.append("\n");
        desc.append("// Specific document reference\n");
        desc.append("TypeItem documentType = (TypeItem)typeProvider.getProxy(\"Document.GoodsReceipt\");\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(documentType)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Enum reference\n");
        desc.append(buildEnumRefTypeDescription());
        desc.append("\n");
        desc.append("// Specific enum reference\n");
        desc.append("TypeItem enumType = (TypeItem)typeProvider.getProxy(\"Enum.OrderStatus\");\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(enumType)\n");
        desc.append("    .build();\n");
        desc.append("```\n\n");

        desc.append("**Other Special Types:**\n");
        desc.append("```java\n");
        desc.append("// ValueStorage\n");
        desc.append(buildValueStorageTypeDescription());
        desc.append("\n");
        desc.append("// UUID\n");
        desc.append(buildUuidTypeDescription());
        desc.append("\n");
        desc.append("// Undefined\n");
        desc.append(buildUndefinedTypeDescription());
        desc.append("\n");
        desc.append("// Any reference (generic)\n");
        desc.append(buildAnyRefTypeDescription());
        desc.append("\n");
        desc.append("// Universal characteristic\n");
        desc.append(buildCharacteristicTypeDescription());
        desc.append("```\n\n");

        desc.append("**Register Reference Types:**\n");
        desc.append("```java\n");
        desc.append("// Accumulation register (generic)\n");
        desc.append(buildAccumulationRegisterRefTypeDescription());
        desc.append("\n");
        desc.append("// Specific accumulation register\n");
        desc.append("TypeItem accReg = (TypeItem)typeProvider.getProxy(\"AccumulationRegister.GoodsInStock\");\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(accReg)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Accounting register (generic)\n");
        desc.append(buildAccountingRegisterRefTypeDescription());
        desc.append("\n");
        desc.append("// Information register (generic)\n");
        desc.append(buildInformationRegisterRefTypeDescription());
        desc.append("\n");
        desc.append("// Calculation register (generic)\n");
        desc.append(buildCalculationRegisterRefTypeDescription());
        desc.append("```\n\n");

        desc.append("**Chart/Plan Reference Types:**\n");
        desc.append("```java\n");
        desc.append("// Chart of accounts (generic)\n");
        desc.append(buildChartOfAccountsRefTypeDescription());
        desc.append("\n");
        desc.append("// Specific chart of accounts\n");
        desc.append("TypeItem coa = (TypeItem)typeProvider.getProxy(\"ChartOfAccounts.ПланСчетов\");\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(coa)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Chart of calculation types (generic)\n");
        desc.append(buildChartOfCalcTypesRefTypeDescription());
        desc.append("\n");
        desc.append("// Chart of characteristic types (generic)\n");
        desc.append(buildChartOfCharTypesRefTypeDescription());
        desc.append("\n");
        desc.append("// Exchange plan (generic)\n");
        desc.append(buildExchangePlanRefTypeDescription());
        desc.append("\n");
        desc.append("// Business process (generic)\n");
        desc.append(buildBusinessProcessRefTypeDescription());
        desc.append("\n");
        desc.append("// Task (generic)\n");
        desc.append(buildTaskRefTypeDescription());
        desc.append("```\n\n");

        desc.append("### All Available IEObjectTypeNames Constants\n\n");
        desc.append("**Basic Types:**\n");
        desc.append("- `UNDEFINED` - Неопределено\n");
        desc.append("- `NULL` - Null value\n");
        desc.append("- `BOOLEAN` - Булево\n");
        desc.append("- `NUMBER` - Число\n");
        desc.append("- `STRING` - Строка\n");
        desc.append("- `DATE` - Дата\n");
        desc.append("- `TYPE` - Тип\n");
        desc.append("- `VALUESTORAGE` - ХранилищеЗначения\n");
        desc.append("- `UUID` - UUID/GUID\n");
        desc.append("- `BINARY_DATA` - ДвоичныеДанные\n\n");

        desc.append("**Primary Metadata Reference Types:**\n");
        desc.append("- `CATALOG_REF` - СправочникСсылка\n");
        desc.append("- `CATALOG_OBJ` - СправочникОбъект\n");
        desc.append("- `DOCUMENT_REF` - ДокументСсылка\n");
        desc.append("- `DOCUMENT_OBJ` - ДокументОбъект\n");
        desc.append("- `ENUM_REF` - ПеречислениеСсылка\n");
        desc.append("- `BUSINESS_PROCESS_REF` - БизнесПроцессСсылка\n");
        desc.append("- `TASK_REF` - ЗадачаСсылка\n\n");

        desc.append("**Register Reference Types:**\n");
        desc.append("- `ACCUMULATION_REGISTER_REF` - РегистрНакопленияСсылка\n");
        desc.append("- `ACCOUNTING_REGISTER_REF` - РегистрБухгалтерииСсылка\n");
        desc.append("- `INFORMATION_REGISTER_REF` - РегистрСведенийСсылка\n");
        desc.append("- `CALCULATION_REGISTER_REF` - РегистрРасчетаСсылка\n\n");

        desc.append("**Chart/Plan Reference Types:**\n");
        desc.append("- `CHART_OF_ACCOUNTS_REF` - ПланСчетовСсылка\n");
        desc.append("- `CHART_OF_CALCULATION_TYPES_REF` - ПланВидовРасчетаСсылка\n");
        desc.append("- `CHART_OF_CHARACTERISTIC_TYPES_REF` - ПланВидовХарактеристикСсылка\n");
        desc.append("- `EXCHANGE_PLAN_REF` - ПланОбменаСсылка\n\n");

        desc.append("**Special Types:**\n");
        desc.append("- `ANY_REF` - ЛюбаяСсылка (generic reference)\n");
        desc.append("- `CHARACTERISTIC` - Характеристика (universal)\n");
        desc.append("- `DEFINED_TYPE` - DefinedType (user-defined)\n\n");

        desc.append("### Specific Metadata Type References\n\n");
        desc.append("To reference specific metadata objects, use fully qualified names (FQN):\n");
        desc.append("```java\n");
        desc.append("// Specific catalog\n");
        desc.append("TypeItem productsRef = (TypeItem)typeProvider.getProxy(\"Catalog.Products\");\n");
        desc.append("TypeItem nomenclatureRef = (TypeItem)typeProvider.getProxy(\"Catalog.Номенклатура\");\n");
        desc.append("\n");
        desc.append("// Specific document\n");
        desc.append("TypeItem goodsReceiptRef = (TypeItem)typeProvider.getProxy(\"Document.GoodsReceipt\");\n");
        desc.append("TypeItem salesOrderRef = (TypeItem)typeProvider.getProxy(\"Document.ЗаказКлиента\");\n");
        desc.append("\n");
        desc.append("// Specific enum\n");
        desc.append("TypeItem orderStatusRef = (TypeItem)typeProvider.getProxy(\"Enum.OrderStatus\");\n");
        desc.append("TypeItem sexRef = (TypeItem)typeProvider.getProxy(\"Enum.Пол\");\n");
        desc.append("\n");
        desc.append("// Specific registers\n");
        desc.append(
            "TypeItem goodsStockRef = (TypeItem)typeProvider.getProxy(\"AccumulationRegister.GoodsInStock\");\n");
        desc.append("TypeItem pricesRef = (TypeItem)typeProvider.getProxy(\"InformationRegister.Prices\");\n");
        desc.append("\n");
        desc.append("// Specific charts/plans\n");
        desc.append("TypeItem planOfAccounts = (TypeItem)typeProvider.getProxy(\"ChartOfAccounts.ПланСчетов\");\n");
        desc.append(
            "TypeItem chartOfCalc = (TypeItem)typeProvider.getProxy(\"ChartOfCalculationTypes.ВидыРасчетов\");\n");
        desc.append("```\n\n");

        desc.append("### Type Qualifiers (Advanced)\\n\\n");
        desc.append("**⚠️ IMPORTANT RESTRICTION:**\\n");
        desc.append(
            "Type qualifiers (StringQualifiers, NumberQualifiers) are ABSTRACT classes and CANNOT be instantiated directly.\\n");
        desc.append("For most use cases, use TypeDescriptionBuilder WITHOUT qualifiers or use default types.\\n\\n");

        desc.append("**⚠️ CRITICAL: Always use IEObjectProvider inside transaction!**\\n");
        desc.append("```java\\n");
        desc.append("// ✅ CORRECT: Create typeProvider INSIDE transaction\\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\\n");
        desc.append("\\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\\n");
        desc.append("    .addType(stringType)\\n");
        desc.append("    .build();\\n");
        desc.append("```\\n");

        desc.append("If qualifiers are required, use modelFactory to create them:\\n");
        desc.append("```java\\n");
        desc.append("// NOTE: This requires modelFactory context and may timeout in JShell\\n");
        desc.append("// For JShell, prefer simple types without qualifiers\\n");
        desc.append("\\n");
        desc.append("// If needed, create qualifiers via modelFactory:\\n");
        desc.append("// StringQualifiers stringQuals = modelFactory.createStringQualifiers();\\n");
        desc.append("// stringQuals.setLength(150);\\n");
        desc.append("// NumberQualifiers numberQuals = modelFactory.createNumberQualifiers();\\n");
        desc.append("// numberQuals.setPrecision(10);\\n");
        desc.append("// numberQuals.setScale(2);\\n");
        desc.append("```\\n");

        desc.append("### Best Practices\n\n");

        desc.append("**1. Always use typeProvider for version compatibility**\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT - uses project version\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem type = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("\n");
        desc.append("// ❌ WRONG - doesn't account for version\n");
        desc.append("// TypeItem type = TypeItem.eINSTANCE; // Do NOT do this\n");
        desc.append("```\n\n");

        desc.append("**2. Create TypeDescription once, reuse when possible**\n");
        desc.append("```java\n");
        desc.append("// Create type description outside loop\n");
        desc.append("TypeDescription stringTypeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Reuse for multiple attributes\n");
        desc.append("for (CatalogAttribute attr : attributes) {\n");
        desc.append("    attr.setType(stringTypeDesc);\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("**3. Handle composite types correctly**\n");
        desc.append("```java\n");
        desc.append("// Composite type (String or Number)\n");
        desc.append("TypeDescription compositeType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .addType(numberType)\n");
        desc.append("    .build();\n");
        desc.append("attribute.setType(compositeType);\n");
        desc.append("```\n\n");

        desc.append("**4. Type-specific considerations**\n");
        desc.append(
            "- **Catalog/Document references**: Use generic `*_REF` types when specific metadata may not exist\n");
        desc.append("- **Number types**: Consider precision/scale requirements. ⚠️ CRITICAL: Scale MUST be <= Precision (SU8 error)\n");
        desc.append("  - Precision: total number of digits (integer + decimal)\n");
        desc.append("  - Scale: number of digits after decimal point\n");
        desc.append("  - Example: Number(10, 2) = up to 10 total digits, 2 after decimal\n");
        desc.append("  - Example: 12345678.90 has 8 integer digits + 2 decimal = 10 total digits\n");
        desc.append("- **String types**: Consider length requirements (use StringQualifiers)\n");
        desc.append("- **Date types**: May need DateQualifiers in advanced scenarios\n");
        desc.append("- **Enum references**: Enum must exist in configuration\n");
        desc.append("- **Composite types**: Order affects type priority in some contexts\n");
        desc.append(
            "- **Register references**: Use `ACCUMULATION_REGISTER_REF`, `ACCOUNTING_REGISTER_REF`, `INFORMATION_REGISTER_REF`, `CALCULATION_REGISTER_REF` for generic register references\n");
        desc.append(
            "- **Chart/Plan references**: Use `CHART_OF_ACCOUNTS_REF`, `CHART_OF_CALCULATION_TYPES_REF`, `CHART_OF_CHARACTERISTIC_TYPES_REF`, `EXCHANGE_PLAN_REF` for generic chart/plan references\n");
        desc.append(
            "- **Business Process/Task references**: Use `BUSINESS_PROCESS_REF` and `TASK_REF` for workflow metadata\n");
        desc.append("- **ANY_REF**: Universal reference type - use when any reference type is acceptable\n");
        desc.append("- **CHARACTERISTIC**: Universal characteristic type for flexible attribute handling\n");
        desc.append(
            "- **Specific metadata references**: Use FQN like `\"Catalog.Products\"` - requires metadata to exist in configuration\n\n");

        desc.append("### Complete Example\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create catalog with types\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append(
            "        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("        catalog.setName(\"Products\");\n");
        desc.append("        catalog.getSynonym().put(\"ru\", \"Товары\");\n");
        desc.append("        catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\n");
        desc.append("\n");
        desc.append("        // Create type provider\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("\n");
        desc.append("        // Create attributes with different types\n");
        desc.append("        CatalogAttribute code = mdFactory.createCatalogAttribute();\n");
        desc.append("        code.setName(\"Code\");\n");
        desc.append("        code.getSynonym().put(\"ru\", \"Код\");\n");
        desc.append("        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("        TypeDescription codeType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(numberType)\n");
        desc.append("            .build();\n");
        desc.append("        code.setType(codeType);\n");
        desc.append("        catalog.getAttributes().add(code);\n");
        desc.append("\n");
        desc.append("        CatalogAttribute description = mdFactory.createCatalogAttribute();\n");
        desc.append("        description.setName(\"Description\");\n");
        desc.append("        description.getSynonym().put(\"ru\", \"Наименование\");\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription descType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(stringType)\n");
        desc.append("            .build();\n");
        desc.append("        description.setType(descType);\n");
        desc.append("        catalog.getAttributes().add(description);\n");
        desc.append("\n");
        desc.append("        CatalogAttribute category = mdFactory.createCatalogAttribute();\n");
        desc.append("        category.setName(\"Category\");\n");
        desc.append("        category.getSynonym().put(\"ru\", \"Категория\");\n");
        desc.append(
            "        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("        TypeDescription catType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(catalogRefType)\n");
        desc.append("            .build();\n");
        desc.append("        category.setType(catType);\n");
        desc.append("        catalog.getAttributes().add(category);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs\n");
        desc.append("        catalog.setUuid(UUID.randomUUID());\n");
        desc.append("        code.setUuid(UUID.randomUUID());\n");
        desc.append("        description.setUuid(UUID.randomUUID());\n");
        desc.append("        category.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append(
            "        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("        configuration.getCatalogs().add(catalog);\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildSafeCatalogWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create Catalog\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Catalog created = globalContext.execute(new AbstractBmTask<Catalog>(\"Create catalog\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append(
            "        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("        catalog.setName(\"Products\");\n");
        desc.append("        catalog.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("        catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\n");
        desc.append("        catalog.setCodeLength(9);\n");
        desc.append("        catalog.setDescriptionLength(150);\n");
        desc.append("\n");
        desc.append("        CatalogAttribute article = mdFactory.createCatalogAttribute();\n");
        desc.append("        article.setName(\"Article\");\n");
        desc.append("        article.getSynonym().put(\"ru\", \"Article\");\n");
        desc.append("\n");
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        // Note: String/Number qualifiers can be set via TypeDescriptionBuilder if needed\n");
        desc.append("        article.setType(typeDesc);\n");
        desc.append("        catalog.getAttributes().add(article);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)\n");
        desc.append("        catalog.setUuid(UUID.randomUUID());\n");
        desc.append("        article.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("        configuration.getCatalogs().add(catalog);\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");

        desc.append("### HierarchyType constants\n\n");
        desc.append("- `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS` is available and safe as a default\n");
        desc.append("- `HierarchyType.HIERARCHY_OF_ITEMS` is available when folders are not needed\n");
        desc.append("- `HierarchyType.HIERARCHY_GROUPS` does not exist in EDT API\n");
        desc.append("- `HierarchyType.HIERARCHY_HIERARCHICAL` does not exist in EDT API\n\n");

        desc.append("### Safe reference type pattern\n\n");
        desc.append("```java\n");
        desc.append("TypeItem unitsRef = (TypeItem)typeProvider.getProxy(\"Catalog.Units\");\n");
        desc.append("if (unitsRef == null) {\n");
        desc.append("    System.err.println(\"ERROR: Cannot resolve Catalog.Units\");\n");
        desc.append("    TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("    TypeDescription fallbackType = new TypeDescriptionBuilder()\n");
        desc.append("        .addType(catalogRef)\n");
        desc.append("        .build();\n");
        desc.append("    article.setType(fallbackType);\n");
        desc.append("} else {\n");
        desc.append("    TypeDescription strictType = new TypeDescriptionBuilder()\n");
        desc.append("        .addType(unitsRef)\n");
        desc.append("        .build();\n");
        desc.append("    article.setType(strictType);\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### Register Type Example\n\n");
        desc.append("```java\n");
        desc.append("// Create information register with dimensions of different types\n");
        desc.append("InformationRegister prices = mdFactory.createInformationRegister();\n");
        desc.append("prices.setName(\"Prices\");\n");
        desc.append("prices.getSynonym().put(\"ru\", \"Цены\");\n");
        desc.append("\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("\n");
        desc.append("// Dimension 1: Product (specific catalog reference)\n");
        desc.append("InformationRegisterDimension productDim = mdFactory.createInformationRegisterDimension();\n");
        desc.append("productDim.setName(\"Product\");\n");
        desc.append("TypeItem productsRef = (TypeItem)typeProvider.getProxy(\"Catalog.Products\");\n");
        desc.append("TypeDescription productType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(productsRef)\n");
        desc.append("    .build();\n");
        desc.append("productDim.setType(productType);\n");
        desc.append("prices.getDimensions().add(productDim);\n");
        desc.append("\n");
        desc.append("// Dimension 2: PriceType (generic catalog reference)\n");
        desc.append("InformationRegisterDimension priceTypeDim = mdFactory.createInformationRegisterDimension();\n");
        desc.append("priceTypeDim.setName(\"PriceType\");\n");
        desc.append("TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("TypeDescription catalogRefType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(catalogRef)\n");
        desc.append("    .build();\n");
        desc.append("priceTypeDim.setType(catalogRefType);\n");
        desc.append("prices.getDimensions().add(priceTypeDim);\n");
        desc.append("\n");
        desc.append("// Resource: Price (Number type)\n");
        desc.append("InformationRegisterResource price = mdFactory.createInformationRegisterResource();\n");
        desc.append("price.setName(\"Price\");\n");
        desc.append("TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("TypeDescription numberTypeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(numberType)\n");
        desc.append("    .build();\n");
        desc.append("price.setType(numberTypeDesc);\n");
        desc.append("prices.getResources().add(price);\n");
        desc.append("```\n\n");

        desc.append("### Chart of Accounts Type Example\n\n");
        desc.append("```java\n");
        desc.append("// Create accounting register with ChartOfAccounts reference\n");
        desc.append("AccountingRegister accReg = mdFactory.createAccountingRegister();\n");
        desc.append("accReg.setName(\"Accounting\");\n");
        desc.append("\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("\n");
        desc.append("// Dimension: Account (specific ChartOfAccounts reference)\n");
        desc.append("AccountingRegisterDimension accountDim = mdFactory.createAccountingRegisterDimension();\n");
        desc.append("accountDim.setName(\"Account\");\n");
        desc.append("TypeItem coaRef = (TypeItem)typeProvider.getProxy(\"ChartOfAccounts.ПланСчетов\");\n");
        desc.append("TypeDescription coaType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(coaRef)\n");
        desc.append("    .build();\n");
        desc.append("accountDim.setType(coaType);\n");
        desc.append("accReg.getDimensions().add(accountDim);\n");
        desc.append("```\n\n");

        desc.append("### JShell-safe UUID strategy\n");
        desc.append("⚠️ **WARNING:** `modelFactory.fillDefaultReferences()` may timeout in JShell due to OSGi service limitations.\n");
        desc.append("Prefer manual UUID assignment (Option 1) for reliable JShell execution.\n\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("// ...\n\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setName(\"Article\");\n");
        desc.append("attr.setUuid(UUID.randomUUID());\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeDescription attrType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .build();\n");
        desc.append("attr.setType(attrType);\n");
        desc.append("catalog.getAttributes().add(attr);\n\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("```\n\n");

        desc.append("### Common Mistake: Not setting UUIDs\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG - UUIDs not set, validation fails\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("// Error: SU45 - UUID required\n");
        desc.append("```\n");
        desc.append("\n### Important notes\n");
        desc.append("- Validate `typeProvider.getProxy(...)` before `addType(...)`; `null` causes `IllegalArgumentException`\n");
        desc.append("- References to metadata objects created in the same unfinished scenario may be unavailable; use a generic reference type or split work into steps\n");
        return desc.toString();
    }

    @Override
    public Collection<Class<?>> getSignificantClasses()
    {
        return List.of(
            MdClassFactory.class,
            MdObject.class,
            Configuration.class,
            Catalog.class,
            CatalogAttribute.class,
            Document.class,
            DocumentTabularSection.class,
            Report.class,
            TabularSectionAttribute.class,
            BasicFeature.class,
            CommonModule.class,
            AccumulationRegister.class, AccumulationRegisterDimension.class, AccumulationRegisterResource.class,
            AccountingRegister.class, AccountingRegisterDimension.class, AccountingRegisterResource.class,
            CalculationRegister.class, CalculationRegisterDimension.class, CalculationRegisterResource.class,
            ChartOfAccounts.class, ChartOfCalculationTypes.class, Recalculation.class, AccumulationRegisterType.class,
            CalculationRegisterPeriodicity.class,
            IBmNamespace.class,
            IBmTransaction.class,
            IBmModel.class,
            IBmEditingContext.class,
            IBmGlobalEditingContext.class,
            IBmTask.class,
            IV8Project.class,
            IV8ProjectManager.class,
            ITopObjectFqnGenerator.class,
            IModelObjectFactory.class,
            IResourceLookup.class,
            IBmModelManager.class,
            IEObjectProvider.class,
            IEObjectTypeNames.class,
            McorePackage.class,
            TypeDescription.class,
            TypeItem.class,
            TypeDescriptionBuilder.class,
            IProject.class,
            IWorkspaceRoot.class,
            EcoreUtil.class
        );
    }

    @SuppressWarnings("nls")
    @Override
    public Collection<String> getImports()
    {
        // @formatter:off
        return List.of(
            "import org.eclipse.core.resources.*;",
            "import com._1c.g5.v8.dt.metadata.mdclass.*;",
            "import com._1c.g5.v8.bm.core.*;",
            "import com._1c.g5.v8.bm.integration.*;",
            "import com._1c.g5.v8.dt.core.model.*;",
            "import com._1c.g5.v8.dt.core.naming.*;",
            "import com._1c.g5.v8.dt.core.platform.*;",
            "import com._1c.g5.v8.dt.platform.*;",
            "import com._1c.g5.v8.dt.mcore.McorePackage;",
            "import com._1c.g5.v8.dt.mcore.TypeDescription;",
            "import com._1c.g5.v8.dt.mcore.TypeItem;",
            "import com._1c.g5.v8.dt.platform.core.typeinfo.*;",
            "import org.eclipse.emf.ecore.util.EcoreUtil;"
        );
        // @formatter:on
    }

    @SuppressWarnings("nls")
    private String buildMdFactoryDescription()
    {
        var desc = new StringBuilder();
        desc.append("## MdClassFactory\n\n");
        desc.append("**Primary Purpose:** Factory for creating 1C metadata objects (catalogs, documents, registers, etc.).\n");
        desc.append("**Use Case:** Create NEW metadata objects inside BM transactions.\n\n");

        desc.append("### ⚠️ CRITICAL RESTRICTIONS:\n\n");
        desc.append("1. **MUST be used ONLY inside BM transaction** (`AbstractBmTask.execute()` body)\n");
        desc.append("2. **Do NOT use for editing existing objects** - use `getTopObjectByFqn()` instead\n");
        desc.append(
            "3. **Objects MUST have UUIDs set** - use manual assignment `object.setUuid(UUID.randomUUID())` for JShell (RECOMMENDED); avoid `modelFactory.fillDefaultReferences(object)` in JShell because it may timeout\n");
        desc.append("4. **Do NOT use `attachTopObject()` for existing objects** - causes `BmFqnAlreadyInUseException`\n\n");

        desc.append("### Supported Metadata Object Types:\n\n");
        desc.append("| Type | Factory Method | FQN Prefix |\n");
        desc.append("|------|----------------|------------|\n");
        desc.append("| Catalog (Справочник) | `createCatalog()` | `Catalog.` |\n");
        desc.append("| Document (Документ) | `createDocument()` | `Document.` |\n");
        desc.append("| BusinessProcess (Бизнес-процесс) | `createBusinessProcess()` | `BusinessProcess.` |\n");
        desc.append("| Task (Задача) | `createTask()` | `Task.` |\n");
        desc.append("| Sequence (Последовательность) | `createSequence()` | `Sequence.` |\n");
        desc.append("| DocumentJournal (ЖурналДокументов) | `createDocumentJournal()` | `DocumentJournal.` |\n");
        desc.append("| DocumentNumerator (НумераторДокументов) | `createDocumentNumerator()` | `DocumentNumerator.` |\n");
        desc.append("| DefinedType (ОпределяемыйТип) | `createDefinedType()` | `DefinedType.` |\n");
        desc.append("| InformationRegister (РегистрСведений) | `createInformationRegister()` | `InformationRegister.` |\n");
        desc.append("| AccumulationRegister (РегистрНакопления) | `createAccumulationRegister()` | `AccumulationRegister.` |\n");
        desc.append("| AccountingRegister (РегистрБухгалтерии) | `createAccountingRegister()` | `AccountingRegister.` |\n");
        desc.append("| CalculationRegister (РегистрРасчета) | `createCalculationRegister()` | `CalculationRegister.` |\n");
        desc.append("| Enum (Перечисление) | `createEnum()` | `Enum.` |\n");
        desc.append("| ChartOfCharacteristicTypes (ПланВидовХарактеристик) | `createChartOfCharacteristicTypes()` | `ChartOfCharacteristicTypes.` |\n");
        desc.append("| ChartOfAccounts (ПланСчетов) | `createChartOfAccounts()` | `ChartOfAccounts.` |\n");
        desc.append("| ChartOfCalculationTypes (ПланВидовРасчета) | `createChartOfCalculationTypes()` | `ChartOfCalculationTypes.` |\n");
        desc.append("| Report (Отчет) | `createReport()` | `Report.` |\n");
        desc.append("| DataProcessor (Обработка) | `createDataProcessor()` | `DataProcessor.` |\n");
        desc.append("| CommonModule (ОбщийМодуль) | `createCommonModule()` | `CommonModule.` |\n");
        desc.append("| Constant (Константа) | `createConstant()` | `Constant.` |\n");
        desc.append("| CommonAttribute (ОбщийРеквизит) | `createCommonAttribute()` | N/A |\n");
        desc.append("| ExchangePlan (ПланОбмена) | `createExchangePlan()` | `ExchangePlan.` |\n");
        desc.append("| EventSubscription (ПодпискаНаСобытие) | `createEventSubscription()` | N/A |\n");
        desc.append("| ScheduledJob (РегламентноеЗадание) | `createScheduledJob()` | N/A |\n");
        desc.append("| FilterCriterion (КритерийОтбора) | `createFilterCriterion()` | N/A |\n");
        desc.append("| FunctionalOption (ФункциональнаяОпция) | `createFunctionalOption()` | N/A |\n");
        desc.append("| WSReference (WSСсылка) | `createWSReference()` | `WSReference.` |\n");
        desc.append("| HTTPService (HTTPСервис) | `createHTTPService()` | `HTTPService.` |\n");
        desc.append("| WebService (Web-сервис) | `createWebService()` | `WebService.` |\n");
        desc.append("| IntegrationService (СервисИнтеграции) | `createIntegrationService()` | `IntegrationService.` |\n\n");

        desc.append("### Supported Attribute/Section Types:\n\n");
        desc.append("| Type | Factory Method | Parent Object |\n");
        desc.append("|------|----------------|---------------|\n");
        desc.append("| CatalogAttribute | `createCatalogAttribute()` | Catalog |\n");
        desc.append("| DocumentAttribute | `createDocumentAttribute()` | Document |\n");
        desc.append("| BusinessProcessAttribute | `createBusinessProcessAttribute()` | BusinessProcess |\n");
        desc.append("| TaskAttribute | `createTaskAttribute()` | Task |\n");
        desc.append("| RegisterAttribute | `createRegisterAttribute()` | Register |\n");
        desc.append("| InformationRegisterDimension | `createInformationRegisterDimension()` | InformationRegister |\n");
        desc.append("| InformationRegisterResource | `createInformationRegisterResource()` | InformationRegister |\n");
        desc.append("| AccumulationRegisterDimension | `createAccumulationRegisterDimension()` | AccumulationRegister |\n");
        desc.append("| AccumulationRegisterResource | `createAccumulationRegisterResource()` | AccumulationRegister |\n");
        desc.append("| AccountingRegisterDimension | `createAccountingRegisterDimension()` | AccountingRegister |\n");
        desc.append("| AccountingRegisterResource | `createAccountingRegisterResource()` | AccountingRegister |\n");
        desc.append("| CalculationRegisterDimension | `createCalculationRegisterDimension()` | CalculationRegister |\n");
        desc.append("| CalculationRegisterResource | `createCalculationRegisterResource()` | CalculationRegister |\n");
        desc.append("| TabularSectionAttribute | `createTabularSectionAttribute()` | TabularSection |\n");
        desc.append("| CatalogTabularSection | `createCatalogTabularSection()` | Catalog |\n");
        desc.append("| DocumentTabularSection | `createDocumentTabularSection()` | Document |\n");
        desc.append("| BusinessProcessTabularSection | `createBusinessProcessTabularSection()` | BusinessProcess |\n");
        desc.append("| TaskTabularSection | `createTaskTabularSection()` | Task |\n");
        desc.append("| BasicForm | `createBasicForm()` | Any metadata object |\n");
        desc.append("| BasicCommand | `createBasicCommand()` | Any metadata object |\n");
        desc.append("| Template | `createTemplate()` | Any metadata object |\n");
        desc.append("| EnumValue | `createEnumValue()` | Enum |\n");
        desc.append("| PredefinedItem | `createPredefinedItem()` | Catalog, ChartOfCharacteristicTypes |\n");
        desc.append("| Method | `createMethod()` | CommonModule |\n");
        desc.append("| Parameter | `createParameter()` | Method |\n");
        desc.append("| Operation | `createOperation()` | WebService |\n");
        desc.append("| Column | `createColumn()` | Cube, Table |\n");
        desc.append("| DimensionTable | `createDimensionTable()` | Cube |\n");
        desc.append("| Table | `createTable()` | Cube |\n");
        desc.append("| Recalculation | `createRecalculation()` | CalculationRegister |\n");
        desc.append("| RecalculationDimension | `createRecalculationDimension()` | Recalculation |\n\n");

        desc.append("### Correct Usage Example:\\n\\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT: Creating a new catalog\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\n");
        desc.append("catalog.setCodeLength(9);\n");
        desc.append("catalog.setDescriptionLength(150);\n");
        desc.append("\n");
        desc.append("// Add attribute\n");
        desc.append("CatalogAttribute attribute = mdFactory.createCatalogAttribute();\n");
        desc.append("attribute.setName(\"Article\");\n");
        desc.append("attribute.getSynonym().put(\"ru\", \"Article\");\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeDescription attrType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .build();\n");
        desc.append("attribute.setType(attrType);\n");
        desc.append("catalog.getAttributes().add(attribute);\n");
        desc.append("\n");
        desc.append("// CRITICAL: Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("attribute.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("// Generate FQN and attach to transaction\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("\n");
        desc.append("// Add to configuration\n");
        desc.append("Configuration config = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("config.getCatalogs().add(catalog);\n");
        desc.append("```\n\n");

        desc.append("### Common Mistakes:\n\n");
        desc.append("**❌ WRONG #1: Using mdFactory to edit existing object**\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog(); // Creates NEW object\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, \"Catalog.Products\"); // ❌ FQN already exists!\n");
        desc.append("```\n\n");
        desc.append("**❌ WRONG #2: Not setting UUIDs**\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("// Error: SU45 - UUID required for all metadata objects\n");
        desc.append("```\n\n");
        desc.append("**❌ WRONG #3: Using mdFactory outside transaction**\n");
        desc.append("```java\n");
        desc.append("// ❌ This will fail!\n");
        desc.append("Catalog catalog = mdFactory.createCatalog(); // Outside transaction\n");
        desc.append("```\n\n");

        desc.append("**✅ CORRECT: Edit existing object**\n");
        desc.append("```java\n");
        desc.append("// Get EXISTING object - do NOT use mdFactory\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("if (catalog != null) {\n");
        desc.append("    catalog.setDescriptionLength(200); // Modify directly\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### Available Public Methods:\n\n");

        var methodSignatures = methodListProvider.getPublicMethodSignatures(MdClassFactory.class);
        for (String signature : methodSignatures)
        {
            desc.append("- `").append(signature).append("`\n");
        }

        desc.append("\n");
        desc.append("**Note:** In JShell, prefer `mdFactory` plus manual UUID assignment for new metadata objects.\n");
        desc.append("Use `modelFactory` only when you specifically need its higher-level behavior and can tolerate possible OSGi timeout issues.");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildFqnGeneratorDescription()
    {
        var desc = new StringBuilder();
        desc.append("## ITopObjectFqnGenerator\n\n");
        desc.append("Generates FQN for top-level metadata objects before `attachTopObject`. "
            + "Required when creating NEW metadata objects.\n\n");
        desc.append("```java\n");
        desc.append("// CORRECT: Generate FQN for new object\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("String fqn = fqnGenerator\n");
        desc.append("    .generateStandaloneObjectFqn(catalog.eClass(), catalog.getName())\n");
        desc.append("    .toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("```\n\n");
        desc.append("**⚠️ NOT needed for editing existing objects:**\n");
        desc.append("```java\n");
        desc.append("// Get existing object - FQN already known\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("// No need to generate FQN!\n");
        desc.append("```\n\n");
        desc.append("**FQN Format Examples:**\n");
        desc.append("- `Catalog.Products`\n");
        desc.append("- `Document.GoodsReceipt`\n");
        desc.append("- `InformationRegister.ExchangeRates`\n");
        desc.append("- `AccumulationRegister.GoodsInStock`\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildModelFactoryDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IModelObjectFactory\n\n");
        desc.append("Higher-level creation API for project/version context.\n");
        desc.append("For JShell, prefer `mdFactory` plus manual UUID assignment because `fillDefaultReferences(...)` may timeout.\n\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = (Catalog)modelFactory.create(MdClassPackage.Literals.CATALOG, v8project);\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("\n");
        desc.append("CatalogAttribute attribute = (CatalogAttribute)modelFactory.create(\n");
        desc.append("    MdClassPackage.Literals.CATALOG_ATTRIBUTE, catalog, v8project.getVersion());\n");
        desc.append("attribute.setName(\"Article\");\n");
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n        ")).append("\n");
        desc.append("attribute.setType(typeDesc);\n");
        desc.append("catalog.getAttributes().add(attribute);\n");
        desc.append("\n");
        desc.append("// modelFactory.fillDefaultReferences(catalog); // Avoid in JShell\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildProjectManagerDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IV8ProjectManager\n\n");
        desc.append("Resolves `IV8Project` from Eclipse project.\n\n");
        desc.append("```java\n");
        desc.append("IProject eclipseProject = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(eclipseProject);\n");
        desc.append("if (v8project != null) {\n");
        desc.append("    System.out.println(\"Project: \" + v8project.getProject().getName());\n");
        desc.append("    System.out.println(\"Version: \" + v8project.getVersion());\n");
        desc.append("}\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildModelManagerDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IBmModelManager\n\n");
        desc.append("Provides BM model and editing context for metadata transactions. "
            + "Use `globalContext.execute()` for all read/write operations.\n\n");
        desc.append("### Reading existing objects:\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Catalog result = globalContext.execute(new AbstractBmTask<Catalog>(\"Read catalog\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        return (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Creating new objects:\n");
        desc.append("```java\n");
        desc.append("Catalog result = globalContext.execute(new AbstractBmTask<Catalog>(\"Create catalog\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("        catalog.setName(\"NewCatalog\");\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("        config.getCatalogs().add(catalog);\n");
        desc.append("        return catalog;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildResourceLookupDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IResourceLookup\n\n");
        desc.append("Maps metadata/model objects to Eclipse resources.\n\n");
        desc.append("```java\n");
        desc.append("IProject project = resourceLookup.getProject(catalog);\n");
        desc.append("IFile file = resourceLookup.getFile(catalog);\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildStringTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildNumberTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(numberType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildStringTypeWithQualifiersDescription()
    {
        var desc = new StringBuilder();
        desc.append("// Create String type with length qualifier\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Set string qualifiers (length)\n");
        desc.append("// Note: StringQualifiers must be set on the TypeDescription, not passed to builder\n");
        desc.append("// StringQualifiers stringQualifiers = modelFactory.createStringQualifiers();\n");
        desc.append("// stringQualifiers.setLength(50);\n");
        desc.append("// typeDesc.setStringQualifiers(stringQualifiers);\n");
        desc.append("\n");
        desc.append("// Simplified: just set length on TypeDescription's qualifiers\n");
        desc.append("if (typeDesc.getStringQualifiers() == null) {\n");
        desc.append("    typeDesc.setStringQualifiers(modelFactory.createStringQualifiers());\n");
        desc.append("}\n");
        desc.append("typeDesc.getStringQualifiers().setLength(50);\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildNumberTypeWithQualifiersDescription()
    {
        var desc = new StringBuilder();
        desc.append("// Create Number type with precision and scale qualifiers\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(numberType)\n");
        desc.append("    .build();\n");
        desc.append("\n");
        desc.append("// Set number qualifiers (precision, scale)\n");
        desc.append("// ⚠️ CRITICAL: Scale must be <= Precision, otherwise SU8 error occurs\n");
        desc.append("// Precision = total number of digits (including decimal places)\n");
        desc.append("// Scale = number of digits after decimal point\n");
        desc.append("if (typeDesc.getNumberQualifiers() == null) {\n");
        desc.append("    typeDesc.setNumberQualifiers(modelFactory.createNumberQualifiers());\n");
        desc.append("}\n");
        desc.append("typeDesc.getNumberQualifiers().setPrecision(10);\n");
        desc.append("typeDesc.getNumberQualifiers().setScale(2);\n");
        desc.append("// This creates a Number(10, 2) type: up to 10 total digits, 2 after decimal point\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDateTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(dateType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildBooleanTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem booleanType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BOOLEAN);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(booleanType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }


    @SuppressWarnings("nls")
    private String buildCatalogRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(catalogRefType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDocumentRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem documentRefType = typeProvider.getProxy(IEObjectTypeNames.DOCUMENT_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(documentRefType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildEnumRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem enumRefType = typeProvider.getProxy(IEObjectTypeNames.ENUM_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(enumRefType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildValueStorageTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem valueType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.VALUESTORAGE);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(valueType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildUuidTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem uuidType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UUID);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(uuidType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildUndefinedTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem undefinedType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UNDEFINED);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(undefinedType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildAnyRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem anyRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ANY_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(anyRefType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCharacteristicTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem characteristicType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHARACTERISTIC);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(characteristicType)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildAccumulationRegisterRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem accRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCUMULATION_REGISTER_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(accRegRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildAccountingRegisterRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem accRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCOUNTING_REGISTER_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(accRegRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildInformationRegisterRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem infoRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.INFORMATION_REGISTER_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(infoRegRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCalculationRegisterRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem calcRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CALCULATION_REGISTER_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(calcRegRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildChartOfAccountsRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem coaRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_ACCOUNTS_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(coaRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildChartOfCalcTypesRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem cctRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CALCULATION_TYPES_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(cctRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildChartOfCharTypesRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem cchtRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CHARACTERISTIC_TYPES_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(cchtRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildExchangePlanRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append(
            "TypeItem exchangePlanRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.EXCHANGE_PLAN_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(exchangePlanRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildBusinessProcessRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem bpRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BUSINESS_PROCESS_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(bpRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildTaskRefTypeDescription()
    {
        var desc = new StringBuilder();
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem taskRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.TASK_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(taskRef)\n");
        desc.append("    .build();\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildTransactionManagementScenarios()
    {
        var desc = new StringBuilder();
        desc.append("## Transaction Management Scenarios\n\n");
        desc.append("### When to use `attachTopObject()`\n\n");
        desc.append("| Scenario | Use attachTopObject() |\n");
        desc.append("|----------|---------------------|\n");
        desc.append("| Creating NEW object | ✅ Yes, once |\n");
        desc.append("| Reading existing | ❌ No |\n");
        desc.append("| Editing existing | ❌ No |\n");
        desc.append("| Renaming FQN | ❌ No, use `updateTopObjectFqn()` |\n");
        desc.append("| Detaching object | ❌ No, use `detachTopObject()` |\n\n");
        desc.append("### Key Transaction Rules\n\n");
        desc.append("**1. attachTopObject() - ONLY for NEW objects**\n");
        desc.append("```java\n");
        desc.append("// CORRECT: Creating a new catalog\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn); // ✅ OK\n");
        desc.append("configuration.getCatalogs().add(catalog);\n");
        desc.append("```\n\n");
        desc.append("**2. Editing existing - NO attachTopObject()**\n");
        desc.append("```java\n");
        desc.append("// CORRECT: Editing an existing catalog\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("catalog.setDescriptionLength(200); // ✅ OK\n");
        desc.append("// No attachTopObject() call!\n");
        desc.append("```\n\n");
        desc.append("**3. Avoiding BmFqnAlreadyInUseException**\n");
        desc.append("```java\n");
        desc.append("// Check before creating\n");
        desc.append("String fqn = \"Catalog.Products\";\n");
        desc.append("if (transaction.getTopObjectByFqn(fqn) == null) {\n");
        desc.append("    Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("    catalog.setName(\"Products\");\n");
        desc.append("    transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("    configuration.getCatalogs().add(catalog);\n");
        desc.append("} else {\n");
        desc.append("    // Object already exists - handle appropriately\n");
        desc.append("}\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDocumentWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create Document\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Document document = globalContext.execute(new AbstractBmTask<Document>(\"Create document\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        // Check if document already exists to avoid BmFqnAlreadyInUseException\n");
        desc.append("        String documentFqn = \"Document.GoodsReceipt\";\n");
        desc.append("        if (transaction.getTopObjectByFqn(documentFqn) != null) {\n");
        desc.append("            System.out.println(\"Document already exists: \" + documentFqn);\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("\n");
        desc.append("        // Create document\n");
        desc.append("        Document document = mdFactory.createDocument();\n");
        desc.append("        document.setName(\"GoodsReceipt\");\n");
        desc.append("        document.getSynonym().put(\"ru\", \"Приход товаров\");\n");
        desc.append("\n");
        desc.append("        // Set document number type - IMPORTANT: use correct enum constant\n");
        desc.append("        document.setNumberType(DocumentNumberType.NUMBER);\n");
        desc.append("        document.setNumberLength(9);\n");
        desc.append("        document.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);\n");
        desc.append("        document.setRealTimePosting(RealTimePosting.DENY);\n");
        desc.append("\n");
        desc.append("        // Create type provider INSIDE transaction\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("\n");
        desc.append("        // Add warehouse attribute (Catalog reference)\n");
        desc.append("        DocumentAttribute warehouse = mdFactory.createDocumentAttribute();\n");
        desc.append("        warehouse.setName(\"Warehouse\");\n");
        desc.append("        warehouse.getSynonym().put(\"ru\", \"Склад\");\n");
        desc.append("        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("        TypeDescription warehouseType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(catalogRefType)\n");
        desc.append("            .build();\n");
        desc.append("        warehouse.setType(warehouseType);\n");
        desc.append("        document.getAttributes().add(warehouse);\n");
        desc.append("\n");
        desc.append("        // Add date attribute\n");
        desc.append("        DocumentAttribute dateAttr = mdFactory.createDocumentAttribute();\n");
        desc.append("        dateAttr.setName(\"Date\");\n");
        desc.append("        dateAttr.getSynonym().put(\"ru\", \"Дата\");\n");
        desc.append("        TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);\n");
        desc.append("        TypeDescription dateTypeDesc = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(dateType)\n");
        desc.append("            .build();\n");
        desc.append("        dateAttr.setType(dateTypeDesc);\n");
        desc.append("        document.getAttributes().add(dateAttr);\n");
        desc.append("\n");
        desc.append("        // Add tabular section with typed line attributes\n");
        desc.append("        DocumentTabularSection products = mdFactory.createDocumentTabularSection();\n");
        desc.append("        products.setName(\"Products\");\n");
        desc.append("        products.getSynonym().put(\"ru\", \"Товары\");\n");
        desc.append("        products.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("        TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();\n");
        desc.append("        product.setName(\"Product\");\n");
        desc.append("        product.getSynonym().put(\"ru\", \"Номенклатура\");\n");
        desc.append("        TypeDescription productType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(catalogRefType)\n");
        desc.append("            .build();\n");
        desc.append("        product.setType(productType);\n");
        desc.append("        product.setUuid(UUID.randomUUID());\n");
        desc.append("        products.getAttributes().add(product);\n");
        desc.append("\n");
        desc.append("        TabularSectionAttribute quantity = mdFactory.createTabularSectionAttribute();\n");
        desc.append("        quantity.setName(\"Quantity\");\n");
        desc.append("        quantity.getSynonym().put(\"ru\", \"Количество\");\n");
        desc.append("        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("        TypeDescription quantityType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(numberType)\n");
        desc.append("            .build();\n");
        desc.append("        quantity.setType(quantityType);\n");
        desc.append("        quantity.setUuid(UUID.randomUUID());\n");
        desc.append("        products.getAttributes().add(quantity);\n");
        desc.append("        document.getTabularSections().add(products);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)\n");
        desc.append("        document.setUuid(UUID.randomUUID());\n");
        desc.append("        warehouse.setUuid(UUID.randomUUID());\n");
        desc.append("        dateAttr.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("        // Generate FQN and attach to transaction\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)document, fqn);\n");
        desc.append("        configuration.getDocuments().add(document);\n");
        desc.append("\n");
        desc.append("        System.out.println(\"Document created successfully: \" + fqn);\n");
        desc.append("        return document;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT Notes:**\n");
        desc.append("- **DocumentNumberType.NUMBER** (not `Number`) - use correct enum constant\n");
        desc.append("- **DocumentNumberPeriodicity.NONPERIODICAL** (not `Nonperiodical`) - use correct enum constant\n");
        desc.append("- **Do not call `document.setPosted(...)`** - this method is not present in EDT API\n");
        desc.append("- **`setRealTimePosting(...)` expects `RealTimePosting` enum** such as `RealTimePosting.DENY` or `RealTimePosting.ALLOW`\n");
        desc.append("- **TypeDescriptionBuilder** must be used INSIDE the transaction\n");
        desc.append("- **IEObjectProvider** must use `v8project.getVersion()` for version compatibility\n");
        desc.append("- **UUIDs** MUST be set for document and all attributes to avoid SU45 errors\n");
        desc.append("- **Every `DocumentAttribute` and `TabularSectionAttribute` must call `setType(...)`** before `add(...)`; otherwise EDT reports `md-legacy-emf-check` / `type is required`\n");
        desc.append("- **Check before creating** to avoid `BmFqnAlreadyInUseException`\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildEditExistingObjectWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Edit Existing Metadata Object\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Catalog result = globalContext.execute(new AbstractBmTask<Catalog>(\"Edit catalog\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Get EXISTING object - NO attachTopObject()\n");
        desc.append("        Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("\n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            // Modify properties directly\n");
        desc.append("            catalog.setDescriptionLength(200);\n");
        desc.append("\n");
        desc.append("            // Add new attribute\n");
        desc.append("            CatalogAttribute newAttr = mdFactory.createCatalogAttribute();\n");
        desc.append("            newAttr.setName(\"Brand\");\n");
        desc.append("            newAttr.setUuid(UUID.randomUUID());\n");
        desc.append("            ").append(buildStringTypeDescription().replace("\n", "\n            "));
        desc.append("\n");
        desc.append("            newAttr.setType(typeDesc);\n");
        desc.append("            catalog.getAttributes().add(newAttr);\n");
        desc.append("\n");
        desc.append("            return catalog;\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildTabularSectionWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Add Tabular Section to Existing Document\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Document result = globalContext.execute(new AbstractBmTask<Document>(\"Add tabular section\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // Get EXISTING document - NO attachTopObject()\n");
        desc.append("        Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("\n");
        desc.append("        if (document != null) {\n");
        desc.append("            // Create tabular section\n");
        desc.append("            DocumentTabularSection products = mdFactory.createDocumentTabularSection();\n");
        desc.append("            products.setName(\"Products\");\n");
        desc.append("            products.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("            products.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("            // Create tabular section attributes\n");
        desc.append("            TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();\n");
        desc.append("            product.setName(\"Product\");\n");
        desc.append("            product.getSynonym().put(\"ru\", \"Product\");\n");
        desc.append("            product.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("            ").append(buildCatalogRefTypeDescription().replace("\n", "\n            "));
        desc.append("\n");
        desc.append("            product.setType(typeDesc);\n");
        desc.append("\n");
        desc.append("            products.getAttributes().add(product);\n");
        desc.append("            document.getTabularSections().add(products);\n");
        desc.append("\n");
        desc.append("            return document;\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildRenameObjectWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Rename Metadata Object (Update FQN)\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Catalog result = globalContext.execute(new AbstractBmTask<Catalog>(\"Rename catalog\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("\n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            // Use updateTopObjectFqn - NOT attachTopObject\n");
        desc.append("            String newFqn = \"Catalog.Goods\";\n");
        desc.append("            transaction.updateTopObjectFqn(catalog, newFqn);\n");
        desc.append("\n");
        desc.append("            // Also update the object name\n");
        desc.append("            catalog.setName(\"Goods\");\n");
        desc.append("\n");
        desc.append("            return catalog;\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteAttributeWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Delete Attribute from Catalog\n\n");
        desc.append("```java\n");
        desc.append("Catalog result = globalContext.execute(new AbstractBmTask<Catalog>(\"Delete attribute\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Товары\");\n");
        desc.append("        \n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            // Find attribute by name\n");
        desc.append("            CatalogAttribute attrToRemove = null;\n");
        desc.append("            for (CatalogAttribute attr : catalog.getAttributes()) {\n");
        desc.append("                if (\"ПолноеНаименование\".equals(attr.getName())) {\n");
        desc.append("                    attrToRemove = attr;\n");
        desc.append("                    break;\n");
        desc.append("                }\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            // ⚠️ WARNING: simple remove() may not work correctly!\n");
        desc.append("            // Use EcoreUtil.delete() instead\n");
        desc.append("            if (attrToRemove != null) {\n");
        desc.append("                EcoreUtil.delete(attrToRemove);\n");
        desc.append("            }\n");
        desc.append("        }\n");
        desc.append("        return catalog;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**NOTE:** Always use `EcoreUtil.delete()` instead of `getAttributes().remove()` for proper entity deletion.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCommonPitfalls()
    {
        var desc = new StringBuilder();
        desc.append("## Common Pitfalls and Solutions\n\n");
        desc.append("This section covers the most frequent mistakes when working with metadata creation and editing.\n\n");
        desc.append("### ❌ Pitfall #0: `return;` inside task code that expects a value\n\n");
        desc.append("**Error:** `incompatible types: missing return value`\n\n");
        desc.append("**Problem:** Using `return;` inside `AbstractBmTask.execute()` when the method returns `Void` or another value.\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE\n");
        desc.append("if (projectHandle.exists()) {\n");
        desc.append("    System.err.println(\"ERROR: Project already exists\");\n");
        desc.append("    return;\n");
        desc.append("}\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE\n");
        desc.append("if (projectHandle.exists()) {\n");
        desc.append("    System.err.println(\"ERROR: Project already exists\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### ❌ Pitfall #1: attachTopObject on existing object\n\n");
        desc.append("**Error:** `BmFqnAlreadyInUseException`\n\n");
        desc.append("**Problem:** Trying to attach an object that already exists in the transaction.\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("document.setDescriptionLength(200); // Modification\n");
        desc.append("transaction.attachTopObject((IBmObject)document, fqn); // ❌ Exception!\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("if (document != null) {\n");
        desc.append("    document.setDescriptionLength(200); // Direct modification\n");
        desc.append("    // NO attachTopObject() call needed for existing objects!\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### ❌ Pitfall #2: Creating object with existing FQN\n\n");
        desc.append("**Error:** `BmFqnAlreadyInUseException` or validation error\n\n");
        desc.append("**Problem:** Creating new object with FQN that already exists.\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE\n");
        desc.append("String fqn = \"Catalog.Products\"; // Already exists in configuration!\n");
        desc.append("Catalog newCatalog = mdFactory.createCatalog();\n");
        desc.append("transaction.attachTopObject((IBmObject)newCatalog, fqn); // ❌ Exception!\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE - Check before creating\n");
        desc.append("String fqn = \"Catalog.NewProducts\";\n");
        desc.append("if (transaction.getTopObjectByFqn(fqn) == null) {\n");
        desc.append("    Catalog newCatalog = mdFactory.createCatalog();\n");
        desc.append("    newCatalog.setName(\"NewProducts\");\n");
        desc.append("    newCatalog.setUuid(UUID.randomUUID()); // Set UUID\n");
        desc.append("    String generatedFqn = fqnGenerator.generateStandaloneObjectFqn(\n");
        desc.append("        newCatalog.eClass(), newCatalog.getName()).toString();\n");
        desc.append("    transaction.attachTopObject((IBmObject)newCatalog, generatedFqn); // ✅ OK\n");
        desc.append("    configuration.getCatalogs().add(newCatalog);\n");
        desc.append("} else {\n");
        desc.append("    // Object already exists - handle appropriately\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### ❌ Pitfall #3: Not setting UUIDs\n\n");
        desc.append("**Error:** SU45 - UUID required for all metadata objects\n\n");
        desc.append("**Problem:** Creating metadata objects without UUIDs.\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setName(\"Article\");\n");
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n        ")).append("\n");
        desc.append("attr.setType(typeDesc);\n");
        desc.append("catalog.getAttributes().add(attr);\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("// Error: SU45 - UUID required for catalog and attributes!\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE - Option 1: Manual UUID assignment (RECOMMENDED for JShell)\n");
        desc.append("import java.util.UUID;\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setUuid(UUID.randomUUID()); // Must set UUID for children too!\n");
        desc.append("attr.setName(\"Article\");\n");
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n        ")).append("\n");
        desc.append("attr.setType(typeDesc);\n");
        desc.append("catalog.getAttributes().add(attr);\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ❌ PROHIBITED - Do NOT use fillDefaultReferences() in JShell\n");
        desc.append("// This method will timeout due to OSGi service limitations\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setName(\"Article\");\n");
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n        ")).append("\n");
        desc.append("attr.setType(typeDesc);\n");
        desc.append("catalog.getAttributes().add(attr);\n");
        desc.append("// ❌ modelFactory.fillDefaultReferences(catalog); // DO NOT USE!\n");
        desc.append("```\n\n");
        desc.append("**Always use manual UUID assignment instead.**\n\n");

        desc.append("### ❌ Pitfall #4: Using mdFactory outside transaction\n\n");
        desc.append("**Error:** Runtime exception or incorrect behavior\n\n");
        desc.append("**Problem:** Trying to use mdFactory outside AbstractBmTask.execute().\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE\n");
        desc.append("// Outside of transaction - this will fail!\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE - Always inside transaction\n");
        desc.append("Catalog result = globalContext.execute(new AbstractBmTask<Catalog>(\"Create catalog\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // mdFactory MUST be used inside execute() method\n");
        desc.append("        Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("        catalog.setName(\"Products\");\n");
        desc.append("        catalog.setUuid(UUID.randomUUID()); // Set UUID\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("        configuration.getCatalogs().add(catalog);\n");
        desc.append("        return catalog;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");

        desc.append("### ❌ Pitfall #5: Forgetting to add to parent collection\n\n");
        desc.append("**Error:** Object created but not visible in configuration\n\n");
        desc.append("**Problem:** Creating object but not adding it to configuration collection.\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("// ❌ Missing: configuration.getCatalogs().add(catalog);\n");
        desc.append("// Object is in transaction but not part of configuration!\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE\n");
        desc.append("Configuration config = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("configuration.getCatalogs().add(catalog); // ✅ Critical step!\n");
        desc.append("```\n\n");

        desc.append("### ❌ Pitfall #6: Setting non-existent properties\n\n");
        desc.append("**Error:** Compilation error or runtime exception\n\n");
        desc.append("**Problem:** Trying to set properties that don't exist on the object type.\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setNameLength(25); // ❌ setNameLength() doesn't exist!\n");
        desc.append("catalog.setDescriptionLength(150); // ✅ This exists\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setDescriptionLength(150); // ✅ Correct property\n");
        desc.append("catalog.setCodeLength(9); // ✅ Correct property\n");
        desc.append("// Catalog has: name, synonym, comment, hierarchical, hierarchyType,\n");
        desc.append("// codeLength, descriptionLength, codeType, etc.\n");
        desc.append("```\n\n");

        desc.append("### ❌ Pitfall #7: Incorrect top-level object deletion\n\n");
        desc.append("**Error:** `UnsupportedOperationException`\n\n");
        desc.append("**Problem:** Using `EcoreUtil.delete()` for top-level metadata objects causes exception.\n\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG CODE for top-level objects\n");
        desc.append("EcoreUtil.delete(catalog); // UnsupportedOperationException!\n");
        desc.append("```");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE for top-level objects\n");
        desc.append("Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("configuration.getCatalogs().remove(catalog);\n");
        desc.append("transaction.detachTopObject((IBmObject)catalog);\n");
        desc.append("```\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE for child objects (attributes, etc.)\n");
        desc.append("EcoreUtil.delete(attr); // Works correctly for child objects\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\\n");
        desc.append("**Note:** Deleting a catalog will cascade delete all its attributes, tabular sections, forms, and templates.\\n");

        desc.append("### ❌ Pitfall #8: Incorrect Enum Constants in JShell\\n\\n");
        desc.append("**Error:** Compilation error - cannot find symbol or type mismatch\\n\\n");
        desc.append("**Problem:** Using outdated enum constants or passing the wrong parameter type.\\n\\n");
        desc.append("```java\\n");
        desc.append("// ❌ WRONG CODE - Incorrect enum constants\\n");
        desc.append("catalog.setHierarchyType(HierarchyType.HIERARCHY_GROUPS); // Does not exist\\n");
        desc.append("document.setPosted(true); // Method does not exist\\n");
        desc.append("document.setRealTimePosting(true); // Expects RealTimePosting enum\\n");
        desc.append("```\\n\\n");
        desc.append("```java\\n");
        desc.append("// ✅ CORRECT CODE - Use correct enum constants\\n");
        desc.append("catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\\n");
        desc.append("document.setNumberType(DocumentNumberType.NUMBER);\\n");
        desc.append("document.setRealTimePosting(RealTimePosting.DENY);\\n");
        desc.append("```\\n\\n");

        desc.append("### ❌ Pitfall #9: Incorrect TypeDescription Usage in JShell\\n\\n");
        desc.append("**Error:** Type mismatch or NullPointerException\\n\\n");
        desc.append("**Problem:** Passing wrong values to TypeDescriptionBuilder or using a proxy that resolved to null.\\n\\n");
        desc.append("```java\\n");
        desc.append("// ❌ WRONG CODE - String type cannot be passed directly\\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\\n");
        desc.append("    .addType(\"Date\") // ❌ Wrong! Must use TypeItem\\n");
        desc.append("    .build();\\n");
        desc.append("\\n");
        desc.append("// ❌ WRONG CODE - Catalog object cannot be passed directly\\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\\n");
        desc.append("    .addType(catalogKontragenty) // ❌ Wrong! Must use TypeItem\\n");
        desc.append("    .build();\\n");
        desc.append("\\n");
        desc.append("// ❌ WRONG CODE - typeProvider outside transaction\\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\\n");
        desc.append("TypeItem type = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\\n");
        desc.append("\\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Test\") {\\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\\n");
        desc.append("        attribute.setType(new TypeDescriptionBuilder().addType(type).build());\\n");
        desc.append("        // ❌ TypeItem/proxy may be invalid for the current transaction\\n");
        desc.append("        return null;\\n");
        desc.append("    }\\n");
        desc.append("});\\n");
        desc.append("```\\n\\n");
        desc.append("```java\\n");
        desc.append("// ✅ CORRECT CODE - Use IEObjectProvider inside transaction\\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create metadata\") {\\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\\n");
        desc.append("        // Create typeProvider INSIDE transaction\\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\\n");
        desc.append("        \\n");
        desc.append("        // Get TypeItem INSIDE transaction\\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\\n");
        desc.append("        TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);\\n");
        desc.append("        TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\\n");
        desc.append("        \\n");
        desc.append("        // Build TypeDescription INSIDE transaction\\n");
        desc.append("        TypeDescription typeDesc = new TypeDescriptionBuilder()\\n");
        desc.append("            .addType(stringType)\\n");
        desc.append("            .build();\\n");
        desc.append("        \\n");
        desc.append("        attribute.setType(typeDesc);\\n");
        desc.append("        return null;\\n");
        desc.append("    }\\n");
        desc.append("});\\n");
        desc.append("```\\n\\n");

        desc.append("### Updated guidance: enum constants and TypeDescription proxies\n\n");
        desc.append("**HierarchyType:** use only `HIERARCHY_FOLDERS_AND_ITEMS` or `HIERARCHY_OF_ITEMS`.\n");
        desc.append("`HIERARCHY_GROUPS` and `HIERARCHY_HIERARCHICAL` are not present in EDT API.\n\n");
        desc.append("```java\n");
        desc.append("catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\n");
        desc.append("// or\n");
        desc.append("catalog.setHierarchyType(HierarchyType.HIERARCHY_OF_ITEMS);\n");
        desc.append("```\n\n");
        desc.append("**TypeDescriptionBuilder:** always validate `typeProvider.getProxy(...)` before `addType(...)`.\n");
        desc.append("Unresolved specific proxies may happen for typos, non-existent metadata, or references to objects that are not yet visible.\n\n");
        desc.append("```java\n");
        desc.append("TypeItem proxy = (TypeItem)typeProvider.getProxy(\"Catalog.Units\");\n");
        desc.append("if (proxy == null) {\n");
        desc.append("    System.err.println(\"ERROR: Cannot resolve Catalog.Units\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(proxy)\n");
        desc.append("    .build();\n");
        desc.append("```\n\n");

        desc.append("### ❌ Pitfall #10: Runtime BmFqnAlreadyInUseException\\n\\n");
        desc.append("**Error:** `com._1c.g5.v8.bm.core.BmFqnAlreadyInUseException`\\n");
        desc.append("**Problem:** Trying to create object with FQN that already exists.\\n\\n");
        desc.append("**Solution:** Always check if object exists before creating.\\n\\n");
        desc.append("```java\\n");
        desc.append("// ✅ CORRECT CODE - Check before creating\\n");
        desc.append("String fqn = \"Catalog.Products\";\\n");
        desc.append("if (transaction.getTopObjectByFqn(fqn) == null) {\\n");
        desc.append("    Catalog catalog = mdFactory.createCatalog();\\n");
        desc.append("    catalog.setName(\"Products\");\\n");
        desc.append("    catalog.setUuid(UUID.randomUUID());\\n");
        desc.append("    String generatedFqn = fqnGenerator.generateStandaloneObjectFqn(\\n");
        desc.append("        catalog.eClass(), catalog.getName()).toString();\\n");
        desc.append("    transaction.attachTopObject((IBmObject)catalog, generatedFqn);\\n");
        desc.append("    configuration.getCatalogs().add(catalog);\\n");
        desc.append("} else {\\n");
        desc.append("    System.out.println(\"Object already exists: \" + fqn);\\n");
        desc.append("}\\n");
        desc.append("```\\n\\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildMetadataValidationErrors()
    {
        var desc = new StringBuilder();
        desc.append("## Metadata Validation Errors - Common Fixes\n\n");
        desc.append("### Missing `type` on `BasicFeature` (`md-legacy-emf-check`)\n\n");
        desc.append("**Error Message:** \"Должна быть задана сущность 'type', необходимая для...\" or \"Тип не указан\"\n\n");
        desc.append("**Problem:** A metadata object derived from `BasicFeature` is missing a required type definition.\n\n");
        desc.append("**Common Causes:**\n");
        desc.append("- `CatalogAttribute` added without TypeDescription\n");
        desc.append("- `DocumentAttribute` added without TypeDescription\n");
        desc.append("- `TabularSectionAttribute` added without TypeDescription\n");
        desc.append("- TypeDescription created with `null` proxy or not assigned via `setType(...)`\n\n");
        desc.append("**Fix Pattern:**\n");
        desc.append("```java\n");
        desc.append("// Get the attribute and set its type\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Контрагенты\");\n");
        desc.append("if (catalog != null) {\n");
        desc.append("    for (CatalogAttribute attr : catalog.getAttributes()) {\n");
        desc.append("        if (\"ИНН\".equals(attr.getName())) {\n");
        desc.append("            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("            TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("            TypeDescription typeDesc = new TypeDescriptionBuilder()\n");
        desc.append("                .addType(stringType)\n");
        desc.append("                .build();\n");
        desc.append("            attr.setType(typeDesc);\n");
        desc.append("            System.out.println(\"Fixed attribute type\");\n");
        desc.append("        }\n");
        desc.append("    }\n");
        desc.append("}\n");
        desc.append("```\n\n");
        desc.append("**Important Notes:**\n");
        desc.append("- Always get typeProvider INSIDE the transaction\n");
        desc.append("- TypeItem proxies must be obtained and used within the SAME IBmTransaction\n");
        desc.append("- For String types, use buildStringTypeDescription() or buildStringTypeWithQualifiersDescription()\n");
        desc.append("- For Number types, use buildNumberTypeDescription() or buildNumberTypeWithQualifiersDescription()\n\n");

        desc.append("### SU8: Scale Cannot Exceed Precision\n\n");
        desc.append("**Error Message:** \"Точность числа не может быть больше его длины\"\n\n");
        desc.append("**Problem:** NumberQualifiers has Scale > Precision, which is invalid.\n\n");
        desc.append("**Understanding Precision and Scale:**\n");
        desc.append("- **Precision**: Total number of digits (integer part + decimal part)\n");
        desc.append("- **Scale**: Number of digits after the decimal point\n");
        desc.append("- **Rule**: Scale MUST be <= Precision\n\n");
        desc.append("**Examples:**\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT: Number(10, 2) - 10 total digits, 2 after decimal\n");
        desc.append("// Values: 12345678.90 (8 + 2 = 10 digits)\n");
        desc.append("typeDesc.getNumberQualifiers().setPrecision(10);\n");
        desc.append("typeDesc.getNumberQualifiers().setScale(2);\n\n");
        desc.append("// ❌ WRONG: Number(2, 10) - Scale (10) > Precision (2)\n");
        desc.append("// This causes SU8 error\n");
        desc.append("typeDesc.getNumberQualifiers().setPrecision(2);\n");
        desc.append("typeDesc.getNumberQualifiers().setScale(10); // ❌ SU8 error!\n\n");
        desc.append("// ✅ CORRECT: Number(15, 4) - 15 total digits, 4 after decimal\n");
        desc.append("// Values: 1234567890123.4567 (11 + 4 = 15 digits)\n");
        desc.append("typeDesc.getNumberQualifiers().setPrecision(15);\n");
        desc.append("typeDesc.getNumberQualifiers().setScale(4);\n\n");
        desc.append("// ✅ CORRECT: Number(20, 0) - 20 total digits, 0 after decimal\n");
        desc.append("// Integer only: 12345678901234567890 (20 digits)\n");
        desc.append("typeDesc.getNumberQualifiers().setPrecision(20);\n");
        desc.append("typeDesc.getNumberQualifiers().setScale(0);\n");
        desc.append("```\n\n");
        desc.append("**Fix Pattern - Find and Correct All SU8 Errors:**\n");
        desc.append("```java\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Fix SU8 errors\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration config = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        \n");
        desc.append("        // Fix catalogs\n");
        desc.append("        for (Catalog catalog : config.getCatalogs()) {\n");
        desc.append("            for (CatalogAttribute attr : catalog.getAttributes()) {\n");
        desc.append("                if (attr.getType() != null && attr.getType().getNumberQualifiers() != null) {\n");
        desc.append("                    NumberQualifiers nq = attr.getType().getNumberQualifiers();\n");
        desc.append("                    if (nq.getScale() > nq.getPrecision()) {\n");
        desc.append("                        System.out.println(\"Fixing \" + catalog.getName() + \".\" + attr.getName());\n");
        desc.append("                        // Swap precision and scale to fix the error\n");
        desc.append("                        int temp = nq.getPrecision();\n");
        desc.append("                        nq.setPrecision(nq.getScale());\n");
        desc.append("                        nq.setScale(temp);\n");
        desc.append("                    }\n");
        desc.append("                }\n");
        desc.append("            }\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // Fix documents\n");
        desc.append("        for (Document document : config.getDocuments()) {\n");
        desc.append("            for (DocumentAttribute attr : document.getAttributes()) {\n");
        desc.append("                if (attr.getType() != null && attr.getType().getNumberQualifiers() != null) {\n");
        desc.append("                    NumberQualifiers nq = attr.getType().getNumberQualifiers();\n");
        desc.append("                    if (nq.getScale() > nq.getPrecision()) {\n");
        desc.append("                        int temp = nq.getPrecision();\n");
        desc.append("                        nq.setPrecision(nq.getScale());\n");
        desc.append("                        nq.setScale(temp);\n");
        desc.append("                    }\n");
        desc.append("                }\n");
        desc.append("            }\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // Fix registers (Accumulation, Information, Accounting, Calculation)\n");
        desc.append("        // Similar pattern for dimensions and resources...\n");
        desc.append("        \n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("**Common Mistakes to Avoid:**\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG: Trying to access non-existent methods\n");
        desc.append("// NumberQualifiers nq = attr.getType().getNumberQualifiers();\n");
        desc.append("// nq.getLength(); // ❌ This method does NOT exist!\n");
        desc.append("\n");
        desc.append("// ✅ CORRECT: Use correct methods\n");
        desc.append("int precision = nq.getPrecision();\n");
        desc.append("int scale = nq.getScale();\n");
        desc.append("```\n\n");
        desc.append("### General Workflow for Fixing Metadata Validation Errors\n\n");
        desc.append("**Step 1: Identify the error**\n");
        desc.append("- Check EDT markers/problems view\n");
        desc.append("- Note the error code (SU45, SU8, etc.)\n");
        desc.append("- Note the object path (Catalog.Контрагенты, Document.ПриходТовара, etc.)\n\n");
        desc.append("**Step 2: Understand the requirement**\n");
        desc.append("- SU45: Type must be specified - use TypeDescriptionBuilder\n");
        desc.append("- SU8: Scale <= Precision for Number types\n\n");
        desc.append("**Step 3: Implement fix inside BM transaction**\n");
        desc.append("- Always use `globalContext.execute(new AbstractBmTask<Void>(...) {...})`\n");
        desc.append("- Get object by FQN: `transaction.getTopObjectByFqn(\"Catalog.Имя\")`\n");
        desc.append("- Modify directly (no attachTopObject needed for existing objects)\n");
        desc.append("- Set type or qualifiers using modelFactory\n\n");
        desc.append("**Step 4: Verify fix**\n");
        desc.append("- Refresh/Rebuild project in EDT\n");
        desc.append("- Check markers view for remaining errors\n\n");
        desc.append("**Step 5: Consider project-wide fixes**\n");
        desc.append("- If multiple objects have same error, iterate through collections\n");
        desc.append("- Use Configuration object to access all catalogs, documents, registers\n\n");
        desc.append("**Important Reminders:**\n");
        desc.append("- TypeDescription and TypeItem must be created INSIDE the transaction\n");
        desc.append("- Use `modelFactory` for creating qualifiers in JShell context\n");
        desc.append("- Set UUIDs manually when creating new metadata objects\n");
        desc.append("- For existing objects: modify directly, don't use attachTopObject()\n");
        desc.append("- Check that Scale <= Precision for all Number types\n");
        desc.append("- Verify that all attributes have valid TypeDescription set\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCreateConfigurationWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create New 1C Configuration Project\n\n");
        desc.append("This workflow creates a complete 1C:Enterprise configuration project in the workspace.\n");
        desc.append("This includes all necessary files and structures for a functional V8 project.\n\n");
        desc.append("```java\n");
        desc.append("// Step 1: Define project name\n");
        desc.append("String projectName = \"MyNewConfiguration\";\n");
        desc.append("IProject projectHandle = workspaceRoot.getProject(projectName);\n");
        desc.append("\n");
        desc.append("// Step 2: Check if project already exists\n");
        desc.append("if (projectHandle.exists()) {\n");
        desc.append("    System.err.println(\"ERROR: Project already exists: \" + projectName);\n");
        desc.append("    // Stop here in JShell and choose another project name.\n");
        desc.append("}\n");
        desc.append("\n");
        desc.append("try {\n");
        desc.append("    // Step 3: Create project description with natures AND build command SET BEFORE creation\n");
        desc.append("    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);\n");
        desc.append("    \n");
        desc.append("    // Set natures\n");
        desc.append("    String[] natures = new String[2];\n");
        desc.append("    natures[0] = \"org.eclipse.xtext.ui.shared.xtextNature\";\n");
        desc.append("    natures[1] = \"com._1c.g5.v8.dt.core.V8ConfigurationNature\";\n");
        desc.append("    description.setNatureIds(natures);\n");
        desc.append("    \n");
        desc.append("    // Set build command (CRITICAL for Xtext builder)\n");
        desc.append("    ICommand[] commands = new ICommand[1];\n");
        desc.append("    ICommand command = description.newCommand();\n");
        desc.append("    command.setBuilderName(\"org.eclipse.xtext.ui.shared.xtextBuilder\");\n");
        desc.append("    commands[0] = command;\n");
        desc.append("    description.setBuildSpec(commands);\n");
        desc.append("\n");
        desc.append("    // Step 4: Create the project with pre-configured description\n");
        desc.append("    projectHandle.create(description, new NullProgressMonitor());\n");
        desc.append("    projectHandle.open(new NullProgressMonitor());\n");
        desc.append("\n");
        desc.append("    // Step 5: Create basic project structure\n");
        desc.append("    IFolder srcFolder = projectHandle.getFolder(\"src\");\n");
        desc.append("    srcFolder.create(false, true, new NullProgressMonitor());\n");
        desc.append("\n");
        desc.append("    IFolder configFolder = srcFolder.getFolder(\"Configuration\");\n");
        desc.append("    configFolder.create(false, true, new NullProgressMonitor());\n");
        desc.append("\n");
        desc.append("    // Step 6: Create DT-INF folder and PROJECT.PMF file (CRITICAL for V8 project)\n");
        desc.append("    IFolder dtinfFolder = projectHandle.getFolder(\"DT-INF\");\n");
        desc.append("    dtinfFolder.create(false, true, new NullProgressMonitor());\n");
        desc.append("\n");
        desc.append("    IFile pmfFile = dtinfFolder.getFile(\"PROJECT.PMF\");\n");
        desc.append("    // CRITICAL: PROJECT.PMF must be OSGi manifest format, NOT XML!\n");
        desc.append("    String pmfContent = \"Manifest-Version: 1.0\\nRuntime-Version: 8.3.24\\n\";\n");
        desc.append("    pmfFile.create(new ByteArrayInputStream(pmfContent.getBytes()), true, new NullProgressMonitor());\n");
        desc.append("\n");
        desc.append("    // Step 7: Create Configuration.mdo file (CRITICAL for metadata initialization)\n");
        desc.append("    // IMPORTANT: Use CORRECT format with mdclass namespace (NOT mdobject!)\n");
        desc.append("    IFile configFile = configFolder.getFile(\"Configuration.mdo\");\n");
        desc.append("    String configContent = \"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\n\" +\n");
        desc.append("        \"<mdclass:Configuration xmlns:mdclass=\\\"http://g5.1c.ru/v8/dt/metadata/mdclass\\\" uuid=\\\"\" + UUID.randomUUID().toString() + \"\\\">\\n\" +\n");
        desc.append("        \"  <name>Configuration</name>\\n\" +\n");
        desc.append("        \"  <synonym>\\n\" +\n");
        desc.append("        \"    <key>ru</key>\\n\" +\n");
        desc.append("        \"    <value>Конфигурация</value>\\n\" +\n");
        desc.append("        \"  </synonym>\\n\" +\n");
        desc.append("        \"  <defaultRunMode>ManagedApplication</defaultRunMode>\\n\" +\n");
        desc.append("        \"  <usePurposes>PersonalComputer</usePurposes>\\n\" +\n");
        desc.append("        \"  <usedMobileApplicationFunctionalities>\\n\" +\n");
        desc.append("        \"    <functionality>\\n\" +\n");
        desc.append("        \"      <use>true</use>\\n\" +\n");
        desc.append("        \"    </functionality>\\n\" +\n");
        desc.append("        \"  </usedMobileApplicationFunctionalities>\\n\" +\n");
        desc.append("        \"</mdclass:Configuration>\";\n");
        desc.append("    configFile.create(new ByteArrayInputStream(configContent.getBytes(\"UTF-8\")), true, new NullProgressMonitor());\n");
        desc.append("\n");
        desc.append("    // Step 8: Refresh project to ensure file system synchronization\n");
        desc.append("    projectHandle.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());\n");
        desc.append("\n");
        desc.append("    // Step 9: Wait for asynchronous project initialization using POLLING mechanism\n");
        desc.append("    boolean initialized = false; // FIX: Declare initialized variable before use\n");
        desc.append("    java.lang.System.out.println(\"Waiting for project initialization (polling mechanism)...\");\n");
        desc.append("    int maxAttempts = 30; // 30 attempts × 500ms = 15 seconds max\n");
        desc.append("    int attempt = 0;\n");
        desc.append("    \n");
        desc.append("    while (attempt < maxAttempts && !initialized) {\n");
        desc.append("        attempt++;\n");
        desc.append("        try {\n");
        desc.append("            Thread.sleep(500); // Short pause between attempts\n");
        desc.append("            \n");
        desc.append("            IV8Project v8project = projectManager.getProject(projectHandle);\n");
        desc.append("            if (v8project != null) {\n");
        desc.append("                // Verify BM model is also available\n");
        desc.append("                IBmModel bmModel = modelManager.getModel(projectHandle);\n");
        desc.append("                if (bmModel != null) {\n");
        desc.append("                    // Check Configuration object is accessible\n");
        desc.append("                    IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("                    Configuration config = globalContext.execute(new AbstractBmTask<Configuration>(\"Check Config\") {\n");
        desc.append("                        @Override\n");
        desc.append("                        public Configuration execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("                            return (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("                        }\n");
        desc.append("                    });\n");
        desc.append("                    \n");
        desc.append("                    if (config != null) {\n");
        desc.append("                        initialized = true;\n");
        desc.append("                        java.lang.System.out.println(\"Project initialized in \" + (attempt * 500) + \"ms (attempt \" + attempt + \")\");\n");
        desc.append("                        java.lang.System.out.println(\"SUCCESS: V8 project created\");\n");
        desc.append("                        java.lang.System.out.println(\"Version: \" + v8project.getVersion());\n");
        desc.append("                        java.lang.System.out.println(\"SUCCESS: BM model initialized\");\n");
        desc.append("                        java.lang.System.out.println(\"SUCCESS: Configuration object is accessible\");\n");
        desc.append("                        java.lang.System.out.println(\"Configuration name: \" + config.getName());\n");
        desc.append("                        java.lang.System.out.println(\"Configuration project created successfully: \" + projectName);\n");
        desc.append("                    }\n");
        desc.append("                }\n");
        desc.append("            }\n");
        desc.append("        } catch (InterruptedException e) {\n");
        desc.append("            System.err.println(\"Polling interrupted: \" + e.getMessage());\n");
        desc.append("            break;\n");
        desc.append("        }\n");
        desc.append("    }\n");
        desc.append("    \n");
        desc.append("    if (!initialized) {\n");
        desc.append("        System.err.println(\"ERROR: Project initialization failed after \" + maxAttempts + \" attempts\");\n");
        desc.append("        System.err.println(\"Please check project logs for details\");\n");
        desc.append("        // Stop here in JShell and inspect project logs before continuing.\n");
        desc.append("    }\n");
        desc.append("\n");
        desc.append("} catch (CoreException | UnsupportedEncodingException e) {\n");
        desc.append("    System.err.println(\"ERROR creating project: \" + e.getMessage());\n");
        desc.append("    e.printStackTrace();\n");
        desc.append("}\n");
        desc.append("```\n");
        desc.append("\n**Critical Issues Fixed:**\n");
        desc.append("\n");
        desc.append("**Issue 1: Incorrect PROJECT.PMF format**\n");
        desc.append("- ✅ FIX: Create DT-INF folder with OSGi manifest format (NOT XML)\n");
        desc.append("- PROJECT.PMF must be OSGi manifest format: \"Manifest-Version: 1.0\\nRuntime-Version: 8.3.24\\n\"\n");
        desc.append("- Without correct format, causes ProjectManifestException\n");
        desc.append("- XML format (<?xml version=...?>) is INCORRECT for PROJECT.PMF\n");
        desc.append("\n");
        desc.append("**Issue 2: INCORRECT Configuration.mdo format**\n");
        desc.append("- ✅ FIX: Use mdclass namespace instead of mdobject\n");
        desc.append("- Correct namespace: http://g5.1c.ru/v8/dt/metadata/mdclass (NOT mdobject)\n");
        desc.append("- Without correct namespace, causes BM model initialization failure\n");
        desc.append("- Remove optional elements that cause issues: scriptVariant, defaultLanguage, configurationCompatibility\n");
        desc.append("- Add REQUIRED elements: defaultRunMode, usedMobileApplicationFunctionalities\n");
        desc.append("\n");
        desc.append("**Issue 3: Asynchronous project initialization**\n");
        desc.append("- ✅ FIX: Use polling mechanism with adaptive waiting instead of fixed sleep\n");
        desc.append("- Polling checks project readiness every 500ms (30 attempts × 500ms = 15 seconds max)\n");
        desc.append("- Automatically stops when project is fully initialized (IV8Project + BM model + Configuration object)\n");
        desc.append(
            "- Proven efficiency: typically initializes in 2-3 seconds (50% faster than fixed 5-second delay)\n");
        desc.append("- For production: use project build listener instead of polling\n");
        desc.append("\n");
        desc.append("**Issue 4: No Configuration object verification**\n");
        desc.append("- ✅ FIX: Add verification that Configuration object is accessible via BM transaction\n");
        desc.append("- Without verification, project may appear to work but fail on metadata operations\n");
        desc.append("- Configuration object accessibility confirms BM model is properly initialized\n");
        desc.append("\n");
        desc.append("**Issue 5: Missing build command**\n");
        desc.append("- ✅ FIX: Add Xtext builder command to project description\n");
        desc.append("- Build command is required for proper V8 project initialization\n");
        desc.append("- Without it, project may not be recognized as V8 configuration\n");
        desc.append("\n");
        desc.append("**Important Notes:**\n");
        desc.append("- PROJECT.PMF is REQUIRED for V8 project initialization\n");
        desc.append("- PROJECT.PMF must be OSGi manifest format (Manifest-Version: 1.0, Runtime-Version: X.X.X)\n");
        desc.append("- DO NOT use XML format for PROJECT.PMF - it will cause ProjectManifestException\n");
        desc.append("- Configuration.mdo MUST use mdclass namespace (http://g5.1c.ru/v8/dt/metadata/mdclass)\n");
        desc.append("- Configuration.mdo MUST include: defaultRunMode, usedMobileApplicationFunctionalities\n");
        desc.append("- DO NOT use mdobject namespace - it causes BM model initialization failure\n");
        desc.append("- Both natures AND build command MUST be set BEFORE project creation\n");
        desc.append("- BM model is initialized on first access via modelManager.getModel()\n");
        desc.append("- After creation, you can use mdFactory workflows to add metadata objects\n");
        desc.append("- For production use, use proper IProgressMonitor implementation\n");
        desc.append("- Verify all initialization steps succeed before proceeding with metadata operations\n");
        desc.append("- Polling mechanism is RECOMMENDED for JShell context - adaptive and efficient\n");
        desc.append("- Polling checks: IV8Project + BM model + Configuration object accessibility\n");
        desc.append("- Polling stops automatically when project is ready (typically 2-3 seconds)\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteConfigurationWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete 1C Configuration Project\n\n");
        desc.append("This workflow permanently deletes a 1C:Enterprise configuration project from the workspace.\n");
        desc.append("⚠️ **WARNING:** This operation cannot be undone. All project data will be permanently deleted.\n\n");
        desc.append("### Part 1: Dissociate Infobases (if any)\n\n");
        desc.append("```java\n");
        desc.append("// Step 1: Get the project to delete\n");
        desc.append("String projectName = \"MyConfigurationToDelete\";\n");
        desc.append("IProject project = workspaceRoot.getProject(projectName);\n");
        desc.append("\n");

        desc.append("// Step 2: Check if project exists\n");
        desc.append("if (project.exists()) {\n");
        desc.append("    \n");
        desc.append("    // Step 3: Dissociate infobases before deleting (if they exist)\n");
        desc.append("    // Note: IInfobaseAssociationManager must be injected via dependency injection\n");
        desc.append("    // This example shows the workflow pattern\n");
        desc.append("\n");
        desc.append("/*\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IInfobaseAssociationManager associationManager = getInfobaseAssociationManager();\n");
        desc.append("    \n");
        desc.append("try {\n");
        desc.append("    Optional<IInfobaseAssociation> association = associationManager.getAssociation(project);\n");
        desc.append("        \n");
        desc.append("    if (association.isPresent()) {\n");
        desc.append("        Collection<InfobaseReference> infobases = association.get().getInfobases();\n");
        desc.append("        InfobaseAssociationContext context = association.get().getContext();\n");
        desc.append("            \n");
        desc.append("        for (InfobaseReference infobase : infobases) {\n");
        desc.append("            // Dissociate each infobase\n");
        desc.append("            associationManager.dissociate(project, infobase, context);\n");
        desc.append("            System.out.println(\"Dissociated infobase: \" + infobase.getName());\n");
        desc.append("        }\n");
        desc.append("    }\n");
        desc.append("} catch (InfobaseAssociationException e) {\n");
        desc.append("    System.err.println(\"Error dissociating infobases: \" + e.getMessage());\n");
        desc.append("}\n");
        desc.append("*/\n");
        desc.append("\n");

        desc.append("```\n");
        desc.append("### Part 2: Delete the Project\n");
        desc.append("```java\n");
        desc.append("    // Step 4: Close the project if it's open\n");
        desc.append("    if (project.isOpen()) {\n");
        desc.append("        project.close(new NullProgressMonitor());\n");
        desc.append("    }\n");
        desc.append("    \n");
        desc.append("    // Step 5: Delete the project (true = force delete, true = delete content on disk)\n");
        desc.append("    project.delete(true, true, new NullProgressMonitor());\n");
        desc.append("    \n");
        desc.append("    System.out.println(\"Configuration project deleted successfully: \" + projectName);\n");
        desc.append("} else {\n");
        desc.append("    System.out.println(\"Project does not exist: \" + projectName);\n");
        desc.append("}\n");
        desc.append("```\n");
        desc.append("### Part 3: Optional - Delete Infobase from Registry\n");
        desc.append("```java\n");
        desc.append("// Step 6: Optionally delete infobase reference from the registry\n");
        desc.append("// Note: This removes the infobase from EDT's infobase list\n");
        desc.append("// This requires IInfobaseManager and should be done carefully\n");
        desc.append("\n");
        desc.append("/*\n");
        desc.append("// IInfobaseManager infobaseManager = getInfobaseManager();\n");
        desc.append("// List<Section> allSections = infobaseManager.getAll();\n");
        desc.append("// List<InfobaseReference> allInfobases = InfobaseReferences.asPlainList(allSections);\n");
        desc.append("// \n");
        desc.append("// for (InfobaseReference infobase : allInfobases) {\n");
        desc.append("//     if (infobase.getName().equals(\"MyInfobaseName\")) {\n");
        desc.append("//         infobaseManager.delete(infobase);\n");
        desc.append("//         System.out.println(\"Infobase deleted from registry: \" + infobase.getName());\n");
        desc.append("//         break;\n");
        desc.append("//     }\n");
        desc.append("// }\n");
        desc.append("*/\n");
        desc.append("```\n");
        desc.append("**Important Notes:**\n");
        desc.append("**Part 1 - Infobase Dissociation:**\n");
        desc.append("- Dissociating infobases before project deletion is RECOMMENDED to clean up associations\n");
        desc.append("- This requires IInfobaseAssociationManager (injected via dependency injection)\n");
        desc.append("- Dissociation fires events that notify listeners (e.g., application deletion notifications)\n");
        desc.append("- If infobases are not dissociated, they remain in the association store (garbage data)\n");
        desc.append("**Part 2 - Project Deletion:**\n");
        desc.append("- First parameter (true): Force delete - deletes project even if resources are locked\n");
        desc.append("- Second parameter (true): Delete content on disk - removes files from filesystem\n");
        desc.append("- Set second parameter to false if you want to keep the project files\n");
        desc.append("- This operation cannot be undone - ensure you have backups if needed\n");
        desc.append("- For production use, use proper IProgressMonitor implementation\n");
        desc.append("- Close the project before deleting to avoid resource locks\n");
        desc.append("- Check for open editors and unsaved changes before deletion\n");
        desc.append("- Verify no other projects reference this project\n");
        desc.append("**Part 3 - Infobase Registry Cleanup (Optional):**\n");
        desc.append("- This removes infobase from EDT's infobase registry list\n");
        desc.append("- Use only if you want to completely remove the infobase\n");
        desc.append("- Requires IInfobaseManager (injected via dependency injection)\n");
        desc.append("- Be careful: this affects all projects that use this infobase\n");
        desc.append("**Alternative: Delete without removing disk files**\n");
        desc.append("```java\n");
        desc.append("// This removes the project from workspace but keeps files on disk\n");
        desc.append("project.delete(true, false, new NullProgressMonitor());\n");
        desc.append("```\n");
        desc.append("**Complete Safe Workflow (recommended for production):**\n");
        desc.append("```java\n");
        desc.append("public void deleteConfigurationProject(String projectName) {\n");
        desc.append("    IProject project = workspaceRoot.getProject(projectName);\n");
        desc.append("    \n");
        desc.append("    if (!project.exists()) {\n");
        desc.append("        System.out.println(\"Project does not exist: \" + projectName);\n");
        desc.append("        // Stop here in JShell because the project does not exist.\n");
        desc.append("    }\n");
        desc.append("    \n");
        desc.append("    try {\n");
        desc.append("        // Step 1: Get IV8Project\n");
        desc.append("        IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("        \n");
        desc.append("        // Step 2: Dissociate infobases (if any)\n");
        desc.append("        // Optional: depends on whether infobase management is needed\n");
        desc.append("/*\n");
        desc.append("        Optional<IInfobaseAssociation> association = associationManager.getAssociation(project);\n");
        desc.append("        if (association.isPresent()) {\n");
        desc.append("            for (InfobaseReference infobase : association.get().getInfobases()) {\n");
        desc.append("                associationManager.dissociate(project, infobase, association.get().getContext());\n");
        desc.append("            }\n");
        desc.append("        }\n");
        desc.append("*/\n");
        desc.append("        \n");
        desc.append("        // Step 3: Check for open editors and unsaved changes\n");
        desc.append("        // This is important to prevent data loss during deletion\n");
        desc.append("        // Note: In production code, you should check for open editors in the workbench\n");
        desc.append("        // and prompt the user to save unsaved changes\n");
        desc.append("        \n");
        desc.append("        // Step 4: Close project\n");
        desc.append("        if (project.isOpen()) {\n");
        desc.append("            project.close(new NullProgressMonitor());\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // Step 5: Delete project\n");
        desc.append("        project.delete(true, true, new NullProgressMonitor());\n");
        desc.append("        \n");
        desc.append("        System.out.println(\"Configuration project deleted successfully: \" + projectName);\n");
        desc.append("        \n");
        desc.append("    } catch (CoreException e) {\n");
        desc.append("        System.err.println(\"Error deleting project: \" + e.getMessage());\n");
        desc.append("        e.printStackTrace();\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**Note:** After creating the common module metadata, create the corresponding Module.bsl file\n");
        desc.append("in the project: `src/CommonModules/<ModuleName>/Module.bsl`\n");
        desc.append("**Deletion:** Common modules can be deleted by removing from configuration.getCommonModules()\n");
        desc.append("and detaching from transaction. Remove the module from configuration.getCommonModules() and detach it from the transaction.\n");
        desc.append("**Properties explanation:**\n");
        desc.append("- `server`: Execution on server side\n");
        desc.append("- `clientManagedApplication`: Execution in managed application client\n");
        desc.append("- `clientOrdinaryApplication`: Execution in ordinary application client\n");
        desc.append("- `serverCall`: Allow calls from client to server\n");
        desc.append("- `externalConnection`: Execution in external connection\n");
        desc.append("- `privileged`: Execution in privileged mode\n");
        desc.append("- `global`: Export module functions to global context\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildEnhancedCatalogCreationWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Enhanced Workflow: Create Catalog with Existence Check and UUID Error Handling\n\n");
        desc.append("This workflow provides a more robust approach with:\n");
        desc.append("- Pre-creation existence check (prevents BmFqnAlreadyInUseException)\n");
        desc.append("- UUID assignment with try-catch error handling\n");
        desc.append("- Comprehensive validation before attachment\n");
        desc.append("- Detailed error reporting\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Catalog result = globalContext.execute(new AbstractBmTask<Catalog>(\"Create catalog safely\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        \n");
        desc.append("        // STEP 1: Check if catalog already exists BEFORE creation\n");
        desc.append("        String catalogFqn = \"Catalog.Products\";\n");
        desc.append("        if (transaction.getTopObjectByFqn(catalogFqn) != null) {\n");
        desc.append("            System.err.println(\"ERROR: Catalog already exists: \" + catalogFqn);\n");
        desc.append("            return null; // Stop creation\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // STEP 2: Create catalog object\n");
        desc.append("        Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("        catalog.setName(\"Products\");\n");
        desc.append("        catalog.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("        catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\n");
        desc.append("        catalog.setCodeLength(9);\n");
        desc.append("        catalog.setDescriptionLength(150);\n");
        desc.append("        \n");
        desc.append("        // STEP 3: Set UUID with error handling\n");
        desc.append("        try {\n");
        desc.append("            catalog.setUuid(UUID.randomUUID());\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for catalog: \" + e.getMessage());\n");
        desc.append("            return null; // Stop creation if UUID cannot be set\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // STEP 4: Create type provider INSIDE transaction\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        \n");
        desc.append("        // STEP 5: Create attributes with UUID error handling\n");
        desc.append("        CatalogAttribute code = mdFactory.createCatalogAttribute();\n");
        desc.append("        code.setName(\"Code\");\n");
        desc.append("        code.getSynonym().put(\"ru\", \"Code\");\n");
        desc.append("        try {\n");
        desc.append("            code.setUuid(UUID.randomUUID()); // CRITICAL: UUID required for all child objects\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for attribute Code: \" + e.getMessage());\n");
        desc.append("            return null; // Stop creation if UUID cannot be set\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // Create type description INSIDE transaction\n");
        desc.append("        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("        TypeDescription codeType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(numberType)\n");
        desc.append("            .build();\n");
        desc.append("        code.setType(codeType);\n");
        desc.append("        catalog.getAttributes().add(code);\n");
        desc.append("        \n");
        desc.append("        CatalogAttribute name = mdFactory.createCatalogAttribute();\n");
        desc.append("        name.setName(\"Name\");\n");
        desc.append("        name.getSynonym().put(\"ru\", \"Name\");\n");
        desc.append("        try {\n");
        desc.append("            name.setUuid(UUID.randomUUID()); // CRITICAL: UUID required for all child objects\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for attribute Name: \" + e.getMessage());\n");
        desc.append("            return null; // Stop creation if UUID cannot be set\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription nameType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(stringType)\n");
        desc.append("            .build();\n");
        desc.append("        name.setType(nameType);\n");
        desc.append("        catalog.getAttributes().add(name);\n");
        desc.append("        \n");
        desc.append("        // STEP 6: Validate before attachment\n");
        desc.append("        if (catalog.getAttributes().isEmpty()) {\n");
        desc.append("            System.err.println(\"ERROR: Catalog has no attributes - this may cause validation issues\");\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // STEP 7: Generate FQN and attach to transaction\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("        \n");
        desc.append("        try {\n");
        desc.append("            transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("            configuration.getCatalogs().add(catalog);\n");
        desc.append("            System.out.println(\"SUCCESS: Catalog created: \" + fqn);\n");
        desc.append("            return catalog;\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to attach catalog: \" + e.getMessage());\n");
        desc.append("            e.printStackTrace();\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Key Improvements:\n\n");
        desc.append("**1. Pre-creation existence check**\n");
        desc.append("- Uses `transaction.getTopObjectByFqn()` BEFORE creating object\n");
        desc.append("- Prevents `BmFqnAlreadyInUseException` runtime errors\n");
        desc.append("- Provides clear error message about why creation stopped\n\n");
        desc.append("**2. UUID assignment with error handling**\n");
        desc.append("- Every UUID assignment wrapped in try-catch block\n");
        desc.append("- Critical for catalog and ALL child objects (attributes, tabular sections)\n");
        desc.append("- Prevents SU45 validation errors (UUID required for all metadata objects)\n");
        desc.append("- Stops creation gracefully if UUID cannot be set\n\n");
        desc.append("**3. Comprehensive validation**\n");
        desc.append("- Checks for empty attribute lists before attachment\n");
        desc.append("- Validates TypeDescription creation inside transaction\n");
        desc.append("- Attachment operation wrapped in try-catch for robustness\n\n");
        desc.append("**4. Detailed error reporting**\n");
        desc.append("- Clear error messages at each failure point\n");
        desc.append("- Stack traces for unexpected exceptions\n");
        desc.append("- Success confirmation message\n\n");
        desc.append("### When to use this workflow:\n");
        desc.append("- Creating new catalogs in production code\n");
        desc.append("- When object existence might be uncertain\n");
        desc.append("- When UUID assignment failures are possible\n");
        desc.append("- For scripts that need reliable error handling\n\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildSafeUuidAssignmentWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe UUID Assignment for All Metadata Objects\n\n");
        desc.append("UUIDs are CRITICAL for all metadata objects and their child elements.\n");
        desc.append("Missing UUIDs cause SU45 validation errors. This workflow provides safe UUID assignment.\n\n");
        desc.append("### Why UUIDs are critical:\n\n");
        desc.append("- **Top-level objects**: Catalogs, Documents, Registers, etc. MUST have UUIDs\n");
        desc.append("- **Child objects**: Attributes, Tabular sections, Dimensions, Resources MUST also have UUIDs\n");
        desc.append("- **Validation**: SU45 error occurs if any object lacks a UUID\n");
        desc.append("- **JShell**: Manual UUID assignment is RECOMMENDED (avoids OSGi timeout from fillDefaultReferences)\n\n");
        desc.append("### Safe UUID Assignment Pattern:\n\n");
        desc.append("```java\n");
        desc.append("// Pattern 1: Safe UUID assignment with error handling\n");
        desc.append("public boolean assignUuidSafely(MdObject object, String objectName) {\n");
        desc.append("    try {\n");
        desc.append("        object.setUuid(UUID.randomUUID());\n");
        desc.append("        return true; // Success\n");
        desc.append("    } catch (Exception e) {\n");
        desc.append("        System.err.println(\"ERROR: Failed to set UUID for \" + objectName + \": \" + e.getMessage());\n");
        desc.append("        e.printStackTrace();\n");
        desc.append("        return false; // Failure\n");
        desc.append("    }\n");
        desc.append("}\n");
        desc.append("\n");
        desc.append("// Pattern 2: Batch UUID assignment for child objects\n");
        desc.append("public boolean assignUuidsToChildren(java.util.List<? extends MdObject> children, String parentName) {\n");
        desc.append("    boolean allSuccess = true;\n");
        desc.append("    for (MdObject child : children) {\n");
        desc.append("        try {\n");
        desc.append("            child.setUuid(UUID.randomUUID());\n");
        desc.append("            System.out.println(\"UUID assigned to: \" + parentName + \".\" + child.getName());\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for \" + parentName + \".\" + child.getName() + \": \" + e.getMessage());\n");
        desc.append("            allSuccess = false; // Continue with remaining objects\n");
        desc.append("        }\n");
        desc.append("    }\n");
        desc.append("    return allSuccess;\n");
        desc.append("}\n");
        desc.append("```\n\n");
        desc.append("### Complete Example: Catalog with All UUIDs Safely Assigned\n\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("\n");
        desc.append("// Assign UUID to top-level object\n");
        desc.append("if (!assignUuidSafely(catalog, \"Catalog.Products\")) {\n");
        desc.append("    System.err.println(\"FATAL: Cannot create catalog without UUID\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("\n");
        desc.append("// Create and assign UUIDs to attributes\n");
        desc.append("CatalogAttribute code = mdFactory.createCatalogAttribute();\n");
        desc.append("code.setName(\"Code\");\n");
        desc.append("if (!assignUuidSafely(code, \"Catalog.Products.Code\")) {\n");
        desc.append("    System.err.println(\"FATAL: Cannot create attribute without UUID\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("\n");
        desc.append("CatalogAttribute description = mdFactory.createCatalogAttribute();\n");
        desc.append("description.setName(\"Description\");\n");
        desc.append("if (!assignUuidSafely(description, \"Catalog.Products.Description\")) {\n");
        desc.append("    System.err.println(\"FATAL: Cannot create attribute without UUID\");\n");
        desc.append("    return null;\n");
        desc.append("}\n");
        desc.append("\n");
        desc.append("// Assign UUIDs to all child objects at once\n");
        desc.append("java.util.List<MdObject> attributes = new java.util.ArrayList<>();\n");
        desc.append("attributes.add(code);\n");
        desc.append("attributes.add(description);\n");
        desc.append("\n");
        desc.append("if (!assignUuidsToChildren(attributes, \"Catalog.Products\")) {\n");
        desc.append("    System.err.println(\"WARNING: Some attributes failed UUID assignment\");\n");
        desc.append("    // Decide whether to continue or abort\n");
        desc.append("}\n");
        desc.append("\n");
        desc.append("// Set types and add to catalog\n");
        desc.append("// ... (TypeDescription creation and assignment)\n");
        desc.append("catalog.getAttributes().add(code);\n");
        desc.append("catalog.getAttributes().add(description);\n");
        desc.append("```\n\n");
        desc.append("### UUID Assignment for Different Metadata Types:\n\n");
        desc.append("**Catalog with attributes:**\n");
        desc.append("```java\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("catalog.getAttributes().forEach(attr -> attr.setUuid(UUID.randomUUID()));\n");
        desc.append("catalog.getTabularSections().forEach(ts -> {\n");
        desc.append("    ts.setUuid(UUID.randomUUID());\n");
        desc.append("    ts.getAttributes().forEach(tsa -> tsa.setUuid(UUID.randomUUID()));\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("**Document with attributes and tabular sections:**\n");
        desc.append("```java\n");
        desc.append("document.setUuid(UUID.randomUUID());\n");
        desc.append("document.getAttributes().forEach(attr -> attr.setUuid(UUID.randomUUID()));\n");
        desc.append("document.getTabularSections().forEach(ts -> {\n");
        desc.append("    ts.setUuid(UUID.randomUUID());\n");
        desc.append("    ts.getAttributes().forEach(tsa -> tsa.setUuid(UUID.randomUUID()));\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("**InformationRegister with dimensions and resources:**\n");
        desc.append("```java\n");
        desc.append("register.setUuid(UUID.randomUUID());\n");
        desc.append("register.getDimensions().forEach(dim -> dim.setUuid(UUID.randomUUID()));\n");
        desc.append("register.getResources().forEach(res -> res.setUuid(UUID.randomUUID()));\n");
        desc.append("register.getAttributes().forEach(attr -> attr.setUuid(UUID.randomUUID()));\n");
        desc.append("```\n\n");
        desc.append("**Enum with enum values:**\n");
        desc.append("```java\n");
        desc.append("enumObject.setUuid(UUID.randomUUID());\n");
        desc.append("enumObject.getEnumValues().forEach(value -> value.setUuid(UUID.randomUUID()));\n");
        desc.append("```\n\n");
        desc.append("**Common Pitfalls to Avoid:**\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG: Forgetting child object UUIDs\n");
        desc.append("catalog.setUuid(UUID.randomUUID()); // OK\n");
        desc.append("catalog.getAttributes().add(attr); // ❌ attr has no UUID! SU45 error!\n");
        desc.append("\n");
        desc.append("// ❌ WRONG: Using fillDefaultReferences in JShell (may timeout)\n");
        desc.append("// modelFactory.fillDefaultReferences(catalog); // DO NOT USE in JShell!\n");
        desc.append("\n");
        desc.append("// ❌ WRONG: Not checking UUID assignment success\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("// If this fails, object will have null UUID and cause SU45 error\n");
        desc.append("\n");
        desc.append("// ✅ CORRECT: Manual UUID assignment with error handling\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("attr.setUuid(UUID.randomUUID());\n");
        desc.append("// Both objects have valid UUIDs\n");
        desc.append("```\n\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildTypeDescriptionBuilderBestPractices()
    {
        var desc = new StringBuilder();
        desc.append("## TypeDescriptionBuilder Best Practices - Transaction Context\n\n");
        desc.append("### ⚠️ CRITICAL: TypeDescription Must Be Created Inside Transaction\n\n");
        desc.append("TypeDescription and TypeItem proxies MUST be created and used within the SAME BM transaction.\n");
        desc.append("Creating TypeDescription outside the transaction context can lead to:\n");
        desc.append("- NullPointerException when setting types\n");
        desc.append("- Inconsistent type resolution\n");
        desc.append("- Transaction isolation violations\n\n");
        desc.append("### ✅ CORRECT: Complete TypeDescription Creation Pattern\n\n");
        desc.append("```java\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create metadata\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        \n");
        desc.append("        // STEP 1: Get typeProvider INSIDE transaction\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        \n");
        desc.append("        // STEP 2: Get TypeItem proxies INSIDE transaction\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("        TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("        \n");
        desc.append("        // STEP 3: Validate proxies before building\n");
        desc.append("        if (stringType == null) {\n");
        desc.append("            System.err.println(\"ERROR: Cannot resolve STRING type\");\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        if (catalogRef == null) {\n");
        desc.append("            System.err.println(\"ERROR: Cannot resolve CATALOG_REF type\");\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // STEP 4: Build TypeDescription INSIDE transaction\n");
        desc.append("        TypeDescription stringTypeDesc = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(stringType)\n");
        desc.append("            .build();\n");
        desc.append("        \n");
        desc.append("        TypeDescription catalogRefTypeDesc = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(catalogRef)\n");
        desc.append("            .build();\n");
        desc.append("        \n");
        desc.append("        // STEP 5: Set TypeDescription to attribute INSIDE transaction\n");
        desc.append("        attribute.setType(stringTypeDesc);\n");
        desc.append("        referenceAttribute.setType(catalogRefTypeDesc);\n");
        desc.append("        \n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### ❌ WRONG Patterns:\n\n");
        desc.append("**Pattern 1: Getting typeProvider outside transaction**\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG: TypeProvider created OUTSIDE transaction\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Create metadata\") {\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        attribute.setType(new TypeDescriptionBuilder().addType(stringType).build());\n");
        desc.append("        // ❌ May cause NullPointerException\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("**Pattern 2: Using TypeItem from different transaction**\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG: TypeItem from transaction #1 used in transaction #2\n");
        desc.append("TypeItem typeFromTrans1 = null;\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Transaction 1\") {\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        typeFromTrans1 = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Transaction 2\") {\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        attribute.setType(new TypeDescriptionBuilder().addType(typeFromTrans1).build());\n");
        desc.append("        // ❌ TypeItem is invalid for this transaction\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Frequently Used Patterns:\n\n");
        desc.append("**Pattern 1: Simple String Type**\n");
        desc.append("```java\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder().addType(stringType).build();\n");
        desc.append("attribute.setType(typeDesc);\n");
        desc.append("```\n\n");
        desc.append("**Pattern 2: Number with Qualifiers**\n");
        desc.append("```java\n");
        desc.append("IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder().addType(numberType).build();\n");
        desc.append("\n");
        desc.append("// Set number qualifiers (inside transaction)\n");
        desc.append("if (typeDesc.getNumberQualifiers() == null) {\n");
        desc.append("    typeDesc.setNumberQualifiers(modelFactory.createNumberQualifiers());\n");
        desc.append("}\n");
        desc.append("typeDesc.getNumberQualifiers().setPrecision(10); // Total digits\n");
        desc.append("typeDesc.getNumberQualifiers().setScale(2);     // Decimal places\n");
        desc.append("// NOTE: Scale must be <= Precision to avoid SU8 error\n");
        desc.append("```\n\n");
        desc.append("**Pattern 3: Composite Type (String or Number)**\n");
        desc.append("```java\n");
        desc.append("TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("TypeDescription compositeType = new TypeDescriptionBuilder()\n");
        desc.append("    .addType(stringType)\n");
        desc.append("    .addType(numberType)\n");
        desc.append("    .build();\n");
        desc.append("attribute.setType(compositeType);\n");
        desc.append("```\n\n");
        desc.append("**Pattern 4: Catalog Reference (Generic)**\n");
        desc.append("```java\n");
        desc.append("TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder().addType(catalogRef).build();\n");
        desc.append("attribute.setType(typeDesc);\n");
        desc.append("```\n\n");
        desc.append("**Pattern 5: Specific Catalog Reference (Requires existing catalog)**\n");
        desc.append("```java\n");
        desc.append("TypeItem productsRef = (TypeItem)typeProvider.getProxy(\"Catalog.Products\");\n");
        desc.append("if (productsRef == null) {\n");
        desc.append("    System.err.println(\"ERROR: Catalog.Products does not exist\");\n");
        desc.append("    return null; // Stop creation\n");
        desc.append("}\n");
        desc.append("TypeDescription typeDesc = new TypeDescriptionBuilder().addType(productsRef).build();\n");
        desc.append("attribute.setType(typeDesc);\n");
        desc.append("```\n\n");
        desc.append("**Pattern 6: Fallback Pattern (Specific → Generic)**\n");
        desc.append("```java\n");
        desc.append("TypeItem unitsRef = (TypeItem)typeProvider.getProxy(\"Catalog.Units\");\n");
        desc.append("TypeDescription typeDesc;\n");
        desc.append("if (unitsRef == null) {\n");
        desc.append("    System.out.println(\"WARNING: Catalog.Units not found, using generic CATALOG_REF\");\n");
        desc.append("    TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("    typeDesc = new TypeDescriptionBuilder().addType(catalogRef).build();\n");
        desc.append("} else {\n");
        desc.append("    typeDesc = new TypeDescriptionBuilder().addType(unitsRef).build();\n");
        desc.append("}\n");
        desc.append("attribute.setType(typeDesc);\n");
        desc.append("```\n\n");
        desc.append("### Performance Considerations:\n");
        desc.append("- **Reuse TypeDescription**: Create once, use multiple times for same type\n");
        desc.append("- **Validate early**: Check `typeProvider.getProxy(...)` before building\n");
        desc.append("- **Minimize proxy calls**: Get all needed TypeItems at once\n");
        desc.append("- **Stay in transaction**: All TypeDescription operations in same transaction\n\n");
        desc.append("### Common Errors and Solutions:\n");
        desc.append("**Error**: `NullPointerException` when setting type\n");
        desc.append("**Solution**: Ensure typeProvider and TypeItem are created INSIDE transaction\n\n");
        desc.append("**Error**: `IllegalArgumentException` when adding type\n");
        desc.append("**Solution**: Validate `typeProvider.getProxy(...)` returns non-null before `addType(...)`\n\n");
        desc.append("**Error**: SU8 - Scale > Precision\n");
        desc.append("**Solution**: Ensure `scale <= precision` for Number types\n\n");
        desc.append("**Error**: Type not found for specific FQN\n");
        desc.append("**Solution**: Use fallback pattern or ensure referenced metadata exists first\n\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildEnhancedDocumentCreationWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Enhanced Workflow: Create Document with Full Validation\n\n");
        desc.append("This workflow provides comprehensive document creation with:\n");
        desc.append("- Existence check before creation\n");
        desc.append("- UUID assignment for all objects (document, attributes, tabular sections, line attributes)\n");
        desc.append("- Type validation and error handling\n");
        desc.append("- Complete validation before attachment\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Document result = globalContext.execute(new AbstractBmTask<Document>(\"Create document safely\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        \n");
        desc.append("        // CHECK 1: Document existence\n");
        desc.append("        String documentFqn = \"Document.GoodsReceipt\";\n");
        desc.append("        if (transaction.getTopObjectByFqn(documentFqn) != null) {\n");
        desc.append("            System.err.println(\"ERROR: Document already exists: \" + documentFqn);\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // CREATE document\n");
        desc.append("        Document document = mdFactory.createDocument();\n");
        desc.append("        document.setName(\"GoodsReceipt\");\n");
        desc.append("        document.getSynonym().put(\"ru\", \"Приход товаров\");\n");
        desc.append("        \n");
        desc.append("        // SET document properties\n");
        desc.append("        document.setNumberType(DocumentNumberType.NUMBER);\n");
        desc.append("        document.setNumberLength(9);\n");
        desc.append("        document.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);\n");
        desc.append("        document.setRealTimePosting(RealTimePosting.DENY);\n");
        desc.append("        \n");
        desc.append("        // ASSIGN UUID to document\n");
        desc.append("        try {\n");
        desc.append("            document.setUuid(UUID.randomUUID());\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for document: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // CREATE type provider\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        \n");
        desc.append("        // CREATE attributes with UUID assignment\n");
        desc.append("        DocumentAttribute warehouse = mdFactory.createDocumentAttribute();\n");
        desc.append("        warehouse.setName(\"Warehouse\");\n");
        desc.append("        warehouse.getSynonym().put(\"ru\", \"Склад\");\n");
        desc.append("        try {\n");
        desc.append("            warehouse.setUuid(UUID.randomUUID()); // CRITICAL: UUID for all child objects\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for Warehouse attribute: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("        TypeDescription warehouseType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(catalogRefType)\n");
        desc.append("            .build();\n");
        desc.append("        warehouse.setType(warehouseType);\n");
        desc.append("        document.getAttributes().add(warehouse);\n");
        desc.append("        \n");
        desc.append("        // CREATE tabular section\n");
        desc.append("        DocumentTabularSection products = mdFactory.createDocumentTabularSection();\n");
        desc.append("        products.setName(\"Products\");\n");
        desc.append("        products.getSynonym().put(\"ru\", \"Товары\");\n");
        desc.append("        try {\n");
        desc.append("            products.setUuid(UUID.randomUUID()); // CRITICAL: UUID for tabular section\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for Products tabular section: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // CREATE tabular section attributes with UUID assignment\n");
        desc.append("        TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();\n");
        desc.append("        product.setName(\"Product\");\n");
        desc.append("        product.getSynonym().put(\"ru\", \"Номенклатура\");\n");
        desc.append("        try {\n");
        desc.append("            product.setUuid(UUID.randomUUID()); // CRITICAL: UUID for line attributes\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for Product attribute: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        TypeDescription productType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(catalogRefType)\n");
        desc.append("            .build();\n");
        desc.append("        product.setType(productType);\n");
        desc.append("        products.getAttributes().add(product);\n");
        desc.append("        \n");
        desc.append("        TabularSectionAttribute quantity = mdFactory.createTabularSectionAttribute();\n");
        desc.append("        quantity.setName(\"Quantity\");\n");
        desc.append("        quantity.getSynonym().put(\"ru\", \"Количество\");\n");
        desc.append("        try {\n");
        desc.append("            quantity.setUuid(UUID.randomUUID()); // CRITICAL: UUID for line attributes\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for Quantity attribute: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("        TypeDescription quantityType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(numberType)\n");
        desc.append("            .build();\n");
        desc.append("        quantity.setType(quantityType);\n");
        desc.append("        products.getAttributes().add(quantity);\n");
        desc.append("        document.getTabularSections().add(products);\n");
        desc.append("        \n");
        desc.append("        // VALIDATE before attachment\n");
        desc.append("        if (document.getAttributes().isEmpty()) {\n");
        desc.append("            System.err.println(\"ERROR: Document has no attributes\");\n");
        desc.append("        }\n");
        desc.append("        if (document.getTabularSections().isEmpty()) {\n");
        desc.append("            System.out.println(\"WARNING: Document has no tabular sections\");\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // ATTACH and add to configuration\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();\n");
        desc.append("        try {\n");
        desc.append("            transaction.attachTopObject((IBmObject)document, fqn);\n");
        desc.append("            configuration.getDocuments().add(document);\n");
        desc.append("            System.out.println(\"SUCCESS: Document created: \" + fqn);\n");
        desc.append("            return document;\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to attach document: \" + e.getMessage());\n");
        desc.append("            e.printStackTrace();\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### UUID Assignment Checklist for Documents:\n");
        desc.append("- [ ] Document top-level object: `document.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] All DocumentAttribute objects: `attr.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] All DocumentTabularSection objects: `ts.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] All TabularSectionAttribute objects: `tsa.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] Wrap ALL UUID assignments in try-catch blocks\n");
        desc.append("- [ ] Abort creation if any UUID assignment fails\n\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildEnhancedRegisterCreationWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Enhanced Workflow: Create Register with Full Validation\n\n");
        desc.append("This workflow provides comprehensive register creation (Information, Accumulation, Accounting, Calculation) with:\n");
        desc.append("- Existence check before creation\n");
        desc.append("- UUID assignment for all objects (register, dimensions, resources, attributes)\n");
        desc.append("- Type validation and error handling\n");
        desc.append("- Complete validation before attachment\n\n");
        desc.append("### Pattern for All Register Types:\n\n");
        desc.append("```java\n");
        desc.append("// This pattern applies to:\n");
        desc.append("// - InformationRegister\n");
        desc.append("// - AccumulationRegister\n");
        desc.append("// - AccountingRegister\n");
        desc.append("// - CalculationRegister\n");
        desc.append("\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("InformationRegister result = globalContext.execute(new AbstractBmTask<InformationRegister>(\"Create register safely\") {\n");
        desc.append("    @Override\n");
        desc.append("    public InformationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        \n");
        desc.append("        // CHECK 1: Register existence\n");
        desc.append("        String registerFqn = \"InformationRegister.Prices\";\n");
        desc.append("        if (transaction.getTopObjectByFqn(registerFqn) != null) {\n");
        desc.append("            System.err.println(\"ERROR: Register already exists: \" + registerFqn);\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // CREATE register\n");
        desc.append("        InformationRegister register = mdFactory.createInformationRegister();\n");
        desc.append("        register.setName(\"Prices\");\n");
        desc.append("        register.getSynonym().put(\"ru\", \"Цены\");\n");
        desc.append("        register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY);\n");
        desc.append("        register.setUseStandardCommands(true);\n");
        desc.append("        \n");
        desc.append("        // ASSIGN UUID to register\n");
        desc.append("        try {\n");
        desc.append("            register.setUuid(UUID.randomUUID());\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for register: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // CREATE type provider\n");
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        \n");
        desc.append("        // CREATE dimension with UUID\n");
        desc.append("        InformationRegisterDimension product = mdFactory.createInformationRegisterDimension();\n");
        desc.append("        product.setName(\"Product\");\n");
        desc.append("        product.getSynonym().put(\"ru\", \"Товар\");\n");
        desc.append("        try {\n");
        desc.append("            product.setUuid(UUID.randomUUID()); // CRITICAL: UUID for dimensions\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for dimension Product: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("        TypeDescription productType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(catalogRefType)\n");
        desc.append("            .build();\n");
        desc.append("        product.setType(productType);\n");
        desc.append("        register.getDimensions().add(product);\n");
        desc.append("        \n");
        desc.append("        // CREATE resource with UUID\n");
        desc.append("        InformationRegisterResource price = mdFactory.createInformationRegisterResource();\n");
        desc.append("        price.setName(\"Price\");\n");
        desc.append("        price.getSynonym().put(\"ru\", \"Цена\");\n");
        desc.append("        try {\n");
        desc.append("            price.setUuid(UUID.randomUUID()); // CRITICAL: UUID for resources\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to set UUID for resource Price: \" + e.getMessage());\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);\n");
        desc.append("        TypeDescription priceType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(numberType)\n");
        desc.append("            .build();\n");
        desc.append("        price.setType(priceType);\n");
        desc.append("        register.getResources().add(price);\n");
        desc.append("        \n");
        desc.append("        // VALIDATE before attachment\n");
        desc.append("        if (register.getDimensions().isEmpty()) {\n");
        desc.append("            System.err.println(\"ERROR: Register has no dimensions (at least one required)\");\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        // ATTACH and add to configuration\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();\n");
        desc.append("        try {\n");
        desc.append("            transaction.attachTopObject((IBmObject)register, fqn);\n");
        desc.append("            configuration.getInformationRegisters().add(register);\n");
        desc.append("            System.out.println(\"SUCCESS: Register created: \" + fqn);\n");
        desc.append("            return register;\n");
        desc.append("        } catch (Exception e) {\n");
        desc.append("            System.err.println(\"ERROR: Failed to attach register: \" + e.getMessage());\n");
        desc.append("            e.printStackTrace();\n");
        desc.append("            return null;\n");
        desc.append("        }\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### UUID Assignment Checklist for Registers:\n");
        desc.append("- [ ] Register top-level object: `register.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] All InformationRegisterDimension objects: `dim.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] All InformationRegisterResource objects: `res.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] All RegisterAttribute objects: `attr.setUuid(UUID.randomUUID())`\n");
        desc.append("- [ ] For CalculationRegister: Recalculation objects need UUID\n");
        desc.append("- [ ] Wrap ALL UUID assignments in try-catch blocks\n");
        desc.append("- [ ] Abort creation if any UUID assignment fails\n\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildChildElementsUuidImportance()
    {
        var desc = new StringBuilder();
        desc.append("## CRITICAL: UUID Importance for Child Elements\n\n");
        desc.append("### Why UUIDs are Required for ALL Child Elements\n\n");
        desc.append("**Metadata Model Requirements:**\n");
        desc.append("- ALL metadata objects derived from `MdObject` MUST have UUIDs\n");
        desc.append("- This includes ALL child elements (attributes, tabular sections, dimensions, resources)\n");
        desc.append("- Missing UUIDs cause SU45 validation errors\n");
        desc.append("- UUIDs are used for object identification and references\n\n");
        desc.append("**Common Child Elements That Need UUIDs:**\n");
        desc.append("\n");
        desc.append("**Catalogs:**\n");
        desc.append("- CatalogAttribute\n");
        desc.append("- CatalogTabularSection\n");
        desc.append("- TabularSectionAttribute (inside tabular sections)\n");
        desc.append("- PredefinedItem (predefined catalog items)\n\n");
        desc.append("**Documents:**\n");
        desc.append("- DocumentAttribute\n");
        desc.append("- DocumentTabularSection\n");
        desc.append("- TabularSectionAttribute (inside tabular sections)\n\n");
        desc.append("**Registers (Information, Accumulation, Accounting, Calculation):**\n");
        desc.append("- RegisterAttribute\n");
        desc.append("- InformationRegisterDimension\n");
        desc.append("- InformationRegisterResource\n");
        desc.append("- AccumulationRegisterDimension\n");
        desc.append("- AccumulationRegisterResource\n");
        desc.append("- AccountingRegisterDimension\n");
        desc.append("- AccountingRegisterResource\n");
        desc.append("- CalculationRegisterDimension\n");
        desc.append("- CalculationRegisterResource\n");
        desc.append("- Recalculation (CalculationRegister specific)\n");
        desc.append("- RecalculationDimension (inside Recalculation)\n\n");
        desc.append("**BusinessProcess / Task:**\n");
        desc.append("- BusinessProcessAttribute\n");
        desc.append("- TaskAttribute\n");
        desc.append("- BusinessProcessTabularSection\n");
        desc.append("- TaskTabularSection\n");
        desc.append("- TabularSectionAttribute (inside tabular sections)\n\n");
        desc.append("**Enum:**\n");
        desc.append("- EnumValue\n\n");
        desc.append("**Common Forms, Commands, Templates:**\n");
        desc.append("- BasicForm (attached to any metadata object)\n");
        desc.append("- BasicCommand (attached to any metadata object)\n");
        desc.append("- Template (attached to any metadata object)\n\n");
        desc.append("**CommonModule:**\n");
        desc.append("- Method (inside module)\n");
        desc.append("- Parameter (inside Method)\n\n");
        desc.append("**ExternalDataSource:**\n");
        desc.append("- Table\n");
        desc.append("- Column (inside Table)\n");
        desc.append("- Cube\n");
        desc.append("- DimensionTable (inside Cube)\n");
        desc.append("- Table (inside Cube)\n");
        desc.append("- Column (inside Cube tables)\n\n");
        desc.append("### UUID Assignment Pattern:\n\n");
        desc.append("```java\n");
        desc.append("// Pattern 1: Top-level object\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.setUuid(UUID.randomUUID()); // ✅ REQUIRED\n");
        desc.append("\n");
        desc.append("// Pattern 2: Child object (attribute)\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setName(\"Code\");\n");
        desc.append("attr.setUuid(UUID.randomUUID()); // ✅ REQUIRED\n");
        desc.append("\n");
        desc.append("// Pattern 3: Nested child object (tabular section attribute)\n");
        desc.append("CatalogTabularSection ts = mdFactory.createCatalogTabularSection();\n");
        desc.append("ts.setName(\"Items\");\n");
        desc.append("ts.setUuid(UUID.randomUUID()); // ✅ REQUIRED\n");
        desc.append("\n");
        desc.append("TabularSectionAttribute tsa = mdFactory.createTabularSectionAttribute();\n");
        desc.append("tsa.setName(\"Quantity\");\n");
        desc.append("tsa.setUuid(UUID.randomUUID()); // ✅ REQUIRED\n");
        desc.append("ts.getAttributes().add(tsa);\n");
        desc.append("catalog.getTabularSections().add(ts);\n");
        desc.append("\n");
        desc.append("// Pattern 4: Multiple child objects\n");
        desc.append("catalog.getAttributes().forEach(attr -> attr.setUuid(UUID.randomUUID()));\n");
        desc.append("catalog.getTabularSections().forEach(ts -> {\n");
        desc.append("    ts.setUuid(UUID.randomUUID());\n");
        desc.append("    ts.getAttributes().forEach(tsa -> tsa.setUuid(UUID.randomUUID()));\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Common Mistakes:\n");
        desc.append("```java\n");
        desc.append("// ❌ WRONG: Forgetting child object UUIDs\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setUuid(UUID.randomUUID()); // ✅ OK\n");
        desc.append("catalog.getAttributes().add(attr); // ❌ attr has no UUID! SU45 error!\n");
        desc.append("\n");
        desc.append("// ❌ WRONG: Using fillDefaultReferences in JShell\n");
        desc.append("// modelFactory.fillDefaultReferences(catalog); // May timeout\n");
        desc.append("\n");
        desc.append("// ❌ WRONG: Not handling UUID assignment errors\n");
        desc.append("attr.setUuid(UUID.randomUUID());\n");
        desc.append("// If this fails silently, attr.getUuid() remains null → SU45 error\n");
        desc.append("\n");
        desc.append("// ✅ CORRECT: Manual UUID assignment with error handling\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("attr.setUuid(UUID.randomUUID());\n");
        desc.append("// All objects have valid UUIDs\n");
        desc.append("\n");
        desc.append("// ✅ CORRECT: Error handling for UUID assignment\n");
        desc.append("try {\n");
        desc.append("    attr.setUuid(UUID.randomUUID());\n");
        desc.append("} catch (Exception e) {\n");
        desc.append("    System.err.println(\"ERROR: Cannot create attribute without UUID\");\n");
        desc.append("    return null; // Stop creation\n");
        desc.append("}\n");
        desc.append("```\n\n");
        desc.append("### Validation SU45 - UUID Required:\n");
        desc.append("When SU45 error occurs:\n");
        desc.append("1. Check ALL top-level objects have UUIDs\n");
        desc.append("2. Check ALL child objects have UUIDs (attributes, tabular sections, etc.)\n");
        desc.append("3. Check ALL nested child objects (tabular section attributes)\n");
        desc.append("4. Ensure UUID is set BEFORE adding to parent collection\n");
        desc.append("5. Verify no UUID assignment exceptions were silently ignored\n\n");
        desc.append("### Summary:\n");
        desc.append("- **ALL** metadata objects need UUIDs, not just top-level\n");
        desc.append("- Child elements are often forgotten, causing SU45 errors\n");
        desc.append("- Use manual UUID assignment with error handling\n");
        desc.append("- Validate UUID assignment success before attachment\n\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDocumentRegistersWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Add Document Registers (Set Up Registrars on Document Side)\n\n");
        desc.append("Registrars are configured on the **document** side through `Document.getRegisterRecords()`. \n");
        desc.append("This creates a bidirectional relationship: document → register.\n\n");
        desc.append("**⚠️ IMPORTANT:** Registers do NOT have `getRegisteredDocuments()` method. \n");
        desc.append("Registrars are managed from the document side only.\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Add document registers\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        \n");
        desc.append("        // Get the document to configure\n");
        desc.append("        Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("        \n");
        desc.append("        // Get the registers to add as registrars\n");
        desc.append("        AccumulationRegister stockRegister = (AccumulationRegister)transaction.getTopObjectByFqn(\"AccumulationRegister.GoodsInStock\");\n");
        desc.append("        InformationRegister priceRegister = (InformationRegister)transaction.getTopObjectByFqn(\"InformationRegister.Prices\");\n");
        desc.append("        \n");
        desc.append("        if (document != null) {\n");
        desc.append("            // Add accumulation register as registrar\n");
        desc.append("            if (stockRegister != null) {\n");
        desc.append("                document.getRegisterRecords().add(stockRegister);\n");
        desc.append("                System.out.println(\"Added: AccumulationRegister.GoodsInStock as registrar\");\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            // Add information register as registrar\n");
        desc.append("            if (priceRegister != null) {\n");
        desc.append("                document.getRegisterRecords().add(priceRegister);\n");
        desc.append("                System.out.println(\"Added: InformationRegister.Prices as registrar\");\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            System.out.println(\"Document registers configured successfully\");\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Key Points:\n");
        desc.append("- **Document side**: Registrators are configured on `Document.getRegisterRecords()`\n");
        desc.append("- **No register.getRegisteredDocuments()**: Registers don't have this method\n");
        desc.append("- **Bidirectional**: Setting document→register establishes both directions\n");
        desc.append("- **Multiple registers**: One document can have multiple registers\n");
        desc.append("- **Accumulation registers**: Use for stock/quantity tracking\n");
        desc.append("- **Accounting registers**: Use for accounting operations\n");
        desc.append("- **Calculation registers**: Use for payroll and calculation operations\n");
        desc.append("\n");
        desc.append("### \u26A0 CRITICAL: InformationRegister CANNOT be a registrar\n");
        desc.append("**IMPORTANT**: InformationRegister cannot be added to `Document.getRegisterRecords()`.\n");
        desc.append("InformationRegister stores periodic data but is NOT a register for documents.\n");
        desc.append("\n");
        desc.append("### Valid Register Types for Documents:\n");
        desc.append("- **AccumulationRegister**: Stock registers (BALANCE or TURNOVERS) - CAN be a registrar\n");
        desc.append("- **AccountingRegister**: Chart of accounts-based accounting - CAN be a registrar\n");
        desc.append("- **CalculationRegister**: Payroll and calculation registers - CAN be a registrar\n");
        desc.append("- **InformationRegister**: Periodic data - CANNOT be a registrar (error: SU45)\n");
        desc.append("- **Note**: Only AccumulationRegister, AccountingRegister, and CalculationRegister can be document registrars\n");
        desc.append("\n");
        desc.append("### Verification:\n");
        desc.append("After adding registers, verify:\n");
        desc.append("```java\n");
        desc.append("System.out.println(\"Document registers: \" + document.getRegisterRecords().size());\n");
        desc.append("document.getRegisterRecords().forEach(reg -> \n");
        desc.append("    System.out.println(\"  \" + reg.getName() + \" (\" + reg.eClass().getName() + \")\"));\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDocumentJournalDocumentsWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Add Registered Documents To Document Journal\n\n");
        desc.append("Registered documents are configured on the **journal** side through `DocumentJournal.getRegisteredDocuments()`. \n");
        desc.append("This determines which documents appear in the journal.\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Add journal documents\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        \n");
        desc.append("        // Get the journal to configure\n");
        desc.append("        DocumentJournal journal = (DocumentJournal)transaction.getTopObjectByFqn(\"DocumentJournal.GoodsMovement\");\n");
        desc.append("        \n");
        desc.append("        // Get the documents to add to the journal\n");
        desc.append("        Document receiptDoc = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("        Document saleDoc = (Document)transaction.getTopObjectByFqn(\"Document.GoodsSale\");\n");
        desc.append("        Document returnDoc = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReturn\");\n");
        desc.append("        \n");
        desc.append("        if (journal != null) {\n");
        desc.append("            // Add documents to the journal\n");
        desc.append("            if (receiptDoc != null) {\n");
        desc.append("                journal.getRegisteredDocuments().add(receiptDoc);\n");
        desc.append("                System.out.println(\"Added: Document.GoodsReceipt\");\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            if (saleDoc != null) {\n");
        desc.append("                journal.getRegisteredDocuments().add(saleDoc);\n");
        desc.append("                System.out.println(\"Added: Document.GoodsSale\");\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            if (returnDoc != null) {\n");
        desc.append("                journal.getRegisteredDocuments().add(returnDoc);\n");
        desc.append("                System.out.println(\"Added: Document.GoodsReturn\");\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            System.out.println(\"Journal documents configured successfully\");\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Key Points:\n");
        desc.append("- **Journal side**: Registered documents are configured on `DocumentJournal.getRegisteredDocuments()`\n");
        desc.append("- **Multiple documents**: One journal can contain multiple documents\n");
        desc.append("- **Document order**: Order in collection determines display order in journal\n");
        desc.append("- **Clear existing**: Use `clear()` to remove all documents before adding new ones\n");
        desc.append("- **Journal purpose**: Groups related documents for easier navigation and filtering\n");
        desc.append("- **No registration required**: Adding to journal doesn't affect registrar relationships\n");
        desc.append("\n");
        desc.append("### Verification:\n");
        desc.append("After adding documents, verify:\n");
        desc.append("```java\n");
        desc.append("System.out.println(\"Journal documents: \" + journal.getRegisteredDocuments().size());\n");
        desc.append("journal.getRegisteredDocuments().forEach(doc -> \n");
        desc.append("    System.out.println(\"  \" + doc.getName() + \" (\" + doc.getSynonym().get(\"ru\") + \")\"));\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildExchangePlanThisNodeWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Set Exchange Plan ThisNode\n\n");
        desc.append("The `thisNode` property identifies which node of the exchange plan represents the current system. \n");
        desc.append("This is a UUID reference that must be set for the exchange plan to work correctly.\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Set exchange plan thisNode\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("        \n");
        desc.append("        // Get the exchange plan\n");
        desc.append("        ExchangePlan exchangePlan = (ExchangePlan)transaction.getTopObjectByFqn(\"ExchangePlan.Synchronization\");\n");
        desc.append("        \n");
        desc.append("        if (exchangePlan != null) {\n");
        desc.append("            // Generate a random UUID for the thisNode\n");
        desc.append("            UUID thisNodeUUID = UUID.randomUUID();\n");
        desc.append("            \n");
        desc.append("            // Set the thisNode property\n");
        desc.append("            exchangePlan.setThisNode(thisNodeUUID);\n");
        desc.append("            \n");
        desc.append("            System.out.println(\"ExchangePlan thisNode set to: \" + thisNodeUUID);\n");
        desc.append("            System.out.println(\"This node now participates in exchange\");\n");
        desc.append("        } else {\n");
        desc.append("            System.err.println(\"ExchangePlan not found\");\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n\n");
        desc.append("### Key Points:\n");
        desc.append("- **UUID property**: `setThisNode(UUID)` takes a UUID, not a node object\n");
        desc.append("- **Random UUID**: Use `UUID.randomUUID()` for new nodes\n");
        desc.append("- **Existing nodes**: If participating in existing exchange, use UUID from other node\n");
        desc.append("- **Content items**: ExchangePlan content items are separate from thisNode\n");
        desc.append("- **Bidirectional**: Set thisNode on each participating node in the exchange\n");
        desc.append("\n");
        desc.append("### Common Scenarios:\n");
        desc.append("\n**1. New exchange plan (single node):**\n");
        desc.append("```java\n");
        desc.append("UUID thisNodeUUID = UUID.randomUUID();\n");
        desc.append("exchangePlan.setThisNode(thisNodeUUID);\n");
        desc.append("// This node is now the only participant\n");
        desc.append("```\n");
        desc.append("\n**2. Existing exchange (join as new node):**\n");
        desc.append("```java\n");
        desc.append("// Get UUID from existing exchange plan content\n");
        desc.append("UUID existingNodeUUID = UUID.fromString(\"550e8400-e29b-41d4-a716-446655440000\");\n");
        desc.append("exchangePlan.setThisNode(existingNodeUUID);\n");
        desc.append("// This node now joins existing exchange\n");
        desc.append("```\n");
        desc.append("\n**3. Exchange with multiple nodes:**\n");
        desc.append("```java\n");
        desc.append("// Node 1 (Main office)\n");
        desc.append("exchangePlan.setThisNode(UUID.fromString(\"550e8400-e29b-41d4-a716-446655440000\"));\n");
        desc.append("\n");
        desc.append("// Node 2 (Warehouse)\n");
        desc.append("exchangePlan.setThisNode(UUID.fromString(\"660e8400-e29b-41d4-a716-446655440001\"));\n");
        desc.append("// Each node has its own thisNode UUID\n");
        desc.append("```\n");
        desc.append("\n");
        desc.append("### Verification:\n");
        desc.append("```java\n");
        desc.append("UUID currentThisNode = exchangePlan.getThisNode();\n");
        desc.append("if (currentThisNode != null) {\n");
        desc.append("    System.out.println(\"ThisNode UUID: \" + currentThisNode);\n");
        desc.append("    System.out.println(\"This node is configured for exchange\");\n");
        desc.append("} else {\n");
        desc.append("    System.out.println(\"ThisNode is not set - exchange plan won't work\");\n");
        desc.append("}\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildEditExistingMetadataWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Edit Existing Metadata Object\n\n");
        desc.append("Generic workflow for editing any existing metadata object safely.\n\n");
        desc.append("### Core Rules:\n");
        desc.append("- ✅ **Load** existing object from BM transaction\n");
        desc.append("- ✅ **Modify** properties directly on the loaded object\n");
        desc.append("- ✅ **Return** the modified object\n");
        desc.append("- ❌ **NEVER use** `attachTopObject()` for existing objects\n");
        desc.append("- ❌ **NEVER use** `transaction.detachTopObject()` for existing objects\n");
        desc.append("\n");
        desc.append("### Generic Pattern:\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("Document result = globalContext.execute(new AbstractBmTask<Document>(\"Edit document\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        // STEP 1: Load existing object\n");
        desc.append("        Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("        \n");
        desc.append("        if (document != null) {\n");
        desc.append("            // STEP 2: Modify properties directly\n");
        desc.append("            document.setDescriptionLength(200);\n");
        desc.append("            document.setUseStandardCommands(true);\n");
        desc.append("            document.setRealTimePosting(RealTimePosting.DENY);\n");
        desc.append("            document.setNumberType(DocumentNumberType.NUMBER);\n");
        desc.append("            \n");
        desc.append("            // STEP 3: Add/Remove child objects through collections\n");
        desc.append("            // Add attribute\n");
        desc.append("            DocumentAttribute attr = mdFactory.createDocumentAttribute();\n");
        desc.append("            attr.setName(\"NewAttribute\");\n");
        desc.append("            attr.setUuid(UUID.randomUUID());\n");
        desc.append("            // ... set type ...\n");
        desc.append("            document.getAttributes().add(attr);\n");
        desc.append("            \n");
        desc.append("            // Remove attribute by finding and deleting\n");
        desc.append("            DocumentAttribute oldAttr = document.getAttributes().stream()\n");
        desc.append("                .filter(a -> \"OldAttribute\".equals(a.getName()))\n");
        desc.append("                .findFirst()\n");
        desc.append("                .orElse(null);\n");
        desc.append("            if (oldAttr != null) {\n");
        desc.append("                EcoreUtil.delete(oldAttr);\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            // STEP 4: Return modified object\n");
        desc.append("            return document;\n");
        desc.append("        }\n");
        desc.append("        \n");
        desc.append("        return null; // Object not found\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("\n");
        desc.append("### Edit Different Object Types:\n");
        desc.append("\n**Catalog:**\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("catalog.setCodeLength(10);\n");
        desc.append("catalog.setDescriptionLength(200);\n");
        desc.append("return catalog;\n");
        desc.append("```\n");
        desc.append("\n**Information Register:**\n");
        desc.append("```java\n");
        desc.append("InformationRegister register = (InformationRegister)transaction.getTopObjectByFqn(\"InformationRegister.Prices\");\n");
        desc.append("register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY);\n");
        desc.append("register.setWriteMode(RegisterWriteMode.INDEPENDENT);\n");
        desc.append("return register;\n");
        desc.append("```\n");
        desc.append("\n**Accumulation Register:**\n");
        desc.append("```java\n");
        desc.append("AccumulationRegister register = (AccumulationRegister)transaction.getTopObjectByFqn(\"AccumulationRegister.GoodsInStock\");\n");
        desc.append("register.setRegisterType(AccumulationRegisterType.BALANCE);\n");
        desc.append("return register;\n");
        desc.append("```\n");
        desc.append("\n**Enum:**\n");
        desc.append("```java\n");
        desc.append("Enum enumObj = (Enum)transaction.getTopObjectByFqn(\"Enum.Statuses\");\n");
        desc.append("enumObj.getEnumValues().get(0).setName(\"Active\");\n");
        desc.append("return enumObj;\n");
        desc.append("```\n");
        desc.append("\n");
        desc.append("### Common Pitfalls to Avoid:\n");
        desc.append("\n**❌ WRONG: Using attachTopObject for existing object**\n");
        desc.append("```java\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("document.setDescriptionLength(200);\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)document, fqn); // ❌ BmFqnAlreadyInUseException!\n");
        desc.append("```\n");
        desc.append("\n**❌ WRONG: Detaching existing object**\n");
        desc.append("```java\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("transaction.detachTopObject((IBmObject)document); // ❌ Detach should not be used for editing\n");
        desc.append("```\n");
        desc.append("\n**✅ CORRECT: Direct modification only**\n");
        desc.append("```java\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("if (document != null) {\n");
        desc.append("    document.setDescriptionLength(200); // ✅ Direct modification\n");
        desc.append("    // No attachTopObject() call\n");
        desc.append("    // No detachTopObject() call\n");
        desc.append("    // Just modify and return\n");
        desc.append("}\n");
        desc.append("```\n");
        desc.append("\n");
        desc.append("### Edit Document Registers:\n");
        desc.append("```java\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("AccumulationRegister register = (AccumulationRegister)transaction.getTopObjectByFqn(\"AccumulationRegister.GoodsInStock\");\n");
        desc.append("if (document != null && register != null) {\n");
        desc.append("    document.getRegisterRecords().add(register);\n");
        desc.append("    System.out.println(\"Added register to document\");\n");
        desc.append("}\n");
        desc.append("```\n");
        desc.append("\n");
        desc.append("### Edit Document Journal Documents:\n");
        desc.append("```java\n");
        desc.append("DocumentJournal journal = (DocumentJournal)transaction.getTopObjectByFqn(\"DocumentJournal.GoodsMovement\");\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("if (journal != null && document != null) {\n");
        desc.append("    journal.getRegisteredDocuments().add(document);\n");
        desc.append("    System.out.println(\"Added document to journal\");\n");
        desc.append("}\n");
        desc.append("```\n");
        desc.append("\n");
        desc.append("### Delete Child Objects:\n");
        desc.append("```java\n");
        desc.append("// Remove child object properly\n");
        desc.append("CatalogAttribute attrToRemove = catalog.getAttributes().stream()\n");
        desc.append("    .filter(a -> \"OldName\".equals(a.getName()))\n");
        desc.append("    .findFirst()\n");
        desc.append("    .orElse(null);\n");
        desc.append("if (attrToRemove != null) {\n");
        desc.append("    EcoreUtil.delete(attrToRemove); // ✅ Correct deletion method\n");
        desc.append("    System.out.println(\"Removed attribute: \" + attrToRemove.getName());\n");
        desc.append("}\n");
        desc.append("```\n");
        return desc.toString();
    }
}
