## Enhanced Workflow: Create Register with Full Validation

This workflow provides comprehensive register creation (Information, Accumulation, Accounting, Calculation) with:
- Existence check before creation
- UUID assignment for all objects (register, dimensions, resources, attributes)
- Type validation and error handling
- Complete validation before attachment

### Pattern for All Register Types:

```java
// This pattern applies to:
// - InformationRegister
// - AccumulationRegister
// - AccountingRegister
// - CalculationRegister

IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

InformationRegister result = globalContext.execute(new AbstractBmTask<InformationRegister>("Create register safely") {
    @Override
    public InformationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // CHECK 1: Register existence
        String registerFqn = "InformationRegister.Prices";
        if (transaction.getTopObjectByFqn(registerFqn) != null) {
            System.err.println("ERROR: Register already exists: " + registerFqn);
            return null;
        }

        // CREATE register
        InformationRegister register = mdFactory.createInformationRegister();
        register.setName("Prices");
        register.getSynonym().put("ru", "Цены");
        register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY);
        register.setUseStandardCommands(true);

        // ASSIGN UUID to register
        try {
            register.setUuid(UUID.randomUUID());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for register: " + e.getMessage());
            return null;
        }

        // CREATE type provider
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // CREATE dimension with UUID
        InformationRegisterDimension product = mdFactory.createInformationRegisterDimension();
        product.setName("Product");
        product.getSynonym().put("ru", "Товар");
        try {
            product.setUuid(UUID.randomUUID()); // CRITICAL: UUID for dimensions
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for dimension Product: " + e.getMessage());
            return null;
        }

        TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription productType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();
        product.setType(productType);
        register.getDimensions().add(product);

        // CREATE resource with UUID
        InformationRegisterResource price = mdFactory.createInformationRegisterResource();
        price.setName("Price");
        price.getSynonym().put("ru", "Цена");
        try {
            price.setUuid(UUID.randomUUID()); // CRITICAL: UUID for resources
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for resource Price: " + e.getMessage());
            return null;
        }

        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription priceType = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();
        price.setType(priceType);
        register.getResources().add(price);

        // VALIDATE before attachment
        if (register.getDimensions().isEmpty()) {
            System.err.println("ERROR: Register has no dimensions (at least one required)");
            return null;
        }

        // ATTACH and add to configuration
        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        try {
            transaction.attachTopObject((IBmObject)register, fqn);
            configuration.getInformationRegisters().add(register);
            System.out.println("SUCCESS: Register created: " + fqn);
            return register;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to attach register: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
});
```

### UUID Assignment Checklist for Registers:
- [ ] Register top-level object: `register.setUuid(UUID.randomUUID())`
- [ ] All InformationRegisterDimension objects: `dim.setUuid(UUID.randomUUID())`
- [ ] All InformationRegisterResource objects: `res.setUuid(UUID.randomUUID())`
- [ ] All RegisterAttribute objects: `attr.setUuid(UUID.randomUUID())`
- [ ] For CalculationRegister: Recalculation objects need UUID
- [ ] Wrap ALL UUID assignments in try-catch blocks
- [ ] Abort creation if any UUID assignment fails
- [ ] After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and fix new validation markers

