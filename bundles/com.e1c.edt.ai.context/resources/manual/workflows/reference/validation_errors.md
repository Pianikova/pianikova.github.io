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
- For qualified types, use `TypeDescriptionBuilder` fluent methods:
  `setStringQualifiers(length, fixed)`,
  `setNumberQualifiers(scale, precision, nonNegative)`,
  `setBinaryQualifiers(length, fixed)`, or
  `setDateQualifiers(DateFractions)`
- Do not use `modelFactory` to create type qualifiers in JShell snippets; it is
  slower, easier to misuse, and may hit OSGi timeout issues

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
- Call `GetMarkers` with `marker_type: "1c"` or check EDT markers/problems view
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
- Call `GetMarkers` again with `marker_type: "1c"`
- Check returned markers for remaining errors

**Step 5: Consider project-wide fixes**
- If multiple objects have same error, iterate through collections
- Use Configuration object to access all catalogs, documents, registers

**Important Reminders:**
- TypeDescription and TypeItem must be created INSIDE the transaction
- Use `TypeDescriptionBuilder` for qualifiers in JShell context
- Set UUIDs manually when creating new metadata objects
- For existing objects: modify directly, don't use attachTopObject()
- Check that Scale <= Precision for all Number types; `setNumberQualifiers(scale, precision, nonNegative)` uses scale first
- Verify that all attributes have valid TypeDescription set

### Standard Document Attribute Names Cannot Be Overridden

**Error Message:** "Некорректное значение свойства \"name\" реквизита \"<имя>\". Совпадает с именем стандартного реквизита"

**Problem:** Trying to create a document attribute with a name that matches a built-in standard document property.

**Common Causes:**
- Creating a custom attribute named "Date"
- Creating a custom attribute named "Number", "Posted", "DeletionMark", or "Ref"
- Creating localized standard-name attributes such as "Дата", "Номер", "Проведен", "ПометкаУдаления", or "Ссылка"

**Standard Document Attributes (Built-in, Cannot Override):**
- `Date` - Document date (standard property, always exists)
- `Number` - Document number (standard property, always exists)
- `Posted` - Posted status (standard property)
- `Ref` - Document reference (standard property)
- `DeletionMark` - Deletion mark (standard property)
- `Дата`, `Номер`, `Проведен`, `Ссылка`, `ПометкаУдаления` - localized standard names are also reserved

**Fix Pattern:**
```java
// ❌ WRONG - This causes validation error
DocumentAttribute dateAttr = mdFactory.createDocumentAttribute();
dateAttr.setName("Дата"); // ❌ Conflicts with standard attribute!
dateAttr.setType(dateTypeDesc);
document.getAttributes().add(dateAttr);
// Error: "Некорректное значение свойства \"name\" реквизита \"Дата\". Совпадает с именем стандартного реквизита"

// ✅ CORRECT - Use a different name
DocumentAttribute documentDate = mdFactory.createDocumentAttribute();
documentDate.setName("ДатаДокумента"); // ✅ Unique name
documentDate.setType(dateTypeDesc);
document.getAttributes().add(documentDate);
```

**Important Notes:**
- Never create custom attributes with names matching standard document properties
- Standard attributes are automatically provided by the platform
- Only create custom attributes with unique, descriptive names
- Common custom attribute names: Warehouse, Customer, Amount, Counterparty, etc.

### Accumulation/Accounting/Calculation Registers Must Have Registrars

**Error Message:** "Некорректный состав регистраторов регистра. Ни один из документов не является регистратором для регистра"

**Problem:** Creating an AccumulationRegister, AccountingRegister, or CalculationRegister without any document registrars.

**Common Causes:**
- Creating a register and not configuring any document to record to it
- Misunderstanding where registrars are configured (on documents, not on registers)

**Understanding Registrars:**
- Registers MUST have at least one document that records to them
- Registrars are configured on the **DOCUMENT** side, not on the **REGISTER** side
- Documents record to registers via `Document.getRegisterRecords().add(register)`
- Registers do NOT have a method to list their registrars (no `getRegisteredDocuments()`)

**Fix Pattern:**
```java
// Step 1: Create the register
AccumulationRegister register = mdFactory.createAccumulationRegister();
register.setName("GoodsInStock");
register.setRegisterType(AccumulationRegisterType.BALANCE);
// ... add dimensions and resources ...
register.setUuid(UUID.randomUUID());
String registerFqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
transaction.attachTopObject((IBmObject)register, registerFqn);
configuration.getAccumulationRegisters().add(register);

// Step 2: Create a document
Document document = mdFactory.createDocument();
document.setName("GoodsReceipt");
// ... configure document ...
document.setUuid(UUID.randomUUID());
String documentFqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();
transaction.attachTopObject((IBmObject)document, documentFqn);
configuration.getDocuments().add(document);

// Step 3: Add the register as a registrar for the document
// ⚠️ CRITICAL: This step is REQUIRED to avoid validation error
document.getRegisterRecords().add(register);
```

**Important Notes:**
- AccumulationRegister, AccountingRegister, and CalculationRegister MUST have registrars
- InformationRegister does NOT need registrars (periodic data, not document movements)
- Multiple documents can record to the same register
- One document can record to multiple registers
- Always configure registrars on the document side via `document.getRegisterRecords().add(register)`

**Valid Register Types for Document Registrars:**
- ✅ `AccumulationRegister` - CAN be a registrar (stock/quantity tracking)
- ✅ `AccountingRegister` - CAN be a registrar (chart of accounts based)
- ✅ `CalculationRegister` - CAN be a registrar (payroll and calculations)
- ❌ `InformationRegister` - CANNOT be a registrar (periodic data only)
