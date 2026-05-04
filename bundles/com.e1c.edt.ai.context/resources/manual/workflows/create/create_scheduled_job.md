## Safe Workflow: Create ScheduledJob

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ScheduledJob") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ScheduledJob job = mdFactory.createScheduledJob();
        job.setName("ScheduledJobSample");
        job.setUuid(UUID.randomUUID());
        job.setName("NightlyCleanup");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(job.eClass(), job.getName()).toString();
        transaction.attachTopObject((IBmObject)job, fqn);
        configuration.getScheduledJobs().add(job);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getScheduledJobs()`

### Notes
- Add schedule details and called method configuration after the top-level job object exists.
