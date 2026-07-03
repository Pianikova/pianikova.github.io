# Edit, Navigation and View Commands

Call `JShell` with `scope: "eclipse"`. Execute commands by Java code via `handlerService`
(patterns: manual `eclipse_command_workflow`).

## Which command is for what

| Command id | Purpose (зачем) | Notes |
|---|---|---|
| `org.eclipse.ui.edit.undo` | Undo (отменить) | needs an active editor with history |
| `org.eclipse.ui.edit.redo` | Redo (повторить) | needs undo history |
| `org.eclipse.ui.edit.copy` | Copy (копировать) | needs a selection in the active part |
| `org.eclipse.ui.edit.paste` | Paste (вставить) | needs an editable active part |
| `org.eclipse.ui.edit.delete` | Delete selection (удалить) | selection-dependent |
| `org.eclipse.ui.edit.rename` | Rename selection (переименовать) | selection-dependent; may open inline editor |
| `org.eclipse.ui.edit.findReplace` | Find/Replace (найти/заменить) | opens a dialog — prefer telling the user |
| `org.eclipse.ui.views.showView` | Open a view (открыть представление) | param `org.eclipse.ui.views.showView.viewId`; verified |
| `org.eclipse.ui.navigate.next` / `.previous` | Next/previous annotation | editor-dependent |
| `org.eclipse.ui.navigate.openResource` | Open Resource dialog | ⛔ MODAL — use `IDE.openEditor` API instead |
| `org.eclipse.ui.window.preferences` | Preferences dialog | ⛔ MODAL — do not execute |

## Examples

Wrap executable snippets in `{ ... }` with LOCAL names (persistent-session rule, see
`eclipse_command_workflow`).

Open a view by id (verified — Pattern 2, string parameter):
```java
{
    var command = commandService.getCommand("org.eclipse.ui.views.showView");
    var params = new java.util.HashMap<String, String>();
    params.put("org.eclipse.ui.views.showView.viewId", "org.eclipse.pde.runtime.LogView");
    var pc = ParameterizedCommand.generateCommand(command, params);
    Object r = handlerService.executeCommand(pc, null);
    System.out.println("showView result=" + r);
}
return null;
```

Open a specific FILE in an editor — there is no non-modal command; use the API (verified):
```java
{
    var project = workspaceRoot.getProject("MyProject");
    org.eclipse.core.resources.IFile file = project.getFile(".project");
    var win = workbench.getActiveWorkbenchWindow();
    var page = win != null ? win.getActivePage() : null;
    var editor = org.eclipse.ui.ide.IDE.openEditor(page, file);
    System.out.println("opened: " + (editor != null ? editor.getTitle() : "null"));
}
return null;
```

Undo in the active editor (only when the user asked and an editor is active):
```java
{
    var cmd = commandService.getCommand("org.eclipse.ui.edit.undo");
    if (cmd.isEnabled()) { handlerService.executeCommand("org.eclipse.ui.edit.undo", null); System.out.println("undone"); }
    else { System.out.println("undo is disabled (no active editor/history)"); }
}
return null;
```

## Rules

- Edit commands act on the ACTIVE editor/selection — verify the context first (`workbench`, manual
  `active_workbench`), never run them blindly.
- Never execute the modal commands from the table.
- Always end with `return <value>;`.
