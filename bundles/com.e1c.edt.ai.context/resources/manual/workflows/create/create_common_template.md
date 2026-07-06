## Safe Workflow: Create CommonTemplate (общий макет)

### STOP: source-backed templates need a source body first

If the user asks for a common template of type `ADD_IN`, `BINARY_DATA`, `GEOGRAPHICAL_SCHEMA`,
`GRAPHICAL_SCHEMA`, `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, `HTML_DOCUMENT`, or `TEXT_DOCUMENT`
without providing the actual source body/file, the correct action is to stop after this manual call.
Do not call JShell. Do not create `CommonTemplate` metadata. Do not create a stub. Do not reinterpret
absence of a source file as permission to create a metadata-only stub. Final answer must ask for the
source file/content, for example: "Для макета внешней компоненты нужен реальный файл компоненты
(`Template.addin`). Пришлите путь к файлу."

Russian prompt terms that MUST trigger this stop rule: `двоичные данные`, `текстовый документ`,
`HTML документ`, `НТМЛ документ`, `географическая схема`, `графическая схема`,
`макет оформления компоновки данных`, `внешняя компонента`. A metadata-only source-backed template
with `GetMarkers=0` is still incomplete and must not be created or reported as successful.

Creates a top-level `CommonTemplate` and attaches a valid body resource in the same BM
transaction. The result must be an openable common template with both:

- `src/CommonTemplates/<Name>/<Name>.mdo`
- `src/CommonTemplates/<Name>/Template.<ext>` (`.mxlx` for spreadsheet, `.dcs` for DCS)

Do not report success after only the `.mdo` exists.
First consult `template_type_matrix` when the prompt names a specific kind of common template.
ERP2 examples show these body names: `Template.mxlx`, `Template.txt`, `Template.htmldoc`,
`Template.bin`, `Template.geos`, `Template.dcs`, `Template.dcsat`, and `Template.addin`.

### First decision by body kind

- If the requested common template is `SPREADSHEET_DOCUMENT` or `DATA_COMPOSITION_SCHEMA`, create
  metadata and a blank body through the EDT model API in one BM transaction.
- If the requested common template is `TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`,
  `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`, `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, or `ADD_IN`
  and the prompt does not provide real body content/source file, stop immediately: no JShell, no
  metadata, no marker check. Ask the user for the source file/content.
- If the prompt says `двоичные данные`, this means `BINARY_DATA` and requires a real binary file
  before any metadata is created.
- Never create a metadata-only `CommonTemplate` for a source-backed body unless the prompt literally
  says `metadata-only stub`, `только метаданные`, or `заглушка без файла`.
- Never use `Write` to create an empty or fake `Template.txt`, `Template.htmldoc`, `Template.bin`,
  `Template.geos`, `Template.scheme`, `Template.dcsat`, or `Template.addin`.

### Hard rules

- Use this scenario for shared/stand-alone templates: `общий макет`, `CommonTemplate`.
  For a template owned by `Catalog`, `Document`, `Report`, etc. use `create_object_template`.
- Never create `CommonTemplate.mdo`, `Template.dcs`, `Template.mxl`, or `Template.mxlx` with `Write`.
  Use EDT API in a BM transaction. `Edit` is allowed only after EDT has generated the files and only
  for narrow text-level refinements.
- Create the metadata object and body in one BM task:
  `template.setTemplate(content)` plus
  `transaction.attachTopObject((IBmObject)content, fqnGenerator.generateExternalPropertyFqn(template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE))`.
- For `SPREADSHEET_DOCUMENT`, use `SheetFactory.createSpreadsheetDocument()`, not
  `moxelFactory.createSpreadsheetDocument()`.
- For `DATA_COMPOSITION_SCHEMA`, use `dcsFactory.createDataCompositionSchema()`.
  If you need the Java type, import exactly
  `com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchema`. Do not import
  `com._1c.g5.v8.dt.dcs.DataCompositionSchema`, and do not invent
  `DataCompositionSchemaFactory`.
- For `TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`, `GEOGRAPHICAL_SCHEMA`,
  `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, and `ADD_IN`, set the matching `TemplateType` but do not
  invent a body. These types require explicit text/html/binary/schema/add-in source content or an
  existing file to import/edit. Without that source, stop before JShell and ask. Body names are
  `Template.txt`, `Template.htmldoc`, `Template.bin`, `Template.geos`, `Template.dcsat`, and
  `Template.addin`.
- Do not adapt the spreadsheet/DCS executable examples below by merely changing
  `template.setTemplateType(...)` to `BINARY_DATA`, `HTML_DOCUMENT`, `TEXT_DOCUMENT`,
  `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`, `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, or `ADD_IN`.
  That creates an incomplete metadata-only template.
- Check for an existing `CommonTemplate.<Name>` first. If it exists, do not reattach it.
- Set `template.setUuid(UUID.randomUUID())` and `template.setTemplateType(...)`.
- After creation, call `GetMarkers` on the project-relative path
  `src/CommonTemplates/<Name>/<Name>.mdo` and verify the body file exists. A missing body file is a
  failed/incomplete template.
- Never invent an absolute root such as `C:\EDT_projects\...`. If you need the absolute project
  location, print `project.getLocation().toOSString()` from JShell.
- Verify the body through Eclipse resources before any file tool:
  `project.getFile("src/CommonTemplates/<Name>/Template.mxlx").exists()` (or `Template.dcs`). Do not
  guess a default workspace path and do not call `Glob`.
- Do not paste HTML-escaped generics into JShell. If a snippet contains `&lt;String&gt;`, convert it to
  `<String>` before running, or use the raw `AbstractBmTask` form from the example below.

### Minimal spreadsheet common template

```java
import java.util.UUID;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.metadata.mdclass.CommonTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
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

String created = (String)globalContext.execute(new AbstractBmTask("Create common template") {
    @Override
    public Object execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        if (configuration == null) {
            throw new IllegalStateException("Missing Configuration top object");
        }
        String fqn = "CommonTemplate.ПечатнаяФорма";
        if (transaction.getTopObjectByFqn(fqn) != null) {
            throw new IllegalStateException("CommonTemplate already exists: " + fqn);
        }

        CommonTemplate template = mdFactory.createCommonTemplate();
        template.setName("ПечатнаяФорма");
        template.getSynonym().put("ru", "Печатная форма");
        template.setTemplateType(TemplateType.SPREADSHEET_DOCUMENT);
        template.setUuid(UUID.randomUUID());
        configuration.getCommonTemplates().add(template);

        SpreadsheetDocument body = SheetFactory.createSpreadsheetDocument();
        template.setTemplate(body);

        String templateFqn = fqnGenerator.generateStandaloneObjectFqn(
            template.eClass(), template.getName()).toString();
        transaction.attachTopObject((IBmObject)template, templateFqn);

        String contentFqn = fqnGenerator.generateExternalPropertyFqn(
            template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
        transaction.attachTopObject((IBmObject)body, contentFqn);

        return template.getName();
    }
});
System.out.println("Created common template: " + created);
System.out.println("Project path: " + project.getLocation().toOSString());
System.out.println("Body exists: " +
    project.getFile("src/CommonTemplates/ПечатнаяФорма/Template.mxlx").exists());
return created;
```

### Minimal DCS common template

Use the same workflow, but set `TemplateType.DATA_COMPOSITION_SCHEMA` and create the body as:

```java
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchema;

DataCompositionSchema schema = dcsFactory.createDataCompositionSchema();
template.setTemplate(schema);
String contentFqn = fqnGenerator.generateExternalPropertyFqn(
    template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
transaction.attachTopObject((IBmObject)schema, contentFqn);
```

### Other common template types

For non-spreadsheet and non-DCS bodies, do not create/register the `CommonTemplate` until the source
body is available. A metadata-only stub is allowed only when the user's prompt explicitly requests
`metadata-only stub`, `только метаданные`, or `заглушка без файла`. Do not create fake files
for external components, geographical schemas, binary data, HTML, or text formats. Use
`template_type_matrix` for the exact type/extension mapping. If a real source file path is provided,
use `create_source_backed_template` and its Eclipse EFS-based JShell workflow. Do not handoff to
Task/design and do not stop after saying "создаю".

### Required post-check

For `CommonTemplate.ПечатнаяФорма`, check:

```text
<projectRoot>/src/CommonTemplates/ПечатнаяФорма/ПечатнаяФорма.mdo
<projectRoot>/src/CommonTemplates/ПечатнаяФорма/Template.mxlx
```

Call `GetMarkers` with `marker_type: "1c"` on the project-relative `.mdo` path. To verify body
existence, prefer the Eclipse resource API:

```java
project.getFile("src/CommonTemplates/ПечатнаяФорма/Template.mxlx").exists()
```

If a file tool is still needed, build the absolute path only from `project.getLocation().toOSString()`
printed by JShell and the deterministic relative path. Do not use `java.io.File` in JShell: it is
restricted. Do not call `Glob` for template verification. If the body file is missing, rerun the EDT
API attach step; do not create the missing file with `Write`.
