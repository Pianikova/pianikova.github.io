## Safe Workflow: Edit Enum

`Enum` conflicts with `java.lang.Enum` in JShell. Always use the fully-qualified
EDT type `com._1c.g5.v8.dt.metadata.mdclass.Enum`.

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit Enum") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        com._1c.g5.v8.dt.metadata.mdclass.Enum enumObject =
            (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.Statuses");
        if (enumObject != null) {
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
- Never write `Enum enumObject` in JShell; it is ambiguous with `java.lang.Enum`
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers.
