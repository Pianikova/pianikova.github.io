## Safe Workflow: Create Task

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Task") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Task task = mdFactory.createTask();
        task.setName("TaskSample");
        task.setUuid(UUID.randomUUID());
        task.setAutonumbering(true);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(task.eClass(), task.getName()).toString();
        transaction.attachTopObject((IBmObject)task, fqn);
        configuration.getTasks().add(task);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getTasks()`

### Notes
- Tasks often mirror BusinessProcess patterns. Use TaskAttribute/TaskTabularSection for child objects.
