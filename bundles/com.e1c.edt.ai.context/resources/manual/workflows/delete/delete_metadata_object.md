## Scenario: Delete Metadata Object

### Safe pattern
```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Delete object") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (catalog != null) {
            configuration.getCatalogs().remove(catalog);
            transaction.detachTopObject((IBmObject)catalog);
            System.out.println("Catalog deleted successfully");
        }
        return null;
    }
});
```

### Delete different object types

**Catalog:**
```java
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
if (catalog != null) {
    configuration.getCatalogs().remove(catalog);
    transaction.detachTopObject((IBmObject)catalog);
}
```

**Document:**
```java
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
if (document != null) {
    configuration.getDocuments().remove(document);
    transaction.detachTopObject((IBmObject)document);
}
```

**Accumulation Register:**
```java
AccumulationRegister register = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
if (register != null) {
    configuration.getAccumulationRegisters().remove(register);
    transaction.detachTopObject((IBmObject)register);
}
```

**Information Register:**
```java
InformationRegister register = (InformationRegister)transaction.getTopObjectByFqn("InformationRegister.Prices");
if (register != null) {
    configuration.getInformationRegisters().remove(register);
    transaction.detachTopObject((IBmObject)register);
}
```

**Enum:**
```java
com._1c.g5.v8.dt.metadata.mdclass.Enum enumObj = (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.Statuses");
if (enumObj != null) {
    configuration.getEnums().remove(enumObj);
    transaction.detachTopObject((IBmObject)enumObj);
}
```

**Subsystem:**
```java
Subsystem subsystem = (Subsystem)transaction.getTopObjectByFqn("Subsystem.MySubsystem");
if (subsystem != null) {
    configuration.getSubsystems().remove(subsystem);
    transaction.detachTopObject((IBmObject)subsystem);
}
```

### Rules
- ✅ Remove top-level objects from the correct `Configuration` collection first
- ✅ Then call `transaction.detachTopObject((IBmObject)object)`
- ❌ **NEVER** use `EcoreUtil.delete()` for top-level metadata objects (causes `UnsupportedOperationException`)
- ❌ **NEVER** skip `detachTopObject()` call (object will remain in transaction state)
- Check if object exists before attempting deletion to avoid NullPointerException
- For a multi-object delete, delete all requested objects in one BM task when the safe order is known, then run one project-wide `GetMarkers`.
- Do not rerun a successful delete transaction unless a verification read or marker check shows that an object still exists. A second run can hide the real result behind `NOT_FOUND` noise.

### Required post-check

After deleting metadata, call `GetMarkers` project-wide because references may break outside the deleted object's file:

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "max_count": 50
}
```

**Derive the dependent objects' `.mdo` paths directly from their FQNs — do not `Glob` to find them.** EDT removes the deleted object's own file as part of the delete, so do not target it with a per-path `GetMarkers` — markers will surface on the **dependent** files that referenced it. The dependent paths follow `<projectRoot>/src/<TypePluralFolder>/<Name>/<Name>.mdo`. Common cases:

| Dependent FQN prefix            | `.mdo` path                                          |
|---------------------------------|------------------------------------------------------|
| `Catalog.<Name>`                | `src/Catalogs/<Name>/<Name>.mdo`                     |
| `Document.<Name>`               | `src/Documents/<Name>/<Name>.mdo`                    |
| `InformationRegister.<Name>`    | `src/InformationRegisters/<Name>/<Name>.mdo`         |
| `AccumulationRegister.<Name>`   | `src/AccumulationRegisters/<Name>/<Name>.mdo`        |
| `CommonModule.<Name>`           | `src/CommonModules/<Name>/<Name>.mdo`                |
| `Subsystem.<Name>`              | `src/Subsystems/<Name>/<Name>.mdo`                   |

Copy `<Name>` exactly — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping. Project-wide markers remain the primary tool here because delete operations can break references the script cannot enumerate ahead of time.

### Important notes

**Deleting a catalog/document/register will cascade delete:**
- All attributes and tabular sections
- All forms and templates
- All modules (manager module, object module, etc.)
- All child objects

**Check before deletion:**
```java
String objectFqn = "Catalog.Products";
if (transaction.getTopObjectByFqn(objectFqn) != null) {
    // Object exists, safe to delete
} else {
    System.out.println("Object not found: " + objectFqn);
}
```

### Common mistakes

**❌ WRONG - Using EcoreUtil.delete() for top-level objects**
```java
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
EcoreUtil.delete(catalog); // ❌ UnsupportedOperationException!
```

**❌ WRONG - Forgetting to remove from collection**
```java
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
transaction.detachTopObject((IBmObject)catalog); // ❌ Object remains in configuration!
```

**❌ WRONG - Not checking if object exists**
```java
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
configuration.getCatalogs().remove(catalog); // ❌ NullPointerException if null!
```

**✅ CORRECT - Complete deletion pattern**
```java
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
if (catalog != null) {
    configuration.getCatalogs().remove(catalog);
    transaction.detachTopObject((IBmObject)catalog);
    System.out.println("Catalog deleted successfully");
}
```
