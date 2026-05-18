## Rename Metadata Object (Update FQN)

### ⚠️ `transaction.updateTopObjectFqn(...)` ALONE is NOT a full rename

It updates the BM index entry, but does **not**:

- delete the old `src/<TypePluralFolder>/<OldName>/<OldName>.mdo` file from disk,
- re-serialize `Configuration.mdo` so the catalog/document/... list references
  the new FQN instead of the old one.

Using `updateTopObjectFqn` on its own leaves the project in an inconsistent
state — both old and new `.mdo` files exist, `Configuration.mdo` still
lists the old FQN, and `GetMarkers` may report 0 markers because EDT
has not refreshed.

### Canonical path — `IMdRefactoringService.createMdObjectRenameRefactoring`

EDT exposes a refactoring service that handles the full workflow:
FQN update, on-disk file move, `Configuration.mdo` reference rewrites,
and updates to all `CatalogRef.X` / `DocumentRef.X` / etc. references in
other metadata objects. It is the same service EDT's UI calls when you
press F2 in the Project Navigator.

It is **not** a JShell binding — resolve it via OSGi inside your code.

```java
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import com._1c.g5.v8.dt.md.refactoring.core.IMdRefactoringService;
import com._1c.g5.v8.dt.refactoring.core.IRefactoring;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import java.util.Collection;

IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

// Step 1 — fetch the MdObject (Catalog/Document/Enum/...) from BM.
//   Use a read-only BM task to peek; the refactoring will open its own
//   write transaction internally.
Catalog oldCatalog = globalContext.execute(new AbstractBmTask<Catalog>("Lookup catalog to rename") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        return (Catalog)transaction.getTopObjectByFqn("Catalog.OldName");
    }
});
if (oldCatalog == null) {
    throw new IllegalStateException("Catalog.OldName not found — nothing to rename");
}

// Step 2 — resolve the OSGi service.
var bundle = FrameworkUtil.getBundle(IMdRefactoringService.class);
var ctx = bundle.getBundleContext();
ServiceReference<IMdRefactoringService> serviceRef =
    ctx.getServiceReference(IMdRefactoringService.class);
if (serviceRef == null) {
    throw new IllegalStateException("IMdRefactoringService not registered — falls back to manual updateTopObjectFqn path");
}
IMdRefactoringService refactoringService = ctx.getService(serviceRef);

try {
    // Step 3 — create the refactoring and execute every part.
    Collection<IRefactoring> refactorings =
        refactoringService.createMdObjectRenameRefactoring(oldCatalog, "NewName");
    for (IRefactoring r : refactorings) {
        r.perform();
    }
    System.out.println("Renamed Catalog.OldName -> Catalog.NewName via IMdRefactoringService");
} finally {
    ctx.ungetService(serviceRef);
}
```

What this does that `updateTopObjectFqn` alone does not:

1. Updates the FQN in the BM index (same as `updateTopObjectFqn`).
2. Moves `src/Catalogs/OldName/OldName.mdo` → `src/Catalogs/NewName/NewName.mdo`
   (and removes the old folder if empty).
3. Rewrites `Configuration.mdo` so `<catalogs>Catalog.NewName</catalogs>`
   appears in place of `Catalog.OldName`.
4. Walks every `CatalogRef.OldName` / `OldNameRef.OldName` reference
   across the configuration and rewrites them to the new FQN.
5. Handles adopted counterparts in extension projects.

The same `IMdRefactoringService` exposes `createMdObjectDeleteRefactoring(Collection<MdObject>)` for **full** delete with the same file-removal + reference-cleanup semantics — prefer it over manually removing from `configuration.getCatalogs()` / `detachTopObject(...)` for top-level deletes too.

### Fallback path (use only when `IMdRefactoringService` is unavailable)

If `getServiceReference(IMdRefactoringService.class)` returns `null` —
e.g. when the bundle `com._1c.g5.v8.dt.md.refactoring` is not loaded —
you can still attempt a partial rename, but report it as **incomplete**
to the user. Do not silently fall back. The partial recipe:

```java
globalContext.execute(new AbstractBmTask<Void>("Partial rename") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.OldName");
        if (catalog == null) return null;
        transaction.updateTopObjectFqn(catalog, "Catalog.NewName");
        catalog.setName("NewName");
        return null;
    }
});
// Old .mdo file and Configuration.mdo entries are NOT cleaned up here.
// You must tell the user that manual EDT-UI rename or a project rebuild
// is required to finish the operation.
```

### Required post-check

Always call `GetMarkers` project-wide after a rename — references across
unrelated objects may break, so a file-scoped check is insufficient.

**Derive the renamed object's `.mdo` path directly from its NEW FQN — do not `Glob` to find it.**
Schema: `<projectRoot>/src/<TypePluralFolder>/<NewName>/<NewName>.mdo`. The most common cases:

| FQN prefix (new)                | `.mdo` path                                          |
|---------------------------------|------------------------------------------------------|
| `Catalog.<NewName>`             | `src/Catalogs/<NewName>/<NewName>.mdo`               |
| `Document.<NewName>`            | `src/Documents/<NewName>/<NewName>.mdo`              |
| `Enum.<NewName>`                | `src/Enums/<NewName>/<NewName>.mdo`                  |
| `InformationRegister.<NewName>` | `src/InformationRegisters/<NewName>/<NewName>.mdo`   |
| `CommonModule.<NewName>`        | `src/CommonModules/<NewName>/<NewName>.mdo`          |
| `Subsystem.<NewName>`           | `src/Subsystems/<NewName>/<NewName>.mdo`             |

Copy `<NewName>` exactly — same case, same Cyrillic. Use the project's
path separator as-is (`\\` on Windows, `/` on Linux). Extension is
lowercase `.mdo`. See `check_1c_markers_after_crud` for the full
FQN → folder mapping.

After a successful rename you must also verify:

1. The old `.mdo` path does **not** exist on disk anymore.
2. `Configuration.mdo` lists `Catalog.<NewName>` (or whichever type),
   not the old FQN.
3. Every `*.Ref` reference to the old name has been rewritten.

If any of those three checks fails after `IRefactoring.perform()`, treat
the operation as incomplete — do not declare success.
