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
import com._1c.g5.v8.bm.core.IBmObject;
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
import com.e1c.edt.ai.tools.JShellExecutionContext;
import com.e1c.edt.ai.tools.JShellExecutionResult;
import com.e1c.edt.ai.tools.ManualResourceLoader;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provides JShell bindings for 1C metadata operations. The scenario-oriented
 * manual is served by {@link MetadataManualCatalog} from {@code resources/manual/};
 * this class only exposes the runtime objects (factories, managers, generators)
 * and their per-binding descriptions.
 * <p>
 * Binding descriptions live as static markdown under
 * {@code resources/manual/bindings/<bindingName>.md}. The {@code mdFactory}
 * description has a {@code ${@method-list:MdClassFactory}} placeholder that is
 * resolved at runtime against {@link IMethodListProvider}.
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
    private final ManualResourceLoader loader;

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
        this.loader = new ManualResourceLoader(MetadataBindingProvider.class, "/manual"); //$NON-NLS-1$
        this.loader.registerDynamicResolver("method-list", this::renderMethodList); //$NON-NLS-1$
    }

    @Override
    public String getScope()
    {
        return "edt"; //$NON-NLS-1$
    }

    private String renderMethodList(String className)
    {
        Class<?> target;
        switch (className)
        {
        case "MdClassFactory": //$NON-NLS-1$
            target = MdClassFactory.class;
            break;
        default:
            return "_(unknown class: " + className + ")_"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        var sigs = methodListProvider.getPublicMethodSignatures(target);
        var sb = new StringBuilder();
        for (var s : sigs)
        {
            sb.append("- `").append(s).append("`\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return sb.toString();
    }

    private String bindingDoc(String name)
    {
        return loader.load("bindings/" + name + ".md", Map.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @SuppressWarnings("nls")
    @Override
    public Map<String, JShellBindingDescription> getBindings()
    {
        var bindings = new HashMap<String, JShellBindingDescription>();

        bindings.put("mdFactory", new JShellBindingDescription("Factory for creating 1C metadata objects",
            bindingDoc("mdFactory"), MdClassFactory.eINSTANCE, MdClassFactory.class,
            "**⚠️ RESTRICTION: Cannot be used outside BM transaction.** Use `mdFactory` ONLY in "
                + "AbstractBmTask.execute() body, where IBmTransaction is available. Do not use attachTopObject() for existing objects. "
                + "**IMPORTANT**: Objects created with mdFactory MUST have UUIDs set via "
                + "manual assignment: `object.setUuid(UUID.randomUUID())`. "
                + "NOTE: `modelFactory.fillDefaultReferences()` may timeout in JShell due to OSGi service limitations."));

        bindings.put("fqnGenerator", new JShellBindingDescription(
            "Generates FQNs (Fully Qualified Names) for top-level metadata objects. Required before attachTopObject().",
            bindingDoc("fqnGenerator"), topObjectFqnGenerator, ITopObjectFqnGenerator.class));

        bindings.put("modelFactory", new JShellBindingDescription(
            "Creates model objects in project/version context",
            bindingDoc("modelFactory"), modelObjectFactory, IModelObjectFactory.class));

        bindings.put("projectManager", new JShellBindingDescription(
            "Resolves IV8Project from Eclipse projects",
            bindingDoc("projectManager"), v8projectManager, IV8ProjectManager.class));

        bindings.put("modelManager", new JShellBindingDescription(
            "Provides BM model and editing contexts. Use for read/write operations with transactions.",
            bindingDoc("modelManager"), modelManager, IBmModelManager.class));

        bindings.put("resourceLookup", new JShellBindingDescription(
            "Maps metadata/model objects to Eclipse resources",
            bindingDoc("resourceLookup"), resourceLookup, IResourceLookup.class));

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
            IBmObject.class,
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
            "import com._1c.g5.v8.dt.mcore.McoreFactory;",
            "import com._1c.g5.v8.dt.mcore.Type;",
            "import com._1c.g5.v8.dt.mcore.TypeDescription;",
            "import com._1c.g5.v8.dt.mcore.TypeItem;",
            "import com._1c.g5.v8.dt.platform.core.typeinfo.*;",
            "import org.eclipse.emf.ecore.util.EcoreUtil;"
        );
        // @formatter:on
    }

    @SuppressWarnings("nls")
    @Override
    public String getRequiredNextStep(JShellExecutionContext context)
    {
        if (context == null || context.result == null || hasExecutionErrors(context.result)
            || !isLikelyMetadataMutation(context.code))
        {
            return "";
        }

        return "Before reporting success, compare the original user request, request_description, executed code, "
            + "and response_description. If any requested metadata object, attribute, tabular section, enum value, "
            + "type, reference, registrar, or property was omitted or narrowed, run a corrective JShell operation "
            + "for the same changed entity or fail explicitly; markers cannot detect missing requested features. "
            + "Call GetMarkers with marker_type \"1c\" scoped to each changed top-level entity .mdo path when "
            + "that path is known or can be derived. Fix only markers relevant to the entities changed by this "
            + "CRUD operation. Do not fix unrelated project-wide markers. Use project-wide GetMarkers only for "
            + "changes that can affect other objects, such as delete, rename, registrar links, references, "
            + "command interfaces, or configuration-level changes; even then, filter fixes to the changed "
            + "entities and directly affected references. Inspect all relevant 1C markers for the changed "
            + "entity/top object, including errors, warnings, and infos; do not check only errors. If SU45/type "
            + "markers appear, ensure every BasicFeature child "
            + "has its own fresh TypeDescription instance; do not reuse one TypeDescription across multiple "
            + "attributes, dimensions, or resources. For document CRUD, do not create custom attributes named "
            + "Date/Дата, Number/Номер, Posted/Проведен, Ref/Ссылка, or DeletionMark/ПометкаУдаления. For numeric "
            + "types, TypeDescriptionBuilder.setNumberQualifiers uses (scale, precision, nonNegative), for example "
            + "Number(10,2) is setNumberQualifiers(2, 10, false). For string types, do not use "
            + "setStringQualifiers with length greater than 100 unless the user explicitly requires it and the "
            + "EDT model accepts it; default to setStringQualifiers(100, false), not 150 or 1000. If the user "
            + "requested a concrete reference type such as CatalogRef.Контрагенты, CatalogRef.ХранимыеФайлы, "
            + "or EnumRef.ВидыТоваров, verify that the assigned TypeItem is the exact proxy "
            + "typeProvider.getProxy(\"CatalogRef.Контрагенты\"), getProxy(\"CatalogRef.ХранимыеФайлы\"), or "
            + "getProxy(\"EnumRef.ВидыТоваров\"); do not use generic IEObjectTypeNames.CATALOG_REF or ENUM_REF "
            + "unless the field is intentionally polymorphic. If a concrete reference proxy is null but the "
            + "referenced top object exists in this transaction/project, create a named Mcore Type fallback "
            + "with McoreFactory.eINSTANCE.createType(), setName(\"CatalogRef.Name\")/setNameRu(\"СправочникСсылка.Name\") "
            + "or setName(\"EnumRef.Name\")/setNameRu(\"ПеречислениеСсылка.Name\"), and use that TypeItem. Never replace "
            + "a requested reference with String and never report success after printing ERROR to stderr; throw "
            + "IllegalStateException for blocking preconditions. For registers, link registrar documents via "
            + "document.getRegisterRecords().add(register). Fix relevant markers before reporting success or starting "
            + "another 1C metadata CRUD operation.";
    }

    private boolean hasExecutionErrors(JShellExecutionResult result)
    {
        return result.compilationErrors != null && !result.compilationErrors.isEmpty()
            || result.runtimeErrors != null && !result.runtimeErrors.isEmpty();
    }

    private boolean isLikelyMetadataMutation(String code)
    {
        if (code == null || code.isBlank())
        {
            return false;
        }

        var hasMetadataContext = containsAny(code,
            "mdFactory", "modelFactory", "modelManager", "fqnGenerator", "IBmTransaction", "IBmModel", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "IBmGlobalEditingContext", "Configuration", "com._1c.g5.v8.dt.metadata", "com._1c.g5.v8.bm"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        var hasMutation = containsAny(code,
            "globalContext.execute", "new AbstractBmTask", "attachTopObject", "detachTopObject", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            ".set", ".add(", ".remove(", ".clear()"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return hasMetadataContext && hasMutation;
    }

    private boolean containsAny(String text, String... fragments)
    {
        for (var fragment : fragments)
        {
            if (text.contains(fragment))
            {
                return true;
            }
        }
        return false;
    }
}
