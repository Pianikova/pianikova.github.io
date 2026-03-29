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
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.Report;
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
                "**⚠️ RESTRICTION: Cannot be used inside BM transaction.** Use `mdFactory` for object creation in "
                    + "AbstractBmTask.execute() body, where IBmTransaction is available."));
        }

        bindings.put("fqnGenerator", new JShellBindingDescription(
            "Generates FQNs (Fully Qualified Names) for top-level metadata objects",
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
            "Provides BM model and editing contexts",
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
        return buildApiCompatibilityNotes() + "\n\n" + buildSafeCatalogWorkflow();
    }

    @SuppressWarnings("nls")
    private String buildApiCompatibilityNotes()
    {
        var desc = new StringBuilder();
        desc.append("## API Compatibility Notes (EDT 8.3.24)\n\n");
        desc.append("Use these rules in JShell to avoid frequent compile/runtime failures:\n\n");
        desc.append("- Use `globalContext.execute(new AbstractBmTask<...>(\"Task name\") { ... })`.\n");
        desc.append("- Do not use `executeReadonlyTask(...)` for metadata creation.\n");
        desc.append("- Use `v8project.getVersion()`, not `getRuntimeVersion()`.\n");
        desc.append("- Localized fields are `EMap<String, String>`: use `put(\"ru\", \"...\")`.\n");
        desc.append("- Use `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS` or `HierarchyType.HIERARCHY_OF_ITEMS`.\n");
        desc.append("- Catalog has `setDescriptionLength(...)`; `setNameLength(...)` is unavailable.\n");
        desc.append("- For catalog attributes use `CatalogAttribute` (`createCatalogAttribute` or `modelFactory` + EClass).\n");
        desc.append("- Do not override final methods `getId()` / `getServiceId()` in `AbstractBmTask`.\n");
        desc.append("- Create top object, generate FQN, call `attachTopObject`, then add object to configuration collection.\n");
        desc.append("- If `IModelObjectFactory` is unavailable at runtime, use `mdFactory` for object creation.\n");
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
        desc.append("            .setStringQualifiers(50, false)\n");
        desc.append("            .build();\n");
        desc.append("        article.setType(articleType);\n");
        desc.append("        catalog.getAttributes().add(article);\n");
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
            Report.class,
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
        desc.append("**⚠️ IMPORTANT:** `mdFactory` cannot be used inside BM transaction. Use it only in "
            + "`AbstractBmTask.execute()` body where `IBmTransaction` is available.\n\n");
        desc.append("```java\n");
        desc.append("Catalog catalog = mdFactory.createCatalog();\n");
        desc.append("catalog.setName(\"Products\");\n");
        desc.append("catalog.getSynonym().put(\"ru\", \"Products\");\n");
        desc.append("catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);\n");
        desc.append("\n");
        desc.append("CatalogAttribute attribute = mdFactory.createCatalogAttribute();\n");
        desc.append("attribute.setName(\"Article\");\n");
        desc.append("attribute.getSynonym().put(\"ru\", \"Article\");\n");
        desc.append("catalog.getAttributes().add(attribute);\n");
        desc.append("```\n");
        desc.append("\n");
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
        desc.append("Generates FQN for top-level metadata objects before `attachTopObject`.\n\n");
        desc.append("```java\n");
        desc.append("String fqn = fqnGenerator\n");
        desc.append("    .generateStandaloneObjectFqn(catalog.eClass(), catalog.getName())\n");
        desc.append("    .toString();\n");
        desc.append("transaction.attachTopObject((IBmObject)catalog, fqn);\n");
        desc.append("```\n");
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
        desc.append("Provides BM model and editing context for metadata transactions.\n\n");
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
}
