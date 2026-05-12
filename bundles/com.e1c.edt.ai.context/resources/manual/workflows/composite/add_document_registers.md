## Add Document Registers (Set Up Registrars on Document Side)

Registrars are configured on the **document** side through `Document.getRegisterRecords()`.
This creates a bidirectional relationship: document → register.

**⚠️ IMPORTANT:** Registers do NOT have `getRegisteredDocuments()` method.
Registrars are managed from the document side only.

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Add document registers") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // Get the document to configure
        Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");

        // Get the registers to add as registrars.
        // Only AccumulationRegister, AccountingRegister, and CalculationRegister are valid here.
        AccumulationRegister stockRegister = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
        AccountingRegister accountingRegister = (AccountingRegister)transaction.getTopObjectByFqn("AccountingRegister.MainLedger");
        CalculationRegister payrollRegister = (CalculationRegister)transaction.getTopObjectByFqn("CalculationRegister.Payroll");

        if (document != null) {
            // Add accumulation register as registrar
            if (stockRegister != null) {
                document.getRegisterRecords().add(stockRegister);
                System.out.println("Added: AccumulationRegister.GoodsInStock as registrar");
            }

            // Add accounting register as registrar
            if (accountingRegister != null) {
                document.getRegisterRecords().add(accountingRegister);
                System.out.println("Added: AccountingRegister.MainLedger as registrar");
            }

            // Add calculation register as registrar
            if (payrollRegister != null) {
                document.getRegisterRecords().add(payrollRegister);
                System.out.println("Added: CalculationRegister.Payroll as registrar");
            }

            System.out.println("Document registers configured successfully");
        }

        return null;
    }
});
```

### Key Points:
- **Document side**: Registrators are configured on `Document.getRegisterRecords()`
- **No register.getRegisteredDocuments()**: Registers don't have this method
- **Bidirectional**: Setting document→register establishes both directions
- **Multiple registers**: One document can have multiple registers
- **Accumulation registers**: Use for stock/quantity tracking
- **Accounting registers**: Use for accounting operations
- **Calculation registers**: Use for payroll and calculation operations

### ⚠ CRITICAL: InformationRegister CANNOT be a registrar
**IMPORTANT**: InformationRegister cannot be added to `Document.getRegisterRecords()`.
InformationRegister stores periodic data but is NOT a register for documents.

### Valid Register Types for Documents:
- **AccumulationRegister**: Stock registers (BALANCE or TURNOVERS) - CAN be a registrar
- **AccountingRegister**: Chart of accounts-based accounting - CAN be a registrar
- **CalculationRegister**: Payroll and calculation registers - CAN be a registrar
- **InformationRegister**: Periodic data - CANNOT be a registrar (error: SU45)
- **Note**: Only AccumulationRegister, AccountingRegister, and CalculationRegister can be document registrars

### Forbidden API / Patterns

Do not use these when configuring document registrars:
- `DocumentRegisterRecord`
- `mdFactory.createDocumentRegisterRecord()`
- `register.getRegisteredDocuments()`
- `Document.getRegisterRecords().add(informationRegister)`
- Any `InformationRegister` as a document registrar

If `JShellReflection` returns `not-found` for a register-record class or factory
method, do not invent a replacement class. Use `Document.getRegisterRecords()`
with an existing `AccumulationRegister`, `AccountingRegister`, or
`CalculationRegister`.

### Verification:
After adding registers, verify:
```java
System.out.println("Document registers: " + document.getRegisterRecords().size());
document.getRegisterRecords().forEach(reg ->
    System.out.println("  " + reg.getName() + " (" + reg.eClass().getName() + ")"));
```

### Required post-check

After changing metadata links, call `GetMarkers` with `marker_type: "1c"` for the project, but fix only markers on changed entities and directly affected references before reporting success.
