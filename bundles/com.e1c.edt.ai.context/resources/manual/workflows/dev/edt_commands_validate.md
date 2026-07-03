# EDT Commands: Validate Configuration

Call `JShell` with `scope: "edt"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`, scope `eclipse`).

## Which command is for what

| Command id | Purpose (зачем) | Notes |
|---|---|---|
| `com._1c.g5.v8.dt.commands.validate` | Запустить проверку (валидацию) выбранных объектов/проекта | selection-dependent |
| `com._1c.g5.v8.dt.ui.command.startCheck` | Запустить проверки (check framework) | selection-dependent |
| `com._1c.g5.v8.dt.ui.command.openCheck` | Открыть описание проверки | UI navigation |

## Preferred result channel — the GetMarkers tool

Validation results surface as 1C markers. Do NOT parse UI output: call the `GetMarkers` tool with
`marker_type: "1c"` on the project or the specific `.mdo` — it waits for the background Derived
Data pipeline and returns complete markers (see manual `check_1c_markers_after_crud`).

## Workflow

1. Verify the command via `GetCommands`; check `isEnabled()`.
2. Trigger validation with a typed project selection (Pattern 3):

Wrap the snippet in `{ ... }` with LOCAL names (persistent-session rule, see
`eclipse_command_workflow`):

```java
{
    var project = workspaceRoot.getProject("МояКонфигурация");
    var command = commandService.getCommand("com._1c.g5.v8.dt.commands.validate");
    System.out.println("defined=" + command.isDefined() + " enabled=" + command.isEnabled());
    var base = handlerService.getCurrentState();
    var sel = new org.eclipse.jface.viewers.StructuredSelection(project);
    var ctx = new EvaluationContext(base, sel);
    ctx.addVariable("selection", sel);
    var event = new ExecutionEvent(command, java.util.Collections.emptyMap(), null, ctx);
    try {
        Object r = command.executeWithChecks(event);
        System.out.println("validation requested, result=" + r);
    } catch (org.eclipse.core.commands.NotEnabledException | org.eclipse.core.commands.NotHandledException e) {
        System.out.println("command not available: " + e.getClass().getSimpleName()
            + " -> fallback: project.build + GetMarkers");
        project.build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD,
            new org.eclipse.core.runtime.NullProgressMonitor());
        System.out.println("build triggered");
    }
}
return null;
```

3. Read the results with the `GetMarkers` tool (`marker_type:"1c"`) and report the concrete
   markers.

## Rules

- `GetMarkers` (`marker_type:"1c"`) is the source of truth for validation results.
- Validate BEFORE updating an infobase (manual `edt_commands_infobase`).
- Do not finish with "проверьте вручную" — report the found markers.
- Always end with `return <value>;`.
