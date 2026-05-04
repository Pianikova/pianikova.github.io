## Safe Workflow: Create HTTPService

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create HTTPService") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        HTTPService service = mdFactory.createHTTPService();
        service.setName("HTTPServiceSample");
        service.setUuid(UUID.randomUUID());
        service.setName("OrdersApi");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(service.eClass(), service.getName()).toString();
        transaction.attachTopObject((IBmObject)service, fqn);
        configuration.getHttpServices().add(service);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getHttpServices()`

### Notes
- URL templates and HTTP methods are child objects. Keep routing details in follow-up steps.
