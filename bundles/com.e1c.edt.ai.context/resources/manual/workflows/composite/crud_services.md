# CRUD Bundle: Service Objects

Use this bundle for baseline CRUD of service metadata. Do not call `JShellReflection` for the listed top-level properties.

Mandatory validation rule: call `JShell` with `scope: "edt"`, `request_description`, and `response_description`. In `response_description`, name the changed top-level objects and known `.mdo` paths. After every JShell create or update/edit of a 1C metadata object, the next tool call must be `GetMarkers` with `marker_type: "1c"` and `path` to each changed top-level `.mdo` when known or derivable. Do not report success and do not start the next CRUD operation until all relevant markers for the changed entity/top object are checked and fixed or explicitly explained, including errors, warnings, and infos. Do not check only errors. Do not fix unrelated project-wide markers; use project-wide `GetMarkers` only for delete, rename, registrars, references, command interfaces, configuration-level changes, or when the `.mdo` path cannot be derived.

Objects:
- `HTTPService`
- `WebService`
- `IntegrationService`
- `WSReference`
- `XDTOPackage`

## Create

| Type | Factory | Collection | Safe setup |
| --- | --- | --- | --- |
| `HTTPService` | `mdFactory.createHTTPService()` | `configuration.getHttpServices()` | `setRootURL("/api")` |
| `WebService` | `mdFactory.createWebService()` | `configuration.getWebServices()` | `setNamespace("http://example.com/ws")` |
| `IntegrationService` | `mdFactory.createIntegrationService()` | `configuration.getIntegrationServices()` | none |
| `WSReference` | `mdFactory.createWSReference()` | `configuration.getWsReferences()` | none |
| `XDTOPackage` | `mdFactory.createXDTOPackage()` | `configuration.getXDTOPackages()` | required `setNamespace("http://example.com/xdto")` |

## Read

Use standalone FQN prefixes:
- `HTTPService.Name`
- `WebService.Name`
- `IntegrationService.Name`
- `WSReference.Name`
- `XDTOPackage.Name`

## Edit

Safe edits:
- `HTTPService`: `setRootURL(...)`
- `WebService`: `setNamespace(...)`
- `XDTOPackage`: `setNamespace(...)`
- `IntegrationService` and `WSReference`: rename/synonym only for baseline workflow

Exact getters:
- `HTTPService`: `getRootURL()`, not `getRootUrl()`
- `WebService`: `getNamespace()`, not `getNamespaceName()`
- `XDTOPackage`: `getNamespace()`

## Delete

Use `delete_metadata_object`: resolve the top-level `MdObject`, execute
`IMdRefactoringService.createMdObjectDeleteRefactoring(...)`, refresh the
project, then run project-wide `GetMarkers`. Do not remove from the
`Configuration` collection and call `detachTopObject(...)` manually for
top-level deletes.

## Reflection boundary

Use one batch `JShellReflection` for:
- HTTP route methods and URL templates
- web service operations and parameters
- integration service channels
- WSDL details
- XDTO schema content
