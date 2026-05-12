## Check 1C Markers After Metadata CRUD

After any create, edit, or delete operation on 1C metadata objects, run the `GetMarkers` tool before considering the task complete. EDT validation markers often appear only after the BM transaction is applied and the project markers are refreshed. Inspect all relevant severities for the changed entity: errors, warnings, and infos. Do not check only errors.

Default rule: validate the exact changed top-level entity first. If JShell changed `Catalog.Товары`, `Document.ПоступлениеТоваров`, or `AccumulationRegister.ОстаткиТоваров`, call `GetMarkers` with `path` to that entity's `.mdo` file and fix only markers relevant to that entity. Do not use project-wide marker output as a todo list for unrelated old problems.

### Required post-check

Use a file-scoped marker request when the changed metadata file is known or can be derived:

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "path": "D:\\Projects\\_Eclipse\\EDT_Plugin\\MyProject\\src\\Catalogs\\TestCatalog\\TestCatalog.mdo",
  "max_count": 200
}
```

Use project-wide markers only when the operation can affect references, generated objects, registrars, command interfaces, configuration-level state, or multiple files:

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "max_count": 200
}
```

### How to use the result

- Treat returned 1C markers as validation feedback for the CRUD operation.
- If markers point to objects just created, edited, deleted, renamed, or to objects affected by those changes, fix them before reporting success.
- If the call was project-wide, filter the result: fix only markers on the changed top-level entities and directly affected references. Do not fix unrelated project-wide markers unless the user asks for that broader cleanup.
- Warnings and infos are not automatically safe to ignore. If they are relevant to the changed entity/top object, fix them or explicitly explain why they are acceptable.
- If markers are pre-existing and unrelated, mention that they remain and distinguish them from the current change.
- Prefer one file-scoped request per changed `.mdo` after a narrow create/edit, then a project-wide request only when the change touches references between metadata objects.

### Common marker causes after CRUD

- Missing `TypeDescription` on attributes, dimensions, resources, or other `BasicFeature` children.
- Missing UUID on newly created metadata objects or child elements.
- Invalid number qualifiers, such as scale greater than precision.
- Duplicate names or FQN conflicts.
- Registers missing document registrars.
- References to metadata objects that were renamed, deleted, or not yet created.

### Post-check contract by scenario type

| Scenario type | Marker contract |
|---|---|
| Top-level create/edit | Run `GetMarkers` with `marker_type: "1c"` and `path` to the changed `.mdo` before reporting success. Any new relevant marker for the changed object means the operation is incomplete, regardless of severity. |
| Child creation (`BasicFeature`, attributes, dimensions, resources) | Treat missing `type` and missing UUID markers as blocking. Fix them in the same workflow. |
| Accumulation/Accounting/Calculation register create | If creating a complete workflow, SU45 registrar markers are blocking. Link a document through `Document.getRegisterRecords().add(register)`. |
| Register-only intermediate create | SU45 registrar markers are allowed only if the user explicitly asked to create the register for later linking. Report that registrar linking remains required. |
| Information register create | A registrar marker is not expected. Do not add `InformationRegister` to `Document.getRegisterRecords()`. |
| Delete or rename | Run project-wide markers because references can break outside the changed object's file, but fix only markers on directly affected references unless the user asked for wider cleanup. |

Do not summarize a CRUD operation as successful while relevant 1C markers remain.
