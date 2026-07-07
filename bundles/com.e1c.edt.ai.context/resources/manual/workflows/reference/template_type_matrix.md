## Template Type Matrix

Use this card before creating or filling any `Template`/`CommonTemplate`.
It maps user words to `TemplateType`, disk file names observed in EDT projects, and the safe
creation strategy.

### Type selection

| User wording                                                    | Java `TemplateType`                    | `.mdo` value                             | Body file          | Strategy                                                                                                     |
|-----------------------------------------------------------------|----------------------------------------|------------------------------------------|--------------------|--------------------------------------------------------------------------------------------------------------|
| табличный документ, MXL, печатная форма, печатный макет, ценник | `SPREADSHEET_DOCUMENT`                 | `SpreadsheetDocument` or omitted default | `Template.mxlx`    | Create metadata and blank body with `SheetFactory.createSpreadsheetDocument()`, attach as external property. |
| схема компоновки данных, СКД                                    | `DATA_COMPOSITION_SCHEMA`              | `DataCompositionSchema`                  | `Template.dcs`     | Create metadata and blank body with `dcsFactory.createDataCompositionSchema()`, attach as external property. |
| текстовый документ, txt, xml as text                            | `TEXT_DOCUMENT`                        | `TextDocument`                           | `Template.txt`     | Create metadata with this type. Body text requires explicit source/content.                                  |
| HTML документ, HTML-макет                                       | `HTML_DOCUMENT`                        | `HTMLDocument`                           | `Template.htmldoc` | Create metadata with this type. Body HTML requires explicit source/content.                                  |
| двоичные данные, binary, zip/docx/png/etc.                      | `BINARY_DATA`                          | `BinaryData`                             | `Template.bin`     | Create metadata with this type only when the binary source is provided or already exists.                    |
| географическая схема                                            | `GEOGRAPHICAL_SCHEMA`                  | `GeographicalSchema`                     | `Template.geos`    | Usually imported/edited by EDT specialized tooling. Ask for source file if needed.                           |
| графическая схема                                               | `GRAPHICAL_SCHEMA`                     | `GraphicalSchema`                        | `Template.scheme`  | Usually imported/edited by EDT specialized tooling. Ask for source file if needed.                           |
| макет оформления компоновки данных, оформление СКД              | `DATA_COMPOSITION_APPEARANCE_TEMPLATE` | `DataCompositionAppearanceTemplate`      | `Template.dcsat`   | Requires an appearance-template source/body. Do not treat as ordinary DCS.                                   |
| внешняя компонента, add-in                                      | `ADD_IN`                               | `AddIn`                                  | `Template.addin`   | Requires a real external component file. Never create a placeholder add-in.                                  |

### ERP2 examples

Examples from `C:\Projects\erp2\ERP2\src`:

- `CommonTemplates\ГеографическаяСхемаРоссияРегионыИВсеГорода`: `GeographicalSchema` + `Template.geos`.
- `CommonTemplates\ОформлениеОтчетовЗеленый`: `DataCompositionAppearanceTemplate` + `Template.dcsat`.
- `CommonTemplates\ДрайверАТОЛККТ54ФЗ10XФФД12_ru`: `AddIn` + `Template.addin`.
- object-owned graphical schema templates use `GraphicalSchema` + `Template.scheme`.
- text/common exchange templates use `TextDocument` + `Template.txt`.
- HTML templates use `HTMLDocument` + `Template.htmldoc`.
- binary templates use `BinaryData` + `Template.bin`.

### Body file formats (what is inside each `Template.<ext>`)

Knowing the real on-disk format prevents two failure modes: inventing a wrong XML dialect, and
treating a generable body as source-only (or vice versa).

| Body file          | Format                        | Root element / content                                                                                                                                                                                                                                      | Can the agent produce it?                                                                                                                                                                         |
|--------------------|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Template.mxlx`    | XML (moxel spreadsheet)       | `<document xmlns="http://v8.1c.ru/8.2/data/spreadsheet">` with `languageSettings`, `columns`, `rowsItem`, cells                                                                                                                                             | YES, but ONLY via `SheetFactory.createSpreadsheetDocument()` in a BM task, or by copying a real `.mxlx`. Never compose this XML by hand — the cell/format model is too intricate.                 |
| `Template.dcs`     | XML (data composition schema) | `<DataCompositionSchema xmlns="http://v8.1c.ru/8.1/data-composition-system/schema">`: `dataSource` → `dataSet` (`xsi:type="DataSetQuery"` with `field xsi:type="DataSetFieldField"` and `<query>`) → optional `parameter`s → `settingsVariant` (`dcsset:*`) | YES: blank via `dcsFactory.createDataCompositionSchema()`; meaningful content via the canonical skeleton in `fill_template_content` (change only query/fields/names) or by copying a real `.dcs`. |
| `Template.txt`     | Plain text (UTF-8)            | The text itself, no wrapper markup                                                                                                                                                                                                                          | YES, when the user supplies the text content or a source file. Do not invent business content.                                                                                                    |
| `Template.htmldoc` | HTML (UTF-8)                  | Regular HTML page                                                                                                                                                                                                                                           | YES, when the user supplies HTML/content or a source file. Do not invent business content.                                                                                                        |
| `Template.bin`     | Binary                        | Arbitrary bytes (images, archives, office files)                                                                                                                                                                                                            | NO. Only a byte-exact copy of a user-provided file.                                                                                                                                               |
| `Template.addin`   | Binary (ZIP)                  | 1C external component package                                                                                                                                                                                                                               | NO. Only a byte-exact copy of a real component file.                                                                                                                                              |
| `Template.geos`    | XML (geographical schema)     | `<geographicalSchema ...>`, typically megabytes of map geometry                                                                                                                                                                                             | NO. Only a copy of a real `.geos` file — geometry cannot be composed.                                                                                                                             |
| `Template.scheme`  | XML (graphical schema)        | `<GraphicalSchema xmlns="http://v8.1c.ru/8.3/xcf/scheme">` with `sch:*` graph nodes                                                                                                                                                                         | NO. Only a copy of a real `.scheme` file; the EDT graphical editor owns this format.                                                                                                              |
| `Template.dcsat`   | XML (appearance template)     | `<AppearanceTemplate xmlns="http://v8.1c.ru/8.1/data-composition-system/appearance-template">`                                                                                                                                                              | NO. Only a copy of a real `.dcsat` file. Do not treat it as a `DataCompositionSchema` — different root and namespace.                                                                             |

Common trap: all these XML dialects share the `v8:`/`dcsset:`/`xsi:` namespaces but have DIFFERENT
element structures. Never transplant elements between formats and never guess element or attribute
names — if the exact structure is not shown in a manual example or a real source file you read, do
not write it.

### Hard rules

- Do not confuse Java enum constants with XML values: Java uses constants like
  `TemplateType.TEXT_DOCUMENT`, while `.mdo` stores values like `TextDocument`.
- `SPREADSHEET_DOCUMENT` and `DATA_COMPOSITION_SCHEMA` are the only body types currently covered by
  the model API examples in `create_object_template`, `create_common_template`, and
  `fill_template_content`.
- For `TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`, `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`,
  `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, and `ADD_IN`, do not invent body content and do not report
  a complete body unless a real source/body file is provided or already exists.
- If the user asks for one of those source-backed types without providing content, stop and ask for
  the source file/content (see `create_source_backed_template`). Do not create metadata-only
  registrations and do not create fake `Template.bin`, `Template.addin`, `Template.geos`,
  `Template.scheme`, or `Template.dcsat`.
- The body file name is always `Template.<ext>` — never `<TemplateName>.<ext>`.
