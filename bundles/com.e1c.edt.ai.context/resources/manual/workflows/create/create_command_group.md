## Safe Workflow: Create CommandGroup

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create CommandGroup") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        CommandGroup group = mdFactory.createCommandGroup();
        group.setName("CommandGroupSample");
        group.setUuid(UUID.randomUUID());
        group.setName("SalesCommands");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(group.eClass(), group.getName()).toString();
        transaction.attachTopObject((IBmObject)group, fqn);
        configuration.getCommandGroups().add(group);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getCommandGroups()`

### Notes
- Use command groups to organize common commands and UI navigation after the group object exists.
