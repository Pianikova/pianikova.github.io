# EDT JShell Manual Resources

This directory holds scenario-oriented guides served to the LLM via the
`JShellManual` MCP tool. Files here are loaded at runtime by
`MetadataManualCatalog` through `ManualResourceLoader`.

## Layout

```
manual/
  index.json              array of manual entry manifests
  README.md               this file
  _templates/             parameterized markdown templates (used via index.json `template` field)
    top-level-create.md   shared shell for simple `create_*` scenarios
  fragments/              reusable markdown chunks (included via {{>id}})
  types/                  TypeDescription snippets (one per primitive/ref type)
  workflows/
    create/               create_<entity> guides — only the complex ones (catalog, document, registers, type-description)
    edit/                 edit_<entity> guides
    delete/               delete_* guides
    composite/            multi-step or cross-entity guides
    enhanced/             "enhanced" creation guides with extra checks
    reference/            cross-cutting reference docs (overview, validation_errors, ...)
  configuration/          configuration project lifecycle (create/delete)
  bindings/               per-binding markdown (one .md per JShell binding name)
```

## index.json schema

Each entry is an object with:

| field      | required          | meaning                                                              |
|------------|-------------------|----------------------------------------------------------------------|
| id         | yes               | scenario id used by `JShellManual` (`scenario` request field)        |
| scope      | yes               | `edt` or `eclipse`                                                   |
| category   | yes               | `create`/`edit`/`delete`/`composite`/`enhanced`/`reference`/`configuration` |
| title      | yes               | short human title                                                    |
| summary    | yes               | one-line description shown in `available_scenarios`                  |
| guide      | one-of            | path to a standalone guide `.md`, relative to `manual/`              |
| template   | one-of            | template id under `_templates/<id>.md` (mutually exclusive with `guide`) |
| vars       | with template     | `{{var}}` substitution map for the template                          |
| bindings   | yes               | recommended JShell binding names                                     |
| keywords   | yes               | search keywords used by `JShellManualMcpTool` ranking                |

### Authoring a new simple create_* scenario

If your scenario fits the canonical `top-level-create` shape (just calls
`mdFactory.createXxx()` and adds to a `Configuration` collection), prefer the
declarative form — no new `.md` file needed:

```json
{
  "id": "create_widget",
  "scope": "edt",
  "category": "create",
  "title": "Create Widget",
  "summary": "Create Widget metadata.",
  "template": "top-level-create",
  "vars": {
    "title": "Widget",
    "typeName": "Widget",
    "variableName": "widget",
    "createMethod": "createWidget()",
    "collection": "getWidgets()",
    "setupBlock": "",
    "notes": "..."
  },
  "bindings": ["workspaceRoot", "modelManager", "mdFactory", "fqnGenerator"],
  "keywords": ["widget"]
}
```

For scenarios with extra structure (multiple sections, embedded type
descriptions, validation prose), write a standalone `.md` and use the `guide`
field instead.

## Authoring rules

- One scenario = one `.md` file. Don't bundle multiple scenarios into one file.
- Reuse cross-cutting content via `{{>fragment-id}}` (resolved against `fragments/`).
- Keep guide bodies focused on a single workflow; put preambles in fragments.
- Don't add a trailing blank line — the loader already strips it.
