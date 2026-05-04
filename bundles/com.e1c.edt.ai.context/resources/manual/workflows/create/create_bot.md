## Safe Workflow: Create Bot

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create Bot") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        Bot bot = mdFactory.createBot();
        bot.setName("BotSample");
        bot.setUuid(UUID.randomUUID());
        bot.setName("SupportBot");
        String fqn = fqnGenerator.generateStandaloneObjectFqn(bot.eClass(), bot.getName()).toString();
        transaction.attachTopObject((IBmObject)bot, fqn);
        configuration.getBots().add(bot);
        return null;
    }
});
```

### Rules
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.getBots()`

### Notes
- Conversation scenarios, commands, and integration endpoints are configured after the bot metadata object is attached.
