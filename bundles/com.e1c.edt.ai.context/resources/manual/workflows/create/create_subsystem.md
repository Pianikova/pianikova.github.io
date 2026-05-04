## Safe Workflow: Create Subsystem

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Subsystem") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Subsystem subsystem = mdFactory.createSubsystem();
        subsystem.setName("SubsystemSample");
        subsystem.setUuid(UUID.randomUUID());
        subsystem.setName("Sales");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(subsystem.eClass(), subsystem.getName()).toString();
        transaction.attachTopObject((IBmObject)subsystem, fqn);
        configuration.getSubsystems().add(subsystem);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getSubsystems()`

### Notes
- Configuration keeps subsystem references; command interfaces and object composition are follow-up steps.
