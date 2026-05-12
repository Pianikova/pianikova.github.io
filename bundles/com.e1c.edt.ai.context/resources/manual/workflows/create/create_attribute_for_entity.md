## Scenario: Create Attribute For Entity

### Correct child types
- `Catalog` -> `CatalogAttribute`
- `Document` -> `DocumentAttribute`
- `BusinessProcess` -> `BusinessProcessAttribute`
- `Task` -> `TaskAttribute`
- registers -> specific register attribute class
- tabular section -> `TabularSectionAttribute`

### Example pattern
```java
IV8Project v8project = projectManager.getProject(project);
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
CatalogAttribute article = mdFactory.createCatalogAttribute();
article.setName("Article");
article.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build();

article.setType(typeDesc);
catalog.getAttributes().add(article);
```

### Safe checklist
1. Choose the exact child class for the parent (`CatalogAttribute`, `DocumentAttribute`, `TabularSectionAttribute`, ...)
2. Set `name` and `uuid` on the new child object
3. Resolve `IEObjectProvider` INSIDE the current transaction
4. Build a fresh `TypeDescription` BEFORE adding the object to the parent collection
5. Call `setType(typeDesc)` on every object derived from `BasicFeature`
6. Only after `setType(...)` add the object to `getAttributes()` / `getDimensions()` / `getResources()`

### Wrong vs correct
```java
IV8Project v8project = projectManager.getProject(project);
// WRONG: adding BasicFeature child without type
DocumentAttribute counterparty = mdFactory.createDocumentAttribute();
counterparty.setName("Counterparty");
counterparty.setUuid(UUID.randomUUID());
document.getAttributes().add(counterparty); // md-legacy-emf-check: type is required

// CORRECT: build and assign TypeDescription first
DocumentAttribute counterparty = mdFactory.createDocumentAttribute();
counterparty.setName("Counterparty");
counterparty.setUuid(UUID.randomUUID());
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem catalogRefType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
TypeDescription counterpartyType = new TypeDescriptionBuilder()
    .addType(catalogRefType)
    .build();
counterparty.setType(counterpartyType);
document.getAttributes().add(counterparty);
```

### Tabular section example
```java
TabularSectionAttribute quantity = mdFactory.createTabularSectionAttribute();
quantity.setName("Quantity");
quantity.setUuid(UUID.randomUUID());
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
TypeDescription quantityType = new TypeDescriptionBuilder()
    .addType(numberType)
    .build();
quantity.setType(quantityType);
products.getAttributes().add(quantity);
```

### Recovery pattern for real error (`Catalog.Авторы` -> `Страна`)
```java
IV8Project v8project = projectManager.getProject(project);
Catalog authors = (Catalog)transaction.getTopObjectByFqn("Catalog.Авторы");
if (authors != null) {
    IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
        .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
    CatalogAttribute country = authors.getAttributes().stream()
        .filter(a -> "Страна".equals(a.getName()))
        .findFirst()
        .orElse(null);
    if (country == null) {
        country = mdFactory.createCatalogAttribute();
        country.setName("Страна");
        country.setUuid(UUID.randomUUID());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription countryType = new TypeDescriptionBuilder()
            .addType(stringType)
            .setStringQualifiers(100, false)
            .build();
        country.setType(countryType);
        authors.getAttributes().add(country);
    } else if (country.getType() == null || country.getType().getTypes().isEmpty()) {
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription countryType = new TypeDescriptionBuilder()
            .addType(stringType)
            .setStringQualifiers(100, false)
            .build();
        country.setType(countryType);
    }
}
```

### Rules
- Always choose the child class that matches the parent entity
- Every attribute derived from `BasicFeature` must have `setType(...)` before it is added to the parent collection
- Never reuse one `TypeDescription` instance for multiple children. It is an EMF containment object and moves to the latest owner. Reuse `TypeItem` proxies, then build a new `TypeDescription` per child.
- Before attaching or finishing a bulk CRUD transaction, loop through all new `BasicFeature` children and fail if `getType() == null || getType().getTypes().isEmpty()`
- `CatalogAttribute`, `DocumentAttribute`, and `TabularSectionAttribute` are the most common sources of `md-legacy-emf-check` when `type` is omitted
- For child objects, UUID is still recommended in JShell
- For `IEObjectTypeNames.STRING`, always set finite qualifiers with `.setStringQualifiers(length, false)` to avoid SU8 unlimited-string markers.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
