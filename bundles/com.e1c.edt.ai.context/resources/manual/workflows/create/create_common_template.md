## Safe Workflow: Create CommonTemplate (общий макет)

Creates a top-level `CommonTemplate` and attaches a valid body resource in the same BM
transaction. The result must be an openable common template with both:

- `src/CommonTemplates/<Name>/<Name>.mdo`
- `src/CommonTemplates/<Name>/Template.<ext>` (`.mxlx` for spreadsheet, `.dcs` for DCS)

Do not report success after only the `.mdo` exists.

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
