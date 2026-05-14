## Safe Workflow: Create Catalog

> 🛑 **STOP — read PRE-FLIGHT first, before writing any code.**
> Skipping PRE-FLIGHT is the most common reason this scenario fails on
> reference attributes.

### ⚠️ Critical API note — how to resolve `CatalogRef.X` / `EnumRef.X` inside JShell

**Do NOT use `typeProvider.getProxy("CatalogRef.X")` for metadata reference
types.** That global type index is populated asynchronously by EDT and is
**not refreshed inside a JShell session** even after the producing
transaction commits. It returns `null` for every freshly-created Catalog /
Enum / Document — the only thing it knows about are primitive types
(`STRING`, `NUMBER`, ...). Relying on it is the single biggest reason this
scenario fails and the LLM falls back to generic `IEObjectTypeNames.CATALOG_REF`
or to a `String` placeholder.

**Use this instead** — `MdProducedTypesUtil.getProducedType(mdObject, eClass)`.
It reads the `TypeItem` directly from the EMF object you already hold
(or fetched via `getTopObjectByFqn`), so it works on objects created earlier
in the same transaction **and** on objects created in a previous transaction
of the same JShell session.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

Catalog suppliersCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
TypeItem suppliersRef = MdProducedTypesUtil.getProducedType(
    suppliersCatalog, MdTypePackage.Literals.MD_REF_TYPE);  // "CatalogRef.Контрагенты"
```

EClass mapping (`MdTypePackage.Literals.*`):

| What user asks for                  | EClass literal       | Resulting `TypeItem.name`               |
|-------------------------------------|----------------------|-----------------------------------------|
| `СправочникСсылка.X` (`CatalogRef`) | `MD_REF_TYPE`        | `CatalogRef.X` / `DocumentRef.X` / ...  |
| `СправочникОбъект.X` (`CatalogObject`) | `MD_OBJECT_TYPE`  | `CatalogObject.X`                       |
| `СправочникСписок.X` (`CatalogList`)| `MD_LIST_TYPE`       | `CatalogList.X`                         |
| Перечисление-ссылка                 | `MD_REF_TYPE`        | `EnumRef.X`                             |
| Табличная часть, row-type           | `MD_ROW_TYPE`        | `CatalogTabularSectionRow.X.Y`          |
| `ОпределяемыйТип`                   | `MD_USER_DEFINED_TYPE` | `DefinedType.X` (TypeSet)             |

`typeProvider.getProxy(...)` is still the correct way to resolve **primitive
built-in types** — `IEObjectTypeNames.STRING`, `NUMBER`, `BOOLEAN`, `DATE`.
Keep using it for those.

### PRE-FLIGHT (mandatory)

Before you generate the catalog-creation code, do this **in order**:

1. **Extract every reference the user named**, including Russian forms like
   `СправочникСсылка.X` → `CatalogRef.X`, `ПеречислениеСсылка.Y` → `EnumRef.Y`,
   `ДокументСсылка.Z` → `DocumentRef.Z`, `ПланВидовХарактеристикСсылка.W` →
   `ChartOfCharacteristicTypesRef.W`. Build an explicit dependency list.
2. **Probe existence of every dependency in one small JShell call** using
   `transaction.getTopObjectByFqn("Catalog.X")` / `"Enum.Y"` / etc.
   Print a structured `MISSING: ...` / `ALL_EXIST` summary via
   `System.out.println(...)` (returning a value from `globalContext.execute`
   alone does not appear in `std_out`).
3. **For every missing dependency, create it first** with its own scenario
   (`create_enum`, `create_catalog`, ...) in a separate JShell transaction.
   After each creation call `GetMarkers` with `marker_type: "1c"` on the new
   `.mdo` and fix any errors.
4. **Create the main catalog**, and for every reference attribute use
   `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`
   where `depMdObject = transaction.getTopObjectByFqn("Catalog.X")` /
   `"Enum.X"` / etc. **Never** use `typeProvider.getProxy("CatalogRef.X")`
   for this — see the critical API note above.

### Hard rules — never violate

- ❌ **Never create the catalog "partially" without the reference attributes
  the user asked for.** That is treated as a failed operation even if
  `compilation_errors` and `runtime_errors` are empty.
- ❌ **Never silently swap a requested `CatalogRef.X` / `EnumRef.X` for
  `String` or for generic `IEObjectTypeNames.CATALOG_REF` / `ENUM_REF`.**
- ❌ **Never finish the task with "пользователь, добавь поля вручную"** for
  metadata attributes that this manual supports.
- ❌ **Never call `typeProvider.getProxy("CatalogRef.X")`** for a metadata
  reference inside JShell — it always returns `null` for freshly-created
  objects. Use `MdProducedTypesUtil.getProducedType(...)`.
- ✅ If a precondition cannot be satisfied, throw `IllegalStateException`
  from inside the BM task. `System.err.println(...) + return null` is **not**
  a failure — JShell will report success.

### Pre-flight dependency probe (copy and adapt)

```java
{
    IProject project = workspaceRoot.getProject("MyProject");
    IBmModel bmModel = modelManager.getModel(project);
    IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

    String result = globalContext.execute(new AbstractBmTask<String>("Probe deps") {
        @Override
        public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
            String[] requiredFqns = {
                "Catalog.Контрагенты",
                "Catalog.ХранимыеФайлы",
                "Enum.ВидыТоваров"
            };
            StringBuilder missing = new StringBuilder();
            for (String fqn : requiredFqns) {
                if (transaction.getTopObjectByFqn(fqn) == null) {
                    missing.append(fqn).append("; ");
                }
            }
            return missing.length() == 0 ? "ALL_EXIST" : "MISSING: " + missing;
        }
    });
    System.out.println(result);
    return result;
}
```

If the probe returns `MISSING: ...`, your next JShell calls **must** be
`create_enum` / `create_catalog` / ... for the missing FQNs, **one
top-level object per JShell transaction**, each followed by `GetMarkers`.
Only after every dependency is created do you submit the main
`create_catalog` transaction.

### Worked example — Catalog.Номенклатура with cross-object references

User asks: создать `Catalog.Номенклатура` со ссылочными реквизитами
`Поставщик → CatalogRef.Контрагенты`,
`Картинка → CatalogRef.ХранимыеФайлы`,
`Вид → EnumRef.ВидыТоваров`,
плюс строковые `Артикул`, `Штрихкод`, `Описание`.
Зависимостей в проекте нет.

Correct ordering of JShell calls (each in its own transaction):

1. **Probe** — script above, expect `MISSING: Catalog.Контрагенты; Catalog.ХранимыеФайлы; Enum.ВидыТоваров;`.
2. **Create Enum.ВидыТоваров** (with values `Товар`, `Услуга`) — `create_enum`. Then `GetMarkers` on its `.mdo`.
3. **Create Catalog.Контрагенты** — minimal catalog. Then `GetMarkers` on its `.mdo`.
4. **Create Catalog.ХранимыеФайлы** — same. Then `GetMarkers` on its `.mdo`.
5. **Create Catalog.Номенклатура with the full attribute set**, resolving
   each reference attribute via
   `MdProducedTypesUtil.getProducedType(transaction.getTopObjectByFqn("Catalog.Контрагенты"), MdTypePackage.Literals.MD_REF_TYPE)`.
   Then `GetMarkers` on its `.mdo`.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

// Inside the main `Catalog.Номенклатура` BM task:
Catalog suppliersCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
if (suppliersCatalog == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Контрагенты — create it first");
}
TypeItem suppliersRef = MdProducedTypesUtil.getProducedType(
    suppliersCatalog, MdTypePackage.Literals.MD_REF_TYPE);

CatalogAttribute supplier = mdFactory.createCatalogAttribute();
supplier.setName("Поставщик");
supplier.getSynonym().put("ru", "Поставщик");
supplier.setUuid(UUID.randomUUID());
supplier.setType(new TypeDescriptionBuilder().addType(suppliersRef).build());
catalog.getAttributes().add(supplier);

// And the same pattern for ХранимыеФайлы and ВидыТоваров — fetch the dep
// MdObject by FQN, build a fresh TypeDescription per attribute.
```

Note: steps 2–4 can in fact be merged into one BM transaction with the
main catalog now that `MdProducedTypesUtil` reads the type directly from
the EMF object (no global index dependency). The "one transaction per
top object" rule still applies if you want incremental marker checks —
prefer that when zero-dependency proven projects are not your target.

---

> вљ пёЏ **HierarchyType вЂ” only two valid constants in EDT API.**
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
        // вќЊ catalog.setHierarchyType(HierarchyType.HIERARCHY_GROUPS);       // does NOT exist
        // вќЊ catalog.setHierarchyType(HierarchyType.HIERARCHY_HIERARCHICAL); // does NOT exist
        // вќЊ catalog.setHierarchyType(HierarchyType.HIERARCHY_NONE);         // does NOT exist вЂ” omit the call instead
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

For a non-hierarchical catalog simply do not call `setHierarchyType(...)` вЂ”
the platform default is correct.

### Safe reference type pattern

When the user names a concrete reference type such as `CatalogRef.РљРѕРЅС‚СЂР°РіРµРЅС‚С‹`,
`CatalogRef.РҐСЂР°РЅРёРјС‹РµР¤Р°Р№Р»С‹`, or `EnumRef.Р’РёРґС‹РўРѕРІР°СЂРѕРІ`, resolve that exact metadata
reference type with `typeProvider.getProxy("CatalogRef.РљРѕРЅС‚СЂР°РіРµРЅС‚С‹")`,
`typeProvider.getProxy("CatalogRef.РҐСЂР°РЅРёРјС‹РµР¤Р°Р№Р»С‹")`, or
`typeProvider.getProxy("EnumRef.Р’РёРґС‹РўРѕРІР°СЂРѕРІ")`. Do not silently replace it with
generic `IEObjectTypeNames.CATALOG_REF` or `IEObjectTypeNames.ENUM_REF`; generic
reference types are valid only when the user explicitly asks for "any catalog" /
"any enum" polymorphism.

Completeness guard: if the user asked for reference attributes, the catalog
creation code must create those attributes in the same operation or throw a
blocking exception. Do not temporarily create "only string attributes", omit
reference attributes, or describe the catalog as complete when requested fields
were skipped.

```java
TypeItem unitsRef = (TypeItem)typeProvider.getProxy("CatalogRef.Units");
if (unitsRef == null) {
    if (transaction.getTopObjectByFqn("Catalog.Units") == null) {
        throw new IllegalStateException("Missing referenced catalog: Catalog.Units");
    }
    throw new IllegalStateException("CatalogRef.Units is not available yet. Run a scoped marker check for Catalog.Units, let EDT refresh produced types, then retry exact typeProvider.getProxy(\"CatalogRef.Units\").");
}
TypeDescription strictType = new TypeDescriptionBuilder()
    .addType(unitsRef)
    .build();
article.setType(strictType);
```

Concrete Russian-name example:

```java
TypeItem suppliersRef = (TypeItem)typeProvider.getProxy("CatalogRef.РљРѕРЅС‚СЂР°РіРµРЅС‚С‹");
TypeItem pictureRef = (TypeItem)typeProvider.getProxy("CatalogRef.РҐСЂР°РЅРёРјС‹РµР¤Р°Р№Р»С‹");
TypeItem kindRef = (TypeItem)typeProvider.getProxy("EnumRef.Р’РёРґС‹РўРѕРІР°СЂРѕРІ");
if (suppliersRef == null) {
    if (transaction.getTopObjectByFqn("Catalog.РљРѕРЅС‚СЂР°РіРµРЅС‚С‹") == null) {
        throw new IllegalStateException("Missing referenced catalog: Catalog.РљРѕРЅС‚СЂР°РіРµРЅС‚С‹");
    }
    throw new IllegalStateException("Exact reference TypeItem is not available yet. Run a scoped marker check for the referenced object, let EDT refresh produced types, then retry exact typeProvider.getProxy(...).");
}
if (pictureRef == null) {
    if (transaction.getTopObjectByFqn("Catalog.РҐСЂР°РЅРёРјС‹РµР¤Р°Р№Р»С‹") == null) {
        throw new IllegalStateException("Missing referenced catalog: Catalog.РҐСЂР°РЅРёРјС‹РµР¤Р°Р№Р»С‹");
    }
    throw new IllegalStateException("Exact reference TypeItem is not available yet. Run a scoped marker check for the referenced object, let EDT refresh produced types, then retry exact typeProvider.getProxy(...).");
}
if (kindRef == null) {
    if (transaction.getTopObjectByFqn("Enum.Р’РёРґС‹РўРѕРІР°СЂРѕРІ") == null) {
        throw new IllegalStateException("Missing referenced enum: Enum.Р’РёРґС‹РўРѕРІР°СЂРѕРІ");
    }
    throw new IllegalStateException("Exact reference TypeItem is not available yet. Run a scoped marker check for the referenced object, let EDT refresh produced types, then retry exact typeProvider.getProxy(...).");
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
prices.getSynonym().put("ru", "Р¦РµРЅС‹");

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
    if (transaction.getTopObjectByFqn("Catalog.PriceTypes") == null) {
        throw new IllegalStateException("Missing referenced catalog: Catalog.PriceTypes");
    }
    throw new IllegalStateException("CatalogRef.PriceTypes is not available yet. Run a scoped marker check for Catalog.PriceTypes, let EDT refresh produced types, then retry exact typeProvider.getProxy(\"CatalogRef.PriceTypes\").");
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
TypeItem coaRef = (TypeItem)typeProvider.getProxy("ChartOfAccounts.РџР»Р°РЅРЎС‡РµС‚РѕРІ");
TypeDescription coaType = new TypeDescriptionBuilder()
    .addType(coaRef)
    .build();
accountDim.setType(coaType);
accReg.getDimensions().add(accountDim);
```

### JShell-safe UUID strategy
вљ пёЏ **WARNING:** `modelFactory.fillDefaultReferences()` may timeout in JShell due to OSGi service limitations.
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
// вќЊ WRONG - UUIDs not set, validation fails
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
transaction.attachTopObject((IBmObject)catalog, fqn);
// Error: SU45 - UUID required
```

### Important notes
- Validate `typeProvider.getProxy(...)` before `addType(...)`; `null` causes `IllegalArgumentException`
- References to metadata objects created in the same unfinished scenario may be unavailable through `getProxy(...)` until EDT refreshes dynamic type indexes. If the referenced top object exists but the exact `CatalogRef.Name` / `EnumRef.Name` proxy is still `null`, do not create a transient fallback type. Validate the referenced object, let EDT refresh produced types, then retry exact `typeProvider.getProxy(...)`.
- Do not use `typeProvider.createProxy(...)`, `IDtConstants.getCatalogRefQName(...)`, or `IDtConstants.getEnumRefQName(...)` for EDT TypeDescription creation in JShell. Use `typeProvider.getProxy("CatalogRef.Name")`, `typeProvider.getProxy("EnumRef.Name")`, etc. Top-object FQNs are still `"Catalog.Name"` / `"Enum.Name"` for `transaction.getTopObjectByFqn(...)`; TypeItem names are `"CatalogRef.Name"` / `"EnumRef.Name"`.
- For blocking preconditions, throw `IllegalStateException`. Do not only print `ERROR` to `System.err` and `return null`, because that can make JShell look successful.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
