## Safe Workflow: Create AccumulationRegister

Call `JShell` with `scope: "edt"` for this workflow.

If the prompt asks for an accumulation register and a form, this is a chained workflow. First create
and validate the `AccumulationRegister` `.mdo`; then immediately call `JShellManual` for
`create_object_form` and create or repair the generated form structure through EDT `formGenerator`.
After `Form.form` exists, read it, apply the mandatory safe `Edit` improvement pass, and run
`GetMarkers`. Do not report success after only the register `.mdo` exists or after a raw untouched
default form is generated. Never use `Write` for `.form` or owner `.mdo` files.
For the generated register list form, the minimum required improvement is a form-level title edit:
replace `<autoTitle>true</autoTitle>` with `<autoTitle>false</autoTitle>` and insert a Russian
`<title>` before `<autoUrl>true</autoUrl>`. Do this with `Edit` on `Form.form`; do not edit
`ListSettingsComposerUserSettings` or other service controls to satisfy the requirement.

Use the exact project name from `GetProjects` / the user request. Never call
`workspaceRoot.getProject()` without a name and never leave `MyProject` in executable JShell.
Check `project.exists()`, `projectManager.getProject(project) != null`, and
`modelManager.getModel(project) != null` before entering the BM task.

Copy these exact imports into the JShell snippet (do not guess packages; `manual_ids` do not import
automatically). Wrong guesses like `dt.mcore.IEObjectTypeNames`, `dt.core.project.IV8Project`, or
`dt.bm.model.*` cause "cannot find symbol" / "package does not exist".

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
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterType;
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

### ⚠️ Resolving concrete `CatalogRef.X` / `DocumentRef.X` dimensions

If a dimension or resource must reference a specific catalog/document (e.g.
`CatalogRef.Warehouses` instead of the generic `IEObjectTypeNames.CATALOG_REF`
shown below), do **NOT** use `typeProvider.getProxy("CatalogRef.Warehouses")`
— it returns `null` in JShell. Use
`MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`
where `depMdObject = (Catalog)transaction.getTopObjectByFqn("Catalog.Warehouses")`.
If the dep is `null`, throw `IllegalStateException` — create it first.
Primitive types (`NUMBER`, etc.) still use `typeProvider.getProxy(...)`.
Do not call `TypeDescriptionBuilder.setStringLength(...)`; that method does not exist. Use
`setStringQualifiers(length, fixed)` for string dimensions/attributes.
Do not import or instantiate `AllowedLength` or `StringQualifiers` for this builder. In this EDT
build `TypeDescriptionBuilder.setStringQualifiers` takes exactly `(int length, boolean fixed)`,
for example `.setStringQualifiers(100, false)`.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
// inside the BM transaction, when concrete reference is needed:
Catalog whDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Warehouses");
if (whDep == null) throw new IllegalStateException("Missing Catalog.Warehouses — create it first");
TypeItem warehouseRef = MdProducedTypesUtil.getProducedType(whDep, MdTypePackage.Literals.MD_REF_TYPE);
```

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

AccumulationRegister register = globalContext.execute(new AbstractBmTask<AccumulationRegister>("Create register") {
    @Override
    public AccumulationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        AccumulationRegister register = mdFactory.createAccumulationRegister();
        register.setName("GoodsInStock");
        register.getSynonym().put("ru", "Goods In Stock");
        register.setRegisterType(AccumulationRegisterType.BALANCE);

        // Add dimension
        AccumulationRegisterDimension warehouse = mdFactory.createAccumulationRegisterDimension();
        warehouse.setName("Warehouse");
        warehouse.getSynonym().put("ru", "Warehouse");

        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        Catalog warehousesCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Warehouses");
        if (warehousesCatalog == null) {
            throw new IllegalStateException("Missing dependency: Catalog.Warehouses");
        }
        TypeItem warehouseRefType = MdProducedTypesUtil.getProducedType(
            warehousesCatalog, MdTypePackage.Literals.MD_REF_TYPE);
        TypeDescription warehouseType = new TypeDescriptionBuilder()
            .addType(warehouseRefType)
            .build();

        warehouse.setType(warehouseType);
        register.getDimensions().add(warehouse);

        // Add resource
        AccumulationRegisterResource quantity = mdFactory.createAccumulationRegisterResource();
        quantity.setName("Quantity");
        quantity.getSynonym().put("ru", "Quantity");

        // Set numeric type for resource
        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription quantityType = new TypeDescriptionBuilder()
            .addType(numberType)
            .setNumberQualifiers(0, 10, false)
            .build();

        quantity.setType(quantityType);
        register.getResources().add(quantity);

        // Set UUIDs manually (RECOMMENDED for JShell)
        register.setUuid(UUID.randomUUID());
        warehouse.setUuid(UUID.randomUUID());
        quantity.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getAccumulationRegisters().add(register);
        return register;
    }
});
```
**Note:** Registers require at least one Dimension. Resources are optional but recommended.
**Note:** In Java code use generated enum constants `AccumulationRegisterType.BALANCE` and `AccumulationRegisterType.TURNOVERS`. The `.mdo` XML may serialize them as `Balance` / `Turnovers`, but those are not the Java constants to write in JShell.
**Note:** `AccumulationRegisterDimension` does not have `setBalance(...)`; do not call it in JShell examples.
**Note:** Do not use `AccumulationRegisterAttribute` in JShell. In the tested
EDT API this class is not available. For accumulation registers use
`AccumulationRegisterDimension` and `AccumulationRegisterResource`; if a
scenario asks for another register child kind, verify the exact class with one
batch `JShellReflection` before coding.
**Note:** Each dimension/resource/attribute needs its own fresh `TypeDescription`; do not reuse one instance.
**Note:** For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first. For `Number(10,2)`, call `.setNumberQualifiers(2, 10, false)`.

### Registrar modes

Use one of these modes deliberately:
- **Mode A: register only for later linking.** Creating only the register is acceptable as an intermediate step, but `GetMarkers` may return SU45 until a document registrar is linked.
- **Mode B: register with registrar document.** Preferred when the user asks for a complete valid register workflow.

Accumulation, accounting, and calculation registers MUST have at least one document that records to them. Registrars are configured on documents, not on registers.

```java
Document goodsReceipt = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
AccumulationRegister stockRegister = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
if (goodsReceipt != null && stockRegister != null && !goodsReceipt.getRegisterRecords().contains(stockRegister)) {
    goodsReceipt.getRegisterRecords().add(stockRegister);
}
```

If the registrar document does not exist, create it in the same BM transaction or call `create_document` first, then call `add_document_registers`.

### Expected validation marker for Mode A

If you create the register without linking a registrar document, `GetMarkers` can return SU45: "Некорректный состав регистраторов регистра. Ни один из документов не является регистратором для регистра".

Do not report success while this marker remains unless the user explicitly asked to create an invalid intermediate register for later linking.
If the business request says the document should change stock, make movements,
or write records to this register, this marker is blocking. Do not answer that
metadata must be changed manually. Call `JShellManual` for
`add_document_registers` and execute that workflow.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed register `.mdo`. For Mode B, fix markers relevant to the changed register and its registrar links before reporting success. For Mode A, explicitly report that registrar linking is still required if SU45 remains.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
For an `AccumulationRegister.<Name>` the path is always:

```
<projectRoot>/src/AccumulationRegisters/<Name>/<Name>.mdo
```

If you also linked or created a registrar document, its `.mdo` lives at `src/Documents/<DocName>/<DocName>.mdo`. Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn(...)` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
