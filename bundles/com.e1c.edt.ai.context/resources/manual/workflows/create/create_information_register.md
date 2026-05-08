## Safe Workflow: Create InformationRegister

### Recommended bindings
- `workspaceRoot`, `projectManager`, `modelManager`, `mdFactory`, `fqnGenerator`

### Example
```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create information register") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        InformationRegister register = mdFactory.createInformationRegister();
        register.setName("Prices");
        register.getSynonym().put("ru", "Prices");
        register.setUuid(UUID.randomUUID());

        InformationRegisterDimension product = mdFactory.createInformationRegisterDimension();
        product.setName("Product");
        product.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();

        product.setType(typeDesc);
        register.getDimensions().add(product);

        InformationRegisterResource price = mdFactory.createInformationRegisterResource();
        price.setName("Price");
        price.setUuid(UUID.randomUUID());
        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        typeDesc = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();

        price.setType(typeDesc);
        register.getResources().add(price);

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getInformationRegisters().add(register);
        return null;
    }
});
```

### Notes
- InformationRegister usually needs at least one dimension and often one resource
- Every new feature derived from BasicFeature must have `setType(...)`
- Use a specific reference type like `Catalog.Products` when you need a strict typed dimension
- If a dimension, resource, or attribute uses `IEObjectTypeNames.STRING`, build it with finite qualifiers, for example `.setStringQualifiers(100, false)`, otherwise `GetMarkers` can report SU8: "Строка не может быть неограниченной длины"

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project and fix new validation markers before reporting success.
