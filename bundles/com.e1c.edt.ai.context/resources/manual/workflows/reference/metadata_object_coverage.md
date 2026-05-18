## Metadata Object Coverage

Use this page as the maintenance map for choosing a `JShellManual` scenario by
1C metadata object kind. Most top-level metadata objects use the shared
`top-level-create` template from `_templates/top-level-create.md`; object-specific
guides are kept only where the workflow needs special rules.

### Covered by Specific Guides

| 1C object kind | Scenario |
|---|---|
| Configuration project | `create_configuration_project` |
| Catalogs | `create_catalog` |
| Documents | `create_document` |
| Information registers | `create_information_register` |
| Accumulation registers | `create_accumulation_register` |
| Accounting registers | `create_accounting_register` |
| Calculation registers | `create_calculation_register` |
| Enumerations | `create_enum` |
| Subsystems | `create_subsystem` |
| External data source cubes | `create_cube` |
| External data source tables | `create_table` |
| Type descriptions | `create_type_description` |

### Covered As Child Or Nested Workflows

These object kinds appear in `MdType.xcore` as dynamic type families, but they
are not simple configuration top-level objects. Use the parent workflow instead
of adding a guessed top-level template.

| 1C object kind / MdType family | Parent workflow |
|---|---|
| External data source cube dimension tables (`CubeDimensionTableTypes`) | `create_cube` |
| Calculation register recalculations (`RecalculationTypes`) | `create_calculation_register` |
| Integration service channels (`IntegrationServiceChannelTypes`) | `create_integration_service` |
| Tabular section object/row types (`TabularSectionTypes`) | `add_tabular_section` |
| Accounting extra dimension types (`MdExtDimensionTypes`) | `create_chart_of_accounts` and `create_accounting_register` |

### Covered by the Shared Top-Level Template

| 1C object kind | Scenario |
|---|---|
| Document journals | `create_document_journal` |
| Document numerators | `create_document_numerator` |
| Document sequences | `create_sequence` |
| Constants | `create_constant` |
| Charts of characteristic types | `create_chart_of_characteristic_types` |
| Charts of accounts | `create_chart_of_accounts` |
| Charts of calculation types | `create_chart_of_calculation_types` |
| Exchange plans | `create_exchange_plan` |
| Business processes | `create_business_process` |
| Tasks | `create_task` |
| Common modules | `create_common_module` |
| Common forms | `create_common_form` |
| Common commands | `create_common_command` |
| Common pictures | `create_common_picture` |
| Common templates | `create_common_template` |
| Common attributes | `create_common_attribute` |
| Roles | `create_role` |
| Session parameters | `create_session_parameter` |
| Command groups | `create_command_group` |
| Functional options | `create_functional_option` |
| Functional option parameters | `create_functional_options_parameter` |
| Styles | `create_style` |
| Defined types | `create_defined_type` |
| External data sources | `create_external_data_source` |
| HTTP services | `create_http_service` |
| Web services | `create_web_service` |
| WS references | `create_ws_reference` |
| Integration services | `create_integration_service` |
| XDTO packages | `create_xdto_package` |
| Reports | `create_report` |
| Data processors | `create_data_processor` |
| Scheduled jobs | `create_scheduled_job` |
| Settings storages | `create_settings_storage` |
| Filter criteria | `create_filter_criterion` |
| Event subscriptions | `create_event_subscription` |
| Bots | `create_bot` |

### Creation Order For Cross-References

When several 1C metadata objects are created together, do not create dependent
attributes with placeholder types. Create objects that produce reference types
first, check markers, let EDT update produced types, and only then create
dependent objects that use exact `TypeItem` proxies.

Recommended order:

1. Root configuration/project setup.
2. Independent classifiers and catalogs/enums/plans: `Catalog`, `Enum`,
   `ChartOfCharacteristicTypes`, `ChartOfAccounts`, `ChartOfCalculationTypes`,
   `ExchangePlan`, `DefinedType`, `SettingsStorage`, `FilterCriterion`.
3. Documents, document journals, document numerators, and sequences. If registers
   will use documents as registrars, create the registrar documents before
   final register validation.
4. Business processes and tasks. A `BusinessProcess` must have a valid `Task`;
   create the task first or in the same transaction and assign it.
5. Registers. Create dimensions/resources after their referenced catalogs,
   enums, documents, plans, or charts exist. Link registrar documents when the
   register mode requires recorders.
6. Objects that consume existing metadata through attributes or parameters:
   constants, common attributes, catalog/document attributes, tabular section
   attributes, command parameters, chart value types, service parameters.
7. Forms, commands, modules, templates, routes, service operations, XDTO details,
   and external data source internals after their owner metadata object exists.

Reference type examples:

| Reference needed later | Create/validate first |
|---|---|
| `CatalogRef.<Name>` | `Catalog.<Name>` |
| `EnumRef.<Name>` | `Enum.<Name>` |
| `DocumentRef.<Name>` and registrar links | `Document.<Name>` |
| chart/account reference types | matching `ChartOfAccounts.<Name>` |
| calculation type references | matching `ChartOfCalculationTypes.<Name>` |
| characteristic references/value types | matching `ChartOfCharacteristicTypes.<Name>` and its value type |
| exchange-plan references | matching `ExchangePlan.<Name>` |
| business process task relation | matching `Task.<Name>` |

Use exact `typeProvider.getProxy("...Ref.Name")` only after the target object
exists. If the exact proxy is still `null`, retry after EDT refresh/build/indexing
instead of using `String`, generic roots, or transient `McoreFactory` types.

### Present In MdType.xcore, Not Configuration Top-Level

`MdType.xcore` also contains runtime type families for external standalone
artifacts. They should not be added to the standard `Configuration.get*`
top-level template without confirming the actual EDT workflow and storage model.

| MdType family | Manual status |
|---|---|
| `ExternalReportTypes` | Use file/import workflow when available; do not use `Configuration.getReports()` blindly |
| `ExternalDataProcessorTypes` | Use file/import workflow when available; do not use `Configuration.getDataProcessors()` blindly |

### MdType.xcore Cross-Check

The following concrete `*Types` classes from `MdType.xcore` are accounted for by
the manual map. If a new `*Types` class appears in `MdType.xcore`, add it here
and either map it to an existing scenario or mark it as not top-level.

| MdType.xcore class | Scenario / status |
|---|---|
| `AccountingRegisterTypes` | `create_accounting_register` |
| `AccumulationRegisterTypes` | `create_accumulation_register` |
| `BusinessProcessTypes` | `create_business_process` |
| `CalculationRegisterTypes` | `create_calculation_register` |
| `CatalogTypes` | `create_catalog` |
| `ChartOfAccountsTypes` | `create_chart_of_accounts` |
| `ChartOfCalculationTypesTypes` | `create_chart_of_calculation_types` |
| `ChartOfCharacteristicTypesTypes` | `create_chart_of_characteristic_types` |
| `ConstantTypes` | `create_constant` |
| `CubeDimensionTableTypes` | `create_cube` |
| `CubeTypes` | `create_cube` |
| `DataProcessorTypes` | `create_data_processor` |
| `DefinedTypeTypes` | `create_defined_type` |
| `DocumentJournalTypes` | `create_document_journal` |
| `DocumentTypes` | `create_document` |
| `EnumTypes` | `create_enum` |
| `ExchangePlanTypes` | `create_exchange_plan` |
| `ExternalDataProcessorTypes` | not configuration top-level |
| `ExternalDataSourceTypes` | `create_external_data_source` |
| `ExternalReportTypes` | not configuration top-level |
| `FilterCriterionTypes` | `create_filter_criterion` |
| `InformationRegisterTypes` | `create_information_register` |
| `IntegrationServiceChannelTypes` | `create_integration_service` |
| `IntegrationServiceTypes` | `create_integration_service` |
| `MdExtDimensionTypes` | `create_chart_of_accounts`, `create_accounting_register` |
| `RecalculationTypes` | `create_calculation_register` |
| `ReportTypes` | `create_report` |
| `SequenceTypes` | `create_sequence` |
| `SettingsStorageTypes` | `create_settings_storage` |
| `TableTypes` | `create_table` |
| `TabularSectionTypes` | `add_tabular_section` |
| `TaskTypes` | `create_task` |
| `WSReferenceTypes` | `create_ws_reference` |

### Not Yet Templated

These platform object kinds are intentionally listed here instead of guessed in
`index.json`. Add a template entry only after `JShellReflection` confirms the
exact `MdClassFactory.create*` method, `Configuration.get*` collection, and type
name for the EDT version in use.

| 1C object kind | Before adding a template, verify |
|---|---|
| Data areas | `MdClassFactory.create*DataArea*`, `Configuration.get*DataArea*` |
| Monitoring centers | `MdClassFactory.create*Monitoring*`, `Configuration.get*Monitoring*` |
| Route maps | `MdClassFactory.create*Route*Map*`, `Configuration.get*Route*Map*` |
| Presentation/display packages | `MdClassFactory.create*Presentation*Package*` or `create*Display*Package*`, matching `Configuration.get*Packages*` |

### Maintenance Rules

- Prefer `_templates/top-level-create.md` for simple top-level objects.
- Create a dedicated guide only when the object has mandatory child objects,
  mandatory type descriptions, registrar rules, external files, or validation
  traps.
- Do not add guessed factory methods, collection getters, enum constants, or
  type names. Confirm them with `JShellReflection` first.
- After adding a new entry to `index.json`, verify that `JShellManual` can load
  the scenario and that the referenced guide/template resource exists.
