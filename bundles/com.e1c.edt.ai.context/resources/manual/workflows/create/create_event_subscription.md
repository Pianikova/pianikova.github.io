## Safe Workflow: Create EventSubscription

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create EventSubscription") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        EventSubscription subscription = mdFactory.createEventSubscription();
        subscription.setName("EventSubscriptionSample");
        subscription.setUuid(UUID.randomUUID());
        subscription.setName("OnDocumentPost");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(subscription.eClass(), subscription.getName()).toString();
        transaction.attachTopObject((IBmObject)subscription, fqn);
        configuration.getEventSubscriptions().add(subscription);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getEventSubscriptions()`

### Notes
- After creation, configure source object, event, and handler module explicitly.
