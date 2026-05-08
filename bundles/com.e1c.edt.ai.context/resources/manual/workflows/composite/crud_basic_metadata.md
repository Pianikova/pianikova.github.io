# CRUD Bundle: Basic Metadata

Use this bundle for baseline CRUD of common 1C metadata objects. If only the listed setup is required, use manual templates and do not call reflection.

Mandatory validation rule: after every JShell create, update/edit, or delete of a 1C metadata object, the next tool call must be `GetMarkers` with `marker_type: "1c"` for the affected project. Do not report success and do not start the next CRUD operation until marker response is checked and non-empty markers are fixed.

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

Remove from the matching `Configuration` collection, detach by FQN, then run `GetMarkers`.
