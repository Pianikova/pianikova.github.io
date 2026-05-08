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

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project and fix new validation markers before reporting success.
