## Add Document Registers (Set Up Registrars on Document Side)

Call `JShell` with `scope: "edt"` for this workflow.

Use this workflow for real 1C developer requests such as:
- "документ должен увеличивать остатки";
- "документ должен уменьшать остатки";
- "сделать движения документа по регистру накопления";
- "добавить записи регистров";
- "сделать документ регистратором регистра".

If the document and register already exist, this is an editable metadata
operation. Do not answer that the user must change metadata manually. Execute a
BM transaction, load both top objects by FQN, and add the register to
`document.getRegisterRecords()` when it is not already present.

This workflow only configures the metadata link that lets a document write to a
register. Do not treat writing `ObjectModule.bsl` as a replacement for this
metadata edit: BSL posting code can be a separate follow-up step, but the
document still must contain the register in `getRegisterRecords()`.

Registrars are configured on the document side through
`Document.getRegisterRecords()`. This creates the document-to-register
relationship that EDT uses for accumulation/accounting/calculation register
validation.

Registers do not have `getRegisteredDocuments()`. Registrars are managed from
the document side only.

```java
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Document;

IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String result = globalContext.execute(new AbstractBmTask<String>("Add document register") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Document document =
            (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
        if (document == null) {
            throw new IllegalStateException("Missing document: Document.GoodsReceipt");
        }

        AccumulationRegister stockRegister =
            (AccumulationRegister)transaction.getTopObjectByFqn(
                "AccumulationRegister.GoodsInStock");
        if (stockRegister == null) {
            throw new IllegalStateException(
                "Missing register: AccumulationRegister.GoodsInStock");
        }

        if (!document.getRegisterRecords().contains(stockRegister)) {
            document.getRegisterRecords().add(stockRegister);
            return "Added AccumulationRegister.GoodsInStock to Document.GoodsReceipt";
        }
        return "Already linked AccumulationRegister.GoodsInStock to Document.GoodsReceipt";
    }
});
System.out.println(result);
```

### Multiple Register Types

Only these register types can be document registrars:
- `AccumulationRegister`
- `AccountingRegister`
- `CalculationRegister`

`InformationRegister` cannot be added to `Document.getRegisterRecords()`.
Information registers store periodic data and are not document-movement
registers.

For accounting or calculation registers, use the same document-side pattern:

```java
AccountingRegister accounting =
    (AccountingRegister)transaction.getTopObjectByFqn("AccountingRegister.MainLedger");
CalculationRegister calculation =
    (CalculationRegister)transaction.getTopObjectByFqn("CalculationRegister.Payroll");

if (accounting != null && !document.getRegisterRecords().contains(accounting)) {
    document.getRegisterRecords().add(accounting);
}
if (calculation != null && !document.getRegisterRecords().contains(calculation)) {
    document.getRegisterRecords().add(calculation);
}
```

### Forbidden API / Patterns

Do not use these when configuring document registrars:
- `DocumentRegisterRecord`
- `mdFactory.createDocumentRegisterRecord()`
- `register.getRegisteredDocuments()`
- `Document.getRegisterRecords().add(informationRegister)`
- any `InformationRegister` as a document registrar

If `JShellReflection` returns `not-found` for a register-record class or
factory method, do not invent a replacement class. Use
`Document.getRegisterRecords()` with an existing `AccumulationRegister`,
`AccountingRegister`, or `CalculationRegister`.

### Verification

After adding registers, verify inside the same BM read transaction:

```java
System.out.println("Document registers: " + document.getRegisterRecords().size());
for (BasicRegister reg : document.getRegisterRecords()) {
    System.out.println("  " + reg.getName() + " (" + reg.eClass().getName() + ")");
}
```

### Required Post-Check

After changing metadata links, call `GetMarkers` with `marker_type: "1c"` for
the changed document and the linked register. Fix only markers on changed
entities and directly affected references before reporting success.

Derive `.mdo` paths directly from FQNs; do not use `Glob` to discover them:

| FQN prefix                    | `.mdo` path                                   |
|-------------------------------|-----------------------------------------------|
| `Document.<Name>`             | `src/Documents/<Name>/<Name>.mdo`             |
| `AccumulationRegister.<Name>` | `src/AccumulationRegisters/<Name>/<Name>.mdo` |
| `AccountingRegister.<Name>`   | `src/AccountingRegisters/<Name>/<Name>.mdo`   |
| `CalculationRegister.<Name>`  | `src/CalculationRegisters/<Name>/<Name>.mdo`  |

Copy `<Name>` exactly from the FQN used in `getTopObjectByFqn(...)`: same case,
same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on
Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the
full FQN-to-folder mapping. A scoped per-document and per-register path check is
usually enough; fall back to project-wide markers only if cross-object
references may have broken.
