## formGenerator (`IFormGenerator`)

**Primary purpose:** generate the default `Form` (form.model) structure for a `BasicForm`
metadata object (`CatalogForm`, `DocumentForm`, `CommonForm`, …).

**Package:** `com._1c.g5.v8.dt.form.generator.IFormGenerator`

### Method

```java
Form generateForm(
    MdObject mdObject,                      // parent metadata object (Catalog, Document, ...); for CommonForm pass null is NOT allowed — see notes
    BasicForm basicForm,                    // the form metadata object you created with mdFactory
    FormType formType,                      // com._1c.g5.v8.dt.form.generator.FormType (GENERIC, OBJECT, LIST, ...)
    ScriptVariant scriptVariant,            // v8project.getScriptVariant()
    String languageCode,                    // editingLanguageManager.getEditingLanguageCode(project)
    Version version,                        // v8project.getVersion()
    FormFieldInfo rootField,                // formFieldGenerator.getFormGeneratorFields(mdObject, formType, scriptVariant, version)
    Integer columnCount,                    // nullable; only meaningful for CONSTANT/OBJECT/GROUP/RECORD
    InterfaceCompatibilityMode mode);       // nullable
```

Returns a `com._1c.g5.v8.dt.form.model.Form` (never null).

### ⚠️ Two different `FormType` enums — do not confuse them

| Enum | Package | Meaning |
|------|---------|---------|
| `FormType` (generator) | `com._1c.g5.v8.dt.form.generator` | WHAT to generate: `GENERIC`, `OBJECT`, `LIST`, `CHOICE`, `RECORD`, `RECORD_SET`, `FOLDER`, … |
| `FormType` (mdclass) | `com._1c.g5.v8.dt.metadata.mdclass` | managed vs ordinary: `Managed` / `Ordinary` — used in `BasicForm.setFormType(...)` |

The `formGenerator` argument uses the **generator** enum. Import it explicitly to avoid clashes.

### Persisting the generated form

The generated `Form` is an external-property object: link it back with `form.setMdForm(basicForm)`
and attach it as a top object using the external-property FQN:

```java
form.setMdForm(basicForm);
String formFqn = fqnGenerator.generateExternalPropertyFqn(basicForm, MdClassPackage.Literals.BASIC_FORM__FORM);
transaction.attachTopObject((IBmObject)form, formFqn);
```

See the `create_object_form` manual scenario for the full, ordered workflow and the
external-resource verification rules.
