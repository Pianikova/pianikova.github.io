## Safe Workflow: Create object form with generated structure

Use this for missing object-owned forms such as `Catalog.Номенклатура.Form.ФормаЭлемента`.
The result is complete only when both metadata and `Form.form` are produced.

### Hard rules

- Do not create only `CatalogForm`/`DocumentForm` metadata. Always call `formGenerator.generateForm`
  and attach the generated `Form` as `BASIC_FORM__FORM`.
- If the form metadata already exists but `catalogForm.getForm()` is null or `Form.form` is missing,
  reuse that existing form metadata and attach a generated structure. Do not create a duplicate form.
- In this EDT build, `generateForm(...)` has 8 arguments and ends with `Integer columnCount`.
  Do not pass a ninth `null` argument.
- For `FormType.OBJECT`, `FOLDER`, `CONSTANTS`, `RECORD`, and `REPORT`, pass non-null
  `Integer.valueOf(1)` as `columnCount`.
- Exact imports: generator form type is `com._1c.g5.v8.dt.form.generator.FormType`, not
  `metadata.mdclass.FormType` and not `form.model.FormType`.

### Example — generate Catalog item form

```java
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.form.generator.FormFieldInfo;
import com._1c.g5.v8.dt.form.generator.FormType;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.AbstractForm;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogForm;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.ScriptVariant;
import com._1c.g5.v8.dt.platform.version.Version;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import java.util.UUID;

{
IProject project = workspaceRoot.getProject("<ProjectName>");
if (project == null || !project.exists()) {
    throw new IllegalStateException("Project not found: <ProjectName>");
}
IV8Project v8project = projectManager.getProject(project);
if (v8project == null) {
    throw new IllegalStateException("V8 project is not available: " + project.getName());
}
IBmModel bmModel = modelManager.getModel(project);
if (bmModel == null) {
    throw new IllegalStateException("BM model is not available: " + project.getName());
}
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String result = globalContext.execute(new AbstractBmTask<String>("Create or repair item form") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Номенклатура");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Номенклатура");
        }

        CatalogForm catalogForm = null;
        for (CatalogForm existing : owner.getForms()) {
            if ("ФормаЭлемента".equals(existing.getName())) {
                catalogForm = existing;
                break;
            }
        }
        if (catalogForm == null) {
            catalogForm = mdFactory.createCatalogForm();
            catalogForm.setName("ФормаЭлемента");
            catalogForm.getSynonym().put("ru", "Форма элемента");
            catalogForm.setUuid(UUID.randomUUID());
            owner.getForms().add(catalogForm);
            owner.setDefaultObjectForm(catalogForm);
        }

        AbstractForm old = catalogForm.getForm();
        if (old != null) {
            transaction.detachTopObject((IBmObject)old);
        }

        ScriptVariant scriptVariant = v8project.getScriptVariant();
        Version version = v8project.getVersion();
        String languageCode = editingLanguageManager.getEditingLanguageCode(project);
        FormType genType = FormType.OBJECT;
        FormFieldInfo rootField =
            formFieldGenerator.getFormGeneratorFields(owner, genType, scriptVariant, version);
        Form form = formGenerator.generateForm(owner, catalogForm, genType, scriptVariant,
            languageCode, version, rootField, Integer.valueOf(1));

        form.setMdForm(catalogForm);
        String formFqn = fqnGenerator.generateExternalPropertyFqn(
            catalogForm, MdClassPackage.Literals.BASIC_FORM__FORM);
        transaction.attachTopObject((IBmObject)form, formFqn);
        return "Generated form: " + catalogForm.getName();
    }
});
System.out.println(result);
}
```

### Required post-check

Run `GetMarkers` on the owner's `.mdo` and confirm
`src/Catalogs/<OwnerName>/Forms/<FormName>/Form.form` exists.
