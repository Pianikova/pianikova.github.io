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
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
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
 * Provides JShell bindings for 1C metadata creation and editing operations.
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
        return "1C metadata API (factories, project manager, BM model)";
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
        desc.append(buildDocumentWorkflow());
        desc.append("\n\n");
        desc.append(buildEditExistingObjectWorkflow());
        desc.append("\n\n");
        desc.append(buildTabularSectionWorkflow());
        desc.append("\n\n");
        desc.append(buildRenameObjectWorkflow());
        desc.append("\n\n");
        desc.append(buildCommonPitfalls());
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
        desc.append("import java.util.UUID;\n");
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
        desc.append("        Configuration configuration = (Configuration)transaction.getTopObjectByFqn(\"Configuration\");\n");
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
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription articleType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(stringType)\n");
        desc.append("            .build();\n");
        desc.append("        // Note: String/Number qualifiers can be set via TypeDescriptionBuilder if needed\n");
        desc.append("        article.setType(articleType);\n");
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
        desc.append("```\n\n");
        desc.append("If attribute value types are required, create `TypeDescription` via EDT mcore type utilities for current project version.");
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
        desc.append("import java.util.UUID;\n\n");
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
        desc.append("import java.util.UUID;\n\n");
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
            IWorkspaceRoot.class
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
            "import com._1c.g5.v8.dt.platform.core.typeinfo.*;"
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
        desc.append("| RegisterAttribute | `createRegisterAttribute()` | Register |\n");
        desc.append("| RegisterDimension | `createRegisterDimension()` | Register |\n");
        desc.append("| RegisterResource | `createRegisterResource()` | Register |\n");
        desc.append("| TabularSectionAttribute | `createTabularSectionAttribute()` | TabularSection |\n");
        desc.append("| CatalogTabularSection | `createCatalogTabularSection()` | Catalog |\n");
        desc.append("| DocumentTabularSection | `createDocumentTabularSection()` | Document |\n");
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
        desc.append("| Table | `createTable()` | Cube |\n\n");

        desc.append("### Correct Usage Example:\n\n");
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
        desc.append("import java.util.UUID;\n");
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
        desc.append("        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);\n");
        desc.append("        TypeDescription warehouseType = new TypeDescriptionBuilder()\n");
        desc.append("            .addType(stringType)\n");
        desc.append("            .build();\n");
        desc.append("        warehouse.setType(warehouseType);\n");
        desc.append("        document.getAttributes().add(warehouse);\n");
        desc.append("\n");
        desc.append("        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)\n");
        desc.append("        import java.util.UUID;\n");
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
        desc.append("            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE\n");
        desc.append("                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());\n");
        desc.append("\n");
        desc.append("            TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);\n");
        desc.append("            TypeDescription productType = new TypeDescriptionBuilder()\n");
        desc.append("                .addType(catalogRefType)\n");
        desc.append("                .build();\n");
        desc.append("            product.setType(productType);\n");
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
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
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

        desc.append("### ⚠️ CRITICAL: Validate After Entity Operations\n\n");
        desc.append("After creating, editing, or deleting metadata entities, you **MUST** check for validation errors.\n\n");
        desc.append("```java\n");
        desc.append("// After ANY metadata modification, always validate:\n");
        desc.append("// 1. Create/edit/delete metadata objects\n");
        desc.append("// 2. Use GetMarkers tool to check for errors\n");
        desc.append("// 3. Handle critical validation errors\n\n");
        desc.append("// Example: Check for 1C validation errors\n");
        desc.append("var markers = toolCall(\"GetMarkers\", Map.of(\n");
        desc.append("    \"project_name\", projectName,\n");
        desc.append("    \"marker_type\", \"1c\"\n");
        desc.append("));\n");
        desc.append("// Check for critical errors (SU45, reference errors, etc.)\n");
        desc.append("```\n\n");
        desc.append("**This validation step is MANDATORY** to ensure:\n");
        desc.append("- ✅ Metadata structure integrity\n");
        desc.append("- ✅ No SU45 (UUID required) errors\n");
        desc.append("- ✅ No reference resolution errors\n");
        desc.append("- ✅ Proper configuration consistency\n");
        desc.append("- ✅ No duplicate FQN errors\n\n");

        desc.append("### ✅ Best Practices Checklist\n\n");
        desc.append("When creating metadata objects:\\n\\n");
        desc.append("1. **✅ Use transaction:** Always wrap operations in `globalContext.execute(new AbstractBmTask<...>())`\\n");
        desc.append("2. **✅ Check existence:** Verify `getTopObjectByFqn(fqn) == null` before creating\\n");
        desc.append("3. **✅ Set UUIDs:** Call `object.setUuid(UUID.randomUUID())` for all objects\\n");
        desc.append("4. **✅ Generate FQN:** Use `fqnGenerator.generateStandaloneObjectFqn(eClass(), name)`\\n");
        desc.append("5. **✅ Attach once:** Call `transaction.attachTopObject()` ONLY for new objects\\n");
        desc.append("6. **✅ Add to collection:** Add object to parent: `configuration.getXxxs().add(object)`\\n");
        desc.append("7. **✅ Validate:** Always check markers after operations\\n\\n");
        desc.append("When editing existing objects:\n\n");
        desc.append("1. **✅ Use getTopObjectByFqn:** Retrieve existing object with FQN\n");
        desc.append("2. **✅ Modify directly:** Change properties without `attachTopObject()`\n");
        desc.append("3. **✅ No attachTopObject:** NEVER call `attachTopObject()` on existing objects\n");
        desc.append("4. **✅ Validate:** Always check markers after operations\n\n");
        desc.append("When renaming objects:\n\n");
        desc.append("1. **✅ Use updateTopObjectFqn:** Call `transaction.updateTopObjectFqn(object, newFqn)`\n");
        desc.append("2. **✅ Update name:** Also change the object name property\n");
        desc.append("3. **✅ No attachTopObject:** NEVER use `attachTopObject()` for renaming\n");
        desc.append("4. **✅ Validate:** Always check markers after operations\n\n");

        return desc.toString();
    }
}
