# Discover and Execute IDE Commands — Workflow

Call `JShell` with `scope: "eclipse"` for this workflow. The command itself is executed by JAVA
CODE via the `commandService` / `handlerService` bindings — there is no separate "execute command"
tool.

## The workflow

1. **Discover the command id** — never guess ids:
   - `GetCommandCategories` — list categories (returns `category_id`).
   - `GetCommands` with a `category_id` — commands of the category: exact `command_id`, declared
     `parameters` (id / name / optional / value constraints), return type, hotkey.
2. **Load the matching scenario manual** (`JShellManual`): a themed command reference may exist
   (`eclipse_commands_files_build`, `eclipse_commands_edit_navigate`, `eclipse_commands_run_debug`,
   or in 1C:EDT `edt_commands_*`). For invocation patterns see `execute_command`.
3. **Execute via JShell** (Java code, see patterns below).
4. **Verify the result** and report it; on `NotEnabledException`/`NotHandledException` fix the
   context (activate the right editor/view, set a selection) and retry.

## Invocation patterns (Java code in JShell)

⚠️ **Always wrap the executable snippet in a `{ ... }` block** and use local variable names, then
`return` after the block. The JShell session is PERSISTENT: a bare top-level variable (e.g.
`Object r = handlerService.executeCommand(...)`) is remembered, and re-declaring the same name in a
later call RE-RUNS the earlier initializer — replaying its side effect (a second `closeAll` etc.)
and often throwing `NotEnabledException`. A `{ ... }` block keeps names local and side-effect-free
across calls.

Parameterless command:
```java
{
    Object result = handlerService.executeCommand("the.command.id", null);
    System.out.println("result=" + result);
}
return null;
```

Command with declared string parameters:
```java
{
    var command = commandService.getCommand("the.command.id");
    var params = new java.util.HashMap<String, String>();
    params.put("declaredParamId", "value");
    var pc = ParameterizedCommand.generateCommand(command, params);
    Object result = handlerService.executeCommand(pc, null);
    System.out.println("result=" + result);
}
return null;
```

Arbitrary typed arguments / explicit context (full details in manual `execute_command`):
```java
{
    var command = commandService.getCommand("the.command.id");
    var base = handlerService.getCurrentState();
    var ctx = new EvaluationContext(base, base.getDefaultVariable());
    ctx.addVariable("myArgName", someTypedObject);   // any Object, not only String
    var event = new ExecutionEvent(command, java.util.Collections.emptyMap(), null, ctx);
    System.out.println("result=" + command.executeWithChecks(event));
}
return null;
```

Pre-check a command before executing:
```java
{
    var cmd = commandService.getCommand("the.command.id");
    System.out.println("defined=" + cmd.isDefined() + " enabled=" + cmd.isEnabled());
}
return null;
```

## Hard rules

- ✅ Wrap every executable snippet in a `{ ... }` block with LOCAL variable names and put `return`
  after the block. Never leave a side-effecting top-level variable (see the warning above) — reused
  names replay earlier commands in the persistent session.
- ⛔ NEVER execute commands that open MODAL dialogs/wizards — they block the UI thread. Known
  modal commands include `org.eclipse.ui.window.preferences`, `org.eclipse.ui.file.properties`,
  `org.eclipse.ui.navigate.openResource`, `org.eclipse.ui.project.cleanAction`, and most
  export/import wizard commands. If unsure whether a command opens UI, tell the user instead of
  executing it.
- Many commands are enabled only with the right active editor/selection; a `false` from
  `isEnabled()` in a programmatic context is common — see "Selection-dependent commands" in
  manual `execute_command` (set the live selection or use the equivalent API).
- Code already runs on the UI thread — do NOT add `Display.syncExec`.
- Always end with `return <value>;` (use `return null;` when there is nothing to return).
