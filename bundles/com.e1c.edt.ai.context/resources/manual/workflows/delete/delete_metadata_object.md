## Scenario: Delete Metadata Object

### Safe pattern
```java
globalContext.execute(new AbstractBmTask<Void>("Delete object") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (catalog != null) {
            configuration.getCatalogs().remove(catalog);
            transaction.detachTopObject((IBmObject)catalog);
        }
        return null;
    }
});
```

### Rules
- remove top-level objects from the correct `Configuration` collection first
- then call `transaction.detachTopObject(...)`
- do not use `EcoreUtil.delete()` for top-level metadata objects
- for child objects, remove them from the owning collection instead of detaching top-level state
