# EDT Commands: BSL Code Editing, Refactoring and Navigation

Call `JShell` with `scope: "edt"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`, scope `eclipse`).

These commands act on the ACTIVE BSL editor and its cursor/selection. Most refactorings open an
INPUT DIALOG (rename target, method name). From an agent, prefer: (a) report the command/hotkey to
the user for interactive use, or (b) edit the BSL model/text directly. Verify `isEnabled()` — these
are disabled without an active BSL editor.

## Which command is for what (verified present in EDT)

| Command id | Purpose (зачем) | Caution |
|---|---|---|
| `com._1c.g5.v8.dt.bsl.ui.refactoring.RenameElement` | Переименовать элемент BSL (метод/переменную) | opens rename dialog; active editor |
| `com._1c.g5.v8.dt.bsl.ui.BslMethodExtractor` | Извлечь метод из выделения | opens dialog; needs selection |
| `com._1c.g5.v8.dt.bsl.ui.BslLocalVariableExtractor` | Извлечь локальную переменную | opens dialog; needs selection |
| `com._1c.g5.v8.dt.bsl.ui.BslWrapInRegion` | Обернуть код в область #Область | active editor + selection |
| `com._1c.g5.v8.dt.bsl.ui.BslGenerateMethodComment` | Сгенерировать комментарий метода | cursor on a method |
| `com._1c.g5.v8.dt.bsl.ui.editor.callhierarchy.CallHierarchy` | Показать иерархию вызовов | opens a view |
| `com._1c.g5.v8.dt.search.ui.findRefToMdObject` | Найти ссылки на объект конфигурации | opens Search view |
| `com._1c.g5.v8.dt.md.ui.openMdObjectDialog` | Открыть объект конфигурации (выбор) | opens a chooser dialog |
| `com._1c.g5.v8.dt.ui.commands.openEditor` | Открыть редактор объекта (p=2) | check parameters via `GetCommands` |
| `com._1c.g5.v8.dt.ui.commands.focusNavigator` | Сфокусировать Навигатор | non-modal; safe |
| `com._1c.g5.v8.dt.ui.commands.source_update` | Обновить исходники проекта | regenerates project sources |
| `org.eclipse.ui.edit.text.goto.line` | Перейти к строке | active text editor |

## Discover parameters and enablement first

```java
{
    String[] ids = {
      "com._1c.g5.v8.dt.bsl.ui.refactoring.RenameElement",
      "com._1c.g5.v8.dt.bsl.ui.BslMethodExtractor",
      "com._1c.g5.v8.dt.bsl.ui.BslWrapInRegion",
      "com._1c.g5.v8.dt.md.ui.openMdObjectDialog",
      "com._1c.g5.v8.dt.ui.commands.focusNavigator",
      "com._1c.g5.v8.dt.ui.commands.source_update"
    };
    for (String id : ids) {
        var c = commandService.getCommand(id);
        System.out.println(id + " | defined=" + c.isDefined() + " | enabled=" + c.isEnabled());
    }
}
return null;
```

## Execute a non-modal command (example: focus the navigator)

```java
{
    var cmd = commandService.getCommand("com._1c.g5.v8.dt.ui.commands.focusNavigator");
    if (cmd.isEnabled()) {
        handlerService.executeCommand("com._1c.g5.v8.dt.ui.commands.focusNavigator", null);
        System.out.println("navigator focused");
    } else {
        System.out.println("focusNavigator disabled in current context");
    }
}
return null;
```

## Rules

- ⛔ Refactoring/open commands open DIALOGS — do not execute them blindly from the agent; they block
  on user input. Prefer editing the BSL model/text directly, or hand the command/hotkey to the user.
- These commands need an ACTIVE BSL editor and a valid cursor/selection — check `isEnabled()` and,
  if needed, open the target first (manual `execute_command` / `edt_commands_*`).
- `openEditor` takes parameters — read them via `GetCommands` before building a `ParameterizedCommand`.
- Wrap executable snippets in `{ ... }` with local names (persistent-session rule).
- Always end with `return <value>;`.
