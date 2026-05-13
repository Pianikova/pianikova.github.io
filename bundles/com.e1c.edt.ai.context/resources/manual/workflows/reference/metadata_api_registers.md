# EDT Metadata API Cards: Registers

Registers have more validation dependencies than simple top-level objects. Use the enhanced register workflow when adding dimensions, resources, attributes, chart references, or registrar documents.

## InformationRegister

Factory: `mdFactory.createInformationRegister()`
Collection: `configuration.getInformationRegisters()`
FQN prefix: `InformationRegister`
Common safe setters:
- `setWriteMode(RegisterWriteMode.INDEPENDENT)` for an independent register.
- `setWriteMode(RegisterWriteMode.RECORDER_SUBORDINATE)` for recorder-subordinate behavior when that is required by the scenario.

Known constants above do not require `JShellReflection`. If periodicity or record manager behavior is not listed in a scenario, call `JShellReflection` once for all required register APIs.

Dimensions, resources, and attributes are child objects and need UUIDs.

Validation trap: string dimensions/resources/attributes must use finite string qualifiers. For `IEObjectTypeNames.STRING`, build `TypeDescription` with `.setStringQualifiers(100, false)` or a smaller explicit length to avoid SU8 "Строка не может быть неограниченной длины". Do not use values greater than 100, such as `150` or `1000`, unless the user explicitly requires it and the current EDT model accepts it.

## AccumulationRegister

Factory: `mdFactory.createAccumulationRegister()`
Collection: `configuration.getAccumulationRegisters()`
FQN prefix: `AccumulationRegister`
Common safe setters:
- `setRegisterType(AccumulationRegisterType.BALANCE)` for balance/remainder registers.
- `setRegisterType(AccumulationRegisterType.TURNOVERS)` for turnover registers.

Do not use `AccumulationRegisterType.REMAINDERS`: this constant is from old 1C terminology and is not the EDT API constant. Known constants above do not require `JShellReflection`.

Dimensions, resources, and registrar document relationships are composite operations. Use `enhanced_register_creation` and `add_document_registers`.

## AccountingRegister

Factory: `mdFactory.createAccountingRegister()`
Collection: `configuration.getAccountingRegisters()`
FQN prefix: `AccountingRegister`
Required dependency:
- a `ChartOfAccounts` is usually needed before useful accounting register setup.

Dimensions, resources, chart links, and register records are not baseline CRUD.

## MdExtDimension

`MdType.xcore` exposes extended dimension runtime types for accounting-related metadata. These are not top-level metadata objects and should not be created through a generic `Configuration` collection. Configure accounting register extra dimensions only through a dedicated accounting register workflow or after a batch reflection query that includes the register, chart of accounts, and extra dimension classes.

## CalculationRegister

Factory: `mdFactory.createCalculationRegister()`
Collection: `configuration.getCalculationRegisters()`
FQN prefix: `CalculationRegister`
Required dependency:
- a `ChartOfCalculationTypes` is usually needed before useful calculation register setup.

Schedules, recalculations, dimensions, resources, and registrar documents require a dedicated workflow or batch reflection.

## Recalculation

Factory: use `JShellReflection` to verify the exact factory and parent collection for the installed EDT version.
Parent: calculation register / chart calculation context depending on scenario.

This object is not a generic top-level create target in the current manual.
