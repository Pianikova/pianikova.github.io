## Safe Workflow: Create DataProcessor

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create DataProcessor") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        DataProcessor processor = mdFactory.createDataProcessor();
        processor.setName("DataProcessorSample");
        processor.setUuid(UUID.randomUUID());
        processor.setName("DataMaintenance");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(processor.eClass(), processor.getName()).toString();
        transaction.attachTopObject((IBmObject)processor, fqn);
        configuration.getDataProcessors().add(processor);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getDataProcessors()`

### Notes
- For executable behavior, add forms or module content separately after metadata creation.
