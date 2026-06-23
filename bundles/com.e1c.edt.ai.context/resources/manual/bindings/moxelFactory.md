## moxelFactory (`MoxelFactory.eINSTANCE`)

**Primary purpose:** EMF factory for `SpreadsheetDocument` (табличный документ) content of a
template whose `TemplateType` is `SPREADSHEET_DOCUMENT`.

**Package:** `com._1c.g5.v8.dt.moxel.MoxelFactory`

### Usage

```java
SpreadsheetDocument document = MoxelFactory.eINSTANCE.createSpreadsheetDocument();
// optionally add rows/columns/areas, then attach to the template:
template.setTemplateType(TemplateType.SPREADSHEET_DOCUMENT);
template.setTemplate(document);   // external-property reference, persisted to a separate file
```

`template.setTemplate(...)` is a transient `@ExternalProperty` reference — EDT persists the
content as a separate `.mxl`-style resource on commit; there is no manual "register" call.
An empty `SpreadsheetDocument` is a valid (blank) template. Always verify with `GetMarkers`
afterwards (see `fill_template_content`).
