## Add Tabular Section to Existing Document

```java
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Document result = globalContext.execute(new AbstractBmTask<Document>("Add tabular section") {
    @Override
    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // Get EXISTING document - NO attachTopObject()
        Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");

        if (document != null) {
            // Create tabular section
            DocumentTabularSection products = mdFactory.createDocumentTabularSection();
            products.setName("Products");
            products.getSynonym().put("ru", "Products");
            products.setUuid(UUID.randomUUID());

            // Create tabular section attributes
            TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();
            product.setName("Product");
            product.getSynonym().put("ru", "Product");
            product.setUuid(UUID.randomUUID());

            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
            TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
            TypeDescription typeDesc = new TypeDescriptionBuilder()
                .addType(catalogRefType)
                .build();

            product.setType(typeDesc);

            products.getAttributes().add(product);
            document.getTabularSections().add(products);

            return document;
        }
        return null;
    }
});
```
