# Run / Debug / Terminate Commands

Call `JShell` with `scope: "eclipse"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`).

## Which command is for what

| Command id | Purpose (зачем) | Notes |
|---|---|---|
| `org.eclipse.debug.ui.commands.RunLast` | Repeat the last launch (повторить запуск) | needs launch history, else `NotEnabledException` |
| `org.eclipse.debug.ui.commands.DebugLast` | Repeat the last launch in debug (отладка) | needs launch history |
| `org.eclipse.debug.ui.commands.Terminate` | Terminate the selected process (остановить) | selection in Debug/Console view |
| `org.eclipse.debug.ui.commands.TerminateAll` | Terminate all processes (остановить все) | defined; enabled when something runs |
| `org.eclipse.debug.ui.commands.RunToLine` | Run to line while debugging | debug session required |

In 1C:EDT the 1C client is launched by EDT-specific commands — see manual `edt_commands_launch`
(scope `edt`).

## Examples

Wrap executable snippets in `{ ... }` with LOCAL names (persistent-session rule, see
`eclipse_command_workflow`).

Repeat the last launch:
```java
{
    var cmd = commandService.getCommand("org.eclipse.debug.ui.commands.RunLast");
    if (cmd.isEnabled()) {
        Object r = handlerService.executeCommand("org.eclipse.debug.ui.commands.RunLast", null);
        System.out.println("run last: " + r);
    } else {
        System.out.println("nothing was launched yet in this session");
    }
}
return null;
```

Terminate all running processes:
```java
{
    var cmd = commandService.getCommand("org.eclipse.debug.ui.commands.TerminateAll");
    if (cmd.isEnabled()) {
        handlerService.executeCommand("org.eclipse.debug.ui.commands.TerminateAll", null);
        System.out.println("terminate all requested");
    } else {
        System.out.println("no running processes");
    }
}
return null;
```

## Rules

- Launching/terminating affects REAL processes — do it only on explicit user request.
- `RunLast`/`DebugLast` need launch history in this IDE session; when there is none, ask the user
  which configuration to launch instead of guessing.
- Check `isEnabled()` first and report the disabled state honestly.
- Always end with `return <value>;`.
