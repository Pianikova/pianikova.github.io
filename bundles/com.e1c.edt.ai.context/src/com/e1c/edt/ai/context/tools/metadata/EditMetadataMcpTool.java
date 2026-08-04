/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.tools.McpToolConstants;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;

/**
 * Declarative and guarded editor for 1C metadata. Unlike the former JShell path, the model decides
 * what operation to invoke, but never generates or executes EDT Java API calls.
 */
public final class EditMetadataMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = McpToolConstants.EDIT_METADATA_TOOL_NAME;

    private final IJson json;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final MetadataOperationRegistry registry;
    private final MetadataMutationService mutationService;
    private final McpToolCallSpecification specification;

    @Inject
    public EditMetadataMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        MetadataOperationRegistry registry, MetadataMutationService mutationService)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(registry);
        Preconditions.checkNotNull(mutationService);
        this.json = json;
        this.messageFactory = messageFactory;
        this.registry = registry;
        this.mutationService = mutationService;
        this.specification = createSpecification();
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return specification;
    }

    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = true;
        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = "Edit 1C metadata"; //$NON-NLS-1$
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        final JsonObject arguments;
        try
        {
            var parsed = JsonParser.parseString(call.function.arguments);
            if (!parsed.isJsonObject())
            {
                throw new ToolException("Arguments must be a single JSON object."); //$NON-NLS-1$
            }
            arguments = parsed.getAsJsonObject();
        }
        catch (RuntimeException e)
        {
            if (e instanceof ToolException)
            {
                throw e;
            }
            throw new ToolException("Cannot parse arguments as JSON object.", e, ToolErrorType.RETRYABLE); //$NON-NLS-1$
        }

        normalizeCommonModelArguments(arguments);
        var validationErrors = registry.validate(arguments);
        if (!validationErrors.isEmpty())
        {
            throw new ToolException(String.join("\n", validationErrors)); //$NON-NLS-1$
        }
        var request = json.deserialize(arguments.toString(), MetadataRequest.class)
            .orElseThrow(() -> new ToolException("Cannot deserialize metadata operation arguments.")); //$NON-NLS-1$

        // Run on a single-threaded FIFO executor, never the common pool. The model routinely emits a
        // batch of dependent operations in one message (add a tabular section, then its attributes);
        // McpTools dispatches that batch in order, so a FIFO queue makes execution follow the model's
        // intended order. On the common pool the order is arbitrary and dependent operations failed with
        // "Child metadata object not found" even though the batch itself was correct.
        return CompletableFuture.supplyAsync(() -> {
            Object result = "help".equals(request.operation) ? help(request.topic) //$NON-NLS-1$
                : mutationService.execute(request, cancellationToken);
            details.responseMarkdown = "1C metadata operation completed"; //$NON-NLS-1$
            return messageFactory.createMessage(this, call, json.serialize(result), details);
        }, SEQUENTIAL_EXECUTOR);
    }

    public static void normalizeCommonModelArguments(JsonObject arguments)
    {
        var operationElement = arguments.get("operation"); //$NON-NLS-1$
        var operation = operationElement != null && operationElement.isJsonPrimitive()
            ? operationElement.getAsString() : ""; //$NON-NLS-1$
        var objectNameElement = arguments.get("object_name"); //$NON-NLS-1$
        if ("help".equals(operation)) //$NON-NLS-1$
        {
            for (var parameter : new ArrayList<>(arguments.keySet()))
            {
                if (!"operation".equals(parameter) && !"topic".equals(parameter) && !"verify".equals(parameter)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                {
                    arguments.remove(parameter);
                }
            }
        }
        if ("inspectObject".equals(operation) && objectNameElement != null && objectNameElement.isJsonPrimitive() //$NON-NLS-1$
            && objectNameElement.getAsString().contains(".Form.")) //$NON-NLS-1$
        {
            arguments.addProperty("operation", "inspectForm"); //$NON-NLS-1$ //$NON-NLS-2$
            operation = "inspectForm"; //$NON-NLS-1$
        }
        else if ("inspectForm".equals(operation) && objectNameElement != null //$NON-NLS-1$
            && objectNameElement.isJsonPrimitive() && !objectNameElement.getAsString().contains(".Form.")) //$NON-NLS-1$
        {
            arguments.addProperty("operation", "inspectObject"); //$NON-NLS-1$ //$NON-NLS-2$
            operation = "inspectObject"; //$NON-NLS-1$
        }
        if ("setObjectProperty".equals(operation)) //$NON-NLS-1$
        {
            arguments.remove("name"); //$NON-NLS-1$
            arguments.remove("title"); //$NON-NLS-1$
            var propertyName = arguments.get("property_name"); //$NON-NLS-1$
            if (propertyName != null && propertyName.isJsonPrimitive())
            {
                var value = propertyName.getAsString();
                if (!value.isEmpty() && Character.isUpperCase(value.charAt(0)))
                {
                    arguments.addProperty("property_name", Character.toLowerCase(value.charAt(0)) //$NON-NLS-1$
                        + value.substring(1));
                }
            }
        }
        if ("createObject".equals(operation)) //$NON-NLS-1$
        {
            arguments.remove("language_code"); //$NON-NLS-1$
        }
        if ("setChildProperty".equals(operation) && arguments.has("property_name") //$NON-NLS-1$ //$NON-NLS-2$
            && "type".equals(arguments.get("property_name").getAsString()) && arguments.has("type")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            arguments.addProperty("operation", "setChildType"); //$NON-NLS-1$ //$NON-NLS-2$
            arguments.remove("property_name"); //$NON-NLS-1$
            arguments.remove("property_value"); //$NON-NLS-1$
            arguments.remove("title"); //$NON-NLS-1$
        }
        if ("setFormItemProperty".equals(operation) && arguments.has("property_name") //$NON-NLS-1$ //$NON-NLS-2$
            && "handlers".equalsIgnoreCase(arguments.get("property_name").getAsString()) //$NON-NLS-1$ //$NON-NLS-2$
            && arguments.has("property_value")) //$NON-NLS-1$
        {
            // A form field/table/form's event handlers are a collection (Event -> handler name), not a
            // scalar; the model routinely still tries to write it as one via setFormItemProperty. Redirect
            // to the dedicated operation instead of surfacing a dead-end "not a scalar" error, accepting
            // both `"OnChange"` and `"OnChange:MyHandler"` as the property_value shape.
            var value = arguments.get("property_value").getAsString(); //$NON-NLS-1$
            var separator = value.indexOf(':');
            var eventName = (separator >= 0 ? value.substring(0, separator) : value).trim();
            var handlerName = separator >= 0 ? value.substring(separator + 1).trim() : null;
            arguments.addProperty("operation", "addFormEventHandler"); //$NON-NLS-1$ //$NON-NLS-2$
            arguments.remove("property_name"); //$NON-NLS-1$
            arguments.remove("property_value"); //$NON-NLS-1$
            arguments.addProperty("event", eventName); //$NON-NLS-1$
            if (handlerName != null && !handlerName.isBlank())
            {
                arguments.addProperty("handler", handlerName); //$NON-NLS-1$
            }
            operation = "addFormEventHandler"; //$NON-NLS-1$
        }
        if (("addFormEventHandler".equals(operation) || "removeFormEventHandler".equals(operation)) //$NON-NLS-1$ //$NON-NLS-2$
            && !arguments.has("event") && arguments.has("event_name")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            arguments.add("event", arguments.remove("event_name")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ("addFormCommand".equals(operation)) //$NON-NLS-1$
        {
            if (!arguments.has("name") && arguments.has("command_name")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                arguments.add("name", arguments.remove("command_name")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            arguments.remove("form_type"); //$NON-NLS-1$
        }
        if ("addFormButton".equals(operation) && !arguments.has("name") && arguments.has("command_name")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            arguments.add("name", arguments.get("command_name")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ("removeFormAttribute".equals(operation) && !arguments.has("name") //$NON-NLS-1$ //$NON-NLS-2$
            && arguments.has("property_name")) //$NON-NLS-1$
        {
            arguments.add("name", arguments.remove("property_name")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ("addFormField".equals(operation) && !arguments.has("data_path") && arguments.has("dataPath")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            arguments.add("data_path", arguments.remove("dataPath")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ("addFormField".equals(operation) && !arguments.has("name") && arguments.has("data_path")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            var dataPath = arguments.get("data_path").getAsString(); //$NON-NLS-1$
            var separator = dataPath.lastIndexOf('.');
            var inferredName = separator >= 0 ? dataPath.substring(separator + 1) : dataPath;
            if (!inferredName.isBlank())
            {
                arguments.addProperty("name", inferredName); //$NON-NLS-1$
            }
        }
    }

    /**
     * Serializes all 1C metadata operations in submission order. Static so that a single queue is used
     * regardless of how many tool instances Guice creates.
     */
    private static final java.util.concurrent.ExecutorService SEQUENTIAL_EXECUTOR =
        java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
            var thread = new Thread(runnable, "1c-edit-metadata"); //$NON-NLS-1$
            thread.setDaemon(true);
            return thread;
        });

    private Object help(String topic)
    {
        if (topic == null || topic.isBlank())
        {
            var result = new LinkedHashMap<String, Object>();
            result.put("tool", TOOL_NAME); //$NON-NLS-1$
            var operations = new ArrayList<Object>();
            for (var descriptor : registry.all())
            {
                var item = new LinkedHashMap<String, Object>();
                item.put("operation", descriptor.name); //$NON-NLS-1$
                item.put("description", descriptor.description); //$NON-NLS-1$
                operations.add(item);
            }
            result.put("operations", operations); //$NON-NLS-1$
            result.put("next", "Call operation=help with topic=<operation> before the first mutation."); //$NON-NLS-1$ //$NON-NLS-2$
            return result;
        }

        if ("objectTypes".equals(topic)) //$NON-NLS-1$
        {
            var result = new LinkedHashMap<String, Object>();
            var types = new ArrayList<Object>();
            for (var type : MetadataObjectTypeRegistry.all())
            {
                var item = new LinkedHashMap<String, Object>();
                item.put("name", type.name); //$NON-NLS-1$
                item.put("resource_folder", type.folder); //$NON-NLS-1$
                item.put("external_project", type.external); //$NON-NLS-1$
                types.add(item);
            }
            result.put("topic", topic); //$NON-NLS-1$
            result.put("count", types.size()); //$NON-NLS-1$
            result.put("object_types", types); //$NON-NLS-1$
            return result;
        }

        var descriptor = registry.get(topic);
        if (descriptor == null)
        {
            var result = new LinkedHashMap<String, Object>();
            result.put("error", "Unknown help topic `" + topic + "`."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            var validTopics = new ArrayList<String>(registry.names());
            validTopics.add("objectTypes"); //$NON-NLS-1$
            result.put("valid_topics", validTopics); //$NON-NLS-1$
            result.put("next", "Choose one exact value from valid_topics and call help again."); //$NON-NLS-1$ //$NON-NLS-2$
            return result;
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("operation", descriptor.name); //$NON-NLS-1$
        result.put("description", descriptor.description); //$NON-NLS-1$
        result.put("required_parameters", descriptor.requiredParameters); //$NON-NLS-1$
        result.put("optional_parameters", descriptor.optionalParameters); //$NON-NLS-1$
        result.put("example", descriptor.example); //$NON-NLS-1$
        return result;
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;
        spec.function.description = "Creates and modifies 1C metadata through guarded native EDT operations."
            + " It never executes model-generated Java code and never edits .mdo/.form files directly."
            + " Never use file editing tools to create or repair metadata; remove and recreate a wrong child instead."
            + " Operations: " + String.join(", ", new MetadataOperationRegistry().names()) + "."
            + " createObject supports every top-level object type returned by help topic=objectTypes."
            + " The content of an existing form is editable too: inspectForm reads its attributes,"
            + " commands and item tree, and addFormAttribute/addFormField/addFormGroup/addFormButton/"
            + "addFormCommand/addFormEventHandler/moveFormItem/setFormItemProperty change it. Those"
            + " operations take the form FQN in object_name (Catalog.Products.Form.ItemForm, or"
            + " CommonForm.Settings). addFormEventHandler wires an event (OnChange, OnCreateAtServer, ...)"
            + " on the form itself or on one of its fields/tables to a module procedure; a form command"
            + " (addFormCommand) is for a button/menu action instead, not a field event."
            + " Never try to edit a Form.form file with Write or Edit: EDT locks it, and these operations"
            + " are the supported way in."
            + " A multilingual property (synonym, title, listPresentation, objectPresentation,"
            + " explanation) is set with setObjectProperty/setChildProperty/setFormItemProperty like any"
            + " other property; language_code picks the language and defaults to ru."
            + " For an attribute inside a tabular section use addTabularSectionAttribute with"
            + " object_name=Type.Object.TabularSection; addObjectAttribute is only for a top-level object."
            + " Call with operation=help to list operations, then operation=help and topic=<operation>"
            + " for its exact parameters."
            + " Each mutation auto-checks itself: the response carries marker_count {errors, warnings, infos, total}"
            + " and the most important markers of the changed resource, so a separate GetMarkers call for that file"
            + " is unnecessary (use GetMarkers for whole-project checks, passing marker_path verbatim, never shortened"
            + " to a directory). Never report success while marker_count.errors is above zero: fix the reported"
            + " markers first. markers_incomplete=true means the list is truncated or validation had not settled,"
            + " so re-check with GetMarkers. Pass verify=false to skip this check during bulk work."
            + " Successful object/form/template create and remove operations already verify physical persistence;"
            + " do not call Glob or Read to re-check their files."
            + " This tool never touches .bsl files. For BSL code modules call listModules to get the exact .bsl path"
            + " of every module kind the object supports, then create that file with Write, change its text with"
            + " Edit, and remove it with Delete: a 1C module is its .bsl file, and those tools enforce the"
            + " vendor-support rules of the owning object. A module needs no entry in the .mdo, but its owner must"
            + " exist first: a common module is created with createObject CommonModule.Name (its execution context"
            + " is then set with setObjectProperty), and a form module requires createObjectForm."
            + " A configuration or an individual object may be on vendor support; when a support rule forbids the"
            + " change this tool refuses it, and only the user can lift that restriction, so do not retry."
            + " When creating or editing a configuration and the user did not state its parameters"
            + " (platform_version, compatibility_mode, script_variant, language, version, vendor), ask for them"
            + " with the AskUser tool before proceeding instead of guessing."
            + " Several operations may be requested in one message: they are applied one at a time, in the order"
            + " given, so a dependent sequence (create a tabular section, then add its attributes) is safe."
            + " Unknown operations and parameters are rejected.";

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        parameters.properties = new HashMap<>();
        property(parameters, "operation", "string", "Operation name. Start with `help`.");
        property(parameters, "topic", "string", "Operation name for detailed help.");
        property(parameters, "project_name", "string", "Exact EDT workspace project name.");
        property(parameters, "object_name", "string", "Top-level FQN such as Catalog.Products. For a tabular-section attribute: Type.Object.Section.");
        property(parameters, "name", "string", "Identifier of the element being created or addressed: an attribute, tabular section, enum value, register field, form, template, form item, form attribute or form command. Always required by those operations, and never interchangeable with `title` — `title` is only the display text.");
        property(parameters, "new_name", "string", "New top-level object name for renameObject.");
        property(parameters, "title", "string", "Display text (synonym) of a new object or child, in the language given by language_code. Never a substitute for `name`: pass both.");
        property(parameters, "property_name", "string", "Scalar EDT metadata property name.");
        property(parameters, "property_value", "string", "Scalar property value encoded as a string.");
        property(parameters, "type", "string", "String, Number, Boolean, Date, or a reference: CatalogRef.X, DocumentRef.X, EnumRef.X, ChartOfCharacteristicTypesRef.X, ChartOfAccountsRef.X, ChartOfCalculationTypesRef.X, BusinessProcessRef.X, TaskRef.X, ExchangePlanRef.X. The referenced object must already exist.");
        property(parameters, "length", "integer", "String length or total number digits.");
        property(parameters, "precision", "integer", "Number digits after the decimal point. For Number(N.S), pass length=N and precision=S.");
        property(parameters, "date_fractions", "string", "Date, Time, or DateTime.");
        property(parameters, "field_kind", "string", "Register field kind: dimension, resource, or attribute.");
        property(parameters, "child_kind", "string", "Child kind: object_attribute, tabular_section, tabular_section_attribute, enum_value, dimension, resource, register_attribute, form, or template.");
        property(parameters, "related_object_name", "string", "FQN of a related object, for example AccumulationRegister.Stock.");
        property(parameters, "form_type", "string", "Generated form type. Common: OBJECT (object form), LIST (list form), FOLDER (group form), CHOICE (choice form), FOLDER_CHOICE, RECORD (record form), RECORD_SET (record set form), REPORT, CONSTANTS, GENERIC (arbitrary form). Also supported: SEARCH, REPORT_SETTINGS, REPORT_VARIANT, SAVE, LOAD, DYNAMIC_LIST, CHANGE_HISTORY, VERSION_DATA, VERSION_DIFFERENCES. Pick the type matching the owner: a catalog with groups supports FOLDER and FOLDER_CHOICE, a register supports RECORD_SET and LIST.");
        property(parameters, "template_type", "string", "Template body type. Creatable empty: SPREADSHEET_DOCUMENT (mxl layout), DATA_COMPOSITION_SCHEMA, DATA_COMPOSITION_APPEARANCE_TEMPLATE, HTML_DOCUMENT. The response reports the created body file in details.body_path; an HTML body can then be filled with the Edit tool. TEXT_DOCUMENT, BINARY_DATA, ADD_IN, ACTIVE_DOCUMENT, GRAPHICAL_SCHEMA and GEOGRAPHICAL_SCHEMA wrap external content and cannot be created empty.");
        property(parameters, "begin_time", "string", "Daily scheduled-job start time in 24-hour HH:mm format, for example 07:00.");
        property(parameters, "days_repeat_period", "integer", "Number of days between scheduled-job runs; defaults to 1.");
        property(parameters, "platform_version", "string", "Runtime 1C platform version of a new configuration, for example 8.3.24.");
        property(parameters, "compatibility_mode", "string", "Configuration compatibility mode, for example 8.3.24. Defaults to platform_version.");
        property(parameters, "script_variant", "string", "Built-in language variant of a new configuration: English or Russian.");
        property(parameters, "default_language_code", "string", "Interface language code of a new configuration, for example ru or en. Creates the language and marks it default.");
        property(parameters, "default_language_name", "string", "Name of the default language object. Optional; defaults from default_language_code (ru -> Русский, en -> English).");
        property(parameters, "version", "string", "Configuration version string, for example 1.0.0.1.");
        property(parameters, "vendor", "string", "Configuration vendor name.");
        property(parameters, "data_path", "string", "Form data path a field binds to: a form attribute name, or a path inside it such as Объект.Наименование. inspectForm lists the valid paths.");
        property(parameters, "parent", "string", "Name of the form group that receives a new or moved item. Omit for the form root.");
        property(parameters, "position", "integer", "Zero-based index inside the parent container. Omit to append.");
        property(parameters, "item_type", "string", "Concrete form field kind: InputField, CheckBoxField, LabelField, PictureField, SpreadsheetDocumentField, CalendarField, ChartField, and so on. Omit to let EDT derive it from the data path.");
        property(parameters, "group_type", "string", "Form group kind: UsualGroup, Pages, Page, CommandBar, ButtonGroup, ColumnGroup, Popup. A Page must be added inside a Pages group.");
        property(parameters, "command_name", "string", "Name of the existing form command a button runs.");
        property(parameters, "handler", "string", "Name of the form-module procedure a form command or event handler calls. Defaults to the command name for addFormCommand, or to <name><event in Russian> for addFormEventHandler.");
        property(parameters, "event", "string", "Event name (English or Russian) for addFormEventHandler/removeFormEventHandler, for example OnChange or ПриИзменении. inspectForm lists the handlers already wired; a wrong value is rejected with the exact list this form/field/table supports.");
        property(parameters, "language_code", "string", "Language code of a multilingual property value such as synonym, title, listPresentation or objectPresentation. Defaults to ru.");
        property(parameters, "main", "boolean", "Marks a new form attribute as the form's main attribute.");
        property(parameters, "dry_run", "boolean", "Validate and describe the mutation without applying it.");
        property(parameters, "verify", "boolean", "Post-mutation marker auto-check. Enabled by default: the response carries marker_count and the most important markers of the changed resource. Pass false only for bulk work where you will check markers yourself afterwards.");
        parameters.required = Arrays.asList("operation");
        spec.function.parameters = parameters;
        return spec;
    }

    private static void property(McpToolCallParameters parameters, String name, String type, String description)
    {
        var property = new McpToolCallProperty();
        property.type = type;
        property.description = description;
        parameters.properties.put(name, property);
    }
}
