## Safe Workflow: Create CommonTemplate

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create CommonTemplate") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        CommonTemplate template = mdFactory.createCommonTemplate();
        template.setName("CommonTemplateSample");
        template.setUuid(UUID.randomUUID());
        template.setName("PrintLayout");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(template.eClass(), template.getName()).toString();
        transaction.attachTopObject((IBmObject)template, fqn);
        configuration.getCommonTemplates().add(template);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getCommonTemplates()`

### Notes
- Template binary or text content is maintained separately after the metadata object exists.
