## Safe Workflow: Show an object requisite on a form (regenerate, do not hand-build)

Use this for requests like "добавь на форму элемента справочника Номенклатура реквизит Бренд".
The safe route is two metadata/model steps: first ensure the object attribute exists, then generate
or regenerate the form structure with `formGenerator`.

### Direct edit route for existing generated forms

Prefer this route for small changes and improvements in an existing generated form: hide/show a
field, change a field title, caption, synonym text, visibility flag, or another local layout flag.
It is also the allowed route for improving a default generated form when the user says it looks raw,
bad, poorly arranged, or asks to make it nicer. Examples: "hide field Code", "change the title of
field X on Catalog.Y item form", "make field X invisible", "improve the default form".
Run this route after creating a new default form as the mandatory safe improvement pass.
Do not use JShell form layout APIs for these edits. They are too fragile for direct manual
construction; EDT already created a valid default layout, so refine the existing file instead.

1. Use `SearchFiles`/`Read` to find and read the existing `Form.form`
   (`src/Catalogs/<Owner>/Forms/<FormName>/Form.form` or the matching owner folder).
   If the user named an exact owner (`Catalog.ТоварыLife02`), do not edit a similar owner
   (`ТоварыLife01`, `Товары`, etc.) when the exact owner/form is missing. Stop and report the
   missing exact object instead.
2. Verify the target element exists in that file. For object fields, look for names like
   `<name>Code</name>` and data-path segments like `<segments>Object.Code</segments>`.
3. Use `Edit` with a small exact replacement in that existing `Form.form`. For hiding a field,
   replace only the relevant `<visible>true</visible>`/visibility fragment for that field or its
   directly associated items. For simple cosmetic improvements, make only exact, reviewable XML
   changes such as title/caption values, visibility flags, or moving an already existing item block.
   Do not rewrite the whole form.
4. Run `GetMarkers` with `marker_type:"1c"` for the owner `.mdo`.
5. Report success only after the file was edited and markers were checked.

`Write` is forbidden for `.form` and `.mdo`. `Edit` is allowed only for an existing file that was
read first. If `Form.form` is missing, first create or repair the form through `create_object_form`;
never create `Form.form` with `Write`.

If the requested target is absent from the existing `Form.form`, stop and report that the form does
not contain that field/control yet. Do not switch to `Task skill="design"`, do not hand-build form
layout objects, and do not silently regenerate the form unless the user's request is structural
("add/show requisite X" or "regenerate the form"). For a missing object requisite, create the
metadata requisite first and then regenerate through `formGenerator`; for a missing standard field
such as `Code`, explain that the current generated form has no `Object.Code` control to edit.

### Decision table

| Request type | Route |
|--------------|-------|
| Create missing form | `create_object_form` via EDT API/generator |
| Existing form, hide/show field | `SearchFiles`/`Read` -> `Edit` existing `Form.form` -> `GetMarkers` |
| Existing form, change title/caption | `SearchFiles`/`Read` -> `Edit` existing `Form.form` -> `GetMarkers` |
| Existing default form, make nicer/improve | `SearchFiles`/`Read` -> small `Edit` refinements -> `GetMarkers` |
| Just created default form | `Read` generated `Form.form` -> safe presentation `Edit` if possible -> `GetMarkers` |
| Add missing object requisite and show it | create/repair metadata attribute, then regenerate with `formGenerator` |
| `Form.form` missing or broken | repair through EDT API; do not `Write` raw XML |

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
- Do not dismiss default generated forms as failed or "bad" merely because they are simple. If the
  user wants a nicer layout, improve the existing `Form.form` with precise `Edit` operations.
- After any successful form creation, run a safe improvement pass through this route. Prefer obvious
  user-facing fixes such as `Code` -> `Код` and `Description` -> `Наименование` captions/titles
  when the exact controls are present. If title/caption nodes are absent, add a safe form title or
  group title by editing existing XML structure. If no exact safe replacement is available, keep the
  generated layout and report that it was inspected.
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
- Never switch from the exact requested owner to a fuzzy match. A request for `ТоварыLife02` must not
  read or edit `ТоварыLife01` just because it is the nearest existing catalog.
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

        catalogForm.setForm(form);
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
