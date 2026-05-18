## Add Registered Documents To Document Journal

Registered documents are configured on the **journal** side through `DocumentJournal.getRegisteredDocuments()`.
This determines which documents appear in the journal.

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Add journal documents") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // Get the journal to configure
        DocumentJournal journal = (DocumentJournal)transaction.getTopObjectByFqn("DocumentJournal.GoodsMovement");

        // Get the documents to add to the journal
        Document receiptDoc = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
        Document saleDoc = (Document)transaction.getTopObjectByFqn("Document.GoodsSale");
        Document returnDoc = (Document)transaction.getTopObjectByFqn("Document.GoodsReturn");

        if (journal != null) {
            // Add documents to the journal
            if (receiptDoc != null) {
                journal.getRegisteredDocuments().add(receiptDoc);
                System.out.println("Added: Document.GoodsReceipt");
            }

            if (saleDoc != null) {
                journal.getRegisteredDocuments().add(saleDoc);
                System.out.println("Added: Document.GoodsSale");
            }

            if (returnDoc != null) {
                journal.getRegisteredDocuments().add(returnDoc);
                System.out.println("Added: Document.GoodsReturn");
            }

            System.out.println("Journal documents configured successfully");
        }

        return null;
    }
});
```

### Key Points:
- **Journal side**: Registered documents are configured on `DocumentJournal.getRegisteredDocuments()`
- **Multiple documents**: One journal can contain multiple documents
- **Document order**: Order in collection determines display order in journal
- **Clear existing**: Use `clear()` to remove all documents before adding new ones
- **Journal purpose**: Groups related documents for easier navigation and filtering
- **No registration required**: Adding to journal doesn't affect registrar relationships

### Verification:
After adding documents, verify:
```java
System.out.println("Journal documents: " + journal.getRegisteredDocuments().size());
journal.getRegisteredDocuments().forEach(doc ->
    System.out.println("  " + doc.getName() + " (" + doc.getSynonym().get("ru") + ")"));
```

### Required post-check

After changing metadata links, call `GetMarkers` with `marker_type: "1c"` for the project, but fix only markers on changed entities and directly affected references before reporting success.

**Derive the `.mdo` paths of the changed entities directly from their FQNs — do not `Glob` to find them.**
For this scenario the primary changed entity is the journal; the registered documents may also surface markers:

| FQN prefix                | `.mdo` path                                  |
|---------------------------|----------------------------------------------|
| `DocumentJournal.<Name>`  | `src/DocumentJournals/<Name>/<Name>.mdo`     |
| `Document.<Name>`         | `src/Documents/<Name>/<Name>.mdo`            |

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn(...)` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping. A scoped path check on the journal `.mdo` is usually enough; fall back to project-wide markers only when cross-object references may have broken.
