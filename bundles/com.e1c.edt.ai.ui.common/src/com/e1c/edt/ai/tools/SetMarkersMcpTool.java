/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class SetMarkersMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "SetMarkers"; //$NON-NLS-1$
    public static final String ACTION_CALL_ATTRIBUTE = "action_call"; //$NON-NLS-1$
    public static final String ACTION_DETAILS_ATTRIBUTE = "action_details"; //$NON-NLS-1$
    public static final String QUICK_FIX_ID = "com.e1c.edt.ai.ui.commands.applyprompt.ai"; //$NON-NLS-1$
    public static final String QUICK_FIX_ATTRIBUTE = "org.eclipse.ui.workbench.texteditor.quickFixId"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  \"markers\": [\n"
        + "    {\n"
        + "      \"type\": \"bookmark\",\n"
        + "      \"relative_file_path\": \"Forms/MyForm/Module.bsl\",\n"
        + "      \"line\": 10,\n"
        + "      \"message\": \"Important code section\",\n"
        + "      \"location\": \"Form Module\",\n"
        + "      \"char_start\": 100,\n"
        + "      \"char_end\": 150\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"task\",\n"
        + "      \"relative_file_path\": \"CommonModules/MyModule/Module.bsl\",\n"
        + "      \"line\": 25,\n"
        + "      \"message\": \"TODO: Refactor this code\",\n"
        + "      \"priority\": \"normal\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"problem\",\n"
        + "      \"relative_file_path\": \"CommonModules/AnotherModule/Module.bsl\",\n"
        + "      \"line\": 42,\n"
        + "      \"message\": \"Syntax error\",\n"
        + "      \"severity\": \"error\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"text\",\n"
        + "      \"relative_file_path\": \"CommonModules/TextModule/Module.bsl\",\n"
        + "      \"line\": 15,\n"
        + "      \"message\": \"Important text note\",\n"
        + "      \"char_start\": 50,\n"
        + "      \"char_end\": 100\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"ai_marker\",\n"
        + "      \"relative_file_path\": \"CommonModules/AIModule/Module.bsl\",\n"
        + "      \"line\": 30,\n"
        + "      \"message\": \"AI suggestion\",\n"
        + "      \"severity\": \"info\",\n"
        + "      \"action_prompt\": \"Please refactor this code to use modern patterns\",\n"
        + "      \"action_title\": \"Refactor code\",\n"
        + "      \"action_description\": \"Update code to use modern design patterns\"\n"
        + "    }\n"
        + "  ]\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample = "Successfully created 5 markers";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public SetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory)
    {
        this.json = Preconditions.checkNotNull(json);
        this.messageFactory = Preconditions.checkNotNull(messageFactory);
        this.spec = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return true;
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
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture.completedFuture(
                messageFactory.createError(this, call, "Invalid request format. Example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        if (request.projectName == null || request.projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Project name is required"));
        }

        if (request.markers == null || request.markers.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "At least one marker is required"));
        }

        return CompletableFuture.supplyAsync(() -> {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject project = root.getProject(request.projectName);
            if (!project.exists())
            {
                return messageFactory.createError(this, call, "Project not found: " + request.projectName);
            }

            if (!project.isOpen())
            {
                return messageFactory.createError(this, call, "Project is closed: " + request.projectName);
            }

            var markersSet = 0;
            var errors = new StringBuilder();
            for (int i = 0; i < request.markers.size(); i++)
            {
                if (cancellationToken.isCanceled())
                {
                    errors.append("Operation cancelled after setting ").append(markersSet).append(" markers");
                    break;
                }

                var markerReq = request.markers.get(i);
                markerReq.projectName = project.getName();
                try
                {
                    createMarker(project, call, markerReq);
                    markersSet++;
                }
                catch (CoreException | IllegalArgumentException e)
                {
                    errors.append("Marker [").append(i).append("] error: ").append(e.getMessage()).append("; ");
                }
            }

            if (errors.length() > 0)
            {
                return messageFactory.createError(this, call,
                    "Completed with errors: " + errors + ". Markers set: " + markersSet);
            }

            return messageFactory.createMessage(this, call, "Successfully created " + markersSet + " markers");
        });
    }

    @SuppressWarnings("nls")
    private void createMarker(IProject project, McpToolCall call, MarkerRequest markerReq)
        throws CoreException, IllegalArgumentException
    {
        // Validate required fields
        if (markerReq.relativeFilePath == null || markerReq.relativeFilePath.isBlank())
        {
            throw new IllegalArgumentException("relative_file_path is required");
        }

        if (markerReq.message == null || markerReq.message.isBlank())
        {
            throw new IllegalArgumentException("message is required");
        }

        if (markerReq.type == null || markerReq.type.isBlank())
        {
            throw new IllegalArgumentException("type is required");
        }

        // Validate action_prompt for ai_marker
        if ("ai_marker".equalsIgnoreCase(markerReq.type))
        {
            if (markerReq.actionPrompt == null || markerReq.actionPrompt.isBlank())
            {
                throw new IllegalArgumentException("action_prompt is required for ai_marker");
            }

            if (markerReq.actionTitle == null || markerReq.actionTitle.isBlank())
            {
                throw new IllegalArgumentException("action_title is required for ai_marker");
            }

            if (markerReq.actionDescription == null || markerReq.actionDescription.isBlank())
            {
                throw new IllegalArgumentException("action_description is required for ai_marker");
            }
        }

        var relativePath = new Path(markerReq.relativeFilePath);
        var file = project.getFile(relativePath);
        if (!file.exists())
        {
            throw new IllegalArgumentException("File not found: " + relativePath);
        }

        // Convert request type to MarkerType enum
        var markerType = MarkerType.fromDisplayName(markerReq.type);
        if (markerType == null)
        {
            throw new IllegalArgumentException("Unknown marker type: " + markerReq.type);
        }

        var marker = file.createMarker(markerType.getTypeId());
        setMarkerAttributes(marker, call, markerReq, markerType);
    }

    private void setMarkerAttributes(IMarker marker, McpToolCall call, MarkerRequest markerReq, MarkerType markerType)
        throws CoreException
    {
        // Common attributes for all marker types
        marker.setAttribute(IMarker.MESSAGE, markerReq.message);
        if (markerReq.line > 0)
        {
            marker.setAttribute(IMarker.LINE_NUMBER, markerReq.line);
        }

        // Location (generate if not provided)
        String location = markerReq.location != null ? markerReq.location : "Line " + markerReq.line; //$NON-NLS-1$
        marker.setAttribute(IMarker.LOCATION, location);
        // Character positions
        if (markerReq.charStart != null && markerReq.charEnd != null)
        {
            marker.setAttribute(IMarker.CHAR_START, markerReq.charStart);
            marker.setAttribute(IMarker.CHAR_END, markerReq.charEnd);
        }

        // Action attributes - only for AI markers
        if (markerType == MarkerType.AI_MARKER && markerReq.actionPrompt != null && !markerReq.actionPrompt.isBlank())
        {
            marker.setAttribute(ACTION_CALL_ATTRIBUTE, call);
            marker.setAttribute(ACTION_DETAILS_ATTRIBUTE, markerReq);
            marker.setAttribute(QUICK_FIX_ATTRIBUTE, QUICK_FIX_ID);
        }

        // Type-specific attributes
        switch (markerType)
        {
        case BOOKMARK:
            setBooleanAttribute(marker, IMarker.DONE, markerReq.done);
            setBooleanAttribute(marker, IMarker.TRANSIENT, markerReq.transientFlag);
            setBooleanAttribute(marker, IMarker.USER_EDITABLE, markerReq.userEditable);
            if (markerReq.sourceId != null)
            {
                marker.setAttribute(IMarker.SOURCE_ID, markerReq.sourceId);
            }
            break;
        case TASK:
            setBooleanAttribute(marker, IMarker.DONE, markerReq.done);
            setBooleanAttribute(marker, IMarker.USER_EDITABLE, markerReq.userEditable);
            if (markerReq.priority != null)
            {
                marker.setAttribute(IMarker.PRIORITY, convertPriority(markerReq.priority));
            }
            break;
        case PROBLEM:
        case AI_MARKER:
            if (markerReq.severity != null)
            {
                marker.setAttribute(IMarker.SEVERITY, convertSeverity(markerReq.severity));
            }
            if (markerReq.priority != null)
            {
                marker.setAttribute(IMarker.PRIORITY, convertPriority(markerReq.priority));
            }
            break;
        default:
            // No additional attributes for other types
            break;
        }
    }

    private void setBooleanAttribute(IMarker marker, String attr, Boolean value) throws CoreException
    {
        if (value != null)
        {
            marker.setAttribute(attr, value);
        }
    }

    @SuppressWarnings("nls")
    private int convertSeverity(String severity)
    {
        if (severity == null)
            return IMarker.SEVERITY_INFO;
        switch (severity.toLowerCase())
        {
        case "error":
            return IMarker.SEVERITY_ERROR;
        case "warning":
            return IMarker.SEVERITY_WARNING;
        default:
            return IMarker.SEVERITY_INFO;
        }
    }

    @SuppressWarnings("nls")
    private int convertPriority(String priority)
    {
        if (priority == null)
            return IMarker.PRIORITY_NORMAL;
        switch (priority.toLowerCase())
        {
        case "high":
            return IMarker.PRIORITY_HIGH;
        case "low":
            return IMarker.PRIORITY_LOW;
        default:
            return IMarker.PRIORITY_NORMAL;
        }
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;
        var description = new StringBuilder();
        description.append("Creates various types of markers in project files. ");
        description.append("\n\nUsage:");
        description.append(
            "\n- ALWAYS use `" + MarkerType.AI_MARKER.getDisplayName()
                + "` to show any issues, problems, errors, warnings, etc.");
        description.append(
            "\n- ALWAYS use `" + MarkerType.TASK.getDisplayName()
                + "` for plans, schedules, proposals, tasks, TODO, etc.");
        description.append("\n- ALWAYS use `" + MarkerType.BOOKMARK.getDisplayName() + "` for summaries, reports.");
        description.append("\n\nSupported marker types:");
        // Generate supported types from MarkerType enum
        for (MarkerType type : MarkerType.values())
        {
            description.append("\n- ").append(type.getDisplayName()).append(": ").append(type.getDescription());
        }
        description.append("\n\nCommon properties for all markers:");
        description.append("\n- type: Marker type (required)");
        description.append("\n- relative_file_path: File path relative to project root (required)");
        description.append("\n- message: Marker description (required)");
        description.append(
            "\n- line: Line number (required). An integer value indicating the line number for a marker. It is 1-relative.");
        description.append(
            "\n- location: Human-readable location string (optional). The location is a human-readable (localized) string which can be used to distinguish between markers on a resource. As such it should be concise and aimed at users.");
        description.append(
            "\n- char_start: Character start offset (optional). An integer value indicating where a marker starts. It is zero-relative and inclusive.");
        description.append(
            "\n- char_end: Character end offset (optional). An integer value indicating where a marker ends. It is zero-relative and exclusive.");
        description.append(
            "\n- action_prompt: AI prompt to execute when marker is activated (required for ai_marker)");
        description.append(
            "\n- action_title: Short title for the quick fix action (required for ai_marker)");
        description.append(
            "\n- action_description: Detailed description of the quick fix action (required for ai_marker)");
        description.append("\n\nType-specific properties:");
        description.append("\n- bookmark: done, transient, user_editable, source_id");
        description.append("\n- task: done, user_editable, priority");
        description.append("\n- problem: severity, priority");
        description.append("\n- ai_marker: severity, priority");
        description.append("\n\nQuick fix actions:");
        description
            .append("\n- For `ai_marker` with `action_prompt`: adds quick fix action prompt");
        description.append("\n\nExample request:\n").append(QuestionExample);
        description.append("\nExample response:\n").append(AnswerExample);
        spec.function.description = description.toString();
        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();
        // Project name
        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "Target project name";
        properties.put("project_name", projectNameProp);
        // Markers array
        var markersProp = new McpToolCallProperty();
        markersProp.type = "array";
        markersProp.description = "List of markers to create";
        properties.put("markers", markersProp);
        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "markers");
        spec.function.parameters = parameters;
        return spec;
    }

    private static class Request
    {
        @SerializedName("project_name")
        public String projectName;

        @SerializedName("markers")
        public List<MarkerRequest> markers;
    }

    public class MarkerRequest
    {
        @SerializedName("type")
        public String type;

        @SerializedName("project_name")
        public String projectName;

        @SerializedName("relative_file_path")
        public String relativeFilePath;

        @SerializedName("message")
        public String message;

        @SerializedName("line")
        public int line;

        @SerializedName("severity")
        public String severity;

        @SerializedName("priority")
        public String priority;

        @SerializedName("char_start")
        public Integer charStart;

        @SerializedName("char_end")
        public Integer charEnd;

        @SerializedName("location")
        public String location;

        @SerializedName("done")
        public Boolean done;

        @SerializedName("transient")
        public Boolean transientFlag;

        @SerializedName("user_editable")
        public Boolean userEditable;

        @SerializedName("source_id")
        public String sourceId;

        @SerializedName("action_prompt")
        public String actionPrompt;

        @SerializedName("action_title")
        public String actionTitle;

        @SerializedName("action_description")
        public String actionDescription;
    }
}