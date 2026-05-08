# EDT Metadata API Cards: Processes And Misc

Use these cards to avoid reflection for baseline top-level CRUD. Call one batch `JShellReflection` for unknown child objects, enum constants, or version-dependent fields.

## BusinessProcess

Factory: `mdFactory.createBusinessProcess()`
Collection: `configuration.getBusinessProcesses()`
FQN prefix: `BusinessProcess`
Common safe setters:
- `setAutonumbering(true|false)`

Validation trap: practical business process scenarios often require a related `Task` and route points. Create `Task` first or use a dedicated workflow before configuring route map details.

Required for valid baseline create: `BusinessProcess.getTask()` must be non-null. Create and attach a `Task` in the same transaction, then call `businessProcess.setTask(task)`. Missing task produces SU45: property `task` is not selected.

## Task

Factory: `mdFactory.createTask()`
Collection: `configuration.getTasks()`
FQN prefix: `Task`
Common safe setters:
- `setAutonumbering(true|false)` when task numbering is required.

Task/business process links are composite details.

## Bot

Factory: `mdFactory.createBot()`
Collection: `configuration.getBots()`
FQN prefix: `Bot`
Safe optional setters: none for baseline CRUD.

Conversation scenarios, commands, channels, and integration endpoints are separate operations.

## DataArea / ConfigurationArea

The installed `MdType.xcore` inspected under `C:\Projects\dt` does not expose `DataAreaTypes` or `ConfigurationAreaTypes`. If the current EDT installation has `DataArea` or `ConfigurationArea` in `MdClassFactory`, use `JShellReflection` once to verify:
- factory method
- configuration collection
- required fields

Do not invent `createDataArea()`, `createConfigurationArea()`, `getDataAreas()`, or `getConfigurationAreas()` without reflection in this version.

## MonitoringCenter

The inspected `MdType.xcore` does not expose a `MonitoringCenterTypes` class. Do not attempt creation unless a prior batch `JShellReflection` returns the exact `mdFactory.create...` method, configuration collection, and required fields for the installed EDT version.

## RouteMap

The inspected `MdType.xcore` does not expose a top-level `RouteMapTypes` class. Route maps may be tied to business process route content. Do not attempt top-level creation unless a prior batch `JShellReflection` returns the exact factory and parent collection.

## DisplayPackage

The inspected `MdType.xcore` does not expose a `DisplayPackageTypes` or `PresentationPackageTypes` class. Do not attempt creation unless a prior batch `JShellReflection` returns the exact factory, collection, and required fields.
