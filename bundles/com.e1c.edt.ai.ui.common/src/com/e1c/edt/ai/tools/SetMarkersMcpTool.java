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
    public static final String AI_MARKER_TYPE = "com.e1c.edt.ai.marker"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  \"markers\": [\n"
        + "    {\n"
        + "      \"severity\": \"bookmark\",\n"
        + "      \"relative_file_path\": \"Forms/MyForm/Module.bsl\",\n"
        + "      \"line\": 10,\n"
        + "      \"message\": \"Important code section\",\n"
        + "      \"location\": \"Form Module\",\n"
        + "      \"char_start\": 100,\n"
        + "      \"char_end\": 150\n"
        + "    },\n"
        + "    {\n"
        + "      \"severity\": \"info\",\n"
        + "      \"relative_file_path\": \"CommonModules/MyModule/Module.bsl\",\n"
        + "      \"line\": 25,\n"
        + "      \"message\": \"AI: Unused variable detected\",\n"
        + "      \"priority\": \"low\"\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\"success\": true, \"markers_set\": 2, \"bookmarks\": 1, \"ai_markers\": 1}";
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

            int markersSet = 0;
            int bookmarksSet = 0;
            int aiMarkersSet = 0;
            StringBuilder errors = new StringBuilder();

            for (int i = 0; i < request.markers.size(); i++)
            {
                if (cancellationToken.isCanceled())
                {
                    errors.append("Operation cancelled after setting ").append(markersSet).append(" markers");
                    break;
                }

                var markerReq = request.markers.get(i);
                try
                {
                    String markerType = createMarker(project, markerReq);
                    markersSet++;

                    if (IMarker.BOOKMARK.equals(markerType))
                    {
                        bookmarksSet++;
                    }
                    else if (AI_MARKER_TYPE.equals(markerType))
                    {
                        aiMarkersSet++;
                    }
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

            var response = new Response();
            response.success = true;
            response.markersSet = markersSet;
            response.bookmarksSet = bookmarksSet;
            response.aiMarkersSet = aiMarkersSet;

            return messageFactory.createMessage(this, call, json.serialize(response));
        });
    }

    @SuppressWarnings("nls")
    private String createMarker(IProject project, MarkerRequest markerReq)
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
        if (markerReq.severity == null || markerReq.severity.isBlank())
        {
            throw new IllegalArgumentException("severity is required");
        }

        var relativePath = new Path(markerReq.relativeFilePath);
        var file = project.getFile(relativePath);

        if (!file.exists())
        {
            throw new IllegalArgumentException("File not found: " + relativePath);
        }

        // Determine marker type based on severity
        String markerType;
        if ("bookmark".equalsIgnoreCase(markerReq.severity))
        {
            markerType = IMarker.BOOKMARK;
        }
        else
        {
            markerType = AI_MARKER_TYPE;
        }

        var marker = file.createMarker(markerType);
        setMarkerAttributes(marker, markerReq, markerType);
        return markerType;
    }

    private void setMarkerAttributes(IMarker marker, MarkerRequest markerReq, String markerType) throws CoreException
    {
        // Common attributes for all marker types
        marker.setAttribute(IMarker.MESSAGE, markerReq.message);

        if (markerReq.line > 0)
        {
            marker.setAttribute(IMarker.LINE_NUMBER, markerReq.line);
        }

        // Location (generate if not provided)
        var location = markerReq.location != null ? markerReq.location : "Line " + markerReq.line; //$NON-NLS-1$
        marker.setAttribute(IMarker.LOCATION, location);

        // Character positions
        if (markerReq.charStart != null && markerReq.charEnd != null)
        {
            marker.setAttribute(IMarker.CHAR_START, markerReq.charStart);
            marker.setAttribute(IMarker.CHAR_END, markerReq.charEnd);
        }

        // Bookmark specific attributes
        if (markerType.equals(IMarker.BOOKMARK))
        {
            setBooleanAttribute(marker, IMarker.DONE, markerReq.done);
            setBooleanAttribute(marker, IMarker.TRANSIENT, markerReq.transientFlag);
            setBooleanAttribute(marker, IMarker.USER_EDITABLE, markerReq.userEditable);

            if (markerReq.sourceId != null)
            {
                marker.setAttribute(IMarker.SOURCE_ID, markerReq.sourceId);
            }
        }
        // AI marker specific attributes
        else
        {
            // Severity and priority are required for AI markers
            marker.setAttribute(IMarker.SEVERITY, convertSeverity(markerReq.severity));

            if (markerReq.priority != null)
            {
                marker.setAttribute(IMarker.PRIORITY, convertPriority(markerReq.priority));
            }
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
        description.append("Creates markers and bookmarks in project files");
        description.append("\n\nUsage:");
        description.append("\n- Use the `GetMarkers` tool to get current markers.");
        description.append("\n- Use the `ClearMarkers` tool to clean current markers.");
        description.append("\n\nParameters:");
        description.append("\n- project_name: Target project name (required)");
        description.append("\n- markers: List of markers to create (required)\n");
        description.append("\nCommon marker properties:");
        description.append("\n- relative_file_path: File path relative to project root (required)");
        description.append("\n- message: Marker description (required)");
        description.append("\n- severity: Determines marker type (required)");
        description.append("\n- line: Line number (1-based, recommended)");
        description.append("\n- location: Human-readable location string (optional)");
        description.append("\n- char_start: Character start offset (optional, 0-based)");
        description.append("\n- char_end: Character end offset (optional, 0-based)");
        description.append("\n\nSeverity values:");
        description.append("\n- 'bookmark': Creates a standard bookmark");
        description.append("\n- 'error', 'warning', 'info': Creates AI marker with specified severity");
        description.append("\n\nAI Marker specific properties:");
        description.append("\n- priority: `high`, `normal`, or `low` (optional)");
        description.append("\n\nBookmark specific properties:");
        description.append("\n- done: Completion status (optional)");
        description.append("\n- transient: Persistence flag (optional)");
        description.append("\n- user_editable: Edit permission (optional)");
        description.append("\n- source_id: Marker source identifier (optional)");
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

    // Request DTO for JSON deserialization
    private static class Request
    {
        @SerializedName("project_name")
        public String projectName;

        @SerializedName("markers")
        public List<MarkerRequest> markers;
    }

    private static class MarkerRequest
    {
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        @SerializedName("message")
        public String message;

        @SerializedName("severity")
        public String severity;

        @SerializedName("line")
        public int line;

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
    }

    private static class Response
    {
        @SerializedName("success")
        public boolean success;

        @SerializedName("markers_set")
        public int markersSet;

        @SerializedName("bookmarks")
        public int bookmarksSet;

        @SerializedName("ai_markers")
        public int aiMarkersSet;
    }
}