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
- For String types, use buildStringTypeDescription() or buildStringTypeWithQualifiersDescription()
- For Number types, use buildNumberTypeDescription() or buildNumberTypeWithQualifiersDescription()

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
- Set type or qualifiers using modelFactory

**Step 4: Verify fix**
- Refresh/Rebuild project in EDT
- Check markers view for remaining errors

**Step 5: Consider project-wide fixes**
- If multiple objects have same error, iterate through collections
- Use Configuration object to access all catalogs, documents, registers

**Important Reminders:**
- TypeDescription and TypeItem must be created INSIDE the transaction
- Use `modelFactory` for creating qualifiers in JShell context
- Set UUIDs manually when creating new metadata objects
- For existing objects: modify directly, don't use attachTopObject()
- Check that Scale <= Precision for all Number types
- Verify that all attributes have valid TypeDescription set
