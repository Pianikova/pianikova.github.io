## Safe Workflow: Edit CommonModule

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit CommonModule") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        CommonModule commonModule = (CommonModule)transaction.getTopObjectByFqn("CommonModule.WorkingWithData");
        if (commonModule != null) {
            commonModule.setServer(true);
            commonModule.setServerCall(true);
        }
        return null;
    }
});
```

### Notes
- Load the existing object by FQN from the transaction
- Do not recreate or reattach the object
- Use the listed setters for edits. Do not invent boolean getters such as `getServer()`, `getServerCall()`, `getClient()`, or `getClientManagedForm()`; use one batch `JShellReflection` for flag reads if exact accessor names are needed.
- Metadata flags and BSL source are separate concerns. Update Module.bsl through file tools, not through mdFactory.
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers.
