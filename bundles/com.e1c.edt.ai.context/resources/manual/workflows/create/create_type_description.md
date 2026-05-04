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

### String with qualifiers

```java
IV8Project v8project = projectManager.getProject(project);
// Create String type with length qualifier
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();

// Set string qualifiers (length)
// Note: StringQualifiers must be set on the TypeDescription, not passed to builder
// StringQualifiers stringQualifiers = modelFactory.createStringQualifiers();
// stringQualifiers.setLength(50);
// typeDesc.setStringQualifiers(stringQualifiers);

// Simplified: just set length on TypeDescription's qualifiers
if (typeDesc.getStringQualifiers() == null) {
    typeDesc.setStringQualifiers(modelFactory.createStringQualifiers());
}
typeDesc.getStringQualifiers().setLength(50);
```

### Number with qualifiers

```java
IV8Project v8project = projectManager.getProject(project);
// Create Number type with precision and scale qualifiers
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(numberType)
    .build();

// Set number qualifiers (precision, scale)
// ⚠️ CRITICAL: Scale must be <= Precision, otherwise SU8 error occurs
// Precision = total number of digits (including decimal places)
// Scale = number of digits after decimal point
if (typeDesc.getNumberQualifiers() == null) {
    typeDesc.setNumberQualifiers(modelFactory.createNumberQualifiers());
}
typeDesc.getNumberQualifiers().setPrecision(10);
typeDesc.getNumberQualifiers().setScale(2);
// This creates a Number(10, 2) type: up to 10 total digits, 2 after decimal point
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
