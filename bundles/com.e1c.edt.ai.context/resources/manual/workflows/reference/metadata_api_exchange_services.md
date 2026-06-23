# EDT Metadata API Cards: Exchange And Services

Use this file for service-like and exchange-like top-level metadata. Baseline CRUD for listed top-level objects does not need reflection.

## ExchangePlan

Factory: `mdFactory.createExchangePlan()`
Collection: `configuration.getExchangePlans()`
FQN prefix: `ExchangePlan`
Common safe setters:
- `setAutonumbering(true|false)` when numbering is needed.

Validation trap: configure `thisNode` for scenarios that need a valid distributed infobase node. Use `set_exchange_plan_thisnode`; do not guess the node object shape.

## ExternalDataSource

Factory: `mdFactory.createExternalDataSource()`
Collection: `configuration.getExternalDataSources()`
FQN prefix: `ExternalDataSource`
Safe optional setters: none for baseline CRUD.

Tables and cubes are children of external data source; use `create_table` and `create_cube`.

## Table

Factory: `mdFactory.createTable()`
Parent collection: external data source tables, not `Configuration`.
Safe optional setters: none for baseline CRUD.

Use only inside an `ExternalDataSource` scenario.

## Cube

Factory: `mdFactory.createCube()`
Parent collection: external data source cubes, not `Configuration`.
Safe optional setters: none for baseline CRUD.

Use only inside an `ExternalDataSource` scenario.

## HTTPService

Factory: `mdFactory.createHTTPService()`
Collection: `configuration.getHttpServices()`
FQN prefix: `HTTPService`
Safe optional setters:
- `setRootURL("/api")`
Safe getters:
- `getRootURL()`

`rootURL` is required for a marker-clean baseline HTTP service. Missing or
empty values produce SU45. Method name is all-caps `URL`; do not use
`getRootUrl()`.

Methods, templates, route patterns, and handlers are child/service details. Use reflection when creating them.

## WebService

Factory: `mdFactory.createWebService()`
Collection: `configuration.getWebServices()`
FQN prefix: `WebService`
Safe optional setters:
- `setNamespace("http://example.com/ws")`
Safe getters:
- `getNamespace()`

Do not use `getNamespaceName()`.

Operations and parameters are child objects. Use reflection for exact operation APIs.

## WSReference

Factory: `mdFactory.createWSReference()`
Collection: `configuration.getWsReferences()`
FQN prefix: `WSReference`
Required baseline setters:
- `setLocationURL("<real user-provided WSDL URL>")`
Safe getters:
- `getLocationURL()`

`locationURL` is required to avoid the SU45 "URI is not specified" marker.
Do not use placeholder URLs for real tasks. If `GetMarkers` returns SU22
("не найдено wsdl описания"), the WSReference is incomplete: ask for a real
WSDL/import flow or update the object with the real URL before reporting
success. Imported service metadata and namespace details are not baseline CRUD.

## IntegrationService

Factory: `mdFactory.createIntegrationService()`
Collection: `configuration.getIntegrationServices()`
FQN prefix: `IntegrationService`
Safe optional setters: none for baseline CRUD.

Channels are child objects. Use `mdFactory.createIntegrationServiceChannel()` only after verifying the parent collection and required channel fields for the installed EDT version.

## XDTOPackage

Factory: `mdFactory.createXDTOPackage()`
Collection: `configuration.getXDTOPackages()`
FQN prefix: `XDTOPackage`
Required setters:
- `setNamespace("http://example.com/xdto")`
Safe getters:
- `getNamespace()`

Namespace is required for a valid baseline XDTO package. Missing namespace produces a 1C validation marker. Schema content, imports, object types, and value types are separate XDTO model operations.

## Delete

For every top-level object in this card, including services and XDTO packages,
use `delete_metadata_object` and
`IMdRefactoringService.createMdObjectDeleteRefactoring(...)`. Do not delete via
`configuration.getX().remove(...) + transaction.detachTopObject(...)`.
