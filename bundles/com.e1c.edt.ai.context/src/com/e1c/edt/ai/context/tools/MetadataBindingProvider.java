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
import com.e1c.edt.ai.tools.JShellBindingDescription;
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
 *   <li>Transaction-based modifications</li>
 * </ul>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 *   <li>Always use BM transactions ({@link IBmGlobalEditingContext#execute()}) for modifications</li>
 *   <li>For deletion: remove from parent collection and detach from transaction - NEVER use collection {@code remove()} alone or {@code EcoreUtil.delete()} for top-level objects</li>
 *   <li>New objects MUST have UUIDs set via {@code object.setUuid(UUID.randomUUID())}</li>
 *   <li>{@code mdFactory} can only be used inside BM transaction</li>
 * </ul>
 */
@Singleton
public class MetadataBindingProvider
    implements IJShellBindingProvider
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
    public String getDescription()
    {
        return "1C metadata API: Create, edit, and delete Catalog, Document, Register, and other metadata objects. "
            + "Includes factories (mdFactory, modelFactory), FQN generator, BM model with transactions, "
            + "and resource lookup for Eclipse integration.";
    }

    @Override
    @SuppressWarnings("nls")
    public String getUseCases()
    {
        var desc = new StringBuilder();
        desc.append(buildApiCompatibilityNotes());
        desc.append("\n\n");
        desc.append(buildUuidHandlingWorkflow());
        desc.append("\n\n");
        desc.append(buildTransactionManagementScenarios());
        desc.append("\n\n");
        desc.append(buildSafeCatalogWorkflow());
        desc.append("\n\n");
        desc.append(buildCommonModuleWorkflow());
        desc.append("\n\n");
        desc.append(buildDocumentWorkflow());
        desc.append("\n\n");
        desc.append(buildEditExistingObjectWorkflow());
        desc.append("\n\n");
        desc.append(buildTabularSectionWorkflow());
        desc.append("\n\n");
        desc.append(buildRenameObjectWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteAttributeWorkflow());
        desc.append("\n\n");
        desc.append(buildCommonPitfalls());
        desc.append("\n\n");
        desc.append(buildAccumulationRegisterWorkflow());
        desc.append("\n\n");
        desc.append(buildAccountingRegisterWorkflow());
        desc.append("\n\n");
        desc.append(buildCalculationRegisterWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteCatalogWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteDocumentWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteAccumulationRegisterWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteAccountingRegisterWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteCalculationRegisterWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteInformationRegisterWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteEnumWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteReportWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteDataProcessorWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteCommonModuleWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteConstantWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteRegisterObjectsWorkflow());
        desc.append("\n\n");
        desc.append(buildCreateConfigurationWorkflow());
        desc.append("\n\n");
        desc.append(buildDeleteConfigurationWorkflow());
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
        desc.append("        register.setRegisterType(AccumulationRegisterType.Balance);\n");
        desc.append("\n");
        desc.append("        // Add dimension\n");
        desc.append(
            "        AccumulationRegisterDimension warehouse = mdFactory.createAccumulationRegisterDimension();\n");
        desc.append("        warehouse.setName(\"Warehouse\");\n");
        desc.append("        warehouse.getSynonym().put(\"ru\", \"Warehouse\");\n");
        desc.append("        warehouse.setBalance(true);\n");
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
        desc.append("        // Set type for resource (Number or other)\n");
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
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n        "));
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
            "**Note:** AccountingRegister requires ChartOfAccounts reference and at least one Dimension with Account type.\n");
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
        desc.append("        register.setPeriodicity(CalculationRegisterPeriodicity.Month);\n");
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

        desc.append("#### Metadata Object-Specific Rules\n\n");
        desc.append("**Catalog (Справочник):**\n");
        desc.append("- Use `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS` or `HierarchyType.HIERARCHY_OF_ITEMS`\n");
        desc.append("- Use `setDescriptionLength(...)` - `setNameLength(...)` is NOT AVAILABLE\n");
        desc.append("- For attributes: use `CatalogAttribute` via `createCatalogAttribute()` or `modelFactory` + EClass\n");
        desc.append("- Supports: hierarchical, codeType (Number/String), checkUnique, autonumbering\n\n");

        desc.append("**Document (Документ):**\n");
        desc.append("- Use `DocumentNumberType.Number` or `DocumentNumberType.String`\n");
        desc.append("- Use `DocumentNumberPeriodicity` (Nonperiodical, Year, Quarter, Month, Day)\n");
        desc.append("- Supports: posting, realTimePosting, registerRecordsDeletion, sequenceFilling\n");
        desc.append("- May reference: numerator, registerRecords (array of BasicRegister)\n\n");

        desc.append("**InformationRegister (РегистрСведений):**\n");
        desc.append("- Use `InformationRegisterPeriodicity` (Nonperiodical, Second, Day, Month, Quarter, Year)\n");
        desc.append("- Use `RegisterWriteMode` (Independent, RecorderSubordinate)\n");
        desc.append("- Contains: resources, attributes, dimensions (all require types)\n\n");

        desc.append("**Enum (Перечисление):**\n");
        desc.append("- Contains `EnumValue[] enumValues` - create with `createEnumValue()`\n");
        desc.append("- Each EnumValue has: name, description, color (since 8.5.1)\n");
        desc.append("- NO attributes or tabular sections\n\n");

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
        desc.append("        return catalog;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("If attribute value types are required, create `TypeDescription` via EDT mcore type utilities for current project version.");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildCommonModuleWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Create CommonModule\n\n");
        desc.append("```java\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("IV8Project v8project = projectManager.getProject(project);\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("CommonModule result = globalContext.execute(new AbstractBmTask<CommonModule>(\"Create common module\") {\n");
        desc.append("    @Override\n");
        desc.append("    public CommonModule execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append(
            "        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("\n");
        desc.append("        CommonModule module = mdFactory.createCommonModule();\n");
        desc.append("        module.setName(\"MyCommonModule\");\n");
        desc.append("        module.getSynonym().put(\"ru\", \"Мой общий модуль\");\n");
        desc.append("\n");
        desc.append("        // Set execution properties\n");
        desc.append("        module.setServer(true);        // Server execution\n");
        desc.append("        module.setClientManagedApplication(false);        // Client execution (managed application)\n");
        desc.append("        module.setClientOrdinaryApplication(false);\n");
        desc.append("        module.setServerCall(false);    // Calls from client to server\n");
        desc.append("        module.setExternalConnection(false);\n");
        desc.append("        module.setPrivileged(false);    // Privileged mode\n");
        desc.append("        module.setGlobal(false);       // Global context\n");
        desc.append("\n");
        desc.append("        // Set UUID manually (RECOMMENDED for JShell)\n");
        desc.append("        module.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(module.eClass(), module.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)module, fqn);\n");
        desc.append("        configuration.getCommonModules().add(module);\n");
        desc.append("        return module;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**Note:** After creating the common module metadata, create the corresponding Module.bsl file\n");
        desc.append("in the project: `src/CommonModules/<ModuleName>/Module.bsl`\n\n");
        desc.append("**Deletion:** Common modules can be deleted by removing from configuration.getCommonModules()\n");
        desc.append("and detaching from transaction. See buildDeleteCommonModuleWorkflow() for details.\n\n");
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
    private String buildUuidHandlingWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## UUID Handling for Metadata Objects\n\n");
        desc.append("**IMPORTANT:** All metadata objects (catalogs, documents, attributes, forms, etc.) ");
        desc.append("must have a unique UUID. Failure to set UUIDs causes validation errors (SU45).\n\n");

        desc.append("### Option 1: Manual UUID assignment (RECOMMENDED for JShell)\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("catalog.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("// ... set other properties ...\n\n");
        desc.append("// For child objects, set UUIDs manually\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setName(\"Article\");\n");
        desc.append("attr.setUuid(UUID.randomUUID());\n");
        desc.append("catalog.getAttributes().add(attr);\n\n");
        desc.append(
            "String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("```\n\n");

        desc.append("### Option 2: Use modelFactory.fillDefaultReferences()\n");
        desc.append("⚠️ **WARNING:** This method may timeout in JShell due to OSGi service limitations.\n");
        desc.append("Use manual UUID assignment (Option 1) for reliable JShell execution.\n\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("// ...\n\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setName(\"Article\");\n");
        desc.append("attr.setUuid(UUID.randomUUID());\n");
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
            "import com._1c.g5.v8.dt.mcore.*;",
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
            "3. **Objects MUST have UUIDs set** - use manual assignment `object.setUuid(UUID.randomUUID())` for JShell (RECOMMENDED) or `modelFactory.fillDefaultReferences(object)`\n");
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
        desc.append("| RegisterDimension | `createRegisterDimension()` | Register |\n");
        desc.append("| RegisterResource | `createRegisterResource()` | Register |\n");
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
        desc.append("// Set attribute type using TypeDescriptionBuilder...\n");
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
        desc.append("**Note:** For top-level objects in project context, `modelFactory` is preferred but may have OSGi timeout issues.\n");
        desc.append("`mdFactory` is recommended for most operations due to better reliability in JShell context.");
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
        desc.append("Preferred way to create objects in project/version context.\n\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = (Catalog)modelFactory.create(MdClassPackage.Literals.CATALOG, v8project);\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("\n");
        desc.append("CatalogAttribute attribute = (CatalogAttribute)modelFactory.create(\n");
        desc.append("    MdClassPackage.Literals.CATALOG_ATTRIBUTE, catalog, v8project.getVersion());\n");
        desc.append("attribute.setName(\"Article\");\n");
        desc.append("catalog.getAttributes().add(attribute);\n");
        desc.append("\n");
        desc.append("modelFactory.fillDefaultReferences(catalog);\n");
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
        desc.append("        Document document = mdFactory.createDocument();\n");
        desc.append("        document.setName(\"GoodsReceipt\");\n");
        desc.append("        document.getSynonym().put(\"ru\", \"Goods Receipt\");\n");
        desc.append("        document.setNumberLength(9);\n");
        desc.append("\n");
        desc.append("        DocumentAttribute warehouse = mdFactory.createDocumentAttribute();\n");
        desc.append("        warehouse.setName(\"Warehouse\");\n");
        desc.append("        warehouse.getSynonym().put(\"ru\", \"Warehouse\");\n");
        desc.append("\n");
        desc.append("        ").append(buildStringTypeDescription().replace("\n", "\n        "));
        desc.append("\n");
        desc.append("        warehouse.setType(typeDesc);\n");
        desc.append("        document.getAttributes().add(warehouse);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)\n");
        desc.append("        document.setUuid(UUID.randomUUID());\n");
        desc.append("        warehouse.setUuid(UUID.randomUUID());\n");
        desc.append("\n");
        desc.append("        String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();\n");
        desc.append("        transaction.attachTopObject((IBmObject)document, fqn);\n");
        desc.append("        configuration.getDocuments().add(document);\n");
        desc.append("        return document;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
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
        desc.append("catalog.getAttributes().add(attr);\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("// Error: SU45 - UUID required for catalog and attributes!\n");
        desc.append("```\n\n");
        desc.append("```java\n");
        desc.append("// ✅ CORRECT CODE - Option 1: Manual UUID assignment (RECOMMENDED for JShell)\n");
        desc.append("import java.util.UUID;\n");
        desc.append("Catalog catalog = mdFactory.cr eateCatalog();\n");
        desc.append("catalog.setUuid(UUID.randomUUID());\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("CatalogAttribute attr = mdFactory.createCatalogAttribute();\n");
        desc.append("attr.setUuid(UUID.randomUUID()); // Must set UUID for children too!\n");
        desc.append("attr.setName(\"Article\");\n");
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
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a catalog will cascade delete all its attributes, tabular sections, forms, and templates.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteCatalogWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete Catalog\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete catalog\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("        \n");
        desc.append("        if (catalog != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("            configuration.getCatalogs().remove(catalog);\n");
        desc.append("            transaction.detachTopObject((IBmObject)catalog);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a catalog will cascade delete all its attributes, tabular sections, forms, and templates.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteDocumentWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete Document\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete document\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("        \n");
        desc.append("        if (document != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("            configuration.getDocuments().remove(document);\n");
        desc.append("            transaction.detachTopObject((IBmObject)document);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a document will also delete all its attributes, tabular sections, forms, templates, and related register records.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteAccumulationRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete AccumulationRegister\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete accumulation register\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        AccumulationRegister register = (AccumulationRegister)transaction.getTopObjectByFqn(\"AccumulationRegister.GoodsInStock\");\n");
        desc.append("        \n");
        desc.append("        if (register != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("            configuration.getAccumulationRegisters().remove(register);\n");
        desc.append("            transaction.detachTopObject((IBmObject)register);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting an accumulation register will also delete all its dimensions, resources, forms, and templates.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteAccountingRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete AccountingRegister\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete accounting register\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        AccountingRegister register = (AccountingRegister)transaction.getTopObjectByFqn(\"AccountingRegister.Accounting\");\n");
        desc.append("        \n");
        desc.append("        if (register != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("            configuration.getAccountingRegisters().remove(register);\n");
        desc.append("            transaction.detachTopObject((IBmObject)register);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting an accounting register will also delete all its dimensions, resources, forms, and templates.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteCalculationRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete CalculationRegister\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete calculation register\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        CalculationRegister register = (CalculationRegister)transaction.getTopObjectByFqn(\"CalculationRegister.SalaryCalculation\");\n");
        desc.append("        \n");
        desc.append("        if (register != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("            configuration.getCalculationRegisters().remove(register);\n");
        desc.append("            transaction.detachTopObject((IBmObject)register);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a calculation register will also delete all its dimensions, resources, recalculations, forms, and templates.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteInformationRegisterWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete InformationRegister\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete information register\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        InformationRegister register = (InformationRegister)transaction.getTopObjectByFqn(\"InformationRegister.ExchangeRates\");\n");
        desc.append("        \n");
        desc.append("        if (register != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("            configuration.getInformationRegisters().remove(register);\n");
        desc.append("            transaction.detachTopObject((IBmObject)register);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting an information register will also delete all its dimensions, resources, attributes, forms, and templates.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteEnumWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete Enum\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\"Delete enum\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Enum enumObj = (Enum)transaction.getTopObjectByFqn(\"Enum.Status\");\n");
        desc.append("        \n");
        desc.append("        if (enumObj != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
        desc.append("            configuration.getEnums().remove(enumObj);\n");
        desc.append("            transaction.detachTopObject((IBmObject)enumObj);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting an enum will also delete all its enum values, forms, and templates.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteReportWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete Report\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\\\"Delete report\\\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Report report = (Report)transaction.getTopObjectByFqn(\\\"Report.SalesReport\\\"\");\n");
        desc.append("        \n");
        desc.append("        if (report != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\\\"Configuration\\\"\");\n");
        desc.append("            configuration.getReports().remove(report);\n");
        desc.append("            transaction.detachTopObject((IBmObject)report);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a report will also delete all its forms, templates, commands, and attributes.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteDataProcessorWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete DataProcessor\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\\\"Delete data processor\\\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        DataProcessor dataProcessor = (DataProcessor)transaction.getTopObjectByFqn(\\\"DataProcessor.ImportData\\\"\");\n");
        desc.append("        \n");
        desc.append("        if (dataProcessor != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\\\"Configuration\\\"\");\n");
        desc.append("            configuration.getDataProcessors().remove(dataProcessor);\n");
        desc.append("            transaction.detachTopObject((IBmObject)dataProcessor);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a data processor will also delete all its forms, templates, commands, attributes, and tabular sections.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteCommonModuleWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete CommonModule\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\\\"Delete common module\\\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        CommonModule commonModule = (CommonModule)transaction.getTopObjectByFqn(\\\"CommonModule.WorkingWithData\\\"\");\n");
        desc.append("        \n");
        desc.append("        if (commonModule != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\\\"Configuration\\\"\");\n");
        desc.append("            configuration.getCommonModules().remove(commonModule);\n");
        desc.append("            transaction.detachTopObject((IBmObject)commonModule);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a common module will also delete all its methods and properties. Check that no other objects reference this module.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteConstantWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete Constant\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\\\"Delete constant\\\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        Constant constant = (Constant)transaction.getTopObjectByFqn(\\\"Constant.DefaultWarehouse\\\"\");\n");
        desc.append("        \n");
        desc.append("        if (constant != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\\\"Configuration\\\"\");\n");
        desc.append("            configuration.getConstants().remove(constant);\n");
        desc.append("            transaction.detachTopObject((IBmObject)constant);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:** Remove from parent collection and detach from transaction.\n");
        desc.append("**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.\n");
        desc.append("**Note:** Deleting a constant will also delete its value manager if defined. Check that no other objects reference this constant.\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildDeleteRegisterObjectsWorkflow()
    {
        var desc = new StringBuilder();
        desc.append("## Safe Workflow: Delete Any Metadata Object\n\n");
        desc.append("This workflow demonstrates how to delete any metadata object by its FQN.\n\n");
        desc.append("```java\n");
        desc.append("IBmModel bmModel = modelManager.getModel(project);\n");
        desc.append("IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();\n");
        desc.append("\n");
        desc.append("globalContext.execute(new AbstractBmTask<Void>(\\\"Delete metadata object\\\") {\n");
        desc.append("    @Override\n");
        desc.append("    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {\n");
        desc.append("        \n");
        desc.append("        // Example: Delete ChartOfCharacteristicTypes\n");
        desc.append("        String objectFqn = \\\"ChartOfCharacteristicTypes.Properties\\\";\n");
        desc.append("        MdObject objectToDelete = (MdObject)transaction.getTopObjectByFqn(objectFqn);\n");
        desc.append("        \n");
        desc.append("        if (objectToDelete != null) {\n");
        desc.append("            Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\\\"Configuration\\\"\");\n");
        desc.append("            \n");
        desc.append("            // Remove from appropriate collection based on object type\n");
        desc.append("            if (objectToDelete instanceof Catalog) {\n");
        desc.append("                configuration.getCatalogs().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof Document) {\n");
        desc.append("                configuration.getDocuments().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof AccumulationRegister) {\n");
        desc.append("                configuration.getAccumulationRegisters().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof AccountingRegister) {\n");
        desc.append("                configuration.getAccountingRegisters().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof CalculationRegister) {\n");
        desc.append("                configuration.getCalculationRegisters().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof InformationRegister) {\n");
        desc.append("                configuration.getInformationRegisters().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof Enum) {\n");
        desc.append("                configuration.getEnums().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof Report) {\n");
        desc.append("                configuration.getReports().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof DataProcessor) {\n");
        desc.append("                configuration.getDataProcessors().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof CommonModule) {\n");
        desc.append("                configuration.getCommonModules().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof Constant) {\n");
        desc.append("                configuration.getConstants().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof ChartOfCharacteristicTypes) {\n");
        desc.append("                configuration.getChartsOfCharacteristicTypes().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof ChartOfAccounts) {\n");
        desc.append("                configuration.getChartsOfAccounts().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof ChartOfCalculationTypes) {\n");
        desc.append("                configuration.getChartsOfCalculationTypes().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof ExchangePlan) {\n");
        desc.append("                configuration.getExchangePlans().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof BusinessProcess) {\n");
        desc.append("                configuration.getBusinessProcesses().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof Task) {\n");
        desc.append("                configuration.getTasks().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof DocumentJournal) {\n");
        desc.append("                configuration.getDocumentJournals().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof DocumentNumerator) {\n");
        desc.append("                configuration.getDocumentNumerators().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof Sequence) {\n");
        desc.append("                configuration.getSequences().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof DefinedType) {\n");
        desc.append("                configuration.getDefinedTypes().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof SettingsStorage) {\n");
        desc.append("                configuration.getSettingsStorages().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof FilterCriterion) {\n");
        desc.append("                configuration.getFilterCriteria().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof EventSubscription) {\n");
        desc.append("                configuration.getEventSubscriptions().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof ScheduledJob) {\n");
        desc.append("                configuration.getScheduledJobs().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof FunctionalOption) {\n");
        desc.append("                configuration.getFunctionalOptions().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof CommonAttribute) {\n");
        desc.append("                configuration.getCommonAttributes().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof CommonForm) {\n");
        desc.append("                configuration.getCommonForms().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof CommonTemplate) {\n");
        desc.append("                configuration.getCommonTemplates().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof CommonCommand) {\n");
        desc.append("                configuration.getCommonCommands().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof WebService) {\n");
        desc.append("                configuration.getWebServices().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof HTTPService) {\n");
        desc.append("                configuration.getHTTPServices().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof IntegrationService) {\n");
        desc.append("                configuration.getIntegrationServices().remove(objectToDelete);\n");
        desc.append("            } else if (objectToDelete instanceof WSReference) {\n");
        desc.append("                configuration.getWSReferences().remove(objectToDelete);\n");
        desc.append("            }\n");
        desc.append("            \n");
        desc.append("            transaction.detachTopObject((IBmObject)objectToDelete);\n");
        desc.append("        }\n");
        desc.append("        return null;\n");
        desc.append("    }\n");
        desc.append("});\n");
        desc.append("```\n");
        desc.append("**IMPORTANT:**\n");
        desc.append("1. Remove object from the appropriate parent collection based on its type\n");
        desc.append("2. Then detach from transaction: `transaction.detachTopObject((IBmObject)object)`\n");
        desc.append("3. Do NOT use `EcoreUtil.delete()` for top-level objects - causes `UnsupportedOperationException`\n");
        desc.append("4. Deleting an object will cascade delete all its child objects (attributes, forms, templates, etc.)\n");
        desc.append("5. Check that no other objects reference the object being deleted\n");
        desc.append("6. Always validate after deletion to check for reference errors\n");
        desc.append("**Supported object types for deletion:** Catalog, Document, AccumulationRegister, AccountingRegister, CalculationRegister, InformationRegister, ChartOfCharacteristicTypes, ChartOfAccounts, ChartOfCalculationTypes, Enum, Report, DataProcessor, ExternalReport, ExternalDataProcessor, CommonModule, Constant, ExchangePlan, BusinessProcess, Task, Sequence, DocumentJournal, DocumentNumerator, DefinedType, SettingsStorage, FilterCriterion, EventSubscription, ScheduledJob, FunctionalOption, CommonAttribute, CommonForm, CommonTemplate, CommonCommand, WebService, HTTPService, IntegrationService, WSReference, and others.\n");
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
        desc.append("    return;\n");
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
        desc.append("        return;\n");
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
        desc.append("        return;\n");
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
        desc.append("and detaching from transaction. See buildDeleteCommonModuleWorkflow() for details.\n");
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
}
