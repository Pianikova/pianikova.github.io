## Scenario: Create TypeDescription

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
`CatalogRef.Контрагенты`, use `typeProvider.getProxy("CatalogRef.Контрагенты")`
and stop if it is not found.

#### Specific catalog reference

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem suppliersRef = (TypeItem)typeProvider.getProxy("CatalogRef.Контрагенты");
if (suppliersRef == null) {
    throw new IllegalStateException("CatalogRef.Контрагенты is not available yet. Create Catalog.Контрагенты first, run a scoped marker check, let EDT refresh produced types, then retry exact typeProvider.getProxy(\"CatalogRef.Контрагенты\").");
}
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
"any enum reference" value. If the requested type is concrete, for example
`EnumRef.ВидыТоваров`, use `typeProvider.getProxy("EnumRef.ВидыТоваров")`.

#### Specific enum reference

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem productKindRef = (TypeItem)typeProvider.getProxy("EnumRef.ВидыТоваров");
if (productKindRef == null) {
    throw new IllegalStateException("EnumRef.ВидыТоваров is not available yet. Create Enum.ВидыТоваров first, run a scoped marker check, let EDT refresh produced types, then retry exact typeProvider.getProxy(\"EnumRef.ВидыТоваров\").");
}
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

```java
TypeItem unitsRef = (TypeItem)typeProvider.getProxy("CatalogRef.Units");
if (unitsRef == null) {
    System.err.println("ERROR: Cannot resolve type proxy CatalogRef.Units");
    return null;
}
TypeDescription unitsType = new TypeDescriptionBuilder()
    .addType(unitsRef)
    .build();
```

### Rules
- Prefer a specific proxy like `CatalogRef.Products` when the business rule is narrow
- Use generic IEObjectTypeNames only when polymorphism is desired
- Build the type before assigning it to attributes, dimensions, resources, constants, or defined types
- Always validate `typeProvider.getProxy(...)` before `addType(...)`; `null` causes `IllegalArgumentException`
- If a specific metadata proxy like `"CatalogRef.Units"` is null, do not call `addType(null)`. If the referenced top object exists but the dynamic type index is not ready, do not create a transient `McoreFactory.eINSTANCE.createType()` fallback. Validate the referenced object, let EDT refresh produced types, then retry exact `typeProvider.getProxy(...)`. Use a generic type such as `IEObjectTypeNames.CATALOG_REF` only when the user explicitly asked for polymorphic reference type.
- Specific references only work for metadata objects that already exist and are visible to the current transaction
- Do not use `typeProvider.createProxy(...)`, `IDtConstants.getCatalogRefQName(...)`, or `IDtConstants.getEnumRefQName(...)`; use `typeProvider.getProxy("CatalogRef.Name")`, `typeProvider.getProxy("EnumRef.Name")`, etc. Use `"Catalog.Name"` / `"Enum.Name"` only for top-object metadata FQNs.
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
