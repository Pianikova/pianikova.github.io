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
import com._1c.g5.v8.dt.core.platform.IEditingLanguageManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.dcs.model.schema.DcsFactory;
import com._1c.g5.v8.dt.form.generator.IFormFieldGenerator;
import com._1c.g5.v8.dt.form.generator.IFormGenerator;
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.moxel.MoxelFactory;
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
    private final IFormGenerator formGenerator;
    private final IFormFieldGenerator formFieldGenerator;
    private final IEditingLanguageManager editingLanguageManager;
    private final ManualResourceLoader loader;

    @Inject
    public MetadataBindingProvider(IV8ProjectManager v8projectManager, IBmModelManager modelManager,
        ITopObjectFqnGenerator topObjectFqnGenerator, IResourceLookup resourceLookup,
        IModelObjectFactory modelObjectFactory, IMethodListProvider methodListProvider,
        IFormGenerator formGenerator, IFormFieldGenerator formFieldGenerator,
        IEditingLanguageManager editingLanguageManager)
    {
        Preconditions.checkNotNull(v8projectManager);
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(topObjectFqnGenerator);
        Preconditions.checkNotNull(resourceLookup);
        Preconditions.checkNotNull(modelObjectFactory);
        Preconditions.checkNotNull(methodListProvider);
        Preconditions.checkNotNull(formGenerator);
        Preconditions.checkNotNull(formFieldGenerator);
        Preconditions.checkNotNull(editingLanguageManager);

        this.v8projectManager = v8projectManager;
        this.modelManager = modelManager;
        this.topObjectFqnGenerator = topObjectFqnGenerator;
        this.resourceLookup = resourceLookup;
        this.modelObjectFactory = modelObjectFactory;
        this.methodListProvider = methodListProvider;
        this.formGenerator = formGenerator;
        this.formFieldGenerator = formFieldGenerator;
        this.editingLanguageManager = editingLanguageManager;
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
            bindingDoc("mdFactory"), MdClassFactory.eINSTANCE, MdClassFactory.class));

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

        bindings.put("formGenerator", new JShellBindingDescription(
            "Generates the default Form (form.model) structure for a BasicForm owned by a metadata object.",
            bindingDoc("formGenerator"), formGenerator, IFormGenerator.class));

        bindings.put("formFieldGenerator", new JShellBindingDescription(
            "Builds the root FormFieldInfo tree required by formGenerator.generateForm(...).",
            bindingDoc("formFieldGenerator"), formFieldGenerator, IFormFieldGenerator.class));

        bindings.put("formFactory", new JShellBindingDescription(
            "EMF factory (FormFactory.eINSTANCE) for form.model items: FormGroup, FormField, FormAttribute.",
            bindingDoc("formFactory"), FormFactory.eINSTANCE, FormFactory.class));

        bindings.put("dcsFactory", new JShellBindingDescription(
            "EMF factory (DcsFactory.eINSTANCE) for DataCompositionSchema template content.",
            bindingDoc("dcsFactory"), DcsFactory.eINSTANCE, DcsFactory.class));

        bindings.put("moxelFactory", new JShellBindingDescription(
            "EMF factory (MoxelFactory.eINSTANCE) for SpreadsheetDocument template content.",
            bindingDoc("moxelFactory"), MoxelFactory.eINSTANCE, MoxelFactory.class));

        bindings.put("editingLanguageManager", new JShellBindingDescription(
            "Resolves the current editing language code, required by formGenerator.generateForm(...).",
            bindingDoc("editingLanguageManager"), editingLanguageManager, IEditingLanguageManager.class));

        return bindings;
    }

    @SuppressWarnings("nls")
    @Override
    public String getUseCases()
    {
        return "- Use this scope when the matched manual resource requires these bindings"
            + "\n- For scenario workflow, validation, and API usage rules, use `JShellManual`";
    }

    @SuppressWarnings("nls")
    @Override
    public String getDescription()
    {
        return "Binding provider for the `edt` scope. Detailed usage guidance is stored in manual markdown resources.";
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
            IFormGenerator.class,
            IFormFieldGenerator.class,
            FormFactory.class,
            DcsFactory.class,
            MoxelFactory.class,
            IEditingLanguageManager.class,
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
            "import java.util.UUID;",
            "import org.eclipse.core.resources.IProject;",
            "import org.eclipse.core.resources.IWorkspaceRoot;",
            "import org.eclipse.core.runtime.IProgressMonitor;",
            "import com._1c.g5.v8.bm.core.IBmNamespace;",
            "import com._1c.g5.v8.bm.core.IBmObject;",
            "import com._1c.g5.v8.bm.core.IBmTransaction;",
            "import com._1c.g5.v8.bm.integration.AbstractBmTask;",
            "import com._1c.g5.v8.bm.integration.IBmEditingContext;",
            "import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;",
            "import com._1c.g5.v8.bm.integration.IBmModel;",
            "import com._1c.g5.v8.bm.integration.IBmTask;",
            "import com._1c.g5.v8.dt.core.model.IModelObjectFactory;",
            "import com._1c.g5.v8.dt.core.naming.ITopObjectFqnGenerator;",
            "import com._1c.g5.v8.dt.core.platform.IBmModelManager;",
            "import com._1c.g5.v8.dt.core.platform.IResourceLookup;",
            "import com._1c.g5.v8.dt.core.platform.IV8Project;",
            "import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;",
            "import com._1c.g5.v8.dt.mcore.McorePackage;",
            "import com._1c.g5.v8.dt.mcore.TypeDescription;",
            "import com._1c.g5.v8.dt.mcore.TypeItem;",
            "import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;",
            "import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegisterDimension;",
            "import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegisterResource;",
            "import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;",
            "import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterDimension;",
            "import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterResource;",
            "import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterType;",
            "import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;",
            "import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;",
            "import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterDimension;",
            "import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterPeriodicity;",
            "import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterResource;",
            "import com._1c.g5.v8.dt.metadata.mdclass.Catalog;",
            "import com._1c.g5.v8.dt.metadata.mdclass.CatalogAttribute;",
            "import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;",
            "import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypes;",
            "import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;",
            "import com._1c.g5.v8.dt.metadata.mdclass.Configuration;",
            "import com._1c.g5.v8.dt.metadata.mdclass.Constant;",
            "import com._1c.g5.v8.dt.metadata.mdclass.Document;",
            "import com._1c.g5.v8.dt.metadata.mdclass.DocumentNumberPeriodicity;",
            "import com._1c.g5.v8.dt.metadata.mdclass.DocumentNumberType;",
            "import com._1c.g5.v8.dt.metadata.mdclass.DocumentTabularSection;",
            "import com._1c.g5.v8.dt.metadata.mdclass.EnumValue;",
            "import com._1c.g5.v8.dt.metadata.mdclass.HierarchyType;",
            "import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;",
            "import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterDimension;",
            "import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterPeriodicity;",
            "import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterResource;",
            "import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;",
            "import com._1c.g5.v8.dt.metadata.mdclass.MdObject;",
            "import com._1c.g5.v8.dt.metadata.mdclass.Recalculation;",
            "import com._1c.g5.v8.dt.metadata.mdclass.Report;",
            "import com._1c.g5.v8.dt.metadata.mdclass.TabularSectionAttribute;",
            "import com._1c.g5.v8.dt.platform.IEObjectProvider;",
            "import com._1c.g5.v8.dt.platform.IEObjectTypeNames;",
            "import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;",
            "import com._1c.g5.v8.dt.platform.version.Version;",
            "import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;",
            "import com._1c.g5.v8.dt.metadata.mdclass.MdObject;",
            "import com._1c.g5.v8.dt.metadata.mdclass.ScriptVariant;",
            "import com._1c.g5.v8.dt.metadata.mdclass.InterfaceCompatibilityMode;",
            "import com._1c.g5.v8.dt.metadata.mdclass.Template;",
            "import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;",
            "import com._1c.g5.v8.dt.metadata.mdclass.CatalogForm;",
            "import com._1c.g5.v8.dt.metadata.mdclass.DocumentForm;",
            "import com._1c.g5.v8.dt.form.generator.IFormGenerator;",
            "import com._1c.g5.v8.dt.form.generator.IFormFieldGenerator;",
            "import com._1c.g5.v8.dt.form.generator.FormType;",
            "import com._1c.g5.v8.dt.form.generator.FormFieldInfo;",
            "import com._1c.g5.v8.dt.form.model.Form;",
            "import com._1c.g5.v8.dt.form.model.FormFactory;",
            "import com._1c.g5.v8.dt.dcs.model.schema.DcsFactory;",
            "import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchema;",
            "import com._1c.g5.v8.dt.moxel.MoxelFactory;",
            "import com._1c.g5.v8.dt.moxel.SpreadsheetDocument;",
            "import com._1c.g5.v8.dt.core.platform.IEditingLanguageManager;",
            "import org.eclipse.emf.ecore.util.EcoreUtil;"
        );
        // @formatter:on
    }

}
