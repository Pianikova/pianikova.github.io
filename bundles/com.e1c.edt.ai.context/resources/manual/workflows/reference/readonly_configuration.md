# Read-Only Configuration (Full Vendor Support)

A 1C configuration project can be on **full vendor support** (полная поддержка
поставщика): its root `Configuration.mdo` has
`<objectBelonging>Adopted</objectBelonging>`. Such a configuration MUST NOT be
modified in any way until the user enables editing (switches the support mode so
`objectBelonging` becomes `Native`).

## How to detect

1. Preferred: call `GetProjects` — the project has `"read_only": true`, and
   `details."1C project details".object_belonging` = `"Adopted"`.
2. From JShell (read-only check, safe):

```java
import org.eclipse.core.resources.IProject;
import com._1c.g5.v8.dt.core.platform.IConfigurationProject;

IProject project = workspaceRoot.getProject("MyProject");
var v8 = projectManager.getProject(project);
if (v8 instanceof IConfigurationProject) {
    // ADOPTED => read-only (full vendor support)
    System.out.println(((IConfigurationProject)v8).getConfiguration().getObjectBelonging());
}
```

## Rules

- If `read_only: true`: do NOT create, edit, or delete metadata objects, forms,
  templates, modules, or any project files. The `Write`/`Edit`/`Delete` tools
  refuse such projects; do NOT bypass the refusal via JShell BM transactions,
  EDT commands, or git — modifying an adopted configuration breaks vendor
  support.
- Read-only operations are allowed: `Read`, `Find`, `GetMarkers`, JShell reads,
  analysis, reports, validation.
- Only the root Configuration object carries `objectBelonging`; child `.mdo`
  files do not — always check at the project level via `GetProjects`.
- Extension projects (`type: "Extension"`) are editable even though their own
  `Configuration.mdo` also says `Adopted` — this rule applies only to regular
  configuration projects. When appropriate, suggest implementing the change in
  an extension of the read-only configuration.
- When the user asks for a change in a read-only project: explain that the
  configuration is on full vendor support and that they must first enable
  editing (change the support settings so the configuration becomes `Native` —
  «снять с полной поддержки» / «включить возможность изменения»), or target an
  extension project. Do not attempt the change yourself and do not retry.
