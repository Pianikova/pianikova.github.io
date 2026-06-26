## Safe Workflow: Fill Template content (СКД / табличный документ)

Attaches a content object to an existing `Template`/`CommonTemplate` whose `TemplateType` matches.
Run after `create_object_template` (or `create_common_template`).

### Hard rules — never violate

- ⛔ **NEVER re-create or re-attach the owner or the template.** Both already exist — resolve the
  owner with `transaction.getTopObjectByFqn(...)` and find the `Template` in `owner.getTemplates()`.
  Do not call `attachTopObject(owner, …)` (throws `BmFqnAlreadyInUseException`) and do not recreate
  the template. This step builds the content object, calls `template.setTemplate(content)`, **and
  attaches the content as a top object** (see next rule).
- ⛔ **The content MUST be attached as a top object, or it is NOT written to disk.** `setTemplate`
  alone only updates the in-memory model — the `.dcs`/`.mxl` file is produced only when the content
  is attached via its external-property FQN:
  `transaction.attachTopObject((IBmObject)content, fqnGenerator.generateExternalPropertyFqn(template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE))`.
  This mirrors how a form's structure is persisted (`BASIC_FORM__FORM`). Omitting it is the most
  common reason the template body never appears on disk.
- ❌ **Never create template files with `Write`** (`*.dcs`, `*.mxl`, `Template.mdo`, settings
  `*.xml`). The body must first be generated/registered through the EMF factory +
  `setTemplate(...)` inside a BM transaction (below). After the generated body file exists, `Edit`
  may be used for small targeted changes, followed by `GetMarkers`.
- ✅ **Use `Edit` only as refinement.** Once EDT has produced the body file, file `Edit` may adjust
  a small known text fragment in an existing `.dcs`/`.mxl(x)`/`Template.mdo` resource. Always read
  the file first, keep the replacement exact and minimal, and validate the owner `.mdo` with
  `GetMarkers`. If the file does not exist, return to this EDT API workflow instead of `Write`.
- ⛔ **Exact factory packages — do not guess.** The DCS factory is
  `com._1c.g5.v8.dt.dcs.model.schema.DcsFactory` (NOT `com._1c.g5.v8.dt.dcs.util.*`). The
  spreadsheet factory is `com._1c.g5.v8.dt.moxel.MoxelFactory`. Both are exposed as the
  pre-bound `dcsFactory` / `moxelFactory` session variables — use those rather than importing.
- ✅ If the template does not exist yet, run `create_object_template` first; do not create the
  template metadata here.
- ✅ **Use the real request project name.** Never leave `MyProject` in executable JShell. Check
  `project.exists()` and `modelManager.getModel(project) != null` before `getGlobalContext()`.
  Missing project, missing BM model, missing owner, and missing template must be explicit
  `IllegalStateException`s, not `NullPointerException`s.

### How template content is stored (required two steps)

`BasicTemplate.getTemplate()/setTemplate(EObject)` is a transient `@ExternalProperty` reference.
To actually persist the body to disk you must do **both**:

```java
template.setTemplate(content);                                  // 1) link content to the template
String contentFqn = fqnGenerator.generateExternalPropertyFqn(   // 2) attach content as a top object
    template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
transaction.attachTopObject((IBmObject)content, contentFqn);    //    -> EDT writes the .dcs/.mxl file
```

The global editing context auto-saves on commit, but **only attached top objects (and the modified
owner) are written**. `setTemplate(...)` without `attachTopObject(...)` leaves the content
in-memory only — no `.dcs`/`.mxl` file. Always verify with `GetMarkers` and by checking the
produced file on disk.

### Content factory by TemplateType

| TemplateType | factory binding | create method | content type |
|--------------|-----------------|---------------|--------------|
| `DATA_COMPOSITION_SCHEMA` | `dcsFactory` | `DcsFactory.eINSTANCE.createDataCompositionSchema()` | `DataCompositionSchema` |
| `SPREADSHEET_DOCUMENT` | `SheetFactory` | `SheetFactory.createSpreadsheetDocument()` | `SpreadsheetDocument` |

> ⛔ For `SPREADSHEET_DOCUMENT` do **not** use the bare `MoxelFactory.eINSTANCE.createSpreadsheetDocument()`.
> It omits the mandatory `printSettings`/`viewSettings`/`formats`/`columns`/`defaultFormatIndex`, so the
> `.mxlx` never persists/loads and the editor fails with
> `Unsupported embedded object type ... EObjectImpl`. Use `SheetFactory.createSpreadsheetDocument()`
> (package `com._1c.g5.v8.dt.moxel.sheet`, pre-imported in the `edt` scope), which initialises them.

Other types (`TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`, …) are stored as plain
text/binary resources — for those, create the `Template` metadata with the right `TemplateType`
and write the body file with the file tools, rather than via these EMF factories.

### Example — DataCompositionSchema content

```java
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.dcs.model.schema.DcsFactory;
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchema;

IProject project = workspaceRoot.getProject("<ProjectName>");
if (project == null || !project.exists()) {
    throw new IllegalStateException("Project not found: <ProjectName>");
}
IBmModel bmModel = modelManager.getModel(project);
if (bmModel == null) {
    throw new IllegalStateException("BM model is not available: " + project.getName());
}
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String result = globalContext.execute(new AbstractBmTask<String>("Fill template content") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Report owner = (Report)transaction.getTopObjectByFqn("Report.SalesAnalysis");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Report.SalesAnalysis");
        }
        Template template = null;
        for (Template t : owner.getTemplates()) {
            if ("ОсновнаяСхемаКомпоновкиДанных".equals(t.getName())) { template = t; break; }
        }
        if (template == null) {
            throw new IllegalStateException("Template not found; run create_object_template first");
        }
        if (template.getTemplateType() != TemplateType.DATA_COMPOSITION_SCHEMA) {
            throw new IllegalStateException("Template type must be DATA_COMPOSITION_SCHEMA");
        }

        DataCompositionSchema schema = DcsFactory.eINSTANCE.createDataCompositionSchema();
        // add data sources / data sets here as required by the report
        template.setTemplate(schema);
        String contentFqn = fqnGenerator.generateExternalPropertyFqn(
            template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
        transaction.attachTopObject((IBmObject)schema, contentFqn);   // persists Template.dcs

        return template.getName();
    }
});
System.out.println("Filled content for: " + result);
return result;
```

### Example — SpreadsheetDocument content (blank table)

```java
import com._1c.g5.v8.dt.moxel.SpreadsheetDocument;
import com._1c.g5.v8.dt.moxel.sheet.SheetFactory;

// inside the BM task, template.getTemplateType() == TemplateType.SPREADSHEET_DOCUMENT
SpreadsheetDocument document = SheetFactory.createSpreadsheetDocument();   // fully initialised blank table
template.setTemplate(document);   // an empty spreadsheet is a valid blank template
String contentFqn = fqnGenerator.generateExternalPropertyFqn(
    template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
transaction.attachTopObject((IBmObject)document, contentFqn);   // persists Template.mxl
```

### Required post-check

Call `GetMarkers` with `marker_type: "1c"` on the owner's `.mdo` and confirm the content file
was written, e.g.:

```
src/Reports/SalesAnalysis/Templates/ОсновнаяСхемаКомпоновкиДанных/Template.dcs   (DCS)
src/Catalogs/Products/Templates/ПечатнаяФорма/Template.mxlx                      (spreadsheet)
```

If the file is missing or markers appear, the template metadata still exists — report that and
rerun this scenario only after fixing the concrete JShell/API error that prevented
`attachTopObject(...)` from committing. Do not tell the user to fill the body manually for
`DATA_COMPOSITION_SCHEMA` or `SPREADSHEET_DOCUMENT`; those content types are covered here.
