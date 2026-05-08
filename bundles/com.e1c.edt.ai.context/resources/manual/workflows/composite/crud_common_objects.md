# CRUD Bundle: Common Objects

Use this bundle when asked to create, read, update, and delete common metadata objects. Baseline CRUD for these objects should use `JShellManual` only; do not call `JShellReflection` unless the request includes form internals, module text APIs, command parameter types, or command placement.

Mandatory validation rule: after every JShell create, update/edit, or delete of a 1C metadata object, the next tool call must be `GetMarkers` with `marker_type: "1c"` for the affected project. Do not report success and do not start the next CRUD operation until marker response is checked and non-empty markers are fixed.

Objects:
- `CommonModule`
- `CommonForm`
- `CommonCommand`

## Create

Use `top-level-create` with:

| Type | Factory | Collection | Safe setup |
| --- | --- | --- | --- |
| `CommonModule` | `mdFactory.createCommonModule()` | `configuration.getCommonModules()` | `setServer(true)`, `setServerCall(true)` |
| `CommonForm` | `mdFactory.createCommonForm()` | `configuration.getCommonForms()` | none |
| `CommonCommand` | `mdFactory.createCommonCommand()` | `configuration.getCommonCommands()` | `setModifiesData(false)` |

## Read

Inside a BM transaction, load by standalone FQN:

```java
CommonModule module = (CommonModule)transaction.getTopObjectByFqn("CommonModule.CommonModuleSample");
CommonForm form = (CommonForm)transaction.getTopObjectByFqn("CommonForm.UniversalSearchForm");
CommonCommand command = (CommonCommand)transaction.getTopObjectByFqn("CommonCommand.OpenDashboard");
```

Alternatively, scan the matching `Configuration` collection by `getName()`.

## Edit

Edit the existing object only. Do not reattach it.

Safe edits:
- `CommonModule`: `setServer(...)`, `setServerCall(...)`, `setClientManagedApplication(...)`, `setGlobal(...)`
- `CommonCommand`: `setModifiesData(...)`
- `CommonForm`: rename/synonym only unless a form workflow is provided

Safe CommonModule read rule: print `getName()`, `getSynonym()`, and other proven metadata first. Do not guess boolean getters such as `getServer()` or `getClientManagedForm()`; if flag values must be read, use one `JShellReflection` call with all `CommonModule.*server*`, `CommonModule.*client*`, and `CommonModule.*global*` queries.

## Delete

Remove from the matching `Configuration` collection and detach the top object by FQN. Then run `GetMarkers`.
