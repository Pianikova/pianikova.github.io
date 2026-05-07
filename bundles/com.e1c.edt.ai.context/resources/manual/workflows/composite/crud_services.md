# CRUD Bundle: Service Objects

Use this bundle for baseline CRUD of service metadata. Do not call `JShellReflection` for the listed top-level properties.

Mandatory validation rule: after every JShell create, update/edit, or delete of a 1C metadata object, the next tool call must be `GetMarkers` with `marker_type: "1c"` for the affected project. Do not report success and do not start the next CRUD operation until marker response is checked and non-empty markers are fixed.

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

Remove from the matching `Configuration` collection and detach by FQN. Then run `GetMarkers`.

## Reflection boundary

Use one batch `JShellReflection` for:
- HTTP route methods and URL templates
- web service operations and parameters
- integration service channels
- WSDL details
- XDTO schema content
