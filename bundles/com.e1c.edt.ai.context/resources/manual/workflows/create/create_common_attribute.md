## Safe Workflow: Create CommonAttribute

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create CommonAttribute") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        CommonAttribute attribute = mdFactory.createCommonAttribute();
        attribute.setName("CommonAttributeSample");
        attribute.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
                TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
                TypeDescription typeDesc = new TypeDescriptionBuilder()
                    .addType(stringType)
                    .build();

                attribute.setType(typeDesc);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(attribute.eClass(), attribute.getName()).toString();
        transaction.attachTopObject((IBmObject)attribute, fqn);
        configuration.getCommonAttributes().add(attribute);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getCommonAttributes()`

### Notes
- CommonAttribute extends BasicFeature, so assign `TypeDescription` before adding it to configuration.
