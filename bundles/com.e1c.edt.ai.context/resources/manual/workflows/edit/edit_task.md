## Safe Workflow: Edit Task

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit Task") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Task task = (Task)transaction.getTopObjectByFqn("Task.SupportRequest");
        if (task != null) {
        InformationRegister addressing = (InformationRegister)transaction.getTopObjectByFqn("InformationRegister.TaskAddressing");
                task.setAddressing(addressing);
                task.setAutonumbering(true);
        }
        return null;
    }
});
```

### Notes
- Load the existing object by FQN from the transaction
- Do not recreate or reattach the object
- Use the generic attribute/tabular-section scenarios when you need to modify child collections.
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers.
