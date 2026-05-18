# CRUD Bundle: Basic Metadata

Use this bundle for baseline CRUD of common 1C metadata objects. If only the listed setup is required, use manual templates and do not call reflection.

Mandatory validation rule: call `JShell` with `scope: "edt"`, `request_description`, and `response_description`. In `response_description`, name the changed top-level objects and known `.mdo` paths. After every JShell create or update/edit of a 1C metadata object, the next tool call must be `GetMarkers` with `marker_type: "1c"` and `path` to each changed top-level `.mdo` when known or derivable. Do not report success and do not start the next CRUD operation until all relevant markers for the changed entity/top object are checked and fixed or explicitly explained, including errors, warnings, and infos. Do not check only errors. Do not fix unrelated project-wide markers; use project-wide `GetMarkers` only for delete, rename, registrars, references, command interfaces, configuration-level changes, or when the `.mdo` path cannot be derived.

Do not narrow the user's CRUD request. If the user requested attributes with
`CatalogRef.*` / `EnumRef.*`, the JShell code must create those attributes with
reference TypeDescriptions or throw a blocking exception. Do not create "only
string attributes", omit requested reference attributes, or report partial
success unless the user explicitly changed the requirement.

Objects:
- `Catalog`
- `Document`
- `Enum`
- `Constant`
- `Subsystem`
- `Report`
- `DataProcessor`

## Create

| Type | Factory | Collection | Safe setup |
| --- | --- | --- | --- |
| `Catalog` | `mdFactory.createCatalog()` | `configuration.getCatalogs()` | `setCodeLength(9)`, `setDescriptionLength(100)` |
| `Document` | `mdFactory.createDocument()` | `configuration.getDocuments()` | `setAutonumbering(true)`, `setNumberLength(9)` |
| `Enum` | `mdFactory.createEnum()` | `configuration.getEnums()` | add enum values with UUIDs when requested |
| `Constant` | `mdFactory.createConstant()` | `configuration.getConstants()` | assign `TypeDescription` |
| `Subsystem` | `mdFactory.createSubsystem()` | `configuration.getSubsystems()` | none |
| `Report` | `mdFactory.createReport()` | `configuration.getReports()` | none |
| `DataProcessor` | `mdFactory.createDataProcessor()` | `configuration.getDataProcessors()` | none |

## Child objects

Attributes, tabular sections, enum values, dimensions, resources, forms, commands, and templates are child objects. Every child needs UUID unless the API explicitly creates one.

## Delete

Use `delete_metadata_object`: resolve the top-level `MdObject`, execute
`IMdRefactoringService.createMdObjectDeleteRefactoring(...)`, refresh the
project, then run project-wide `GetMarkers`. Do not remove from the
`Configuration` collection and call `detachTopObject(...)` manually for
top-level deletes.
