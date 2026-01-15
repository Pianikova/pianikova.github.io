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
import com.e1c.edt.ai.ILog;
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

    // Fully qualified marker type
    public static final String AI_MARKER_TYPE = "com.e1c.edt.ai.marker"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  \"markers\": [\n"
        + "    {\n"
        + "      \"relative_file_path\": \"Forms/MyForm/Module.bsl\",\n"
        + "      \"line\": 10,\n"
        + "      \"message\": \"AI: Possible optimization here\",\n"
        + "      \"severity\": \"warning\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"relative_file_path\": \"CommonModules/MyModule/Module.bsl\",\n"
        + "      \"line\": 25,\n"
        + "      \"message\": \"AI: Unused variable detected\",\n"
        + "      \"severity\": \"info\",\n"
        + "      \"priority\": \"low\"\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample = "Success";
    // @formatter:on

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public SetMarkersMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);

        this.log = log;
        this.json = json;
        this.messageFactory = messageFactory;

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
        // Deserialize request parameters
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call,
                "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }
        var request = optionalRequest.get();
        var projectName = request.projectName;

        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Project name is required."));
        }

        if (request.markers == null || request.markers.isEmpty())
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call, "Markers are required."));
        }

        return CompletableFuture.supplyAsync(() -> {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject project = root.getProject(projectName);

            // Validate project existence and state
            if (!project.exists())
            {
                return messageFactory.createError(this, call, "Project not found: " + projectName);
            }
            if (!project.isOpen())
            {
                return messageFactory.createError(this, call, "Project is closed: " + projectName);
            }

            // Process each marker request
            for (var markerReq : request.markers)
            {
                if (cancellationToken.isCanceled())
                {
                    continue;
                }

                try
                {
                    // Build relative path from request
                    var relativePath = new Path(markerReq.relativeFilePath);

                    // Get file handle relative to project
                    var file = project.getFile(relativePath);

                    // Verify file exists in workspace
                    if (!file.exists())
                    {
                        return messageFactory.createError(this, call, "File not found: " + relativePath);
                    }

                    // Create custom AI marker
                    var marker = file.createMarker(AI_MARKER_TYPE);

                    // Set standard attributes for visibility
                    marker.setAttribute(IMarker.MESSAGE, markerReq.message);
                    marker.setAttribute(IMarker.LOCATION, "Line " + markerReq.line); // Добавлено для лучшего отображения
                    marker.setAttribute(IMarker.LINE_NUMBER, markerReq.line);

                    // Set severity (required for display)
                    var severity = convertSeverity(markerReq.severity);
                    marker.setAttribute(IMarker.SEVERITY, severity);

                    // Set priority if provided
                    if (markerReq.priority != null)
                    {
                        var priority = convertPriority(markerReq.priority);
                        marker.setAttribute(IMarker.PRIORITY, priority);
                    }

                    // Custom attributes for AI markers
                    marker.setAttribute("ai.generated", true);

                    // Set character position for better highlighting
                    marker.setAttribute(IMarker.CHAR_START, -1);
                    marker.setAttribute(IMarker.CHAR_END, -1);
                }
                catch (CoreException error)
                {
                    return messageFactory.createError(this, call, "Failed to set marker. " + error.getMessage());
                }
            }
            return messageFactory.createMessage(this, call, "Success");
        });
    }

    @SuppressWarnings("nls")
    private int convertSeverity(String severity)
    {
        // Convert string severity to Eclipse IMarker constant
        switch (severity.toLowerCase())
        {
        case "error":
            return IMarker.SEVERITY_ERROR;
        case "warning":
            return IMarker.SEVERITY_WARNING;
        case "info":
        default:
            return IMarker.SEVERITY_INFO;
        }
    }

    @SuppressWarnings("nls")
    private int convertPriority(String priority)
    {
        // Convert string priority to Eclipse IMarker constant
        switch (priority.toLowerCase())
        {
        case "high":
            return IMarker.PRIORITY_HIGH;
        case "low":
            return IMarker.PRIORITY_LOW;
        case "normal":
        default:
            return IMarker.PRIORITY_NORMAL;
        }
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        // Create tool specification for AI assistant
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        // Build tool description
        var description = new StringBuilder();
        description.append(
            "Sets AI-generated markers in project files. Use for highlighting code issues, suggestions, or findings.");
        description.append("\n\nParameters:");
        description.append("\n- project_name: Target project name (required)");
        description.append("\n- markers: List of markers to create (required)");
        description.append("\n\nMarker properties:");
        description.append("\n- relative_file_path: File path relative to project root (required)");
        description.append("\n- line: Line number (required)");
        description.append("\n- message: Marker description (required)");
        description.append("\n- severity: `error`, `warning`, or `info` (required)");
        description.append("\n- priority: `high`, `normal`, or `low` (optional)");
        description.append("\n\nExample request:");
        description.append("\n").append(QuestionExample);
        description.append("\n\nExample response:");
        description.append("\n").append(AnswerExample);

        spec.function.description = description.toString();

        // Define parameters schema
        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        // Project name property
        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "Name of the target project";
        properties.put("project_name", projectNameProp);

        // Markers array property
        var markersProp = new McpToolCallProperty();
        markersProp.type = "array";
        markersProp.description = "List of markers to create";
        properties.put("markers", markersProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "markers");
        spec.function.parameters = parameters;

        return spec;
    }

    /**
     * Request DTO for deserialization
     */
    private static class Request
    {
        @SerializedName("project_name")
        public String projectName;

        @SerializedName("markers")
        public List<MarkerRequest> markers;
    }

    /**
     * Marker request DTO with relative paths
     */
    private static class MarkerRequest
    {
        @SerializedName("relative_file_path")
        public String relativeFilePath; // Relative path from project root

        @SerializedName("line")
        public int line; // Line number in file

        @SerializedName("message")
        public String message; // Marker description

        @SerializedName("severity")
        public String severity; // Error level

        @SerializedName("priority")
        public String priority; // Optional priority
    }
}