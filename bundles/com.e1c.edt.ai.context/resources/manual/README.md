# EDT JShell Manual Resources

This directory holds scenario-oriented guides served to the LLM via the
`JShellManual` MCP tool. Files here are loaded at runtime by
`MetadataManualCatalog` through `ManualResourceLoader`.

## Layout

```
manual/
  index.json              array of manual entry manifests
  README.md               this file
  fragments/              reusable markdown chunks (included via {{>id}})
  types/                  TypeDescription snippets (one per primitive/ref type)
  workflows/
    create/               create_<entity> guides (one .md per entity)
    edit/                 edit_<entity> guides
    delete/               delete_* guides
    composite/            multi-step or cross-entity guides
    enhanced/             "enhanced" creation guides with extra checks
    overview.md           edt_overview master guide (composes fragments)
  configuration/          configuration project lifecycle (create/delete)
```

## index.json schema

Each entry is an object with:

| field      | required | meaning                                                          |
|------------|----------|------------------------------------------------------------------|
| id         | yes      | scenario id used by `JShellManual` (`scenario` request field)    |
| scope      | yes      | `edt` or `eclipse`                                               |
| title      | yes      | short human title                                                |
| summary    | yes      | one-line description shown in `available_scenarios`              |
| guide      | yes      | path to the guide markdown, relative to `manual/`                |
| bindings   | yes      | recommended JShell binding names                                 |
| keywords   | yes      | search keywords used by `JShellManualMcpTool` ranking            |
| vars       | no       | optional `{{var}}` substitution map for parameterized templates  |

## Authoring rules

- One scenario = one `.md` file. Don't bundle multiple scenarios into one file.
- Reuse cross-cutting content via `{{>fragment-id}}` (resolved against `fragments/`).
- Keep guide bodies focused on a single workflow; put preambles in fragments.
- Don't add a trailing blank line — the loader already strips it.
