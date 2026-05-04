## Safe Workflow: Create Report

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Report") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Report report = mdFactory.createReport();
        report.setName("ReportSample");
        report.setUuid(UUID.randomUUID());
        report.setName("SalesAnalysis");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(report.eClass(), report.getName()).toString();
        transaction.attachTopObject((IBmObject)report, fqn);
        configuration.getReports().add(report);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getReports()`

### Notes
- Metadata object creation does not create layouts, forms, or module code automatically.
