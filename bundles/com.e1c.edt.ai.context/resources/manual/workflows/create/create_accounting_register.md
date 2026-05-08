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

### Registrar modes

Use one of these modes deliberately:
- **Mode A: register only for later linking.** Creating only the register is acceptable as an intermediate step, but `GetMarkers` may return SU45 until a document registrar is linked.
- **Mode B: register with registrar document.** Preferred when the user asks for a complete valid register workflow.

Accounting registers MUST have at least one document that records to them. Registrars are configured on documents, not on registers.

```java
Document accountingOperation = (Document)transaction.getTopObjectByFqn("Document.AccountingOperation");
AccountingRegister accounting = (AccountingRegister)transaction.getTopObjectByFqn("AccountingRegister.Accounting");
if (accountingOperation != null && accounting != null && !accountingOperation.getRegisterRecords().contains(accounting)) {
    accountingOperation.getRegisterRecords().add(accounting);
}
```

If the registrar document does not exist, create it in the same BM transaction or call `create_document` first, then call `add_document_registers`.

### Expected validation marker for Mode A

If you create the register without linking a registrar document, `GetMarkers` can return SU45: "Некорректный состав регистраторов регистра. Ни один из документов не является регистратором для регистра".

Do not report success while this marker remains unless the user explicitly asked to create an invalid intermediate register for later linking.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project. For Mode B, fix all new validation markers before reporting success. For Mode A, explicitly report that registrar linking is still required if SU45 remains.
