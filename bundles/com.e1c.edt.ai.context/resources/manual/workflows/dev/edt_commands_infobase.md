# EDT Commands: Infobase Operations

Call `JShell` with `scope: "edt"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`, scope `eclipse`).

## Which command is for what

| Command id | Purpose (зачем) | Caution |
|---|---|---|
| `com._1c.g5.v8.dt.platform.services.ui.commands.updateInfobase` | Обновить конфигурацию информационной базы из проекта | long-running; mutates the infobase |
| `com._1c.g5.v8.dt.platform.services.ui.commands.launchDesigner` | Открыть Конфигуратор для ИБ | starts external app; may be absent — check `isDefined()` first |
| `com._1c.g5.v8.dt.platform.services.ui.commands.dump` | Выгрузить ИБ в файл .dt | may open a file dialog |
| `com._1c.g5.v8.dt.platform.services.ui.commands.restore` | Загрузить ИБ из файла .dt | destructive; may open a dialog |
| `com._1c.g5.v8.dt.platform.services.ui.commands.export.cf` | Выгрузить конфигурацию в .cf | may open a file dialog |
| `com._1c.g5.v8.dt.platform.services.ui.commands.import.configuration` | Импортировать конфигурацию из ИБ в проект | may open a wizard |
| `com._1c.g5.v8.dt.platform.services.ui.commands.import.extension` | Импортировать расширение из ИБ | may open a wizard |

## Workflow — update infobase (обновить ИБ)

1. Verify the command and its parameters via `GetCommands` (category of EDT infobase commands);
   check `isEnabled()`.
2. The command acts on the infobase/project selection. Pass the project as a typed selection
   (Pattern 3):

Wrap the snippet in `{ ... }` with LOCAL names (persistent-session rule, see
`eclipse_command_workflow`):

```java
{
    var project = workspaceRoot.getProject("МояКонфигурация");
    var command = commandService.getCommand("com._1c.g5.v8.dt.platform.services.ui.commands.updateInfobase");
    System.out.println("defined=" + command.isDefined() + " enabled=" + command.isEnabled());
    var base = handlerService.getCurrentState();
    var sel = new org.eclipse.jface.viewers.StructuredSelection(project);
    var ctx = new EvaluationContext(base, sel);
    ctx.addVariable("selection", sel);
    var event = new ExecutionEvent(command, java.util.Collections.emptyMap(), null, ctx);
    try {
        Object r = command.executeWithChecks(event);
        System.out.println("update requested, result=" + r);
    } catch (org.eclipse.core.commands.NotEnabledException | org.eclipse.core.commands.NotHandledException e) {
        System.out.println("command not available: select the infobase/project in the navigator first ("
            + e.getClass().getSimpleName() + ")");
    }
}
return null;
```

The update runs as a background job — report that it was started, do not block waiting.

## Rules

- All these operations touch a REAL infobase — run only on explicit user request; `restore` is
  DESTRUCTIVE (overwrites the infobase) — always confirm with the user first.
- ⚠️ `dump`/`restore`/`export.cf`/`import.*` may open dialogs or wizards. If a dialog would open,
  stop and tell the user to complete it interactively instead of blocking the UI thread.
- Validate the configuration before updating the infobase: manual `edt_commands_validate` —
  updating from a broken configuration fails.
- Never invent infobase names/paths; take them from the user or the selection.
- Always end with `return <value>;`.
