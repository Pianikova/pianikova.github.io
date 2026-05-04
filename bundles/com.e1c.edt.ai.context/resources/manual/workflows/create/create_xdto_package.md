## Safe Workflow: Create XDTOPackage

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create XDTOPackage") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        XDTOPackage packageObject = mdFactory.createXDTOPackage();
        packageObject.setName("XDTOPackageSample");
        packageObject.setUuid(UUID.randomUUID());
        packageObject.setName("CommonSchema");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(packageObject.eClass(), packageObject.getName()).toString();
        transaction.attachTopObject((IBmObject)packageObject, fqn);
        configuration.getXDTOPackages().add(packageObject);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getXDTOPackages()`

### Notes
- Add XDTO package content and imported types after the package metadata object exists.
