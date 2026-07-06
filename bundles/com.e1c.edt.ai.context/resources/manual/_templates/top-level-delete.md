## Safe Workflow: Delete ${title}

Use `delete_metadata_object` for top-level metadata deletion. Top-level
metadata must be deleted through
`IMdRefactoringService.createMdObjectDeleteRefactoring(...)`, not through a
manual BM task that removes from `Configuration.${collection}` and calls
`detachTopObject(...)`.

### Required shape

1. Load `${typeName}` by FQN `${fqnPrefix}.${sampleName}` in a short BM task.
2. If it is absent, print that it is already deleted and stop.
3. Resolve `IMdRefactoringService` through OSGi.
4. Execute `createMdObjectDeleteRefactoring(Arrays.asList((MdObject)object))`.
5. Call `refactoring.perform()`.
6. Refresh the project with `project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor())`.
7. Run project-wide `GetMarkers`.
8. Read back the FQN and verify it returns `null`.

### Rules

- ⛔ Before deleting, confirm the target project is writable: `GetProjects` must show `read_only: false`. If `read_only: true` (full vendor support, `object_belonging: Adopted`), do NOT run this workflow — including via JShell — see `readonly_configuration`.
- Do not use `EcoreUtil.delete()` for top-level metadata objects.
- Do not use `configuration.${collection}.remove(object)` +
  `transaction.detachTopObject((IBmObject)object)` for JShell CRUD deletes.
- For child metadata objects, use the child delete workflow and validate the
  parent top-level `.mdo`.
- If this workflow requires more than one unknown EDT type, method, factory,
  field, or enum, call `JShellReflection` once with the full `queries` array
  before writing JShell code.
- Mandatory next tool after delete: run `GetMarkers` with `marker_type: "1c"`
  project-wide because references may break outside the deleted object's file.
- Do not report success and do not start the next 1C metadata CRUD operation
  until the `GetMarkers` response is checked.
- Do not fix unrelated project-wide markers. For delete, repair only markers
  caused by the deleted entity or directly affected references unless the user
  asks for broader cleanup.

### Notes

- ${notes}
