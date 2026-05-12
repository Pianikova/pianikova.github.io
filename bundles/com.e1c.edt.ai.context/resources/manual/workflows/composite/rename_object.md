## Rename Metadata Object (Update FQN)

```java
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Rename catalog") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");

        if (catalog != null) {
            // Use updateTopObjectFqn - NOT attachTopObject
            String newFqn = "Catalog.Goods";
            transaction.updateTopObjectFqn(catalog, newFqn);

            // Also update the object name
            catalog.setName("Goods");

            return catalog;
        }
        return null;
    }
});
```

### Required post-check

After renaming metadata, call `GetMarkers` with `marker_type: "1c"` for the project, but fix only markers on changed entities and directly affected references before reporting success.
