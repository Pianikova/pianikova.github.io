# Dev Autopilot — feedback-loop testing of the 1C assistant

A file-driven harness that lets **any external AI agent** test and iteratively improve the
assistant on real 1C tasks — sending prompts to the real assistant, reading a structured
transcript of what it did, and re-running — **without the EDT UI and without restarting EDT**.

It is implemented by `DevAutopilot` / `DevToolCallRecorder` in bundle `com.e1c.edt.ai` and started
from `BaseActivator`.

> **1C metadata is edited exclusively through the `1C_EditMetadata` tool.** It performs guarded,
> declarative operations (create/modify/remove objects, children, forms, templates, and whole
> configurations) through native EDT APIs. Do **not** edit metadata by hand-writing `.mdo`/`.form`
> XML, and do **not** edit metadata through JShell. JShell remains available only as a
> general-purpose fallback for operations that are not 1C-entity editing.

---

## 1. Enabling it

1. **Experimental mode must be ON.** The harness starts only when `ISettings.isExperimental()`
   returns `true`. This flag is read **once at bundle activation** — toggling it requires an EDT
   restart.
2. **(Optional) channel directory** via VM arg in the EDT launch / `.ini`:
   ```
   -Dai.dev.channel.dir=<some-dir>
   ```
   If unset, the default is `<workspace>/.metadata/ai-dev` (the Eclipse workspace/instance
   location), e.g. `D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev`. There is a
   `java.io.tmpdir` fallback if the workspace location is unavailable.
3. On start, the harness logs the resolved absolute channel path to the EDT log, line
   `[dev-autopilot] started. Channel dir: …`. Use it to discover where to read/write.

---

## 2. Channel layout

```
<channelDir>/
  inbox/        ← you DROP request files here:  <id>.json
  processing/   ← harness moves a request here while running it (lock)
  outbox/       ← harness WRITES the transcript here: <id>.json
```

The harness polls `inbox/` every second, processes requests **serially** (one assistant turn at a
time), and writes the transcript to `outbox/<id>.json` (same `<id>` as the request file name).
Pick a sortable `<id>` (zero-padded counter or timestamp) — requests are processed in filename order.

---

## 3. Request schema (`inbox/<id>.json`)

```json
{
  "prompt": "создай справочник Контрагенты с реквизитом ИНН",
  "project": "<ProjectName>"
}
```

| field             | required | meaning                                                                                                                                                             |
|-------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `prompt`          | yes      | user message sent to the assistant (a new conversation is forced each turn)                                                                                          |
| `project`         | yes      | 1C project name to target; it must already exist in the workspace or the turn fails before it starts. Pass the real project name from your environment; never hard-code README examples or translated guesses. For a configuration-level task (`createConfiguration`), point this at any existing project as the conversation context — the name of the configuration to create goes into the tool arguments, not here. |
| `preamble`        | no       | agent preamble prepended to the prompt: omit/`null` → built-in default preamble (drives the model to complete the task with tools); `""` → bare prompt, no preamble; any string → that preamble |
| `max_tool_rounds` | no       | cap on tool-execution rounds for the turn; omit → harness default (200). Multi-step tasks (object + attributes + form/template) plus self-correction need far more than the chat default of 10 |

Keep normal request files to `{ "prompt": "...", "project": "..." }` plus only the rare overrides
you are intentionally testing (`preamble`, `max_tool_rounds`). For routing diagnostics only you may
add `"skill": "raw"`; do not make that the normal format.

> **Why the preamble.** The dev/helper conversation (skill `custom`) lacks the interactive chat's
> agent system prompt, so a bare prompt makes the model gather context and stop. The default
> preamble restores agentic multi-step execution and points metadata work at `1C_EditMetadata`.

---

## 4. Transcript schema (`outbox/<id>.json`)

```json
{
  "id": "001",
  "prompt": "…",
  "project": "<ProjectName>",
  "conversation_id": "…",
  "reply_to_message_uuid": "…",
  "final_text": "assistant's final message to the user",
  "reasoning": "the model's reasoning_content for its final message (if the server returns it)",
  "assistant_message_count": 3,
  "auto_continue_count": 0,
  "tool_calls": [
    { "tool": "1c_editmetadata", "arguments": "{…}", "result": "{…}", "error": null },
    { "tool": "getmarkers",      "arguments": "{…}", "result": "{…}", "error": null }
  ],
  "tool_call_count": 5,
  "tool_error_count": 0,
  "has_tool_failures": false,
  "stalled": false,
  "tools_count": 25,
  "tools_definition_chars": 76000,
  "error": null,
  "duration_ms": 15230
}
```

- `tool_calls` — ordered list of MCP tool calls the model made this turn. For metadata tasks the key
  calls are `1c_editmetadata` (each returns `success`, `resource_path`, `marker_path`, `warnings`)
  and `getmarkers`.
- `conversation_id` / `reply_to_message_uuid` — the server-side conversation, to cross-reference
  server logs (each turn forces a fresh conversation).
- `reasoning` — the final assistant message's `reasoning_content` when the server provides it; the
  best signal for **why** the model stalled or chose a path.
- `assistant_message_count` — finished assistant messages; a stall is typically one empty message.
- `auto_continue_count` — automatic "continue with tools" nudges the harness sent inside the same
  request when the model wrote "создам/начну" but stopped before acting.
- `stalled` — `true` when no mutating tool ran (the model gathered context and stopped).
- `tool_error_count`, `has_tool_failures` — aggregate failure flags. Treat `has_tool_failures:true`
  as a failed/dirty run even if the final assistant text says success.
- `tools_count` / `tools_definition_chars` — size of the fixed tool-definition prelude sent every turn.
- `error` — set only on harness-level failure; per-tool errors appear inside `tool_calls[].error` /
  `tool_calls[].result`.
- Turn timeout is 300s.

---

## 5. The feedback loop

```
1. write  inbox/<id>.json  { prompt, project }
2. wait for outbox/<id>.json to appear
3. read the transcript; evaluate against the rubric (§6)
4. if it fails → adjust the tool (its operation set / description / validation) or the request,
   then go to 1 with a new id
5. repeat until pass; keep a short changelog of what you changed and why
```

Because requests are serial, only submit the next request after the previous transcript appears.

---

## 6. Evaluation rubric

Judge each transcript on:

1. **Right tool** — every 1C metadata mutation goes through `1C_EditMetadata` (never hand-written
   `.mdo`/`.form`/`.dcs`/`.mxl` XML, never JShell). Whole-configuration create/delete goes through the
   tool's `createConfiguration` / `removeConfiguration` operations.
2. **Right operation** — e.g. an attribute on a tabular section uses `addTabularSectionAttribute`
   (`object_name = Type.Object.Section`), not `addObjectAttribute`; a nested subordinate object uses
   `addSubordinateObject`.
3. **Success, not just words** — each `1c_editmetadata` result has `success: true`, and
   `has_tool_failures` is `false`. The tool already verifies physical persistence; the model must not
   re-discover paths with `Glob`/`Read` when `marker_path` is returned.
4. **Markers clean** — a follow-up `GetMarkers` on the returned `marker_path` is clean, or only shows
   expected validation markers for an intentionally incomplete business setup (distinguish a CRUD
   failure from a semantic-configuration marker). For a created configuration, verify the Eclipse
   project name matches the requested name exactly.
5. **No punting** — the assistant must not finish with "сделайте вручную" for something the tool covers.
6. **Low friction** — count `tool_calls`, `auto_continue_count`, `tool_error_count`, duration.
   Repeated tool errors or auto-continues indicate the operation set or descriptions need work.

Suggested quality bands:

| Result                                                         | Interpretation                                        |
|----------------------------------------------------------------|-------------------------------------------------------|
| `success:true`, artifact exists, markers clean                 | Good baseline                                         |
| `has_tool_failures:true` or repeated tool errors               | Failed/dirty baseline; fix the tool before relying on it |
| Empty `final_text` or read-only-only tools                     | Preamble / agentic-flow problem                       |
| Success text but missing file / dirty markers                  | Failure; strengthen the tool's persistence/verification |

---

## 7. Limitations & safety

- Each turn is a **real LLM call** (tokens + network); the EDT instance must have a valid token
  configured. Cap the number of iterations per scenario.
- Requests run **serially**; do not expect parallelism.
- Mutating requests change the test project (create catalogs/forms/templates/…). Clean up stray
  artifacts between runs, or target a throwaway configuration project (`createConfiguration` /
  `removeConfiguration`).
- The harness writes an informational `[dev-autopilot] started …` line to the EDT log on startup.

---

## 8. Improving metadata behaviour

Metadata capability lives in the `1C_EditMetadata` tool (bundle `com.e1c.edt.ai.context`,
package `…tools.metadata`), not in Markdown scenario manuals. When a transcript shows the model
failing a metadata task, fix it there:

- **Missing capability** — add or extend an operation in `MetadataOperationRegistry` +
  `MetadataMutationService` (and the object registry `MetadataObjectTypeRegistry` for new types).
  Operations on the content of an existing form (attributes, commands, items) live in
  `FormMutationService`; property writing shared by all of them lives in `MetadataPropertyWriter`.
- **Model chose the wrong operation / parameters** — improve the operation `description` and
  `example` in `MetadataOperationRegistry`, or the tool description in `EditMetadataMcpTool`.
- **Validation too strict/loose** — adjust `MetadataTypeService` (type/length/precision handling).

Changes here are Java: they need a recompile (the running Eclipse rebuilds `bin/` on save) and an
EDT restart to reload the bundle. Re-verify through the loop above.
