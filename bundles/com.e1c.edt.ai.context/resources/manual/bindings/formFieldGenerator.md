## formFieldGenerator (`IFormFieldGenerator`)

**Primary purpose:** build the root `FormFieldInfo` tree that
`formGenerator.generateForm(...)` requires as its `rootField` argument.

**Package:** `com._1c.g5.v8.dt.form.generator.IFormFieldGenerator`

### Method

```java
FormFieldInfo getFormGeneratorFields(
    MdObject mdObject,            // same parent object you pass to generateForm
    FormType formType,            // com._1c.g5.v8.dt.form.generator.FormType
    ScriptVariant scriptVariant,  // v8project.getScriptVariant()
    Version version);             // v8project.getVersion()
```

The returned `FormFieldInfo` is the field-selection tree the form wizard would show on its
second page (all selectable attributes of the parent object). Pass it straight into
`generateForm(...)` to get a default form populated with the object's fields.

Use inside the same BM transaction. See `create_object_form` for the full workflow.
