## Safe Workflow: Create ExchangePlan

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ExchangePlan") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ExchangePlan exchangePlan = mdFactory.createExchangePlan();
        exchangePlan.setName("ExchangePlanSample");
        exchangePlan.setUuid(UUID.randomUUID());
        exchangePlan.setCodeLength(9);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(exchangePlan.eClass(), exchangePlan.getName()).toString();
        transaction.attachTopObject((IBmObject)exchangePlan, fqn);
        configuration.getExchangePlans().add(exchangePlan);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getExchangePlans()`

### Notes
- Exchange plans are top-level objects; node definitions and related metadata can be added afterwards.
