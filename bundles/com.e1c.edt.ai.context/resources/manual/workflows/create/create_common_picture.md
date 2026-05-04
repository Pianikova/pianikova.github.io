## Safe Workflow: Create CommonPicture

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create CommonPicture") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        CommonPicture picture = mdFactory.createCommonPicture();
        picture.setName("CommonPictureSample");
        picture.setUuid(UUID.randomUUID());
        picture.setName("Logo");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(picture.eClass(), picture.getName()).toString();
        transaction.attachTopObject((IBmObject)picture, fqn);
        configuration.getCommonPictures().add(picture);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getCommonPictures()`

### Notes
- After creation, assign the actual picture resource or content through the corresponding picture APIs.
