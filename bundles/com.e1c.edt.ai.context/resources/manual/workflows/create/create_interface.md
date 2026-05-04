## Safe Workflow: Create Interface

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Interface") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Interface interfaceObject = mdFactory.createInterface();
        interfaceObject.setName("InterfaceSample");
        interfaceObject.setUuid(UUID.randomUUID());
        interfaceObject.setName("MainInterface");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(interfaceObject.eClass(), interfaceObject.getName()).toString();
        transaction.attachTopObject((IBmObject)interfaceObject, fqn);
        configuration.getInterfaces().add(interfaceObject);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getInterfaces()`

### Notes
- Configure sections, command interface, and navigation structure after the top-level interface is attached.
