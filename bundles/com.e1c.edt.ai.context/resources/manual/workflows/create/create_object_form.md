## Safe Workflow: Create an object-owned Form (CatalogForm / DocumentForm / …)

Creates a form that belongs to a metadata object (e.g. `Catalog.Products`), **generates its
structure** with `formGenerator`, links it back to the metadata, and persists it as an external
`.form` resource. The deliverable is a **working form with a layout**, not an empty metadata stub.
After the default layout exists, a small safe improvement pass through `Edit` on the existing
`Form.form` is mandatory. The edit must improve user-facing presentation: the form title,
business field captions/titles, a safe business group, or the main list/table presentation. Editing
service controls such as `ListSettingsComposerUserSettings`, search/status additions, command bars,
context menus, or extended tooltips does **not** count as the required improvement. If the generated
form contains standard `Object.Code` and/or `Object.Description` controls, at least one real `Edit`
call is required before reporting success.
Never use `Write` to create a `Form.form` file.

**Creation vs refinement policy:** use EDT API/generators for creation and structural regeneration;
use file `Edit` only for narrow changes to an existing generated file. This is intentional:
the full form layout API is too large and fragile for reliable LLM hand-construction, but EDT can
generate a valid default layout. After `Form.form` exists, simple changes such as captions,
visibility, titles, and small layout flags must be changed by `SearchFiles`/`Read` -> `Edit` ->
`GetMarkers` as the required post-generation improvement pass. `Write` is never allowed for
`.form` or `.mdo`.

**Reference style from real EDT forms (SSL):** simple catalog item forms either keep a flat list of
real business fields or put `Object.Description` and `Object.Code` into a compact top group. The
common safe pattern is a `form:FormGroup` named like `ГруппаНаименованиеКод` / `ГруппаНаименование`,
with `type` = `UsualGroup` and `extInfo/group` = `HorizontalIfPossible`; inside it, the description
field is usually named/titled `Наименование`, code is named/titled `Код`, and business attributes
stay below in a readable order. Do not invent fields only to make the form look richer. If the owner
has only standard code/description, improving the default form should preferably rename/title those
controls and, when an exact XML move is safe, group them as `Наименование` + `Код`. A lone form title
change is not enough for the mandatory improvement pass when Code/Description controls are present.

> This scenario is for forms **owned by an object**. For a stand-alone `CommonForm` use
> `create_common_form` first, then run the `formGenerator` steps below (pass the `CommonForm` as
> `basicForm`).

### Hard rules — never violate

- ❌ **Do NOT stop after creating only the `BasicForm` metadata.** A `CatalogForm`/`DocumentForm`/
  `*RegisterForm`
  without a generated `Form` structure has no `Form.form` file — the form will not open and the
  owner's `default*Form` points at an empty stub. Creating only the metadata object is a
  **failed** result, even when `compilation_errors`/`runtime_errors` are empty. You MUST run
  `formGenerator` and `attachTopObject` the structure in the same task. The only acceptable
  metadata-only outcome is when `generateForm(...)` itself **throws** — then report that error
  honestly (see post-check), do not silently downgrade to "metadata only".
- ✅ **All three steps in one BM task:** (1) create + add the `BasicForm`, (2) `generateForm`,
  (3) `setMdForm` + `attachTopObject` the structure. Do not split into a "metadata first, layout
  later" plan.
- ⛔ **Do not hand-write generated resources.** Never use `Write` to create `Form.form`, owner
  `.mdo`, or any form XML. If the generated file is missing, fix the EDT API workflow; do not
  synthesize the file with text tools.
- ✅ **Refine only after generation.** When the user asks to tweak an existing generated form
  ("hide field", "change title/caption", "make visible=false"), route to `edit_form`: read the
  existing `Form.form`, apply a small `Edit`, then validate with `GetMarkers`.
- ✅ **Do not report an untouched default form as success.** After creating a form for requests like
  "Создай справочник товаров и форму для него", read the generated `Form.form` and perform at least
  one safe exact `Edit` when `Object.Code` / `Object.Description` controls are present. Examples:
  add a Russian form title, add a group title `Основные данные`, or add/replace user-facing
  titles/captions with `Код` / `Наименование` where the XML nodes exist. If no safe exact edit is
  available, explicitly report that no safe edit was available.
- ✅ **Improve business UI, not service UI.** For list forms, prefer a readable form title or
  user-facing table/field captions. Do not edit `ListSettingsComposerUserSettings`,
  `ListSearchString`, `ListSearchControl`, `ListViewStatus`, `FormCommandBar`, `ListCommandBar`,
  `ContextMenu`, or `ExtendedTooltip` merely to satisfy the improvement requirement.
- ✅ **Use the real request project name.** Never leave `MyProject` in executable JShell. Check
  `project.exists()`, `projectManager.getProject(project) != null`, and
  `modelManager.getModel(project) != null` before entering the BM task.
- ✅ **Check existence first.** Fetch the owner with `transaction.getTopObjectByFqn("Catalog.<Name>")`
  and inspect `owner.getForms()`. If a form with the requested name already exists, do not
  recreate it — edit it (`edit_form`) or stop.
- ✅ **Set a UUID** on the `BasicForm` (`form.setUuid(UUID.randomUUID())`) — missing UUID causes SU45.
- ✅ **Add the form to the owner collection** (`owner.getForms().add(catalogForm)`).
- ⚠️ **Two `FormType` enums exist.** The `formGenerator`/`formFieldGenerator` argument uses
  `com._1c.g5.v8.dt.form.generator.FormType` (`GENERIC`, `OBJECT`, `LIST`, …).
  `BasicForm.setFormType(...)` uses `com._1c.g5.v8.dt.metadata.mdclass.FormType`
  (`Managed`/`Ordinary`). Import the **generator** one explicitly.
- ✅ **Register form type must match owner capabilities.** `InformationRegister` supports record and
  list forms (`FormType.RECORD` -> `setDefaultRecordForm`, `FormType.LIST` -> `setDefaultListForm`).
  `AccumulationRegister`, `AccountingRegister`, and `CalculationRegister` support list forms only in
  this workflow. For them use `FormType.LIST` and `setDefaultListForm(...)`; never call
  `setDefaultRecordForm(...)` and never use `FormType.RECORD`. `FormType.RECORD` on these registers
  fails in `FormFieldsGenerator` with `MdRecordManagerType` / `There is no feature with type ...`.
- ⛔ **`columnCount` must be non-null for `OBJECT`, `FOLDER`, `CONSTANTS`, `RECORD`, `REPORT`.**
  `generateForm` declares it as `Integer`, but for these types it is unboxed to a primitive `int`
  inside the generator — passing `null` causes a `NullPointerException` deep in
  `FormGenerator`/`ItemFormContainGenerator` (empty `runtime_errors` is **not** guaranteed; it is a
  real NPE). Pass a concrete value, e.g. `1`. Only `LIST`/`CHOICE`/`FOLDER_CHOICE`/`RECORD_SET`
  ignore `columnCount` (there you may pass `null`). In this EDT build the method has 8 arguments
  and ends with `columnCount`; do not pass a ninth `interfaceCompatibilityMode` argument.

### Choosing the form factory method and generator FormType

| Owner | `mdFactory` method | element type | typical generator `FormType` |
|-------|--------------------|--------------|------------------------------|
| Catalog | `createCatalogForm()` | `CatalogForm` | `OBJECT` (item) / `LIST` (list) |
| Document | `createDocumentForm()` | `DocumentForm` | `OBJECT` / `LIST` |
| InformationRegister | `createInformationRegisterForm()` | `InformationRegisterForm` | `RECORD` for record form / `LIST` for list form |
| AccumulationRegister | `createAccumulationRegisterForm()` | `AccumulationRegisterForm` | `LIST` only |
| AccountingRegister | `createAccountingRegisterForm()` | `AccountingRegisterForm` | `LIST` only |
| CalculationRegister | `createCalculationRegisterForm()` | `CalculationRegisterForm` | `LIST` only |

Default form references on the owner: `setDefaultObjectForm(...)`, `setDefaultListForm(...)`,
`setDefaultFolderForm(...)` (Catalog), etc. — set them only when the user wants this form to be
the default of its kind.

### Worked example — default item form for Catalog.Products (full, required flow)

```java
import com._1c.g5.v8.dt.form.generator.FormType;        // GENERATOR FormType — not the mdclass one
import com._1c.g5.v8.dt.form.generator.FormFieldInfo;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogForm;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.ScriptVariant;
import com._1c.g5.v8.dt.platform.version.Version;

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

String formName = globalContext.execute(new AbstractBmTask<String>("Create object form") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Products — create it first");
        }
        // idempotency: do not recreate an existing form
        for (CatalogForm existing : owner.getForms()) {
            if ("ФормаЭлемента".equals(existing.getName())) {
                throw new IllegalStateException("Form already exists: Catalog.Products.Form.ФормаЭлемента");
            }
        }

        // STEP 1 (required) — BasicForm metadata
        CatalogForm catalogForm = mdFactory.createCatalogForm();
        catalogForm.setName("ФормаЭлемента");
        catalogForm.getSynonym().put("ru", "Форма элемента");
        catalogForm.setUuid(UUID.randomUUID());
        owner.getForms().add(catalogForm);
        owner.setDefaultObjectForm(catalogForm);   // optional: make it the default item form

        // STEP 2 (required) — generate the form structure
        ScriptVariant scriptVariant = v8project.getScriptVariant();
        Version version = v8project.getVersion();
        String languageCode = editingLanguageManager.getEditingLanguageCode(project);
        FormType genType = FormType.OBJECT;   // generator enum
        Integer columnCount = Integer.valueOf(1);   // REQUIRED for OBJECT — null here => NullPointerException

        FormFieldInfo rootField =
            formFieldGenerator.getFormGeneratorFields(owner, genType, scriptVariant, version);
        Form form = formGenerator.generateForm(owner, catalogForm, genType, scriptVariant,
            languageCode, version, rootField, columnCount);

        // STEP 3 (required) — link + persist the structure as an external resource
        catalogForm.setForm(form);
        form.setMdForm(catalogForm);
        String formFqn = fqnGenerator.generateExternalPropertyFqn(
            catalogForm, MdClassPackage.Literals.BASIC_FORM__FORM);
        transaction.attachTopObject((IBmObject)form, formFqn);

        return catalogForm.getName();
    }
});
System.out.println("Created form with structure: " + formName);
return formName;
```

### Required post-check

After the task commits, call `GetMarkers` with `marker_type: "1c"` on the owner's `.mdo`, confirm
the structure file exists, and complete the required safe improvement pass with `Read` -> `Edit` ->
`GetMarkers`:

```
<projectRoot>/src/<TypePluralFolder>/<OwnerName>/Forms/<FormName>/Form.form
```

| Owner FQN | Form file |
|-----------|-----------|
| `Catalog.Products` | `src/Catalogs/Products/Forms/ФормаЭлемента/Form.form` |
| `Document.GoodsReceipt` | `src/Documents/GoodsReceipt/Forms/<FormName>/Form.form` |

- **`Form.form` exists, at least one safe user-facing `Edit` improvement was made when
  Code/Description controls or other editable business presentation fragments are present, and
  markers clean → success.** Edits to service-only controls do not satisfy this condition.
- **`Form.form` exists but is still the untouched EDT default with Code/Description controls → not
  success.** Run `edit_form`/`Edit` before reporting done.
- **`Form.form` is missing** → the form is incomplete. This is **not** success: STEP 2/3 did not
  run or did not persist. Do not report done. Re-check that `generateForm`/`attachTopObject` were
  actually executed (not omitted), fix, and re-run. If `generateForm(...)` threw, report the
  concrete exception and stop — do not present the empty `BasicForm` as a finished form.

Do not retry `attachTopObject` for a form FQN that already exists.
