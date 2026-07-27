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

        var validationErrors = registry.validate(arguments);
        if (!validationErrors.isEmpty())
        {
            throw new ToolException(String.join("\n", validationErrors)); //$NON-NLS-1$
        }
        var request = json.deserialize(call.function.arguments, MetadataRequest.class)
            .orElseThrow(() -> new ToolException("Cannot deserialize metadata operation arguments.")); //$NON-NLS-1$

        return CompletableFuture.supplyAsync(() -> {
            Object result = "help".equals(request.operation) ? help(request.topic) //$NON-NLS-1$
                : mutationService.execute(request, cancellationToken);
            details.responseMarkdown = "1C metadata operation completed"; //$NON-NLS-1$
            return messageFactory.createMessage(this, call, json.serialize(result), details);
        });
    }

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
            + " For BSL code modules use listModules to discover an object's module kinds and .bsl paths,"
            + " createModule to add an empty module, and removeModule to delete one; edit the BSL text itself"
            + " with the Edit tool at the returned resource_path, never through this tool."
            + " When creating or editing a configuration and the user did not state its parameters"
            + " (platform_version, compatibility_mode, script_variant, language, version, vendor), ask for them"
            + " with the AskUser tool before proceeding instead of guessing."
            + " Unknown operations and parameters are rejected.";

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        parameters.properties = new HashMap<>();
        property(parameters, "operation", "string", "Operation name. Start with `help`.");
        property(parameters, "topic", "string", "Operation name for detailed help.");
        property(parameters, "project_name", "string", "Exact EDT workspace project name.");
        property(parameters, "object_name", "string", "Top-level FQN such as Catalog.Products. For a tabular-section attribute: Type.Object.Section.");
        property(parameters, "name", "string", "Name of a child metadata element.");
        property(parameters, "new_name", "string", "New top-level object name for renameObject.");
        property(parameters, "title", "string", "Russian synonym of a new object or child.");
        property(parameters, "property_name", "string", "Scalar EDT metadata property name.");
        property(parameters, "property_value", "string", "Scalar property value encoded as a string.");
        property(parameters, "type", "string", "String, Number, Boolean, Date, CatalogRef.X, DocumentRef.X, or EnumRef.X.");
        property(parameters, "length", "integer", "String length or total number digits.");
        property(parameters, "precision", "integer", "Number digits after the decimal point. For Number(N.S), pass length=N and precision=S.");
        property(parameters, "date_fractions", "string", "Date, Time, or DateTime.");
        property(parameters, "field_kind", "string", "Register field kind: dimension, resource, or attribute.");
        property(parameters, "child_kind", "string", "Child kind: object_attribute, tabular_section, tabular_section_attribute, enum_value, dimension, resource, register_attribute, form, or template.");
        property(parameters, "related_object_name", "string", "FQN of a related object, for example AccumulationRegister.Stock.");
        property(parameters, "form_type", "string", "Generated form type. Common: OBJECT (object form), LIST (list form), FOLDER (group form), CHOICE (choice form), FOLDER_CHOICE, RECORD (record form), RECORD_SET (record set form), REPORT, CONSTANTS, GENERIC (arbitrary form). Also supported: SEARCH, REPORT_SETTINGS, REPORT_VARIANT, SAVE, LOAD, DYNAMIC_LIST, CHANGE_HISTORY, VERSION_DATA, VERSION_DIFFERENCES. Pick the type matching the owner: a catalog with groups supports FOLDER and FOLDER_CHOICE, a register supports RECORD_SET and LIST.");
        property(parameters, "template_type", "string", "Template body type. Creatable empty: SPREADSHEET_DOCUMENT (mxl layout), DATA_COMPOSITION_SCHEMA, DATA_COMPOSITION_APPEARANCE_TEMPLATE, HTML_DOCUMENT. The response reports the created body file in details.body_path; an HTML body can then be filled with the Edit tool. TEXT_DOCUMENT, BINARY_DATA, ADD_IN, ACTIVE_DOCUMENT, GRAPHICAL_SCHEMA and GEOGRAPHICAL_SCHEMA wrap external content and cannot be created empty.");
        property(parameters, "module_kind", "string", "BSL code module kind: object_module, manager_module, record_set_module, value_manager_module, command_module, module, managed_application_module, ordinary_application_module, external_connection_module, or session_module. Use object_name=Configuration for application-level modules.");
        property(parameters, "platform_version", "string", "Runtime 1C platform version of a new configuration, for example 8.3.24.");
        property(parameters, "compatibility_mode", "string", "Configuration compatibility mode, for example 8.3.24. Defaults to platform_version.");
        property(parameters, "script_variant", "string", "Built-in language variant of a new configuration: English or Russian.");
        property(parameters, "default_language_code", "string", "Interface language code of a new configuration, for example ru or en. Creates the language and marks it default.");
        property(parameters, "default_language_name", "string", "Name of the default language object. Optional; defaults from default_language_code (ru -> Русский, en -> English).");
        property(parameters, "version", "string", "Configuration version string, for example 1.0.0.1.");
        property(parameters, "vendor", "string", "Configuration vendor name.");
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
