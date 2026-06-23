## formFactory (`FormFactory.eINSTANCE`)

**Primary purpose:** EMF factory for `com._1c.g5.v8.dt.form.model` items, used to add controls
to an existing `Form` structure (the object returned by `formGenerator` or loaded from an
existing form).

**Package:** `com._1c.g5.v8.dt.form.model.FormFactory`

### Common create methods

| Method | Creates |
|--------|---------|
| `createForm()` | empty `Form` (do **not** use for object-owned form creation/repair; use `formGenerator`) |
| `createFormAttribute()` | `FormAttribute` (form data attribute) |
| `createFormField()` | `FormField` (input/label/… control bound to a data path) |
| `createFormGroup()` | `FormGroup` (container) |

### Adding items

```java
// form is a com._1c.g5.v8.dt.form.model.Form
FormGroup group = FormFactory.eINSTANCE.createFormGroup();
group.setName("MainGroup");
form.getItems().add(group);

FormField field = FormFactory.eINSTANCE.createFormField();
field.setName("DescriptionField");
group.getItems().add(field);          // FormGroup is a FormItemContainer

FormAttribute attr = FormFactory.eINSTANCE.createFormAttribute();
attr.setName("Object");
attr.setMain(true);
form.getAttributes().add(attr);
```

`FormItem` ids and `dataPath` binding are non-trivial. For object-owned form creation or repair,
never fall back to `FormFactory.eINSTANCE.createForm()`: it creates an empty shell and can leave a
bad default form on disk. Use `formGenerator.generateForm(...)` for the complete default layout.
Use `formFactory` only for targeted additions in `edit_form`, and always run `GetMarkers`
afterwards.
