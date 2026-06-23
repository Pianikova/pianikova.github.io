## Safe Workflow: Create object form with generated structure

Use this for missing object-owned forms such as `Catalog.Номенклатура.Form.ФормаЭлемента`.
The result is complete only when both metadata and `Form.form` are produced and the generated
default form has received a small safe improvement pass. After the default layout exists, improve it
with `Edit` on the existing `Form.form` as a second step. The edit must touch user-facing
presentation: the form title, business field captions/titles, a safe business group, or the main
list/table presentation. Editing service controls such as `ListSettingsComposerUserSettings`,
search/status additions, command bars, context menus, or extended tooltips does **not** count as the
required improvement. If the generated form contains standard `Object.Code` and/or
`Object.Description` controls, at least one real `Edit` call is mandatory before reporting success.
Never use `Write` to create a `Form.form` file.

### Create/Edit/Delete policy for form resources

- Create or repair missing forms only through EDT API: create/reuse `CatalogForm`/`DocumentForm`/
  `InformationRegisterForm`/`AccumulationRegisterForm`/`AccountingRegisterForm`/
  `CalculationRegisterForm`,
  call `formGenerator.generateForm(...)`, set both links (`basicForm.setForm(form)` and
  `form.setMdForm(basicForm)`), and
  `transaction.attachTopObject(...BASIC_FORM__FORM...)`.
- Do not use `Write` for `.form` or owner `.mdo` files. Missing `Form.form` means this workflow did
  not finish; repair the EDT API path instead of writing XML.
- After `Form.form` exists, small layout improvements (visibility, title/caption text, local flags,
  simple ordering/grouping changes that can be expressed as exact XML moves/replacements) should use
  the `edit_form` route: `SearchFiles`/`Read` -> `Edit` existing `Form.form` -> `GetMarkers`.
- A newly generated default form must not be reported as fully done until this safe improvement pass
  has been applied. At minimum, read the generated `Form.form` and improve obvious machine defaults.
  For Russian metadata with `Object.Code` / `Object.Description`, do at least one exact `Edit`, such
  as adding a Russian form title, adding a group title `Основные данные`, or adding/replacing
  user-facing titles/captions with `Код` / `Наименование` where the corresponding XML nodes exist.
  Only if the generated file has no safe editable presentation fragments at all may you keep it
  unchanged, and then you must explicitly report that no safe exact edit was available.
- Improve business UI, not service UI. For list forms, prefer a readable form title or user-facing
  table/field captions. Do not edit `ListSettingsComposerUserSettings`, `ListSearchString`,
  `ListSearchControl`, `ListViewStatus`, `FormCommandBar`, `ListCommandBar`, `ContextMenu`, or
  `ExtendedTooltip` merely to satisfy the improvement requirement.
- Prefer the simple style used by real SSL catalog item forms: `Object.Description` is presented as
  `Наименование`, `Object.Code` as `Код`, and both may be placed into a compact `form:FormGroup`
  (`type` = `UsualGroup`, `extInfo/group` = `HorizontalIfPossible`) named
  `ГруппаНаименованиеКод` or `ГруппаНаименование`. Business attributes, if they already exist on
  the owner, stay below in a readable order. Do not invent attributes just to enrich the form. A
  form-title-only edit is not enough when Code/Description controls are present and can be safely
  improved.
- If the user asks "why does the form look bad/raw?" or "make the default form nicer", explain that
  the form is the safe EDT-generated default and offer or perform concrete `Edit` refinements on the
  existing `Form.form`. Do not claim the form is broken only because it is default-generated.
- Delete an object-owned form with `delete_object_form`, not file deletion and not top-level
  metadata refactoring.

### Hard rules

- Do not create only `CatalogForm`/`DocumentForm` metadata. Always call `formGenerator.generateForm`
  and attach the generated `Form` as `BASIC_FORM__FORM`.
- Do not stop immediately after `formGenerator` succeeds. Read the generated `Form.form`, perform
  safe exact `Edit` refinements for obvious default presentation issues, then run `GetMarkers`.
  A create-catalog-and-form request like "Создай справочник товаров и форму для него" is incomplete
  if the final form is still the untouched EDT default.
- Do not create `Form.form` with `Write`. If the form resource is missing, create or repair it
  through this EDT workflow so the form is registered in the BM model.
- If the form metadata already exists but `catalogForm.getForm()` is null or `Form.form` is missing,
  reuse that existing form metadata and attach a generated structure. Do not create a duplicate form.
- When repairing an existing metadata-only form, `catalogForm.setForm(form)` is mandatory before
  `form.setMdForm(catalogForm)`. Without the forward link, EDT can persist the owner `.mdo`
  reference while no external `Form.form` file is written.
- In this EDT build, `generateForm(...)` has 8 arguments and ends with `Integer columnCount`.
  Do not pass a ninth `null` argument.
- For `FormType.OBJECT`, `FOLDER`, `CONSTANTS`, `RECORD`, and `REPORT`, pass non-null
  `Integer.valueOf(1)` as `columnCount`.
- `InformationRegister` may use `FormType.RECORD` for record forms or `FormType.LIST` for list
  forms. `AccumulationRegister`, `AccountingRegister`, and `CalculationRegister` must use
  `FormType.LIST` in this workflow and must set `defaultListForm`/`auxiliaryListForm`; never call
  `setDefaultRecordForm(...)` for them. If `FormType.RECORD` is used on these registers, EDT throws
  `There is no feature with type 'MdRecordManagerType'`.
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

        catalogForm.setForm(form);
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

Run `GetMarkers` on the owner's `.mdo`, confirm
`src/Catalogs/<OwnerName>/Forms/<FormName>/Form.form` exists, and confirm the safe improvement pass
changed the existing `Form.form` through `Edit` unless no safe exact edit was available.
