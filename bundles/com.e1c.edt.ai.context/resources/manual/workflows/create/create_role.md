## Safe Workflow: Create Role

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Role") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Role role = mdFactory.createRole();
        role.setName("RoleSample");
        role.setUuid(UUID.randomUUID());
        role.setName("PowerUser");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(role.eClass(), role.getName()).toString();
        transaction.attachTopObject((IBmObject)role, fqn);
        configuration.getRoles().add(role);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getRoles()`

### Notes
- Rights matrices and permissions are configured after the role object exists.
