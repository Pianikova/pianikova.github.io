## Safe Workflow: Edit ${title}

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

${typeName} result = globalContext.execute(new AbstractBmTask<${typeName}>("Edit ${title}") {
    @Override
    public ${typeName} execute(IBmTransaction transaction, IProgressMonitor monitor) {
        ${typeName} ${variableName} = (${typeName})transaction.getTopObjectByFqn("${fqnPrefix}.${sampleName}");
        if (${variableName} == null) {
            System.out.println("${title} not found: ${fqnPrefix}.${sampleName}");
            return null;
        }

${editBlock}        return ${variableName};
    }
});
```

### Rules
- ⛔ Before mutating, confirm the target project is writable: `GetProjects` must show `read_only: false`. If `read_only: true` (full vendor support), do NOT run this workflow — including via JShell — see `readonly_configuration`.
- Load existing top-level metadata with `transaction.getTopObjectByFqn(...)`
- Modify the loaded object directly
- Do not call `attachTopObject()` while editing an existing object
- Do not call `detachTopObject()` while editing an existing object
- For new child objects, set UUID and required `TypeDescription` before adding them to collections
- If this workflow requires more than one unknown EDT type, method, factory, field, or enum, call `JShellReflection` once with the full `queries` array before writing JShell code
- Call `JShell` with `scope: "edt"`, `request_description`, and `response_description`
- Mandatory next tool after this JShell edit/update: run `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when the path is known or can be derived
- Do not report success and do not start the next 1C metadata CRUD operation until the `GetMarkers` response is checked
- Inspect all relevant 1C markers for the changed entity/top object, including errors, warnings, and infos; do not check only errors
- Do not fix unrelated project-wide markers. Use project-wide `GetMarkers` only for references, registrars, command interfaces, configuration-level changes, or when the `.mdo` path cannot be derived.
- If `GetMarkers` returns relevant validation markers, treat the edit as incomplete until they are fixed or explicitly explained

### Notes
- ${notes}
