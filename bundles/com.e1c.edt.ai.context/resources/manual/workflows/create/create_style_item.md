## Safe Workflow: Create StyleItem

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create StyleItem") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        StyleItem styleItem = mdFactory.createStyleItem();
        styleItem.setName("StyleItemSample");
        styleItem.setUuid(UUID.randomUUID());
        styleItem.setName("PrimaryButton");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(styleItem.eClass(), styleItem.getName()).toString();
        transaction.attachTopObject((IBmObject)styleItem, fqn);
        configuration.getStyleItems().add(styleItem);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getStyleItems()`

### Notes
- StyleItem visual type and concrete appearance settings should be configured explicitly after creation.
