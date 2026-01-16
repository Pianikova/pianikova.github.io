/**
* Copyright (C) 极2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

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

public class ClearMarkersMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ClearMarkers"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  // \"marker_type\": \"ai_marker\", // Optional: bookmark, task, text, ai_marker\n"
        + "  // \"relative_file_path\": \"optional/file/path.bsl\"\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample = "Markers cleared successfully";
    // @formatter:on

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public ClearMarkersMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory)
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
                "Cannot deserialize arguments. Use example: " + QuestionExample));
        }
        var request = optionalRequest.get();
        var projectName = request.projectName;

        // Validate project name
        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Project name is required"));
        }

        return CompletableFuture.supplyAsync(() -> {
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);

            // Check project existence and state
            if (!project.exists())
            {
                return messageFactory.createError(this, call, "Project not found: " + projectName);
            }
            if (!project.isOpen())
            {
                return messageFactory.createError(this, call, "Project is closed: " + projectName);
            }

            try
            {
                var markerType = getMarkerType(request.markerType);
                if (request.relativeFilePath != null && !request.relativeFilePath.isBlank())
                {
                    // Clear markers for specific file
                    var resource = project.findMember(request.relativeFilePath);
                    if (resource == null || !resource.exists())
                    {
                        return messageFactory.createError(this, call,
                            "File not found in project: " + request.relativeFilePath);
                    }

                    deleteMarkers(resource, markerType);
                }
                else
                {
                    // Clear markers for entire project
                    deleteMarkers(project, markerType);
                }

                return messageFactory.createMessage(this, call, "Markers cleared successfully");
            }
            catch (CoreException | IllegalArgumentException error)
            {
                log.logError(error);
                return messageFactory.createError(this, call, "Failed to clear markers: " + error.getMessage());
            }
        });
    }

    private void deleteMarkers(IResource resource, MarkerType markerType) throws CoreException
    {
        if (markerType != null)
        {
            // Delete specific marker type
            resource.deleteMarkers(markerType.getTypeId(), true, IResource.DEPTH_INFINITE);
        }
        else
        {
            // Delete all non-problem markers (default behavior)
            for (MarkerType type : MarkerType.values())
            {
                if (type != MarkerType.PROBLEM)
                {
                    resource.deleteMarkers(type.getTypeId(), true, IResource.DEPTH_INFINITE);
                }
            }
        }
    }

    private MarkerType getMarkerType(String markerType) throws IllegalArgumentException
    {
        if (markerType == null || markerType.isBlank())
        {
            return null;
        }

        MarkerType type = MarkerType.fromDisplayName(markerType);
        if (type == null)
        {
            throw new IllegalArgumentException("Unknown marker type: " + markerType); //$NON-NLS-1$
        }

        return type;
    }

    @SuppressWarnings("nls")
    private McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        // Tool description
        var description = new StringBuilder();
        description.append("Removes markers from project or specific file. ");
        description.append("By default, removes all non-problem markers. ");
        description.append("You can specify marker type to remove only specific markers.\n\n");

        description.append("Parameters:\n");
        description.append("- project_name: Name of target project (required)\n");
        description.append("- marker_type: Optional type of markers to remove:\n");

        // Generate supported types from MarkerType enum
        for (MarkerType type : MarkerType.values())
        {
            if (type != MarkerType.PROBLEM)
            {
                description.append("  • ").append(type.getDisplayName()).append("\n");
            }
        }

        description.append("- relative_file_path: Optional relative path to file\n\n");

        description.append("Example requests:\n");
        description.append("- Clear all non-problem markers in project:\n").append(QuestionExample);
        description.append("- Clear AI markers in specific file:\n");
        description.append(
            "  {\"project_name\":\"MyProject\",\"marker_type\":\"ai_marker\",\"relative_file_path\":\"src/Module.bsl\"}");
        description.append("\n\nExample response:\n").append(AnswerExample);

        spec.function.description = description.toString();

        // Parameters schema
        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        // Property: project_name
        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "Name of the target project";
        properties.put("project_name", projectNameProp);

        // Property: marker_type (optional)
        var markerTypeProp = new McpToolCallProperty();
        markerTypeProp.type = "string";
        markerTypeProp.description = "Type of markers to remove";
        properties.put("marker_type", markerTypeProp);

        // Property: relative_file_path (optional)
        var filePathProp = new McpToolCallProperty();
        filePathProp.type = "string";
        filePathProp.description = "Relative path to target file from project root";
        properties.put("relative_file_path", filePathProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name");
        spec.function.parameters = parameters;

        return spec;
    }

    /**
     * DTO for request deserialization
     */
    private static class Request
    {
        @SerializedName("project_name")
        public String projectName;

        @SerializedName("marker_type")
        public String markerType;

        @SerializedName("relative_file_path")
        public String relativeFilePath;
    }
}