## Safe Workflow: Create FilterCriterion

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create FilterCriterion") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        FilterCriterion criterion = mdFactory.createFilterCriterion();
        criterion.setName("FilterCriterionSample");
        criterion.setUuid(UUID.randomUUID());
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();

        criterion.setType(typeDesc);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(criterion.eClass(), criterion.getName()).toString();
        transaction.attachTopObject((IBmObject)criterion, fqn);
        configuration.getFilterCriteria().add(criterion);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getFilterCriteria()`

### Notes
- FilterCriterion implements TypeDescriptionProvider. Make the type narrow and intentional.
