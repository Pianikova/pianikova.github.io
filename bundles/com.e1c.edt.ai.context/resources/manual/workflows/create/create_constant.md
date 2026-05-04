## Safe Workflow: Create Constant

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Constant") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Constant constant = mdFactory.createConstant();
        constant.setName("ConstantSample");
        constant.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
                TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
                TypeDescription typeDesc = new TypeDescriptionBuilder()
                    .addType(stringType)
                    .build();

                constant.setType(typeDesc);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(constant.eClass(), constant.getName()).toString();
        transaction.attachTopObject((IBmObject)constant, fqn);
        configuration.getConstants().add(constant);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getConstants()`

### Notes
- Constants implement TypeDescriptionProvider, so `setType(...)` is mandatory.
