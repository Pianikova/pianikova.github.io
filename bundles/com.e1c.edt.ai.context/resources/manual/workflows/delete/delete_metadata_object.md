## Scenario: Delete Metadata Object

Top-level metadata deletion must use EDT refactoring, not a hand-written
`configuration.getX().remove(...) + transaction.detachTopObject(...)` BM task.

Manual detach can remove the `.mdo` file while other EDT services still hold
platform object URIs or dependent index objects. In the EDT/AI context sync this
shows up as repeated log errors like:

```text
Resource /<Project>/src/Catalogs/<Name>/<Name>.mdo does not exist
at ...PlatformObjectManager.doCreateResource(...)
at ...EntitiesWalker.walk(...)
```

### Canonical path — IMdRefactoringService

Use the same service EDT UI uses for delete. It removes the top object, rewrites
references, deletes files and dependent resources, and lets EDT refresh indexes.

```java
import java.util.Arrays;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.md.refactoring.core.IMdRefactoringService;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.refactoring.core.IRefactoring;

IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Catalog catalog = globalContext.execute(new AbstractBmTask<Catalog>("Lookup catalog to delete") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        return (Catalog)transaction.getTopObjectByFqn("Catalog.TempCatalog");
    }
});

if (catalog == null) {
    System.out.println("Catalog.TempCatalog already absent");
} else {
    var bundle = FrameworkUtil.getBundle(IMdRefactoringService.class);
    var ctx = bundle.getBundleContext();
    ServiceReference<IMdRefactoringService> serviceRef =
        ctx.getServiceReference(IMdRefactoringService.class);
    if (serviceRef == null) {
        throw new IllegalStateException("IMdRefactoringService is not registered; do not fall back to detachTopObject");
    }

    IMdRefactoringService refactoringService = ctx.getService(serviceRef);
    try {
        IRefactoring refactoring =
            refactoringService.createMdObjectDeleteRefactoring(Arrays.asList((MdObject)catalog));
        refactoring.perform();
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        System.out.println("Deleted Catalog.TempCatalog via IMdRefactoringService");
    } finally {
        ctx.ungetService(serviceRef);
    }
}
```

### Object lookup examples

Fetch the object in a short BM task, then perform refactoring outside that task.
The refactoring opens its own write operations internally.

```java
Document document = globalContext.execute(new AbstractBmTask<Document>("Lookup document to delete") {
    @Override
    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {
        return (Document)transaction.getTopObjectByFqn("Document.TempDocument");
    }
});
```

```java
InformationRegister register =
    globalContext.execute(new AbstractBmTask<InformationRegister>("Lookup register to delete") {
        @Override
        public InformationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
            return (InformationRegister)transaction.getTopObjectByFqn("InformationRegister.TempRegister");
        }
    });
```

```java
com._1c.g5.v8.dt.metadata.mdclass.Enum enumObject =
    globalContext.execute(new AbstractBmTask<com._1c.g5.v8.dt.metadata.mdclass.Enum>("Lookup enum to delete") {
        @Override
        public com._1c.g5.v8.dt.metadata.mdclass.Enum execute(IBmTransaction transaction, IProgressMonitor monitor) {
            return (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.TempEnum");
        }
    });
```

Pass the fetched object as `(MdObject)object` to
`createMdObjectDeleteRefactoring(Arrays.asList(...))`.

### Rules

- Use `IMdRefactoringService.createMdObjectDeleteRefactoring(...)` for
  top-level `MdObject` deletes: catalogs, documents, enums, registers,
  common modules, reports, data processors, subsystems, roles, and similar
  standalone metadata.
- Do not use `configuration.getX().remove(object)` plus
  `transaction.detachTopObject((IBmObject)object)` for top-level deletes in
  JShell CRUD scenarios. That can leave stale platform-object references for
  background context sync and index walkers.
- Do not use `EcoreUtil.delete(...)` for top-level metadata objects.
- It is safe for a delete to be idempotent: if `getTopObjectByFqn(...)` returns
  `null`, print that the object is already absent and stop.
- For child objects inside a parent top-level object, such as attributes or
  tabular sections, use the dedicated child workflow. Child deletion can use
  `EcoreUtil.delete(child)` and must validate the parent `.mdo`.

### Required post-check

After top-level deletion:

1. Refresh the Eclipse project with
   `project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor())`
   after `refactoring.perform()`.
2. Run `GetMarkers` project-wide because references may break outside the
   deleted object's own file.
3. Read back `transaction.getTopObjectByFqn("<Type>.<Name>")` and verify it is
   `null`.
4. Do not call `GetMarkers` with `path` to the deleted object's `.mdo`; the
   file is gone. If dependent markers exist, validate the dependent objects'
   `.mdo` files.

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "max_count": 50
}
```

### Testing note

Avoid adding artificial "create then delete a temporary top-level object" steps
to broad business prompts unless the goal is specifically to test delete
refactoring. Most real 1C developer tasks validate delete behavior by removing
an obsolete object that is known to be unused or by deleting a child attribute
from an existing object.
