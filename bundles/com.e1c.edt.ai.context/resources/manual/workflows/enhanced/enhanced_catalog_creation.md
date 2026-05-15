## Enhanced Workflow: Create Catalog with Existence Check and UUID Error Handling

This workflow provides a more robust approach with:
- Pre-creation existence check (prevents BmFqnAlreadyInUseException)
- UUID assignment with try-catch error handling
- Comprehensive validation before attachment
- Detailed error reporting

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Create catalog safely") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // STEP 1: Check if catalog already exists BEFORE creation
        String catalogFqn = "Catalog.Products";
        if (transaction.getTopObjectByFqn(catalogFqn) != null) {
            System.err.println("ERROR: Catalog already exists: " + catalogFqn);
            return null; // Stop creation
        }

        // STEP 2: Create catalog object
        Catalog catalog = mdFactory.createCatalog();
        catalog.setName("Products");
        catalog.getSynonym().put("ru", "Products");
        catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);
        // ❌ catalog.setHierarchyType(HierarchyType.HIERARCHY_GROUPS);       // does NOT exist
        // ❌ catalog.setHierarchyType(HierarchyType.HIERARCHY_HIERARCHICAL); // does NOT exist
        // ❌ catalog.setHierarchyType(HierarchyType.HIERARCHY_NONE);         // does NOT exist — omit the call instead
        catalog.setCodeLength(9);
        catalog.setDescriptionLength(150);

        // STEP 3: Set UUID with error handling
        try {
            catalog.setUuid(UUID.randomUUID());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for catalog: " + e.getMessage());
            return null; // Stop creation if UUID cannot be set
        }

        // STEP 4: Create type provider INSIDE transaction
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // STEP 5: Create attributes with UUID error handling
        CatalogAttribute code = mdFactory.createCatalogAttribute();
        code.setName("Code");
        code.getSynonym().put("ru", "Code");
        try {
            code.setUuid(UUID.randomUUID()); // CRITICAL: UUID required for all child objects
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for attribute Code: " + e.getMessage());
            return null; // Stop creation if UUID cannot be set
        }

        // Create type description INSIDE transaction
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription codeType = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();
        code.setType(codeType);
        catalog.getAttributes().add(code);

        CatalogAttribute name = mdFactory.createCatalogAttribute();
        name.setName("Name");
        name.getSynonym().put("ru", "Name");
        try {
            name.setUuid(UUID.randomUUID()); // CRITICAL: UUID required for all child objects
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for attribute Name: " + e.getMessage());
            return null; // Stop creation if UUID cannot be set
        }

        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription nameType = new TypeDescriptionBuilder()
            .addType(stringType)
            .setStringQualifiers(100, false)
            .build();
        name.setType(nameType);
        catalog.getAttributes().add(name);

        // STEP 6: Validate before attachment
        if (catalog.getAttributes().isEmpty()) {
            System.err.println("ERROR: Catalog has no attributes - this may cause validation issues");
        }

        // STEP 7: Generate FQN and attach to transaction
        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();

        try {
            transaction.attachTopObject((IBmObject)catalog, fqn);
            configuration.getCatalogs().add(catalog);
            System.out.println("SUCCESS: Catalog created: " + fqn);
            return catalog;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to attach catalog: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
});
```

### Key Improvements:

**1. Pre-creation existence check**
- Uses `transaction.getTopObjectByFqn()` BEFORE creating object
- Prevents `BmFqnAlreadyInUseException` runtime errors
- Provides clear error message about why creation stopped

**2. UUID assignment with error handling**
- Every UUID assignment wrapped in try-catch block
- Critical for catalog and ALL child objects (attributes, tabular sections)
- Prevents SU45 validation errors (UUID required for all metadata objects)
- Stops creation gracefully if UUID cannot be set

**3. Comprehensive validation**
- Checks for empty attribute lists before attachment
- Validates TypeDescription creation inside transaction
- Attachment operation wrapped in try-catch for robustness

**4. Detailed error reporting**
- Clear error messages at each failure point
- Stack traces for unexpected exceptions
- Success confirmation message

### When to use this workflow:
- Creating new catalogs in production code
- When object existence might be uncertain
- When UUID assignment failures are possible
- For scripts that need reliable error handling

### Required post-check

After enhanced metadata creation, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
For a `Catalog.<Name>` the path is always:

```
<projectRoot>/src/Catalogs/<Name>/<Name>.mdo
```

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn("Catalog.<Name>")` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping if this scenario was extended to other types.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.

