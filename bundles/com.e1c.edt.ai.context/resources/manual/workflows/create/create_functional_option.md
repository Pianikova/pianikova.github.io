## Safe Workflow: Create FunctionalOption

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create FunctionalOption") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        FunctionalOption option = mdFactory.createFunctionalOption();
        option.setName("FunctionalOptionSample");
        option.setUuid(UUID.randomUUID());
        option.setName("UseAdvancedPricing");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(option.eClass(), option.getName()).toString();
        transaction.attachTopObject((IBmObject)option, fqn);
        configuration.getFunctionalOptions().add(option);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getFunctionalOptions()`

### Notes
- Bind commands, forms, or object availability to the option after it is created.
