## Check 1C Markers After Metadata CRUD

After any create, edit, or delete operation on 1C metadata objects, run the `GetMarkers` tool before considering the task complete. EDT validation markers often appear only after the BM transaction is applied and the project markers are refreshed. Inspect all relevant severities for the changed entity: errors, warnings, and infos. Do not check only errors.

Reading objects back with JShell, counting metadata collections, or printing
"EXISTS" for FQNs is useful diagnostic output, but it is not validation. A
CRUD operation is not complete until the relevant `GetMarkers` result was
checked after the transaction.

Default rule: validate the exact changed top-level entity first. If JShell changed `Catalog.Товары`, `Document.ПоступлениеТоваров`, or `AccumulationRegister.ОстаткиТоваров`, call `GetMarkers` with `path` to that entity's `.mdo` file and fix only markers relevant to that entity. Do not use project-wide marker output as a todo list for unrelated old problems.

### Required post-check

Use a file-scoped marker request when the changed metadata file is known or can be derived.

#### `.mdo` path layout — derive directly from FQN, do not guess

Every top-level metadata object lives in its own folder named after the
object, and the `.mdo` file inside that folder has the same name. The
folder under `src/` is the **plural-English** form of the metadata type,
not the FQN prefix used in `getTopObjectByFqn(...)`:

```
<projectRoot>/src/<TypePluralFolder>/<Name>/<Name>.mdo
```

Use the project's path separator as-is (`\\` on Windows, `/` on Linux/macOS).
Filenames and folder names are case-sensitive — copy the user-visible
`Name` exactly, including Cyrillic. The `.mdo` extension is lowercase.

| FQN prefix (in JShell)             | `src/` folder                         | Example `.mdo` path                                                           |
|------------------------------------|---------------------------------------|-------------------------------------------------------------------------------|
| `Configuration`                    | `Configuration`                       | `src/Configuration/Configuration.mdo`                                         |
| `Catalog.Name`                     | `Catalogs`                            | `src/Catalogs/Name/Name.mdo`                                                  |
| `Document.Name`                    | `Documents`                           | `src/Documents/Name/Name.mdo`                                                 |
| `Enum.Name`                        | `Enums`                               | `src/Enums/Name/Name.mdo`                                                     |
| `InformationRegister.Name`         | `InformationRegisters`                | `src/InformationRegisters/Name/Name.mdo`                                      |
| `AccumulationRegister.Name`        | `AccumulationRegisters`               | `src/AccumulationRegisters/Name/Name.mdo`                                     |
| `AccountingRegister.Name`          | `AccountingRegisters`                 | `src/AccountingRegisters/Name/Name.mdo`                                       |
| `CalculationRegister.Name`         | `CalculationRegisters`                | `src/CalculationRegisters/Name/Name.mdo`                                      |
| `ChartOfAccounts.Name`             | `ChartsOfAccounts`                    | `src/ChartsOfAccounts/Name/Name.mdo`                                          |
| `ChartOfCharacteristicTypes.Name`  | `ChartsOfCharacteristicTypes`         | `src/ChartsOfCharacteristicTypes/Name/Name.mdo`                               |
| `ChartOfCalculationTypes.Name`     | `ChartsOfCalculationTypes`            | `src/ChartsOfCalculationTypes/Name/Name.mdo`                                  |
| `BusinessProcess.Name`             | `BusinessProcesses`                   | `src/BusinessProcesses/Name/Name.mdo`                                         |
| `Task.Name`                        | `Tasks`                               | `src/Tasks/Name/Name.mdo`                                                     |
| `ExchangePlan.Name`                | `ExchangePlans`                       | `src/ExchangePlans/Name/Name.mdo`                                             |
| `CommonModule.Name`                | `CommonModules`                       | `src/CommonModules/Name/Name.mdo`                                             |
| `CommonAttribute.Name`             | `CommonAttributes`                    | `src/CommonAttributes/Name/Name.mdo`                                          |
| `Constant.Name`                    | `Constants`                           | `src/Constants/Name/Name.mdo`                                                 |
| `DataProcessor.Name`               | `DataProcessors`                      | `src/DataProcessors/Name/Name.mdo`                                            |
| `Report.Name`                      | `Reports`                             | `src/Reports/Name/Name.mdo`                                                   |
| `Role.Name`                        | `Roles`                               | `src/Roles/Name/Name.mdo`                                                     |
| `Subsystem.Name`                   | `Subsystems`                          | `src/Subsystems/Name/Name.mdo`                                                |
| `FilterCriterion.Name`             | `FilterCriteria`                      | `src/FilterCriteria/Name/Name.mdo`                                            |
| `Sequence.Name`                    | `Sequences`                           | `src/Sequences/Name/Name.mdo`                                                 |
| `DefinedType.Name`                 | `DefinedTypes`                        | `src/DefinedTypes/Name/Name.mdo`                                              |
| `SettingsStorage.Name`             | `SettingsStorages`                    | `src/SettingsStorages/Name/Name.mdo`                                          |
| `XDTOPackage.Name`                 | `XDTOPackages`                        | `src/XDTOPackages/Name/Name.mdo`                                              |
| `WebService.Name`                  | `WebServices`                         | `src/WebServices/Name/Name.mdo`                                               |
| `HTTPService.Name`                 | `HTTPServices`                        | `src/HTTPServices/Name/Name.mdo`                                              |
| `WSReference.Name`                 | `WSReferences`                        | `src/WSReferences/Name/Name.mdo`                                              |
| `IntegrationService.Name`         | `IntegrationServices`                 | `src/IntegrationServices/Name/Name.mdo`                                       |
| `DocumentJournal.Name`             | `DocumentJournals`                    | `src/DocumentJournals/Name/Name.mdo`                                          |
| `DocumentNumerator.Name`           | `DocumentNumerators`                  | `src/DocumentNumerators/Name/Name.mdo`                                        |
| `Style.Name`                       | `Styles`                              | `src/Styles/Name/Name.mdo`                                                    |
| `StyleItem.Name`                   | `StyleItems`                          | `src/StyleItems/Name/Name.mdo`                                                |
| `Language.Name`                    | `Languages`                           | `src/Languages/Name/Name.mdo`                                                 |
| `EventSubscription.Name`           | `EventSubscriptions`                  | `src/EventSubscriptions/Name/Name.mdo`                                        |
| `ScheduledJob.Name`                | `ScheduledJobs`                       | `src/ScheduledJobs/Name/Name.mdo`                                             |
| `FunctionalOption.Name`            | `FunctionalOptions`                   | `src/FunctionalOptions/Name/Name.mdo`                                         |
| `FunctionalOptionsParameter.Name`  | `FunctionalOptionsParameters`         | `src/FunctionalOptionsParameters/Name/Name.mdo`                               |
| `SessionParameter.Name`            | `SessionParameters`                   | `src/SessionParameters/Name/Name.mdo`                                         |
| `CommonForm.Name`                  | `CommonForms`                         | `src/CommonForms/Name/Name.mdo`                                               |
| `CommonCommand.Name`               | `CommonCommands`                      | `src/CommonCommands/Name/Name.mdo`                                            |
| `CommandGroup.Name`                | `CommandGroups`                       | `src/CommandGroups/Name/Name.mdo`                                             |
| `CommonTemplate.Name`              | `CommonTemplates`                     | `src/CommonTemplates/Name/Name.mdo`                                           |
| `CommonPicture.Name`               | `CommonPictures`                      | `src/CommonPictures/Name/Name.mdo`                                            |
| `ExternalDataSource.Name`          | `ExternalDataSources`                 | `src/ExternalDataSources/Name/Name.mdo`                                       |

If you are unsure which plural-folder name a metadata type uses, derive
it from `<projectRoot>/src/` listing (one-time `Glob` with pattern
`src/*/<Name>/<Name>.mdo`) instead of guessing. Common LLM mistakes:

- ❌ `src/Catalogs/Номенклатура.mdo` (missing intermediate folder)
- ❌ `src/Catalogs/Номенклатура/Номенклатура.mdO` (wrong case extension)
- ❌ `src/Catalog/Номенклатура/...` (singular folder)
- ❌ `src/Catalogs/Nomenclature/...` (transliteration of a Cyrillic name)
- ❌ `src/Catalogs/НоменклатураTypo/...` (any character drift from the
  exact `Name` used in `getTopObjectByFqn("Catalog.Name")`)

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "path": "D:\\Projects\\_Eclipse\\EDT_Plugin\\MyProject\\src\\Catalogs\\TestCatalog\\TestCatalog.mdo",
  "max_count": 200
}
```

Use project-wide markers only when the operation can affect references, generated objects, registrars, command interfaces, configuration-level state, or multiple files:

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "max_count": 200
}
```

### How to use the result

- Treat returned 1C markers as validation feedback for the CRUD operation.
- If markers point to objects just created, edited, deleted, renamed, or to objects affected by those changes, fix them before reporting success.
- If the call was project-wide, filter the result: fix only markers on the changed top-level entities and directly affected references. Do not fix unrelated project-wide markers unless the user asks for that broader cleanup.
- Warnings and infos are not automatically safe to ignore. If they are relevant to the changed entity/top object, fix them or explicitly explain why they are acceptable.
- If markers are pre-existing and unrelated, mention that they remain and distinguish them from the current change.
- Prefer one file-scoped request per changed `.mdo` after a narrow create/edit, then a project-wide request only when the change touches references between metadata objects.

### Common marker causes after CRUD

- Missing `TypeDescription` on attributes, dimensions, resources, or other `BasicFeature` children.
- Missing UUID on newly created metadata objects or child elements.
- Invalid number qualifiers, such as scale greater than precision.
- Duplicate names or FQN conflicts.
- Registers missing document registrars.
- References to metadata objects that were renamed, deleted, or not yet created.

### Post-check contract by scenario type

| Scenario type | Marker contract |
|---|---|
| Top-level create/edit | Run `GetMarkers` with `marker_type: "1c"` and `path` to the changed `.mdo` before reporting success. Any new relevant marker for the changed object means the operation is incomplete, regardless of severity. |
| Child creation (`BasicFeature`, attributes, dimensions, resources) | Treat missing `type` and missing UUID markers as blocking. Fix them in the same workflow. |
| Accumulation/Accounting/Calculation register create | If creating a complete workflow, SU45 registrar markers are blocking. Link a document through `Document.getRegisterRecords().add(register)`. |
| Register-only intermediate create | SU45 registrar markers are allowed only if the user explicitly asked to create the register for later linking. Report that registrar linking remains required. |
| Information register create | A registrar marker is not expected. Do not add `InformationRegister` to `Document.getRegisterRecords()`. |
| Delete or rename | Run project-wide markers because references can break outside the changed object's file, but fix only markers on directly affected references unless the user asked for wider cleanup. |

Do not summarize a CRUD operation as successful while relevant 1C markers remain.
