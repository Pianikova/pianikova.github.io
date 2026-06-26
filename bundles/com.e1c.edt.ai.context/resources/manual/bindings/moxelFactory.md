## moxelFactory (`MoxelFactory.eINSTANCE`)

**Primary purpose:** EMF factory for `SpreadsheetDocument` (табличный документ) content of a
template whose `TemplateType` is `SPREADSHEET_DOCUMENT`.

**Package:** `com._1c.g5.v8.dt.moxel.MoxelFactory`

### Usage

⛔ **Do NOT build the body with the bare `MoxelFactory.eINSTANCE.createSpreadsheetDocument()`.**
That returns a `SpreadsheetDocument` with no `printSettings`, `viewSettings`, `formats`, `columns`
or `defaultFormatIndex`. Such an under-initialised document does not round-trip through the `.mxlx`
serializer: nothing (or an invalid resource) is written, so on reopen the template's
`BASIC_TEMPLATE__TEMPLATE` reference resolves to an unresolved `EObjectImpl` proxy and the
spreadsheet editor fails with
`Unsupported embedded object type. SpreadsheetDocument expected, but actual is: ... EObjectImpl`.

✅ **Always create the body with `SheetFactory.createSpreadsheetDocument()`** (package
`com._1c.g5.v8.dt.moxel.sheet`, already imported in the `edt` scope). This is exactly how EDT's own
model layer builds a fresh spreadsheet — it initialises the mandatory `printSettings`,
`viewSettings`, a default `Format`, a default `Columns`, and `defaultFormatIndex`:

```java
SpreadsheetDocument document = SheetFactory.createSpreadsheetDocument();   // fully initialised blank table
// optionally add rows/areas, then attach to the template:
template.setTemplateType(TemplateType.SPREADSHEET_DOCUMENT);
template.setTemplate(document);   // external-property reference, persisted to a separate file
```

`MoxelFactory.eINSTANCE` is still used for the individual pieces (rows, cells, drawings) you add to
that document; it is just not the right entry point for the document itself.

`template.setTemplate(...)` is a transient `@ExternalProperty` reference — EDT persists the
content as a separate `.mxlx` resource on commit; there is no manual "register" call.
A `SheetFactory.createSpreadsheetDocument()` with no rows is a valid (blank) table. Always verify
with `GetMarkers` afterwards (see `fill_template_content`).
