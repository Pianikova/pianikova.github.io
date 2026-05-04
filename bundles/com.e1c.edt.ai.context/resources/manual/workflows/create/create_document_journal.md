## Safe Workflow: Create DocumentJournal

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create DocumentJournal") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        DocumentJournal journal = mdFactory.createDocumentJournal();
        journal.setName("DocumentJournalSample");
        journal.setUuid(UUID.randomUUID());
        journal.setName("DocumentsJournal");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(journal.eClass(), journal.getName()).toString();
        transaction.attachTopObject((IBmObject)journal, fqn);
        configuration.getDocumentJournals().add(journal);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getDocumentJournals()`

### Notes
- DocumentJournal collects document views; related columns and commands can be configured later.
