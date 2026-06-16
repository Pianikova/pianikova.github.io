## Safe Workflow: Create an object-owned Template (макет)

Creates a `Template` that belongs to a metadata object (`Catalog`, `Document`, `Report`, …) with
a chosen `TemplateType`. To also fill the template body, chain `fill_template_content`.

> For a stand-alone shared template use `create_common_template` (creates a top-level
> `CommonTemplate`). This scenario is for templates **owned by an object**, added to
> `owner.getTemplates()`.

### Hard rules — never violate

- ❌ **Never hand-write the template with file tools** (`Write`/`Edit` on `Template.mdo`,
  `*.dcs`, `*.mxl`, settings `*.xml`). Create the `Template` metadata via `mdFactory.createTemplate()`
  inside a BM transaction, then fill the body via `fill_template_content` (`dcsFactory`/`moxelFactory`
  + `template.setTemplate(...)`). Hand-written XML is fragile and is not registered with the owner.
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
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;

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

        Template template = mdFactory.createTemplate();
        template.setName("ПечатнаяФорма");
        template.getSynonym().put("ru", "Печатная форма");
        template.setTemplateType(TemplateType.SPREADSHEET_DOCUMENT);
        template.setUuid(UUID.randomUUID());
        owner.getTemplates().add(template);

        return template.getName();
    }
});
System.out.println("Created template: " + created);
return created;
```

To fill the body (spreadsheet / СКД), run `fill_template_content` next.

### Required post-check

A `Template` is a child object — call `GetMarkers` with `marker_type: "1c"` on the **owner's**
`.mdo` (`src/Catalogs/<OwnerName>/<OwnerName>.mdo`). The template body file lives at
`src/Catalogs/<OwnerName>/Templates/<TemplateName>/Template.<ext>` once content is added. Fix
only markers relevant to the new template before reporting success.
