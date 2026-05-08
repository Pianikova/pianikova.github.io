## Safe Workflow: Edit ChartOfAccounts

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit ChartOfAccounts") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        ChartOfAccounts chartOfAccounts = (ChartOfAccounts)transaction.getTopObjectByFqn("ChartOfAccounts.MainChart");
        if (chartOfAccounts != null) {
        chart.setCodeLength(10);
        chart.setDescriptionLength(100);
        }
        return null;
    }
});
```

### Notes
- Load the existing object by FQN from the transaction
- Do not recreate or reattach the object
- Be careful with references from AccountingRegister and dependent objects when renaming or restructuring the chart.
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers.
