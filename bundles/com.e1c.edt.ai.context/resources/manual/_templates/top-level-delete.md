## Safe Workflow: Delete ${title}

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Delete ${title}") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ${typeName} ${variableName} = (${typeName})transaction.getTopObjectByFqn("${fqnPrefix}.${sampleName}");
        if (${variableName} == null) {
            System.out.println("${title} not found: ${fqnPrefix}.${sampleName}");
            return null;
        }

        configuration.${collection}.remove(${variableName});
        transaction.detachTopObject((IBmObject)${variableName});
        System.out.println("${title} deleted successfully: ${sampleName}");
        return null;
    }
});
```

### Rules
- Remove top-level metadata from the correct `Configuration.${collection}` collection first
- Then call `transaction.detachTopObject((IBmObject)object)`
- Do not use `EcoreUtil.delete()` for top-level metadata objects
- Check the object exists before removing it
- If this workflow requires more than one unknown EDT type, method, factory, field, or enum, call `JShellReflection` once with the full `queries` array before writing JShell code
- Call `JShell` with `scope: "edt"`, `request_description`, and `response_description`
- Mandatory next tool after this JShell delete: run `GetMarkers` with `marker_type: "1c"` project-wide because references may break outside the deleted object's file
- Do not report success and do not start the next 1C metadata CRUD operation until the `GetMarkers` response is checked
- Inspect all relevant 1C markers for the changed entity/top object and affected references, including errors, warnings, and infos; do not check only errors
- Do not fix unrelated project-wide markers. For delete, repair only markers caused by the deleted entity or directly affected references unless the user asks for broader cleanup.
- If `GetMarkers` returns relevant validation markers, treat the deletion as incomplete until references are repaired or explicitly explained

### Notes
- ${notes}
