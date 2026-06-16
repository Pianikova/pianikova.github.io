## formFactory (`FormFactory.eINSTANCE`)

**Primary purpose:** EMF factory for `com._1c.g5.v8.dt.form.model` items, used to add controls
to an existing `Form` structure (the object returned by `formGenerator` or loaded from an
existing form).

**Package:** `com._1c.g5.v8.dt.form.model.FormFactory`

### Common create methods

| Method | Creates |
|--------|---------|
| `createForm()` | empty `Form` (prefer `formGenerator` for a usable default) |
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

`FormItem` ids and `dataPath` binding are non-trivial. For a usable default form prefer
`formGenerator`; use `formFactory` for targeted additions in `edit_form`, and always run
`GetMarkers` afterwards.
