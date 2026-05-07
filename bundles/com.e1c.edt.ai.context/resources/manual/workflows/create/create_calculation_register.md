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

### Registrar modes

Use one of these modes deliberately:
- **Mode A: register only for later linking.** Creating only the register is acceptable as an intermediate step, but `GetMarkers` may return SU45 until a document registrar is linked.
- **Mode B: register with registrar document.** Preferred when the user asks for a complete valid register workflow.

Calculation registers MUST have at least one document that records to them. Registrars are configured on documents, not on registers.

```java
Document payrollDocument = (Document)transaction.getTopObjectByFqn("Document.Payroll");
CalculationRegister salaryCalculation = (CalculationRegister)transaction.getTopObjectByFqn("CalculationRegister.SalaryCalculation");
if (payrollDocument != null && salaryCalculation != null && !payrollDocument.getRegisterRecords().contains(salaryCalculation)) {
    payrollDocument.getRegisterRecords().add(salaryCalculation);
}
```

If the registrar document does not exist, create it in the same BM transaction or call `create_document` first, then call `add_document_registers`.

### Expected validation marker for Mode A

If you create the register without linking a registrar document, `GetMarkers` can return SU45: "Некорректный состав регистраторов регистра. Ни один из документов не является регистратором для регистра".

Do not report success while this marker remains unless the user explicitly asked to create an invalid intermediate register for later linking.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project. For Mode B, fix all new validation markers before reporting success. For Mode A, explicitly report that registrar linking is still required if SU45 remains.
