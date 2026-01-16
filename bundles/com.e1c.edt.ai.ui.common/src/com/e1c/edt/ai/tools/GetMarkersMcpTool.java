/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;

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

public class GetMarkersMcpTool
    implements IMcpTool
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
        + "    \"absolute_path\": \"/path/to/project/MyProject/Forms/MyForm/Module.bsl\",\n"
        + "    \"relative_path\": \"Forms/MyForm/Module.bsl\",\n"
        + "    \"line\": 5,\n"
        + "    \"message\": \"Syntax error: missing semicolon\",\n"
        + "    \"severity\": \"error\",\n"
        + "    \"priority\": \"high\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"absolute_path\": \"/path/to/project/MyProject/CommonModules/MyModule/Module.bsl\",\n"
        + "    \"relative_path\": \"CommonModules/MyModule/Module.bsl\",\n"
        + "    \"line\": 12,\n"
        + "    \"message\": \"Unused variable: myVar\",\n"
        + "    \"severity\": \"warning\",\n"
        + "    \"priority\": \"normal\"\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IBuildWaiter buildWaiter;

    @Inject
    public GetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IBuildWaiter buildWaiter)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(buildWaiter);
        this.json = json;
        this.messageFactory = messageFactory;
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
                return messageFactory.createError(this, call, "Operation cancelled during error collection");
            }

            // Retrieve all problem, bookmark and AI markers in the project
            var problemMarkers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            var bookmarkMarkers = project.findMarkers(IMarker.BOOKMARK, true, IResource.DEPTH_INFINITE);
            var aiMarkers = project.findMarkers(SetMarkersMcpTool.AI_MARKER_TYPE, true, IResource.DEPTH_INFINITE);

            var allMarkers = new ArrayList<IMarker>();
            allMarkers.addAll(Arrays.asList(problemMarkers));
            allMarkers.addAll(Arrays.asList(bookmarkMarkers));
            allMarkers.addAll(Arrays.asList(aiMarkers));

            var response = new ArrayList<MarkerInfo>();

            // Process each marker
            for (var marker : allMarkers)
            {
                // Check cancellation during marker processing
                if (cancellationToken.isCanceled())
                {
                    return messageFactory.createError(this, call, "Operation cancelled during marker processing");
                }

                String severity;
                String priority;

                // Determine marker type
                String markerType = marker.getType();
                if (markerType.equals(IMarker.PROBLEM))
                {
                    // Map marker severity to string representation
                    int severityValue = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                    switch (severityValue)
                    {
                    case IMarker.SEVERITY_ERROR:
                        severity = "error";
                        break;
                    case IMarker.SEVERITY_WARNING:
                        severity = "warning";
                        break;
                    default:
                        severity = "info";
                    }

                    // Map marker priority to string representation
                    int priorityValue = marker.getAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
                    switch (priorityValue)
                    {
                    case IMarker.PRIORITY_HIGH:
                        priority = "high";
                        break;
                    case IMarker.PRIORITY_NORMAL:
                        priority = "normal";
                        break;
                    case IMarker.PRIORITY_LOW:
                        priority = "low";
                        break;
                    default:
                        priority = "unknown";
                    }
                }
                else if (markerType.equals(IMarker.BOOKMARK))
                {
                    // Bookmark specific values
                    severity = "bookmark";
                    priority = "none";
                }
                else if (markerType.equals(SetMarkersMcpTool.AI_MARKER_TYPE))
                {
                    // AI marker specific values
                    severity = "ai_marker";
                    priority = "none";
                }
                else
                {
                    // Skip other marker types
                    continue;
                }

                var resource = marker.getResource();
                var location = resource.getLocation();
                var relativePath = resource.getProjectRelativePath().toPortableString();

                MarkerInfo markerInfo = new MarkerInfo();
                markerInfo.absolutePath = location != null ? location.toFile().getAbsolutePath() : "";
                markerInfo.relativePath = relativePath;
                markerInfo.line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                markerInfo.message = marker.getAttribute(IMarker.MESSAGE, "");
                markerInfo.severity = severity;
                markerInfo.priority = priority;

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

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Returns all errors, warnings, bookmarks and AI markers in the specified IDE project.");
        description.append("\nResponse contains:");
        description.append("\n- absolute_path: Absolute file system path (OS-dependent format)");
        description.append("\n- relative_path: Project-relative path");
        description.append("\n- line: Line number (-1 if unknown)");
        description.append("\n- message: Error description");
        description.append("\n- severity: `error`, `warning`, `info`, `bookmark` or `ai_marker`");
        description.append("\n- priority: Priority level as string (`high`, `normal`, `low`, `none`)");
        description.append("\nExample request:");
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
        projectNameProp.description = "Name of the IDE project to analyze for errors";
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
        @SerializedName("absolute_path")
        public String absolutePath;

        @SerializedName("relative_path")
        public String relativePath;

        @SerializedName("line")
        public int line;

        @SerializedName("message")
        public String message;

        @SerializedName("severity")
        public String severity;

        @SerializedName("priority")
        public String priority;
    }
}