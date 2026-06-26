# TypeDescriptionBuilder Best Practices

Use this card before creating or editing metadata fields that store a 1C type: attributes, common attributes, constants, dimensions, resources, chart value types, command parameters, form attributes, and similar `BasicFeature` children.

## Canonical packages

```java
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

⛔ **Wrong packages that cause `cannot find symbol` — never use:**
- `com._1c.g5.v8.dt.mcore.TypeDescriptionBuilder` — does NOT exist; use `com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder`
- `com._1c.g5.v8.dt.mcore.IEObjectTypeNames` — does NOT exist; use `com._1c.g5.v8.dt.platform.IEObjectTypeNames`
- `com._1c.g5.v8.dt.core.mcore.TypeDescriptionBuilder` — does NOT exist (invented package)
- `StringQualifiers.setAllowedLength(boolean)` — method does NOT exist; set string length via `TypeDescriptionBuilder.setStringQualifiers(int length, boolean fixed)`, e.g. `.setStringQualifiers(100, false)`

```java
// For metadata reference types (CatalogRef.X / EnumRef.Y / DocumentRef.Z / ...):
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
```

## ⚠️ How to resolve `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` in JShell

**Do NOT use** `typeProvider.getProxy("CatalogRef.X")` for metadata
reference types in JShell. The global `IEObjectProvider` type index is
populated asynchronously by EDT and is **not refreshed within a JShell
session** — it returns `null` for every Catalog / Enum / Document / etc.
that was created or even already existed when the project opened during
this session. This is the most common reason an LLM falls back to generic
`IEObjectTypeNames.CATALOG_REF` / `ENUM_REF` or to a `String` placeholder.

**Use** `MdProducedTypesUtil.getProducedType(mdObject, eClass)` instead.
It reads the produced `TypeItem` directly from the EMF object — works
inside the same transaction the dependency was created in, and across
transactions of the same session.

```java
Catalog suppliers = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
if (suppliers == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Контрагенты — create it first");
}
TypeItem suppliersRef = MdProducedTypesUtil.getProducedType(
    suppliers, MdTypePackage.Literals.MD_REF_TYPE);   // "CatalogRef.Контрагенты"

attribute.setType(new TypeDescriptionBuilder().addType(suppliersRef).build());
```

`MdTypePackage.Literals.*` mapping:

| User intent                              | EClass literal           | `TypeItem.name`                         |
|------------------------------------------|--------------------------|-----------------------------------------|
| `СправочникСсылка.X` / `EnumRef.X` / ... | `MD_REF_TYPE`            | `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` |
| `СправочникОбъект.X`                     | `MD_OBJECT_TYPE`         | `CatalogObject.X` / `DocumentObject.X`  |
| `СправочникСписок.X`                     | `MD_LIST_TYPE`           | `CatalogList.X` / `DocumentList.X`      |
| Табличная часть, row-type                | `MD_ROW_TYPE`            | `CatalogTabularSectionRow.X.Y`          |
| `ОпределяемыйТип`                        | `MD_USER_DEFINED_TYPE`   | `DefinedType.X` (returns `TypeSet`)     |

`typeProvider.getProxy(IEObjectTypeNames.STRING)` /  `NUMBER` / `BOOLEAN`
/ `DATE` is the correct path for **primitive built-in types** — keep
using it for those. The `null`-on-metadata-refs issue is specific to
metadata-produced reference types.

Do not import `IEObjectProvider` from `mcore` or `metadata.md`. Do not import `TypeDescriptionBuilder` from `dt.md`.

## Core rules

- Resolve `IEObjectProvider` inside the BM transaction where the metadata object is changed.
- Validate every `typeProvider.getProxy(...)` result before `addType(...)`; never pass `null` to `TypeDescriptionBuilder`.
- Build a fresh `TypeDescription` for every target object. `TypeDescription` is EMF containment; reusing one instance moves it to the newest owner and leaves the previous owner without `type`.
- Reuse `TypeItem` proxies when useful, not `TypeDescription` instances.
- Set `TypeDescription` before adding a new `BasicFeature` child to its parent collection.
- For string fields in metadata CRUD, prefer finite length `<= 100`, usually `.setStringQualifiers(100, false)` or a smaller requested length.
- For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first. `Number(10,2)` is `.setNumberQualifiers(2, 10, false)`, not `(10, 2, false)`.

These patterns mirror EDT code that uses `TypeDescriptionBuilder().addType(...)`, `.clone(...)`, and qualifier setters in `C:\Projects\dt` form, moxel, md-ui, ql, and dcs modules.

## Primitive type examples

```java
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
if (stringType == null) {
    throw new IllegalStateException("Cannot resolve STRING type");
}
TypeDescription string100 = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build();

TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
if (numberType == null) {
    throw new IllegalStateException("Cannot resolve NUMBER type");
}
TypeDescription amount = new TypeDescriptionBuilder()
    .addType(numberType)
    .setNumberQualifiers(2, 10, false)
    .build();
```

## Concrete reference type examples

Concrete 1C reference types are project-produced `TypeItem` names. They are not Java classes, and `JShellReflection` cannot resolve `CatalogRef.Контрагенты` or `EnumRef.ВидыТоваров`.

Use `MdProducedTypesUtil.getProducedType(...)` from the referenced metadata object:

```java
Catalog suppliers = (Catalog)transaction.getTopObjectByFqn("Catalog.Контрагенты");
if (suppliers == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Контрагенты");
}
TypeItem supplierRef = MdProducedTypesUtil.getProducedType(
    suppliers, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription supplierType = new TypeDescriptionBuilder()
    .addType(supplierRef)
    .build();

com._1c.g5.v8.dt.metadata.mdclass.Enum itemKinds =
    (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.ВидыТоваров");
if (itemKinds == null) {
    throw new IllegalStateException("Missing dependency: Enum.ВидыТоваров");
}
TypeItem kindRef = MdProducedTypesUtil.getProducedType(
    itemKinds, MdTypePackage.Literals.MD_REF_TYPE);
TypeDescription kindType = new TypeDescriptionBuilder()
    .addType(kindRef)
    .build();
```

Do not replace concrete references with generic `IEObjectTypeNames.CATALOG_REF` / `IEObjectTypeNames.ENUM_REF` unless the user explicitly asks for “any catalog” or “any enum”. Do not replace them with `String`.

Do not create `McoreFactory.eINSTANCE.createType()` fallback for persisted metadata attributes. A transient `Type` is not a project-produced type and can fail later with `Failed to persist reference value`.

## Creation order for reference types

When a task creates multiple metadata objects and some objects reference others, create reference targets first, validate them, then create dependents.

Recommended sequence:

1. Create independent classifiers and plans that produce reference types:
   `Catalog`, `Enum`, `ChartOfAccounts`, `ChartOfCalculationTypes`, `ChartOfCharacteristicTypes`, `ExchangePlan`, `BusinessProcess`, `Task`, `Document`, `DocumentJournal`, `DocumentNumerator`, `Sequence`, `Report`, `DataProcessor`, `SettingsStorage`, `FilterCriterion`.
2. Run scoped `GetMarkers` for each created target `.mdo`.
3. Resolve produced reference types from the dependency object with `MdProducedTypesUtil.getProducedType(dep, MdTypePackage.Literals.MD_REF_TYPE)`. Do not use `typeProvider.getProxy("...Ref.Name")` for metadata-produced refs in JShell.
4. Create dependent objects and fields that reference those targets:
   catalog/document attributes, tabular section attributes, register dimensions/resources, common attributes, constants, chart value types, command parameters.
5. Create registers after their dimensions' reference targets exist. For recorder-dependent registers, create/link registrar documents before reporting completion.
6. Create business processes together with or after their `Task` dependency.
7. Create forms, commands, templates, modules, routes, service operations, and UI/content details after their owner metadata object exists.

Common examples:

| Needed type | Create first | Resolve later |
| --- | --- | --- |
| `CatalogRef.Контрагенты` | `Catalog.Контрагенты` | `MdProducedTypesUtil.getProducedType(catalog, MdTypePackage.Literals.MD_REF_TYPE)` |
| `CatalogRef.ХранимыеФайлы` | `Catalog.ХранимыеФайлы` | `MdProducedTypesUtil.getProducedType(catalog, MdTypePackage.Literals.MD_REF_TYPE)` |
| `EnumRef.ВидыТоваров` | `Enum.ВидыТоваров` | `MdProducedTypesUtil.getProducedType(enumObj, MdTypePackage.Literals.MD_REF_TYPE)` |
| document registrar refs | target `Document.*` | link via register/document recorder APIs |
| accounting dimensions | `ChartOfAccounts.*` | exact chart/account produced types after chart exists |
| calculation dimensions | `ChartOfCalculationTypes.*` | exact calculation type produced types after chart exists |
| characteristic values | `ChartOfCharacteristicTypes.*` and its value type | exact characteristic produced types after chart exists |
| exchange nodes | `ExchangePlan.*` | exact exchange-plan produced types after plan exists |
| business process tasks | `Task.*` | set `BusinessProcess.task` to the existing task |

## Wrong patterns

```java
// WRONG: concrete reference requested, but generic root used.
TypeItem supplier = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);

// WRONG: concrete reference requested, but String used.
attribute.setType(new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build());

// WRONG: transient Type fallback for persisted metadata.
Type transientType = McoreFactory.eINSTANCE.createType();
transientType.setName("CatalogRef.Контрагенты");
attribute.setType(new TypeDescriptionBuilder().addType(transientType).build());

// WRONG: one TypeDescription instance reused for several children.
TypeDescription shared = new TypeDescriptionBuilder().addType(stringType).build();
attr1.setType(shared);
attr2.setType(shared);
```

## Error recovery

| Symptom | Action |
| --- | --- |
| `The 'no null' constraint is violated` in `addType` | Check whether the dependency object is `null`; create the referenced metadata first, then resolve via `MdProducedTypesUtil.getProducedType(...)`. |
| `Failed to persist reference value` | Remove transient/manual `Type` fallback and use a project-produced `TypeItem` from `MdProducedTypesUtil`. |
| `type` marker on attribute/dimension/resource | Assign a fresh non-empty `TypeDescription` before adding or saving the child. |
| String length/range marker | Use `.setStringQualifiers(100, false)` or a smaller valid requested length. |
| Number scale/precision marker | Ensure `scale <= precision` and use `setNumberQualifiers(scale, precision, nonNegative)`. |
