## Scenario: Create TypeDescription

### ⚠️ Critical API note — resolving `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` in JShell

**Do NOT use `typeProvider.getProxy("CatalogRef.X")` for metadata reference
types.** The global `IEObjectProvider` type index is populated asynchronously
by EDT and is **not refreshed within a JShell session** — it returns `null`
for every Catalog / Document / Enum, including ones that already existed
when the project opened. Use `MdProducedTypesUtil.getProducedType(mdObject, eClass)`
instead — it reads the produced `TypeItem` directly from the EMF object.
Primitive types (`STRING`, `NUMBER`, `BOOLEAN`, `DATE`) still use
`typeProvider.getProxy(IEObjectTypeNames.STRING)` — keep that idiom.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

Catalog dep = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
if (dep == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Контрагенты — create it first");
}
TypeItem suppliersRef = MdProducedTypesUtil.getProducedType(
    dep, MdTypePackage.Literals.MD_REF_TYPE);  // "CatalogRef.Контрагенты"
```

`MdTypePackage.Literals.*` mapping: `MD_REF_TYPE` → `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X`; `MD_OBJECT_TYPE` → `CatalogObject.X`; `MD_LIST_TYPE` → `CatalogList.X`; `MD_ROW_TYPE` → `CatalogTabularSectionRow.X.Y`; `MD_USER_DEFINED_TYPE` → `DefinedType.X`.

### String

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();
```

### Number

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(numberType)
    .build();
```

### String with qualifiers (length, fixed)

Use `TypeDescriptionBuilder.setStringQualifiers(int length, boolean fixed)`.
The builder constructs the qualifier internally via `McoreFactory.eINSTANCE`,
so this path is JShell-safe (no OSGi service involved, no `modelFactory`).
Default to `length <= 100`. Do not use values greater than 100, such as `150` or `1000`, unless the user explicitly requires the larger length and the current EDT model accepts it. Recent EDT validation can reject variable string lengths outside `0..100`.

### Preflight-blocked patterns

Do not send JShell code with these EDT patterns:

```java
// WRONG: do not call setStringQualifiers with length 150, 1000, or any value above 100.

// WRONG: Ecore data types are not EDT TypeItem values.
TypeItem stringType = (TypeItem)modelFactory.create(EcorePackage.Literals.ESTRING, v8project);
TypeItem numberType = (TypeItem)modelFactory.create(EcorePackage.Literals.EINT, v8project);

// WRONG: TypeDescriptionBuilder.addType expects EDT TypeItem, not Ecore literals.
new TypeDescriptionBuilder().addType(EcorePackage.Literals.ESTRING).build();
```

Use `IEObjectProvider` and `IEObjectTypeNames`:

```java
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
```

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);

// Variable-length string up to 100 chars (safe default for EDT metadata)
TypeDescription bioType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    // ❌ .stringQualifiersLength(100)              — method does NOT exist
    // ❌ .stringQualifiersLength(100).fixed(false) — method does NOT exist
    // ❌ .setLength(100)                           — TypeDescriptionBuilder has no such method
    .build();

// Fixed-length string of 9 chars (e.g. Article code)
TypeDescription articleType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(9, true)
    .build();
```

### Number with qualifiers (scale, precision, nonNegative)

Use `TypeDescriptionBuilder.setNumberQualifiers(int scale, int precision, boolean nonNegative)`.
⚠️ `scale` (digits after the decimal point) MUST be `<= precision` (total digits),
otherwise the platform raises an SU8 error.

Do not create `NumberQualifiers` manually in JShell. Prefer the builder fluent method below:
it uses the correct EDT factory internally and avoids missing imports plus scale/precision drift.

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);

// Number(10, 2), non-negative — typical price column
TypeDescription priceType = new TypeDescriptionBuilder()
    .addType(numberType)
    .setNumberQualifiers(2, 10, true)
    // ❌ .numberQualifiersPrecision(10).numberQualifiersScale(2) — methods do NOT exist
    // ❌ .setPrecision(10).setScale(2)                           — TypeDescriptionBuilder has no such methods
    .build();

// Number(15, 4) — quantity with high precision, can be negative
TypeDescription quantityType = new TypeDescriptionBuilder()
    .addType(numberType)
    .setNumberQualifiers(4, 15, false)
    .build();

// Integer (whole number) — there is NO separate "Integer" type.
// Use Number with scale=0. EDT has NO `NumberCategory.INTEGER` enum.
TypeDescription yearType = new TypeDescriptionBuilder()
    .addType(numberType)
    .setNumberQualifiers(0, 4, false)   // scale=0 → integer up to 9999
    // ❌ NumberCategory.INTEGER     — class does NOT exist
    // ❌ IEObjectTypeNames.INTEGER  — constant does NOT exist
    // For non-negative integers (e.g. count), pass true as third argument:
    // .setNumberQualifiers(0, 5, true) → 0..99999
    .build();
```

### Binary with qualifiers (length, fixed)

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem binaryType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.BINARY_DATA);

TypeDescription blobType = new TypeDescriptionBuilder()
    .addType(binaryType)
    .setBinaryQualifiers(0, false)   // 0 = unlimited
    .build();
```

### Date with qualifiers (DateFractions)

`DateFractions` is an enum from `com._1c.g5.v8.dt.mcore`: `DATE`, `TIME`, `DATE_TIME`.

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);

TypeDescription dateOnly = new TypeDescriptionBuilder()
    .addType(dateType)
    .setDateQualifiers(DateFractions.DATE)
    .build();
```

### Catalog reference

Use generic `IEObjectTypeNames.CATALOG_REF` only for an intentionally polymorphic
"any catalog reference" value. If the requested type is concrete, for example
`CatalogRef.Контрагенты`, resolve it via `MdProducedTypesUtil.getProducedType(...)`
(NOT `typeProvider.getProxy("CatalogRef.X")` — see the critical API note above).

#### Specific catalog reference

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

Catalog suppliersDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
if (suppliersDep == null) {
    throw new IllegalStateException("Missing referenced catalog: Catalog.Контрагенты — create it first");
}
TypeItem suppliersRef = MdProducedTypesUtil.getProducedType(
    suppliersDep, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(suppliersRef)
    .build();
```

#### Generic catalog reference

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(catalogRefType)
    .build();
```

### Document reference

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem documentRefType = typeProvider.getProxy(IEObjectTypeNames.DOCUMENT_REF);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(documentRefType)
    .build();
```

### Enum reference

Use generic `IEObjectTypeNames.ENUM_REF` only for an intentionally polymorphic
"any enum reference" value. For a concrete type like `EnumRef.ВидыТоваров`,
resolve it via `MdProducedTypesUtil.getProducedType(...)`.

#### Specific enum reference

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

com._1c.g5.v8.dt.metadata.mdclass.Enum kindDep =
    (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.ВидыТоваров");
if (kindDep == null) {
    throw new IllegalStateException("Missing referenced enum: Enum.ВидыТоваров — create it first");
}
TypeItem productKindRef = MdProducedTypesUtil.getProducedType(
    kindDep, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(productKindRef)
    .build();
```

#### Generic enum reference

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem enumRefType = typeProvider.getProxy(IEObjectTypeNames.ENUM_REF);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(enumRefType)
    .build();
```

### Validate proxy before addType

For primitive types (`STRING`, `NUMBER`, ...) validate the `typeProvider.getProxy(...)` result. For metadata reference types use `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)` and validate `depMdObject` instead — the produced type call itself does not return `null` for an existing top object.

```java
// Primitive — validate the proxy
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
if (stringType == null) {
    throw new IllegalStateException("Cannot resolve STRING type");
}

// Metadata reference — validate the dep MdObject
Catalog unitsDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Units");
if (unitsDep == null) {
    throw new IllegalStateException("Missing referenced catalog: Catalog.Units — create it first");
}
TypeItem unitsRef = MdProducedTypesUtil.getProducedType(
    unitsDep, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription unitsType = new TypeDescriptionBuilder()
    .addType(unitsRef)
    .build();
```

### Rules
- Prefer a specific proxy like `CatalogRef.Products` when the business rule is narrow
- Use generic IEObjectTypeNames only when polymorphism is desired
- Build the type before assigning it to attributes, dimensions, resources, constants, or defined types
- Always validate `typeProvider.getProxy(...)` before `addType(...)`; `null` causes `IllegalArgumentException`
- For specific metadata references (`CatalogRef.X`, `EnumRef.X`, `DocumentRef.X`, ...), resolve via `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`. Do not call `typeProvider.getProxy("CatalogRef.X")` — it returns `null` in JShell. If `transaction.getTopObjectByFqn("Catalog.X")` returns `null`, throw `IllegalStateException`. Do not create a transient `McoreFactory.eINSTANCE.createType()` fallback. Use a generic type like `IEObjectTypeNames.CATALOG_REF` only when the user explicitly asked for a polymorphic reference type.
- Specific references only work for metadata objects that already exist and are visible to the current transaction
- Do not use `typeProvider.createProxy(...)`, `IDtConstants.getCatalogRefQName(...)`, or `IDtConstants.getEnumRefQName(...)`. Use `"Catalog.Name"` / `"Enum.Name"` for top-object FQNs in `transaction.getTopObjectByFqn(...)` and resolve produced `TypeItem`s via `MdProducedTypesUtil`.
- Never replace requested `CatalogRef.*` / `EnumRef.*` fields with `String`. Throw `IllegalStateException` if the referenced top object is missing.
- Set qualifiers via the builder's fluent methods — they internally use `McoreFactory.eINSTANCE` and are JShell-safe

### ❌ Anti-patterns — methods that DO NOT exist on TypeDescriptionBuilder

LLMs often invent these. None of them exist in the EDT API. Use the right name from the table.

| ❌ Hallucinated call                              | ✅ Real API                                                       |
|--------------------------------------------------|-------------------------------------------------------------------|
| `.stringQualifiersLength(N)`                     | `.setStringQualifiers(N, false)` — variable-length up to N        |
| `.stringQualifiersLength(N).stringQualifiersFixed(true)` | `.setStringQualifiers(N, true)` — fixed-length exactly N |
| `.numberQualifiersPrecision(P)`                  | `.setNumberQualifiers(scale, P, false)`                           |
| `.numberQualifiersScale(S)`                      | `.setNumberQualifiers(S, precision, false)`                       |
| `.setLength(N)` (on builder)                     | `.setStringQualifiers(N, false)` (length is a qualifier, not a top-level setter) |
| `new NumberQualifiers(...)` or manual `McoreFactory.eINSTANCE.createNumberQualifiers()` | `.setNumberQualifiers(scale, precision, nonNegative)` |
| `.dateFractions(DateFractions.DATE)`             | `.setDateQualifiers(DateFractions.DATE)`                          |
| `NumberCategory.INTEGER`                         | `.setNumberQualifiers(0, precision, false)` — scale=0 means integer |
| `IEObjectTypeNames.INTEGER`                      | `IEObjectTypeNames.NUMBER` + `setNumberQualifiers(0, ...)`        |
| `DateNumberPeriodicityity.YEAR` (typo)           | `DocumentNumberPeriodicity.YEAR` (only for documents/business processes) |

The full list of qualifier methods on `TypeDescriptionBuilder` is exactly:
`setStringQualifiers(int, boolean)`, `setBinaryQualifiers(int, boolean)`,
`setNumberQualifiers(int, int, boolean)`, `setDateQualifiers(DateFractions)`.
Nothing else for qualifiers. If you need anything beyond what these expose
(e.g. AllowedSign on numbers), set it on the resulting `TypeDescription`
manually after `build()`.

### ❌ Anti-pattern — manual qualifier creation via modelFactory

The pre-builder pattern below works but is fragile in JShell because
`modelFactory` is an OSGi-injected service and `createXxxQualifiers()` may
time out. Prefer the builder methods above.

```java
// ❌ Avoid in JShell — slow, fragile
typeDesc.setStringQualifiers(modelFactory.createStringQualifiers());
typeDesc.getStringQualifiers().setLength(50);

// ✅ Same effect, JShell-safe
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(50, false)
    .build();
```
