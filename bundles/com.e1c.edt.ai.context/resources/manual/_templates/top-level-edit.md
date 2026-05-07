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
- Load existing top-level metadata with `transaction.getTopObjectByFqn(...)`
- Modify the loaded object directly
- Do not call `attachTopObject()` while editing an existing object
- Do not call `detachTopObject()` while editing an existing object
- For new child objects, set UUID and required `TypeDescription` before adding them to collections
- If this workflow requires more than one unknown EDT type, method, factory, field, or enum, call `JShellReflection` once with the full `queries` array before writing JShell code
- Mandatory next tool after this JShell edit/update: run `GetMarkers` with `marker_type: "1c"` for the project or changed file
- Do not report success and do not start the next 1C metadata CRUD operation until the `GetMarkers` response is checked
- If `GetMarkers` returns validation errors, treat the edit as incomplete until they are fixed

### Notes
- ${notes}
