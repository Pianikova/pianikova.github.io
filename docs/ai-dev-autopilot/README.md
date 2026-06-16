# Dev Autopilot — feedback-loop testing of metadata / form / template scenarios

A file-driven harness that lets **any external AI agent** test and iteratively improve the 1C
metadata/form/template JShell scenarios — sending prompts to the real assistant, reading a
structured transcript of what it did, editing the scenario manuals, and re-running — **without
the EDT UI, without recompilation, and without restarting EDT**.

It is implemented by `DevAutopilot` / `DevToolCallRecorder` in bundle `com.e1c.edt.ai` and started
from `BaseActivator`.

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
3. On start, the harness logs the resolved absolute channel path to the EDT log
   (`…\EDT_Plugin_log\trace.log`), line `[dev-autopilot] started. Channel dir: …`. Use it to
   discover where to read/write if you did not set the property.

A **one-time recompile** of the plugin is required to get the harness itself; after that, the
loop below needs no further builds.

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

---

## 3. Request schema (`inbox/<id>.json`)

```json
{
  "prompt": "сделай форму элемента для справочника Номенклатура",
  "project": "<ProjectName>",
  "is_chat": true,
  "skill": "custom"
}
```

| field             | required | meaning                                                                                                                                                                                                             |
|-------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `prompt`          | yes      | user message sent to the assistant (a new conversation is forced each turn)                                                                                                                                         |
| `project`         | no       | 1C project name to target; if omitted/not found, a project-less context is used. In tests, pass the real project name from your environment; never hard-code README examples or translated guesses. |
| `is_chat`         | no       | override the conversation `is_chat` flag; omit to use the default (`false`)                                                                                                                                         |
| `skill`           | no       | override the conversation skill (`custom`/`raw`/`system`/`docstring`/`explain`/`review`/`modify`); omit to use the default (`custom`)                                                                               |
| `preamble`        | no       | agent preamble prepended to the prompt: omit/`null` → built-in default preamble (drives the model to run JShellManual→JShell→GetMarkers to completion); `""` → bare prompt, no preamble; any string → that preamble |
| `max_tool_rounds` | no       | cap on tool-execution rounds for the turn; omit → harness default (30). Multi-artifact tasks (object + form/template + content) plus self-correction need more than the chat default of 10                          |

> **Why the preamble.** The dev/helper conversation (skill `custom`) lacks the interactive chat's
> agent system prompt, so a bare prompt makes the model gather context and stop. The default
> preamble restores agentic multi-step execution. Send `"preamble": ""` to test the bare prompt.

Pick a sortable `<id>` (e.g. a zero-padded counter or timestamp) — requests are processed in
filename order.

## 4. Transcript schema (`outbox/<id>.json`)

```json
{
  "id": "001",
  "prompt": "…",
  "project": "<ProjectName>",
  "final_text": "assistant's final message to the user",
  "tool_calls": [
    { "tool": "jshellmanual", "arguments": "{…}", "result": "{…}", "error": null },
    { "tool": "jshell",       "arguments": "{…code…}", "result": "{\"std_out\":…,\"compilation_errors\":[…],\"runtime_errors\":[…]}", "error": null }
  ],
  "error": null,
  "duration_ms": 41230
}
```

- `tool_calls` is the ordered list of MCP tool calls the model made this turn (this is where you
  see which manual scenario was fetched, the JShell code that ran, and any
  `compilation_errors` / `runtime_errors`).
- `error` is set only on harness-level failure (bad request, timeout, exception); per-tool errors
  appear inside `tool_calls[].error` / `tool_calls[].result`.
- Turn timeout is 300s.

---

## 5. The feedback loop (what an external agent does)

```
1. write  inbox/<id>.json  { prompt, project }
2. wait for outbox/<id>.json to appear
3. read the transcript; evaluate against the rubric (§7)
4. if it fails → edit the scenario manuals (§6), then go to 1 with a new id
   (no recompile, no restart — manual edits are picked up live)
5. repeat until pass; keep a short changelog of what you changed and why
```

Because requests are serial, only submit the next request after the previous transcript appears.

---

## 6. Hot-editing scenarios (no recompile, no restart)

The scenarios live in
`bundles/com.e1c.edt.ai.context/resources/manual/`:

- **Guide bodies** — `workflows/**/<scenario>.md`, `bindings/<binding>.md`,
  `_templates/<template>.md`. Read **lazily on every use** → edits are live immediately.
- **Index / catalog** — `index.json` (scenario ids, titles, summaries, `keywords`, `bindings`,
  `notes`, template `vars`). Hot-reloaded by `MetadataManualCatalog` when the file's modification
  time changes → edits are picked up on the next manual lookup, **no restart**.

So: edit `.md` / `index.json`, then submit the next prompt — the assistant sees the new content.

### Live resource caveat: `resources/manual` vs `bin/manual`

`resources/manual` is the source-of-truth that should be committed. In a running Eclipse/EDT
workspace, however, the active bundle may read a copied resource tree under
`bundles/com.e1c.edt.ai.context/bin/manual/`. If a transcript still shows old `guide_markdown`
after you edited `resources/manual`, check the live copy:

```
bundles/com.e1c.edt.ai.context/bin/manual/
```

For the current no-restart test loop, copy the changed `.md` / `index.json` files from
`resources/manual` to the matching `bin/manual` path, then submit the next request. Keep the real
change in `resources/manual`; `bin/manual` is only a runtime mirror for the already-running EDT.

### If `index.json` reloads but the guide body is stale

Sometimes the catalog metadata hot-reloads (new `summary`, `keywords`, `bindings` appear), but the
Markdown guide body returned by `JShellManual` is still the old content. Verify this with a
non-mutating request such as:

```json
{
  "prompt": "Вызови JShellManual для сценария edit_form и напиши первую строку guide_markdown. Ничего в проекте не меняй.",
  "project": "<ProjectName>",
  "is_chat": true,
  "skill": "custom",
  "max_tool_rounds": 5
}
```

If the guide body is stale, create a new guide file name (for example
`workflows/edit/edit_form_regenerate.md`) and change the scenario's `guide` field in `index.json`
to the new path. Then sync `index.json` and the new guide to `bin/manual` if needed. This forces the
running catalog to read the fresh body without a restart.

> Editing Java (bindings in `MetadataBindingProvider`, the harness itself) **does** require a
> recompile + restart. Iterate on markdown/JSON, not Java, inside the loop.

---

## 7. Evaluation rubric (metadata / forms / templates)

Judge each transcript on:

1. **Right scenario chosen** — e.g. an object-owned form ⇒ `create_object_form` (not
   `create_common_form`); a report layout/СКД ⇒ `create_object_template` + `fill_template_content`
   (not hand-written `.dcs`/`.mxl`/`.mdo` XML).
2. **Correct API path** — forms: `generateForm(...)` with a **non-null `columnCount`** for
   OBJECT/FOLDER/CONSTANTS/RECORD/REPORT, then `setMdForm` + `attachTopObject(…BASIC_FORM__FORM…)`.
   templates: `mdFactory.createTemplate()` + `dcsFactory`/`moxelFactory` + `template.setTemplate(...)`.
3. **No `compilation_errors` / `runtime_errors`** in the final successful `jshell` call.
4. **Artifact really produced** — `Form.form` / `Template.dcs` / `Template.mxl` exists, and a
   follow-up `getmarkers` (`marker_type:"1c"`) on the owner `.mdo` (and the artifact) is clean.
5. **No punting** — the assistant must not finish with "сделайте вручную" / hand-write raw XML
   for something a scenario covers.
6. **Low-friction execution** — a scenario that eventually succeeds after many wrong JShell
   attempts still needs improvement. Count `tool_calls`, JShell calls, `compilation_errors`,
   `runtime_errors`, and duration. Repeated compile/runtime failures mean the guide is still too
   vague or contains a misleading example.

When a failure mode is found, fix it in the scenario (clearer hard rule, correct factory package,
required argument, cross-link, keywords) and re-run. Each fix should be small and verifiable.

Suggested quality bands:

| Result                                                    | Interpretation                                                                        |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------|
| Pass with no final errors, artifact exists, markers clean | Good baseline                                                                         |
| Pass but with repeated compile/runtime attempts           | Improve the guide anyway; remove misleading snippets and move exact signatures upward |
| Empty `final_text` or read-only-only tools                | Preamble/agentic flow or guide first-action problem                                   |
| Success text but missing file/markers dirty               | Failure; strengthen post-check and completeness guard                                 |

---

## 8. Limitations & safety

- Each turn is a **real LLM call** (tokens + network); the EDT instance must have a valid token
  configured. Cap the number of iterations per scenario.
- Requests run **serially**; do not expect parallelism.
- Mutating scenarios change the test project (create catalogs/forms/templates). Clean up stray
  artifacts between runs, or target a throwaway configuration project.
- Repeated tests can leave partially-created/broken artifacts, such as a form metadata object
  without `Form.form`, a template metadata object without `Template.dcs`/`Template.mxl`, or a child
  attribute without `TypeDescription`. A good scenario should repair such states idempotently.
- The harness writes an informational `[dev-autopilot] started …` line to the EDT log on startup.

---

# Part II — Improving the 1C scenarios (for a fresh external LLM)

This part is self-contained: read it and you can improve the scenario manuals for **all** 1C
entities (metadata, forms, templates) and the workflows, from a brand-new session, using the loop
in Part I. You edit Markdown/JSON only — no Java, no recompile, no restart.

## 9. The scenario manual system — what you edit

Everything lives under
`bundles/com.e1c.edt.ai.context/resources/manual/`:

```
manual/
  index.json                 catalog of all scenarios (the model finds scenarios from here)
  workflows/
    create/   <scenario>.md   create a metadata object / form / template / content
    edit/     <scenario>.md   modify an existing object
    delete/   <scenario>.md   delete safely
    composite/<scenario>.md   multi-object flows (registrars, journals, sequences, scaffolding)
    reference/<scenario>.md   API cards & rules the model consults (not actions)
    enhanced/ <scenario>.md   richer create variants
  configuration/<scenario>.md project-level create/delete
  bindings/  <binding>.md     per-binding docs (the JShell variables the code uses)
  _templates/<name>.md        reusable guide bodies filled from index.json `vars`
```

**Two ways a scenario provides its guide text** (set in its `index.json` entry):
- `"guide": "workflows/create/x.md"` — a standalone Markdown file (most scenarios).
- `"template": "top-level-create"` + `"vars": {...}` — reuse `_templates/<template>.md`, substituting
  `${var}` placeholders from `vars` (used for simple top-level objects).

**Hot-reload:** guide `.md` bodies are read on every use (edits are live instantly); `index.json` is
re-read when its modification time changes. So: edit → submit the next prompt → the model sees it.
Editing the Java `bindings/*.md` text is live too; editing actual Java is NOT (needs recompile).

### JShell bindings available to scenario code (pre-bound session variables)

`workspaceRoot`, `workbench`, `projectManager`, `modelManager`, `mdFactory`, `modelFactory`,
`fqnGenerator`, `resourceLookup`, `formGenerator`, `formFieldGenerator`, `formFactory`,
`editingLanguageManager`, `dcsFactory`, `moxelFactory`. Each has a card in `bindings/`. A scenario's
`index.json` `bindings` array lists which ones it needs (shown to the model).

### Reference cards (consult, don't duplicate)

`metadata_api_core`, `metadata_api_data`, `metadata_api_registers`, `metadata_api_exchange_services`,
`metadata_api_processes_misc`, `metadata_object_coverage`, `jshell_edt_canonical_imports`,
`safe_uuid_assignment`, `typedescription_best_practices`, `child_elements_uuid_importance`,
`check_1c_markers_after_crud`, `validation_errors`, `edt_validation_traps`, `edt_overview`. When a rule
is general, put it in/realign with a reference card and link to it rather than copy-pasting.

### Maintenance discipline for external LLMs

When using this README as context for another LLM, instruct it to work in small recursive passes:

1. Pick one narrow surface (for example forms, templates, registers, service objects, or one child
   object family).
2. Run one realistic short Russian prompt.
3. Diagnose the transcript and disk state.
4. Apply the smallest Markdown/JSON fix.
5. Re-run with a new id and a fresh prompt/name.
6. Record a changelog entry before moving to the next surface.

Do not let the LLM "improve everything" by mass-editing manuals without tests. The manuals are
most valuable when each rule is tied to an observed transcript failure.

## 10. 1C entity reference (the universe to cover)

This is the full set of 1C metadata entities you may be asked to work with. It is a **neutral
reference of what exists in 1C** — it deliberately does **not** say which entities already have a
scenario, so you can test/build any of them from scratch without bias. To see which scenarios
currently exist, read `index.json` yourself (ids + categories + keywords); to confirm an entity and
its factory/structure, read the model
`C:\Projects\dt\dt-core\bundles\com._1c.g5.v8.dt.metadata\model\MdClass.xcore`.

**Top-level metadata objects** (each creatable via `mdFactory.create<Type>()`, RU name in parens):

- **Reference data:** Catalog (Справочник), Document (Документ), DocumentJournal (ЖурналДокументов),
  DocumentNumerator (Нумератор), Sequence (Последовательность), Enum (Перечисление),
  ChartOfCharacteristicTypes (ПланВидовХарактеристик), ChartOfAccounts (ПланСчетов),
  ChartOfCalculationTypes (ПланВидовРасчёта).
- **Registers:** InformationRegister (РегистрСведений), AccumulationRegister (РегистрНакопления),
  AccountingRegister (РегистрБухгалтерии), CalculationRegister (РегистрРасчёта).
- **Business logic:** BusinessProcess (Бизнес-процесс), Task (Задача), Report (Отчёт),
  ExternalReport (ВнешнийОтчёт), DataProcessor (Обработка), ExternalDataProcessor (ВнешняяОбработка).
- **Common / config objects:** Constant (Константа), CommonModule (ОбщийМодуль),
  CommonAttribute (ОбщийРеквизит), CommonForm (ОбщаяФорма), CommonCommand (ОбщаяКоманда),
  CommandGroup (ГруппаКоманд), CommonTemplate (ОбщийМакет), CommonPicture (ОбщаяКартинка),
  Role (Роль), Subsystem (Подсистема), Style (Стиль), StyleItem (ЭлементСтиля), Language (Язык),
  SessionParameter (ПараметрСеанса), FunctionalOption (ФункциональнаяОпция),
  FunctionalOptionsParameter (ПараметрФункциональныхОпций), DefinedType (ОпределяемыйТип),
  SettingsStorage (ХранилищеНастроек), ScheduledJob (РегламентноеЗадание),
  EventSubscription (ПодпискаНаСобытие), FilterCriterion (КритерийОтбора), Interface (Интерфейс),
  PaletteColor (ЦветПалитры).
- **Services / integration:** WebService (Web-сервис), HTTPService (HTTP-сервис),
  WSReference (WS-ссылка), IntegrationService (СервисИнтеграции), XDTOPackage (XDTO-пакет),
  ExternalDataSource (ВнешнийИсточникДанных), Bot (Бот), WebSocketClient (WebSocketКлиент).
- **Exchange:** ExchangePlan (ПланОбмена).
- **Project level:** Configuration (Конфигурация).

**Child / nested objects** (created inside their owner, not as top-level objects): attributes
(Реквизиты), tabular sections (Табличные части) and their attributes, forms
(CatalogForm/DocumentForm/… — формы объекта), templates (Макеты) and their content, commands
(Команды), predefined items (Предопределённые), `EnumValue` (ЗначениеПеречисления),
`Recalculation` (Перерасчёт — у РегистраРасчёта), `AddressingAttribute` (РеквизитАдресации — у Задачи),
`AccountingFlag`/`ExtDimensionAccountingFlag` (Признаки учёта / субконто — у ПланаСчетов),
register dimensions/resources (Измерения/Ресурсы), web-service `Operation`/`Parameter`,
HTTP-service `URLTemplate`/`Method`, and external-data-source content
(`Table`/`Cube`/`DimensionTable`/`Field`/`Dimension`/`Resource`/`Function`).

Note: `ExternalReport`/`ExternalDataProcessor` are stand-alone artifacts (separate files), not part of
a configuration's object tree.

## 11. Realistic user-prompt bank (test inputs)

Real 1C users write **short Russian prompts with little/no technical detail**. Test scenarios with
prompts like these (drop them into `inbox/<id>.json` as the `prompt`). Vague phrasing is intentional —
a good scenario must still pick the right path. The default agent preamble (Part I) is applied
automatically; send `"preamble": ""` to test the truly bare prompt.

- **Catalog:** `создай справочник Контрагенты`, `добавь в справочник Номенклатура реквизит Артикул`,
  `сделай иерархический справочник Подразделения`, `справочник Товары со ссылкой на ЕдиницыИзмерения`.
- **Document:** `создай документ РеализацияТоваров`, `добавь в документ Заказ табличную часть Товары`,
  `документ ПоступлениеТоваров должен делать движения по регистру ОстаткиТоваров`.
- **Enum:** `создай перечисление СтатусыЗаказа со значениями Новый, ВРаботе, Закрыт`.
- **Register:** `создай регистр сведений ЦеныНоменклатуры`, `регистр накопления ОстаткиТоваров с измерением Товар и ресурсом Количество`.
- **Form:** `сделай форму элемента для справочника Номенклатура`, `добавь форму списка в Контрагенты`,
  `у документа РеализацияТоваров нет формы, создай`, `выведи на форму элемента реквизит Комментарий`.
- **Template:** `создай макет печатной формы для документа РеализацияТоваров`,
  `создай отчёт АнализПродаж с макетом СКД`, `добавь в отчёт Остатки схему компоновки данных`,
  `нужен табличный макет Ценник в справочнике Номенклатура`.
- **Report/Processor:** `создай отчёт ПродажиЗаПериод`, `создай обработку ЗагрузкаПрайса`.
- **Common:** `создай общий модуль ОбщегоНазначения серверный`, `добавь константу ВалютаУчёта`,
  `создай роль Кладовщик`, `создай подсистему Продажи`, `регламентное задание ЕжедневныйПересчёт`.
- **Service:** `создай HTTP-сервис Orders`, `создай web-сервис ОбменДанными`.
- **Ambiguous / stress (must clarify or pick sensibly, not produce junk):** `форма`,
  `почему документ открывается стандартной формой`, `хочу свою печатную форму у накладной`,
  `добавь скидку в номенклатуру и выведи на форму`.

Add more prompts as you discover weak areas. Include both clean-create and repair-style prompts:

- **Repair:** `у справочника Номенклатура есть форма элемента, но она не открывается`,
  `макет есть, но файла Template.dcs нет`, `у реквизита Бренд ошибка типа, исправь`,
  `форма создана, но реквизит не выводится`.
- **Rare entities:** `создай план обмена ОбменСКассой`, `создай XDTO-пакет Интеграция`,
  `создай подписку на событие ПередЗаписьюНоменклатуры`, `создай критерий отбора АктивныеТовары`.
- **Composite:** `создай документ Поступление и регистр Остатки, документ должен делать движения`,
  `создай журнал документов Продажи и добавь туда РеализацияТоваров`.

## 12. How to author / improve a scenario guide

Mirror `workflows/create/create_catalog.md` and `create_object_form.md`. A solid guide has:

1. **One-line purpose** + when to use (and when to use a different scenario).
2. **Hard rules — never violate** (use ✅/❌/⛔). Encode every failure you observe here. Recurring rules:
   - ✅ Run inside a BM transaction: `bmModel.getGlobalContext().execute(new AbstractBmTask<…>(){…})`.
   - ✅ **Set a UUID** on every created object (`obj.setUuid(UUID.randomUUID())`) — missing → SU45.
   - ✅ **New top object:** `transaction.attachTopObject((IBmObject)obj, fqnGenerator.generate…Fqn(...))`
     and add it to its container (`configuration.getCatalogs().add(...)`).
   - ⛔ **Existing owner:** resolve via `transaction.getTopObjectByFqn("Catalog.X")`; **never**
     `attachTopObject` it again (→ `BmFqnAlreadyInUseException`) and never recreate it. Add only the
     new child to its collection.
   - ⛔ **External-property content** (form structure, template body) persists to disk **only** when
     attached as a top object: `attachTopObject(content, fqnGenerator.generateExternalPropertyFqn(parent, MdClassPackage.Literals.BASIC_FORM__FORM | BASIC_TEMPLATE__TEMPLATE))`.
     `setForm/setTemplate` alone is in-memory only.
   - ⛔ **Forms:** `formGenerator.generateForm(...)` needs a **non-null `columnCount`** for
     `OBJECT/FOLDER/CONSTANTS/RECORD/REPORT` (it is unboxed to `int`) — pass e.g. `1`.
   - ❌ **Never hand-write** `.mdo`/`.form`/`.dcs`/`.mxl`/settings XML with file tools — use the model API.
   - ❌ **Never finish with "сделайте вручную"** for something a scenario supports.
   - ❌ Don't invent helper classes/APIs; use exact factory packages (e.g.
     `com._1c.g5.v8.dt.dcs.model.schema.DcsFactory`, not `…dcs.util`).
3. **PRE-FLIGHT** for dependencies (probe referenced objects with `getTopObjectByFqn`, create missing
   ones first, resolve reference types via `MdProducedTypesUtil.getProducedType(dep, MdTypePackage.Literals.MD_REF_TYPE)`).
4. **One worked example** (copy-pasteable, with explicit imports — manual cards do NOT auto-import).
   Prefer small, incremental snippets (one logical step per `JShell` call) to waste fewer tool rounds.
   Do **not** put dangerous or obsolete anti-patterns in copy-pasteable code fences. If the model
   repeatedly copies a wrong signature (for example an obsolete `generateForm(..., columnCount, null)`),
   remove the bad snippet entirely and state the prohibition in prose.
5. **Idempotency** — check existence before creating; on re-run, continue from existing objects.
   Include repair behavior for common partial states, not only clean-create behavior.
6. **Required post-check** — call `GetMarkers` (`marker_type:"1c"`) on the changed top object's `.mdo`
   (derive the path from the FQN; for children/forms/templates use the owner's `.mdo`) and treat
   relevant markers as failure until fixed.

Keep the technical depth in the guide (the user prompt won't have it). Each fix should be small,
targeted at one observed failure, and re-verified through the loop.

### Changelog template for recursive improvement

Keep a short log next to your notes or in the final response of the external LLM:

```text
Prompt/id:
Observed failure:
Transcript evidence:
Disk/marker evidence:
Manual/index change:
Why this fix is minimal:
Retest prompt/id:
Retest result:
Remaining risk:
```

## 13. Editing `index.json` (discoverability)

The model finds a scenario via `title`/`summary`/`keywords`. When a realistic prompt fails to reach
the right scenario:
- Add **keywords**, including **Russian** synonyms and the words real users type
  (e.g. `форма списка`, `печатная форма`, `СКД`, `схема компоновки данных`, `табличная часть`).
- In a parent scenario's `notes`/`vars`, **cross-link** the follow-up scenario (e.g. `create_report`
  → "to add a СКД layout use `create_object_template` + `fill_template_content`").
- Keep `bindings` accurate (lists the variables the guide's code uses).
- After editing, the catalog hot-reloads. Validate it stays valid JSON (a parse error disables the
  whole catalog). Keep `scope: "edt"`.

## 14. Failure signatures → fix (field-tested)

| Symptom in the transcript / on disk                                     | Fix in the scenario                                                                                                                       |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Model picks the wrong scenario (e.g. hand-writes XML for a layout)      | add keywords + cross-link in `index.json`; add a hard rule forbidding the wrong path                                                      |
| `BmFqnAlreadyInUseException`                                            | the guide must resolve the existing owner via `getTopObjectByFqn` and never re-attach it                                                  |
| `NullPointerException` in `FormGenerator`                               | pass a non-null `columnCount` (e.g. `1`) for OBJECT/FOLDER/CONSTANTS/RECORD/REPORT                                                        |
| Object/form OK but content file (`.dcs`/`.mxl`/`.form`) missing on disk | attach the content as a top object via `generateExternalPropertyFqn(parent, BASIC_*__*)`                                                  |
| `IllegalStateException: Template already exists` / duplicate child      | add/keep the idempotency existence check                                                                                                  |
| `Too many tool rounds`                                                  | task too big for the round budget — split the prompt, or raise `max_tool_rounds` in the request; tighten the guide to fewer/cleaner steps |
| Model stops after read-only tools, empty answer                         | ensure the agent preamble is applied (don't send `"preamble": ""` for real tests); make the guide's first action explicit                 |
| Empty `compilation_errors` but wrong/partial result                     | strengthen hard rules / completeness guard ("do not finish until the artifact exists and markers are checked")                            |

**Verification trap:** EDT `.mdo` elements carry a `uuid` attribute — e.g. `<forms uuid="...">`,
`<templates uuid="...">`. When checking persistence, match `<forms` / `<templates` (or the child
name), **not** the bare `<forms>` — the latter gives a false negative.
