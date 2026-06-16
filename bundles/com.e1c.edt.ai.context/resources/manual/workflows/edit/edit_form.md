## Safe Workflow: Edit an existing Form (add a visible field bound to an attribute)

Loads an existing form structure and adds a **visible input field** bound to a data path, or adds a
form attribute / group. Use this after `create_object_form`, or to modify any existing form.

### What "вывести реквизит на форму" means (two layers — do both)

To **show a catalog/document requisite on the form** you need two things:
1. the **metadata attribute** must exist on the object (e.g. `Catalog.Номенклатура` must have an
   attribute `Комментарий`). If it does not, create it first with `edit_catalog`/`edit_document` —
   a form field can only bind to an existing data path.
2. a **`FormField`** on the form whose `dataPath` is `<mainAttribute>.<attributeName>` (e.g.
   `Объект.Комментарий`). This step adds that field.

If the user names a requisite that is not yet a metadata attribute, do step 1 first (separate BM
task via `edit_catalog`), then step 2 here. Never bind a field to a non-existent attribute — it
fails with `IllegalStateException: Attribute not found`.

### Hard rules — never violate

- ⛔ **Do NOT add a `FormAttribute` named like the requisite and call it done.** A `FormAttribute` is
  **form-local data**, not the object's requisite, and it shows **nothing** on the form. "Вывести
  реквизит X на форму" = a **metadata `CatalogAttribute`/`DocumentAttribute` X** (create with
  `edit_catalog`/`edit_document` if missing) **plus a `FormField`** bound to `<mainAttribute>.X`.
  Adding only a form attribute is a **failed** result.
- ⛔ **Two steps, in order:** (1) ensure the metadata attribute X exists on the object (separate BM
  task via `edit_catalog`/`edit_document`); (2) add the `FormField` (this scenario). If X is not yet
  a metadata attribute, do step 1 first — do not substitute a form attribute for it.
- ⛔ **Load, do not recreate.** Get the form structure via `catalogForm.getForm()` (the loaded
  `com._1c.g5.v8.dt.form.model.Form`). Never `attachTopObject` the form or the owner again
  (`BmFqnAlreadyInUseException`). Modifying the loaded `Form` inside the global-context task
  auto-saves `Form.form` on commit.
- ⛔ **Create form items with the version-aware `modelFactory`, NOT the bare `FormFactory`.**
  `modelFactory.create(FormPackage.Literals.FORM_FIELD, form, version)` initialises mandatory
  defaults (`userVisible`, etc.); `FormFactory.eINSTANCE.createFormField()` does NOT → you get
  "обязательное свойство 'userVisible'" and similar markers. `version = projectManager.getProject(project).getVersion()`.
  (`FormFactory.eINSTANCE.createDataPath()` is fine for the data path — it has no such defaults.)
- ✅ **Unique item id** for every new `FormItem`: `FormIdentifierService.INSTANCE.getNextItemId(form)`.
- ✅ **Field must have** `type` (`ManagedFormFieldType.INPUT_FIELD`) and a matching `extInfo`
  created via the same factory: `modelFactory.create(FormPackage.Literals.INPUT_FIELD_EXT_INFO, version)`.
- ✅ **Bind to an existing data path**; the first segment is the **main attribute** name
  (`form.getAttributes()` where `isMain()` — usually `Объект`/`Object`), then the attribute name.
- ✅ **Add to** `form.getItems()` (or a `FormGroup`'s `getItems()`). `form.getGroup()` is a layout
  enum (`FormChildrenGroup`), NOT a container — do not call `.getItems()` on it.

### Worked example — add field `Комментарий` to Catalog.Номенклатура item form

Prerequisite: `Catalog.Номенклатура` already has attribute `Комментарий` (else run `edit_catalog`
first, in its own BM task).

```java
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.DataPath;
import com._1c.g5.v8.dt.form.model.ManagedFormFieldType;
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.form.model.FormPackage;
import com._1c.g5.v8.dt.form.model.FieldExtInfo;
import com._1c.g5.v8.dt.form.service.FormIdentifierService;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.platform.version.Version;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogForm;

IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
Version version = v8project.getVersion();
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String result = globalContext.execute(new AbstractBmTask<String>("Add form field") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Номенклатура");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Номенклатура");
        }
        // metadata attribute must already exist (create via edit_catalog otherwise)
        boolean attrExists = owner.getAttributes().stream()
            .anyMatch(a -> "Комментарий".equals(a.getName()));
        if (!attrExists) {
            throw new IllegalStateException(
                "Attribute Catalog.Номенклатура.Комментарий does not exist — create it with edit_catalog first");
        }

        CatalogForm catalogForm = null;
        for (CatalogForm f : owner.getForms()) {
            if ("ФормаЭлемента".equals(f.getName())) { catalogForm = f; break; }
        }
        if (catalogForm == null) {
            throw new IllegalStateException("Form ФормаЭлемента not found; create it with create_object_form");
        }
        Form form = (Form)catalogForm.getForm();
        if (form == null) {
            throw new IllegalStateException("Form structure not loaded; (re)create it with create_object_form");
        }

        // main attribute name (Объект/Object) → data path prefix
        String mainAttr = form.getAttributes().stream()
            .filter(FormAttribute::isMain).map(FormAttribute::getName).findFirst()
            .orElseThrow(() -> new IllegalStateException("Form has no main attribute"));

        // idempotency: do not add the same field twice
        boolean fieldExists = form.getItems().stream()
            .anyMatch(i -> i instanceof FormField && "Комментарий".equals(((FormField)i).getName()));
        if (fieldExists) {
            return "field already exists";
        }

        DataPath dataPath = FormFactory.eINSTANCE.createDataPath();
        dataPath.getSegments().add(mainAttr);
        dataPath.getSegments().add("Комментарий");

        // version-aware factory → mandatory defaults (userVisible, …) are initialised
        FormField field = modelFactory.create(FormPackage.Literals.FORM_FIELD, form, version);
        field.setId(FormIdentifierService.INSTANCE.getNextItemId(form));
        field.setName("Комментарий");
        field.setDataPath(dataPath);
        field.setType(ManagedFormFieldType.INPUT_FIELD);
        field.setExtInfo((FieldExtInfo)modelFactory.create(FormPackage.Literals.INPUT_FIELD_EXT_INFO, version));
        form.getItems().add(field);

        return "added field Комментарий";
    }
});
System.out.println(result);
return result;
```

> A `FormAttribute` (form-local data) is a different, advanced thing and is **not** how you show an
> object requisite — do not use it as a shortcut here. The visible control is the `FormField` above,
> bound to the object's metadata attribute via the main-attribute data path.

### Required post-check

Call `GetMarkers` (`marker_type:"1c"`) on the owner's `.mdo`
(`src/Catalogs/<OwnerName>/<OwnerName>.mdo`) and confirm a `<items xsi:type="form:FormField">` with
your `<dataPath>` segments appears in
`src/Catalogs/<OwnerName>/Forms/<FormName>/Form.form`. **Success = a FormField bound to the data
path exists** (and the metadata attribute exists). A run that added only a form attribute, or left
no FormField in `Form.form`, is **not** done — finish it. Fix only markers relevant to your change.
