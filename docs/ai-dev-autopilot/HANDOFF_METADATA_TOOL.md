# Handoff: declarative 1C metadata tool

Updated: 2026-07-23

## Goal and constraints

The original problem is that LLMs frequently invent EDT APIs or generate invalid Java/JShell code while manipulating 1C metadata. The former tools
`JShellSessionMcpTool`, `JShellManualMcpTool`, `JShellMcpTool`, and
`JShellReflectionMcpTool` are temporarily commented out in DI.

The chosen replacement is a declarative, guarded `IMcpTool` named
`1C_EditMetadata`. It accepts explicit operations and parameters, performs
metadata changes through EDT APIs, persists them, and returns exact resource
and marker paths. The main reference implementation is:

`D:\Downloads\MCP-RSV-Server-7.1.0-distr\src\com\radzivillovich\edt\rsv\tools\metadata\EditMetadataTool.java`

Related reference files:

- `EditMetadataCore.java`
- `EditMetadataHelp.java`
- `MdObjectOps.java`
- `D:\Downloads\MCP-RSV-Server-7.1.0-distr\src\com\radzivillovich\edt\rsv\metadata\MetadataTypeRegistry.java`

Important user requirement: do not complicate DevAutopilot preambles. A user
will not write them. All model guidance must live in the tool description,
operation specification, and returned paths. Do not add mandatory prompt
preambles to `docs/ai-dev-autopilot` or its manuals.

## Repository and runtime

- Repository: `C:\Projects\code-ai`
- Tool sources:
  `C:\Projects\code-ai\bundles\com.e1c.edt.ai.context\src\com\e1c\edt\ai\context\tools\metadata`
- Tests:
  `C:\Projects\code-ai\tests\com.e1c.edt.ai.tests\src\com\e1c\edt\ai\context\tools\metadata`
- DevAutopilot instructions:
  `C:\Projects\code-ai\docs\ai-dev-autopilot\README.md`
- Running EDT workspace:
  `D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev`
- Live test project: `Склад`
- DevAutopilot inbox:
  `D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev\inbox`
- DevAutopilot outbox:
  `D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev\outbox`

The application must be restarted after rebuilding the plugin. At the time of
this handoff, a restart after the latest 41-type implementation has not been
confirmed.

## Implemented API

The operation registry includes:

- `help`
- `inspectObject`
- `createObject`
- `setObjectProperty`
- `renameObject`
- `removeObject`
- `addObjectAttribute`
- `removeObjectAttribute`
- `addTabularSection`
- `removeTabularSection`
- `addTabularSectionAttribute`
- `removeTabularSectionAttribute`
- `addEnumValue`
- `removeEnumValue`
- `addRegisterField`
- `removeRegisterField`
- `setChildProperty`
- `setChildType`
- `renameChild`
- `addDocumentRegister`
- `removeDocumentRegister`
- `createObjectForm`
- `removeObjectForm`
- `createObjectTemplate`
- `removeObjectTemplate`

Request fields added during this work include:

- `childKind` / `child_kind`
- `relatedObjectName`
- `formType`
- `templateType`

Responses include:

- `details`
- `resource_path`
- `artifact_path`
- `marker_path`, which must equal the exact persisted resource path appropriate
  for `GetMarkers`

After a successful persisted mutation, the model should use the returned
`marker_path` directly. It must not perform redundant `Glob` or `Read` calls to
rediscover the resource.

## Full supported object registry

The RSV source of truth is `MetadataTypeRegistry.java`. It contains exactly 41
top-level metadata types, despite some RSV documentation referring to
approximately 39:

1. `Catalog`
2. `Document`
3. `InformationRegister`
4. `AccumulationRegister`
5. `AccountingRegister`
6. `CalculationRegister`
7. `ChartOfAccounts`
8. `ChartOfCharacteristicTypes`
9. `ChartOfCalculationTypes`
10. `BusinessProcess`
11. `Task`
12. `Subsystem`
13. `Role`
14. `CommonModule`
15. `CommonForm`
16. `CommonCommand`
17. `CommonAttribute`
18. `Constant`
19. `HTTPService`
20. `WebService`
21. `WSReference`
22. `XDTOPackage`
23. `Enum`
24. `Report`
25. `DataProcessor`
26. `ExchangePlan`
27. `FunctionalOption`
28. `FunctionalOptionsParameter`
29. `DefinedType`
30. `FilterCriterion`
31. `SessionParameter`
32. `EventSubscription`
33. `ScheduledJob`
34. `DocumentJournal`
35. `DocumentNumerator`
36. `Sequence`
37. `Style`
38. `StyleItem`
39. `Language`
40. `ExternalDataProcessor`
41. `ExternalReport`

## Latest implementation

### `MetadataObjectTypeRegistry.java`

New public registry containing all 41 descriptors:

- public type name;
- exact `Configuration` collection feature;
- physical resource folder;
- official EDT initializer class, when one exists;
- external-project flag.

The exact XDTO collection feature is `xDTOPackages`; the physical folder is
`XDTOPackages`.

`validateEdtModel()` verifies:

- each type has an `MdClassPackage` classifier;
- every internal type has its exact `Configuration` collection;
- mapped initializer classes can be loaded.

### `MetadataMutationService.java`

`createObject` now:

- resolves the type through the unified registry;
- uses the official EDT `IMdObjectInitializer` via its class name;
- falls back to the matching `MdClassFactory` `EClass` where there is no
  initializer;
- attaches internal objects to the BM and exact `Configuration` collection;
- handles external objects only in an external-object project;
- rejects internal objects in an external-object project and external objects
  in a normal configuration project;
- ensures the external project nature
  `com._1c.g5.v8.dt.core.V8ExternalObjectsNature`;
- attempts registration through reflective `addExternalObject`.

Additional behavior:

- `CommonForm` receives an actual empty form body using
  `FormFactory.eINSTANCE.createForm()`, with metadata/form links and external
  property attachment.
- Resource folders are resolved through the registry.
- Generic child creation first tries the owner's official EDT initializer
  `createChildObject`, then uses existing explicit fallbacks.
- Tabular-section child initialization uses the corresponding
  `<EClassName>Initializer`.
- Existing form generation uses `IFormGenerator`, `IFormFieldGenerator`, and
  `IEditingLanguageManager`.
- Spreadsheet templates use `SheetFactory`; DCS templates use `DcsFactory`.
- Physical artifact persistence/removal is checked.
- The persistence timeout has a minimum of 120 seconds because DCS deletion can
  take substantially longer than five seconds.

### Tool help

`EditMetadataMcpTool` supports:

```json
{"operation":"help","topic":"objectTypes"}
```

It must return all 41 descriptors, including `name`, `resource_folder`, and
`external_project`. The `createObject` help refers to this list rather than
duplicating a shorter type list.

## Tests added or updated

- New `MetadataObjectTypeRegistryTest.java`
  - asserts the exact ordered 41-type contract;
  - checks folders and external flags;
  - calls the production registry EDT-model validation.
- Updated `MetadataMutationServiceTest.java`
  - verifies irregular resource paths including:
    `ChartsOfAccounts`, `HTTPServices`, `XDTOPackages`, and `ExternalReports`.
- Existing tests retained:
  - `MetadataOperationRegistryTest`
  - `MetadataMutationServiceTest`
  - `DevAutopilotTest`

The model-validation test initially revealed an actual capitalization error.
Neither `xdtoPackages` nor `XDTOPackages` is the EMF feature name. Inspection of
`MdClass.xcore`/bytecode confirmed `xDTOPackages`.

## Verification already completed

Successful targeted clean verification:

```powershell
mvn clean "-Dtest=MetadataObjectTypeRegistryTest,MetadataOperationRegistryTest,MetadataMutationServiceTest" -DfailIfNoTests=false verify
```

Result:

- `BUILD SUCCESS`
- 15 tests
- 0 failures
- 0 errors

Successful complete compile/package gate without tests:

```powershell
mvn -DskipTests verify
```

`git diff --check` was clean apart from expected Windows LF/CRLF warnings.

An earlier non-clean targeted run hit a stale Tycho work-install version
conflict. That was an environment artifact, not a source failure; `mvn clean`
resolved it.

## Previous live validation

These live DevAutopilot outboxes exist:

- `960_extended_metadata_core.json`
- `961_artifact_correction.json`
- `962_cleanup.json`
- `963_dcs_create.json`
- `964_cleanup.json`
- `965_marker_path_retest.json`
- `966_dcs_terminal_delete.json`
- `967_dcs_terminal_create.json`
- `968_dcs_terminal_delete.json`

Results before the latest 41-type extension:

- Core document/register CRUD, tabular sections, properties, types, renames,
  register links, and inspection worked with zero tool errors.
- Object form and spreadsheet-template creation worked.
- Exact returned `marker_path` worked with `GetMarkers`.
- DCS report/template creation worked.
- DCS deletion originally timed out after five seconds but completed later.
  Raising the persistence wait minimum to 120 seconds fixed the terminal
  behavior.
- Run 967 created report `CodexПроверкаDCS` and DCS template
  `CodexОсновнаяСхема` with zero tool errors.
- Run 968 deleted the report with zero tool errors in approximately 15 seconds
  and without fallback calls.
- The directory
  `D:\Projects\_Eclipse\EDT_Plugin\Склад\src\Reports\CodexПроверкаDCS`
  was confirmed absent after cleanup.

These runs do not prove the newly added rare types. The latest registry,
initializer dispatch, external-object handling, and `CommonForm` body still
require live verification after restarting EDT.

## Exact next step

1. Confirm EDT was restarted after the latest build.
2. Run the following first:

   ```json
   {"operation":"help","topic":"objectTypes"}
   ```

   Verify the returned count and names are exactly the 41 types listed above.
3. Use new DevAutopilot requests, likely IDs 969 and above.
4. Test creation, inspection where useful, and deletion in manageable batches.
5. Use returned `marker_path` directly with `GetMarkers`; do not add a user
   preamble and do not rediscover paths with `Glob`/`Read`.
6. Inspect each outbox for `tool_error_count`, `has_tool_failures`, and the tool
   trace.
7. Always clean up every `CodexПроверка*` object, including after partial
   failures, and verify physical folders are absent.

Suggested high-yield live matrix:

- Batch A:
  `AccountingRegister`, `CalculationRegister`, `ChartOfAccounts`,
  `ChartOfCharacteristicTypes`, `ChartOfCalculationTypes`
- Batch B:
  `BusinessProcess`, `Task`, `ExchangePlan`, `DocumentJournal`,
  `DocumentNumerator`, `Sequence`
- Batch C:
  `CommonForm`, `CommonCommand`, `CommonAttribute`, `Role`,
  `SessionParameter`, `DefinedType`
- Batch D:
  `HTTPService`, `WebService`, `WSReference`, `XDTOPackage`,
  `EventSubscription`, `ScheduledJob`, `FunctionalOption`,
  `FunctionalOptionsParameter`, `FilterCriterion`, `Style`, `StyleItem`,
  `Language`

Some types need semantic references or properties before their metadata is
marker-clean. Examples include:

- `BusinessProcess` may require a `Task`;
- `AccountingRegister` may require a `ChartOfAccounts`;
- scheduled jobs and event subscriptions may require handlers/sources;
- `WSReference` requires a real WSDL URL.

Distinguish a successful CRUD/persistence result from expected EDT validation
markers caused by an intentionally incomplete business configuration.

`ExternalDataProcessor` and `ExternalReport` require a real external-object EDT
project. The normal `Склад` project should reject them clearly. Locate an
existing external project through project natures if available. Do not create a
new project or expand scope without user authorization.

## Live-risk checklist

If the first rare-type run fails, check these areas first:

- Initializer loading uses `Class.forName`. It passed the Tycho test runtime,
  but OSGi live class loading may still require the contributing bundle's class
  loader or direct imports.
- Reflective external registration through `addExternalObject` is not yet
  proven live.
- Physical `CommonForm` body persistence is not yet proven live.
- Generic child creation through initializer `createChildObject` is not yet
  proven live for newly supported owners.
- Do not claim all 41 types are fully live-supported until this matrix and
  cleanup pass.

## Current uncommitted files

At handoff time the relevant working-tree state was:

```text
 M bundles/com.e1c.edt.ai.context/src/com/e1c/edt/ai/context/tools/metadata/EditMetadataMcpTool.java
 M bundles/com.e1c.edt.ai.context/src/com/e1c/edt/ai/context/tools/metadata/MetadataMutationService.java
 M bundles/com.e1c.edt.ai.context/src/com/e1c/edt/ai/context/tools/metadata/MetadataOperationRegistry.java
 M tests/com.e1c.edt.ai.tests/src/com/e1c/edt/ai/context/tools/metadata/MetadataMutationServiceTest.java
?? bundles/com.e1c.edt.ai.context/src/com/e1c/edt/ai/context/tools/metadata/MetadataObjectTypeRegistry.java
?? tests/com.e1c.edt.ai.tests/src/com/e1c/edt/ai/context/tools/metadata/MetadataObjectTypeRegistryTest.java
?? docs/ai-dev-autopilot/HANDOFF_METADATA_TOOL.md
```

Do not discard unrelated user changes and do not use destructive Git commands.
Nothing from this work should be committed unless the user explicitly asks.
