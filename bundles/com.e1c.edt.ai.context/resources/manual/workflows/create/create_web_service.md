## Safe Workflow: Create WebService

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create WebService") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        WebService service = mdFactory.createWebService();
        service.setName("WebServiceSample");
        service.setUuid(UUID.randomUUID());
        service.setName("OrderService");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(service.eClass(), service.getName()).toString();
        transaction.attachTopObject((IBmObject)service, fqn);
        configuration.getWebServices().add(service);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getWebServices()`

### Notes
- Operations and parameters are child objects. Create the service first, then add `Operation` and `Parameter` children.
