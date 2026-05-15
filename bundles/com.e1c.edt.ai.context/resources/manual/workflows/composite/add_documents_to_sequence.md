## Add Documents To Sequence

Document membership in a sequence is configured on the sequence side through `Sequence.getDocuments()`.

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Add documents to sequence") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Sequence sequence = (Sequence)transaction.getTopObjectByFqn("Sequence.BatchAccounting");
        Document receipt = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
        Document issue = (Document)transaction.getTopObjectByFqn("Document.GoodsIssue");

        if (sequence == null) {
            throw new IllegalStateException("Missing sequence");
        }
        if (receipt == null || issue == null) {
            throw new IllegalStateException("Missing participating document");
        }

        if (!sequence.getDocuments().contains(receipt)) {
            sequence.getDocuments().add(receipt);
        }
        if (!sequence.getDocuments().contains(issue)) {
            sequence.getDocuments().add(issue);
        }
        return null;
    }
});
```

### Rules

- Use `Sequence.getDocuments().add(document)`.
- Do not call `Sequence.setDocuments(...)`; this method is not present in the tested EDT API.
- Add existing top-level `Document` objects fetched from the BM transaction.
- Avoid duplicate links by checking `contains(...)` before `add(...)`.
- Run `GetMarkers` with `marker_type: "1c"` for the sequence `.mdo`; use project-wide markers when checking the full cross-object scenario.
