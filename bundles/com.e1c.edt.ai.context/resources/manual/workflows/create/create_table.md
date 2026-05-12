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

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
