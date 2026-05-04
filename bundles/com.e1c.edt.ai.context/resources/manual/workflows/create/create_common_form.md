## Safe Workflow: Create CommonForm

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create CommonForm") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        CommonForm form = mdFactory.createCommonForm();
        form.setName("CommonFormSample");
        form.setUuid(UUID.randomUUID());
        form.setName("UniversalSearchForm");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(form.eClass(), form.getName()).toString();
        transaction.attachTopObject((IBmObject)form, fqn);
        configuration.getCommonForms().add(form);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getCommonForms()`

### Notes
- Form structure and controls are separate layers. Start by creating the top-level metadata object.
