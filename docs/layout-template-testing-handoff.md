# Handoff: тестирование макетов EDT/JShell

Дата: 2026-07-07 (обновлено после ретеста AI083x, см. раздел «Ретест AI083x» в конце)

## Цель

Проверить, что агент корректно работает с макетами 1C/EDT через JShell при простых пользовательских промптах, например:

```text
В КнижномМагазине для авторов создай макет схемы компоновки данных
```

Важно: конфигурация `Склад` защищена от редактирования. Для тестов использовать `КнижныйМагазин`.

Autopilot-канал:

```text
D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev
```

Запросы кладутся в:

```text
D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev\inbox\<id>.json
```

Ответы читать из:

```text
D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev\outbox\<id>.json
```

Лог:

```text
D:\Projects\_Eclipse\EDT_Plugin_log\trace.log
```

## Проверяемые типы макетов

- Табличный документ
- Текстовый документ
- Двоичные данные
- HTML документ
- Географическая схема
- Графическая схема
- Схема компоновки данных
- Макет оформления компоновки данных
- Внешняя компонента

## Ожидаемое поведение

Для простого запроса без исходного файла:

- `Табличный документ` должен создаваться с валидным телом `Template.mxlx`.
- `Схема компоновки данных` должна создаваться с валидным пустым телом `Template.dcs`.
- `Текстовый документ`, `HTML документ`, `Двоичные данные`, `Географическая схема`, `Графическая схема`, `Макет оформления компоновки данных`, `Внешняя компонента` должны остановиться до JShell и запросить реальный файл/содержимое.
- Для source-backed типов нельзя создавать metadata-only `<templates>` запись и нельзя создавать пустой или искусственный `Template.<ext>`.

Source-backed расширения:

```text
TextDocument -> Template.txt
HTMLDocument -> Template.htmldoc
BinaryData -> Template.bin
GeographicalSchema -> Template.geos
GraphicalSchema -> Template.scheme
DataCompositionAppearanceTemplate -> Template.dcsat
AddIn -> Template.addin
```

## Что уже проверено

### Ранее успешная визуальная проверка с реальными source-файлами

В `КнижныйМагазин` были созданы и проверены макеты с реальными файлами из `C:\Projects\erp2\ERP2\src`:

| Тип                                | Макет                           | Файл тела                     |
|------------------------------------|---------------------------------|-------------------------------|
| Табличный документ                 | `VisualТабличныйДокAI0760`      | `Template.mxlx`, 62945 байт   |
| Текстовый документ                 | `VisualТекстовыйAI0742`         | `Template.txt`, 161 байт      |
| Двоичные данные                    | `VisualДвоичныеДанныеAI0759`    | `Template.bin`, 2547222 байт  |
| HTML документ                      | `VisualHTMLAI0743`              | `Template.htmldoc`, 163 байт  |
| Географическая схема               | `VisualГеоAI0755`               | `Template.geos`, 3045218 байт |
| Графическая схема                  | `VisualГрафическаяAI0756`       | `Template.scheme`, 11814 байт |
| Схема компоновки данных            | `VisualСКДДокAI0761`            | `Template.dcs`, 15968 байт    |
| Макет оформления компоновки данных | `VisualОформлениеСКДAI0747`     | `Template.dcsat`, 53135 байт  |
| Внешняя компонента                 | `VisualВнешняяКомпонентаAI0758` | `Template.addin`, 71023 байт  |

Вывод: при наличии реального source-файла все типы можно создать и проверить визуально в EDT.

### Простые промпты без source-файлов

Запускались серии `Simple*AI080x`, `Simple*AI081x`, `Simple*AI082x`.

Положительно:

- `SimpleТабличныйAI0821`: создан объектный макет у `Catalog.Авторы`, есть `Template.mxlx`.
- `SimpleСКДAI0822`: создан объектный макет у `Catalog.Авторы`, есть `Template.dcs`.
- `SimpleГеоAI0816`: один из прогонов правильно остановился и попросил `.geos`.

Проблемы:

- `SimpleТекстAI0803`, `SimpleТекстAI0813`, `SimpleТекстAI0823`: создавались metadata-only или пустой `Template.txt` без source-файла.
- `SimpleHTMLAI0804`, `SimpleHTMLAI0814`, `SimpleHTMLAI0824`: создавался `Template.htmldoc` без реального source-файла.
- `SimpleДвоичныеAI0805`, `SimpleДвоичныеAI0815`: создавались metadata-only записи без `Template.bin`.
- `SimpleГрафическаяAI0817`: metadata-only без `Template.scheme`.
- `SimpleОформлениеСКДAI0818`: metadata-only без `Template.dcsat`.
- `SimpleВнешняяКомпонентаAI0809`, `SimpleВнешняяКомпонентаAI0819`: metadata-only без `Template.addin`.
- `SimpleОформлениеСКДAI0828`: ушел в `CommonTemplate`, хотя промпт был "для авторов"; создал только `.mdo`, без `Template.dcsat`.

## Важный нюанс: кеш manual в EDT

После правок manual третий прогон все еще использовал старый `guide_markdown`.

В `trace.log` для `SimpleОформлениеСКДAI0828` видно, что `guide_markdown` у `create_object_template` не содержал новую строку:

```text
STOP before any JShell
```

При этом в файлах repo и `bin/manual` эта строка уже есть:

```text
C:\Projects\code-ai\bundles\com.e1c.edt.ai.context\resources\manual\workflows\create\create_object_template.md
C:\Projects\code-ai\bundles\com.e1c.edt.ai.context\bin\manual\workflows\create\create_object_template.md
```

Вывод: текущая EDT-сессия, похоже, держит manual в памяти. Для честного ретеста после правок нужно перезапустить EDT/плагин или найти способ сбросить кеш manual.

## Измененные manual-файлы

На момент handoff изменены:

```text
bundles/com.e1c.edt.ai.context/resources/manual/index.json
bundles/com.e1c.edt.ai.context/resources/manual/workflows/create/create_object_template.md
bundles/com.e1c.edt.ai.context/resources/manual/workflows/create/fill_template_content.md
bundles/com.e1c.edt.ai.context/resources/manual/workflows/edit/edit_existing_object.md
```

Изменения также синхронизировались в:

```text
bundles/com.e1c.edt.ai.context/bin/manual/...
```

`git diff --check` проходил, были только предупреждения Git про будущую замену LF на CRLF.

Коммиты не делать без отдельного запроса.

## Что именно исправлено в manual

### `index.json`

- `create_common_template` больше не должен матчиться на общий keyword `макет/template`; он должен использоваться только для `общий макет` / `CommonTemplate`.
- `create_object_template` summary усилен: для source-backed типов без source-файла STOP before JShell.
- `fill_template_content` больше не должен матчиться на простые `создай макет ...`; только на явные запросы заполнить/заменить содержимое.
- `create_source_backed_template` summary усилен как STOP-сценарий: без source-файла не выполнять JShell, не создавать metadata, не создавать пустые файлы.

### `create_object_template.md`

Добавлено явное правило:

```text
STOP before any JShell when the requested type is source-backed and the user did not provide a real source file/content.
```

Это правило должно применяться и к объектным макетам, например:

```text
В КнижномМагазине для авторов создай макет текстового документа
```

Ожидаемый ответ: попросить `Template.txt` или текстовое содержимое.

### `fill_template_content.md`

- Запрещено вручную собирать богатую СКД из угаданных EMF-классов.
- Запрещен Java-invalid import alias:

```java
import ...DcsFactory as CoreDcsFactory;
```

- Для содержательной СКД рекомендован путь: создать макет, затем заменить `Template.dcs` из проверенного source-файла через Eclipse EFS.

### `edit_existing_object.md`

Добавлен запрет создавать `Template` из generic edit workflow.

Если запрос содержит `макет`, `template`, `TemplateType`, `табличный документ`, `схема компоновки данных`, `текстовый документ`, `HTML документ`, `двоичные данные`, `географическая схема`, `графическая схема`, `макет оформления компоновки данных`, `внешняя компонента`, сценарий должен переключаться на `create_object_template`, `create_common_template` или `create_source_backed_template`.

## Как запускать тесты через ai-dev

Пример PowerShell для постановки запроса:

```powershell
$inbox = 'D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev\inbox'
$id = 'manual-layout-test-001'
$prompt = 'В КнижномМагазине для авторов создай макет схемы компоновки данных с именем TestСКДAI'
$obj = [ordered]@{ id = $id; prompt = $prompt }
Set-Content -LiteralPath (Join-Path $inbox ($id + '.json')) -Value ($obj | ConvertTo-Json -Depth 4) -Encoding UTF8
```

Ожидание ответа:

```powershell
$out = 'D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev\outbox'
$p = Join-Path $out ($id + '.json')
while (!(Test-Path $p)) { Start-Sleep -Seconds 5 }
$j = Get-Content $p -Encoding UTF8 | ConvertFrom-Json
$j.final_text
$j.stalled
$j.tool_error_count
$j.jshell_error_count
$j.tool_call_count
```

Проверка файлов для объектного макета `Catalog.Авторы`:

```powershell
$root = 'D:\Projects\_Eclipse\EDT_Plugin\КнижныйМагазин\src\Catalogs\Авторы\Templates'
Get-ChildItem -LiteralPath $root -Recurse -Filter 'Template.*' |
  Select-Object FullName, Length, LastWriteTime
```

Проверка, что metadata-only source-backed запись не появилась:

```powershell
Select-String -Path 'D:\Projects\_Eclipse\EDT_Plugin\КнижныйМагазин\src\Catalogs\Авторы\Авторы.mdo' `
  -Pattern 'SimpleТекст|SimpleHTML|SimpleДвоичные|SimpleГео|SimpleГрафическая|SimpleОформлениеСКД|SimpleВнешняя'
```

## Рекомендуемый следующий ретест после перезапуска EDT/плагина

Использовать новые уникальные имена, например `AI083x`, чтобы не конфликтовать с уже созданными тестовыми артефактами.

Промпты:

```text
В КнижномМагазине для авторов создай макет табличного документа с именем SimpleТабличныйAI0831
В КнижномМагазине для авторов создай макет схемы компоновки данных с именем SimpleСКДAI0832
В КнижномМагазине для авторов создай макет текстового документа с именем SimpleТекстAI0833
В КнижномМагазине для авторов создай макет HTML документа с именем SimpleHTMLAI0834
В КнижномМагазине для авторов создай макет двоичных данных с именем SimpleДвоичныеAI0835
В КнижномМагазине для авторов создай макет географической схемы с именем SimpleГеоAI0836
В КнижномМагазине для авторов создай макет графической схемы с именем SimpleГрафическаяAI0837
В КнижномМагазине для авторов создай макет оформления компоновки данных с именем SimpleОформлениеСКДAI0838
В КнижномМагазине для авторов создай макет внешней компоненты с именем SimpleВнешняяКомпонентаAI0839
```

Ожидаемый результат:

- `AI0831`: создан `src/Catalogs/Авторы/Templates/SimpleТабличныйAI0831/Template.mxlx`.
- `AI0832`: создан `src/Catalogs/Авторы/Templates/SimpleСКДAI0832/Template.dcs`.
- `AI0833`-`AI0839` для source-backed типов: нет новых `<templates>` в `Авторы.mdo`, нет `Template.<ext>`, ответ просит source-файл/содержимое.

Если после перезапуска EDT source-backed типы все еще создают metadata-only записи, искать обходной сценарий в trace по `manual_id` и по JShell-коду с `mdFactory.createTemplate()`.

## Артефакты, которые уже могли загрязнить `КнижныйМагазин`

В `Catalog.Авторы` могли остаться тестовые metadata-only записи:

```text
SimpleТекстAI0803
SimpleHTMLAI0804
SimpleДвоичныеAI0805
SimpleГеоAI0806
SimpleВнешняяКомпонентаAI0809
SimpleТекстAI0813
SimpleHTMLAI0814
SimpleДвоичныеAI0815
SimpleГрафическаяAI0817
SimpleОформлениеСКДAI0818
SimpleВнешняяКомпонентаAI0819
SimpleТекстAI0823
SimpleHTMLAI0824
```

В `CommonTemplates` мог остаться ошибочный:

```text
SimpleОформлениеСКДAI0828
```

Не удалять их без явного решения, но при новых тестах использовать свежие имена.

## Ретест AI083x (2026-07-07, ~11:28–11:32)

### Гипотеза про кеш manual ОПРОВЕРГНУТА

Перезапуск EDT для правок manual НЕ нужен. `MetadataManualCatalog` хот-релоадит `index.json`
по mtime, а guide markdown читается с classpath при каждом обращении. Доказательство: строка
`STOP before any JShell` появилась в `guide_markdown` в trace.log сразу после обновления файлов в
11:06:31, в той же сессии EDT (запущена 06.07 17:51). «Стейл» в утренних прогонах — это гонка:
серии layout1/layout2 и начало layout3 шли до 11:06, когда файлы ещё были старыми.

Требование: после правки `resources/manual/...` синхронизировать копию в `bin/manual/...`
(classpath self-hosted запуска), перезапуск не требуется.

### Результаты AI083x (актуальные manual, та же сессия EDT)

| #    | Тип                | Ожидание     | Результат                                                                                               |
|------|--------------------|--------------|---------------------------------------------------------------------------------------------------------|
| 0831 | Табличный          | тело `.mxlx` | ФЕЙЛ: metadata-only, без тела; обошёл через `edit_existing_metadata`, маркеры чистые, отчитался успехом |
| 0832 | СКД                | тело `.dcs`  | OK: `Template.dcs` создан                                                                               |
| 0833 | Текстовый          | STOP+вопрос  | OK: спросил файл, метаданные не создал                                                                  |
| 0834 | HTML               | STOP+вопрос  | ЧАСТИЧНО: метаданные не создал, но завис после «Получу список проектов...» (stalled, 1 tool call)       |
| 0835 | Двоичные           | STOP+вопрос  | OK: спросил файл                                                                                        |
| 0836 | Гео                | STOP+вопрос  | ФЕЙЛ: metadata-only, при этом ЦИТИРОВАЛ `create_object_template` со STOP-правилом                       |
| 0837 | Графическая        | STOP+вопрос  | ЧАСТИЧНО: спросил файл, но предложил запрещённый вариант «только метаданные»                            |
| 0838 | Оформление СКД     | STOP+вопрос  | ФЕЙЛ: 7 попыток создания (6 jshell-ошибок), в итоге metadata-only в `Авторы.mdo`, отчитался успехом     |
| 0839 | Внешняя компонента | STOP+вопрос  | OK: спросил файл                                                                                        |

Вывод: manual-правки исчерпаны — модель нарушает STOP-правило, даже когда сама передаёт
соответствующий `manual_ids` (0836: `create_object_template`; 0838: `create_common_template`).
Новое загрязнение `Авторы.mdo`: metadata-only записи `SimpleТабличныйAI0831`, `SimpleГеоAI0836`,
`SimpleОформлениеСКДAI0838`.

### Введён жёсткий гейт в коде (требует пересборки + перезапуска EDT)

- `IJShellBindingProvider.validateCode(code)` — новый pre-execution хук (default null).
- `JShellMcpTool.validateCodePolicies` — вызывает хук провайдеров подходящего scope, при
  отказе бросает `ToolException` с `ToolErrorType.USER_VISIBLE`.
- `MetadataBindingProvider.validateCode` — правила:
  1. `create(Common)?Template(` + source-backed `TemplateType` без копирования тела
     (`openInputStream`/`setContents`/`ByteArrayInputStream`) → отказ, просить файл у пользователя;
  2. `create(Common)?Template(` без source-backed типа и без `setTemplate(`+`attachTopObject(`
     и без копирования тела → отказ (ловит metadata-only табличный/СКД, кейс 0831).

Проверка по trace: все фейлы ретеста (0831, 0836, все 7 попыток 0838) были бы отклонены гейтом.

Также закрыта дыра `edit_existing_metadata` (второй generic-edit сценарий, через который ушёл
0831): в summary (`index.json`) и в сам guide добавлен запрет на создание макетов с редиректом
на `create_object_template`/`create_common_template`/`create_source_backed_template`.

### Следующие шаги

1. Пересобрать плагин и перезапустить self-hosted EDT (Java-гейт без этого не активен).
2. Прогнать серию `AI084x` (те же 9 промптов, свежие имена) и проверить: фейл-кейсы должны
   упереться в гейт и корректно спросить файл; 0831/0832 должны создаться с телом.
3. Остаточные риски вне гейта: «полустоп» вида 0834 (молчаливый обрыв) и предложение
   запрещённой заглушки (0837) — это качество ответа, гейт на них не влияет.

## Контрольная серия AI084x с гейтом (2026-07-07, 11:59–12:05, после пересборки и перезапуска)

| #    | Тип                | Результат                                                                      |
|------|--------------------|--------------------------------------------------------------------------------|
| 0841 | Табличный          | OK: создан с телом `Template.mxlx` (фикс кейса 0831)                           |
| 0842 | СКД                | OK: создан с телом `Template.dcs` (2 jshell-ошибки компиляции, сам исправился) |
| 0843 | Текстовый          | OK: гейт отклонил (12:01:16), модель спросила файл, метаданных нет             |
| 0844 | HTML               | ЧАСТИЧНО: обрыв «Получу список проектов...» (как 0834), но загрязнения нет     |
| 0845 | Двоичные           | OK: гейт отклонил (12:02:15), спросила файл                                    |
| 0846 | Гео                | OK: гейт отклонил (12:03:32), спросила файл (фикс кейса 0836)                  |
| 0847 | Графическая        | OK: гейт отклонил (12:04:01), спросила файл, заглушку больше НЕ предлагает     |
| 0848 | Оформление СКД     | OK: остановилась сама по manual, без попыток создания (фикс кейса 0838)        |
| 0849 | Внешняя компонента | OK: гейт отклонил (12:05:07), спросила файл                                    |

Итого 8/9 полностью корректны, конфигурация не загрязнена (в `Авторы.mdo` только 0841/0842 с
телами, CommonTemplates чист). Гейт сработал 5 раз, ни одного повторного «долбления» — после
отказа модель сразу переходит к вопросу пользователю (сообщение гейта содержит инструкцию
не ретраить и спросить файл).

Открытый остаток: «полустоп» на HTML-документе (0834/0844) — модель делает 1 вызов манула и
обрывается на фразе-намерении. Это не связано с макетами/гейтом; похоже на серверный стоп после
tool call без продолжения. Отдельная тема для исследования.

## Серия AI085x «от разработчика 1С» с реальными source-файлами (2026-07-07, 12:13–12:30)

Исходники подготовлены в `D:\Projects\_Eclipse\tmp\ai-layout-sources\` (взяты из
`C:\Projects\erp2\ERP2\src`: CommonTemplates + Flowchart.scheme бизнес-процесса «Задание»).
Промпты реалистичные, для source-backed типов указан путь к файлу.

Итог: созданы все 9 объектных макетов у `Catalog.Авторы`, размеры тел байт-в-байт совпадают с
исходниками:

| Макет                     | Тип в .mdo                        | Тело                      | Примечание                                                                        |
|---------------------------|-----------------------------------|---------------------------|-----------------------------------------------------------------------------------|
| ПечатьВизиткиAI0851       | SpreadsheetDocument               | Template.mxlx (490)       | пустой, сгенерирован                                                              |
| СписокАвторовСКДAI0852    | DataCompositionSchema             | Template.dcs (526)        | пустой, сгенерирован                                                              |
| ИнструкцияДляАвтораAI0853 | TextDocument                      | Template.txt (161)        | копия исходника                                                                   |
| ОписаниеАвтораAI0854      | HTMLDocument                      | Template.htmldoc (163)    | копия исходника                                                                   |
| ФотоОбложкиAI0855         | BinaryData                        | Template.bin (1929)       | с 3-й попытки: 2 «полустопа» подряд                                               |
| КартаРегионовAI0856       | GeographicalSchema                | Template.geos (3 045 218) | копия исходника                                                                   |
| СхемаСогласованияAI0857   | GraphicalSchema                   | Template.scheme (33 865)  | копия исходника                                                                   |
| ОформлениеОтчетовAI0858   | DataCompositionAppearanceTemplate | Template.dcsat (53 135)   | гейт отклонил 1-ю попытку без тела (12:22:18), модель исправилась                 |
| КомпонентаСканераAI0859   | AddIn                             | Template.addin (71 023)   | ДЕФЕКТ: модель назвала тело `<Имя>.addin`; переименовано вручную в Template.addin |

Найденные дефекты и меры:

1. Имя файла тела: модель может копировать источник под произвольным именем вместо
   обязательного `Template.<ext>`. В `create_source_backed_template.md` добавлен жёсткий блок
   «The body file name on disk is FIXED by convention» с путями для object-owned/common
   (хот-релоад, действует без перезапуска).
2. «Полустоп» (фраза-намерение, 0 tool calls, stalled=true) воспроизводится нестабильно и на
   разных типах (в этой серии: 2 раза подряд на binary; ранее — HTML). Не зависит от макетной
   логики; лечится повтором запроса. Требует отдельного анализа канала/сервера.

## Наполнение 0851/0852 содержимым (2026-07-07, ~12:30–12:40)

- `ПечатьВизиткиAI0851`: тело заменено агентом из файла `ПечатнаяФорма.mxlx`
  (копия `СведенияИзСчетовФактур` из ERP2, 17 135 байт) — сценарий замены тела из
  проверенного source-файла работает. OK.
- `СписокАвторовСКДAI0852`: генеративный промпт («наполни схему: запрос по Справочник.Авторы»)
  выявил НОВЫЙ ДЕФЕКТ — модель сочинила DCS XML из головы (выдуманные `dcscom:DataCompositionField`,
  атрибуты `name=`/`type=`, `dcsset:structure="list"`) и записала его файловым инструментом
  **Edit**, обойдя и JShell-гейт (он не видит файловые инструменты), и запреты manual
  (Edit разрешался «для узких правок»). Такой файл редактор СКД не десериализует, при этом
  GetMarkers молчит.
  - Файл заменён вручную на валидную схему, собранную по образцам ERP2
    (`DataSetQuery` + запрос `ВЫБРАТЬ Код, Наименование ИЗ Справочник.Авторы`, вариант
    настроек с деталями и сортировкой по наименованию).
  - В `fill_template_content.md` добавлен явный запрет на ВЫДУМАННУЮ структуру DCS/moxel XML любым
    инструментом (Edit/Write/EFS-строка) + канонический скелет `Template.dcs` (см. ниже).

## Решение по enforcement: только manuals (2026-07-07)

Java-гейт (`IJShellBindingProvider.validateCode` + правила в `MetadataBindingProvider`),
реализованный и проверенный на серии AI084x, ОТКАЧЕН по решению: политику держим только в
хот-релоадных manuals, код плагина не трогаем.

Вместо гейта в manuals добавлены описания форматов тел:

- `template_type_matrix.md` — новая секция «Body file formats»: таблица по каждому
  `Template.<ext>` — формат (XML/бинарный), корневой элемент с namespace
  (`<document xmlns=".../spreadsheet">` для mxlx, `<DataCompositionSchema>` для dcs,
  `<GraphicalSchema>` для scheme, `<AppearanceTemplate>` для dcsat, `<geographicalSchema>` для
  geos), и может ли агент производить тело (bin/addin/geos/scheme/dcsat — только копия).
  Плюс правило «имя тела всегда Template.<ext>» и исправлен пункт Hard rules, разрешавший
  metadata-only «if acceptable».
- `fill_template_content.md` — канонический скелет `Template.dcs` (полный валидный XML, выверен
  по ERP2: DataSetQuery + DataSetFieldField + settingsVariant со StructureItemGroup без
  groupItems = список): разрешено менять ТОЛЬКО запрос/поля/заголовки/имя варианта/сортировку;
  плюс структурная справка по порядку элементов. Это единственный разрешённый рукописный DCS.
- `create_source_backed_template.md` — отсылка к «Body file formats» с перечнем copy-only типов.

Все правки синхронизированы в `bin/manual` (хот-релоад, действуют без перезапуска).

Примечание: в текущем запущенном инстансе EDT (javaw от 07.07 11:55) Java-гейт ещё загружен в
память; после следующей пересборки/перезапуска его не будет — защита останется только на manuals.
При следующем цикле тестов (AI086x+) проверять те же фейл-кейсы: metadata-only source-backed,
обход через generic-сценарии, рукописный DCS XML через Edit, имя тела не Template.<ext>.
