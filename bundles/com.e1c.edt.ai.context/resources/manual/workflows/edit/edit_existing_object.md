## Edit Existing Metadata Object

```java
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Edit catalog") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // Get EXISTING object - NO attachTopObject()
        Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");

        if (catalog != null) {
            // Modify properties directly
            catalog.setDescriptionLength(200);

            // Add new attribute
            CatalogAttribute newAttr = mdFactory.createCatalogAttribute();
            newAttr.setName("Brand");
            newAttr.setUuid(UUID.randomUUID());
            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
            TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
            TypeDescription typeDesc = new TypeDescriptionBuilder()
                .addType(stringType)
                .build();

            newAttr.setType(typeDesc);
            catalog.getAttributes().add(newAttr);

            return catalog;
        }
        return null;
    }
});
```

### Required post-check

After editing metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project. Inspect returned markers and fix new validation errors before reporting success.
