## Delete Attribute from Catalog

```java
Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Delete attribute") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Товары");

        if (catalog != null) {
            // Find attribute by name
            CatalogAttribute attrToRemove = null;
            for (CatalogAttribute attr : catalog.getAttributes()) {
                if ("ПолноеНаименование".equals(attr.getName())) {
                    attrToRemove = attr;
                    break;
                }
            }

            // ⚠️ WARNING: simple remove() may not work correctly!
            // Use EcoreUtil.delete() instead
            if (attrToRemove != null) {
                EcoreUtil.delete(attrToRemove);
            }
        }
        return catalog;
    }
});
```
**NOTE:** Always use `EcoreUtil.delete()` instead of `getAttributes().remove()` for proper entity deletion.

### Required post-check

After deleting an attribute or other child metadata object, call `GetMarkers` with `marker_type: "1c"` and `path` to the **parent** top-level object's `.mdo` (attributes have no `.mdo` of their own). Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN of the parent — do not `Glob` to find it.**
Schema: `<projectRoot>/src/<TypePluralFolder>/<ParentName>/<ParentName>.mdo`. Common cases:

| Parent FQN                     | `.mdo` path                                       |
|--------------------------------|---------------------------------------------------|
| `Catalog.<Name>`               | `src/Catalogs/<Name>/<Name>.mdo`                  |
| `Document.<Name>`              | `src/Documents/<Name>/<Name>.mdo`                 |
| `InformationRegister.<Name>`   | `src/InformationRegisters/<Name>/<Name>.mdo`      |
| `AccumulationRegister.<Name>`  | `src/AccumulationRegisters/<Name>/<Name>.mdo`     |

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn(...)` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the deleted attribute was referenced from outside its parent (e.g. via DCS, query language, common modules) or when the path truly cannot be derived.
