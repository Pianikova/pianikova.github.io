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

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);

// Variable-length string up to 1000 chars (e.g. Биография, Description)
TypeDescription bioType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(1000, false)
    // ❌ .stringQualifiersLength(1000)              — method does NOT exist
    // ❌ .stringQualifiersLength(1000).fixed(false) — method does NOT exist
    // ❌ .setLength(1000)                           — TypeDescriptionBuilder has no such method
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
TypeItem unitsRef = (TypeItem)typeProvider.getProxy("Catalog.Units");
if (unitsRef == null) {
    System.err.println("ERROR: Cannot resolve type proxy Catalog.Units");
    return null;
}
TypeDescription unitsType = new TypeDescriptionBuilder()
    .addType(unitsRef)
    .build();
```

### Rules
- Prefer a specific proxy like `Catalog.Products` when the business rule is narrow
- Use generic IEObjectTypeNames only when polymorphism is desired
- Build the type before assigning it to attributes, dimensions, resources, constants, or defined types
- Always validate `typeProvider.getProxy(...)` before `addType(...)`; `null` causes `IllegalArgumentException`
- Specific references only work for metadata objects that already exist and are visible to the current transaction
- When a specific proxy is unavailable, fall back to a generic type like `IEObjectTypeNames.CATALOG_REF`
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
