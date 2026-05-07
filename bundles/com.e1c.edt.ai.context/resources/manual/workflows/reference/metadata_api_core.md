# EDT Metadata API Cards: Core Objects

Use these cards before `JShellReflection`. If the requested operation is baseline create/read/edit/delete and uses only the listed factory, collection, and safe setters, do not call reflection just to re-check the API. If several unknown EDT types, methods, fields, or enum constants are needed, call `JShellReflection` once with the full `queries` array.

All top-level metadata objects follow the same BM pattern:

1. Open `IBmGlobalEditingContext.execute(...)`.
2. Create the object with `mdFactory`.
3. Set `name` and `uuid`.
4. Generate standalone FQN with `fqnGenerator.generateStandaloneObjectFqn(object.eClass(), object.getName())`.
5. `transaction.attachTopObject((IBmObject)object, fqn)`.
6. Add the object to the matching `Configuration` collection.
7. Run `GetMarkers` with `marker_type: "1c"`.

## Configuration

FQN: `Configuration`
Java type: `com._1c.g5.v8.dt.metadata.mdclass.Configuration`

Use `transaction.getTopObjectByFqn("Configuration")` to load it inside a BM transaction. Do not create a second `Configuration` object for an existing project.

## Subsystem

Factory: `mdFactory.createSubsystem()`
Collection: `configuration.getSubsystems()`
FQN prefix: `Subsystem`
Safe optional setters: none for baseline CRUD.
Composite operations: subsystem content is a separate scenario; use `resolve_top_object_and_parent_collection` or reflection only for unknown content APIs.

## CommonModule

Factory: `mdFactory.createCommonModule()`
Collection: `configuration.getCommonModules()`
FQN prefix: `CommonModule`
Safe optional setters:
- `setServer(true)`
- `setServerCall(true)`
- `setClientManagedApplication(true)` when client managed application context is required
- `setGlobal(true)` only when global module context is required

For reads, do not invent `getServer()`, `getServerCall()`, `getClient()`, or `getClientManagedForm()`. Boolean accessors may use `is...` names in the installed EDT API; if you need to print/read flags not listed here, call one batch `JShellReflection` for `CommonModule.*server*`, `CommonModule.*client*`, and `CommonModule.*global*` before writing getter calls.

Metadata flags and `Module.bsl` text are separate layers. Create or update `Module.bsl` through file/resource workflow after the metadata object exists.

## CommonForm

Factory: `mdFactory.createCommonForm()`
Collection: `configuration.getCommonForms()`
FQN prefix: `CommonForm`
Safe optional setters: none for baseline CRUD.

Do not invent form structure manually. Form content, controls, commands, and attributes belong to the form model/resource layer and need a dedicated workflow or one batch `JShellReflection`.

## CommonCommand

Factory: `mdFactory.createCommonCommand()`
Collection: `configuration.getCommonCommands()`
FQN prefix: `CommonCommand`
Safe optional setters:
- `setModifiesData(true|false)`

Command groups, parameter type, UI placement, and command handler modules are separate operations. Use `TypeDescriptionBuilder` only when the command parameter type is explicitly required.

## CommonPicture

Factory: `mdFactory.createCommonPicture()`
Collection: `configuration.getCommonPictures()`
FQN prefix: `CommonPicture`
Safe optional setters: none for baseline CRUD.

Binary picture content is configured separately after the metadata object exists.

## CommonTemplate

Factory: `mdFactory.createCommonTemplate()`
Collection: `configuration.getCommonTemplates()`
FQN prefix: `CommonTemplate`
Safe optional setters: none for baseline CRUD.

Template payload is a separate resource/content operation.

## CommonAttribute

Factory: `mdFactory.createCommonAttribute()`
Collection: `configuration.getCommonAttributes()`
FQN prefix: `CommonAttribute`
Required extra setup:
- assign `TypeDescription` with `TypeDescriptionBuilder`

Use `typedescription_best_practices` before writing code that sets the attribute type.

## Role

Factory: `mdFactory.createRole()`
Collection: `configuration.getRoles()`
FQN prefix: `Role`
Safe optional setters: none for baseline CRUD.

Rights are child/content data. Do not invent rights structure without a dedicated workflow or reflection.

## SessionParameter

Factory: `mdFactory.createSessionParameter()`
Collection: `configuration.getSessionParameters()`
FQN prefix: `SessionParameter`
Required extra setup:
- assign `TypeDescription`

## CommandGroup

Factory: `mdFactory.createCommandGroup()`
Collection: `configuration.getCommandGroups()`
FQN prefix: `CommandGroup`
Safe optional setters: none for baseline CRUD.

## FunctionalOption

Factory: `mdFactory.createFunctionalOption()`
Collection: `configuration.getFunctionalOptions()`
FQN prefix: `FunctionalOption`
Required extra setup:
- assign boolean `TypeDescription` or another valid option type when the scenario requires a value type.

## FunctionalOptionsParameter

Factory: `mdFactory.createFunctionalOptionsParameter()`
Collection: `configuration.getFunctionalOptionsParameters()`
FQN prefix: `FunctionalOptionsParameter`
Required extra setup:
- assign `TypeDescription`.

## Style

Factory: `mdFactory.createStyle()`
Collection: `configuration.getStyles()`
FQN prefix: `Style`
Safe optional setters: none for baseline CRUD.

## StyleItem

Factory: `mdFactory.createStyleItem()`
Collection: `configuration.getStyleItems()`
FQN prefix: `StyleItem`
Safe optional setters: none for baseline CRUD.

Style item details are specialized styling data; use reflection for unknown item features.

## DefinedType

Factory: `mdFactory.createDefinedType()`
Collection: `configuration.getDefinedTypes()`
FQN prefix: `DefinedType`
Required extra setup:
- assign `TypeDescription`.

## Interface

Factory: `mdFactory.createInterface()`
Collection: `configuration.getInterfaces()`
FQN prefix: `Interface`
Safe optional setters: none for baseline CRUD.

## Language

Factory: `mdFactory.createLanguage()`
Collection: `configuration.getLanguages()`
FQN prefix: `Language`
Safe optional setters: none for baseline CRUD.
