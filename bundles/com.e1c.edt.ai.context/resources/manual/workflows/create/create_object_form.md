## Safe Workflow: Create an object-owned Form (CatalogForm / DocumentForm / …)

Creates a form that belongs to a metadata object (e.g. `Catalog.Products`), **generates its
structure** with `formGenerator`, links it back to the metadata, and persists it as an external
`.form` resource. The deliverable is a **working form with a layout**, not an empty metadata stub.

> This scenario is for forms **owned by an object**. For a stand-alone `CommonForm` use
> `create_common_form` first, then run the `formGenerator` steps below (pass the `CommonForm` as
> `basicForm`).

### Hard rules — never violate

- ❌ **Do NOT stop after creating only the `BasicForm` metadata.** A `CatalogForm`/`DocumentForm`
  without a generated `Form` structure has no `Form.form` file — the form will not open and the
  owner's `default*Form` points at an empty stub. Creating only the metadata object is a
  **failed** result, even when `compilation_errors`/`runtime_errors` are empty. You MUST run
  `formGenerator` and `attachTopObject` the structure in the same task. The only acceptable
  metadata-only outcome is when `generateForm(...)` itself **throws** — then report that error
  honestly (see post-check), do not silently downgrade to "metadata only".
- ✅ **All three steps in one BM task:** (1) create + add the `BasicForm`, (2) `generateForm`,
  (3) `setMdForm` + `attachTopObject` the structure. Do not split into a "metadata first, layout
  later" plan.
- ✅ **Check existence first.** Fetch the owner with `transaction.getTopObjectByFqn("Catalog.<Name>")`
  and inspect `owner.getForms()`. If a form with the requested name already exists, do not
  recreate it — edit it (`edit_form`) or stop.
- ✅ **Set a UUID** on the `BasicForm` (`form.setUuid(UUID.randomUUID())`) — missing UUID causes SU45.
- ✅ **Add the form to the owner collection** (`owner.getForms().add(catalogForm)`).
- ⚠️ **Two `FormType` enums exist.** The `formGenerator`/`formFieldGenerator` argument uses
  `com._1c.g5.v8.dt.form.generator.FormType` (`GENERIC`, `OBJECT`, `LIST`, …).
  `BasicForm.setFormType(...)` uses `com._1c.g5.v8.dt.metadata.mdclass.FormType`
  (`Managed`/`Ordinary`). Import the **generator** one explicitly.
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
| InformationRegister | `createInformationRegisterForm()` | `InformationRegisterForm` | `RECORD` / `LIST` |

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

IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
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

After the task commits, call `GetMarkers` with `marker_type: "1c"` on the owner's `.mdo` **and**
confirm the structure file exists:

```
<projectRoot>/src/<TypePluralFolder>/<OwnerName>/Forms/<FormName>/Form.form
```

| Owner FQN | Form file |
|-----------|-----------|
| `Catalog.Products` | `src/Catalogs/Products/Forms/ФормаЭлемента/Form.form` |
| `Document.GoodsReceipt` | `src/Documents/GoodsReceipt/Forms/<FormName>/Form.form` |

- **`Form.form` exists and markers clean → success.**
- **`Form.form` is missing** → the form is incomplete. This is **not** success: STEP 2/3 did not
  run or did not persist. Do not report done. Re-check that `generateForm`/`attachTopObject` were
  actually executed (not omitted), fix, and re-run. If `generateForm(...)` threw, report the
  concrete exception and stop — do not present the empty `BasicForm` as a finished form.

Do not retry `attachTopObject` for a form FQN that already exists.
