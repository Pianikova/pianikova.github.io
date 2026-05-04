## Safe Workflow: Create Document

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

        // Add date attribute
        DocumentAttribute dateAttr = mdFactory.createDocumentAttribute();
        dateAttr.setName("Date");
        dateAttr.getSynonym().put("ru", "Дата");
        TypeItem dateType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.DATE);
        TypeDescription dateTypeDesc = new TypeDescriptionBuilder()
            .addType(dateType)
            .build();
        dateAttr.setType(dateTypeDesc);
        document.getAttributes().add(dateAttr);

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
            .build();
        quantity.setType(quantityType);
        quantity.setUuid(UUID.randomUUID());
        products.getAttributes().add(quantity);
        document.getTabularSections().add(products);

        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)
        document.setUuid(UUID.randomUUID());
        warehouse.setUuid(UUID.randomUUID());
        dateAttr.setUuid(UUID.randomUUID());

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
- **Check before creating** to avoid `BmFqnAlreadyInUseException`
