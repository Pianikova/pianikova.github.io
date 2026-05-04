## Safe Workflow: Create ChartOfAccounts

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ChartOfAccounts") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ChartOfAccounts chart = mdFactory.createChartOfAccounts();
        chart.setName("ChartOfAccountsSample");
        chart.setUuid(UUID.randomUUID());
        chart.setCodeLength(10);
                chart.setDescriptionLength(100);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(chart.eClass(), chart.getName()).toString();
        transaction.attachTopObject((IBmObject)chart, fqn);
        configuration.getChartsOfAccounts().add(chart);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getChartsOfAccounts()`

### Notes
- ChartOfAccounts is commonly referenced by AccountingRegister. Create the chart first if registers depend on it.
