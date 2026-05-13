## Safe Workflow: Create Catalog

> ⚠️ **HierarchyType — only two valid constants in EDT API.**
> Use ONLY `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS` (default) or `HierarchyType.HIERARCHY_OF_ITEMS`.
> Other names from 1C:Enterprise classic API (`HIERARCHY_GROUPS`, `HIERARCHY_HIERARCHICAL`, `HIERARCHY_NONE`) **do not exist** and cause `cannot find symbol`.

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Catalog created = globalContext.execute(new AbstractBmTask<Catalog>("Create catalog") {
    @Override
    public Catalog execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        Catalog catalog = mdFactory.createCatalog();
        catalog.setName("Products");
        catalog.getSynonym().put("ru", "Products");
        catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);
        // ❌ catalog.setHierarchyType(HierarchyType.HIERARCHY_GROUPS);       // does NOT exist
        // ❌ catalog.setHierarchyType(HierarchyType.HIERARCHY_HIERARCHICAL); // does NOT exist
        // ❌ catalog.setHierarchyType(HierarchyType.HIERARCHY_NONE);         // does NOT exist — omit the call instead
        catalog.setCodeLength(9);
        catalog.setDescriptionLength(150);

        CatalogAttribute article = mdFactory.createCatalogAttribute();
        article.setName("Article");
        article.getSynonym().put("ru", "Article");

        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .setStringQualifiers(100, false)
            .build();

        article.setType(typeDesc);
        catalog.getAttributes().add(article);

        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)
        catalog.setUuid(UUID.randomUUID());
        article.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
        transaction.attachTopObject((IBmObject)catalog, fqn);
        configuration.getCatalogs().add(catalog);
        return null;
    }
});
```

### HierarchyType constants

| Use this                                       | Instead of (does NOT exist)                       |
|------------------------------------------------|---------------------------------------------------|
| `HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS`    | `HierarchyType.HIERARCHY_GROUPS`                  |
| `HierarchyType.HIERARCHY_OF_ITEMS`             | `HierarchyType.HIERARCHY_HIERARCHICAL`            |
| _(omit `setHierarchyType` for non-hierarchical)_ | `HierarchyType.HIERARCHY_NONE`                  |

For a non-hierarchical catalog simply do not call `setHierarchyType(...)` —
the platform default is correct.

### Safe reference type pattern

When the user names a concrete reference type such as `CatalogRef.Контрагенты`,
`CatalogRef.ХранимыеФайлы`, or `EnumRef.ВидыТоваров`, resolve that exact metadata
reference type with `typeProvider.getProxy("CatalogRef.Контрагенты")`,
`typeProvider.getProxy("CatalogRef.ХранимыеФайлы")`, or
`typeProvider.getProxy("EnumRef.ВидыТоваров")`. Do not silently replace it with
generic `IEObjectTypeNames.CATALOG_REF` or `IEObjectTypeNames.ENUM_REF`; generic
reference types are valid only when the user explicitly asks for "any catalog" /
"any enum" polymorphism.

```java
TypeItem unitsRef = (TypeItem)typeProvider.getProxy("CatalogRef.Units");
if (unitsRef == null) {
    System.err.println("ERROR: Cannot resolve CatalogRef.Units");
    return null; // Stop instead of creating an incorrect generic CatalogRef
}
TypeDescription strictType = new TypeDescriptionBuilder()
    .addType(unitsRef)
    .build();
article.setType(strictType);
```

Concrete Russian-name example:

```java
TypeItem suppliersRef = (TypeItem)typeProvider.getProxy("CatalogRef.Контрагенты");
TypeItem pictureRef = (TypeItem)typeProvider.getProxy("CatalogRef.ХранимыеФайлы");
TypeItem kindRef = (TypeItem)typeProvider.getProxy("EnumRef.ВидыТоваров");
if (suppliersRef == null || pictureRef == null || kindRef == null) {
    System.err.println("ERROR: Cannot resolve concrete reference type: "
        + "CatalogRef.Контрагенты / CatalogRef.ХранимыеФайлы / EnumRef.ВидыТоваров");
    return null;
}
supplierAttribute.setType(new TypeDescriptionBuilder().addType(suppliersRef).build());
pictureAttribute.setType(new TypeDescriptionBuilder().addType(pictureRef).build());
kindAttribute.setType(new TypeDescriptionBuilder().addType(kindRef).build());
```

### Register Type Example

```java
IV8Project v8project = projectManager.getProject(project);
// Create information register with dimensions of different types
InformationRegister prices = mdFactory.createInformationRegister();
prices.setName("Prices");
prices.getSynonym().put("ru", "Цены");

IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

// Dimension 1: Product (specific catalog reference)
InformationRegisterDimension productDim = mdFactory.createInformationRegisterDimension();
productDim.setName("Product");
TypeItem productsRef = (TypeItem)typeProvider.getProxy("CatalogRef.Products");
TypeDescription productType = new TypeDescriptionBuilder()
    .addType(productsRef)
    .build();
productDim.setType(productType);
prices.getDimensions().add(productDim);

// Dimension 2: PriceType (specific catalog reference when the exact catalog is required)
InformationRegisterDimension priceTypeDim = mdFactory.createInformationRegisterDimension();
priceTypeDim.setName("PriceType");
TypeItem priceTypesRef = (TypeItem)typeProvider.getProxy("CatalogRef.PriceTypes");
if (priceTypesRef == null) {
    System.err.println("ERROR: Cannot resolve CatalogRef.PriceTypes");
    return null;
}
TypeDescription priceTypesType = new TypeDescriptionBuilder()
    .addType(priceTypesRef)
    .build();
priceTypeDim.setType(priceTypesType);
prices.getDimensions().add(priceTypeDim);

// Resource: Price (Number type)
InformationRegisterResource price = mdFactory.createInformationRegisterResource();
price.setName("Price");
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
TypeDescription numberTypeDesc = new TypeDescriptionBuilder()
    .addType(numberType)
    .build();
price.setType(numberTypeDesc);
prices.getResources().add(price);
```

### Chart of Accounts Type Example

```java
IV8Project v8project = projectManager.getProject(project);
// Create accounting register with ChartOfAccounts reference
AccountingRegister accReg = mdFactory.createAccountingRegister();
accReg.setName("Accounting");

IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

// Dimension: Account (specific ChartOfAccounts reference)
AccountingRegisterDimension accountDim = mdFactory.createAccountingRegisterDimension();
accountDim.setName("Account");
TypeItem coaRef = (TypeItem)typeProvider.getProxy("ChartOfAccounts.ПланСчетов");
TypeDescription coaType = new TypeDescriptionBuilder()
    .addType(coaRef)
    .build();
accountDim.setType(coaType);
accReg.getDimensions().add(accountDim);
```

### JShell-safe UUID strategy
⚠️ **WARNING:** `modelFactory.fillDefaultReferences()` may timeout in JShell due to OSGi service limitations.
Prefer manual UUID assignment (Option 1) for reliable JShell execution.

```java
IV8Project v8project = projectManager.getProject(project);
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
catalog.setUuid(UUID.randomUUID());
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
// ...

CatalogAttribute attr = mdFactory.createCatalogAttribute();
attr.setName("Article");
attr.setUuid(UUID.randomUUID());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription attrType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build();
attr.setType(attrType);
catalog.getAttributes().add(attr);

String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
transaction.attachTopObject((IBmObject)catalog, fqn);
```

### Common Mistake: Not setting UUIDs
```java
// ❌ WRONG - UUIDs not set, validation fails
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
transaction.attachTopObject((IBmObject)catalog, fqn);
// Error: SU45 - UUID required
```

### Important notes
- Validate `typeProvider.getProxy(...)` before `addType(...)`; `null` causes `IllegalArgumentException`
- References to metadata objects created in the same unfinished scenario may be unavailable. If the final business type must be concrete, split work into steps and stop on unresolved proxies; do not report success with a generic placeholder.
- Do not use `typeProvider.createProxy(...)`, `IDtConstants.getCatalogRefQName(...)`, or `IDtConstants.getEnumRefQName(...)` for EDT TypeDescription creation in JShell. Use `typeProvider.getProxy("CatalogRef.Name")`, `typeProvider.getProxy("EnumRef.Name")`, etc. Top-object FQNs are still `"Catalog.Name"` / `"Enum.Name"` for `transaction.getTopObjectByFqn(...)`; TypeItem names are `"CatalogRef.Name"` / `"EnumRef.Name"`.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
