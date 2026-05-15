## Add Tabular Section to Existing Document

```java
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Document result = globalContext.execute(new AbstractBmTask<Document>("Add tabular section") {
    @Override
    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // Get EXISTING document - NO attachTopObject()
        Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");

        if (document != null) {
            // Create tabular section
            DocumentTabularSection products = mdFactory.createDocumentTabularSection();
            products.setName("Products");
            products.getSynonym().put("ru", "Products");
            products.setUuid(UUID.randomUUID());

            // Create tabular section attributes
            TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();
            product.setName("Product");
            product.getSynonym().put("ru", "Product");
            product.setUuid(UUID.randomUUID());

            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
            TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
            TypeDescription typeDesc = new TypeDescriptionBuilder()
                .addType(catalogRefType)
                .build();

            product.setType(typeDesc);

            products.getAttributes().add(product);
            document.getTabularSections().add(products);

            return document;
        }
        return null;
    }
});
```

### Required post-check

After changing metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the **parent** top-level object's `.mdo` (a tabular section is a child — it has no `.mdo` of its own). Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN of the parent — do not `Glob` to find it.**
Schema: `<projectRoot>/src/<TypePluralFolder>/<ParentName>/<ParentName>.mdo`.
For this scenario, the parent is typically a `Document` or `Catalog`:

| Parent FQN          | `.mdo` path                              |
|---------------------|------------------------------------------|
| `Document.<Name>`   | `src/Documents/<Name>/<Name>.mdo`        |
| `Catalog.<Name>`    | `src/Catalogs/<Name>/<Name>.mdo`         |

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn(...)` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
