## Safe Workflow: Create Document

### Canonical imports — copy these FQNs exactly, do not guess

`AbstractBmTask`, `IBmTransaction`, `Configuration`, `Document` etc. resolve
via these packages. If you choose to write a fully-qualified name instead
of relying on JShell's auto-import, use these. **Common LLM mistake**:
typing `com._1c.g5.emf.model.bm.job.AbstractBmTask` or
`com._1c.g5.v8.dt.bm.integration.AbstractBmTask` — both are wrong and
produce `package does not exist` compilation errors.

```java
// BM transaction infrastructure
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
// Project
import com._1c.g5.v8.dt.core.platform.IV8Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
// MCore + TypeDescription
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
// Metadata mdclass
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.TabularSectionAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterType;
// Metadata reference types — see Critical API note below
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
// JDK
import java.util.UUID;
```

Wrong package paths you must not invent:

- ❌ `com._1c.g5.emf.model.bm.*` (no such package)
- ❌ `com._1c.g5.v8.dt.bm.integration.*` (extra `dt.`)
- ❌ `com._1c.g5.v8.dt.bm.core.*` (extra `dt.`, then `core` instead of `integration`)
- ❌ `com._1c.g5.v8.dt.bsl.common.*` (BSL package, no BM types live here)
- ❌ `com._1c.g5.v8.dt.metadata.mcore.*` (`mcore` is one level up: `com._1c.g5.v8.dt.mcore`)
- ❌ `com._1c.g5.dt.core.platform.*` (missing `v8.`)
- ❌ `com._1c.g5.v8.dt.platform.version.*` (no `version` subpackage)

#### The `bm.core` vs `bm.integration` split — read carefully

This is the **single most common LLM mistake**. Both packages exist and
have similar-sounding names, but they hold **different** types. Pick the
right one or compilation fails with `package does not exist` or
`cannot find symbol`:

| Type                          | Package                              | Why                                            |
|-------------------------------|--------------------------------------|------------------------------------------------|
| `IBmObject`                   | `com._1c.g5.v8.bm.core`              | low-level BM persistence interface             |
| `AbstractBmTask<T>`           | `com._1c.g5.v8.bm.integration`       | high-level transactional API                   |
| `IBmTransaction`              | `com._1c.g5.v8.bm.integration`       | passed into `AbstractBmTask.execute`           |
| `IBmModel`                    | `com._1c.g5.v8.bm.integration`       | obtained from `modelManager.getModel(project)` |
| `IBmGlobalEditingContext`     | `com._1c.g5.v8.bm.integration`       | `bmModel.getGlobalContext()`                   |

If a class name starts with `IBm` and is used inside a BM task body, it
is **almost always** `com._1c.g5.v8.bm.integration` — except `IBmObject`,
which is `com._1c.g5.v8.bm.core`. Do not mix the two.

#### JShell session hygiene

Inside one JShell session (same `repl_session_id`) JShell remembers every
top-level declaration. If you declare `IProject project = ...` in call A
and then re-declare it as `IProject складProject = ...` in call B, the
shared classloader may still reference the **old** name and throw
`java.lang.NoSuchFieldError: project` at runtime. Mitigations:

- **Keep variable names stable across calls in the same session.** Pick
  `project`, `v8project`, `bmModel`, `globalContext` once and reuse them.
- If a previous call failed and you want to retry with renamed locals,
  ask for a fresh `jshellsession` first.
- Prefer `var` over explicit types for local declarations — JShell
  handles re-declarations of `var`-typed variables more cleanly.

`v8project.getVersion()` is a **method call** that returns a `Version` enum
value, not an import. Do not write `com._1c.g5.v8.dt.platform.version.X`
anywhere.

For other entity types not listed above (registers other than accumulation,
charts, business processes, etc.) read `jshell_edt_canonical_imports` for
the full canonical list.

---

Before writing code, decide which attributes are custom. Do not create document attributes named `Date`, `Р”Р°С‚Р°`,
`Number`, `РќРѕРјРµСЂ`, `Posted`, `РџСЂРѕРІРµРґРµРЅ`, `Ref`, `РЎСЃС‹Р»РєР°`, `DeletionMark`, or `РџРѕРјРµС‚РєР°РЈРґР°Р»РµРЅРёСЏ`: these are standard
document properties and EDT reports SU45 name/type markers if they are added as custom attributes.

When a document has many attributes, prefer small helper methods that return a NEW `TypeDescription` on every call.
Never store one `TypeDescription` and assign it to two attributes.

### ⚠️ Critical API note — resolving `CatalogRef.X` / `EnumRef.X` / `DocumentRef.X` in JShell

**Do NOT use `typeProvider.getProxy("CatalogRef.X")` for metadata reference
types.** The global `IEObjectProvider` type index is populated asynchronously
by EDT and is **not refreshed within a JShell session** — it returns `null`
for every freshly-created Catalog / Document / Enum and even for ones that
already existed when the project opened. Use
`MdProducedTypesUtil.getProducedType(mdObject, eClass)` instead — it reads
the `TypeItem` directly from the EMF object you fetched via
`transaction.getTopObjectByFqn(...)`.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;

Catalog warehouses = (Catalog)transaction.getTopObjectByFqn("Catalog.Warehouses");
if (warehouses == null) {
    throw new IllegalStateException("Missing dependency: Catalog.Warehouses — create it first");
}
TypeItem warehouseRef = MdProducedTypesUtil.getProducedType(
    warehouses, MdTypePackage.Literals.MD_REF_TYPE); // "CatalogRef.Warehouses"
```

EClass mapping: `MD_REF_TYPE` → `CatalogRef.X` / `DocumentRef.X` / `EnumRef.X`;
`MD_OBJECT_TYPE` → `CatalogObject.X`; `MD_LIST_TYPE` → `CatalogList.X`;
`MD_ROW_TYPE` → `CatalogTabularSectionRow.X.Y`; `MD_USER_DEFINED_TYPE` →
`DefinedType.X`. Primitive types (`STRING`, `NUMBER`, `BOOLEAN`, `DATE`) still
use `typeProvider.getProxy(IEObjectTypeNames.STRING)` — keep that idiom.

### Preflight-blocked patterns

Do not send JShell code with these EDT patterns:

```java
// WRONG: do not call setStringQualifiers with length 150, 1000, or any value above 100.

// WRONG: Ecore data types are not EDT TypeItem values.
TypeItem stringType = (TypeItem)modelFactory.create(EcorePackage.Literals.ESTRING, v8project);
TypeItem numberType = (TypeItem)modelFactory.create(EcorePackage.Literals.EINT, v8project);

// WRONG: the requested type is CatalogRef.РќРѕРјРµРЅРєР»Р°С‚СѓСЂР°, but this is generic CatalogRef.
TypeItem productType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
```

Use this pattern instead:

```java
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);

TypeDescription nameType = new TypeDescriptionBuilder()
    .addType(stringType)
    .setStringQualifiers(100, false)
    .build();
```

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

Document document = globalContext.execute(new AbstractBmTask<Document>("Create document") {
    @Override
    public Document execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        // Check if document already exists to avoid BmFqnAlreadyInUseException
        String documentFqn = "Document.GoodsReceipt";
        if (transaction.getTopObjectByFqn(documentFqn) != null) {
            System.out.println("Document already exists: " + documentFqn);
            return null;
        }

        // Create document
        Document document = mdFactory.createDocument();
        document.setName("GoodsReceipt");
        document.getSynonym().put("ru", "РџСЂРёС…РѕРґ С‚РѕРІР°СЂРѕРІ");

        // Set document number type - IMPORTANT: use correct enum constant
        document.setNumberType(DocumentNumberType.NUMBER);
        document.setNumberLength(9);
        document.setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL);
        document.setRealTimePosting(RealTimePosting.DENY);

        // Create type provider INSIDE transaction
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

        // Add warehouse attribute (Catalog reference)
        DocumentAttribute warehouse = mdFactory.createDocumentAttribute();
        warehouse.setName("Warehouse");
        warehouse.getSynonym().put("ru", "РЎРєР»Р°Рґ");
        // Resolve CatalogRef.Warehouses via MdProducedTypesUtil — NOT via typeProvider.getProxy("CatalogRef.Warehouses").
        Catalog warehousesDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Warehouses");
        if (warehousesDep == null) {
            throw new IllegalStateException("Missing referenced catalog: Catalog.Warehouses — create it first");
        }
        TypeItem catalogRefType = MdProducedTypesUtil.getProducedType(
            warehousesDep, MdTypePackage.Literals.MD_REF_TYPE);
        TypeDescription warehouseType = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();
        warehouse.setType(warehouseType);
        document.getAttributes().add(warehouse);

        // вљ пёЏ WARNING: Do NOT create "Date" attribute - it conflicts with standard document property
        // Documents have standard attributes: Date, Number, Posted, Ref - these are built-in
        // Add custom attributes only (do not use names: Date, Number, Posted, DeletionMark, Ref)

        // Add tabular section with typed line attributes
        DocumentTabularSection products = mdFactory.createDocumentTabularSection();
        products.setName("Products");
        products.getSynonym().put("ru", "РўРѕРІР°СЂС‹");
        products.setUuid(UUID.randomUUID());

        TabularSectionAttribute product = mdFactory.createTabularSectionAttribute();
        product.setName("Product");
        product.getSynonym().put("ru", "РќРѕРјРµРЅРєР»Р°С‚СѓСЂР°");
        Catalog productsDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (productsDep == null) {
            throw new IllegalStateException("Missing referenced catalog: Catalog.Products — create it first");
        }
        TypeItem productsRefType = MdProducedTypesUtil.getProducedType(
            productsDep, MdTypePackage.Literals.MD_REF_TYPE);
        TypeDescription productType = new TypeDescriptionBuilder()
            .addType(productsRefType)
            .build();
        product.setType(productType);
        product.setUuid(UUID.randomUUID());
        products.getAttributes().add(product);

        TabularSectionAttribute quantity = mdFactory.createTabularSectionAttribute();
        quantity.setName("Quantity");
        quantity.getSynonym().put("ru", "РљРѕР»РёС‡РµСЃС‚РІРѕ");
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription quantityType = new TypeDescriptionBuilder()
            .addType(numberType)
            .setNumberQualifiers(0, 10, false)
            .build();
        quantity.setType(quantityType);
        quantity.setUuid(UUID.randomUUID());
        products.getAttributes().add(quantity);
        document.getTabularSections().add(products);

        // Validate all child TypeDescription values before attach.
        // TypeDescription is containment: never reuse the same instance across children.
        for (DocumentAttribute attribute : document.getAttributes()) {
            if (attribute.getType() == null || attribute.getType().getTypes().isEmpty()) {
                System.err.println("ERROR: Missing TypeDescription for document attribute: " + attribute.getName());
                return null;
            }
        }
        for (DocumentTabularSection section : document.getTabularSections()) {
            for (TabularSectionAttribute attribute : section.getAttributes()) {
                if (attribute.getType() == null || attribute.getType().getTypes().isEmpty()) {
                    System.err.println("ERROR: Missing TypeDescription for tabular section attribute: "
                        + section.getName() + "." + attribute.getName());
                    return null;
                }
            }
        }

        // Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)
        document.setUuid(UUID.randomUUID());
        warehouse.setUuid(UUID.randomUUID());

        // Generate FQN and attach to transaction
        String fqn = fqnGenerator.generateStandaloneObjectFqn(document.eClass(), document.getName()).toString();
        transaction.attachTopObject((IBmObject)document, fqn);
        configuration.getDocuments().add(document);

        System.out.println("Document created successfully: " + fqn);
        return document;
    }
});
```
**IMPORTANT Notes:**
- **DocumentNumberType.NUMBER** (not `Number`) - use correct enum constant
- **DocumentNumberPeriodicity.NONPERIODICAL** (not `Nonperiodical`) - use correct enum constant
- **Do not call `document.setPosted(...)`** - this method is not present in EDT API
- **`setRealTimePosting(...)` expects `RealTimePosting` enum** such as `RealTimePosting.DENY` or `RealTimePosting.ALLOW`
- **TypeDescriptionBuilder** must be used INSIDE the transaction
- **IEObjectProvider** must use `v8project.getVersion()` for version compatibility
- **UUIDs** MUST be set for document and all attributes to avoid SU45 errors
- **Every `DocumentAttribute` and `TabularSectionAttribute` must call `setType(...)`** before `add(...)`; otherwise EDT reports `md-legacy-emf-check` / `type is required`
- **Never reuse the same `TypeDescription` instance for multiple children.** `TypeDescription` is containment; assigning it to another attribute moves it away from the previous owner and causes `type is required` markers. Reuse `TypeItem`, not `TypeDescription`.
- **For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first.** For `Number(10,2)`, call `.setNumberQualifiers(2, 10, false)`, not `.setNumberQualifiers(10, 2, false)`.
- **For concrete metadata references, use `MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`.** `CatalogRef.РќРѕРјРµРЅРєР»Р°С‚СѓСЂР°` means fetch `Catalog.РќРѕРјРµРЅРєР»Р°С‚СѓСЂР°` via `transaction.getTopObjectByFqn("Catalog.РќРѕРјРµРЅРєР»Р°С‚СѓСЂР°")` and call `MdProducedTypesUtil.getProducedType(...)`, not generic `IEObjectTypeNames.CATALOG_REF` and not `typeProvider.getProxy("CatalogRef.X")` (returns `null` in JShell). The same for `EnumRef.X` / `DocumentRef.X` / `ChartOfAccountsRef.X` / etc.
- If `transaction.getTopObjectByFqn("Catalog.Name")` returns `null`, throw `IllegalStateException` and stop — the dependency must be created first. Otherwise call `MdProducedTypesUtil.getProducedType(...)` directly; do not wait for any "type index refresh" and do not create a transient `McoreFactory.eINSTANCE.createType()` fallback. Never replace a requested reference with `String` or a generic root type.
- **Before `attachTopObject`, verify all document attributes and tabular section attributes have non-null/non-empty `getType()`**
- **Check before creating** to avoid `BmFqnAlreadyInUseException`

**вљ пёЏ CRITICAL: Standard Document Attributes**
- **NEVER create custom attributes with names matching standard document properties**
- Standard document attributes (built-in, cannot be overridden): `Date`, `Number`, `Posted`, `Ref`, `DeletionMark`
- Russian standard names are also reserved: `Р”Р°С‚Р°`, `РќРѕРјРµСЂ`, `РџСЂРѕРІРµРґРµРЅ`, `РЎСЃС‹Р»РєР°`, `РџРѕРјРµС‚РєР°РЈРґР°Р»РµРЅРёСЏ`
- Trying to create an attribute named "Date" will cause validation error: "РќРµРєРѕСЂСЂРµРєС‚РЅРѕРµ Р·РЅР°С‡РµРЅРёРµ СЃРІРѕР№СЃС‚РІР° \"name\" СЂРµРєРІРёР·РёС‚Р° \"Date\". РЎРѕРІРїР°РґР°РµС‚ СЃ РёРјРµРЅРµРј СЃС‚Р°РЅРґР°СЂС‚РЅРѕРіРѕ СЂРµРєРІРёР·РёС‚Р°"
- Only create custom attributes with unique names (e.g., Warehouse, Customer, Amount, etc.)

**Register Registers for Accumulation/Accounting Registers:**
- After creating a document, you can add registers it records to via `document.getRegisterRecords().add(register)`
- Example:
```java
AccumulationRegister stockRegister = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
if (stockRegister != null) {
    document.getRegisterRecords().add(stockRegister);
}
```
- Registers are configured on documents, not on registers themselves
- Each document can record to multiple registers (accumulation, accounting, information, calculation)

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
For a `Document.<Name>` the path is always:

```
<projectRoot>/src/Documents/<Name>/<Name>.mdo
```

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn("Document.<Name>")` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping (catalogs, enums, registers, etc. that this scenario may also touch as dependencies).

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
