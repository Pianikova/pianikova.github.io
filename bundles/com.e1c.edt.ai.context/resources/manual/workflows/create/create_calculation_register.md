## Safe Workflow: Create CalculationRegister

If the prompt asks for a calculation register and a form, this is a chained workflow. First create
and validate the `CalculationRegister` `.mdo`; then immediately call `JShellManual` for
`create_object_form` and create or repair the generated form structure through EDT `formGenerator`.
After `Form.form` exists, read it, apply the mandatory safe `Edit` improvement pass, and run
`GetMarkers`. Do not report success after only the register `.mdo` exists or after a raw untouched
default form is generated. Never use `Write` for `.form` or owner `.mdo` files.
For the generated register list form, the minimum required improvement is a form-level title edit:
replace `<autoTitle>true</autoTitle>` with `<autoTitle>false</autoTitle>` and insert a Russian
`<title>` before `<autoUrl>true</autoUrl>`. Do this with `Edit` on `Form.form`; do not edit
`ListSettingsComposerUserSettings` or other service controls to satisfy the requirement.

For string dimensions/attributes, use `TypeDescriptionBuilder.setStringQualifiers(int length,
boolean fixed)`, for example `.setStringQualifiers(100, false)`. Do not import or instantiate
`AllowedLength` or `StringQualifiers`; do not call `setStringLength(...)`.

Use the exact project name from `GetProjects` / the user request. Never call
`workspaceRoot.getProject()` without a name and never leave `MyProject` in executable JShell.
Check `project.exists()`, `projectManager.getProject(project) != null`, and
`modelManager.getModel(project) != null` before entering the BM task.

Copy these exact imports into the JShell snippet (do not guess packages; `manual_ids` do not import
automatically). Wrong guesses like `dt.mcore.IEObjectTypeNames` or `dt.bm.model.*` cause
"cannot find symbol" / "package does not exist".

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
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypes;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

### ⚠️ Resolving concrete `CatalogRef.X` / `ChartOfCalculationTypesRef.X` dimensions

For concrete metadata reference dimensions (e.g. `CatalogRef.Employees`
instead of the generic `IEObjectTypeNames.CATALOG_REF` shown below), do
**NOT** use `typeProvider.getProxy("CatalogRef.Employees")` — it returns
`null` in JShell. Use
`MdProducedTypesUtil.getProducedType(depMdObject, MdTypePackage.Literals.MD_REF_TYPE)`
where `depMdObject = transaction.getTopObjectByFqn("Catalog.Employees")` (or
`"ChartOfCalculationTypes.X"`, etc.). If the dep is `null`, throw
`IllegalStateException` — create it first. Primitive types (`NUMBER`, etc.)
still use `typeProvider.getProxy(...)`.

```java
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
// inside the BM transaction, when concrete reference is needed:
Catalog empDep = (Catalog)transaction.getTopObjectByFqn("Catalog.Employees");
if (empDep == null) throw new IllegalStateException("Missing Catalog.Employees — create it first");
TypeItem employeeRefConcrete = MdProducedTypesUtil.getProducedType(empDep, MdTypePackage.Literals.MD_REF_TYPE);
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

CalculationRegister register = globalContext.execute(new AbstractBmTask<CalculationRegister>("Create register") {
    @Override
    public CalculationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        CalculationRegister register = mdFactory.createCalculationRegister();
        register.setName("SalaryCalculation");
        register.getSynonym().put("ru", "Salary Calculation");
        register.setPeriodicity(CalculationRegisterPeriodicity.MONTH);
        register.setActionPeriod(true);
        register.setBasePeriod(false);

        // Set ChartOfCalculationTypes reference
        ChartOfCalculationTypes chart = (ChartOfCalculationTypes)transaction.getTopObjectByFqn("ChartOfCalculationTypes.ВидыРасчетов");
        if (chart == null) {
            throw new IllegalStateException("Missing dependency: ChartOfCalculationTypes");
        }
        register.setChartOfCalculationTypes(chart);

        // Add dimension (base dimension)
        CalculationRegisterDimension employee = mdFactory.createCalculationRegisterDimension();
        employee.setName("Employee");
        employee.getSynonym().put("ru", "Employee");
        employee.setBaseDimension(true);

        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        Catalog employeesCatalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Employees");
        if (employeesCatalog == null) {
            throw new IllegalStateException("Missing dependency: Catalog.Employees");
        }
        TypeItem employeeRefType = MdProducedTypesUtil.getProducedType(
            employeesCatalog, MdTypePackage.Literals.MD_REF_TYPE);
        TypeDescription employeeType = new TypeDescriptionBuilder()
            .addType(employeeRefType)
            .build();

        employee.setType(employeeType);
        register.getDimensions().add(employee);

        // Add resource
        CalculationRegisterResource amount = mdFactory.createCalculationRegisterResource();
        amount.setName("Amount");
        amount.getSynonym().put("ru", "Amount");

        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        TypeDescription amountType = new TypeDescriptionBuilder()
            .addType(numberType)
            .setNumberQualifiers(2, 10, false)
            .build();

        amount.setType(amountType);
        register.getResources().add(amount);

        // Add recalculation rule
        Recalculation recalculation = mdFactory.createRecalculation();
        recalculation.setName("Recalculation");
        register.getRecalculations().add(recalculation);

        // Set UUIDs manually (RECOMMENDED for JShell)
        register.setUuid(UUID.randomUUID());
        employee.setUuid(UUID.randomUUID());
        amount.setUuid(UUID.randomUUID());
        recalculation.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getCalculationRegisters().add(register);
        return register;
    }
});
```
**Note:** CalculationRegister requires ChartOfCalculationTypes reference and at least one base Dimension.
**Note:** Use a numeric type for calculation resources such as amount; do not reuse a reference type from a dimension.
**Note:** Each dimension/resource/attribute needs its own fresh `TypeDescription`; do not reuse one instance.
**Note:** For numbers, `setNumberQualifiers(scale, precision, nonNegative)` uses scale first. For `Number(10,2)`, call `.setNumberQualifiers(2, 10, false)`.

### Registrar modes

Use one of these modes deliberately:
- **Mode A: register only for later linking.** Creating only the register is acceptable as an intermediate step, but `GetMarkers` may return SU45 until a document registrar is linked.
- **Mode B: register with registrar document.** Preferred when the user asks for a complete valid register workflow.

Calculation registers MUST have at least one document that records to them. Registrars are configured on documents, not on registers.

```java
Document payrollDocument = (Document)transaction.getTopObjectByFqn("Document.Payroll");
CalculationRegister salaryCalculation = (CalculationRegister)transaction.getTopObjectByFqn("CalculationRegister.SalaryCalculation");
if (payrollDocument != null && salaryCalculation != null && !payrollDocument.getRegisterRecords().contains(salaryCalculation)) {
    payrollDocument.getRegisterRecords().add(salaryCalculation);
}
```

If the registrar document does not exist, create it in the same BM transaction or call `create_document` first, then call `add_document_registers`.

### Expected validation marker for Mode A

If you create the register without linking a registrar document, `GetMarkers` can return SU45: "Некорректный состав регистраторов регистра. Ни один из документов не является регистратором для регистра".

Do not report success while this marker remains unless the user explicitly asked to create an invalid intermediate register for later linking.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed register `.mdo`. For Mode B, fix markers relevant to the changed register and its registrar links before reporting success. For Mode A, explicitly report that registrar linking is still required if SU45 remains.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
For this scenario:

| FQN prefix                            | `.mdo` path                                              |
|---------------------------------------|----------------------------------------------------------|
| `CalculationRegister.<Name>`          | `src/CalculationRegisters/<Name>/<Name>.mdo`             |
| `ChartOfCalculationTypes.<Name>`      | `src/ChartsOfCalculationTypes/<Name>/<Name>.mdo`         |
| `Document.<Name>`                     | `src/Documents/<Name>/<Name>.mdo` (registrar doc)        |

Copy `<Name>` exactly from the FQN you used in `getTopObjectByFqn(...)` — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
