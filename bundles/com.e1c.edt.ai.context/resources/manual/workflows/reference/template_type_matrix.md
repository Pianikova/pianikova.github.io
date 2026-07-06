## Template Type Matrix

Use this card before creating or filling any `Template`/`CommonTemplate`.
It maps user words to `TemplateType`, disk file names observed in EDT projects, and the safe
creation strategy.

### Type selection

| User wording | Java `TemplateType` | `.mdo` value | Body file | Strategy |
|--------------|---------------------|--------------|-----------|----------|
| табличный документ, MXL, печатная форма, печатный макет, ценник | `SPREADSHEET_DOCUMENT` | `SpreadsheetDocument` or omitted default | `Template.mxlx` | Create metadata and blank body with `SheetFactory.createSpreadsheetDocument()`, attach as external property. |
| схема компоновки данных, СКД | `DATA_COMPOSITION_SCHEMA` | `DataCompositionSchema` | `Template.dcs` | Create metadata and blank body with `dcsFactory.createDataCompositionSchema()`, attach as external property. |
| текстовый документ, txt, xml as text | `TEXT_DOCUMENT` | `TextDocument` | `Template.txt` | Create metadata with this type. Body text requires explicit source/content. |
| HTML документ, HTML-макет | `HTML_DOCUMENT` | `HTMLDocument` | `Template.htmldoc` | Create metadata with this type. Body HTML requires explicit source/content. |
| двоичные данные, binary, zip/docx/png/etc. | `BINARY_DATA` | `BinaryData` | `Template.bin` | Create metadata with this type only when the binary source is provided or already exists. |
| географическая схема | `GEOGRAPHICAL_SCHEMA` | `GeographicalSchema` | `Template.geos` | Usually imported/edited by EDT specialized tooling. Ask for source file if needed. |
| графическая схема | `GRAPHICAL_SCHEMA` | `GraphicalSchema` | `Template.scheme` | Usually imported/edited by EDT specialized tooling. Ask for source file if needed. |
| макет оформления компоновки данных, оформление СКД | `DATA_COMPOSITION_APPEARANCE_TEMPLATE` | `DataCompositionAppearanceTemplate` | `Template.dcsat` | Requires an appearance-template source/body. Do not treat as ordinary DCS. |
| внешняя компонента, add-in | `ADD_IN` | `AddIn` | `Template.addin` | Requires a real external component file. Never create a placeholder add-in. |

### ERP2 examples

Examples from `C:\Projects\erp2\ERP2\src`:

- `CommonTemplates\ГеографическаяСхемаРоссияРегионыИВсеГорода`: `GeographicalSchema` + `Template.geos`.
- `CommonTemplates\ОформлениеОтчетовЗеленый`: `DataCompositionAppearanceTemplate` + `Template.dcsat`.
- `CommonTemplates\ДрайверАТОЛККТ54ФЗ10XФФД12_ru`: `AddIn` + `Template.addin`.
- object-owned graphical schema templates use `GraphicalSchema` + `Template.scheme`.
- text/common exchange templates use `TextDocument` + `Template.txt`.
- HTML templates use `HTMLDocument` + `Template.htmldoc`.
- binary templates use `BinaryData` + `Template.bin`.

### Hard rules

- Do not confuse Java enum constants with XML values: Java uses constants like
  `TemplateType.TEXT_DOCUMENT`, while `.mdo` stores values like `TextDocument`.
- `SPREADSHEET_DOCUMENT` and `DATA_COMPOSITION_SCHEMA` are the only body types currently covered by
  the model API examples in `create_object_template`, `create_common_template`, and
  `fill_template_content`.
- For `TEXT_DOCUMENT`, `HTML_DOCUMENT`, `BINARY_DATA`, `GEOGRAPHICAL_SCHEMA`, `GRAPHICAL_SCHEMA`,
  `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, and `ADD_IN`, do not invent body content and do not report
  a complete body unless a real source/body file is provided or already exists.
- If the user asks for one of those source-backed types without providing content, create only the
  metadata if that is acceptable in the request, or ask for the source file/content. Do not create
  fake `Template.bin`, `Template.addin`, `Template.geos`, `Template.scheme`, or `Template.dcsat`.
