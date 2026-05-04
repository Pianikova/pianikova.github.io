## Safe Workflow: Create CommonCommand

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create CommonCommand") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        CommonCommand command = mdFactory.createCommonCommand();
        command.setName("CommonCommandSample");
        command.setUuid(UUID.randomUUID());
        command.setName("OpenDashboard");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(command.eClass(), command.getName()).toString();
        transaction.attachTopObject((IBmObject)command, fqn);
        configuration.getCommonCommands().add(command);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getCommonCommands()`

### Notes
- Command groups and UI placement are configured separately after command creation.
