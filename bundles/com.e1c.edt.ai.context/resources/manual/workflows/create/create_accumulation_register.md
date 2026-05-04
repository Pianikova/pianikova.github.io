## Safe Workflow: Create AccumulationRegister

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

AccumulationRegister register = globalContext.execute(new AbstractBmTask<AccumulationRegister>("Create register") {
    @Override
    public AccumulationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        AccumulationRegister register = mdFactory.createAccumulationRegister();
        register.setName("GoodsInStock");
        register.getSynonym().put("ru", "Goods In Stock");
        register.setRegisterType(AccumulationRegisterType.BALANCE);

        // Add dimension
        AccumulationRegisterDimension warehouse = mdFactory.createAccumulationRegisterDimension();
        warehouse.setName("Warehouse");
        warehouse.getSynonym().put("ru", "Warehouse");

        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();

        warehouse.setType(typeDesc);
        register.getDimensions().add(warehouse);

        // Add resource
        AccumulationRegisterResource quantity = mdFactory.createAccumulationRegisterResource();
        quantity.setName("Quantity");
        quantity.getSynonym().put("ru", "Quantity");

        // Set numeric type for resource
        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        typeDesc = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();

        quantity.setType(typeDesc);
        register.getResources().add(quantity);

        // Set UUIDs manually (RECOMMENDED for JShell)
        register.setUuid(UUID.randomUUID());
        warehouse.setUuid(UUID.randomUUID());
        quantity.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getAccumulationRegisters().add(register);
        return register;
    }
});
```
**Note:** Registers require at least one Dimension. Resources are optional but recommended.
**Note:** `AccumulationRegisterDimension` does not have `setBalance(...)`; do not call it in JShell examples.
