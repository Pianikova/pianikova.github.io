## Safe Workflow: Create Style

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Style") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Style style = mdFactory.createStyle();
        style.setName("StyleSample");
        style.setUuid(UUID.randomUUID());
        style.setName("CorporateStyle");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(style.eClass(), style.getName()).toString();
        transaction.attachTopObject((IBmObject)style, fqn);
        configuration.getStyles().add(style);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getStyles()`

### Notes
- After creation, assign the style in configuration defaults or reference it from forms and UI elements.
