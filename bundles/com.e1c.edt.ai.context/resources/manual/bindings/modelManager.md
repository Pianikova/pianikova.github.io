## IBmModelManager

Provides BM model and editing context for metadata transactions. Use `globalContext.execute()` for all read/write operations.

### Reading existing objects:
```java
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Read catalog") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        return (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
    }
});
```

### Creating new objects:
```java
Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Create catalog") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration config = (Configuration)transaction.getTopObjectByFqn("Configuration");

        Catalog catalog = mdFactory.createCatalog();
        catalog.setName("NewCatalog");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
        transaction.attachTopObject((IBmObject)catalog, fqn);
        config.getCatalogs().add(catalog);
        return catalog;
    }
});
```
