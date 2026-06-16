## editingLanguageManager (`IEditingLanguageManager`)

**Primary purpose:** resolve the current editing language code for a project, required as the
`languageCode` argument of `formGenerator.generateForm(...)`.

**Package:** `com._1c.g5.v8.dt.core.platform.IEditingLanguageManager`

### Usage

```java
String languageCode = editingLanguageManager.getEditingLanguageCode(project); // e.g. "ru"
```

`project` is the `IProject` (`workspaceRoot.getProject("MyProject")`). Used together with
`formGenerator` and `formFieldGenerator` in the `create_object_form` scenario.
