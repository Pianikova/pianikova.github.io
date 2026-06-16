## Safe Workflow: Fill Template content (СКД / табличный документ)

Attaches a content object to an existing `Template`/`CommonTemplate` whose `TemplateType` matches.
Run after `create_object_template` (or `create_common_template`).

### Hard rules — never violate

- ❌ **Never hand-write template files with the file tools** (`Write`/`Edit` on `*.dcs`, `*.mxl`,
  `Template.mdo`, settings `*.xml`). Hand-written DCS/spreadsheet XML is fragile, version-specific,
  and is not registered with the report/owner model — it produces a broken or unloadable template.
  Always go through the EMF factory + `setTemplate(...)` inside a BM transaction (below).
- ⛔ **Exact factory packages — do not guess.** The DCS factory is
  `com._1c.g5.v8.dt.dcs.model.schema.DcsFactory` (NOT `com._1c.g5.v8.dt.dcs.util.*`). The
  spreadsheet factory is `com._1c.g5.v8.dt.moxel.MoxelFactory`. Both are exposed as the
  pre-bound `dcsFactory` / `moxelFactory` session variables — use those rather than importing.
- ✅ If the template does not exist yet, run `create_object_template` first; do not create the
  template metadata here.

### How template content is stored

`BasicTemplate.getTemplate()/setTemplate(EObject)` is a transient `@ExternalProperty` reference.
You create the content model with the matching factory and set it on the template:

```java
template.setTemplate(content);
```

EDT persists `content` to a **separate resource file** on commit — there is **no** manual
"register" call (`IExternalPropertyManager` only resolves owners/references, it does not set
content). Persistence of external content from a JShell transaction is best-effort: **always**
verify with `GetMarkers` and by checking the produced file. If the file is not written, report
"template metadata created; open the template to add content" rather than reporting false success.

### Content factory by TemplateType

| TemplateType | factory binding | create method | content type |
|--------------|-----------------|---------------|--------------|
| `DATA_COMPOSITION_SCHEMA` | `dcsFactory` | `createDataCompositionSchema()` | `DataCompositionSchema` |
| `SPREADSHEET_DOCUMENT` | `moxelFactory` | `createSpreadsheetDocument()` | `SpreadsheetDocument` |

Other types (`TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`, …) are stored as plain
text/binary resources — for those, create the `Template` metadata with the right `TemplateType`
and write the body file with the file tools, rather than via these EMF factories.

### Example — DataCompositionSchema content

```java
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
import com._1c.g5.v8.dt.dcs.model.schema.DcsFactory;
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchema;

IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
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

        return template.getName();
    }
});
System.out.println("Filled content for: " + result);
return result;
```

### Example — SpreadsheetDocument content (blank table)

```java
import com._1c.g5.v8.dt.moxel.MoxelFactory;
import com._1c.g5.v8.dt.moxel.SpreadsheetDocument;

// inside the BM task, template.getTemplateType() == TemplateType.SPREADSHEET_DOCUMENT
SpreadsheetDocument document = MoxelFactory.eINSTANCE.createSpreadsheetDocument();
template.setTemplate(document);   // an empty spreadsheet is a valid blank template
```

### Required post-check

Call `GetMarkers` with `marker_type: "1c"` on the owner's `.mdo` and confirm the content file
was written, e.g.:

```
src/Reports/SalesAnalysis/Templates/ОсновнаяСхемаКомпоновкиДанных/Template.dcs   (DCS)
src/Catalogs/Products/Templates/ПечатнаяФорма/Template.mxl                       (spreadsheet)
```

If the file is missing or markers appear, the template metadata still exists — report that and
advise filling the body in the template editor. Do not loop retrying `setTemplate(...)`.
