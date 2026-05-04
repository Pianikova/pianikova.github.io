## Safe Workflow: Create Sequence

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Sequence") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Sequence sequence = mdFactory.createSequence();
        sequence.setName("SequenceSample");
        sequence.setUuid(UUID.randomUUID());
        sequence.setName("DocumentsSequence");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(sequence.eClass(), sequence.getName()).toString();
        transaction.attachTopObject((IBmObject)sequence, fqn);
        configuration.getSequences().add(sequence);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getSequences()`

### Notes
- After sequence creation, add dimensions and connect participating objects through sequence settings.
