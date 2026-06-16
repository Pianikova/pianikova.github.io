## Safe Workflow: Show an object requisite on a form (regenerate, do not hand-build)

Use this for requests like "добавь на форму элемента справочника Номенклатура реквизит Бренд".
The safe route is two metadata/model steps: first ensure the object attribute exists, then generate
or regenerate the form structure with `formGenerator`.

CRITICAL for this EDT build: call `generateForm` with exactly 8 arguments:

```java
Form form = formGenerator.generateForm(owner, catalogForm, genType, scriptVariant,
    languageCode, version, rootField, Integer.valueOf(1));
```

Never add an obsolete ninth argument after `columnCount`; the extra value does not compile.

### Hard rules — never violate

- Do not create `FormAttribute`, `FormField`, `DataPath`, `FormDataPath`, or `FormItem` manually
  for object attributes. Do not call `setSegments`, `setDataPath`, `setAutoMaxWidth`,
  `FormField.ViewMode`, `FormField.EditMode`, or `form.model.FormType`.
- If the object attribute is absent, create a real `CatalogAttribute`/`DocumentAttribute` first
  with a non-empty `TypeDescription`. A form-local attribute is not a metadata requisite.
- For simple string attributes, prefer copying a known-good string `TypeDescription` from an
  existing attribute on the same object (`EcoreUtil.copy(existing.getType())`). This avoids
  `projectManager.getProject(project)` failures in dev-autopilot sessions. Use
  `IEObjectProvider` only when no suitable existing type can be copied.
- Replace `<ProjectName>` with the real request project name. Never leave `MyProject` or translated
  guesses like `Warehouse` in executable JShell. For Step A do not call `projectManager.getProject`;
  only `modelManager.getModel(project)` is needed. If it is null, the project name is wrong or the
  project is not open; throw a clear error instead of letting a `NullPointerException` escape.
- Wrap each JShell snippet in `{ ... }` so failed attempts do not leave stale top-level variables.
  After an early `NullPointerException` from project/model lookup, create a fresh `jshellsession`
  before retrying.
- If the requested form already exists, regenerate it once. Do not call `create_object_form`, do not
  create another `CatalogForm`, and do not finish with "form already exists".
- If the requested form is missing, run the full `create_object_form` workflow: create `BasicForm`,
  call `formGenerator.generateForm(...)`, `setMdForm(...)`, and attach the form structure through
  `BASIC_FORM__FORM`. Creating only `CatalogForm` metadata is incomplete.
- `catalogForm.getForm()` returns `AbstractForm`; store it only as `AbstractForm old` for detach.
  The newly generated structure is `com._1c.g5.v8.dt.form.model.Form`.
- For generator `FormType.OBJECT`, pass `Integer.valueOf(1)` as `columnCount`; never pass `null`.
- In this EDT build, `formGenerator.generateForm(...)` has 8 arguments and ends with
  `Integer columnCount`. Do not pass a ninth `interfaceCompatibilityMode` argument.
- Exact imports: `IProject` is `org.eclipse.core.resources.IProject`; `IBmModel` and
  `IBmGlobalEditingContext` are in `com._1c.g5.v8.bm.integration`; `IBmTransaction` is in
  `com._1c.g5.v8.bm.core`.

### Step A — create or repair missing string attribute

```java
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogAttribute;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import java.util.UUID;

{
IProject project = workspaceRoot.getProject("<ProjectName>");
if (project == null || !project.exists()) {
    throw new IllegalStateException("Project not found: <ProjectName>");
}
IBmModel bmModel = modelManager.getModel(project);
if (bmModel == null) {
    throw new IllegalStateException("BM model is not available: " + project.getName());
}
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String attrResult = globalContext.execute(new AbstractBmTask<String>("Ensure catalog attribute") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Номенклатура");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Номенклатура");
        }
        TypeDescription stringTypeTemplate = null;
        for (CatalogAttribute existing : owner.getAttributes()) {
            TypeDescription existingType = existing.getType();
            if (existingType != null && existingType.getTypes().stream()
                    .anyMatch(t -> "String".equals(t.getName()))) {
                stringTypeTemplate = existingType;
                break;
            }
        }
        if (stringTypeTemplate == null) {
            throw new IllegalStateException(
                "No existing string TypeDescription to copy; use create_attribute_for_entity");
        }

        for (CatalogAttribute existing : owner.getAttributes()) {
            if ("Бренд".equals(existing.getName())) {
                if (existing.getType() == null || existing.getType().getTypes().isEmpty()) {
                    existing.setType((TypeDescription)EcoreUtil.copy(stringTypeTemplate));
                    return "Repaired attribute type: Бренд";
                }
                return "Attribute already exists: Бренд";
            }
        }

        CatalogAttribute attr = mdFactory.createCatalogAttribute();
        attr.setName("Бренд");
        attr.setUuid(UUID.randomUUID());
        attr.setType((TypeDescription)EcoreUtil.copy(stringTypeTemplate));
        owner.getAttributes().add(attr);
        return "Created attribute: Бренд";
    }
});
System.out.println(attrResult);
}
```

### Step B — regenerate existing item form

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

String result = globalContext.execute(new AbstractBmTask<String>("Regenerate item form") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Номенклатура");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Номенклатура");
        }
        if (owner.getAttributes().stream().noneMatch(a -> "Бренд".equals(a.getName()))) {
            throw new IllegalStateException("Attribute Бренд is missing; run Step A first");
        }
        CatalogForm catalogForm = null;
        for (CatalogForm f : owner.getForms()) {
            if ("ФормаЭлемента".equals(f.getName())) {
                catalogForm = f;
                break;
            }
        }
        if (catalogForm == null) {
            throw new IllegalStateException("Form ФормаЭлемента is missing; run create_object_form full workflow");
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
        return "Regenerated form: " + catalogForm.getName();
    }
});
System.out.println(result);
}
```

### Required post-check

Call `GetMarkers` with `marker_type:"1c"` on the owner's `.mdo` and verify that
`src/Catalogs/<OwnerName>/Forms/<FormName>/Form.form` exists. Report success only after the
attribute exists exactly once and the form resource exists.
