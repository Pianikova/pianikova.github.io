## Safe Workflow: Create SettingsStorage

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create SettingsStorage") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        SettingsStorage storage = mdFactory.createSettingsStorage();
        storage.setName("SettingsStorageSample");
        storage.setUuid(UUID.randomUUID());
        storage.setName("CommonSettingsStorage");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(storage.eClass(), storage.getName()).toString();
        transaction.attachTopObject((IBmObject)storage, fqn);
        configuration.getSettingsStorages().add(storage);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getSettingsStorages()`

### Notes
- Configuration references such as commonSettingsStorage or reportsVariantsStorage should point to this object afterwards.
