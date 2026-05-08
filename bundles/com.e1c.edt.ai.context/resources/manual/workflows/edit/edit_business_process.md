## Safe Workflow: Edit BusinessProcess

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit BusinessProcess") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        BusinessProcess businessProcess = (BusinessProcess)transaction.getTopObjectByFqn("BusinessProcess.Approval");
        if (businessProcess != null) {
        businessProcess.setNumberLength(11);
                businessProcess.setAutonumbering(true);
        }
        return null;
    }
});
```

### Notes
- Load the existing object by FQN from the transaction
- Do not recreate or reattach the object
- Add attributes and tabular sections through their dedicated collections and set TypeDescription on new features.
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers.
