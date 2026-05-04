## Safe Workflow: Create DefinedType

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create DefinedType") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        DefinedType definedType = mdFactory.createDefinedType();
        definedType.setName("DefinedTypeSample");
        definedType.setUuid(UUID.randomUUID());
        // Create String type with length qualifier
                IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
                TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
                TypeDescription typeDesc = new TypeDescriptionBuilder()
                    .addType(stringType)
                    .build();

                // Set string qualifiers (length)
                // Note: StringQualifiers must be set on the TypeDescription, not passed to builder
                // StringQualifiers stringQualifiers = modelFactory.createStringQualifiers();
                // stringQualifiers.setLength(50);
                // typeDesc.setStringQualifiers(stringQualifiers);

                // Simplified: just set length on TypeDescription's qualifiers
                if (typeDesc.getStringQualifiers() == null) {
                    typeDesc.setStringQualifiers(modelFactory.createStringQualifiers());
                }
                typeDesc.getStringQualifiers().setLength(50);

                definedType.setType(typeDesc);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(definedType.eClass(), definedType.getName()).toString();
        transaction.attachTopObject((IBmObject)definedType, fqn);
        configuration.getDefinedTypes().add(definedType);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getDefinedTypes()`

### Notes
- DefinedType is usually used as a reusable alias; choose qualifiers deliberately to avoid broad runtime types.
