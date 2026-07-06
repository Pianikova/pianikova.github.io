## Safe Workflow: Create an object-owned Template (макет)

Creates a `Template` that belongs to a metadata object (`Catalog`, `Document`, `Report`, …) with a
chosen `TemplateType`, **and an empty body of that type attached as a resource** — so the result is a
complete, openable макет (with a `Template.dcs`/`Template.mxlx` file), not a registration without a
body. The `Template` child metadata is stored inside the owner `.mdo`; there is no separate
`Templates/<TemplateName>/<TemplateName>.mdo` for object-owned templates. To add real content
(data sets, cells) afterwards, use `fill_template_content`.
After the default body exists, targeted changes may be made with `Edit` on the existing generated
template/body files. Do not use `Write` to create owner `.mdo`, `.dcs`, `.mxl`, or `.mxlx` files.
First consult `template_type_matrix` when the prompt names a specific kind of макет. The safe body
creation rules differ by type.

### First decision by body kind

- If the requested template is `SPREADSHEET_DOCUMENT` or `DATA_COMPOSITION_SCHEMA`, create the child
  metadata and blank body through the EDT model API in one BM transaction.
- If the requested template is `TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`,
  `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`, `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, or `ADD_IN`
  and the prompt does not provide real body content/source file, do not call JShell and do not
  create metadata. Ask the user for the source file/content.
- Russian prompt terms that trigger this source-backed stop rule: `двоичные данные`,
  `текстовый документ`, `HTML документ`, `НТМЛ документ`, `географическая схема`,
  `графическая схема`, `макет оформления компоновки данных`, `внешняя компонента`.
- A metadata-only source-backed template with `GetMarkers=0` is still incomplete and must not be
  reported as successful.
- Never create a metadata-only source-backed template unless the user explicitly says that a stub
  without `Template.<ext>` is acceptable.
- Never use `Write` to create an empty or fake `Template.txt`, `Template.htmldoc`, `Template.bin`,
  `Template.geos`, `Template.scheme`, `Template.dcsat`, or `Template.addin`.

**Creation vs refinement policy:** create and register templates through EDT API in a BM
transaction. Let EDT create the owner child, default body, external-property FQN, and disk files.
Only after the owner `.mdo` contains `<templates ...>` and the body file already exists may the agent use file `Edit` for narrow
text-level fixes. `Write` is not a template creation mechanism.

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
- ❌ **Never create the template with file tools** (`Write` on owner `.mdo`, `*.dcs`, `*.mxl`,
  settings `*.xml`). Use the model API inside a BM transaction. `Edit` is allowed only after the
  template/body file already exists and was generated/registered by EDT.
- ⛔ **No raw XML bootstrap.** If `Template.dcs`/`Template.mxl(x)` is missing, the workflow is
  incomplete. Re-run or repair the EDT API attach step; do not invent a replacement file with
  `Write`.
- ✅ **Small post-generation edits are allowed.** For existing generated template files, first
  `Read`, then use `Edit` with an exact, minimal replacement, and finish with `GetMarkers` on the
  owner `.mdo`. Keep structural template operations in EDT API.
- ⛔ **Create AND attach an empty body in the SAME task** (for `SPREADSHEET_DOCUMENT` →
  `SheetFactory.createSpreadsheetDocument()` — **not** the bare `moxelFactory.createSpreadsheetDocument()`,
  which omits the mandatory printSettings/viewSettings/formats/columns and leaves the `.mxlx` unloadable
  (`Unsupported embedded object type ... EObjectImpl`); for `DATA_COMPOSITION_SCHEMA` →
  `dcsFactory.createDataCompositionSchema()`): `template.setTemplate(content)` then
  `transaction.attachTopObject((IBmObject)content, fqnGenerator.generateExternalPropertyFqn(template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE))`.
  Creating only the `Template` metadata leaves these макеты **without a body file on disk** — that is
  incomplete.
- ✅ **For source-backed template types** (`TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`,
  `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`, `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, `ADD_IN`), set
  the matching `TemplateType` only when a real body source is available or the user explicitly
  accepts a metadata-only stub. Without that source/acceptance, stop before JShell and ask. Do not
  invent placeholder bodies. Expected body names from ERP2:
  `Template.txt`, `Template.htmldoc`, `Template.bin`, `Template.geos`, `Template.scheme`,
  `Template.dcsat`, `Template.addin`.
- Do not adapt the spreadsheet/DCS executable example by merely changing
  `template.setTemplateType(...)` to a source-backed type. That creates an incomplete
  metadata-only template.
- ✅ **Check existence first** via `owner.getTemplates()`; do not recreate an existing template.
- ✅ **Set a UUID** (`template.setUuid(UUID.randomUUID())`) — missing UUID causes SU45.
- ✅ **Set a `TemplateType`** that matches the intended content. Most common:
  `TemplateType.SPREADSHEET_DOCUMENT` (табличный документ) and
  `TemplateType.DATA_COMPOSITION_SCHEMA` (СКД). Others: `TEXT_DOCUMENT`, `HTML_DOCUMENT`,
  `BINARY_DATA`, `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`,
  `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, `ACTIVE_DOCUMENT`, `ADD_IN`.
- ✅ **Add to the owner collection** (`owner.getTemplates().add(template)`).
- ✅ **Use the real request project name.** Never leave `MyProject` in executable JShell. Before
  calling `bmModel.getGlobalContext()`, check that `workspaceRoot.getProject(...)` exists and
  `modelManager.getModel(project)` is non-null. If the owner FQN is missing, throw
  `IllegalStateException("Missing owner ... create it first")`; do not continue into NPE.
- ✅ **Verify the body through Eclipse resources before any file tool.** After commit, use the same
  `IProject` and `project.getFile("src/.../Template.mxlx").exists()` (or `Template.dcs`) to verify
  the body. If this prints `Body exists: true`, the body check is complete: do not call `Glob` or
  `Read` just to inspect/search the template folder. Do not guess the workspace path for `Read`, and
  do not call `Glob`.

### `TemplateType` constants (enum `com._1c.g5.v8.dt.metadata.mdclass.TemplateType`)

`SPREADSHEET_DOCUMENT`, `BINARY_DATA`, `ACTIVE_DOCUMENT`, `HTML_DOCUMENT`, `TEXT_DOCUMENT`,
`GEOGRAPHICAL_SCHEMA`, `DATA_COMPOSITION_SCHEMA`, `DATA_COMPOSITION_APPEARANCE_TEMPLATE`,
`GRAPHICAL_SCHEMA`, `ADD_IN`.

Use Java enum constants in JShell (`TemplateType.TEXT_DOCUMENT`), not the XML values stored in
`.mdo` (`TextDocument`). See `template_type_matrix` for the full mapping.

### Worked example — spreadsheet template on Catalog.Products

```java
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.moxel.SpreadsheetDocument;
import com._1c.g5.v8.dt.moxel.sheet.SheetFactory;

IProject project = workspaceRoot.getProject("<ProjectName>");
if (project == null || !project.exists()) {
    throw new IllegalStateException("Project not found: <ProjectName>");
}
IBmModel bmModel = modelManager.getModel(project);
if (bmModel == null) {
    throw new IllegalStateException("BM model is not available: " + project.getName());
}
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

        // 2) empty body + attach as a top object -> writes Template.mxlx on commit
        // Use SheetFactory (not the bare MoxelFactory) so the document is fully initialised and loadable.
        SpreadsheetDocument body = SheetFactory.createSpreadsheetDocument();
        template.setTemplate(body);
        String contentFqn = fqnGenerator.generateExternalPropertyFqn(
            template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
        transaction.attachTopObject((IBmObject)body, contentFqn);

        return template.getName();
    }
});
System.out.println("Created template: " + created);
System.out.println("Project path: " + project.getLocation().toOSString());
System.out.println("Body exists: " +
    project.getFile("src/Catalogs/Products/Templates/ПечатнаяФорма/Template.mxlx").exists());
return created;
```

When this JShell call prints `Body exists: true`, do not run `Glob` or `Read` to re-check the body
file. The only required external validation is `GetMarkers` on the owner `.mdo`.

For a `DATA_COMPOSITION_SCHEMA` template, use `dcsFactory.createDataCompositionSchema()` as the body
(writes `Template.dcs`). To add real content (data sets, cells) afterwards, run `fill_template_content`.
For text/html/binary/geo/graphical/DCS appearance/add-in templates, do not use this spreadsheet/DCS
body snippet; ask for or reuse a real source body.

### Required post-check

A `Template` is a child object — call `GetMarkers` with `marker_type: "1c"` on the **owner's**
`.mdo` using a project-relative path (`src/Catalogs/<OwnerName>/<OwnerName>.mdo`) and confirm the body file exists at
`src/Catalogs/<OwnerName>/Templates/<TemplateName>/Template.<ext>` (`.mxlx`/`.mxl` for
SpreadsheetDocument, `.dcs` for DataCompositionSchema). A missing body file means the content was not attached (step 2) —
that is incomplete, not success. Fix only markers relevant to the new template before reporting.

Do **not** call `GetMarkers` on
`src/Catalogs/<OwnerName>/Templates/<TemplateName>/<TemplateName>.mdo`: that file does not exist for
object-owned templates. The metadata lives in the owner `.mdo`; the template body lives in
`Template.mxlx` or `Template.dcs`.

Do **not** call `Glob` to find template files. Avoid both wildcard paths like `**/Catalogs/...` and
rootless paths like `src/Catalogs`. A successful `Glob` is still a dirty scenario because it is an
unnecessary directory search; use the deterministic paths directly from the known project root,
owner kind, owner name, and template name:

```text
<projectRoot>/src/Catalogs/<OwnerName>/<OwnerName>.mdo
<projectRoot>/src/Catalogs/<OwnerName>/Templates/<TemplateName>/Template.mxlx
```

For other owners, replace `Catalogs` with the owner folder (`Documents`, `Reports`, ...). For
`GetMarkers`, use the project-relative owner path with `project_name`; do not pass invented
absolute paths such as `C:\EDT_projects\...` or default-workspace guesses such as
`C:\Users\...\eclipse-workspace\...`. To verify body existence, use the Eclipse resource API in
JShell:

```java
project.getFile("src/Catalogs/<OwnerName>/Templates/<TemplateName>/Template.mxlx").exists()
```

If a file tool is still needed, build the absolute path only from `project.getLocation().toOSString()`
printed by JShell and the deterministic relative path. Do not use `java.io.File` in JShell: it is
restricted. Do not call `Glob` for template verification, do not use `**` as a directory, and do not
pass `src/...` to `Glob`.
