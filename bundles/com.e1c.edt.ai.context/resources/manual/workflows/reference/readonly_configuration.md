# Read-Only Configuration (Full Vendor Support)

A 1C configuration project can be on **full vendor support** (полная поддержка
поставщика): the configuration was delivered by a vendor
(`src/Configuration/ParentConfigurations.bin` is present) and editing is not
enabled. Such a configuration MUST NOT be modified in any way until the user
enables editing in the configuration support settings.

## How to detect

Call `GetProjects` — a read-only project has `"read_only": true`. This flag is
computed the same way EDT editors decide editability (vendor-support rules),
so trust it as the single source of truth. Do not try to infer support state
from project files yourself.

## Rules

- If `read_only: true`: do NOT create, edit, or delete metadata objects, forms,
  templates, modules, or any project files. The `Write`/`Edit`/`Delete` tools
  refuse such projects; do NOT bypass the refusal via JShell BM transactions,
  EDT commands, or git — modifying a supported configuration breaks vendor
  support.
- Read-only operations are allowed: `Read`, `Find`, `GetMarkers`, JShell reads,
  analysis, reports, validation.
- The support state belongs to the whole configuration — always check at the
  project level via `GetProjects` before any mutation workflow.
- Extension projects (`type: "Extension"`) are editable — this rule applies
  only to regular configuration projects. When appropriate, suggest
  implementing the change in an extension of the read-only configuration.
- When the user asks for a change in a read-only project: explain that the
  configuration is on full vendor support and that they must first enable
  editing (change the configuration support settings — «включить возможность
  изменения» / «снять с полной поддержки»), or target an extension project.
  Do not attempt the change yourself and do not retry.
