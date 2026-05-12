# CRUD Bundle: Common Objects

Use this bundle when asked to create, read, update, and delete common metadata objects. Baseline CRUD for these objects should use `JShellManual` only; do not call `JShellReflection` unless the request includes form internals, module text APIs, command parameter types, or command placement.

Mandatory validation rule: call `JShell` with `scope: "edt"`, `request_description`, and `response_description`. In `response_description`, name the changed top-level objects and known `.mdo` paths. After every JShell create or update/edit of a 1C metadata object, the next tool call must be `GetMarkers` with `marker_type: "1c"` and `path` to each changed top-level `.mdo` when known or derivable. Do not report success and do not start the next CRUD operation until all relevant markers for the changed entity/top object are checked and fixed or explicitly explained, including errors, warnings, and infos. Do not check only errors. Do not fix unrelated project-wide markers; use project-wide `GetMarkers` only for delete, rename, registrars, references, command interfaces, configuration-level changes, or when the `.mdo` path cannot be derived.

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
