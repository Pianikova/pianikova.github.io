## Safe Workflow: Create an object-owned Template (макет)

Creates a `Template` that belongs to a metadata object (`Catalog`, `Document`, `Report`, …) with a
chosen `TemplateType`, **and an empty body of that type attached as a resource** — so the result is a
complete, openable макет (with a `Template.dcs`/`Template.mxl` file), not a registration without a
body. To add real content (data sets, cells) afterwards, use `fill_template_content`.

> For a stand-alone shared template use `create_common_template` (creates a top-level
> `CommonTemplate`). This scenario is for templates **owned by an object**, added to
> `owner.getTemplates()`.

### Hard rules — never violate

- ⛔ **NEVER re-create or re-attach the owner object.** The owner (Report/Catalog/Document/…)
  already exists — resolve it with `transaction.getTopObjectByFqn("Report.<Name>")` and only add
  the template to it. Do **not** call `mdFactory.createReport()`/`createCatalog()` or
  `transaction.attachTopObject(owner, …)` for the owner again: re-attaching an existing FQN throws
  `BmFqnAlreadyInUseException` ("FQN '…' is already in use"). If you just created the owner in a
  previous step, add the template in a **separate** BM task that fetches it by FQN — never paste
  the owner-creation code into the template step. Only the new `Template` child is created here.
- ❌ **Never hand-write the template with file tools** (`Write`/`Edit` on `Template.mdo`,
  `*.dcs`, `*.mxl`, settings `*.xml`). Use the model API inside a BM transaction.
- ⛔ **Create AND attach an empty body in the SAME task** (for `SPREADSHEET_DOCUMENT` →
  `moxelFactory.createSpreadsheetDocument()`, for `DATA_COMPOSITION_SCHEMA` →
  `dcsFactory.createDataCompositionSchema()`): `template.setTemplate(content)` then
  `transaction.attachTopObject((IBmObject)content, fqnGenerator.generateExternalPropertyFqn(template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE))`.
  Creating only the `Template` metadata leaves the макет **without a body file on disk** — that is
  incomplete. (For non-EMF types like `TEXT_DOCUMENT`/`HTML_DOCUMENT`/`BINARY_DATA`, create just the
  metadata and write the body with the file tools, or leave it empty.)
- ✅ **Check existence first** via `owner.getTemplates()`; do not recreate an existing template.
- ✅ **Set a UUID** (`template.setUuid(UUID.randomUUID())`) — missing UUID causes SU45.
- ✅ **Set a `TemplateType`** that matches the intended content. Most common:
  `TemplateType.SPREADSHEET_DOCUMENT` (табличный документ) and
  `TemplateType.DATA_COMPOSITION_SCHEMA` (СКД). Others: `TEXT_DOCUMENT`, `HTML_DOCUMENT`,
  `BINARY_DATA`, `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`,
  `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, `ACTIVE_DOCUMENT`, `ADD_IN`.
- ✅ **Add to the owner collection** (`owner.getTemplates().add(template)`).

### `TemplateType` constants (enum `com._1c.g5.v8.dt.metadata.mdclass.TemplateType`)

`SPREADSHEET_DOCUMENT`, `BINARY_DATA`, `ACTIVE_DOCUMENT`, `HTML_DOCUMENT`, `TEXT_DOCUMENT`,
`GEOGRAPHICAL_SCHEMA`, `DATA_COMPOSITION_SCHEMA`, `DATA_COMPOSITION_APPEARANCE_TEMPLATE`,
`GRAPHICAL_SCHEMA`, `ADD_IN`.

### Worked example — spreadsheet template on Catalog.Products

```java
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.moxel.MoxelFactory;
import com._1c.g5.v8.dt.moxel.SpreadsheetDocument;

IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

String created = globalContext.execute(new AbstractBmTask<String>("Create object template") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Catalog owner = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
        if (owner == null) {
            throw new IllegalStateException("Missing owner: Catalog.Products — create it first");
        }
        for (Template t : owner.getTemplates()) {
            if ("ПечатнаяФорма".equals(t.getName())) {
                throw new IllegalStateException("Template already exists: ПечатнаяФорма");
            }
        }

        // 1) template metadata
        Template template = mdFactory.createTemplate();
        template.setName("ПечатнаяФорма");
        template.getSynonym().put("ru", "Печатная форма");
        template.setTemplateType(TemplateType.SPREADSHEET_DOCUMENT);
        template.setUuid(UUID.randomUUID());
        owner.getTemplates().add(template);

        // 2) empty body + attach as a top object -> writes Template.mxl on commit
        SpreadsheetDocument body = MoxelFactory.eINSTANCE.createSpreadsheetDocument();
        template.setTemplate(body);
        String contentFqn = fqnGenerator.generateExternalPropertyFqn(
            template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
        transaction.attachTopObject((IBmObject)body, contentFqn);

        return template.getName();
    }
});
System.out.println("Created template: " + created);
return created;
```

For a `DATA_COMPOSITION_SCHEMA` template, use `dcsFactory.createDataCompositionSchema()` as the body
(writes `Template.dcs`). To add real content (data sets, cells) afterwards, run `fill_template_content`.

### Required post-check

A `Template` is a child object — call `GetMarkers` with `marker_type: "1c"` on the **owner's**
`.mdo` (`src/Catalogs/<OwnerName>/<OwnerName>.mdo`) and confirm the body file exists at
`src/Catalogs/<OwnerName>/Templates/<TemplateName>/Template.<ext>` (`.mxlx`/`.mxl` for
SpreadsheetDocument, `.dcs` for DataCompositionSchema). A missing body file means the content was not attached (step 2) —
that is incomplete, not success. Fix only markers relevant to the new template before reporting.
