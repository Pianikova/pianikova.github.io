## Scenario: Create Attribute For Entity

### ⚠️ Critical API note — resolving `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` in JShell

**Do NOT use `typeProvider.getProxy("CatalogRef.X")` for metadata reference
types.** The global `IEObjectProvider` type index is populated asynchronously
by EDT and is **not refreshed within a JShell session** — it returns `null`
for every freshly-created Catalog / Document / Enum and even existing ones.
Use `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`
where `depMdObject = transaction.getTopObjectByFqn("Catalog.X")` / `"Enum.X"`
/ etc. Primitive types (`STRING`, `NUMBER`, `BOOLEAN`, `DATE`) still use the
platform type provider, but validate the result before `addType(...)`.
`getProxy(IEObjectTypeNames.BOOLEAN)` can return `null` in some JShell
sessions; use `typeProvider.createProxy(...)` as a primitive fallback only.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

Catalog counterpartiesDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Counterparties");
if (counterpartiesDep == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Counterparties — create it first");
}
TypeItem counterpartiesRef = MdProducedTypesUtil.getProducedType(
    counterpartiesDep, MdTypePackage.Literals.MD_REF_TYPE);
```

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
if (stringType == null) {
    stringType = (TypeItem)typeProvider.createProxy(IEObjectTypeNames.STRING);
}
if (stringType == null) {
    throw new IllegalStateException("Cannot resolve primitive type: " + IEObjectTypeNames.STRING);
}
TypeDescription typeDesc = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build();

article.setType(typeDesc);
catalog.getAttributes().add(article);
```

### Safe checklist
1. Choose the exact child class for the parent (`CatalogAttribute`, `DocumentAttribute`, `TabularSectionAttribute`, ...)
2. Before creating a child, search the parent collection by `getName()`.
   If the child already exists, repair that existing child instead of adding a
   duplicate. If more than one child with the same name already exists, stop
   and run a cleanup workflow first.
3. Set `name` and `uuid` on the new child object
4. Resolve `IEObjectProvider` INSIDE the current transaction
5. Validate every `TypeItem` before `TypeDescriptionBuilder.addType(...)`;
   never pass `null`
6. Build a fresh `TypeDescription` BEFORE adding the object to the parent collection
7. Call `setType(typeDesc)` on every object derived from `BasicFeature`
8. Only after `setType(...)` add the object to `getAttributes()` / `getDimensions()` / `getResources()`

### Wrong vs correct
```java
IV8Project v8project = projectManager.getProject(project);
// WRONG: adding BasicFeature child without type
DocumentAttribute counterparty = mdFactory.createDocumentAttribute();
counterparty.setName("Counterparty");
counterparty.setUuid(UUID.randomUUID());
document.getAttributes().add(counterparty); // md-legacy-emf-check: type is required

// CORRECT: build and assign TypeDescription first.
// If the business type is CatalogRef.Counterparties, resolve that exact reference type.
DocumentAttribute counterparty = mdFactory.createDocumentAttribute();
counterparty.setName("Counterparty");
counterparty.setUuid(UUID.randomUUID());
Catalog counterpartiesDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Counterparties");
if (counterpartiesDep == null) {
    throw new IllegalStateException("Missing referenced catalog: Catalog.Counterparties — create it first");
}
TypeItem catalogRefType = MdProducedTypesUtil.getProducedType(
    counterpartiesDep, MdTypePackage.Literals.MD_REF_TYPE);
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
if (numberType == null) {
    numberType = (TypeItem)typeProvider.createProxy(IEObjectTypeNames.NUMBER);
}
if (numberType == null) {
    throw new IllegalStateException("Cannot resolve primitive type: " + IEObjectTypeNames.NUMBER);
}
TypeDescription quantityType = new TypeDescriptionBuilder()
    .addType(numberType)
    .build();
quantity.setType(quantityType);
products.getAttributes().add(quantity);
```

### Recovery pattern for real error (`Catalog.РђРІС‚РѕСЂС‹` -> `РЎС‚СЂР°РЅР°`)
```java
IV8Project v8project = projectManager.getProject(project);
Catalog authors = (Catalog)transaction.getTopObjectByFqn("Catalog.РђРІС‚РѕСЂС‹");
if (authors != null) {
    IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
        .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
    CatalogAttribute country = authors.getAttributes().stream()
        .filter(a -> "РЎС‚СЂР°РЅР°".equals(a.getName()))
        .findFirst()
        .orElse(null);
    if (country == null) {
        country = mdFactory.createCatalogAttribute();
        country.setName("РЎС‚СЂР°РЅР°");
        country.setUuid(UUID.randomUUID());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        if (stringType == null) {
            stringType = (TypeItem)typeProvider.createProxy(IEObjectTypeNames.STRING);
        }
        if (stringType == null) {
            throw new IllegalStateException("Cannot resolve primitive type: " + IEObjectTypeNames.STRING);
        }
        TypeDescription countryType = new TypeDescriptionBuilder()
            .addType(stringType)
            .setStringQualifiers(100, false)
            .build();
        country.setType(countryType);
        authors.getAttributes().add(country);
    } else if (country.getType() == null || country.getType().getTypes().isEmpty()) {
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        if (stringType == null) {
            stringType = (TypeItem)typeProvider.createProxy(IEObjectTypeNames.STRING);
        }
        if (stringType == null) {
            throw new IllegalStateException("Cannot resolve primitive type: " + IEObjectTypeNames.STRING);
        }
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
- When the user gives a concrete reference (`CatalogRef.РљРѕРЅС‚СЂР°РіРµРЅС‚С‹`, `CatalogRef.РҐСЂР°РЅРёРјС‹РµР¤Р°Р№Р»С‹`, `EnumRef.Р’РёРґС‹РўРѕРІР°СЂРѕРІ`), resolve it via `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)` where `depMdObject = transaction.getTopObjectByFqn("Catalog.X")` / `"Enum.X"`. Do not use generic `IEObjectTypeNames.CATALOG_REF` / `ENUM_REF` as a final type, and do not use `typeProvider.getProxy("CatalogRef.X")` — it returns `null` in JShell.
- If `transaction.getTopObjectByFqn("Catalog.Name")` returns `null`, throw `IllegalStateException` and stop — the dependency must be created first. Otherwise call `MdProducedTypesUtil.getProducedType(...)` directly; do not wait for any "type index refresh" and do not create a transient `McoreFactory.eINSTANCE.createType()` fallback. Never replace a requested reference with `String` or a generic root type.
- Do not use `typeProvider.createProxy(...)` or `IDtConstants.get*RefQName(...)`. Use `"Catalog.Name"` / `"Enum.Name"` for top-object FQNs in `transaction.getTopObjectByFqn(...)` and resolve produced `TypeItem`s via `MdProducedTypesUtil`.
- Exception: `typeProvider.createProxy(IEObjectTypeNames.STRING/NUMBER/BOOLEAN/DATE)` is allowed as a fallback for primitive built-in types when `getProxy(...)` returns `null`. It is not allowed for concrete metadata references like `"EnumRef.X"`.
- If `TypeDescriptionBuilder.addType(...)` fails with `The 'no null' constraint is violated`, the immediate fix is to find which `TypeItem` is `null`, resolve it correctly, and retry. Do not stop with instructions for the user to add the attribute manually.
- Never reuse one `TypeDescription` instance for multiple children. It is an EMF containment object and moves to the latest owner. Reuse `TypeItem` proxies, then build a new `TypeDescription` per child.
- Never use only `anyMatch(...)` as the final child-edit logic. Find the
  existing child by name, reuse it when it exists, and verify the final count
  by name is exactly `1`. `GetMarkers` can be clean even when duplicate
  business attributes exist.
- Before attaching or finishing a bulk CRUD transaction, loop through all new `BasicFeature` children and fail if `getType() == null || getType().getTypes().isEmpty()`
- `CatalogAttribute`, `DocumentAttribute`, and `TabularSectionAttribute` are the most common sources of `md-legacy-emf-check` when `type` is omitted
- For child objects, UUID is still recommended in JShell
- For `IEObjectTypeNames.STRING`, always set finite qualifiers with `.setStringQualifiers(length, false)` to avoid SU8 unlimited-string markers. Default to `.setStringQualifiers(100, false)` or smaller; do not use values greater than 100, such as `150` or `1000`, unless the user explicitly requires it and the current EDT model accepts it.

### Required post-check

After adding an attribute, call `GetMarkers` with `marker_type: "1c"` and `path` to the **parent** top-level object's `.mdo` — never to the attribute itself (attributes are not top-level objects and have no `.mdo` of their own). Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN of the parent — do not `Glob` to find it.**
Schema: `<projectRoot>/src/<TypePluralFolder>/<ParentName>/<ParentName>.mdo`.
After adding a `CatalogAttribute` to `Catalog.Products`, the `.mdo` to check is `src/Catalogs/Products/Products.mdo` — not anything under the attribute's name.

| Parent FQN                          | `.mdo` path                                          |
|-------------------------------------|------------------------------------------------------|
| `Catalog.<Name>`                    | `src/Catalogs/<Name>/<Name>.mdo`                     |
| `Document.<Name>`                   | `src/Documents/<Name>/<Name>.mdo`                    |
| `InformationRegister.<Name>`        | `src/InformationRegisters/<Name>/<Name>.mdo`         |
| `AccumulationRegister.<Name>`       | `src/AccumulationRegisters/<Name>/<Name>.mdo`        |
| `BusinessProcess.<Name>`            | `src/BusinessProcesses/<Name>/<Name>.mdo`            |
| `Task.<Name>`                       | `src/Tasks/<Name>/<Name>.mdo`                        |

Copy `<ParentName>` exactly from the FQN you used in `getTopObjectByFqn("Catalog.<ParentName>")` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.

After marker checks, read the parent object back and count changed children by
name. Report success only when each requested child exists exactly once and has
a non-empty `TypeDescription`.

