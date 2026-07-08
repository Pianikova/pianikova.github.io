# AskUser Tool — Interactive User Choice

`AskUser` is a server-side tool (it is not executed by the EDT plugin like `JShell` or `GetProjects`). It asks the user one or more questions with predefined answer options and returns the user's selection.

Use it **only when user input is really necessary to make a decision** — for example, choosing a 1C platform version when the user did not specify one and several versions are available. Do not use it for confirmations of routine steps or questions you can answer yourself from the workspace context.

## Parameters

`questions` (required, array) — one or more question objects:

- `id` (required, string) — unique identifier of the question.
- `text` (required, string) — the question text, markdown supported.
- `label` (optional, string) — short chip label displayed next to the question.
- `options` (required, array) — options the user can select from:
  - `label` (required, string) — option text shown to the user.
  - `description` (optional, string) — explanation shown below the label.
- `multiSelect` (optional, boolean, default `false`) — allow selecting several options.
- `allowOther` (optional, boolean, default `true`) — show a free-form text input in addition to the options.

## Example

```json
{
  "questions": [
    {
      "id": "platform_version",
      "text": "Какую версию платформы 1С использовать для новой конфигурации?",
      "label": "Версия платформы",
      "multiSelect": false,
      "allowOther": true,
      "options": [
        {
          "label": "8.3.27",
          "description": "Последняя поддерживаемая версия (рекомендуется)"
        },
        {
          "label": "8.3.26"
        },
        {
          "label": "8.3.24"
        }
      ]
    }
  ]
}
```

The tool returns the user's selections; use them to guide the following steps.

## Availability (verified experimentally)

- `AskUser` is available **only in the interactive EDT chat**, where the chat UI renders the question with clickable options and returns the user's selection as the tool result.
- In headless/agent runs (automated scenarios) the server does not add `AskUser` to the tool list. Check your tool list first: if `AskUser` is not there, do not attempt to call it — a call to a tool the IDE does not know is answered with `Unknown tool call: <name>` and wastes a round trip (this is what happens to other server-side tool calls such as `TodoWrite` in this mode).

## Rules

- Use `AskUser` only if it is present in your tool list.
- Put the recommended option first and mark it in its `description`.
- Keep the option list short (3–5 items). With `allowOther: true` the user can always type a custom value.
- Ask before starting the work that depends on the answer, not after.
- Do not repeat the question if the user already answered it or already gave the value in the original request.
- If `AskUser` is not available and the decision has a safe default (e.g. the latest available platform version), proceed with the default and explicitly report in the final answer which value was chosen and what the alternatives were. Only for genuinely blocking questions with no safe default, ask as plain text (numbered options) and finish the turn.
