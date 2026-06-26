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

- ✅ **Use exact packages from this workflow; do not guess imports.** Observed bad imports:
  `com._1c.g5.v8.dt.core.project.IV8Project`,
  `com._1c.g5.v8.dt.metadata.mcore.*`, and
  `com._1c.g5.v8.dt.metadata.mdclass.CodeType`,
  `com._1c.g5.v8.dt.mcore.util.McorePackage`, and
  `com._1c.g5.v8.bm.core.IBmModel` do not work in JShell. Use
  `com._1c.g5.v8.dt.core.platform.IV8Project`, `com._1c.g5.v8.dt.mcore.*`, and avoid setting code
  type unless required. `IBmModel` is `com._1c.g5.v8.bm.integration.IBmModel`. If code type is
  required, verify the exact enum with `JShellReflection` instead of inventing `CodeType` or
  `CatalogCodeType.String`.
- ✅ **If the prompt asks for a catalog and a form, this is a two-step workflow.**
  First create the top-level `Catalog` and validate its `.mdo`. Then immediately call
  `JShellManual` for `create_object_form` and create/repair the generated form structure with
  `formGenerator`. After `Form.form` exists, immediately run the mandatory safe improvement pass
  through `edit_form`: read the generated file, apply obvious exact presentation edits when possible,
  and run `GetMarkers`. If the generated form has standard `Object.Code` / `Object.Description`
  controls, at least one `Edit` call on `Form.form` is mandatory. Do not stop after the catalog
  `.mdo` exists or after a raw default form is generated. A final untouched default form is not a
  completed answer for "создай справочник ... и форму". For a simple catalog with only standard
  fields, the expected minimal improvement is not a cosmetic form title only: present
  `Object.Description` as `Наименование`, `Object.Code` as `Код`, and when exact XML movement is
  safe, group them in a compact `UsualGroup` with `HorizontalIfPossible`, matching common SSL
  catalog item forms.
- ✅ **Do not invent attributes.** If the user only says "создай справочник товаров" or
  "создай справочник товаров и форму", create the catalog with standard code/description only.
  Add custom attributes (`Артикул`, `Цена`, `Количество`, ...) only when the user explicitly asks
  for them. Extra guessed attributes increase marker risk and are not part of the request.
- ⛔ **Run inside `bmModel.getGlobalContext().execute(new AbstractBmTask<...>(){...})` — never
  `modelManager.executeReadWriteTask(...)` / `IBmSingleNamespaceTask`.** Only the global editing
  context auto-saves to disk; the other entries commit in-memory only (compiles, prints "created",
  but no `.mdo` is written and it vanishes on restart).
- ✅ **Use the exact project name from `GetProjects` / the user request. Never translate it.**
  If the project is named `Склад`, executable JShell must call
  `workspaceRoot.getProject("Склад")`, not `"Warehouse"`. If a translated or wrong project name
  caused `project == null` / `NullPointerException`, start a fresh `jshellsession` and retry with
  the exact project name before doing any metadata work.
- ✅ **Before creating a catalog, check `transaction.getTopObjectByFqn("Catalog.<Name>")`.**
  If it is non-null, do not call `transaction.attachTopObject(...)` with the
  same FQN. Treat the object as already created and continue with the requested
  follow-up work (for example `create_object_template` or `create_object_form`).
  Do not throw `IllegalStateException("Catalog already exists ...")` in a broad
  prompt after partial success; that makes retries dirty. Re-running a broad
  prompt after partial success must continue from existing objects, not recreate
  them.
- ✅ **For a simple catalog prompt, do not use `TypeDescriptionBuilder` at all.** A request like
  "создай справочник X" or "создай справочник X и табличный макет Y" needs only standard
  code/description settings on `Catalog`; no custom `CatalogAttribute`, no `McorePackage`, no
  primitive type provider.
- ✅ **Every `CatalogAttribute` must receive a non-empty `TypeDescription`
  before `catalog.getAttributes().add(attribute)`.** Do not rely on a later
  fix-up transaction for simple fields such as name/surname/description.
- ❌ **Never create custom catalog attributes named `Код`, `Code`,
  `Наименование`, or `Description`.** Catalog already has standard code and
  description fields. Configure them with `catalog.setCodeLength(...)`,
  `catalog.setCodeType(...)` when needed, and
  `catalog.setDescriptionLength(...)`. Creating child attributes with those
  names produces SU45 markers: "coincides with a standard attribute name".
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
- ❌ **Do not use very large string lengths (`setStringQualifiers(500, ...)`,
  `1000`, `2000`, ...) in ordinary JShell CRUD scenarios unless an exact
  long-text workflow is known and verified for this EDT version.** Prefer
  conservative lengths up to `100` for ordinary text attributes while testing
  metadata CRUD.
- ✅ **For number attributes, scale comes before precision.** If the user explicitly asks for a
  number attribute, call `setNumberQualifiers(scale, precision, nonNegative)`, for example
  `Number(15,2)` is `setNumberQualifiers(2, 15, true)`. Never produce `.mdo` where
  `<scale>` is greater than `<precision>`.
- ❌ **Never import, instantiate, or configure `NumberQualifiers` directly in JShell.**
  It is abstract in this EDT API. Do not write `new NumberQualifiers()`,
  `numberQualifiers.setPrecision(...)`, or `numberQualifiers.setScale(...)`.
  Use only the builder call above; for `Number(10,2)` write
  `.setNumberQualifiers(2, 10, false)`.
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
        Catalog existing = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (existing != null) {
            return existing; // partial retry: continue with follow-up work, do not reattach
        }

        Catalog catalog = mdFactory.createCatalog();
        catalog.setName("Products");
        catalog.getSynonym().put("ru", "Products");
        catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);
        // вќЊ catalog.setHierarchyType(HierarchyType.HIERARCHY_GROUPS);       // does NOT exist
        // вќЊ catalog.setHierarchyType(HierarchyType.HIERARCHY_HIERARCHICAL); // does NOT exist
        // вќЊ catalog.setHierarchyType(HierarchyType.HIERARCHY_NONE);         // does NOT exist вЂ” omit the call instead
        catalog.setCodeLength(9);
        catalog.setDescriptionLength(150);

        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)
        catalog.setUuid(UUID.randomUUID());

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

When the user names a concrete reference type such as `CatalogRef.Units`,
`CatalogRef.Контрагенты`, or `EnumRef.ВидыТоваров`, resolve that exact metadata
reference type from the referenced metadata object with
`MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`.
Do not use `typeProvider.getProxy("CatalogRef.X")` for metadata-produced refs
inside JShell; keep `typeProvider.getProxy(...)` for primitive built-in types
such as `STRING`, `NUMBER`, `BOOLEAN`, and `DATE`.

Completeness guard: if the user asked for reference attributes, the catalog
creation code must create those attributes in the same operation or throw a
blocking exception. Do not temporarily create "only string attributes", omit
reference attributes, or describe the catalog as complete when requested fields
were skipped.

```java
Catalog unitsCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Units");
if (unitsCatalog == null) {
    throw new IllegalStateException("Missing referenced catalog: Catalog.Units");
}
TypeItem unitsRef = MdProducedTypesUtil.getProducedType(
    unitsCatalog, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription strictType = new TypeDescriptionBuilder()
    .addType(unitsRef)
    .build();
article.setType(strictType);
```

Concrete Russian-name example:

```java
Catalog suppliersCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
Catalog pictureCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.ХранимыеФайлы");
com._1c.g5.v8.dt.metadata.mdclass.Enum kindEnum =
    (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.ВидыТоваров");
if (suppliersCatalog == null) throw new IllegalStateException("Missing referenced catalog: Catalog.Контрагенты");
if (pictureCatalog == null) throw new IllegalStateException("Missing referenced catalog: Catalog.ХранимыеФайлы");
if (kindEnum == null) throw new IllegalStateException("Missing referenced enum: Enum.ВидыТоваров");

TypeItem suppliersRef = MdProducedTypesUtil.getProducedType(suppliersCatalog, MdTypePackage.Literals.MD_REF_TYPE);
TypeItem pictureRef = MdProducedTypesUtil.getProducedType(pictureCatalog, MdTypePackage.Literals.MD_REF_TYPE);
TypeItem kindRef = MdProducedTypesUtil.getProducedType(kindEnum, MdTypePackage.Literals.MD_REF_TYPE);
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
Catalog productsCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
if (productsCatalog == null) {
    throw new IllegalStateException("Missing referenced catalog: Catalog.Products");
}
TypeItem productsRef = MdProducedTypesUtil.getProducedType(
    productsCatalog, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription productType = new TypeDescriptionBuilder()
    .addType(productsRef)
    .build();
productDim.setType(productType);
prices.getDimensions().add(productDim);

// Dimension 2: PriceType (specific catalog reference when the exact catalog is required)
InformationRegisterDimension priceTypeDim = mdFactory.createInformationRegisterDimension();
priceTypeDim.setName("PriceType");
Catalog priceTypesCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.PriceTypes");
if (priceTypesCatalog == null) {
    throw new IllegalStateException("Missing referenced catalog: Catalog.PriceTypes");
}
TypeItem priceTypesRef = MdProducedTypesUtil.getProducedType(
    priceTypesCatalog, MdTypePackage.Literals.MD_REF_TYPE);
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

The attribute part of this example is only for prompts that explicitly ask for a custom attribute.
For a simple catalog request, use the standard code/description-only create example above and skip
all `CatalogAttribute` / `TypeDescriptionBuilder` code.
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
- Do not model the standard 1C catalog fields `Код` and `Наименование` as
  `CatalogAttribute` children. They are built into `Catalog`; use
  `setCodeLength(...)` and `setDescriptionLength(...)` instead.
- Use `typeProvider.getProxy(...)` for primitive built-in types only: `STRING`, `NUMBER`, `BOOLEAN`, `DATE`.
- For metadata-produced references such as `CatalogRef.Name` or `EnumRef.Name`, fetch the referenced top object with `transaction.getTopObjectByFqn("Catalog.Name")` / `"Enum.Name"` and resolve the `TypeItem` with `MdProducedTypesUtil.getProducedType(dep, MdTypePackage.Literals.MD_REF_TYPE)`.
- Do not use `typeProvider.createProxy(...)`, `IDtConstants.getCatalogRefQName(...)`, `IDtConstants.getEnumRefQName(...)`, transient `McoreFactory` types, or generic `IEObjectTypeNames.CATALOG_REF` / `ENUM_REF` as fallbacks for concrete persisted metadata references.
- For blocking preconditions, throw `IllegalStateException`. Do not only print `ERROR` to `System.err` and `return null`, because that can make JShell look successful.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
Schema: `<projectRoot>/src/<TypePluralFolder>/<Name>/<Name>.mdo`.
For the types you may create from this scenario:

| FQN prefix             | `.mdo` path                                    |
|------------------------|------------------------------------------------|
| `Catalog.<Name>`       | `src/Catalogs/<Name>/<Name>.mdo`               |
| `Enum.<Name>`          | `src/Enums/<Name>/<Name>.mdo`                  |
| `Document.<Name>`      | `src/Documents/<Name>/<Name>.mdo`              |
| `InformationRegister.<Name>` | `src/InformationRegisters/<Name>/<Name>.mdo` |
| `AccumulationRegister.<Name>` | `src/AccumulationRegisters/<Name>/<Name>.mdo` |
| `ChartOfAccounts.<Name>` | `src/ChartsOfAccounts/<Name>/<Name>.mdo`     |

Rules: use the project's path separator (`\\` on Windows, `/` on Linux); copy
`<Name>` exactly from the FQN you used in `getTopObjectByFqn("Catalog.<Name>")`
— same case, same Cyrillic; extension is lowercase `.mdo`. For the full
FQN → folder mapping (registers, plans, business processes, services,
forms, etc.) see the `check_1c_markers_after_crud` scenario.

Use project-wide markers only when the change can affect references between
metadata objects (delete, rename, registrar links, command interfaces,
configuration-level changes) or when the path truly cannot be derived.

If the prompt also asks for a template/layout (`макет`, `табличный макет`, `печатный макет`), do not
stop after catalog creation: run `create_object_template`. After that scenario prints
`Body exists: true` from `project.getFile("src/Catalogs/<Name>/Templates/<Template>/Template.mxlx").exists()`,
the body check is complete: do not call `Glob` or `Read` to inspect/search the template folder. A
successful `Glob` is still a dirty scenario. Finish with `GetMarkers` on the catalog owner `.mdo`;
for object-owned templates there is no `Templates/<Template>/<Template>.mdo`.
