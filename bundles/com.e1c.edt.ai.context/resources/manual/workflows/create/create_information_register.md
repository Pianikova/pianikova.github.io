## Safe Workflow: Create InformationRegister

If the prompt asks for an information register and a form, this is a chained workflow. First create
and validate the `InformationRegister` `.mdo`; then immediately call `JShellManual` for
`create_object_form` and create or repair the generated form structure through EDT `formGenerator`.
After `Form.form` exists, read it, apply the mandatory safe `Edit` improvement pass, and run
`GetMarkers`. Do not report success after only the register `.mdo` exists or after a raw untouched
default form is generated. Never use `Write` for `.form` or owner `.mdo` files.

Use the exact project name from `GetProjects` / the user request. Never call
`workspaceRoot.getProject()` without a name and never leave `MyProject` in executable JShell.
Check `project.exists()`, `projectManager.getProject(project) != null`, and
`modelManager.getModel(project) != null` before entering the BM task.

### Canonical imports

Copy these imports into the JShell session when creating registers with
dimensions, resources, attributes, or concrete metadata reference types.
`manual_ids` do not import these classes automatically.

```java
import java.util.UUID;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterPeriodicity;
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

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

Never call `MdProducedTypesUtil.getProducedType(...)` with a null dependency.
If `transaction.getTopObjectByFqn(...)` returns null, stop with
`IllegalStateException` and create the missing dependency first. Passing null
causes a runtime `NullPointerException` inside `MdProducedTypesUtil`.

### Periodicity (for "периодический регистр сведений")

The setter is **`setInformationRegisterPeriodicity(...)`** (NOT `setPeriodicity`). The enum is
`com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterPeriodicity` with **exactly** these constants —
do not invent others (e.g. `WITHIN_SESSION` does not exist):

`NONPERIODICAL` (default — non-periodic), `SECOND`, `DAY`, `MONTH`, `QUARTER`, `YEAR`,
`RECORDER_POSITION` (subordinate to a recorder).

```java
register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY); // "периодический" → pick the period the user named
```

For a non-periodic register omit the call (default is `NONPERIODICAL`).

### Recommended bindings
- `workspaceRoot`, `projectManager`, `modelManager`, `mdFactory`, `fqnGenerator`

### Preflight-blocked patterns

Do not send JShell code with these EDT patterns:

```java
// WRONG: do not call setStringQualifiers with length 150, 1000, or any value above 100.
// WRONG: TypeDescriptionBuilder has no setStringLength(...); use setStringQualifiers(length, fixed).

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
IProject project = workspaceRoot.getProject("<ProjectName>");
if (project == null || !project.exists()) {
    throw new IllegalStateException("Project not found: <ProjectName>");
}
IV8Project v8project = projectManager.getProject(project);
if (v8project == null) {
    throw new IllegalStateException("V8 project is not available: " + project.getName());
}
IBmModel bmModel = modelManager.getModel(project);
if (bmModel == null) {
    throw new IllegalStateException("BM model is not available: " + project.getName());
}
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create information register") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        InformationRegister register = mdFactory.createInformationRegister();
        register.setName("Prices");
        register.getSynonym().put("ru", "Prices");
        register.setUuid(UUID.randomUUID());
        register.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY); // omit for non-periodic

        Catalog productsDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (productsDep == null) {
            throw new IllegalStateException("Missing dependency: Catalog.Products — create it first");
        }
        TypeItem productsRef = MdProducedTypesUtil.getProducedType(
            productsDep, MdTypePackage.Literals.MD_REF_TYPE);

        InformationRegisterDimension product = mdFactory.createInformationRegisterDimension();
        product.setName("Product");
        product.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeDescription productType = new TypeDescriptionBuilder()
            .addType(productsRef)
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

        InformationRegisterAttribute comment = mdFactory.createInformationRegisterAttribute();
        comment.setName("Comment");
        comment.setUuid(UUID.randomUUID());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription commentType = new TypeDescriptionBuilder()
            .addType(stringType)
            .setStringQualifiers(100, false)
            .build();

        comment.setType(commentType);
        register.getAttributes().add(comment);

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getInformationRegisters().add(register);
        return null;
    }
});
```

### Notes
- InformationRegister usually needs at least one dimension and often one resource
- If the user requested register attributes ("requisites"), create them with
  `mdFactory.createInformationRegisterAttribute()` and add them to
  `register.getAttributes()`. Do not stop after dimensions and resources just
  because `GetMarkers` is clean; markers validate the metadata, not the full
  business requirement.
- Every new feature derived from BasicFeature must have `setType(...)`
- Never reuse one `TypeDescription` instance across dimensions, resources, or attributes. Reuse `TypeItem` proxies only.
- For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first. For `Number(10,2)`, call `.setNumberQualifiers(2, 10, false)`.
- Use a specific reference type like `CatalogRef.Products` when you need a strict typed dimension
- For `DocumentRef.X`, `EnumRef.X`, and `CatalogRef.X` dimensions, fetch the referenced top object and verify it is non-null before calling `MdProducedTypesUtil.getProducedType(...)`. A missing dependency is a blocking precondition, not something to fix with a generic `String` or root reference type.
- If a dimension, resource, or attribute uses `IEObjectTypeNames.STRING`, build it with finite qualifiers, for example `.setStringQualifiers(100, false)`. Do not use values greater than 100, such as `150` or `1000`, unless the user explicitly requires it and the current EDT model accepts it. Otherwise `GetMarkers` can report SU8: "Строка не может быть неограниченной длины" or "Переменная длина строки должна быть внутри диапазона от 0 до 100".

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Fix only markers relevant to the changed entity before reporting success.

Also read the register back in JShell and verify the requested child
collections by name: `getDimensions()`, `getResources()`, and
`getAttributes()`. A `total_markers: 0` result does not prove that every
dimension/resource/attribute from the user's task was created.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
For an `InformationRegister.<Name>` the path is always:

```
<projectRoot>/src/InformationRegisters/<Name>/<Name>.mdo
```

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn("InformationRegister.<Name>")` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
