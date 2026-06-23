# EDT Validation Traps

Check this file before writing JShell code that mutates metadata. These traps are based on observed logs and EDT model patterns.

## ⛔ Wrong transaction entry → object not saved to disk

Create/edit metadata ONLY through the global editing context:

```java
IBmModel bmModel = modelManager.getModel(project);
bmModel.getGlobalContext().execute(new AbstractBmTask<Void>("...") {
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) { /* ... */ return null; }
});
```

Only `getGlobalContext().execute(...)` auto-saves changed/attached objects to disk on commit.
Do **NOT** use `modelManager.executeReadWriteTask(project, new IBmSingleNamespaceTask<...>(){...})`
or other low-level transaction entries: they commit to the in-memory model only — the snippet
compiles, runs, prints "created successfully", and in-memory reads see the object, but **no `.mdo`
file is written** (it disappears on restart). Observed: a ChartOfAccounts "created" via
`executeReadWriteTask` left `src/ChartsOfAccounts/` empty. If `GetMarkers`/a folder check shows no
`.mdo` after a successful-looking create, you used the wrong entry — redo it with
`getGlobalContext().execute(...)`.

## Always run marker check

After create, edit, or delete, run `GetMarkers` with `marker_type: "1c"`. Treat non-empty 1C markers as incomplete work.

## Do not trust stale JShell session output

If a JShell response names a different metadata type or object than the current
request, stop using that `repl_session_id`. Create a fresh `jshellsession` and
rerun the current operation with explicit imports before making any more
metadata decisions.

Observed symptom: a catalog create request returned text about an existing
sequence from a previous snippet. This is not a validation result for the
catalog. It is a JShell session hygiene failure.

## Set UUIDs

Top-level metadata objects and child metadata objects need UUIDs. This includes attributes, tabular sections, enum values, dimensions, resources, commands, forms, and service child objects.

## Do not recreate existing top objects

For edit/delete, load existing top objects with `transaction.getTopObjectByFqn(...)`. Do not create a replacement object with the same name.

The same rule applies to retried create workflows. Before `transaction.attachTopObject(...)`, check the target FQN. If it already exists, a second attach will fail with `BmFqnAlreadyInUseException: FQN '<Type>.<Name>' is already in use`. Continue by editing/verifying the existing object or stop with a clear precondition failure.

## Configuration project creation

Create the Eclipse project structure before EDT services start reading project settings. At minimum, ensure project folders that EDT expects exist, especially `.settings`, `DT-INF`, and the metadata root. Prefer an EDT project creation/import API over raw file creation when available.

The observed log contains `NoSuchFileException` for a missing `.settings` folder during project initialization. It may be non-fatal, but it is a workflow smell and should be avoided.

## TypeDescription

Objects that implement type/value semantics usually need `TypeDescription`:
- `Constant`
- `CommonAttribute`
- `SessionParameter`
- `FunctionalOptionsParameter`
- `DefinedType`
- `ChartOfCharacteristicTypes`
- many attributes, dimensions, and resources

Use `TypeDescriptionBuilder` with `IEObjectProvider` and `IEObjectTypeNames`. Do not hand-build type XML or string names.

## Standard attributes are not custom attributes

Do not create child attributes whose names duplicate standard 1C attributes.
For catalogs, `Код` / `Code` and `Наименование` / `Description` are standard
fields of the catalog itself. Set their behavior on the top-level `Catalog`
with `setCodeLength(...)`, `setCodeType(...)` when needed, and
`setDescriptionLength(...)`.

Observed markers:

- `SU45: Некорректное значение свойства "name" реквизита "Код". Совпадает с именем стандартного реквизита`
- `SU45: Некорректное значение свойства "name" реквизита "Наименование". Совпадает с именем стандартного реквизита`

The fix is to remove the duplicate child attributes, not to rename standard
catalog fields in place. Apply the same caution to document standard fields
such as `Дата`, `Номер`, `Проведен`, `Ссылка`, and `ПометкаУдаления`.

## ExchangePlan

`ExchangePlan` scenarios that participate in exchange usually need `thisNode`. Use the `set_exchange_plan_thisnode` workflow.

## BusinessProcess and Task

Business process content is not valid as only a named top-level object. Create or resolve the related `Task`, then call `businessProcess.setTask(task)` in the same BM transaction. Missing task produces SU45 for property `task`: task is not selected.

## DocumentNumerator and document numbering

When a document has `Document.getNumerator() != null`, its numbering properties must match the assigned `DocumentNumerator`. Do not change only `Document.setNumberLength(...)` on one document that uses a shared numerator.

Observed marker:

`SU45: Некорректное значение свойства "numberLength". Указан нумератор. Свойства документа должны совпадать с соответствующими свойствами нумератора.`

Fix by choosing one consistent model:
- change the `DocumentNumerator` length/type and all documents that reference it in one BM workflow;
- or keep the document number length equal to the numerator;
- or remove/change the numerator reference intentionally, then validate all affected documents.

## Registers

Information, accumulation, accounting, and calculation registers often need child dimensions/resources and references to documents, charts, or registrar setup. Use enhanced register workflows for anything beyond minimal top-level CRUD.

If a register marker says that no document is a registrar, and the user asked
for a complete document workflow or stock/money movements, fix it through
`add_document_registers`. Do not stop with a manual instruction to edit
"ЗаписиРегистров"; load the `Document` and register in JShell and call
`document.getRegisterRecords().add(register)` in a BM transaction.

For accumulation registers, do not use `AccumulationRegisterAttribute` unless
reflection proves it exists in the installed EDT API. A real JShell run failed
with `cannot find symbol: class AccumulationRegisterAttribute`. Use
`AccumulationRegisterDimension` and `AccumulationRegisterResource` for the
baseline register structure.

## Forms and templates

Common forms, object forms, report forms, and templates have separate resource/model layers. Do not invent form controls, command bars, or template content from the top-level metadata object API alone.

## Services

HTTP services, web services, WS references, and integration services have safe top-level metadata create flows. Routes, operations, parameters, WSDL details, and channels are child APIs; verify them with one batch `JShellReflection`.

Observed baseline service markers:

- `HTTPService.rootURL` is required. Use `setRootURL("/api")`; the method name
  uses all-caps `URL`.
- `WebService.namespace` is required. Use
  `setNamespace("http://example.com/ws")`.
- `WSReference.locationURL` is required. Use
  `setLocationURL("http://example.com/service?wsdl")`. A placeholder endpoint
  can still produce a warning that no WSDL description is found; for a real
  integration prompt, use a real WSDL or explicitly explain the warning.
- `XDTOPackage.namespace` is required. Use
  `setNamespace("http://example.com/xdto")`.

For deleting any top-level service/XDTO object, use `delete_metadata_object` and
`IMdRefactoringService.createMdObjectDeleteRefactoring(...)`. Do not remove it
from a `Configuration` collection and call `transaction.detachTopObject(...)`.
