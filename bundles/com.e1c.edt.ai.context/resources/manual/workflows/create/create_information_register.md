## Safe Workflow: Create InformationRegister

### ⚠️ Critical API note — resolving `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` in JShell

**Do NOT use `typeProvider.getProxy("CatalogRef.X")` for metadata reference
types.** That global type index is populated asynchronously by EDT and is
**not refreshed within a JShell session** — it returns `null` for every
Catalog / Document / Enum. Resolve metadata reference types via
`MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`
where `depMdObject = transaction.getTopObjectByFqn("Catalog.X")` / etc.
Primitive types (`STRING`, `NUMBER`, ...) still use
`typeProvider.getProxy(IEObjectTypeNames.STRING)`.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

Catalog productsDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
if (productsDep == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Products — create it first");
}
TypeItem productsRef = MdProducedTypesUtil.getProducedType(
    productsDep, MdTypePackage.Literals.MD_REF_TYPE);
```

### Recommended bindings
- `workspaceRoot`, `projectManager`, `modelManager`, `mdFactory`, `fqnGenerator`

### Preflight-blocked patterns

Do not send JShell code with these EDT patterns:

```java
// WRONG: do not call setStringQualifiers with length 150, 1000, or any value above 100.

// WRONG: Ecore data types are not EDT TypeItem values.
TypeItem stringType = (TypeItem)modelFactory.create(EcorePackage.Literals.ESTRING, v8project);
TypeItem numberType = (TypeItem)modelFactory.create(EcorePackage.Literals.EINT, v8project);
```

Use `IEObjectProvider` and safe string qualifiers:

```java
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);

TypeDescription dimensionType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build();
```

### Example
```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create information register") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        InformationRegister register = mdFactory.createInformationRegister();
        register.setName("Prices");
        register.getSynonym().put("ru", "Prices");
        register.setUuid(UUID.randomUUID());

        InformationRegisterDimension product = mdFactory.createInformationRegisterDimension();
        product.setName("Product");
        product.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription productType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();

        product.setType(productType);
        register.getDimensions().add(product);

        InformationRegisterResource price = mdFactory.createInformationRegisterResource();
        price.setName("Price");
        price.setUuid(UUID.randomUUID());
        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription priceType = new TypeDescriptionBuilder()
            .addType(numberType)
            .setNumberQualifiers(2, 10, false)
            .build();

        price.setType(priceType);
        register.getResources().add(price);

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getInformationRegisters().add(register);
        return null;
    }
});
```

### Notes
- InformationRegister usually needs at least one dimension and often one resource
- Every new feature derived from BasicFeature must have `setType(...)`
- Never reuse one `TypeDescription` instance across dimensions, resources, or attributes. Reuse `TypeItem` proxies only.
- For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first. For `Number(10,2)`, call `.setNumberQualifiers(2, 10, false)`.
- Use a specific reference type like `CatalogRef.Products` when you need a strict typed dimension
- If a dimension, resource, or attribute uses `IEObjectTypeNames.STRING`, build it with finite qualifiers, for example `.setStringQualifiers(100, false)`. Do not use values greater than 100, such as `150` or `1000`, unless the user explicitly requires it and the current EDT model accepts it. Otherwise `GetMarkers` can report SU8: "Строка не может быть неограниченной длины" or "Переменная длина строки должна быть внутри диапазона от 0 до 100".

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
For an `InformationRegister.<Name>` the path is always:

```
<projectRoot>/src/InformationRegisters/<Name>/<Name>.mdo
```

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn("InformationRegister.<Name>")` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
