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

Use exact proxies:

```java
TypeItem supplierRef = (TypeItem)typeProvider.getProxy("CatalogRef.Контрагенты");
if (supplierRef == null) {
    throw new IllegalStateException("CatalogRef.Контрагенты is not available. Create Catalog.Контрагенты first and retry after EDT updates produced types.");
}
TypeDescription supplierType = new TypeDescriptionBuilder()
    .addType(supplierRef)
    .build();

TypeItem kindRef = (TypeItem)typeProvider.getProxy("EnumRef.ВидыТоваров");
if (kindRef == null) {
    throw new IllegalStateException("EnumRef.ВидыТоваров is not available. Create Enum.ВидыТоваров first and retry after EDT updates produced types.");
}
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
3. Let EDT update produced types. If `typeProvider.getProxy("...Ref.Name")` is still `null`, refresh/build or retry in a new JShell operation; do not use a generic/string/transient fallback.
4. Create dependent objects and fields that reference those targets:
   catalog/document attributes, tabular section attributes, register dimensions/resources, common attributes, constants, chart value types, command parameters.
5. Create registers after their dimensions' reference targets exist. For recorder-dependent registers, create/link registrar documents before reporting completion.
6. Create business processes together with or after their `Task` dependency.
7. Create forms, commands, templates, modules, routes, service operations, and UI/content details after their owner metadata object exists.

Common examples:

| Needed type | Create first | Use exact proxy later |
| --- | --- | --- |
| `CatalogRef.Контрагенты` | `Catalog.Контрагенты` | `typeProvider.getProxy("CatalogRef.Контрагенты")` |
| `CatalogRef.ХранимыеФайлы` | `Catalog.ХранимыеФайлы` | `typeProvider.getProxy("CatalogRef.ХранимыеФайлы")` |
| `EnumRef.ВидыТоваров` | `Enum.ВидыТоваров` | `typeProvider.getProxy("EnumRef.ВидыТоваров")` |
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
| `The 'no null' constraint is violated` in `addType` | Check which proxy is `null`; create the referenced metadata first or retry after produced types refresh. |
| `Failed to persist reference value` | Remove transient/manual `Type` fallback and use an exact project-produced `TypeItem` proxy. |
| `type` marker on attribute/dimension/resource | Assign a fresh non-empty `TypeDescription` before adding or saving the child. |
| String length/range marker | Use `.setStringQualifiers(100, false)` or a smaller valid requested length. |
| Number scale/precision marker | Ensure `scale <= precision` and use `setNumberQualifiers(scale, precision, nonNegative)`. |
