## Safe Workflow: Create CommonModule

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create CommonModule") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        CommonModule commonModule = mdFactory.createCommonModule();
        commonModule.setName("CommonModuleSample");
        commonModule.setUuid(UUID.randomUUID());
        commonModule.setServer(true);
                commonModule.setServerCall(true);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(commonModule.eClass(), commonModule.getName()).toString();
        transaction.attachTopObject((IBmObject)commonModule, fqn);
        configuration.getCommonModules().add(commonModule);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getCommonModules()`

### Notes
- After metadata creation, create or update the corresponding Module.bsl file through file tools. Metadata flags and BSL text are separate layers.
