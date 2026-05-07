## Safe Workflow: Create AccountingRegister

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

AccountingRegister register = globalContext.execute(new AbstractBmTask<AccountingRegister>("Create register") {
    @Override
    public AccountingRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        AccountingRegister register = mdFactory.createAccountingRegister();
        register.setName("Accounting");
        register.getSynonym().put("ru", "Accounting");
        register.setCorrespondence(true);
        register.setPeriodAdjustmentLength(2);

        // Set ChartOfAccounts reference
        ChartOfAccounts chartOfAccounts = (ChartOfAccounts)transaction.getTopObjectByFqn("ChartOfAccounts.ПланСчетов");
        register.setChartOfAccounts(chartOfAccounts);

        // Add dimension
        AccountingRegisterDimension account = mdFactory.createAccountingRegisterDimension();
        account.setName("Account");
        account.getSynonym().put("ru", "Account");
        account.setBalance(true);

        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem coaRef = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CHART_OF_ACCOUNTS_REF);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(coaRef)
            .build();

        account.setType(typeDesc);
        register.getDimensions().add(account);

        // Add resource
        AccountingRegisterResource amount = mdFactory.createAccountingRegisterResource();
        amount.setName("Amount");
        amount.getSynonym().put("ru", "Amount");
        amount.setBalance(true);

        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        typeDesc = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();

        amount.setType(typeDesc);
        register.getResources().add(amount);

        // Set UUIDs manually (RECOMMENDED for JShell)
        register.setUuid(UUID.randomUUID());
        account.setUuid(UUID.randomUUID());
        amount.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getAccountingRegisters().add(register);
        return register;
    }
});
```
**Note:** AccountingRegister requires ChartOfAccounts reference and at least one Dimension with account reference type.
**Note:** `AccountingRegisterDimension` and `AccountingRegisterResource` support `setBalance(boolean)` in EDT API.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project and fix new validation markers before reporting success.
