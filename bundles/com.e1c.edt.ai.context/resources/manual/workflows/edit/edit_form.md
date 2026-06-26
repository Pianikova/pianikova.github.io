## Safe Workflow: Show an object requisite on a form (by regenerating the form)

To make a requisite visible on an object form, the reliable approach is **regeneration**: ensure the
metadata attribute exists, then regenerate the form's default structure with `formGenerator` — the
regenerated form includes every object attribute as a proper, fully-initialised field. This reuses
the proven `create_object_form` generation path instead of hand-building a `FormField` (which is
fragile: fields need version-aware defaults like `userVisible`, the right `ExtInfo`, a valid
`dataPath`, and unique ids).

> **Tradeoff:** regeneration **replaces** the form's structure, so any custom layout is lost. Use it
> for default/auto-generated forms or when "просто покажи реквизит" is acceptable. Do not use it to
> preserve a hand-customised form.

### Direct file editing policy

- `Edit` is allowed for existing generated `Form.form` files when the requested layout change is too
  detailed for the EDT form API. Always `Read` first, make the smallest exact replacement, then run
  `GetMarkers` for the owner metadata object.
- When editing localized form text (`<title>`, captions, group titles), every inserted block must
  contain `<key>ru</key>` before `<value>...`. Do not insert a bare
  `<title><value>...</value></title>` block; EDT reports SU46 because `LocalStringMapEntry.key` is
  required.
- `Write` is forbidden for `Form.form` and `.mdo` files. The form must be created first through the
  EDT metadata workflow so the default layout/resource exists and is registered in BM.
- Do not use `Edit` to create a missing form resource. If `Form.form` is absent, run
  `create_object_form` or regenerate/repair the form through EDT API first.

### What "вывести реквизит X на форму" means (two layers — do both, in order)

1. The **metadata attribute** X must exist on the object (`CatalogAttribute`/`DocumentAttribute`).
   If missing, create it first with `create_attribute_for_entity` or the Step A snippet below **in
   its own BM task**. A form can only show an existing object attribute. A `FormAttribute` is
   form-local data — NOT the object requisite; never substitute it.
2. **Regenerate the form** (this scenario) so the new attribute appears as a field.

For combined prompts like "добавь на форму элемента справочника Номенклатура реквизит Бренд",
execute exactly this sequence:

1. Check `Catalog.Номенклатура.getAttributes()`; if `Бренд` is absent, create `CatalogAttribute`
   with a non-empty `TypeDescription`.
2. Find existing `ФормаЭлемента`; if it exists, regenerate it once. Do not call
   `create_object_form` and do not create another `CatalogForm`.
3. If `ФормаЭлемента` is missing, run the full `create_object_form` workflow. Never create only
   the `CatalogForm` metadata.

### Hard rules — never violate

- ⛔ **Attribute first.** If X is not yet a `CatalogAttribute`/`DocumentAttribute`, run
  `create_attribute_for_entity` (or the object-specific edit workflow) first in a separate BM task.
  Regenerating before the attribute exists produces a form without the field. Do not retry
  regeneration when the runtime error says "реквизит ... не найден" — switch to the attribute
  scenario, create the metadata attribute, then return here.
- ❌ **Do not hand-build form fields for object attributes.** Avoid direct `FormAttribute`,
  `DataPath`, `FormField`, `setSegments(...)`, `setDataPath(...)`, `setAutoMarkIncomplete(...)`,
  `setAutoMaxWidth(...)`, `FormField.ViewMode`, `FormField.EditMode`, `form.model.FormType`, or
  boolean `setUserVisible(...)` code for this task. Those APIs are version-sensitive and caused
  repeated JShell compile failures. Use `formGenerator.generateForm(...)` once instead.
- ⚠️ `catalogForm.getForm()` returns `AbstractForm`, not `com._1c.g5.v8.dt.form.model.Form`.
  Store the old value as `com._1c.g5.v8.dt.metadata.mdclass.AbstractForm old =
  catalogForm.getForm();` only for detach. The new generated value is
  `Form form = formGenerator.generateForm(...)`.
- ⛔ Never use `FormFactory.eINSTANCE.createForm()` to repair or regenerate a form. It creates an
  empty form shell; fix the `formGenerator` code instead.
- ⛔ **Existing form means regenerate, not create.** If `owner.getForms()` already contains
  `ФормаЭлемента`, do not switch to `create_object_form` and do not throw "form already exists" as
  a final answer. Continue with the regeneration task below.
- ⛔ **Replace, don't double-attach.** The form structure is a top object at the form's
  external-property FQN. To regenerate: **detach the old structure** (`transaction.detachTopObject((IBmObject)catalogForm.getForm())`)
  before attaching the new one, else `attachTopObject` throws `BmFqnAlreadyInUseException`.
- ⛔ **Detach + regenerate + attach in ONE atomic BM task** (single `globalContext.execute`). Detach
  is destructive: a detach without a committed re-attach leaves the form with no `Form.form` file
  (registered but broken). Never split detach and attach across tasks.
- ⛔ **Regenerate EXACTLY ONCE.** Run the regeneration task a single time. After it commits and
  `GetMarkers` is clean, **STOP** — do not detach/regenerate again. Repeated regenerations thrash the
  form resource and, combined with the tool-round limit, can leave it broken.
- ⛔ **`columnCount` non-null** for OBJECT/FOLDER/CONSTANTS/RECORD/REPORT generator types (e.g. `1`);
  LIST/CHOICE ignore it (see `create_object_form`).
- ⚠️ Two `FormType` enums: use the **generator** `com._1c.g5.v8.dt.form.generator.FormType`
  (`OBJECT` for an item form, `LIST` for a list form).

### Step A example — create missing object attribute first

Run this only if the object attribute is absent. This creates a normal string
`CatalogAttribute`; adjust the type if the user requested a reference or number.

```java
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogAttribute;

IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String attrResult = globalContext.execute(new AbstractBmTask<String>("Ensure catalog attribute") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Номенклатура");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Номенклатура");
        }
        for (CatalogAttribute existing : owner.getAttributes()) {
            if ("Бренд".equals(existing.getName())) {
                return "Attribute already exists: Бренд";
            }
        }
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        if (stringType == null) {
            stringType = (TypeItem)typeProvider.createProxy(IEObjectTypeNames.STRING);
        }
        if (stringType == null) {
            throw new IllegalStateException("Cannot resolve primitive type: STRING");
        }
        TypeDescription type = new TypeDescriptionBuilder()
            .addType(stringType)
            .setStringQualifiers(100, false)
            .build();

        CatalogAttribute attr = mdFactory.createCatalogAttribute();
        attr.setName("Бренд");
        attr.setUuid(UUID.randomUUID());
        attr.setType(type);
        owner.getAttributes().add(attr);
        return "Created attribute: Бренд";
    }
});
System.out.println(attrResult);
```

### Step B example — show `Производитель` on Catalog.Номенклатура item form

Prerequisite: `Catalog.Номенклатура` already has attribute `Производитель` (else run
`create_attribute_for_entity` or Step A first, in its own BM task).

```java
import com._1c.g5.v8.dt.form.generator.FormType;
import com._1c.g5.v8.dt.form.generator.FormFieldInfo;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogForm;
import com._1c.g5.v8.dt.metadata.mdclass.AbstractForm;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.ScriptVariant;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.platform.version.Version;

IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String result = globalContext.execute(new AbstractBmTask<String>("Regenerate form") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Номенклатура");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Номенклатура");
        }
        if (owner.getAttributes().stream().noneMatch(a -> "Производитель".equals(a.getName()))) {
            throw new IllegalStateException(
                "Attribute Catalog.Номенклатура.Производитель does not exist — create it with edit_catalog first");
        }
        CatalogForm catalogForm = null;
        for (CatalogForm f : owner.getForms()) {
            if ("ФормаЭлемента".equals(f.getName())) { catalogForm = f; break; }
        }
        if (catalogForm == null) {
            throw new IllegalStateException("Form ФормаЭлемента not found; create it with create_object_form");
        }

        // 1) detach the old structure (FQN is already in use)
        com._1c.g5.v8.dt.metadata.mdclass.AbstractForm old = catalogForm.getForm();
        if (old != null) {
            transaction.detachTopObject((IBmObject)old);
        }

        // 2) regenerate (same proven path as create_object_form)
        ScriptVariant scriptVariant = v8project.getScriptVariant();
        Version version = v8project.getVersion();
        String languageCode = editingLanguageManager.getEditingLanguageCode(project);
        FormType genType = FormType.OBJECT;            // item form; use LIST for a list form
        Integer columnCount = Integer.valueOf(1);      // REQUIRED for OBJECT
        FormFieldInfo rootField =
            formFieldGenerator.getFormGeneratorFields(owner, genType, scriptVariant, version);
        Form form = formGenerator.generateForm(owner, catalogForm, genType, scriptVariant,
            languageCode, version, rootField, columnCount);

        // 3) link + attach the regenerated structure
        form.setMdForm(catalogForm);
        org.eclipse.emf.ecore.EReference formReference =
            (org.eclipse.emf.ecore.EReference)catalogForm.eClass().getEStructuralFeature("form");
        String formFqn = fqnGenerator.generateExternalPropertyFqn(
            catalogForm, formReference);
        transaction.attachTopObject((IBmObject)form, formFqn);

        return catalogForm.getName();
    }
});
System.out.println("Regenerated form: " + result);
return result;
```

### Required post-check

Call `GetMarkers` (`marker_type:"1c"`) on the owner's `.mdo`
(`src/Catalogs/<OwnerName>/<OwnerName>.mdo`) and confirm `Form.form` was rewritten and contains a
`<items xsi:type="form:FormField">` whose `<dataPath>` ends with the attribute name. Markers must be
clean. If the field is absent, the metadata attribute was likely missing (step 1) — add it and
regenerate again.
