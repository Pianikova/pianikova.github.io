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
        if (mdClassFactory != null)
        {
            bindings.put("mdFactory", new JShellBindingDescription("Factory for creating 1C metadata objects",
                buildMdFactoryDescription(), mdClassFactory, MdClassFactory.class,
                "**⚠️ RESTRICTION: Cannot be used outside BM transaction.** Use `mdFactory` ONLY in "
                    + "AbstractBmTask.execute() body, where IBmTransaction is available. Do not use attachTopObject() for existing objects. "
                    + "**IMPORTANT**: Objects created with mdFactory MUST have UUIDs set via "
                    + "`modelFactory.fillDefaultReferences(object)` or manual assignment."));
        }

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
        desc.append("Use these rules in JShell to avoid frequent compile/runtime failures:\n\n");
        desc.append("- Use `globalContext.execute(new AbstractBmTask<...>(\"Task name\") { ... })`.\n");
        desc.append("- Do not use `executeReadonlyTask(...)` for metadata creation.\n");
        desc.append("- Use `v8project.getVersion()`, not `getRuntimeVersion()`.\n");
        desc.append("- Localized fields are `EMap<String, String>`: use `put(\"ru\", \"...\")`.\n");
        desc.append("- Use `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS` or `HierarchyType.HIERARCHY_OF_ITEMS`.\n");
        desc.append("- Catalog has `setDescriptionLength(...)`; `setNameLength(...)` is unavailable.\n");
        desc.append("- For catalog attributes use `CatalogAttribute` (`createCatalogAttribute` or `modelFactory` + EClass).\n");
        desc.append("- Do not override final methods `getId()` / `getServiceId()` in `AbstractBmTask`.\n");
        desc.append(
            "- Create top object, generate FQN, call `attachTopObject`, then add object to configuration collection.\n");
        desc.append(
            "- **WARNING**: `modelFactory` may have timeout issues in JShell due to OSGi service dependencies. Prefer `mdFactory` for simpler operations.\n");
        desc.append(
            "- For tabular section attributes, use `TabularSectionAttribute` with `mdFactory.createTabularSectionAttribute()`.\n");
        desc.append(
            "- Type qualifiers (String/Number) are abstract classes and cannot be instantiated directly in JShell. Use TypeDescriptionBuilder without qualifiers or use default types.\n");
        desc.append(
            "- **IMPORTANT**: When using `mdFactory` to create objects, UUIDs must be set. ");
        desc.append("Use `modelFactory.fillDefaultReferences(catalog)` or manually set UUIDs. ");
        desc.append("See UUID handling section below.\n");
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
        desc.append("        // Generate UUIDs for catalog and all child objects\n");
        desc.append("        modelFactory.fillDefaultReferences(catalog);\n");
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

        desc.append("### Option 1: Use modelFactory.fillDefaultReferences() (RECOMMENDED)\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("// ... set other properties ...\n\n");
        desc.append("// Generate UUIDs for catalog and all children\n");
        desc.append("modelFactory.fillDefaultReferences(catalog);\n\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("```\n\n");

        desc.append("### Option 2: Manual UUID assignment\n");
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
            "import com._1c.g5.v8.dt.metadata.mdclass.*;",
            "import com._1c.g5.v8.bm.core.*;",
            "import com._1c.g5.v8.bm.integration.*;",
            "import com._1c.g5.v8.dt.core.model.*;",
            "import com._1c.g5.v8.dt.core.naming.*;",
            "import com._1c.g5.v8.dt.core.platform.*;",
            "import com._1c.g5.v8.dt.platform.*;",
            "import com._1c.g5.v8.dt.mcore.*;",
            "import com._1c.g5.v8.dt.platform.core.typeinfo.*;",
            "import com._1c.g5.v8.dt.metadata.mdclass.*;",
            "import org.eclipse.core.resources.*;",
            "import org.eclipse.core.runtime.*;"
        );
        // @formatter:on
    }

    @SuppressWarnings("nls")
    private String buildMdFactoryDescription()
    {
        var desc = new StringBuilder();
        desc.append("## MdClassFactory\n\n");
        desc.append("Use `mdFactory` for direct creation of metadata objects and object parts.\n\n");
        desc.append("**⚠️ IMPORTANT:** `mdFactory` MUST be used ONLY inside BM transaction (`AbstractBmTask.execute()`). "
            + "Do not use for editing existing objects - use `getTopObjectByFqn()` instead.\n\n");
        desc.append("```java\n");
        desc.append("// CORRECT: Creating a new catalog\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\n");
        desc.append("\n");
        desc.append("CatalogAttribute attribute = mdFactory.createCatalogAttribute();\n");
        desc.append("attribute.setName(\"Article\");\n");
        desc.append("attribute.getSynonym().put(\"ru\", \"Article\");\n");
        desc.append("catalog.getAttributes().add(attribute);\n");
        desc.append("\n");
        desc.append("// Generate FQN and attach\n");
        desc.append("String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("```\n");
        desc.append("\n");
        desc.append("**❌ WRONG: Using mdFactory to edit existing object**\n");
        desc.append("```java\n");
        desc.append("// Don't do this!\n");
        desc.append("Catalog catalog = mdFactory.createCatalog(); // Creates NEW object\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, \"Catalog.Products\"); // ❌ FQN already exists!\n");
        desc.append("```\n\n");
        desc.append("**✅ CORRECT: Edit existing object**\n");
        desc.append("```java\n");
        desc.append("// Get existing object\n");
        desc.append("Catalog catalog = (Catalog)transaction.getTopObjectByFqn(\"Catalog.Products\");\n");
        desc.append("catalog.setDescriptionLength(200); // Modify directly\n");
        desc.append("```\n\n");
        desc.append("### Available Public Methods:\n\n");

        var methodSignatures = methodListProvider.getPublicMethodSignatures(MdClassFactory.class);
        for (String signature : methodSignatures)
        {
            desc.append("- `").append(signature).append("`\n");
        }

        desc.append("\n");
        desc.append("For top-level objects in project context, prefer `modelFactory` inside BM transaction.");
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
        desc.append("\n");
        desc.append("            // Create tabular section attributes\n");
        desc.append("            TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();\n");
        desc.append("            product.setName(\"Product\");\n");
        desc.append("            product.getSynonym().put(\"ru\", \"Product\");\n");
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
        desc.append("### ❌ WRONG: attachTopObject on existing object\n");
        desc.append("```java\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("// ... modifications ...\n");
        desc.append("transaction.attachTopObject((IBmObject)document, fqn); // ❌ BmFqnAlreadyInUseException!\n");
        desc.append("```\n\n");
        desc.append("### ✅ CORRECT: Modify existing object directly\n");
        desc.append("```java\n");
        desc.append("Document document = (Document)transaction.getTopObjectByFqn(\"Document.GoodsReceipt\");\n");
        desc.append("document.setDescriptionLength(200); // ✅ OK\n");
        desc.append("// No attachTopObject() call\n");
        desc.append("```\n\n");
        desc.append("### ❌ WRONG: Trying to create with existing FQN\n");
        desc.append("```java\n");
        desc.append("String fqn = \"Catalog.Products\"; // Already exists!\n");
        desc.append("Catalog newCatalog = mdFactory.createCatalog();\n");
        desc.append("transaction.attachTopObject((IBmObject)newCatalog, fqn); // ❌ Exception!\n");
        desc.append("```\n\n");
        desc.append("### ✅ CORRECT: Check before creating\n");
        desc.append("```java\n");
        desc.append("String fqn = \"Catalog.NewProducts\";\n");
        desc.append("if (transaction.getTopObjectByFqn(fqn) == null) {\n");
        desc.append("    Catalog newCatalog = mdFactory.createCatalog();\n");
        desc.append("    transaction.attachTopObject((IBmObject)newCatalog, fqn); // ✅ OK\n");
        desc.append("}\n");
        desc.append("```\\n\\n");
        desc.append("### ⚠️ IMPORTANT: Validate 1C Errors After Entity Operations\\n\\n");
        desc.append("After creating, editing, or deleting metadata entities, you **MUST** use ");
        desc.append("`GetMarkersMcpTool.TOOL_NAME` (GetMarkers tool) to check for 1C validation errors.\\n\\n");
        desc.append("```java\\n");
        desc.append("// After creating/editing/deleting metadata objects, always check for errors:\\n");
        desc.append("GetMarkersMcpTool.TOOL_NAME // Tool name: \\\"GetMarkers\\\"\\n");
        desc.append("\\n");
        desc.append("// Example usage:\\n");
        desc.append(
            "var markers = toolCall(\\\"GetMarkers\\\", Map.of(\\\"project_name\\\", projectName, \\\"marker_type\\\", \\\"1c\\\"));\\n");
        desc.append("// Check for critical validation errors and handle them appropriately\\n");
        desc.append("```\\n\\n");
        desc.append("**This validation step is mandatory** after any metadata modification operation to ensure:\\n");
        desc.append("- Metadata structure integrity\\n");
        desc.append("- No SU45 (UUID required) errors\\n");
        desc.append("- No reference resolution errors\\n");
        desc.append("- Proper configuration consistency\\n\\n");
        desc.append("### Summary of Best Practices\\n\\n");
        desc.append("1. **Create once, attach once:** Use `attachTopObject()` only when creating a NEW object\n");
        desc.append("2. **Get, don't attach:** Use `getTopObjectByFqn()` to edit existing objects\n");
        desc.append("3. **Check before create:** Verify FQN doesn't exist before attaching\n");
        desc.append("4. **Use proper renaming:** Use `updateTopObjectFqn()` for FQN changes\n");
        desc.append("5. **Complete structure:** Create entire object structure (including tabular sections) in one transaction\n");
        return desc.toString();
    }
}
