## Safe Workflow: Create Language

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Language") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Language language = mdFactory.createLanguage();
        language.setName("LanguageSample");
        language.setUuid(UUID.randomUUID());
        language.setName("English");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(language.eClass(), language.getName()).toString();
        transaction.attachTopObject((IBmObject)language, fqn);
        configuration.getLanguages().add(language);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getLanguages()`

### Notes
- After creation, set configuration default language and local string entries appropriately.
