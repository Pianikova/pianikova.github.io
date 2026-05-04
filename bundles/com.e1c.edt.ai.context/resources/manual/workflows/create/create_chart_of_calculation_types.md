## Safe Workflow: Create ChartOfCalculationTypes

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ChartOfCalculationTypes") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ChartOfCalculationTypes chart = mdFactory.createChartOfCalculationTypes();
        chart.setName("ChartOfCalculationTypesSample");
        chart.setUuid(UUID.randomUUID());
        chart.setCodeLength(10);
                chart.setDescriptionLength(100);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(chart.eClass(), chart.getName()).toString();
        transaction.attachTopObject((IBmObject)chart, fqn);
        configuration.getChartsOfCalculationTypes().add(chart);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getChartsOfCalculationTypes()`

### Notes
- CalculationRegister frequently depends on ChartOfCalculationTypes. Create the chart before assigning register references.
