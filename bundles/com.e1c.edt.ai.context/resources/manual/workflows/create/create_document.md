## Safe Workflow: Create Document

Before writing code, decide which attributes are custom. Do not create document attributes named `Date`, `Дата`,
`Number`, `Номер`, `Posted`, `Проведен`, `Ref`, `Ссылка`, `DeletionMark`, or `ПометкаУдаления`: these are standard
document properties and EDT reports SU45 name/type markers if they are added as custom attributes.

When a document has many attributes, prefer small helper methods that return a NEW `TypeDescription` on every call.
Never store one `TypeDescription` and assign it to two attributes.

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Document document = globalContext.execute(new AbstractBmTask<Document>("Create document") {
    @Override
    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // Check if document already exists to avoid BmFqnAlreadyInUseException
        String documentFqn = "Document.GoodsReceipt";
        if (transaction.getTopObjectByFqn(documentFqn) != null) {
            System.out.println("Document already exists: " + documentFqn);
            return null;
        }

        // Create document
        Document document = mdFactory.createDocument();
        document.setName("GoodsReceipt");
        document.getSynonym().put("ru", "Приход товаров");

        // Set document number type - IMPORTANT: use correct enum constant
        document.setNumberType(DocumentNumberType.NUMBER);
        document.setNumberLength(9);
        document.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);
        document.setRealTimePosting(RealTimePosting.DENY);

        // Create type provider INSIDE transaction
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // Add warehouse attribute (Catalog reference)
        DocumentAttribute warehouse = mdFactory.createDocumentAttribute();
        warehouse.setName("Warehouse");
        warehouse.getSynonym().put("ru", "Склад");
        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription warehouseType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();
        warehouse.setType(warehouseType);
        document.getAttributes().add(warehouse);

        // ⚠️ WARNING: Do NOT create "Date" attribute - it conflicts with standard document property
        // Documents have standard attributes: Date, Number, Posted, Ref - these are built-in
        // Add custom attributes only (do not use names: Date, Number, Posted, DeletionMark, Ref)

        // Add tabular section with typed line attributes
        DocumentTabularSection products = mdFactory.createDocumentTabularSection();
        products.setName("Products");
        products.getSynonym().put("ru", "Товары");
        products.setUuid(UUID.randomUUID());

        TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();
        product.setName("Product");
        product.getSynonym().put("ru", "Номенклатура");
        TypeDescription productType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();
        product.setType(productType);
        product.setUuid(UUID.randomUUID());
        products.getAttributes().add(product);

        TabularSectionAttribute quantity = mdFactory.createTabularSectionAttribute();
        quantity.setName("Quantity");
        quantity.getSynonym().put("ru", "Количество");
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription quantityType = new TypeDescriptionBuilder()
            .addType(numberType)
            .setNumberQualifiers(0, 10, false)
            .build();
        quantity.setType(quantityType);
        quantity.setUuid(UUID.randomUUID());
        products.getAttributes().add(quantity);
        document.getTabularSections().add(products);

        // Validate all child TypeDescription values before attach.
        // TypeDescription is containment: never reuse the same instance across children.
        for (DocumentAttribute attribute : document.getAttributes()) {
            if (attribute.getType() == null || attribute.getType().getTypes().isEmpty()) {
                System.err.println("ERROR: Missing TypeDescription for document attribute: " + attribute.getName());
                return null;
            }
        }
        for (DocumentTabularSection section : document.getTabularSections()) {
            for (TabularSectionAttribute attribute : section.getAttributes()) {
                if (attribute.getType() == null || attribute.getType().getTypes().isEmpty()) {
                    System.err.println("ERROR: Missing TypeDescription for tabular section attribute: "
                        + section.getName() + "." + attribute.getName());
                    return null;
                }
            }
        }

        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)
        document.setUuid(UUID.randomUUID());
        warehouse.setUuid(UUID.randomUUID());

        // Generate FQN and attach to transaction
        String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();
        transaction.attachTopObject((IBmObject)document, fqn);
        configuration.getDocuments().add(document);

        System.out.println("Document created successfully: " + fqn);
        return document;
    }
});
```
**IMPORTANT Notes:**
- **DocumentNumberType.NUMBER** (not `Number`) - use correct enum constant
- **DocumentNumberPeriodicity.NONPERIODICAL** (not `Nonperiodical`) - use correct enum constant
- **Do not call `document.setPosted(...)`** - this method is not present in EDT API
- **`setRealTimePosting(...)` expects `RealTimePosting` enum** such as `RealTimePosting.DENY` or `RealTimePosting.ALLOW`
- **TypeDescriptionBuilder** must be used INSIDE the transaction
- **IEObjectProvider** must use `v8project.getVersion()` for version compatibility
- **UUIDs** MUST be set for document and all attributes to avoid SU45 errors
- **Every `DocumentAttribute` and `TabularSectionAttribute` must call `setType(...)`** before `add(...)`; otherwise EDT reports `md-legacy-emf-check` / `type is required`
- **Never reuse the same `TypeDescription` instance for multiple children.** `TypeDescription` is containment; assigning it to another attribute moves it away from the previous owner and causes `type is required` markers. Reuse `TypeItem`, not `TypeDescription`.
- **For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first.** For `Number(10,2)`, call `.setNumberQualifiers(2, 10, false)`, not `.setNumberQualifiers(10, 2, false)`.
- **Before `attachTopObject`, verify all document attributes and tabular section attributes have non-null/non-empty `getType()`**
- **Check before creating** to avoid `BmFqnAlreadyInUseException`

**⚠️ CRITICAL: Standard Document Attributes**
- **NEVER create custom attributes with names matching standard document properties**
- Standard document attributes (built-in, cannot be overridden): `Date`, `Number`, `Posted`, `Ref`, `DeletionMark`
- Russian standard names are also reserved: `Дата`, `Номер`, `Проведен`, `Ссылка`, `ПометкаУдаления`
- Trying to create an attribute named "Date" will cause validation error: "Некорректное значение свойства \"name\" реквизита \"Date\". Совпадает с именем стандартного реквизита"
- Only create custom attributes with unique names (e.g., Warehouse, Customer, Amount, etc.)

**Register Registers for Accumulation/Accounting Registers:**
- After creating a document, you can add registers it records to via `document.getRegisterRecords().add(register)`
- Example:
```java
AccumulationRegister stockRegister = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
if (stockRegister != null) {
    document.getRegisterRecords().add(stockRegister);
}
```
- Registers are configured on documents, not on registers themselves
- Each document can record to multiple registers (accumulation, accounting, information, calculation)

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project and fix new validation markers before reporting success.
