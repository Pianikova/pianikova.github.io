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
        ${variableName}.setName("${title}Sample");
        ${variableName}.setUuid(UUID.randomUUID());
${setupBlock}        String fqn = fqnGenerator.generateStandaloneObjectFqn(${variableName}.eClass(), ${variableName}.getName()).toString();
        transaction.attachTopObject((IBmObject)${variableName}, fqn);
        configuration.${collection}.add(${variableName});
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.${collection}`
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` for the project or changed file

### Notes
- ${notes}
