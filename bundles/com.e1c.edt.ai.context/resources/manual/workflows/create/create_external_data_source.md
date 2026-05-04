## Safe Workflow: Create ExternalDataSource

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ExternalDataSource") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ExternalDataSource source = mdFactory.createExternalDataSource();
        source.setName("ExternalDataSourceSample");
        source.setUuid(UUID.randomUUID());
        source.setName("WarehouseDwh");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(source.eClass(), source.getName()).toString();
        transaction.attachTopObject((IBmObject)source, fqn);
        configuration.getExternalDataSources().add(source);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getExternalDataSources()`

### Notes
- Tables, cubes, fields, dimensions, and resources are child objects created after the source.
