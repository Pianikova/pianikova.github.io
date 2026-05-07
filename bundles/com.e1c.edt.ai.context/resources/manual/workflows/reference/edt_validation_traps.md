# EDT Validation Traps

Check this file before writing JShell code that mutates metadata. These traps are based on observed logs and EDT model patterns.

## Always run marker check

After create, edit, or delete, run `GetMarkers` with `marker_type: "1c"`. Treat non-empty 1C markers as incomplete work.

## Set UUIDs

Top-level metadata objects and child metadata objects need UUIDs. This includes attributes, tabular sections, enum values, dimensions, resources, commands, forms, and service child objects.

## Do not recreate existing top objects

For edit/delete, load existing top objects with `transaction.getTopObjectByFqn(...)`. Do not create a replacement object with the same name.

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

## ExchangePlan

`ExchangePlan` scenarios that participate in exchange usually need `thisNode`. Use the `set_exchange_plan_thisnode` workflow.

## BusinessProcess and Task

Business process content is not valid as only a named top-level object. Create or resolve the related `Task`, then call `businessProcess.setTask(task)` in the same BM transaction. Missing task produces SU45 for property `task`: task is not selected.

## Registers

Information, accumulation, accounting, and calculation registers often need child dimensions/resources and references to documents, charts, or registrar setup. Use enhanced register workflows for anything beyond minimal top-level CRUD.

## Forms and templates

Common forms, object forms, report forms, and templates have separate resource/model layers. Do not invent form controls, command bars, or template content from the top-level metadata object API alone.

## Services

HTTP services, web services, WS references, and integration services have safe top-level metadata create flows. Routes, operations, parameters, WSDL details, and channels are child APIs; verify them with one batch `JShellReflection`.
