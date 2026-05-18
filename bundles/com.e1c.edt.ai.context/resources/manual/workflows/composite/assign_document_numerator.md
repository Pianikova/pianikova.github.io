## Assign Document Numerator

Documents are attached to a document numerator with `Document.setNumerator(DocumentNumerator)`.

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Assign document numerator") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        DocumentNumerator numerator = (DocumentNumerator)transaction.getTopObjectByFqn("DocumentNumerator.WarehouseDocuments");
        Document receipt = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
        Document issue = (Document)transaction.getTopObjectByFqn("Document.GoodsIssue");

        if (numerator == null || receipt == null || issue == null) {
            throw new IllegalStateException("Missing numerator or document");
        }

        numerator.setNumberType(DocumentNumberType.NUMBER);
        numerator.setNumberLength(11);
        numerator.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);

        receipt.setNumberType(DocumentNumberType.NUMBER);
        receipt.setNumberLength(11);
        receipt.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);
        receipt.setNumerator(numerator);

        issue.setNumberType(DocumentNumberType.NUMBER);
        issue.setNumberLength(11);
        issue.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);
        issue.setNumerator(numerator);
        return null;
    }
});
```

### Rules

- Use `Document.setNumerator(DocumentNumerator)`.
- Keep `numberType`, `numberLength`, and `numberPeriodicity` equal on the numerator and every document that uses it.
- Do not change only one document's numbering properties after a numerator is assigned. EDT reports SU45 when document numbering properties differ from the numerator.
- Run `GetMarkers` for the numerator and all affected documents, or project-wide markers for a multi-object scenario.
