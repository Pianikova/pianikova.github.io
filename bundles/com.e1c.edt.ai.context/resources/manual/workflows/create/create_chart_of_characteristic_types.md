## Safe Workflow: Create ChartOfCharacteristicTypes

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ChartOfCharacteristicTypes") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ChartOfCharacteristicTypes chart = mdFactory.createChartOfCharacteristicTypes();
        chart.setName("ChartOfCharacteristicTypesSample");
        chart.setUuid(UUID.randomUUID());
        chart.setCodeLength(10);
                chart.setDescriptionLength(100);
                IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
                TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
                TypeDescription typeDesc = new TypeDescriptionBuilder()
                    .addType(stringType)
                    .build();

                chart.setType(typeDesc);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(chart.eClass(), chart.getName()).toString();
        transaction.attachTopObject((IBmObject)chart, fqn);
        configuration.getChartsOfCharacteristicTypes().add(chart);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getChartsOfCharacteristicTypes()`

### Notes
- ChartOfCharacteristicTypes requires a value `TypeDescription`; replace the sample string type with the actual allowed characteristic value types.
