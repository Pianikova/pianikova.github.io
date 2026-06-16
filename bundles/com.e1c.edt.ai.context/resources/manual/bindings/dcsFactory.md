## dcsFactory (`DcsFactory.eINSTANCE`)

**Primary purpose:** EMF factory for `DataCompositionSchema` content of a template whose
`TemplateType` is `DATA_COMPOSITION_SCHEMA` (СКД).

**Package:** `com._1c.g5.v8.dt.dcs.model.schema.DcsFactory`

### Common create methods

| Method | Creates |
|--------|---------|
| `createDataCompositionSchema()` | root СКД schema |
| `createDataCompositionSchemaDataSource()` | data source |
| `createDataCompositionSchemaDataSetQuery()` | query data set |

### Usage

```java
DataCompositionSchema schema = DcsFactory.eINSTANCE.createDataCompositionSchema();
// add a data source / data set as needed, then attach it to the template:
template.setTemplateType(TemplateType.DATA_COMPOSITION_SCHEMA);
template.setTemplate(schema);   // external-property reference, persisted to a separate file
```

`template.setTemplate(...)` is a transient `@ExternalProperty` reference — EDT persists the
content as a separate resource on commit; there is no manual "register" call. Always verify
with `GetMarkers` afterwards (see `fill_template_content`).
