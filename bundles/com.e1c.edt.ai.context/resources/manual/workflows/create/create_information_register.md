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
        TypeDescription productType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();

        product.setType(productType);
        register.getDimensions().add(product);

        InformationRegisterResource price = mdFactory.createInformationRegisterResource();
        price.setName("Price");
        price.setUuid(UUID.randomUUID());
        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription priceType = new TypeDescriptionBuilder()
            .addType(numberType)
            .setNumberQualifiers(2, 10, false)
            .build();

        price.setType(priceType);
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
- Never reuse one `TypeDescription` instance across dimensions, resources, or attributes. Reuse `TypeItem` proxies only.
- For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first. For `Number(10,2)`, call `.setNumberQualifiers(2, 10, false)`.
- Use a specific reference type like `Catalog.Products` when you need a strict typed dimension
- If a dimension, resource, or attribute uses `IEObjectTypeNames.STRING`, build it with finite qualifiers, for example `.setStringQualifiers(100, false)`, otherwise `GetMarkers` can report SU8: "Строка не может быть неограниченной длины"

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
