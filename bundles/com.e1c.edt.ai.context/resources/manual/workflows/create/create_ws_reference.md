## Safe Workflow: Create WSReference

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create WSReference") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        WSReference reference = mdFactory.createWSReference();
        reference.setName("WSReferenceSample");
        reference.setUuid(UUID.randomUUID());
        reference.setName("ExternalSoapService");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(reference.eClass(), reference.getName()).toString();
        transaction.attachTopObject((IBmObject)reference, fqn);
        configuration.getWsReferences().add(reference);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getWsReferences()`

### Notes
- After creation, fill endpoint and service metadata according to the referenced WSDL contract.
