## Safe Workflow: Create BusinessProcess

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create BusinessProcess") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        BusinessProcess businessProcess = mdFactory.createBusinessProcess();
        businessProcess.setName("BusinessProcessSample");
        businessProcess.setUuid(UUID.randomUUID());
        businessProcess.setAutonumbering(true);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(businessProcess.eClass(), businessProcess.getName()).toString();
        transaction.attachTopObject((IBmObject)businessProcess, fqn);
        configuration.getBusinessProcesses().add(businessProcess);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getBusinessProcesses()`

### Notes
- Add attributes and tabular sections using the generic attribute and tabular section scenarios.
