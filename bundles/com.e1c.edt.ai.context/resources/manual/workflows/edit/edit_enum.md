## Safe Workflow: Edit Enum

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit Enum") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Enum enum = (Enum)transaction.getTopObjectByFqn("Enum.Statuses");
        if (enum != null) {
        enumObject.getEnumValues().get(0).setName("Active");
        }
        return null;
    }
});
```

### Notes
- Load the existing object by FQN from the transaction
- Do not recreate or reattach the object
- Prefer changing value names/descriptions in-place. For deleted values, remove them from enumObject.getEnumValues().
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers.
