# EDT Commands: Export / Import XML and External Objects

Call `JShell` with `scope: "edt"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`, scope `eclipse`).

## Which command is for what

Verified-present command ids (via `GetCommands` on a real EDT workspace):

| Command id | Purpose (зачем) | Caution |
|---|---|---|
| `com._1c.g5.v8.dt.platform.services.ui.commands.export.cf` | Выгрузить конфигурацию в файл .cf | opens a file dialog |
| `com._1c.g5.v8.dt.platform.services.ui.commands.import.configuration` | Загрузить конфигурацию из ИБ в проект | opens a wizard |
| `com._1c.g5.v8.dt.platform.services.ui.commands.import.extension` | Загрузить расширение из ИБ | opens a wizard |
| `com._1c.g5.v8.dt.export.ui.commands.exportExternalObject` | Выгрузить внешний отчёт/обработку (.erf/.epf) | may open a file dialog |
| `com._1c.g5.v8.dt.import.ui.commands.importExternalObject` | Загрузить внешний отчёт/обработку | may open a file dialog |

Note: there is NO `commandExport`/`commandImport` id — do not use them.

## Workflow

1. Verify the command and parameters via `GetCommands`; check `isEnabled()`.
2. These commands are selection-dependent (project / external object). Pass a typed selection
   (Pattern 3), as in manual `edt_commands_infobase`.
3. ⚠️ The export/import commands open WIZARDS (user interaction). From the agent, prefer:
   - reporting the exact command/hotkey to the user for interactive use, or
   - a non-interactive path when one exists in the scenario catalog.

Pre-check example:

Wrap the snippet in `{ ... }` with LOCAL names (persistent-session rule, see
`eclipse_command_workflow`):

```java
{
    String[] ids = {
      "com._1c.g5.v8.dt.platform.services.ui.commands.export.cf",
      "com._1c.g5.v8.dt.platform.services.ui.commands.import.configuration",
      "com._1c.g5.v8.dt.platform.services.ui.commands.import.extension",
      "com._1c.g5.v8.dt.export.ui.commands.exportExternalObject",
      "com._1c.g5.v8.dt.import.ui.commands.importExternalObject"
    };
    for (String id : ids) {
        var c = commandService.getCommand(id);
        System.out.println(id + " | defined=" + c.isDefined() + " | enabled=" + c.isEnabled());
    }
}
return null;
```

## Rules

- ⛔ Do not blindly execute wizard commands — they block on user input. Report to the user, or ask
  which non-interactive path to take.
- Import mutates the project — explicit user request only; after import run `GetMarkers`
  (`marker_type:"1c"`) on changed objects (manual `check_1c_markers_after_crud`).
- Never hand-write Designer XML with file tools — that path is forbidden; use these commands or
  the scenario catalog.
- Always end with `return <value>;`.
