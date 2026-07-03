# File, Editor and Build Commands

Call `JShell` with `scope: "eclipse"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`).

## Which command is for what

| Command id | Purpose (зачем) | Notes |
|---|---|---|
| `org.eclipse.ui.file.save` | Save the active editor (сохранить) | needs a dirty ACTIVE editor, else disabled |
| `org.eclipse.ui.file.saveAll` | Save all editors (сохранить все) | disabled when nothing is dirty |
| `org.eclipse.ui.file.close` | Close the active editor (закрыть) | needs an active editor |
| `org.eclipse.ui.file.closeAll` | Close all editors (закрыть все) | usually enabled; verified working |
| `org.eclipse.ui.file.refresh` | Refresh selection from disk (обновить) | selection-dependent |
| `org.eclipse.ui.project.buildAll` | Build the whole workspace (собрать всё) | disabled when auto-build is on |
| `org.eclipse.ui.project.buildProject` | Build the selected project (собрать проект) | needs a project selection |
| `org.eclipse.ui.project.cleanAction` | Clean (очистить) | ⛔ MODAL dialog — do not execute |
| `org.eclipse.ui.file.properties` | Properties dialog | ⛔ MODAL — do not execute |

## Examples

Wrap executable snippets in `{ ... }` with LOCAL names (persistent-session rule, see
`eclipse_command_workflow`).

Close all editors (verified). `closeAll` is DISABLED when no editors are open — guard with
`isEnabled()`:
```java
{
    var cmd = commandService.getCommand("org.eclipse.ui.file.closeAll");
    if (cmd.isEnabled()) {
        Object r = handlerService.executeCommand("org.eclipse.ui.file.closeAll", null);
        System.out.println("closeAll result=" + r);
    } else {
        System.out.println("closeAll disabled (no open editors)");
    }
}
return null;
```

Save all editors — command needs dirty editors; the API form is unconditional and preferred:
```java
{
    var win = workbench.getActiveWorkbenchWindow();
    var page = win != null ? win.getActivePage() : null;
    if (page != null) { System.out.println("saved=" + page.saveAllEditors(false)); }
}
return null;
```

## When the command is disabled — equivalent API calls

Selection-dependent commands (`refresh`, `buildProject`, `buildAll`) are usually disabled in a
programmatic context. Use the equivalent API (verified):

```java
{
    var project = workspaceRoot.getProject("MyProject");
    var npm = new org.eclipse.core.runtime.NullProgressMonitor();
    project.refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, npm);   // refresh
    project.build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD, npm); // build
    project.build(org.eclipse.core.resources.IncrementalProjectBuilder.CLEAN_BUILD, npm);       // clean
    System.out.println("done");
}
return null;
```

## Rules

- Discover exact ids/parameters with `GetCommands` first; check `isEnabled()` before executing.
- Never execute the modal commands from the table.
- Always end with `return <value>;`.
