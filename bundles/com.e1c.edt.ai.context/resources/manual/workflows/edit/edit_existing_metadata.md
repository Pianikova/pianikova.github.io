## Edit Existing Metadata Object

Generic workflow for editing any existing metadata object safely.

If the user asks to add requisites/attributes to an existing catalog, use
`edit_catalog` first. If the user asks to add requisites/attributes to an
existing document, use `edit_document` first. These cards contain concrete
child-attribute recipes. Do not use the generic example below as a substitute
for setting child `type`.

### Core Rules:
- ✅ **Load** existing object from BM transaction
- ✅ **Modify** properties directly on the loaded object
- ✅ **Return** the modified object
- ❌ **NEVER use** `attachTopObject()` for existing objects
- ❌ **NEVER use** `transaction.detachTopObject()` for existing objects
- ❌ **NEVER edit `.mdo` text directly** for metadata CRUD from JShell prompts
- ❌ **NEVER invent helper APIs** such as `EnumTypeUtil.createEnumRefType`,
  `TypeItemUtil.getTypeByName`, `StringQualifiersUtil`, or
  `v8project.getModel().createTypeDescription()`
- ❌ **NEVER stop with "внесите вручную"** for supported child metadata such as
  `CatalogAttribute`, `DocumentAttribute`, or register fields

### JShell imports

`manual_ids` do not execute imports from manual cards. If the edit snippet uses
child classes or enum constants such as `DocumentAttribute`,
`TabularSectionAttribute`, `RealTimePosting`, `InformationRegisterAttribute`,
or `MdProducedTypesUtil`, import them in the same JShell session or use fully
qualified names. See `jshell_edt_canonical_imports` for the canonical package
list.

### Required post-check

After editing metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Fix new 1C markers relevant to the changed entity before reporting the edit as complete.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
Schema: `<projectRoot>/src/<TypePluralFolder>/<Name>/<Name>.mdo`. Common cases for this scenario:

| FQN prefix                      | `.mdo` path                                          |
|---------------------------------|------------------------------------------------------|
| `Catalog.<Name>`                | `src/Catalogs/<Name>/<Name>.mdo`                     |
| `Document.<Name>`               | `src/Documents/<Name>/<Name>.mdo`                    |
| `Enum.<Name>`                   | `src/Enums/<Name>/<Name>.mdo`                        |
| `InformationRegister.<Name>`    | `src/InformationRegisters/<Name>/<Name>.mdo`         |
| `AccumulationRegister.<Name>`   | `src/AccumulationRegisters/<Name>/<Name>.mdo`        |
| `DocumentJournal.<Name>`        | `src/DocumentJournals/<Name>/<Name>.mdo`             |

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn(...)` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Fall back to a project-wide marker check (`{ "project_name": "MyProject", "marker_type": "1c", "max_count": 50 }`) only when the change affects references between metadata objects or when the path truly cannot be derived.

### Readback rule

After editing attributes, tabular sections, or register fields, read back the
child collections by name and print the first `TypeItem` with `getName()`.
Do not print a `TypeItem` object directly and do not call `getTypeId()`: JShell
prints implementation identities like `TypeImpl@...`, not the 1C type name.
Also print or assert the count by child name. Each requested child must exist
exactly once. `GetMarkers` may not report duplicate business attributes.

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
            // Add attribute. Generic example only: concrete document attribute
            // edits should use edit_document.
            boolean alreadyExists = document.getAttributes().stream()
                .anyMatch(a -> "NewAttribute".equals(a.getName()));
            if (alreadyExists) {
                throw new IllegalStateException("NewAttribute already exists; repair it instead of adding a duplicate");
            }
            DocumentAttribute attr = mdFactory.createDocumentAttribute();
            attr.setName("NewAttribute");
            attr.setUuid(UUID.randomUUID());
            // Set a real TypeDescription before add(...); see create_attribute_for_entity.
            // Never add a BasicFeature child while type is null or empty.
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
