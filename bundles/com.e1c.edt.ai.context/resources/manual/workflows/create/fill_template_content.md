## Safe Workflow: Fill Template content (СКД / табличный документ / source-backed bodies)

Attaches a content object to an existing `Template`/`CommonTemplate` whose `TemplateType` matches.
Run after `create_object_template` (or `create_common_template`).
Consult `template_type_matrix` before choosing the content path.

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
- ❌ **Never create template files with `Write`** (`*.dcs`, `*.mxl`, owner `.mdo`, settings
  `*.xml`). The body must first be generated/registered through the EMF factory +
  `setTemplate(...)` inside a BM transaction (below). After the generated body file exists, `Edit`
  may be used for small targeted changes, followed by `GetMarkers`.
- ✅ **Use `Edit` only as refinement.** Once EDT has produced the body file, file `Edit` may adjust
  a small known text fragment in an existing `.dcs`/`.mxl(x)` resource or in the owner `.mdo`. Always read
  the file first, keep the replacement exact and minimal, and validate the owner `.mdo` with
  `GetMarkers`. If the file does not exist, return to this EDT API workflow instead of `Write`.
- ⛔ **Never invent DCS/moxel XML structure — with ANY tool.** Replacing the body of a
  `.dcs`/`.mxlx` with XML whose element structure you guessed is forbidden regardless of the
  mechanism: file `Edit`, file `Write`, JShell `IFile.setContents`/EFS with an XML string literal.
  Invented dialects (attribute-style `<dataSource name= type=/>`, made-up `dcscom:DataCompositionField`,
  `dcsset:structure="list"` and similar) are NOT deserializable by the EDT designer, while
  `GetMarkers` stays green — the result looks successful but the макет is broken. The valid sources
  of meaningful DCS content are, in order of preference: (1) copying a real, verified `.dcs` source
  file provided by the user; (2) the canonical `Template.dcs` skeleton below with ONLY the allowed
  substitutions; (3) the `dcsFactory` EMF API inside a BM transaction with each class/method
  verified via JShellReflection. Moxel (`.mxlx`) has no hand-written path at all: `SheetFactory` or
  copy only.
- ⛔ **Exact factory packages — do not guess.** The DCS factory is
  `com._1c.g5.v8.dt.dcs.model.schema.DcsFactory` (NOT `com._1c.g5.v8.dt.dcs.util.*`). The
  spreadsheet factory is `com._1c.g5.v8.dt.moxel.MoxelFactory`. Both are exposed as the
  pre-bound `dcsFactory` / `moxelFactory` session variables — use those rather than importing.
- ⛔ **Do not hand-build a rich DCS from guessed EMF classes.** Java has no import aliases, so
  snippets such as `import ...DcsFactory as CoreDcsFactory;` are invalid and produce
  `compiler.err.expected`. Do not invent classes like `DataCompositionSchemaQuery`,
  `DataCompositionSchemaField`, settings/group/filter APIs, or `CatalogRef.<Name>` proxies from
  memory. If the user asks for a meaningful DCS with data sets/query/settings/resources, either
  copy a known-good `.dcs` source file into the already-created template body, or first verify every
  package/class/method with JShellReflection before writing JShell code. Without a verified source
  or reflected API, stop and ask for a `.dcs` source/sample instead of looping on compilation errors.
- ✅ If the template does not exist yet, run `create_object_template` first; do not create the
  template metadata here.
- ✅ **Use the real request project name.** Never leave `MyProject` in executable JShell. Check
  `project.exists()` and `modelManager.getModel(project) != null` before `getGlobalContext()`.
  Missing project, missing BM model, missing owner, and missing template must be explicit
  `IllegalStateException`s, not `NullPointerException`s.
- ✅ **Verify the body through Eclipse resources before any file tool.** Use the same `IProject` and
  `project.getFile("src/.../Template.mxlx").exists()` (or `Template.dcs`). Do not guess the
  workspace path for `Read`, and do not call `Glob`.

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

| TemplateType              | factory binding | create method                                        | content type            |
|---------------------------|-----------------|------------------------------------------------------|-------------------------|
| `DATA_COMPOSITION_SCHEMA` | `dcsFactory`    | `DcsFactory.eINSTANCE.createDataCompositionSchema()` | `DataCompositionSchema` |
| `SPREADSHEET_DOCUMENT`    | `SheetFactory`  | `SheetFactory.createSpreadsheetDocument()`           | `SpreadsheetDocument`   |

> ⛔ For `SPREADSHEET_DOCUMENT` do **not** use the bare `MoxelFactory.eINSTANCE.createSpreadsheetDocument()`.
> It omits the mandatory `printSettings`/`viewSettings`/`formats`/`columns`/`defaultFormatIndex`, so the
> `.mxlx` never persists/loads and the editor fails with
> `Unsupported embedded object type ... EObjectImpl`. Use `SheetFactory.createSpreadsheetDocument()`
> (package `com._1c.g5.v8.dt.moxel.sheet`, pre-imported in the `edt` scope), which initialises them.

Other types are source-backed resources:

| TemplateType                           | Body file          | Rule                                                    |
|----------------------------------------|--------------------|---------------------------------------------------------|
| `TEXT_DOCUMENT`                        | `Template.txt`     | Requires explicit text content/source.                  |
| `HTML_DOCUMENT`                        | `Template.htmldoc` | Requires explicit HTML content/source.                  |
| `BINARY_DATA`                          | `Template.bin`     | Requires a real binary source.                          |
| `GEOGRAPHICAL_SCHEMA`                  | `Template.geos`    | Requires a real geographical schema source/tool output. |
| `GRAPHICAL_SCHEMA`                     | `Template.scheme`  | Requires a real graphical schema source/tool output.    |
| `DATA_COMPOSITION_APPEARANCE_TEMPLATE` | `Template.dcsat`   | Requires a real DCS appearance template source.         |
| `ADD_IN`                               | `Template.addin`   | Requires a real external component file.                |

For those types, do not use DCS/Moxel factories and do not invent placeholder body files. If the
user did not provide body content/source, stop and ask for it or report that only metadata can be
created now.

### Example — DataCompositionSchema content

This example creates or re-attaches an **empty** DCS body only. It is safe for making the template
openable. It is not a recipe for a report schema with data sets/query/settings.

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
        // Empty DCS body only. Do not add data sets/settings here unless the exact EDT DCS API
        // was verified with JShellReflection in this EDT version.
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

### Example — replace existing object-owned DCS body from a source `.dcs`

Use this path when the user provided a real DCS body or when reusing a proven ERP/ERP2 template.
First create/register the object template through `create_object_template`; then replace the body
file with Eclipse EFS. This avoids fragile manual DCS EMF construction.

```java
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

IProject project = workspaceRoot.getProject("<ProjectName>");
String bodyPath = "src/<OwnerFolder>/<OwnerName>/Templates/<TemplateName>/Template.dcs";
String sourcePathString = "<absolute path to source Template.dcs>";

IFileSystem fileSystem = EFS.getLocalFileSystem();
IFileStore sourceStore = fileSystem.getStore(new Path(sourcePathString));
if (!sourceStore.fetchInfo().exists()) {
    throw new IllegalStateException("Source file not found: " + sourcePathString);
}

IFile targetFile = project.getFile(bodyPath);
if (!targetFile.exists()) {
    throw new IllegalStateException("Target body is missing; run create_object_template first: " + bodyPath);
}

java.io.InputStream sourceStream = sourceStore.openInputStream(0, new NullProgressMonitor());
try {
    targetFile.setContents(sourceStream, IResource.FORCE, new NullProgressMonitor());
} finally {
    sourceStream.close();
}
project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

System.out.println("Body exists: " + targetFile.exists());
return bodyPath;
```

### Canonical `Template.dcs` skeleton (the ONLY allowed hand-written DCS)

When the user asks for a meaningful DCS (a query over configuration data) and provides no source
`.dcs` file, replace the body content with THIS skeleton exactly. It is verified against real
EDT/ERP2 templates. Element structure, element order, namespaces, and every `xsi:type` are FIXED.

Allowed substitutions ONLY: the query text, the field list (`dataPath`/`field`/`title` — one
`<field xsi:type="DataSetFieldField">` per query column), the settings variant name/presentation,
and the sort field. Everything else must stay byte-identical. Do not add invented elements,
attributes (`name=`/`type=` attribute forms do not exist in this schema), or `xsi:type`s.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<DataCompositionSchema xmlns="http://v8.1c.ru/8.1/data-composition-system/schema" xmlns:dcscom="http://v8.1c.ru/8.1/data-composition-system/common" xmlns:dcscor="http://v8.1c.ru/8.1/data-composition-system/core" xmlns:dcsset="http://v8.1c.ru/8.1/data-composition-system/settings" xmlns:v8="http://v8.1c.ru/8.1/data/core" xmlns:v8ui="http://v8.1c.ru/8.1/data/ui" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<dataSource>
		<name>ИсточникДанных1</name>
		<dataSourceType>Local</dataSourceType>
	</dataSource>
	<dataSet xsi:type="DataSetQuery">
		<name>НаборДанных1</name>
		<field xsi:type="DataSetFieldField">
			<dataPath>Код</dataPath>
			<field>Код</field>
			<title xsi:type="v8:LocalStringType">
				<v8:item>
					<v8:lang>ru</v8:lang>
					<v8:content>Код</v8:content>
				</v8:item>
			</title>
		</field>
		<field xsi:type="DataSetFieldField">
			<dataPath>Наименование</dataPath>
			<field>Наименование</field>
			<title xsi:type="v8:LocalStringType">
				<v8:item>
					<v8:lang>ru</v8:lang>
					<v8:content>Наименование</v8:content>
				</v8:item>
			</title>
		</field>
		<dataSource>ИсточникДанных1</dataSource>
		<query>ВЫБРАТЬ
	Авторы.Код КАК Код,
	Авторы.Наименование КАК Наименование
ИЗ
	Справочник.Авторы КАК Авторы</query>
	</dataSet>
	<settingsVariant>
		<dcsset:name>Основной</dcsset:name>
		<dcsset:presentation xsi:type="v8:LocalStringType">
			<v8:item>
				<v8:lang>ru</v8:lang>
				<v8:content>Список</v8:content>
			</v8:item>
		</dcsset:presentation>
		<dcsset:settings xmlns:style="http://v8.1c.ru/8.1/data/ui/style" xmlns:sys="http://v8.1c.ru/8.1/data/ui/fonts/system" xmlns:web="http://v8.1c.ru/8.1/data/ui/colors/web" xmlns:win="http://v8.1c.ru/8.1/data/ui/colors/windows">
			<dcsset:order>
				<dcsset:item xsi:type="dcsset:OrderItemField">
					<dcsset:field>Наименование</dcsset:field>
					<dcsset:orderType>Asc</dcsset:orderType>
				</dcsset:item>
			</dcsset:order>
			<dcsset:item xsi:type="dcsset:StructureItemGroup">
				<dcsset:order>
					<dcsset:item xsi:type="dcsset:OrderItemAuto"/>
				</dcsset:order>
				<dcsset:selection>
					<dcsset:item xsi:type="dcsset:SelectedItemAuto"/>
				</dcsset:selection>
			</dcsset:item>
		</dcsset:settings>
	</settingsVariant>
</DataCompositionSchema>
```

Structure reference (fixed order inside `DataCompositionSchema`): `dataSource` (element children
`name` + `dataSourceType`, NOT attributes) → `dataSet xsi:type="DataSetQuery"` (`name`, then the
`field` list, then `dataSource` reference, then `query`) → `settingsVariant`
(`dcsset:name`, `dcsset:presentation`, `dcsset:settings` with optional `dcsset:order` and one
`dcsset:item xsi:type="dcsset:StructureItemGroup"` — a group without `dcsset:groupItems` renders
detail records, i.e. a plain list). A `StructureItemGroup` with `dcsset:groupItems` adds grouping;
copy that shape from a real `.dcs` only.

### Verified optional DCS blocks (parameters, resources, grouping, selection)

When the user asks for параметры/ресурсы/итоги/группировки/отборы, extend the skeleton ONLY with
these verified shapes (taken from real ERP2 schemas). Top-level element order inside
`DataCompositionSchema` is FIXED: `dataSource` → `dataSet`(s) → `calculatedField`(s) →
`totalField`(s) → `parameter`(s) → `settingsVariant`(s).

Parameter — note `<v8:Type>` (NEVER a bare `<Type>` element, which is not deserializable here):

```xml
	<parameter>
		<name>НачалоПериода</name>
		<title xsi:type="v8:LocalStringType">
			<v8:item>
				<v8:lang>ru</v8:lang>
				<v8:content>Начало периода</v8:content>
			</v8:item>
		</title>
		<valueType>
			<v8:Type>xs:dateTime</v8:Type>
			<v8:DateQualifiers>
				<v8:DateFractions>DateTime</v8:DateFractions>
			</v8:DateQualifiers>
		</valueType>
		<value xsi:type="xs:dateTime">0001-01-01T00:00:00</value>
		<useRestriction>true</useRestriction>
	</parameter>
```

For a string parameter: `<v8:Type>xs:string</v8:Type>` and `<value xsi:type="xs:string"/>`.
Add a `parameter` only when the query text contains the matching `&Имя` placeholder or the user
explicitly asked for a filter parameter.

Resource (итог/ресурс) — a `totalField` after the data sets:

```xml
	<totalField>
		<dataPath>Скидка</dataPath>
		<expression>Сумма(Скидка)</expression>
	</totalField>
```

Calculated field (вычисляемое поле):

```xml
	<calculatedField>
		<dataPath>Поступление</dataPath>
		<expression>ВЫБОР КОГДА ... ТОГДА ... ИНАЧЕ 0 КОНЕЦ</expression>
		<title xsi:type="v8:LocalStringType">
			<v8:item>
				<v8:lang>ru</v8:lang>
				<v8:content>Поступление</v8:content>
			</v8:item>
		</title>
	</calculatedField>
```

Explicit field selection at the settings root (instead of `SelectedItemAuto`):

```xml
			<dcsset:selection>
				<dcsset:item xsi:type="dcsset:SelectedItemField">
					<dcsset:field>ФИО</dcsset:field>
				</dcsset:item>
			</dcsset:selection>
```

Grouping by a field — the ONLY valid `GroupItemField` shape (there is NO `order` child inside
`GroupItemField`; ordering is a separate `dcsset:order` block with `OrderItemField`):

```xml
			<dcsset:item xsi:type="dcsset:StructureItemGroup">
				<dcsset:groupItems>
					<dcsset:item xsi:type="dcsset:GroupItemField">
						<dcsset:field>ФИО</dcsset:field>
						<dcsset:groupType>Items</dcsset:groupType>
						<dcsset:periodAdditionType>None</dcsset:periodAdditionType>
						<dcsset:periodAdditionBegin xsi:type="xs:dateTime">0001-01-01T00:00:00</dcsset:periodAdditionBegin>
						<dcsset:periodAdditionEnd xsi:type="xs:dateTime">0001-01-01T00:00:00</dcsset:periodAdditionEnd>
					</dcsset:item>
				</dcsset:groupItems>
				<dcsset:order>
					<dcsset:item xsi:type="dcsset:OrderItemAuto"/>
				</dcsset:order>
				<dcsset:selection>
					<dcsset:item xsi:type="dcsset:SelectedItemAuto"/>
				</dcsset:selection>
			</dcsset:item>
```

Anything not shown above (filters, conditional appearance, nested unions, charts) must be copied
from a real `.dcs` file — do not extrapolate.

### Minimum requirements for a MEANINGFUL DCS (any mechanism: skeleton or EMF API)

The EMF/`dcsFactory` path guarantees a deserializable FORMAT but not a working SCHEMA. Whether the
body is written from the skeleton above or built via `dcsFactory` in a BM task, the result must
satisfy ALL of these, or the отчёт will silently produce nothing:

1. A DECLARED data source: element `dataSource` with `name` (e.g. `ИсточникДанных1`) and
   `dataSourceType` = `Local`. Via EMF: create the data-source object and add it to the schema's
   data-source collection — verify the exact factory method with JShellReflection.
2. Every `dataSet` must reference the declared source BY ITS NAME (`ИсточникДанных1`). Never write
   `local`, `Local`, or an undeclared name — the data set becomes detached from any source.
3. A `settingsVariant` with settings that select something (`dcsset:selection` with
   `SelectedItemAuto`, or a `StructureItemGroup` as in the skeleton). A schema without a settings
   variant opens in the designer but outputs an empty report.
4. `parameter` elements ONLY for actual `&Параметр` placeholders present in the query text. Do not
   create a parameter per query column — that is noise that confuses the designer.
5. After the write, `Read` the produced `Template.dcs` back and check items 1–3 are present in the
   XML before reporting success. `GetMarkers` alone does not validate schema semantics.

After writing the body, re-open check: the EDT DCS designer must be able to load the file. Validate
with `GetMarkers` on the owner `.mdo` AND report to the user that the schema should be opened in
the DCS editor to confirm.

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

Call `GetMarkers` with `marker_type: "1c"` on the owner's `.mdo` using a project-relative path and
confirm the content file was written, e.g.:

```
src/Reports/SalesAnalysis/Templates/ОсновнаяСхемаКомпоновкиДанных/Template.dcs   (DCS)
src/Catalogs/Products/Templates/ПечатнаяФорма/Template.mxlx                      (spreadsheet)
```

For object-owned templates there is no `Templates/<TemplateName>/<TemplateName>.mdo`. The child
metadata is serialized inside the owner `.mdo` as `<templates ...>`. Validate that owner `.mdo`,
then check the separate body file (`Template.mxlx` or `Template.dcs`).

Do not use `Glob` to find template files. Avoid wildcard directories such as `**/Catalogs/...` and
rootless directories such as `src/Catalogs`. The file location is deterministic:

```text
<projectRoot>/src/<OwnerFolder>/<OwnerName>/Templates/<TemplateName>/Template.mxlx
<projectRoot>/src/<OwnerFolder>/<OwnerName>/Templates/<TemplateName>/Template.dcs
```

For `GetMarkers`, prefer the project-relative owner `.mdo` path with `project_name`; never invent an
absolute root like `C:\EDT_projects\...` or a default workspace such as
`C:\Users\...\eclipse-workspace\...`. Verify the body with the Eclipse resource API in JShell:

```java
project.getFile("src/<OwnerFolder>/<OwnerName>/Templates/<TemplateName>/Template.mxlx").exists()
```

If a file tool is still needed, build the absolute path only from `project.getLocation().toOSString()`
printed by JShell and the deterministic relative path. Do not use `java.io.File` in JShell: it is
restricted. Do not call `Glob` for template verification.

If the file is missing or markers appear, the template metadata still exists — report that and
rerun this scenario only after fixing the concrete JShell/API error that prevented
`attachTopObject(...)` from committing. Do not tell the user to fill the body manually for
`DATA_COMPOSITION_SCHEMA` or `SPREADSHEET_DOCUMENT`; those content types are covered here. For
source-backed types, be explicit that a real source file/content is required.
