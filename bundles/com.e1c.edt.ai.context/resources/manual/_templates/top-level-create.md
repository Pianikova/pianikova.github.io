## Safe Workflow: Create ${title}

```java
IProject project = workspaceRoot.getProject("MyProject");
${extraSetup}IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ${title}") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ${typeName} ${variableName} = mdFactory.${createMethod};
        ${variableName}.setName("${sampleName}");
        ${variableName}.setUuid(UUID.randomUUID());
${setupBlock}        String fqn = fqnGenerator.generateStandaloneObjectFqn(${variableName}.eClass(), ${variableName}.getName()).toString();
        transaction.attachTopObject((IBmObject)${variableName}, fqn);
        configuration.${collection}.add(${variableName});
        return null;
    }
});
```

### Rules
- If this exact scenario/manual card gives the factory, collection, FQN prefix, and safe setters you need, do not call `JShellReflection` before using it
- Before calling `JShellReflection`, check the EDT metadata API cards for the requested type
- If the API card lists the factory, collection, and setters you need, use this manual workflow without reflection
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.${collection}`
- If this workflow requires more than one unknown EDT type, method, factory, field, or enum, call `JShellReflection` once with the full `queries` array before writing JShell code
- Call `JShell` with `scope: "edt"`, `request_description`, and `response_description`
- Mandatory next tool after this JShell create: run `GetMarkers` with `marker_type: "1c"` for the project or changed file
- Do not report success and do not start the next 1C metadata CRUD operation until the `GetMarkers` response is checked
- Inspect all relevant 1C markers for the changed entity/top object, including errors, warnings, and infos; do not check only errors
- If `GetMarkers` returns relevant validation markers, treat the operation as incomplete until they are fixed or explicitly explained

### Notes
- ${notes}
