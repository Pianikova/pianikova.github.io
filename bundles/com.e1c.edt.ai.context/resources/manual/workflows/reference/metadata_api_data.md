# EDT Metadata API Cards: Data Objects

Use these cards before `JShellReflection`. Baseline top-level create/read/edit/delete needs no reflection when the factory, collection, and safe setters below are enough.

## Catalog

Factory: `mdFactory.createCatalog()`
Collection: `configuration.getCatalogs()`
FQN prefix: `Catalog`
Common safe setters:
- `setCodeLength(int)`
- `setDescriptionLength(int)`
- `setHierarchical(true|false)`

Attributes and tabular sections are child objects. Use `create_attribute_for_entity` and `add_tabular_section`.

## Document

Factory: `mdFactory.createDocument()`
Collection: `configuration.getDocuments()`
FQN prefix: `Document`
Common safe setters:
- `setAutonumbering(true|false)`
- `setNumberLength(int)`

Register records, journals, numerators, sequences, and posting settings are composite operations. Use the dedicated composite workflows instead of guessing.

## DocumentJournal

Factory: `mdFactory.createDocumentJournal()`
Collection: `configuration.getDocumentJournals()`
FQN prefix: `DocumentJournal`
Safe optional setters: none for baseline CRUD.
Composite operation: use `add_registered_documents_to_journal` to configure registered documents.

## DocumentNumerator

Factory: `mdFactory.createDocumentNumerator()`
Collection: `configuration.getDocumentNumerators()`
FQN prefix: `DocumentNumerator`
Safe optional setters: none for baseline CRUD.

Document links and numbering details are version-sensitive; use one batch reflection when configuring them.

## Sequence

Factory: `mdFactory.createSequence()`
Collection: `configuration.getSequences()`
FQN prefix: `Sequence`
Safe optional setters: none for baseline CRUD.

Sequence document membership is a composite operation; do not invent collection names.

## Enum

Factory: `mdFactory.createEnum()`
Collection: `configuration.getEnums()`
FQN prefix: `Enum`
Child values: use `mdFactory.createEnumValue()`, set `name`, set `uuid`, then add to `enum.getEnumValues()`.

Every enum value needs a UUID. Missing UUIDs often produce platform validation errors.

## Constant

Factory: `mdFactory.createConstant()`
Collection: `configuration.getConstants()`
FQN prefix: `Constant`
Required extra setup:
- assign `TypeDescription`.

## ChartOfCharacteristicTypes

Factory: `mdFactory.createChartOfCharacteristicTypes()`
Collection: `configuration.getChartsOfCharacteristicTypes()`
FQN prefix: `ChartOfCharacteristicTypes`
Common safe setters:
- `setCodeLength(int)`
- `setDescriptionLength(int)`
Required extra setup:
- assign value `TypeDescription`.

## ChartOfAccounts

Factory: `mdFactory.createChartOfAccounts()`
Collection: `configuration.getChartsOfAccounts()`
FQN prefix: `ChartOfAccounts`
Common safe setters:
- `setCodeLength(int)`
- `setDescriptionLength(int)`

Accounting registers commonly reference a chart of accounts. Create the chart before assigning it to a register.

## ChartOfCalculationTypes

Factory: `mdFactory.createChartOfCalculationTypes()`
Collection: `configuration.getChartsOfCalculationTypes()`
FQN prefix: `ChartOfCalculationTypes`
Common safe setters:
- `setCodeLength(int)`
- `setDescriptionLength(int)`

Calculation registers commonly depend on this object.

## Report

Factory: `mdFactory.createReport()`
Collection: `configuration.getReports()`
FQN prefix: `Report`
Safe optional setters: none for baseline CRUD.

Report forms, commands, layouts, and DCS content are separate operations.

## ExternalReport

`MdType.xcore` exposes `ExternalReportTypes`, but the current manual index does not provide a generic top-level create scenario for external reports. Use one batch `JShellReflection` to verify the installed EDT factory, parent collection, and required resource/content fields before creating or editing an external report.

## DataProcessor

Factory: `mdFactory.createDataProcessor()`
Collection: `configuration.getDataProcessors()`
FQN prefix: `DataProcessor`
Safe optional setters: none for baseline CRUD.

Processor forms, commands, layouts, and modules are separate operations.

## ExternalDataProcessor

`MdType.xcore` exposes `ExternalDataProcessorTypes`, but the current manual index does not provide a generic top-level create scenario for external data processors. Use one batch `JShellReflection` to verify the installed EDT factory, parent collection, and required resource/content fields before creating or editing an external data processor.

## ScheduledJob

Factory: `mdFactory.createScheduledJob()`
Collection: `configuration.getScheduledJobs()`
FQN prefix: `ScheduledJob`
Safe optional setters: none for baseline CRUD.

Schedule, method binding, and enabled/use flags are separate configuration details; use reflection for unknown fields.

## SettingsStorage

Factory: `mdFactory.createSettingsStorage()`
Collection: `configuration.getSettingsStorages()`
FQN prefix: `SettingsStorage`
Safe optional setters: none for baseline CRUD.

## FilterCriterion

Factory: `mdFactory.createFilterCriterion()`
Collection: `configuration.getFilterCriteria()`
FQN prefix: `FilterCriterion`
Required extra setup:
- assign `TypeDescription` when the criterion value type is required by validation.

## EventSubscription

Factory: `mdFactory.createEventSubscription()`
Collection: `configuration.getEventSubscriptions()`
FQN prefix: `EventSubscription`
Safe optional setters: none for baseline CRUD.

Event source, event name, handler module, and handler method need exact object references; use batch reflection or a dedicated workflow.
