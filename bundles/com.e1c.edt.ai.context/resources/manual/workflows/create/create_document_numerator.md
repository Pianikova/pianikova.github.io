## Safe Workflow: Create DocumentNumerator

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create DocumentNumerator") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        DocumentNumerator numerator = mdFactory.createDocumentNumerator();
        numerator.setName("DocumentNumeratorSample");
        numerator.setUuid(UUID.randomUUID());
        numerator.setName("MainNumerator");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(numerator.eClass(), numerator.getName()).toString();
        transaction.attachTopObject((IBmObject)numerator, fqn);
        configuration.getDocumentNumerators().add(numerator);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getDocumentNumerators()`

### Notes
- Attach documents to the numerator later through document properties inside a BM transaction.
