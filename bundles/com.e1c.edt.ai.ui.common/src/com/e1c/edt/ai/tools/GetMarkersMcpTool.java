/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;

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
import com.e1c.edt.ai.ui.IContentSourceProvider;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class GetMarkersMcpTool implements IMcpTool
{
    public static final String TOOL_NAME = "GetMarkers"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\"\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"id\": 1001,\n"
        + "    \"absolute_path\": \"/path/to/project/MyProject/Forms/MyForm/Module.bsl\",\n"
        + "    \"relative_path\": \"Forms/MyForm/Module.bsl\",\n"
        + "    \"start_line\": 5,\n"
        + "    \"message\": \"Syntax error: missing semicolon\",\n"
        + "    \"type\": \"problem\",\n"
        + "    \"severity\": \"error\",\n"
        + "    \"priority\": \"high\",\n"
        + "    \"target_content\": \"a = 1 / 0;\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"id\": 1002,\n"
        + "    \"absolute_path\": \"/path/to/project/MyProject/CommonModules/MyModule/Module.bsl\",\n"
        + "    \"relative_path\": \"CommonModules/MyModule/Module.bsl\",\n"
        + "    \"start_line\": 12,\n"
        + "    \"message\": \"Unused variable: myVar\",\n"
        + "    \"type\": \"problem\",\n"
        + "    \"severity\": \"warning\",\n"
        + "    \"priority\": \"normal\",\n"
        + "    \"target_content\": \"myVar = 0;\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"id\": 2001,\n"
        + "    \"absolute_path\": \"/path/to/project/MyProject/Forms/MyForm/Module.bsl\",\n"
        + "    \"relative_path\": \"Forms/MyForm/Module.bsl\",\n"
        + "    \"start_line\": 10,\n"
        + "    \"message\": \"Important code section\",\n"
        + "    \"type\": \"bookmark\",\n"
        + "    \"done\": false,\n"
        + "    \"target_content\": \"Important code section\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"id\": 3001,\n"
        + "    \"absolute_path\": \"/path/to/project/MyProject/CommonModules/TextModule/Module.bsl\",\n"
        + "    \"relative_path\": \"CommonModules/TextModule/Module.bs极l\",\n"
        + "    \"start_line\": 15,\n"
        + "    \"message\": \"Important text note\",\n"
        + "    \"type\": \"text\",\n"
        + "    \"target_content\": \"Important text note\"\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final IBuildWaiter buildWaiter;

    @Inject
    public GetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider, IBuildWaiter buildWaiter)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(buildWaiter);
        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.buildWaiter = buildWaiter;
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

    @SuppressWarnings({ "nls" })
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

        // Synchronous project validation: Check if project exists and is open
        var root = ResourcesPlugin.getWorkspace().getRoot();
        var project = root.getProject(projectName);
        if (project == null || !project.exists())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Project not found: " + projectName));
        }
        if (!project.isOpen())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Project is closed: " + projectName));
        }

        return buildWaiter.waitForBuilds(cancellationToken).thenCompose(voidResult -> {
            if (cancellationToken.isCanceled())
            {
                return CompletableFuture
                    .completedFuture(messageFactory.createError(this, call, "Operation cancelled after build wait"));
            }
            return CompletableFuture.supplyAsync(() -> createResponse(project, call, cancellationToken));
        }).exceptionally(e -> {
            Throwable cause = e.getCause();
            if (cause instanceof OperationCanceledException)
            {
                return messageFactory.createError(this, call, "Build waiting cancelled");
            }
            if (cause instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
                return messageFactory.createError(this, call, "Build waiting interrupted");
            }
            return messageFactory.createError(this, call, "Error during build waiting: " + e.getMessage());
        });
    }

    @SuppressWarnings("nls")
    private ToolCallMessage createResponse(IProject project, McpToolCall call, ICancellationToken cancellationToken)
    {
        try
        {
            // Early cancellation check
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation cancelled during marker collection");
            }

            // Retrieve all markers in the project
            var markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
            var response = new ArrayList<MarkerInfo>();

            // Process each marker
            for (var marker : markers)
            {
                // Check cancellation during marker processing
                if (cancellationToken.isCanceled())
                {
                    return messageFactory.createError(this, call, "Operation cancelled during marker processing");
                }

                // Determine marker type using enum
                MarkerType markerType = MarkerType.fromTypeId(marker.getType());
                if (markerType == null)
                    continue; // Skip unknown types

                var resource = marker.getResource();
                var location = resource.getLocation();
                var relativePath = resource.getProjectRelativePath().toPortableString();

                MarkerInfo markerInfo = new MarkerInfo();
                markerInfo.id = marker.getId();
                markerInfo.absolutePath = location != null ? location.toFile().getAbsolutePath() : "";
                markerInfo.relativePath = relativePath;
                markerInfo.startLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                markerInfo.message = marker.getAttribute(IMarker.MESSAGE, "");
                markerInfo.type = markerType.getDisplayName();

                // Set common and type-specific attributes
                setMarkerAttributes(project, marker, markerInfo, markerType, cancellationToken);
                response.add(markerInfo);
            }

            var content = json.serialize(response);
            return messageFactory.createMessage(this, call, content);
        }
        catch (CoreException | OperationCanceledException error)
        {
            if (error instanceof CoreException)
            {
                return messageFactory.createError(this, call,
                    "Error retrieving project markers: " + error.getMessage());
            }
            if (error instanceof OperationCanceledException)
            {
                return messageFactory.createError(this, call, "Operation cancelled: " + error.getMessage());
            }
            return messageFactory.createError(this, call, "Unexpected error: " + error.getMessage());
        }
    }

    private void setMarkerAttributes(IProject project, IMarker marker, MarkerInfo markerInfo, MarkerType markerType,
        ICancellationToken cancellationToken) throws CoreException
    {
        // Common attributes for all marker types
        markerInfo.location = marker.getAttribute(IMarker.LOCATION, null);

        // Get char positions
        Integer charStart = null;
        Integer charEnd = null;
        var charStartObj = marker.getAttribute(IMarker.CHAR_START);
        if (charStartObj instanceof Integer)
        {
            charStart = (Integer)charStartObj;
        }

        var charEndObj = marker.getAttribute(IMarker.CHAR_END);
        if (charEndObj instanceof Integer)
        {
            charEnd = (Integer)charEndObj;
        }

        // Read target content if positions are available
        if (charStart != null && charEnd != null && charEnd > charStart)
        {
            try
            {
                IFile file = (IFile)marker.getResource();
                if (file.exists())
                {
                    markerInfo.targetContent = readContentFromFile(file, charStart, charEnd - charStart);
                }
            }
            catch (Exception e)
            {
                // Ignore errors and leave targetContent empty
            }
        }

        // Type-specific attributes
        switch (markerType)
        {
        case BOOKMARK:
            var doneBookmark = marker.getAttribute(IMarker.DONE);
            if (doneBookmark instanceof Boolean)
            {
                markerInfo.done = (Boolean)doneBookmark;
                }
                var sourceId = marker.getAttribute(IMarker.SOURCE_ID);
                if (sourceId instanceof String)
                {
                    markerInfo.sourceId = (String)sourceId;
                }
                break;

            case TASK:
                var doneTask = marker.getAttribute(IMarker.DONE);
                if (doneTask instanceof Boolean)
                {
                    markerInfo.done = (Boolean)doneTask;
                }
                var priorityObj = marker.getAttribute(IMarker.PRIORITY);
                if (priorityObj instanceof Integer)
                {
                    int priority = (Integer)priorityObj;
                    markerInfo.priority = convertPriorityToString(priority);
                }
                break;

            case PROBLEM:
            case AI_MARKER:
                var severityObj = marker.getAttribute(IMarker.SEVERITY);
                if (severityObj instanceof Integer)
                {
                    int severity = (Integer)severityObj;
                    markerInfo.severity = convertSeverityToString(severity);
                }
                var priorityProblem = marker.getAttribute(IMarker.PRIORITY);
                if (priorityProblem instanceof Integer)
                {
                    int priority = (Integer)priorityProblem;
                    markerInfo.priority = convertPriorityToString(priority);
                }
                break;

            default:
                // No additional attributes for other types
                break;
        }
    }

    private String readContentFromFile(IFile file, int charStart, int length) throws BadLocationException
    {
        var optionalDocument = contentSourceProvider.getFileDocument(file);
        if (optionalDocument.isEmpty())
        {
            return null;
        }

        var document = optionalDocument.get();
        return document.getDocument().get(charStart, length);
    }

    @SuppressWarnings("nls")
    private String convertSeverityToString(int severity)
    {
        switch (severity)
        {
        case IMarker.SEVERITY_ERROR:
            return "error";
        case IMarker.SEVERITY_WARNING:
            return "warning";
        default:
            return "info";
        }
    }

    @SuppressWarnings("nls")
    private String convertPriorityToString(int priority)
    {
        switch (priority)
        {
        case IMarker.PRIORITY_HIGH:
            return "high";
        case IMarker.PRIORITY_LOW:
            return "low";
        default:
            return "normal";
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
        description.append(
            "Returns markers (errors, warnings, info, bookmarks, tasks, etc.) in the specified IDE project, including:");

        // Generate supported types from MarkerType enum
        for (MarkerType type : MarkerType.values())
        {
            description.append("\n- ").append(type.getDisplayName()).append(": ").append(type.getDescription());
        }

        description.append("\n\nResponse contains:");
        description.append("\n- id: Unique marker identifier (long number)");
        description.append("\n- absolute_path: Absolute file system path (OS-dependent format)");
        description.append("\n- relative_path: Project-relative path");
        description.append(
            "\n- start_line: Line number (-1 if unknown). An integer value indicating the line number for a marker. It is 1-relative.");
        description.append("\n- message: Marker description");
        description.append("\n- type: Marker type (");
        boolean first = true;
        for (var type : MarkerType.values())
        {
            if (!first)
            {
                description.append(", ");
            }
            description.append(type.getDisplayName());
            first = false;
        }
        description.append(")");
        description.append("\n- severity: For problems and AI markers (error, warning, info)");
        description.append("\n- priority: For problems, tasks and AI markers (high, normal, low)");
        description.append("\n- done: For bookmarks and tasks (true/false)");
        description.append(
            "\n- location: Human-readable location string. The location is a human-readable (localized) string which can be used to distinguish between markers on a resource. As such it should be concise and aimed at users.");
        description
            .append("\n- target_content: The content of the marker (substring of the file at the marker's position)");
        description.append("\n- source_id: Source identifier for bookmarks");
        description.append("\n\nExample request:");
        description.append("\n").append(QuestionExample);
        description.append("\nExample response:");
        description.append("\n").append(AnswerExample);

        spec.function.description = description.toString();

        // Define function parameters
        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "Name of the IDE project to retrieve markers from";
        properties.put("project_name", projectNameProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name");
        spec.function.parameters = parameters;

        return spec;
    }

    // Request DTO for JSON deserialization
    private static class Request
    {
        @SerializedName("project_name")
        public String projectName;
    }

    // Marker information DTO for JSON serialization
    private static class MarkerInfo
    {
        @SerializedName("id")
        public long id;

        @SerializedName("absolute_path")
        public String absolutePath;

        @SerializedName("relative_path")
        public String relativePath;

        @SerializedName("start_line")
        public int startLine;

        @SerializedName("message")
        public String message;

        @SerializedName("type")
        public String type;

        @SerializedName("severity")
        public String severity;

        @SerializedName("priority")
        public String priority;

        @SerializedName("done")
        public Boolean done;

        @SerializedName("location")
        public String location;

        @SerializedName("target_content")
        public String targetContent;

        @SerializedName("source_id")
        public String sourceId;
    }
}