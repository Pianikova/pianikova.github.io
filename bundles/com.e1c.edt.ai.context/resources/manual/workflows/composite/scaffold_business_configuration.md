## Scaffold Business Configuration

Use this guardrail for broad user requests such as "create a full configuration for a shop/accounting workflow".

Broad prompts still must be implemented as verified metadata CRUD. "Do not use subtasks" means do not ask the user to decompose the work; it does not mean skipping validation, markers, or safe creation order.

### Required approach

- Create a new configuration project first and verify that `IV8Project`, `IBmModel`, and top object `Configuration` are accessible.
- For project creation, use the exact `create_configuration_project` workflow from JShellManual. Do not improvise a plain Eclipse project and then try to create `Configuration` through BM; `V8Project is null` means the project creation failed.
- If the user explicitly says that the current configuration must not be used, do not select an existing project from `GetProjects` as the target. Generate a new unique project name, create it, then use only that `IProject` handle/name in every subsequent JShell call.
- At the start of every JShell snippet in a broad scenario, assert the target project name is the newly-created project. If `workspaceRoot.getProject(...)` resolves to any pre-existing working project, throw `IllegalStateException` before mutating metadata.
- Choose a realistic, limited first slice: catalogs/enums/constants, then documents/tabular sections, then registers and cross-object links.
- Before each top-level object creation, probe `transaction.getTopObjectByFqn("<Type>.<Name>")`. If it already exists, continue by reading/verifying/editing that object; do not call `attachTopObject(...)` again for the same FQN. Broad configuration prompts often fail halfway and are then retried, so creation code must be restartable from a partial project.
- Use only top-level metadata objects confirmed by manual or `MdClass.xcore`; do not treat child objects as standalone CRUD objects.
- Use `JShellManual` before each metadata family batch, not only before the first project/catalog/document step. For example, call the register workflow before registers, the service workflow before service objects, and the common-object workflow before modules/reports/processors. Use `JShellReflection` only for exact missing API facts after the closest manual card is loaded.
- If any JShell call returns non-empty `compilation_errors` or `runtime_errors`, stop the broad scenario immediately. Do not continue with the next family of objects and do not report partial success as a completed configuration. Fix the failing step first, then run markers for the affected scope.
- Run `GetMarkers` with `marker_type: "1c"` after the project is created, after each changed top-level `.mdo` when its path is known, and after the final batch. For large cross-object scenarios, project-wide markers are required before reporting success. A JShell "final state" readback is useful, but it is not a replacement for `GetMarkers`.
- Final readback must verify business completeness, not only top-level FQNs.
  For each requested object, check the relevant child collections by name:
  catalog/document attributes, tabular sections and their attributes, register
  dimensions/resources/attributes, enum values, subsystem content, and cross
  links. `GetMarkers` can return 0 when a requested child element was simply
  never created.
- Wrap final verification JShell code in `{ ... }` and avoid reusing top-level
  names such as `result`. Persistent JShell sessions can otherwise execute or
  display stale state from an earlier verification snippet.
- If the user asked for "all that is needed" or "maximum variety", do not report full success after creating only catalogs/documents. Report the actual coverage and what remains unsupported or unverified.

### Attribute guardrails

- Every attribute, dimension, resource, and tabular-section attribute must have a valid `TypeDescription` assigned before it is added to its parent collection.
- For ordinary string attributes in CRUD tests, keep `setStringQualifiers(length, false)` conservative, usually `length <= 100`, unless a specific long-text/manual pattern is available for the target EDT version.
- For numbers, remember the EDT builder order: `setNumberQualifiers(scale, precision, nonNegative)`. For `Number(10,2)`, call `setNumberQualifiers(2, 10, false)`; for `Number(5,0)`, call `setNumberQualifiers(0, 5, true)`.
- Before reporting success, inspect or validate that no generated `.mdo` contains `numberQualifiers` with `scale > precision`. This is the common symptom of accidentally calling `setNumberQualifiers(precision, scale, ...)`.

### Document guardrails

- Do not create document attributes named `Date`, `Дата`, `Number`, `Номер`, `Posted`, `Проведен`, `Ref`, `Ссылка`, `DeletionMark`, or `ПометкаУдаления`. These are standard document properties.
- For document tabular-section numbers, use `setNumberQualifiers(scale, precision, nonNegative)`. For `Number(10,2)`, call `setNumberQualifiers(2, 10, false)`.
- If documents should move stock or money, create the register and link it from the document side with `Document.getRegisterRecords().add(register)`, then run markers.

### Minimum report

The final answer must include:

- top-level metadata objects actually created;
- requested child elements actually created, grouped by object;
- links actually configured;
- `JShellReflection` queries used and why;
- final `GetMarkers` result;
- explicit limitations when the created configuration does not cover the requested business scope.

### Target project guard

Use this pattern in broad prompts after creating the new project. Replace
`AiBookCrm_...` with the exact project name that was just created.

```java
String targetProjectName = "AiBookCrm_20260515_001";
IProject project = workspaceRoot.getProject(targetProjectName);
if (!project.exists()) {
    throw new IllegalStateException("Target project does not exist: " + targetProjectName);
}
if ("Склад".equals(project.getName())) {
    throw new IllegalStateException("Refusing to mutate existing working project: " + project.getName());
}
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
if (v8project == null || bmModel == null) {
    throw new IllegalStateException("Target project is not initialized as a V8 project: " + targetProjectName);
}
```
