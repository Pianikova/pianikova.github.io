## Safe Workflow: Create Table inside ExternalDataSource

### Parent resolution
- Load the parent `ExternalDataSource` from transaction by FQN
- Create `Table` inside the same BM transaction
- Add it to `externalDataSource.getTables()`

### Example
```java
ExternalDataSource source = (ExternalDataSource)transaction.getTopObjectByFqn("ExternalDataSource.WarehouseDwh");
Table table = mdFactory.createTable();
table.setName("Products");
table.setUuid(UUID.randomUUID());
source.getTables().add(table);
```

### Notes
- Child objects inside ExternalDataSource are usually not attached as standalone top-level objects
- Add fields and commands after the table exists

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` — for tables inside an external data source, that is the **parent** `ExternalDataSource.<Name>`'s `.mdo` (a `Table` lives inside the parent and has no standalone top-level `.mdo` of its own).

**Derive the `.mdo` path directly from the parent FQN — do not `Glob` to find it.**
Schema for the parent:

```
<projectRoot>/src/ExternalDataSources/<Name>/<Name>.mdo
```

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn("ExternalDataSource.<Name>")` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
