## Check 1C Markers After Metadata CRUD

After any create, edit, or delete operation on 1C metadata objects, run the `GetMarkers` tool before considering the task complete. EDT validation errors often appear only after the BM transaction is applied and the project markers are refreshed.

### Required post-check

Use project-wide markers when the operation can affect references, generated objects, registrars, command interfaces, or multiple files:

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "max_count": 50
}
```

Use a file-scoped marker request when the changed metadata file is known and the operation is local:

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "path": "D:\\Projects\\_Eclipse\\EDT_Plugin\\MyProject\\src\\Catalogs\\TestCatalog\\TestCatalog.mdo"
}
```

### How to use the result

- Treat returned 1C markers as validation feedback for the CRUD operation.
- If markers point to objects just created or edited, fix them before reporting success.
- If markers are pre-existing and unrelated, mention that they remain and distinguish them from the current change.
- Prefer a file-scoped request after a narrow edit, then a project-wide request when the change touches references between metadata objects.

### Common marker causes after CRUD

- Missing `TypeDescription` on attributes, dimensions, resources, or other `BasicFeature` children.
- Missing UUID on newly created metadata objects or child elements.
- Invalid number qualifiers, such as scale greater than precision.
- Duplicate names or FQN conflicts.
- Registers missing document registrars.
- References to metadata objects that were renamed, deleted, or not yet created.
