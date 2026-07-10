/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.assistent.model.VisualField;
import com.e1c.edt.ai.assistent.model.VisualSnapshot;
import com.e1c.edt.ai.assistent.model.VisualWindow;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

/**
 * Returns everything the user currently sees in the IDE: all open windows/dialogs with their
 * controls, focus and selection, the active editor viewport and the clipboard.
 */
public class GetVisualContextMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "GetVisualContext"; //$NON-NLS-1$

    static final String SCOPE_ALL = "all"; //$NON-NLS-1$
    static final String SCOPE_FOCUSED_WINDOW = "focused_window"; //$NON-NLS-1$
    static final int DEFAULT_MAX_FIELD_LENGTH = 2000;

    // @formatter:off
    @SuppressWarnings("nls")
    private static final String AnswerExample =
        "{\n"
        + "  \"windows\": [\n"
        + "    {\n"
        + "      \"title\": \"New Data Processor\",\n"
        + "      \"is_active\": true,\n"
        + "      \"is_modal\": true,\n"
        + "      \"is_dialog\": true,\n"
        + "      \"fields\": [\n"
        + "        { \"name\": \"Name\", \"value\": \"ReturnProcessing\", \"kind\": \"text\", \"is_focused\": true,\n"
        + "          \"is_multiline\": false, \"selected_text\": \"Return\" },\n"
        + "        { \"name\": \"Use standard commands\", \"kind\": \"checkbox\", \"is_checked\": true },\n"
        + "        { \"name\": \"Type\", \"kind\": \"combo\", \"value\": \"External\", \"options\": [\"Internal\", \"External\"] }\n"
        + "      ],\n"
        + "      \"groups\": [\n"
        + "        { \"title\": \"Details\", \"fields\": [\n"
        + "          { \"kind\": \"table\", \"columns\": [\"Name\", \"Type\"],\n"
        + "            \"rows\": [[\"Customer\", \"CatalogRef\"], [\"Amount\", \"Number\"]],\n"
        + "            \"selected_text\": \"Customer | CatalogRef\" }\n"
        + "        ] }\n"
        + "      ]\n"
        + "    },\n"
        + "    { \"title\": \"MyWorkspace - 1C:EDT\", \"fields\": [] }\n"
        + "  ],\n"
        + "  \"active_editor\": {\n"
        + "    \"title\": \"Module.bsl\",\n"
        + "    \"path\": \"MyProject/src/Documents/Order/Module.bsl\",\n"
        + "    \"is_dirty\": true,\n"
        + "    \"visible_text\": \"Procedure Posting(Cancel, Mode)\\n    // ...\\nEndProcedure\",\n"
        + "    \"selected_text\": \"Cancel\",\n"
        + "    \"cursor_line\": 12,\n"
        + "    \"cursor_column\": 20\n"
        + "  },\n"
        + "  \"clipboard\": { \"text\": \"CopiedText\", \"path\": \"MyProject/src/CommonModules/Common/Module.bsl\" }\n"
        + "}";
    // @formatter:on

    private final IJson json;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IVisualContextProvider visualContextProvider;
    private final McpToolCallSpecification spec;

    @Inject
    public GetVisualContextMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IVisualContextProvider visualContextProvider)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(visualContextProvider);

        this.json = json;
        this.messageFactory = messageFactory;
        this.visualContextProvider = visualContextProvider;

        spec = createSpecification();
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return spec;
    }

    @SuppressWarnings("nls")
    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = true;
        details.hideAfter = true;
        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = Messages.VisualContextTitle;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var arguments = json.deserialize(call.function.arguments, Arguments.class).orElseGet(Arguments::new);

        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var snapshot = visualContextProvider.createSnapshot(cancellationToken);
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled during execution.");
            }

            applyScope(snapshot, arguments.scope);
            applyMaxFieldLength(snapshot, arguments.maxFieldLength);

            var content = json.serialize(snapshot);
            // details.responseMarkdown must stay null: McpToolInvoker prefers responseMarkdown over
            // content, so a human-readable summary here would shadow the JSON for skills.
            return messageFactory.createMessage(this, call, content, details);
        });
    }

    private void applyScope(VisualSnapshot snapshot, String scope)
    {
        if (snapshot.windows == null || !SCOPE_FOCUSED_WINDOW.equals(scope))
        {
            return;
        }

        snapshot.windows.removeIf(window -> !Boolean.TRUE.equals(window.isActive));
    }

    private void applyMaxFieldLength(VisualSnapshot snapshot, Integer maxFieldLength)
    {
        if (maxFieldLength == null || maxFieldLength <= 0 || maxFieldLength >= DEFAULT_MAX_FIELD_LENGTH
            || snapshot.windows == null)
        {
            return;
        }

        for (VisualWindow window : snapshot.windows)
        {
            truncateFields(window.fields, maxFieldLength);
            if (window.groups != null)
            {
                for (var group : window.groups)
                {
                    truncateFields(group.fields, maxFieldLength);
                }
            }
        }
    }

    private void truncateFields(List<VisualField> fields, int maxFieldLength)
    {
        if (fields == null)
        {
            return;
        }

        for (var field : fields)
        {
            field.value = truncateValue(field.value, field, maxFieldLength);
            field.selectedText = truncateValue(field.selectedText, field, maxFieldLength);
        }
    }

    private String truncateValue(String value, VisualField field, int maxFieldLength)
    {
        if (value == null || value.length() <= maxFieldLength)
        {
            return value;
        }

        field.isTruncated = Boolean.TRUE;
        return value.substring(0, maxFieldLength);
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Captures what the user currently SEES in the IDE and returns it as JSON.");
        description.append("\n\nThe snapshot follows the viewport principle - it reflects exactly what is on the"
            + " user's screen:");
        description.append("\n- `windows`: every open window/dialog (the active one first) with titles, labels,"
            + " field values, checkboxes (`is_checked`), combos with `options`, lists, tables/trees"
            + " (`columns` + only the rows currently visible to the user, selected rows always included),"
            + " tabs, links and toolbars.");
        description.append("\n- `is_focused`: true marks the control the user is working with;"
            + " `selected_text` carries the user's selection.");
        description.append("\n- `active_editor`: the active editor with `visible_text` (ONLY the part of the"
            + " document visible in the viewport, never the whole file), `selected_text` and the caret position"
            + " (`cursor_line`/`cursor_column`, 1-based).");
        description.append("\n- `clipboard`: text recently copied inside the IDE and the source file path.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object; all parameters are optional.");
        description.append("\n- Use this tool when the user refers to what they see on the screen"
            + " (\"this dialog\", \"the selected row\", \"fix this field\") or when you need the state of an"
            + " open dialog to fill or correct its fields.");
        description.append("\n- `is_truncated`: true means a value/list was cut by the capture limits.");
        description.append("\n\nExample output:");
        description.append("\n").append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var scopeProp = new McpToolCallProperty();
        scopeProp.type = "string";
        scopeProp.description = "\"" + SCOPE_ALL + "\" (default) - every open window/dialog; \"" + SCOPE_FOCUSED_WINDOW
            + "\" - only the window that has the focus.";
        properties.put("scope", scopeProp);

        var maxFieldLengthProp = new McpToolCallProperty();
        maxFieldLengthProp.type = "integer";
        maxFieldLengthProp.description =
            "Maximum length of a single field value in characters. Default is " + DEFAULT_MAX_FIELD_LENGTH + ".";
        properties.put("max_field_length", maxFieldLengthProp);

        parameters.properties = properties;
        parameters.required = List.of();
        spec.function.parameters = parameters;
        return spec;
    }

    private static class Arguments
    {
        @SerializedName("scope")
        public String scope;

        @SerializedName("max_field_length")
        public Integer maxFieldLength;
    }
}
