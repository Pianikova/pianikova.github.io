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

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` — for cubes inside an external data source, that is the **parent** `ExternalDataSource.<Name>`'s `.mdo` (a `Cube` lives inside the parent and has no standalone top-level `.mdo` of its own).

**Derive the `.mdo` path directly from the parent FQN — do not `Glob` to find it.**
Schema for the parent:

```
<projectRoot>/src/ExternalDataSources/<Name>/<Name>.mdo
```

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn("ExternalDataSource.<Name>")` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
