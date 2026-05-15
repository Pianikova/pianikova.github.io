## Edit Existing Metadata Object

```java
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Catalog result = globalContext.execute(new AbstractBmTask<Catalog>("Edit catalog") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        // Get EXISTING object - NO attachTopObject()
        Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");

        if (catalog != null) {
            // Modify properties directly
            catalog.setDescriptionLength(200);

            // Add new attribute
            CatalogAttribute newAttr = mdFactory.createCatalogAttribute();
            newAttr.setName("Brand");
            newAttr.setUuid(UUID.randomUUID());
            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
            TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
            TypeDescription typeDesc = new TypeDescriptionBuilder()
                .addType(stringType)
                .build();

            newAttr.setType(typeDesc);
            catalog.getAttributes().add(newAttr);

            return catalog;
        }
        return null;
    }
});
```

### Required post-check

After editing metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Inspect returned markers and fix only validation errors relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
Schema: `<projectRoot>/src/<TypePluralFolder>/<Name>/<Name>.mdo`. Common cases:

| FQN prefix                      | `.mdo` path                                          |
|---------------------------------|------------------------------------------------------|
| `Catalog.<Name>`                | `src/Catalogs/<Name>/<Name>.mdo`                     |
| `Document.<Name>`               | `src/Documents/<Name>/<Name>.mdo`                    |
| `Enum.<Name>`                   | `src/Enums/<Name>/<Name>.mdo`                        |
| `InformationRegister.<Name>`    | `src/InformationRegisters/<Name>/<Name>.mdo`         |
| `AccumulationRegister.<Name>`   | `src/AccumulationRegisters/<Name>/<Name>.mdo`        |

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn(...)` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
