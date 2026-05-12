## Safe Workflow: Create Cube inside ExternalDataSource

### Parent resolution
- Load the parent `ExternalDataSource` from transaction by FQN
- Create `Cube` inside the same BM transaction
- Add it to `externalDataSource.getCubes()`

### Example
```java
ExternalDataSource source = (ExternalDataSource)transaction.getTopObjectByFqn("ExternalDataSource.WarehouseDwh");
Cube cube = mdFactory.createCube();
cube.setName("SalesCube");
cube.setUuid(UUID.randomUUID());
source.getCubes().add(cube);
```

### Notes
- Add dimensions, resources, functions, and dimension tables after cube creation
- Keep TypeDescription on child objects explicit to avoid validation noise

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
