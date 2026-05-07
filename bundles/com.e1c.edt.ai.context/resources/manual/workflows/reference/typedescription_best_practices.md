## TypeDescriptionBuilder Best Practices - Transaction Context

### ⚠️ CRITICAL: TypeDescription Must Be Created Inside Transaction

TypeDescription and TypeItem proxies MUST be created and used within the SAME BM transaction.
Creating TypeDescription outside the transaction context can lead to:
- NullPointerException when setting types
- Inconsistent type resolution
- Transaction isolation violations

### ✅ CORRECT: Complete TypeDescription Creation Pattern

```java
IV8Project v8project = projectManager.getProject(project);
globalContext.execute(new AbstractBmTask<Void>("Create metadata") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {

        // STEP 1: Get typeProvider INSIDE transaction
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // STEP 2: Get TypeItem proxies INSIDE transaction
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);

        // STEP 3: Validate proxies before building
        if (stringType == null) {
            System.err.println("ERROR: Cannot resolve STRING type");
            return null;
        }
        if (catalogRef == null) {
            System.err.println("ERROR: Cannot resolve CATALOG_REF type");
            return null;
        }

        // STEP 4: Build TypeDescription INSIDE transaction
        TypeDescription stringTypeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();

        TypeDescription catalogRefTypeDesc = new TypeDescriptionBuilder()
            .addType(catalogRef)
            .build();

        // STEP 5: Set TypeDescription to attribute INSIDE transaction
        attribute.setType(stringTypeDesc);
        referenceAttribute.setType(catalogRefTypeDesc);

        return null;
    }
});
```

### ❌ WRONG Patterns:

**Pattern 1: Getting typeProvider outside transaction**
```java
IV8Project v8project = projectManager.getProject(project);
// ❌ WRONG: TypeProvider created OUTSIDE transaction
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);

globalContext.execute(new AbstractBmTask<Void>("Create metadata") {
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        attribute.setType(new TypeDescriptionBuilder().addType(stringType).build());
        // ❌ May cause NullPointerException
        return null;
    }
});
```

**Pattern 2: Using TypeItem from different transaction**
```java
IV8Project v8project = projectManager.getProject(project);
// ❌ WRONG: TypeItem from transaction #1 used in transaction #2
TypeItem typeFromTrans1 = null;

globalContext.execute(new AbstractBmTask<Void>("Transaction 1") {
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        typeFromTrans1 = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        return null;
    }
});

globalContext.execute(new AbstractBmTask<Void>("Transaction 2") {
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        attribute.setType(new TypeDescriptionBuilder().addType(typeFromTrans1).build());
        // ❌ TypeItem is invalid for this transaction
        return null;
    }
});
```

### Frequently Used Patterns:

**Pattern 1: Simple String Type**
```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription typeDesc = new TypeDescriptionBuilder().addType(stringType).build();
attribute.setType(typeDesc);
```

**Pattern 2: Number with Qualifiers**

Use the builder's fluent `setNumberQualifiers(scale, precision, nonNegative)` —
it constructs the qualifier internally via `McoreFactory.eINSTANCE` and is
JShell-safe (NO `modelFactory` / OSGi service involved).
⚠️ scale MUST be ≤ precision (otherwise SU8 error).

```java
IV8Project v8project = projectManager.getProject(project);
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);

// Number(10, 2), non-negative — typical price/amount column
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(numberType)
    .setNumberQualifiers(2, 10, true)
    .build();
```

**Pattern 2b: String with Qualifiers**

```java
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);

// Variable-length string up to 1000 chars
TypeDescription bioType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(1000, false)
    .build();
```

The full qualifier API on `TypeDescriptionBuilder`: `setStringQualifiers(int, boolean)`,
`setBinaryQualifiers(int, boolean)`, `setNumberQualifiers(int, int, boolean)`,
`setDateQualifiers(DateFractions)`. Nothing else exists for qualifiers — see
the anti-patterns table in the `create_type_description` scenario.

**Pattern 3: Composite Type (String or Number)**
```java
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
TypeDescription compositeType = new TypeDescriptionBuilder()
    .addType(stringType)
    .addType(numberType)
    .build();
attribute.setType(compositeType);
```

**Pattern 4: Catalog Reference (Generic)**
```java
TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
TypeDescription typeDesc = new TypeDescriptionBuilder().addType(catalogRef).build();
attribute.setType(typeDesc);
```

**Pattern 5: Specific Catalog Reference (Requires existing catalog)**
```java
TypeItem productsRef = (TypeItem)typeProvider.getProxy("Catalog.Products");
if (productsRef == null) {
    System.err.println("ERROR: Catalog.Products does not exist");
    return null; // Stop creation
}
TypeDescription typeDesc = new TypeDescriptionBuilder().addType(productsRef).build();
attribute.setType(typeDesc);
```

**Pattern 6: Fallback Pattern (Specific → Generic)**
```java
TypeItem unitsRef = (TypeItem)typeProvider.getProxy("Catalog.Units");
TypeDescription typeDesc;
if (unitsRef == null) {
    System.out.println("WARNING: Catalog.Units not found, using generic CATALOG_REF");
    TypeItem catalogRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
    typeDesc = new TypeDescriptionBuilder().addType(catalogRef).build();
} else {
    typeDesc = new TypeDescriptionBuilder().addType(unitsRef).build();
}
attribute.setType(typeDesc);
```

### Performance Considerations:
- **Reuse TypeDescription**: Create once, use multiple times for same type
- **Validate early**: Check `typeProvider.getProxy(...)` before building
- **Minimize proxy calls**: Get all needed TypeItems at once
- **Stay in transaction**: All TypeDescription operations in same transaction

### Common Errors and Solutions:
**Error**: `NullPointerException` when setting type
**Solution**: Ensure typeProvider and TypeItem are created INSIDE transaction

**Error**: `IllegalArgumentException` when adding type
**Solution**: Validate `typeProvider.getProxy(...)` returns non-null before `addType(...)`

**Error**: SU8 - Scale > Precision
**Solution**: Ensure `scale <= precision` for Number types

**Error**: Type not found for specific FQN
**Solution**: Use fallback pattern or ensure referenced metadata exists first

