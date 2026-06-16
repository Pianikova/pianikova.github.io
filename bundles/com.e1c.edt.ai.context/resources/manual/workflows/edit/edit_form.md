## Safe Workflow: Edit an existing Form (add attributes / fields / groups)

Loads an existing form structure and adds items to it with `formFactory`. Use this after
`create_object_form`, or to modify any existing form.

### Hard rules — never violate

- ✅ **Load, do not recreate.** Resolve the form structure via its external-property FQN; never
  call `mdFactory.createCatalogForm()` for an existing form (causes `BmFqnAlreadyInUseException`).
- ✅ **Edit inside a BM transaction** (`globalContext.execute(AbstractBmTask)`).
- ⚠️ **`FormItem` ids and `dataPath` are non-trivial.** Adding a bare `FormField` without a valid
  data binding can produce 1C markers. Prefer regenerating the whole form via
  `create_object_form` when you need a full default layout; use `edit_form` for small, targeted
  additions, and always run `GetMarkers` afterwards.

### Resolving the existing form structure

The form structure (`com._1c.g5.v8.dt.form.model.Form`) is a separate top object attached under
the owner's `BASIC_FORM__FORM` external-property FQN. Resolve it by FQN:

```java
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.form.model.FormGroup;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogForm;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;

IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String result = globalContext.execute(new AbstractBmTask<String>("Edit form") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Products");
        }
        CatalogForm catalogForm = null;
        for (CatalogForm f : owner.getForms()) {
            if ("ФормаЭлемента".equals(f.getName())) { catalogForm = f; break; }
        }
        if (catalogForm == null) {
            throw new IllegalStateException("Form not found: Catalog.Products.Form.ФормаЭлемента");
        }

        // The Form (form.model) is reachable as the external property of the BasicForm
        Form form = (Form)catalogForm.getForm();
        if (form == null) {
            throw new IllegalStateException(
                "Form structure not loaded; (re)generate it with create_object_form first");
        }

        // Targeted addition: a new attribute
        FormAttribute attr = FormFactory.eINSTANCE.createFormAttribute();
        attr.setName("Комментарий");
        form.getAttributes().add(attr);

        return form.getMdForm() != null ? form.getMdForm().getName() : "form";
    }
});
System.out.println("Edited form: " + result);
return result;
```

`catalogForm.getForm()` returns the loaded `Form` (transient external property). If it is
`null`, the structure resource was not loaded into this transaction — generate it via
`create_object_form` instead of fabricating an empty one.

### Required post-check

Call `GetMarkers` with `marker_type: "1c"` on the owner's `.mdo`
(`src/Catalogs/<OwnerName>/<OwnerName>.mdo`) and confirm no new form-structure markers. Fix only
markers relevant to your change before reporting success.
