## Safe Workflow: Edit InformationRegister

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit InformationRegister") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        InformationRegister informationRegister = (InformationRegister)transaction.getTopObjectByFqn("InformationRegister.Prices");
        if (informationRegister != null) {
        register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY);
                register.setUseStandardCommands(true);
        }
        return null;
    }
});
```

### Notes
- Load the existing object by FQN from the transaction
- Do not recreate or reattach the object
- Do not call attachTopObject() for an existing register. Add or remove child objects through the existing collections.
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers.
