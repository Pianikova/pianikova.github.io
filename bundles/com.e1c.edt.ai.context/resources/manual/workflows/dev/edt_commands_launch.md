# EDT Commands: Launch / Debug 1C Client

Call `JShell` with `scope: "edt"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`, scope `eclipse`).

EDT contributes launch-shortcut commands that start the 1C:Enterprise client for the selected
project (launch configuration type `com._1c.g5.v8.dt.launching.core.RuntimeClient`).

## Which command is for what

| Command id | Purpose (зачем) |
|---|---|
| `com._1c.g5.v8.dt.launching.ui.shortcut.ThinClient.run` | Запустить тонкий клиент 1С |
| `com._1c.g5.v8.dt.launching.ui.shortcut.ThinClient.debug` | Отладка в тонком клиенте |
| `com._1c.g5.v8.dt.launching.ui.shortcut.ThickClient.run` | Запустить толстый клиент |
| `com._1c.g5.v8.dt.launching.ui.shortcut.ThickClient.debug` | Отладка в толстом клиенте |
| `com._1c.g5.v8.dt.launching.ui.shortcut.WebClient.run` | Запустить веб-клиент |
| `com._1c.g5.v8.dt.launching.ui.shortcut.WebClient.debug` | Отладка в веб-клиенте |
| `com._1c.g5.v8.dt.launching.mobile.ui.shortcut.AndroidMobileApplication.run` | Запустить мобильное приложение (Android) |
| `com._1c.g5.v8.dt.launching.mobile.ui.shortcut.AndroidMobileApplication.debug` | Отладка мобильного приложения (Android) |
| `com._1c.g5.v8.dt.debug.ui.RemoteRuntime.debug` | Подключиться к серверу отладки 1С:Предприятия |

## Workflow

1. Confirm the command exists and check its parameters via `GetCommands` (do not guess).
2. These commands act on the SELECTED project. Programmatically pass the project as a typed
   selection (Pattern 3):

Wrap the snippet in `{ ... }` with LOCAL names (persistent-session rule, see
`eclipse_command_workflow`):

```java
{
    var project = workspaceRoot.getProject("МояКонфигурация");
    if (project.exists() && project.isOpen()) {
        var command = commandService.getCommand("com._1c.g5.v8.dt.launching.ui.shortcut.ThinClient.run");
        var base = handlerService.getCurrentState();
        var sel = new org.eclipse.jface.viewers.StructuredSelection(project);
        var ctx = new EvaluationContext(base, sel);
        ctx.addVariable("selection", sel);
        var event = new ExecutionEvent(command, java.util.Collections.emptyMap(), null, ctx);
        try {
            Object r = command.executeWithChecks(event);
            System.out.println("launch requested, result=" + r);
        } catch (org.eclipse.core.commands.NotEnabledException | org.eclipse.core.commands.NotHandledException e) {
            System.out.println("command not available in this context: " + e.getClass().getSimpleName());
        }
    } else {
        System.out.println("project missing/closed");
    }
}
return null;
```

3. Observe the started launch:

```java
{
    var lm = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
    for (var l : lm.getLaunches()) {
        var cfg = l.getLaunchConfiguration();
        System.out.println((cfg != null ? cfg.getName() : "<no config>") + " terminated=" + l.isTerminated());
    }
}
return null;
```

## Rules

- Launching starts a REAL 1C client against a REAL infobase — only on explicit user request.
- The project must be associated with an infobase; the launch may first update the infobase and
  take minutes. If the command reports errors, update the infobase first: manual
  `edt_commands_infobase`.
- To stop the client use `org.eclipse.debug.ui.commands.TerminateAll` (manual
  `eclipse_commands_run_debug`).
- Always end with `return <value>;`.
