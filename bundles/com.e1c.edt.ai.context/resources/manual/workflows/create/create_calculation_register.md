## Safe Workflow: Create CalculationRegister

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

CalculationRegister register = globalContext.execute(new AbstractBmTask<CalculationRegister>("Create register") {
    @Override
    public CalculationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        CalculationRegister register = mdFactory.createCalculationRegister();
        register.setName("SalaryCalculation");
        register.getSynonym().put("ru", "Salary Calculation");
        register.setPeriodicity(CalculationRegisterPeriodicity.MONTH);
        register.setActionPeriod(true);
        register.setBasePeriod(false);

        // Set ChartOfCalculationTypes reference
        ChartOfCalculationTypes chart = (ChartOfCalculationTypes)transaction.getTopObjectByFqn("ChartOfCalculationTypes.ВидыРасчетов");
        register.setChartOfCalculationTypes(chart);

        // Add dimension (base dimension)
        CalculationRegisterDimension employee = mdFactory.createCalculationRegisterDimension();
        employee.setName("Employee");
        employee.getSynonym().put("ru", "Employee");
        employee.setBaseDimension(true);

        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();

        employee.setType(typeDesc);
        register.getDimensions().add(employee);

        // Add resource
        CalculationRegisterResource amount = mdFactory.createCalculationRegisterResource();
        amount.setName("Amount");
        amount.getSynonym().put("ru", "Amount");

        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        typeDesc = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();

        amount.setType(typeDesc);
        register.getResources().add(amount);

        // Add recalculation rule
        Recalculation recalculation = mdFactory.createRecalculation();
        recalculation.setName("Recalculation");
        register.getRecalculations().add(recalculation);

        // Set UUIDs manually (RECOMMENDED for JShell)
        register.setUuid(UUID.randomUUID());
        employee.setUuid(UUID.randomUUID());
        amount.setUuid(UUID.randomUUID());
        recalculation.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getCalculationRegisters().add(register);
        return register;
    }
});
```
**Note:** CalculationRegister requires ChartOfCalculationTypes reference and at least one base Dimension.
**Note:** Use a numeric type for calculation resources such as amount; do not reuse a reference type from a dimension.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project and fix new validation markers before reporting success.
