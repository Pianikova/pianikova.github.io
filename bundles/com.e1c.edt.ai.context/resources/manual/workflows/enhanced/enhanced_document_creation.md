## Enhanced Workflow: Create Document with Full Validation

This workflow provides comprehensive document creation with:
- Existence check before creation
- UUID assignment for all objects (document, attributes, tabular sections, line attributes)
- Type validation and error handling
- Complete validation before attachment

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Document result = globalContext.execute(new AbstractBmTask<Document>("Create document safely") {
    @Override
    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // CHECK 1: Document existence
        String documentFqn = "Document.GoodsReceipt";
        if (transaction.getTopObjectByFqn(documentFqn) != null) {
            System.err.println("ERROR: Document already exists: " + documentFqn);
            return null;
        }

        // CREATE document
        Document document = mdFactory.createDocument();
        document.setName("GoodsReceipt");
        document.getSynonym().put("ru", "Приход товаров");

        // SET document properties
        document.setNumberType(DocumentNumberType.NUMBER);
        document.setNumberLength(9);
        document.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);
        document.setRealTimePosting(RealTimePosting.DENY);

        // ASSIGN UUID to document
        try {
            document.setUuid(UUID.randomUUID());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for document: " + e.getMessage());
            return null;
        }

        // CREATE type provider
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // CREATE attributes with UUID assignment
        DocumentAttribute warehouse = mdFactory.createDocumentAttribute();
        warehouse.setName("Warehouse");
        warehouse.getSynonym().put("ru", "Склад");
        try {
            warehouse.setUuid(UUID.randomUUID()); // CRITICAL: UUID for all child objects
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for Warehouse attribute: " + e.getMessage());
            return null;
        }

        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription warehouseType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();
        warehouse.setType(warehouseType);
        document.getAttributes().add(warehouse);

        // CREATE tabular section
        DocumentTabularSection products = mdFactory.createDocumentTabularSection();
        products.setName("Products");
        products.getSynonym().put("ru", "Товары");
        try {
            products.setUuid(UUID.randomUUID()); // CRITICAL: UUID for tabular section
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for Products tabular section: " + e.getMessage());
            return null;
        }

        // CREATE tabular section attributes with UUID assignment
        TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();
        product.setName("Product");
        product.getSynonym().put("ru", "Номенклатура");
        try {
            product.setUuid(UUID.randomUUID()); // CRITICAL: UUID for line attributes
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for Product attribute: " + e.getMessage());
            return null;
        }

        TypeDescription productType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();
        product.setType(productType);
        products.getAttributes().add(product);

        TabularSectionAttribute quantity = mdFactory.createTabularSectionAttribute();
        quantity.setName("Quantity");
        quantity.getSynonym().put("ru", "Количество");
        try {
            quantity.setUuid(UUID.randomUUID()); // CRITICAL: UUID for line attributes
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for Quantity attribute: " + e.getMessage());
            return null;
        }

        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription quantityType = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();
        quantity.setType(quantityType);
        products.getAttributes().add(quantity);
        document.getTabularSections().add(products);

        // VALIDATE before attachment
        if (document.getAttributes().isEmpty()) {
            System.err.println("ERROR: Document has no attributes");
        }
        if (document.getTabularSections().isEmpty()) {
            System.out.println("WARNING: Document has no tabular sections");
        }

        // ATTACH and add to configuration
        String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();
        try {
            transaction.attachTopObject((IBmObject)document, fqn);
            configuration.getDocuments().add(document);
            System.out.println("SUCCESS: Document created: " + fqn);
            return document;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to attach document: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
});
```

### UUID Assignment Checklist for Documents:
- [ ] Document top-level object: `document.setUuid(UUID.randomUUID())`
- [ ] All DocumentAttribute objects: `attr.setUuid(UUID.randomUUID())`
- [ ] All DocumentTabularSection objects: `ts.setUuid(UUID.randomUUID())`
- [ ] All TabularSectionAttribute objects: `tsa.setUuid(UUID.randomUUID())`
- [ ] Wrap ALL UUID assignments in try-catch blocks
- [ ] Abort creation if any UUID assignment fails

