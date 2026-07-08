## API Compatibility Notes

### ⚠️ Critical API note — resolving `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` in JShell

**Do NOT use `typeProvider.getProxy("CatalogRef.X")` for metadata reference
types.** The global `IEObjectProvider` type index is populated asynchronously
by EDT and is **not refreshed within a JShell session** — it returns `null`
for every freshly-created Catalog / Document / Enum / Chart / etc., and for
ones that already existed when the project opened. Use
`MdProducedTypesUtil.getProducedType(mdObject, eClass)` instead — it reads
the produced `TypeItem` directly from the EMF object you fetched via
`transaction.getTopObjectByFqn(...)`.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

Catalog dep = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
if (dep == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Products — create it first");
}
TypeItem productsRef = MdProducedTypesUtil.getProducedType(
    dep, MdTypePackage.Literals.MD_REF_TYPE);  // "CatalogRef.Products"
```

`MdTypePackage.Literals.*` mapping: `MD_REF_TYPE` → `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` / `BusinessProcessRef.X` / `TaskRef.X` / `ChartOf...Ref.X` / `ExchangePlanRef.X`; `MD_OBJECT_TYPE` → `CatalogObject.X`; `MD_LIST_TYPE` → `CatalogList.X`; `MD_ROW_TYPE` → `CatalogTabularSectionRow.X.Y`; `MD_USER_DEFINED_TYPE` → `DefinedType.X` (returns `TypeSet`).

Primitive types (`STRING`, `NUMBER`, `BOOLEAN`, `DATE`) and **generic
polymorphic** reference roots (`IEObjectTypeNames.CATALOG_REF`, etc.) still
use `typeProvider.getProxy(...)` — keep that idiom. The `null`-on-metadata-refs
issue is specific to project-produced reference types like `"CatalogRef.Products"`.

### ⚠️ CRITICAL RULES - Follow These to Avoid Failures

#### Transaction Management

**✅ REQUIRED:**
- Before any metadata create/edit/delete, confirm the project is not read-only: `GetProjects` must show `read_only: false`. Configurations on full vendor support must not be modified — see `readonly_configuration`.
- Use `globalContext.execute(new AbstractBmTask<...>("Task name") { ... })` for ALL read/write operations
- Access `IBmTransaction` parameter in `execute()` method for metadata operations
- Use `getTopObjectByFqn()` to READ existing objects
- Use `mdFactory.createXxx()` + `attachTopObject()` to CREATE new objects
- Modify existing objects directly (no `attachTopObject()` needed)
- After every metadata CRUD operation, run the `GetMarkers` tool with `marker_type: "1c"` and inspect remaining 1C markers before reporting success
- If a workflow needs more than one unknown EDT type, method, factory, field, or enum, call `JShellReflection` once with the full `queries` array before writing JShell code
- Use canonical imports from `jshell_edt_canonical_imports`: `AbstractBmTask` is in `com._1c.g5.v8.bm.integration`; `IBmTransaction` is in `com._1c.g5.v8.bm.core`. Do not import either one from `com._1c.g5.v8.dt.bm.integration`.
- In persistent JShell sessions, wrap non-trivial snippets in `{ ... }` blocks to avoid stale top-level variables and `NoSuchFieldError`

**❌ PROHIBITED:**
- Do NOT use `executeReadonlyTask(...)` for metadata creation/modification
- Do NOT override final methods `getId()` / `getServiceId()` in `AbstractBmTask`
- Do NOT use `attachTopObject()` on existing objects (causes `BmFqnAlreadyInUseException`)

#### Version and Type Handling

- Use `v8project.getVersion()` (returns `Version` object), NOT `getRuntimeVersion()`
- Localized fields (`synonym`, `comment`, `toolTip`) are `EMap<String, String>`: use `put("ru", "...")`
- Type qualifiers (StringQualifiers, NumberQualifiers) are ABSTRACT classes - CANNOT instantiate directly
- For type handling: use `TypeDescriptionBuilder` WITHOUT qualifiers or use default types

- If the new child object extends `BasicFeature`, treat `setType(...)` as mandatory, not optional

- Before finishing transaction, verify every new `BasicFeature` has non-null/non-empty `type` to avoid `MdTypeSetInferrer` NPE during derived rebuild

#### Metadata Object-Specific Rules

**Catalog (Справочник):**
- Use `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS` or `HierarchyType.HIERARCHY_OF_ITEMS` (correct enum constants)
- Use `setDescriptionLength(...)` - `setNameLength(...)` is NOT AVAILABLE
- For attributes: use `CatalogAttribute` via `createCatalogAttribute()` or `modelFactory` + EClass
- Supports: hierarchical, codeType (Number/String), checkUnique, autonumbering

**Document (Документ):**
- Use `DocumentNumberType.NUMBER` or `DocumentNumberType.STRING`
- Use `DocumentNumberPeriodicity.NONPERIODICAL` (also `YEAR`, `QUARTER`, `MONTH`, `DAY`)
- Supports: realTimePosting, registerRecordsDeletion, sequenceFilling
- Do not use `setPosted(...)` - this setter is not present in EDT API
- May reference: numerator, registerRecords (array of BasicRegister)

```java
// Document number periodicity — pick the right enum CLASS name carefully
document.setNumberPeriodicity(DocumentNumberPeriodicity.YEAR);
// ❌ DateNumberPeriodicityity.YEAR     — class does NOT exist (typo / hallucination)
// ❌ DateNumberPeriodicity.YEAR        — class does NOT exist
// ❌ DocumentPeriodicity.YEAR          — class does NOT exist
// The ONLY correct enum class is DocumentNumberPeriodicity from
// com._1c.g5.v8.dt.metadata.mdclass; valid values: NONPERIODICAL, YEAR, QUARTER, MONTH, DAY.
```

**InformationRegister (РегистрСведений):**
- Use `InformationRegisterPeriodicity.NONPERIODICAL`, `SECOND`, `DAY`, `MONTH`, `QUARTER`, `YEAR`, `RECORDER_POSITION`
- Use setter `setInformationRegisterPeriodicity(...)` (not `setPeriodicity(...)`)
- Use `RegisterWriteMode.INDEPENDENT` or `RegisterWriteMode.RECORDER_SUBORDINATE`
- Use specific child factories (`createInformationRegisterDimension/Resource`); generic `createRegisterDimension/Resource` is not valid in mdFactory
- Contains: resources, attributes, dimensions (all require types)
- String dimensions/resources/attributes must use finite string qualifiers, for example `.setStringQualifiers(100, false)`, otherwise `GetMarkers` can return SU8 "Строка не может быть неограниченной длины". Do not use values greater than 100, such as `150` or `1000`, unless the user explicitly requires it and the current EDT model accepts it.

**AccumulationRegister (РегистрНакопления):**
- Use `AccumulationRegisterType.BALANCE` or `AccumulationRegisterType.TURNOVERS`
- Do NOT use non-existing constants like `REMAINS` or `TURNOVER`
- Use specific child factories: `createAccumulationRegisterDimension()` and `createAccumulationRegisterResource()`

**Enum (Перечисление):**
- Contains `EnumValue[] enumValues` - create with `createEnumValue()`
- Use `enumObject.getEnumValues()` for collection access; do NOT use `getValues()`
- In JShell snippets prefer fully-qualified `com._1c.g5.v8.dt.metadata.mdclass.Enum` and `EnumValue` to avoid ambiguous imports
- Each EnumValue has: name, description, color (since 8.5.1)
- NO attributes or tabular sections

**CommonModule (ОбщийМодуль):**
- Safe baseline flags: `setServer(true)` and `setServerCall(true)`
- Do NOT use `setClient(...)` in EDT API snippets unless you verified the exact method exists in this version

**ChartOfCharacteristicTypes (ПланВидовХарактеристик):**
- Has its own type (TypeDescription) for characteristic values
- Supports: hierarchical, codeSeries, checkUnique, autonumbering
- May reference: characteristicExtValues (Catalog)

**Tabular Sections:**
- Use `TabularSectionAttribute` with `createTabularSectionAttribute()`
- Attributes: name, synonym, type (TypeDescription), indexing, fillChecking
- Line number length configurable (since 8.3.27)

#### Factory Selection Guidelines

**Prefer `mdFactory` for most operations:**
- More reliable in JShell context (no OSGi timeout issues)
- Simpler API for creating metadata objects
- Consistent with 1C metadata creation patterns

**Use `modelFactory` when needed:**
- For project/version context operations
- NOTE: `fillDefaultReferences()` may timeout in JShell due to OSGi service limitations
- **RECOMMENDED for JShell:** Use manual UUID assignment: `object.setUuid(UUID.randomUUID())`

#### Object Creation Workflow (Required Order)

1. Create object with `mdFactory.createXxx()`
2. Set required properties: name, synonym, type-specific settings
3. Add children (attributes, tabular sections, etc.) if needed
4. **CRITICAL for JShell:** Set UUID manually: `object.setUuid(UUID.randomUUID())`
   - For children, set UUIDs: `childObject.setUuid(UUID.randomUUID())`
   - NOTE: `modelFactory.fillDefaultReferences()` may timeout in JShell
5. Generate FQN: `fqnGenerator.generateStandaloneObjectFqn(eClass(), name)`
6. Attach: `transaction.attachTopObject((IBmObject)object, fqn)`
7. Add to parent collection: `configuration.getXxxs().add(object)`
8. After the transaction completes, call `GetMarkers` with `marker_type: "1c"` for the project or changed file and fix any new metadata errors

#### Common Property Setting Patterns

**Names and Synonyms:**
```java
object.setName("ObjectName");
object.getSynonym().put("ru", "Объект");
object.getComment().put("ru", "Комментарий");
```

**Setting Types:**
```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();
attribute.setType(typeDesc);
```

**UUID Handling (CRITICAL):**
```java
// Option 1: RECOMMENDED for JShell - manual UUID assignment
object.setUuid(UUID.randomUUID());
// For children, also set UUIDs:
childObject.setUuid(UUID.randomUUID());

// Option 2: auto-generate all UUIDs (may timeout in JShell)
// modelFactory.fillDefaultReferences(object);
```



## TypeDescription Handling Guide

### Overview
TypeDescription is the EDT API representation of 1C types for metadata attributes and properties. Use `TypeDescriptionBuilder` from `com._1c.g5.v8.dt.platform.core.typeinfo` package to create type descriptions.

### Key Components

**1. IEObjectProvider Registry**
Access platform type registry for current project version:
```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
```

**⚠️ CRITICAL: Context Requirements**
**MUST be created INSIDE BM transaction context:**
- IEObjectProvider MUST use `v8project.getVersion()` from a properly initialized IV8Project
- TypeItem proxies MUST be obtained and used within the SAME IBmTransaction
- It is safe to reuse the already resolved `v8project`, but resolve `TypeItem` and build `TypeDescription` inside the current transaction
- DO NOT reuse TypeDescription created in a different transaction context

**Correct usage pattern:**
```java
IV8Project v8project = projectManager.getProject(project);
globalContext.execute(new AbstractBmTask<Void>("Create metadata") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // Get typeProvider INSIDE transaction
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // Get TypeItem INSIDE transaction
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();

        // Set TypeDescription to attribute INSIDE transaction
        attribute.setType(typeDesc);
        return null;
    }
});
```

**❌ WRONG Pattern - causes NullPointerException:**
```java
IV8Project v8project = projectManager.getProject(project);
// Getting typeProvider OUTSIDE transaction
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

globalContext.execute(new AbstractBmTask<Void>("Create metadata") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // Using TypeItem created OUTSIDE transaction context
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();
        attribute.setType(typeDesc); // ❌ May cause NullPointerException
        return null;
    }
});
```

**2. TypeItem Proxy Retrieval**
Get type proxy by name from `IEObjectTypeNames`:
```java
// Basic types
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);
TypeItem booleanType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BOOLEAN);
TypeItem undefinedType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UNDEFINED);
TypeItem valueType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.VALUESTORAGE);
TypeItem uuidType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UUID);

// Primary metadata reference types. These are generic polymorphic roots.
// For a concrete user request like CatalogRef.Контрагенты or EnumRef.ВидыТоваров,
// resolve via MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)
// where depMdObject = transaction.getTopObjectByFqn("Catalog.Контрагенты") / "Enum.ВидыТоваров".
// Do NOT use typeProvider.getProxy("CatalogRef.X") — it returns null in JShell.
TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
TypeItem documentRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DOCUMENT_REF);
TypeItem enumRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ENUM_REF);
TypeItem businessProcessRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BUSINESS_PROCESS_REF);
TypeItem taskRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.TASK_REF);

// Register reference types
TypeItem accumulationRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCUMULATION_REGISTER_REF);
TypeItem accountingRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCOUNTING_REGISTER_REF);
TypeItem informationRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.INFORMATION_REGISTER_REF);
TypeItem calculationRegisterRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CALCULATION_REGISTER_REF);

// Chart/Plan reference types
TypeItem chartOfAccountsRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_ACCOUNTS_REF);
TypeItem chartOfCalcTypesRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CALCULATION_TYPES_REF);
TypeItem chartOfCharTypesRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CHARACTERISTIC_TYPES_REF);
TypeItem exchangePlanRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.EXCHANGE_PLAN_REF);

// Special types
TypeItem anyRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ANY_REF);
TypeItem characteristic = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHARACTERISTIC);
```

**3. TypeDescriptionBuilder**
Builder pattern for creating TypeDescription:
```java
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();

// Multiple types (composite type)
TypeDescription compositeType = new TypeDescriptionBuilder()
    .addType(stringType)
    .addType(numberType)
    .build();
```

### Common Type Patterns

**Basic Types:**
```java
IV8Project v8project = projectManager.getProject(project);
// String type
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();

// String type with length qualifier (recommended for INN, codes, etc.)
// Create String type with length qualifier
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);

// String, no qualifier
typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();

// String of length 50, variable-length — use the builder's setStringQualifiers(length, fixed).
// This path is JShell-safe (no `modelFactory` / OSGi service involved).
typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(50, false)
    // ❌ .stringQualifiersLength(50) — method does NOT exist
    // ❌ .setLength(50)               — TypeDescriptionBuilder has no such method
    .build();

// Number type — Number(10, 2), non-negative.
// ⚠️ CRITICAL: scale must be <= precision (otherwise SU8 error).
// Signature: setNumberQualifiers(int scale, int precision, boolean nonNegative).
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
typeDesc = new TypeDescriptionBuilder()
    .addType(numberType)
    .setNumberQualifiers(2, 10, true)
    // ❌ .numberQualifiersPrecision(10).numberQualifiersScale(2) — methods do NOT exist
    // ❌ .setPrecision(10).setScale(2)                           — TypeDescriptionBuilder has no such methods
    .build();

// Date type
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);
typeDesc = new TypeDescriptionBuilder()
    .addType(dateType)
    .build();

// Boolean type
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem booleanType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BOOLEAN);
typeDesc = new TypeDescriptionBuilder()
    .addType(booleanType)
    .build();
```

**Reference Types:**
```java
IV8Project v8project = projectManager.getProject(project);
// Catalog reference (generic)
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(catalogRefType)
    .build();

// Specific catalog reference (requires existing catalog).
// ⚠️ Use MdProducedTypesUtil — NOT typeProvider.getProxy("CatalogRef.Products"), which returns null in JShell.
Catalog productsDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
if (productsDep == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Products — create it first");
}
TypeItem catalogType = MdProducedTypesUtil.getProducedType(
    productsDep, MdTypePackage.Literals.MD_REF_TYPE);
typeDesc = new TypeDescriptionBuilder()
    .addType(catalogType)
    .build();

// Document reference (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem documentRefType = typeProvider.getProxy(IEObjectTypeNames.DOCUMENT_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(documentRefType)
    .build();

// Specific document reference — note: TypeItem name is "DocumentRef.X", not "Document.X".
// ⚠️ Use MdProducedTypesUtil — NOT typeProvider.getProxy("DocumentRef.GoodsReceipt") (null in JShell).
Document grDep = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
if (grDep == null) {
    throw new IllegalStateException("Missing dependency: Document.GoodsReceipt — create it first");
}
TypeItem documentType = MdProducedTypesUtil.getProducedType(
    grDep, MdTypePackage.Literals.MD_REF_TYPE);
typeDesc = new TypeDescriptionBuilder()
    .addType(documentType)
    .build();

// Enum reference
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem enumRefType = typeProvider.getProxy(IEObjectTypeNames.ENUM_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(enumRefType)
    .build();

// Specific enum reference.
// ⚠️ Use MdProducedTypesUtil — NOT typeProvider.getProxy("EnumRef.OrderStatus"), which returns null in JShell.
com._1c.g5.v8.dt.metadata.mdclass.Enum orderStatusDep =
    (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.OrderStatus");
if (orderStatusDep == null) {
    throw new IllegalStateException("Missing dependency: Enum.OrderStatus — create it first");
}
TypeItem enumType = MdProducedTypesUtil.getProducedType(
    orderStatusDep, MdTypePackage.Literals.MD_REF_TYPE);
typeDesc = new TypeDescriptionBuilder()
    .addType(enumType)
    .build();
```

**Other Special Types:**
```java
IV8Project v8project = projectManager.getProject(project);
// ValueStorage
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem valueType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.VALUESTORAGE);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(valueType)
    .build();

// UUID
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem uuidType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UUID);
typeDesc = new TypeDescriptionBuilder()
    .addType(uuidType)
    .build();

// Undefined
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem undefinedType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.UNDEFINED);
typeDesc = new TypeDescriptionBuilder()
    .addType(undefinedType)
    .build();

// Any reference (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem anyRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ANY_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(anyRefType)
    .build();

// Universal characteristic
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem characteristicType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHARACTERISTIC);
typeDesc = new TypeDescriptionBuilder()
    .addType(characteristicType)
    .build();
```

**Register Reference Types:**
```java
IV8Project v8project = projectManager.getProject(project);
// Accumulation register (generic)
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem accRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCUMULATION_REGISTER_REF);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(accRegRef)
    .build();

// Specific accumulation register — use MdProducedTypesUtil, NOT typeProvider.getProxy("AccumulationRegisterRef.X").
AccumulationRegister stockDep =
    (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
if (stockDep == null) {
    throw new IllegalStateException("Missing dependency: AccumulationRegister.GoodsInStock");
}
TypeItem accReg = MdProducedTypesUtil.getProducedType(
    stockDep, MdTypePackage.Literals.MD_REF_TYPE);
typeDesc = new TypeDescriptionBuilder()
    .addType(accReg)
    .build();

// Accounting register (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem accRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.ACCOUNTING_REGISTER_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(accRegRef)
    .build();

// Information register (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem infoRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.INFORMATION_REGISTER_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(infoRegRef)
    .build();

// Calculation register (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem calcRegRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CALCULATION_REGISTER_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(calcRegRef)
    .build();
```

**Chart/Plan Reference Types:**
```java
IV8Project v8project = projectManager.getProject(project);
// Chart of accounts (generic)
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem coaRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_ACCOUNTS_REF);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(coaRef)
    .build();

// Specific chart of accounts — use MdProducedTypesUtil, NOT typeProvider.getProxy("ChartOfAccountsRef.X").
ChartOfAccounts coaDep =
    (ChartOfAccounts)transaction.getTopObjectByFqn("ChartOfAccounts.ПланСчетов");
if (coaDep == null) {
    throw new IllegalStateException("Missing dependency: ChartOfAccounts.ПланСчетов");
}
TypeItem coa = MdProducedTypesUtil.getProducedType(
    coaDep, MdTypePackage.Literals.MD_REF_TYPE);
typeDesc = new TypeDescriptionBuilder()
    .addType(coa)
    .build();

// Chart of calculation types (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem cctRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CALCULATION_TYPES_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(cctRef)
    .build();

// Chart of characteristic types (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem cchtRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_CHARACTERISTIC_TYPES_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(cchtRef)
    .build();

// Exchange plan (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem exchangePlanRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.EXCHANGE_PLAN_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(exchangePlanRef)
    .build();

// Business process (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem bpRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BUSINESS_PROCESS_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(bpRef)
    .build();

// Task (generic)
typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem taskRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.TASK_REF);
typeDesc = new TypeDescriptionBuilder()
    .addType(taskRef)
    .build();
```

### All Available IEObjectTypeNames Constants

**Basic Types:**
- `UNDEFINED` - Неопределено
- `NULL` - Null value
- `BOOLEAN` - Булево
- `NUMBER` - Число
- `STRING` - Строка
- `DATE` - Дата
- `TYPE` - Тип
- `VALUESTORAGE` - ХранилищеЗначения
- `UUID` - UUID/GUID
- `BINARY_DATA` - ДвоичныеДанные

**Primary Metadata Reference Types:**
- `CATALOG_REF` - СправочникСсылка
- `CATALOG_OBJ` - СправочникОбъект
- `DOCUMENT_REF` - ДокументСсылка
- `DOCUMENT_OBJ` - ДокументОбъект
- `ENUM_REF` - ПеречислениеСсылка
- `BUSINESS_PROCESS_REF` - БизнесПроцессСсылка
- `TASK_REF` - ЗадачаСсылка

**Register Reference Types:**
- `ACCUMULATION_REGISTER_REF` - РегистрНакопленияСсылка
- `ACCOUNTING_REGISTER_REF` - РегистрБухгалтерииСсылка
- `INFORMATION_REGISTER_REF` - РегистрСведенийСсылка
- `CALCULATION_REGISTER_REF` - РегистрРасчетаСсылка

**Chart/Plan Reference Types:**
- `CHART_OF_ACCOUNTS_REF` - ПланСчетовСсылка
- `CHART_OF_CALCULATION_TYPES_REF` - ПланВидовРасчетаСсылка
- `CHART_OF_CHARACTERISTIC_TYPES_REF` - ПланВидовХарактеристикСсылка
- `EXCHANGE_PLAN_REF` - ПланОбменаСсылка

**Special Types:**
- `ANY_REF` - ЛюбаяСсылка (generic reference)
- `CHARACTERISTIC` - Характеристика (universal)
- `DEFINED_TYPE` - DefinedType (user-defined)

### Specific Metadata Type References

To reference specific metadata objects, fetch the dependency MdObject via
`transaction.getTopObjectByFqn(...)` and resolve its produced `TypeItem`
through `MdProducedTypesUtil`. Do **NOT** call
`typeProvider.getProxy("CatalogRef.X")` / `"DocumentRef.X"` / `"EnumRef.X"`
etc. — they return `null` in JShell.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

// Specific catalog → "CatalogRef.Products"
Catalog productsDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
TypeItem productsRef = MdProducedTypesUtil.getProducedType(productsDep, MdTypePackage.Literals.MD_REF_TYPE);

// Specific document → "DocumentRef.GoodsReceipt"
Document grDep = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
TypeItem goodsReceiptRef = MdProducedTypesUtil.getProducedType(grDep, MdTypePackage.Literals.MD_REF_TYPE);

// Specific enum → "EnumRef.OrderStatus"
com._1c.g5.v8.dt.metadata.mdclass.Enum osDep =
    (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.OrderStatus");
TypeItem orderStatusRef = MdProducedTypesUtil.getProducedType(osDep, MdTypePackage.Literals.MD_REF_TYPE);

// Specific registers → "AccumulationRegisterRef.GoodsInStock", "InformationRegisterRef.Prices"
AccumulationRegister stockDep =
    (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
TypeItem goodsStockRef = MdProducedTypesUtil.getProducedType(stockDep, MdTypePackage.Literals.MD_REF_TYPE);
InformationRegister pricesDep =
    (InformationRegister)transaction.getTopObjectByFqn("InformationRegister.Prices");
TypeItem pricesRef = MdProducedTypesUtil.getProducedType(pricesDep, MdTypePackage.Literals.MD_REF_TYPE);

// Specific charts/plans → "ChartOfAccountsRef.ПланСчетов", "ChartOfCalculationTypesRef.ВидыРасчетов"
ChartOfAccounts coaDep =
    (ChartOfAccounts)transaction.getTopObjectByFqn("ChartOfAccounts.ПланСчетов");
TypeItem planOfAccounts = MdProducedTypesUtil.getProducedType(coaDep, MdTypePackage.Literals.MD_REF_TYPE);
ChartOfCalculationTypes cctDep =
    (ChartOfCalculationTypes)transaction.getTopObjectByFqn("ChartOfCalculationTypes.ВидыРасчетов");
TypeItem chartOfCalc = MdProducedTypesUtil.getProducedType(cctDep, MdTypePackage.Literals.MD_REF_TYPE);

// In every case: throw IllegalStateException if the dep MdObject is null — create it first.
```

### Type Qualifiers (Advanced)

**⚠️ IMPORTANT RESTRICTION:**
Type qualifiers (StringQualifiers, NumberQualifiers) are ABSTRACT classes and CANNOT be instantiated directly.
For most use cases, use TypeDescriptionBuilder WITHOUT qualifiers or use default types.

**⚠️ CRITICAL: Always use IEObjectProvider inside transaction!**
```java
IV8Project v8project = projectManager.getProject(project);
// ✅ CORRECT: Create typeProvider INSIDE transaction
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();
```
If qualifiers are required, use the builder's fluent setters — they construct
the qualifier internally via `McoreFactory.eINSTANCE` and are JShell-safe
(NO `modelFactory` / OSGi service):
```java
// String of length 100, variable-length
TypeDescription strType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build();

// Number(10, 2), non-negative
TypeDescription numType = new TypeDescriptionBuilder()
    .addType(numberType)
    .setNumberQualifiers(2, 10, true)
    .build();
```
### Best Practices

**1. Always use typeProvider for version compatibility**
```java
IV8Project v8project = projectManager.getProject(project);
// ✅ CORRECT - uses project version
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem type = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);

// ❌ WRONG - doesn't account for version
// TypeItem type = TypeItem.eINSTANCE; // Do NOT do this
```

**2. Reuse TypeItem, not TypeDescription**
```java
// TypeDescription is containment. Build a fresh instance for every child.
for (CatalogAttribute attr : attributes) {
    TypeDescription stringTypeDesc = new TypeDescriptionBuilder()
        .addType(stringType)
        .build();
    attr.setType(stringTypeDesc);
}
```

**3. Handle composite types correctly**
```java
// Composite type (String or Number)
TypeDescription compositeType = new TypeDescriptionBuilder()
    .addType(stringType)
    .addType(numberType)
    .build();
attribute.setType(compositeType);
```

**4. Type-specific considerations**
- **Catalog/Document references**: Use generic `*_REF` types when specific metadata may not exist
- **Number types**: Consider precision/scale requirements. ⚠️ CRITICAL: Scale MUST be <= Precision (SU8 error)
  - Precision: total number of digits (integer + decimal)
  - Scale: number of digits after decimal point
  - Example: Number(10, 2) = up to 10 total digits, 2 after decimal
  - Example: 12345678.90 has 8 integer digits + 2 decimal = 10 total digits
- **String types**: Consider length requirements (use StringQualifiers)
- **Date types**: May need DateQualifiers in advanced scenarios
- **Enum references**: Enum must exist in configuration
- **Composite types**: Order affects type priority in some contexts
- **Register references**: Use `ACCUMULATION_REGISTER_REF`, `ACCOUNTING_REGISTER_REF`, `INFORMATION_REGISTER_REF`, `CALCULATION_REGISTER_REF` for generic register references
- **Chart/Plan references**: Use `CHART_OF_ACCOUNTS_REF`, `CHART_OF_CALCULATION_TYPES_REF`, `CHART_OF_CHARACTERISTIC_TYPES_REF`, `EXCHANGE_PLAN_REF` for generic chart/plan references
- **Business Process/Task references**: Use `BUSINESS_PROCESS_REF` and `TASK_REF` for workflow metadata
- **ANY_REF**: Universal reference type - use when any reference type is acceptable
- **CHARACTERISTIC**: Universal characteristic type for flexible attribute handling
- **Specific metadata reference TypeItems**: resolve via `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)` where `depMdObject = transaction.getTopObjectByFqn("Catalog.Products")` / `"Enum.OrderStatus"`. Do NOT call `typeProvider.getProxy("CatalogRef.Products")` — it returns `null` in JShell. Use `"Catalog.Products"` / `"Enum.OrderStatus"` only for top-object FQNs in `transaction.getTopObjectByFqn(...)`.

### Complete Example

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Create catalog with types") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        Catalog catalog = mdFactory.createCatalog();
        catalog.setName("Products");
        catalog.getSynonym().put("ru", "Товары");
        catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);

        // Create type provider
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // Create attributes with different types
        CatalogAttribute code = mdFactory.createCatalogAttribute();
        code.setName("Code");
        code.getSynonym().put("ru", "Код");
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription codeType = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();
        code.setType(codeType);
        catalog.getAttributes().add(code);

        CatalogAttribute description = mdFactory.createCatalogAttribute();
        description.setName("Description");
        description.getSynonym().put("ru", "Наименование");
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription descType = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();
        description.setType(descType);
        catalog.getAttributes().add(description);

        CatalogAttribute category = mdFactory.createCatalogAttribute();
        category.setName("Category");
        category.getSynonym().put("ru", "Категория");
        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription catType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();
        category.setType(catType);
        catalog.getAttributes().add(category);

        // Set UUIDs
        catalog.setUuid(UUID.randomUUID());
        code.setUuid(UUID.randomUUID());
        description.setUuid(UUID.randomUUID());
        category.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
        transaction.attachTopObject((IBmObject)catalog, fqn);
        configuration.getCatalogs().add(catalog);
        return null;
    }
});
```



## Transaction Management Scenarios

### When to use `attachTopObject()`

| Scenario | Use attachTopObject() |
|----------|---------------------|
| Creating NEW object | ✅ Yes, once |
| Reading existing | ❌ No |
| Editing existing | ❌ No |
| Renaming FQN | ❌ No, use `updateTopObjectFqn()` |
| Detaching object | ❌ No, use `detachTopObject()` |

### Key Transaction Rules

**1. attachTopObject() - ONLY for NEW objects**
```java
// CORRECT: Creating a new catalog
Catalog catalog = mdFactory.createCatalog();
String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
transaction.attachTopObject((IBmObject)catalog, fqn); // ✅ OK
configuration.getCatalogs().add(catalog);
```

**2. Editing existing - NO attachTopObject()**
```java
// CORRECT: Editing an existing catalog
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
catalog.setDescriptionLength(200); // ✅ OK
// No attachTopObject() call!
```

**3. Avoiding BmFqnAlreadyInUseException**
```java
// Check before creating
String fqn = "Catalog.Products";
if (transaction.getTopObjectByFqn(fqn) == null) {
    Catalog catalog = mdFactory.createCatalog();
    catalog.setName("Products");
    transaction.attachTopObject((IBmObject)catalog, fqn);
    configuration.getCatalogs().add(catalog);
} else {
    // Object already exists - handle appropriately
}
```


## Common Pitfalls and Solutions

This section covers the most frequent mistakes when working with metadata creation and editing.

### ❌ Pitfall #0: `return;` inside task code that expects a value

**Error:** `incompatible types: missing return value`

**Problem:** Using `return;` inside `AbstractBmTask.execute()` when the method returns `Void` or another value.

```java
// ❌ WRONG CODE
if (projectHandle.exists()) {
    System.err.println("ERROR: Project already exists");
    return;
}
```

```java
// ✅ CORRECT CODE
if (projectHandle.exists()) {
    System.err.println("ERROR: Project already exists");
    return null;
}
```

### ❌ Pitfall #1: attachTopObject on existing object

**Error:** `BmFqnAlreadyInUseException`

**Problem:** Trying to attach an object that already exists in the transaction.

```java
// ❌ WRONG CODE
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
document.setDescriptionLength(200); // Modification
transaction.attachTopObject((IBmObject)document, fqn); // ❌ Exception!
```

```java
// ✅ CORRECT CODE
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
if (document != null) {
    document.setDescriptionLength(200); // Direct modification
    // NO attachTopObject() call needed for existing objects!
}
```

### ❌ Pitfall #2: Creating object with existing FQN

**Error:** `BmFqnAlreadyInUseException` or validation error

**Problem:** Creating new object with FQN that already exists.

```java
// ❌ WRONG CODE
String fqn = "Catalog.Products"; // Already exists in configuration!
Catalog newCatalog = mdFactory.createCatalog();
transaction.attachTopObject((IBmObject)newCatalog, fqn); // ❌ Exception!
```

```java
// ✅ CORRECT CODE - Check before creating
String fqn = "Catalog.NewProducts";
if (transaction.getTopObjectByFqn(fqn) == null) {
    Catalog newCatalog = mdFactory.createCatalog();
    newCatalog.setName("NewProducts");
    newCatalog.setUuid(UUID.randomUUID()); // Set UUID
    String generatedFqn = fqnGenerator.generateStandaloneObjectFqn(
        newCatalog.eClass(), newCatalog.getName()).toString();
    transaction.attachTopObject((IBmObject)newCatalog, generatedFqn); // ✅ OK
    configuration.getCatalogs().add(newCatalog);
} else {
    // Object already exists - handle appropriately
}
```

### ❌ Pitfall #3: Not setting UUIDs

**Error:** SU45 - UUID required for all metadata objects

**Problem:** Creating metadata objects without UUIDs.

```java
IV8Project v8project = projectManager.getProject(project);
// ❌ WRONG CODE
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
CatalogAttribute attr = mdFactory.createCatalogAttribute();
attr.setName("Article");
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();

attr.setType(typeDesc);
catalog.getAttributes().add(attr);
String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
transaction.attachTopObject((IBmObject)catalog, fqn);
// Error: SU45 - UUID required for catalog and attributes!
```

```java
IV8Project v8project = projectManager.getProject(project);
// ✅ CORRECT CODE - Option 1: Manual UUID assignment (RECOMMENDED for JShell)
import java.util.UUID;
Catalog catalog = mdFactory.createCatalog();
catalog.setUuid(UUID.randomUUID());
catalog.setName("Products");
CatalogAttribute attr = mdFactory.createCatalogAttribute();
attr.setUuid(UUID.randomUUID()); // Must set UUID for children too!
attr.setName("Article");
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();

attr.setType(typeDesc);
catalog.getAttributes().add(attr);
String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
transaction.attachTopObject((IBmObject)catalog, fqn);
```

```java
IV8Project v8project = projectManager.getProject(project);
// ❌ PROHIBITED - Do NOT use fillDefaultReferences() in JShell
// This method will timeout due to OSGi service limitations
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
CatalogAttribute attr = mdFactory.createCatalogAttribute();
attr.setName("Article");
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();

attr.setType(typeDesc);
catalog.getAttributes().add(attr);
// ❌ modelFactory.fillDefaultReferences(catalog); // DO NOT USE!
```

**Always use manual UUID assignment instead.**

### ❌ Pitfall #4: Using mdFactory outside transaction

**Error:** Runtime exception or incorrect behavior

**Problem:** Trying to use mdFactory outside AbstractBmTask.execute().

```java
// ❌ WRONG CODE
// Outside of transaction - this will fail!
Catalog catalog = mdFactory.createCatalog();
```

```java
// ✅ CORRECT CODE - Always inside transaction
Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Create catalog") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // mdFactory MUST be used inside execute() method
        Catalog catalog = mdFactory.createCatalog();
        catalog.setName("Products");
        catalog.setUuid(UUID.randomUUID()); // Set UUID
        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
        transaction.attachTopObject((IBmObject)catalog, fqn);
        configuration.getCatalogs().add(catalog);
        return catalog;
    }
});
```

### ❌ Pitfall #5: Forgetting to add to parent collection

**Error:** Object created but not visible in configuration

**Problem:** Creating object but not adding it to configuration collection.

```java
// ❌ WRONG CODE
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
catalog.setUuid(UUID.randomUUID());
String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
transaction.attachTopObject((IBmObject)catalog, fqn);
// ❌ Missing: configuration.getCatalogs().add(catalog);
// Object is in transaction but not part of configuration!
```

```java
// ✅ CORRECT CODE
Configuration config = (Configuration)transaction.getTopObjectByFqn("Configuration");
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
catalog.setUuid(UUID.randomUUID());
String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
transaction.attachTopObject((IBmObject)catalog, fqn);
configuration.getCatalogs().add(catalog); // ✅ Critical step!
```

### ❌ Pitfall #6: Setting non-existent properties

**Error:** Compilation error or runtime exception

**Problem:** Trying to set properties that don't exist on the object type.

```java
// ❌ WRONG CODE
Catalog catalog = mdFactory.createCatalog();
catalog.setNameLength(25); // ❌ setNameLength() doesn't exist!
catalog.setDescriptionLength(150); // ✅ This exists
```

```java
// ✅ CORRECT CODE
Catalog catalog = mdFactory.createCatalog();
catalog.setDescriptionLength(150); // ✅ Correct property
catalog.setCodeLength(9); // ✅ Correct property
// Catalog has: name, synonym, comment, hierarchical, hierarchyType,
// codeLength, descriptionLength, codeType, etc.
```

### ❌ Pitfall #7: Incorrect top-level object deletion

**Errors:** `UnsupportedOperationException`, or repeated
`Resource ... .mdo does not exist` errors from background context/index walkers.

**Problem:** Using `EcoreUtil.delete()` for top-level metadata objects causes
an exception. Manually removing the object from `Configuration` and calling
`detachTopObject(...)` can delete the file but leave stale EDT platform-object
references for background services.

```java
// ❌ WRONG CODE for top-level objects
EcoreUtil.delete(catalog); // UnsupportedOperationException!
```

```java
// ✅ CORRECT CODE for top-level objects
Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
configuration.getCatalogs().remove(catalog);
transaction.detachTopObject((IBmObject)catalog);
```
```java
// ✅ CORRECT CODE for child objects (attributes, etc.)
EcoreUtil.delete(attr); // Works correctly for child objects
```
**IMPORTANT:** Remove from parent collection and detach from transaction.
**Note:** Do NOT use `EcoreUtil.delete()` for top-level objects - it causes `UnsupportedOperationException`.
**Note:** Deleting a catalog will cascade delete all its attributes, tabular sections, forms, and templates.

**Updated JShell rule:** for top-level metadata objects, prefer
`delete_metadata_object` and `IMdRefactoringService.createMdObjectDeleteRefactoring(...)`.
Do not use manual `configuration.getX().remove(...) + detachTopObject(...)`
in CRUD scenarios; it can leave stale platform-object references for EDT
background context sync. `EcoreUtil.delete(...)` remains valid for child
objects such as attributes.
### ❌ Pitfall #8: Incorrect Enum Constants in JShell

**Error:** Compilation error - cannot find symbol or type mismatch

**Problem:** Using outdated enum constants or passing the wrong parameter type.

```java
// ❌ WRONG CODE - Incorrect enum constants
catalog.setHierarchyType(HierarchyType.HIERARCHY_GROUPS); // Does not exist
document.setPosted(true); // Method does not exist
document.setRealTimePosting(true); // Expects RealTimePosting enum
```

```java
// ✅ CORRECT CODE - Use correct enum constants
catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);
document.setNumberType(DocumentNumberType.NUMBER);
document.setRealTimePosting(RealTimePosting.DENY);
```

### ❌ Pitfall #9: Incorrect TypeDescription Usage in JShell

**Error:** Type mismatch or NullPointerException

**Problem:** Passing wrong values to TypeDescriptionBuilder or using a proxy that resolved to null.

```java
IV8Project v8project = projectManager.getProject(project);
// ❌ WRONG CODE - String type cannot be passed directly
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType("Date") // ❌ Wrong! Must use TypeItem
    .build();

// ❌ WRONG CODE - Catalog object cannot be passed directly
typeDesc = new TypeDescriptionBuilder()
    .addType(catalogKontragenty) // ❌ Wrong! Must use TypeItem
    .build();

// ❌ WRONG CODE - typeProvider outside transaction
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem type = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);

globalContext.execute(new AbstractBmTask<Void>("Test") {
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        attribute.setType(new TypeDescriptionBuilder().addType(type).build());
        // ❌ TypeItem/proxy may be invalid for the current transaction
        return null;
    }
});
```

```java
IV8Project v8project = projectManager.getProject(project);
// ✅ CORRECT CODE - Use IEObjectProvider inside transaction
globalContext.execute(new AbstractBmTask<Void>("Create metadata") {
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // Create typeProvider INSIDE transaction
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // Get TypeItem INSIDE transaction
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);
        TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);

        // Build TypeDescription INSIDE transaction
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();

        attribute.setType(typeDesc);
        return null;
    }
});
```

### Updated guidance: enum constants and TypeDescription proxies

**HierarchyType:** use only `HIERARCHY_FOLDERS_AND_ITEMS` or `HIERARCHY_OF_ITEMS`.
`HIERARCHY_GROUPS` and `HIERARCHY_HIERARCHICAL` are not present in EDT API.

```java
catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);
// or
catalog.setHierarchyType(HierarchyType.HIERARCHY_OF_ITEMS);
```

**TypeDescriptionBuilder:** for primitive proxies always validate `typeProvider.getProxy(...)` before `addType(...)`. For concrete metadata reference types use `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)` and validate `depMdObject` — `typeProvider.getProxy("CatalogRef.X")` returns `null` in JShell and must not be used. Use a generic root type (`IEObjectTypeNames.CATALOG_REF`, etc.) only when the user explicitly asked for a polymorphic "any catalog" / "any enum" reference.

```java
Catalog unitsDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Units");
if (unitsDep == null) {
    throw new IllegalStateException("Missing referenced catalog: Catalog.Units — create it first");
}
TypeItem proxy = MdProducedTypesUtil.getProducedType(
    unitsDep, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(proxy)
    .build();
```

### ❌ Pitfall #10: Runtime BmFqnAlreadyInUseException

**Error:** `com._1c.g5.v8.bm.core.BmFqnAlreadyInUseException`
**Problem:** Trying to create object with FQN that already exists.

**Solution:** Always check if object exists before creating.

```java
// ✅ CORRECT CODE - Check before creating
String fqn = "Catalog.Products";
if (transaction.getTopObjectByFqn(fqn) == null) {
    Catalog catalog = mdFactory.createCatalog();
    catalog.setName("Products");
    catalog.setUuid(UUID.randomUUID());
    String generatedFqn = fqnGenerator.generateStandaloneObjectFqn(
        catalog.eClass(), catalog.getName()).toString();
    transaction.attachTopObject((IBmObject)catalog, generatedFqn);
    configuration.getCatalogs().add(catalog);
} else {
    System.out.println("Object already exists: " + fqn);
}
```



## Metadata Validation Errors - Common Fixes

### Missing `type` on `BasicFeature` (`md-legacy-emf-check`)

**Error Message:** "Должна быть задана сущность 'type', необходимая для..." or "Тип не указан"

**Problem:** A metadata object derived from `BasicFeature` is missing a required type definition.

**Common Causes:**
- `CatalogAttribute` added without TypeDescription
- `DocumentAttribute` added without TypeDescription
- `TabularSectionAttribute` added without TypeDescription
- TypeDescription created with `null` proxy or not assigned via `setType(...)`

**Fix Pattern:**
```java
IV8Project v8project = projectManager.getProject(project);
// Get the attribute and set its type
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
if (catalog != null) {
    for (CatalogAttribute attr : catalog.getAttributes()) {
        if ("ИНН".equals(attr.getName())) {
            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
            TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
            TypeDescription typeDesc = new TypeDescriptionBuilder()
                .addType(stringType)
                .build();
            attr.setType(typeDesc);
            System.out.println("Fixed attribute type");
        }
    }
}
```

**Important Notes:**
- Always get typeProvider INSIDE the transaction
- TypeItem proxies must be obtained and used within the SAME IBmTransaction
- For qualified types, use the fluent builder: `setStringQualifiers(length, fixed)`, `setNumberQualifiers(scale, precision, nonNegative)`, `setBinaryQualifiers(length, fixed)`, `setDateQualifiers(DateFractions)` — see scenario `create_type_description`

### SU8: Scale Cannot Exceed Precision

**Error Message:** "Точность числа не может быть больше его длины"

**Problem:** NumberQualifiers has Scale > Precision, which is invalid.

**Understanding Precision and Scale:**
- **Precision**: Total number of digits (integer part + decimal part)
- **Scale**: Number of digits after the decimal point
- **Rule**: Scale MUST be <= Precision

**Examples:**
```java
// ✅ CORRECT: Number(10, 2) - 10 total digits, 2 after decimal
// Values: 12345678.90 (8 + 2 = 10 digits)
typeDesc.getNumberQualifiers().setPrecision(10);
typeDesc.getNumberQualifiers().setScale(2);

// ❌ WRONG: Number(2, 10) - Scale (10) > Precision (2)
// This causes SU8 error
typeDesc.getNumberQualifiers().setPrecision(2);
typeDesc.getNumberQualifiers().setScale(10); // ❌ SU8 error!

// ✅ CORRECT: Number(15, 4) - 15 total digits, 4 after decimal
// Values: 1234567890123.4567 (11 + 4 = 15 digits)
typeDesc.getNumberQualifiers().setPrecision(15);
typeDesc.getNumberQualifiers().setScale(4);

// ✅ CORRECT: Number(20, 0) - 20 total digits, 0 after decimal
// Integer only: 12345678901234567890 (20 digits)
typeDesc.getNumberQualifiers().setPrecision(20);
typeDesc.getNumberQualifiers().setScale(0);
```

**Fix Pattern - Find and Correct All SU8 Errors:**
```java
globalContext.execute(new AbstractBmTask<Void>("Fix SU8 errors") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration config = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // Fix catalogs
        for (Catalog catalog : config.getCatalogs()) {
            for (CatalogAttribute attr : catalog.getAttributes()) {
                if (attr.getType() != null && attr.getType().getNumberQualifiers() != null) {
                    NumberQualifiers nq = attr.getType().getNumberQualifiers();
                    if (nq.getScale() > nq.getPrecision()) {
                        System.out.println("Fixing " + catalog.getName() + "." + attr.getName());
                        // Swap precision and scale to fix the error
                        int temp = nq.getPrecision();
                        nq.setPrecision(nq.getScale());
                        nq.setScale(temp);
                    }
                }
            }
        }

        // Fix documents
        for (Document document : config.getDocuments()) {
            for (DocumentAttribute attr : document.getAttributes()) {
                if (attr.getType() != null && attr.getType().getNumberQualifiers() != null) {
                    NumberQualifiers nq = attr.getType().getNumberQualifiers();
                    if (nq.getScale() > nq.getPrecision()) {
                        int temp = nq.getPrecision();
                        nq.setPrecision(nq.getScale());
                        nq.setScale(temp);
                    }
                }
            }
        }

        // Fix registers (Accumulation, Information, Accounting, Calculation)
        // Similar pattern for dimensions and resources...

        return null;
    }
});
```

**Common Mistakes to Avoid:**
```java
// ❌ WRONG: Trying to access non-existent methods
// NumberQualifiers nq = attr.getType().getNumberQualifiers();
// nq.getLength(); // ❌ This method does NOT exist!

// ✅ CORRECT: Use correct methods
int precision = nq.getPrecision();
int scale = nq.getScale();
```

### General Workflow for Fixing Metadata Validation Errors

**Step 1: Identify the error**
- Check EDT markers/problems view
- Note the error code (SU45, SU8, etc.)
- Note the object path (Catalog.Контрагенты, Document.ПриходТовара, etc.)

**Step 2: Understand the requirement**
- SU45: Type must be specified - use TypeDescriptionBuilder
- SU8: Scale <= Precision for Number types

**Step 3: Implement fix inside BM transaction**
- Always use `globalContext.execute(new AbstractBmTask<Void>(...) {...})`
- Get object by FQN: `transaction.getTopObjectByFqn("Catalog.Имя")`
- Modify directly (no attachTopObject needed for existing objects)
- Set type or qualifiers with `TypeDescriptionBuilder` inside the same BM transaction

**Step 4: Verify fix**
- Refresh/Rebuild project in EDT
- Check markers view for remaining errors

**Step 5: Consider project-wide fixes**
- If multiple objects have same error, iterate through collections
- Use Configuration object to access all catalogs, documents, registers

**Important Reminders:**
- TypeDescription and TypeItem must be created INSIDE the transaction
- Use `TypeDescriptionBuilder` for qualifiers in JShell context
- Set UUIDs manually when creating new metadata objects
- For existing objects: modify directly, don't use attachTopObject()
- Check that Scale <= Precision for all Number types
- Verify that all attributes have valid TypeDescription set
