## Safe Workflow: Create FunctionalOptionsParameter

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create FunctionalOptionsParameter") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        FunctionalOptionsParameter parameter = mdFactory.createFunctionalOptionsParameter();
        parameter.setName("FunctionalOptionsParameterSample");
        parameter.setUuid(UUID.randomUUID());
        parameter.setName("CurrentCompany");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(parameter.eClass(), parameter.getName()).toString();
        transaction.attachTopObject((IBmObject)parameter, fqn);
        configuration.getFunctionalOptionsParameters().add(parameter);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getFunctionalOptionsParameters()`

### Notes
- After creation, link the parameter to functional options and configure dependent expressions.
