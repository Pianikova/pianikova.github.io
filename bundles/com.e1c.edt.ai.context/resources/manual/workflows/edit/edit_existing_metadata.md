## Edit Existing Metadata Object

Generic workflow for editing any existing metadata object safely.

### Core Rules:
- ✅ **Load** existing object from BM transaction
- ✅ **Modify** properties directly on the loaded object
- ✅ **Return** the modified object
- ❌ **NEVER use** `attachTopObject()` for existing objects
- ❌ **NEVER use** `transaction.detachTopObject()` for existing objects

### Required post-check

After editing metadata, call `GetMarkers` for the changed file or the whole project:

```json
{
  "project_name": "MyProject",
  "marker_type": "1c",
  "max_count": 50
}
```

Fix new 1C markers before reporting the edit as complete.

### Generic Pattern:
```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Document result = globalContext.execute(new AbstractBmTask<Document>("Edit document") {
    @Override
    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // STEP 1: Load existing object
        Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");

        if (document != null) {
            // STEP 2: Modify properties directly
            document.setDescriptionLength(200);
            document.setUseStandardCommands(true);
            document.setRealTimePosting(RealTimePosting.DENY);
            document.setNumberType(DocumentNumberType.NUMBER);

            // STEP 3: Add/Remove child objects through collections
            // Add attribute
            DocumentAttribute attr = mdFactory.createDocumentAttribute();
            attr.setName("NewAttribute");
            attr.setUuid(UUID.randomUUID());
            // ... set type ...
            document.getAttributes().add(attr);

            // Remove attribute by finding and deleting
            DocumentAttribute oldAttr = document.getAttributes().stream()
                .filter(a -> "OldAttribute".equals(a.getName()))
                .findFirst()
                .orElse(null);
            if (oldAttr != null) {
                EcoreUtil.delete(oldAttr);
            }

            // STEP 4: Return modified object
            return document;
        }

        return null; // Object not found
    }
});
```

### Edit Different Object Types:

**Catalog:**
```java
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
catalog.setCodeLength(10);
catalog.setDescriptionLength(200);
return catalog;
```

**Information Register:**
```java
InformationRegister register = (InformationRegister)transaction.getTopObjectByFqn("InformationRegister.Prices");
register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY);
register.setWriteMode(RegisterWriteMode.INDEPENDENT);
return register;
```

**Accumulation Register:**
```java
AccumulationRegister register = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
register.setRegisterType(AccumulationRegisterType.BALANCE);
return register;
```

**Enum:**
```java
com._1c.g5.v8.dt.metadata.mdclass.Enum enumObj =
    (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.Statuses");
enumObj.getEnumValues().get(0).setName("Active");
return enumObj;
```

### Common Pitfalls to Avoid:

**❌ WRONG: Using attachTopObject for existing object**
```java
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
document.setDescriptionLength(200);
String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();
transaction.attachTopObject((IBmObject)document, fqn); // ❌ BmFqnAlreadyInUseException!
```

**❌ WRONG: Detaching existing object**
```java
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
transaction.detachTopObject((IBmObject)document); // ❌ Detach should not be used for editing
```

**✅ CORRECT: Direct modification only**
```java
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
if (document != null) {
    document.setDescriptionLength(200); // ✅ Direct modification
    // No attachTopObject() call
    // No detachTopObject() call
    // Just modify and return
}
```

### Edit Document Registers:
```java
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
AccumulationRegister register = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
if (document != null && register != null) {
    document.getRegisterRecords().add(register);
    System.out.println("Added register to document");
}
```

### Edit Document Journal Documents:
```java
DocumentJournal journal = (DocumentJournal)transaction.getTopObjectByFqn("DocumentJournal.GoodsMovement");
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
if (journal != null && document != null) {
    journal.getRegisteredDocuments().add(document);
    System.out.println("Added document to journal");
}
```

### Delete Child Objects:
```java
// Remove child object properly
CatalogAttribute attrToRemove = catalog.getAttributes().stream()
    .filter(a -> "OldName".equals(a.getName()))
    .findFirst()
    .orElse(null);
if (attrToRemove != null) {
    EcoreUtil.delete(attrToRemove); // ✅ Correct deletion method
    System.out.println("Removed attribute: " + attrToRemove.getName());
}
```
