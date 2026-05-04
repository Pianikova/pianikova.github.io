## Safe Workflow: Create IntegrationService

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create IntegrationService") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        IntegrationService service = mdFactory.createIntegrationService();
        service.setName("IntegrationServiceSample");
        service.setUuid(UUID.randomUUID());
        service.setName("ERPIntegration");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(service.eClass(), service.getName()).toString();
        transaction.attachTopObject((IBmObject)service, fqn);
        configuration.getIntegrationServices().add(service);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getIntegrationServices()`

### Notes
- Add `IntegrationServiceChannel` objects after the parent service is attached to configuration.
