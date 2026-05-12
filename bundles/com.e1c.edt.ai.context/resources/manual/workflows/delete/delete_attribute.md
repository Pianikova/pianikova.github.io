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

After deleting an attribute or other child metadata object, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
