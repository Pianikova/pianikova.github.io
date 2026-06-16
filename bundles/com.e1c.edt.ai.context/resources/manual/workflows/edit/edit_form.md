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

### What "вывести реквизит X на форму" means (two layers — do both, in order)

1. The **metadata attribute** X must exist on the object (`CatalogAttribute`/`DocumentAttribute`).
   If missing, create it first with `edit_catalog`/`edit_document` **in its own BM task**. A form can
   only show an existing attribute. (A `FormAttribute` is form-local data — NOT the object requisite;
   never substitute it.)
2. **Regenerate the form** (this scenario) so the new attribute appears as a field.

### Hard rules — never violate

- ⛔ **Attribute first.** If X is not yet a `CatalogAttribute`/`DocumentAttribute`, run
  `edit_catalog`/`edit_document` first (separate task). Regenerating before the attribute exists
  produces a form without the field.
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

### Worked example — show `Производитель` on Catalog.Номенклатура item form

Prerequisite: `Catalog.Номенклатура` already has attribute `Производитель` (else run `edit_catalog`
first, in its own BM task).

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
        AbstractForm old = catalogForm.getForm();
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
            languageCode, version, rootField, columnCount, null);

        // 3) link + attach the regenerated structure
        form.setMdForm(catalogForm);
        String formFqn = fqnGenerator.generateExternalPropertyFqn(
            catalogForm, MdClassPackage.Literals.BASIC_FORM__FORM);
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
