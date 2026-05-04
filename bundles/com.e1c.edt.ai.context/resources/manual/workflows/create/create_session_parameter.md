## Safe Workflow: Create SessionParameter

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create SessionParameter") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        SessionParameter parameter = mdFactory.createSessionParameter();
        parameter.setName("SessionParameterSample");
        parameter.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
                TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
                TypeDescription typeDesc = new TypeDescriptionBuilder()
                    .addType(stringType)
                    .build();

                parameter.setType(typeDesc);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(parameter.eClass(), parameter.getName()).toString();
        transaction.attachTopObject((IBmObject)parameter, fqn);
        configuration.getSessionParameters().add(parameter);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getSessionParameters()`

### Notes
- SessionParameter implements TypeDescriptionProvider, so set `type` before the object is used in session logic.
