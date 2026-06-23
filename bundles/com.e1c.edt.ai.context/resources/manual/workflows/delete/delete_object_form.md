## Safe Workflow: Delete Object-Owned Form

Use this for requests like "delete/remove Catalog.Products.Form.ItemForm" or "удали форму элемента
справочника". This scenario deletes a form owned by a top-level metadata object (`CatalogForm`,
`DocumentForm`, ...). It is not a top-level metadata delete.

### Hard rules

- Do not use `Write`, `Delete`, or raw filesystem operations for `Form.form` or owner `.mdo`.
- Do not use `delete_metadata_object` / `IMdRefactoringService` for an object-owned form. The form is
  a child of the owner metadata object plus an external `BASIC_FORM__FORM` resource.
- Use exact owner and form names. If the user asked to delete a form for `Catalog.ТоварыLife02`,
  do not delete a form from `ТоварыLife01` or another similar object. If the exact owner is missing,
  stop with a clear message.
- Delete in a BM task: fetch owner, find the existing form child, detach the generated form
  module (`catalogForm.getForm().getModule()`) if present, detach the generated form structure
  (`catalogForm.getForm()`), clear owner default-form references that point to this form, remove
  the form from the owner collection, and delete the child object.
- If the form is already absent, print that result and stop. Deletion must be idempotent.
- After deletion, run `GetMarkers` with `marker_type:"1c"` on the owner `.mdo` and verify
  `src/<OwnerFolder>/<OwnerName>/Forms/<FormName>/Form.form` is gone. Also verify that
  `Module.bsl` and the form folder are gone or report them as leftovers; a successful object-form
  delete should not leave `Forms/<FormName>/Module.bsl` behind.

### Example - delete Catalog item form

```java
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.metadata.mdclass.AbstractForm;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogForm;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;

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

String result = globalContext.execute(new AbstractBmTask<String>("Delete catalog form") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.<OwnerName>");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.<OwnerName>");
        }

        CatalogForm target = null;
        for (CatalogForm form : owner.getForms()) {
            if ("<FormName>".equals(form.getName())) {
                target = form;
                break;
            }
        }
        if (target == null) {
            return "Form already absent: Catalog.<OwnerName>.Form.<FormName>";
        }

        if (owner.getDefaultObjectForm() == target) {
            owner.setDefaultObjectForm(null);
        }
        if (owner.getDefaultListForm() == target) {
            owner.setDefaultListForm(null);
        }
        if (owner.getDefaultFolderForm() == target) {
            owner.setDefaultFolderForm(null);
        }

        AbstractForm structure = target.getForm();
        if (structure != null) {
            Module module = structure.getModule();
            if (module != null) {
                structure.setModule(null);
                transaction.detachTopObject((IBmObject)module);
            }
            transaction.detachTopObject((IBmObject)structure);
        }

        owner.getForms().remove(target);
        EcoreUtil.delete(target, true);
        return "Deleted form: Catalog.<OwnerName>.Form.<FormName>";
    }
});
System.out.println(result);
}
```

### Required post-check

Run `GetMarkers` with `marker_type:"1c"` and `path` to the owner `.mdo`, for example:

```
src/Catalogs/<OwnerName>/<OwnerName>.mdo
```

Then verify the external form resources are absent:

```
src/Catalogs/<OwnerName>/Forms/<FormName>/Form.form
src/Catalogs/<OwnerName>/Forms/<FormName>/Module.bsl
```

Do not report success until both checks are complete.
