/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class DeleteMarkersMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "DeleteMarkers"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  // \"marker_type\": \"ai_marker\", // Optional: bookmark, task, text, ai_marker (AIError/AIWarning/AIInfo)\n"
        + "  // \"path\": \"C:/Projects/MyProject/optional/file/path.bsl\",\n"
        + "  // \"id\": 12345 // Optional: specific marker ID\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample = "Markers cleared successfully";
    // @formatter:on

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public DeleteMarkersMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory)
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
        var details = new ToolCallMessageDetails();
        details.autoCall = true;
        if (call.callKind == ToolCallKind.RENDER)
        {
            var optionalRequest = json.deserialize(call.function.arguments, Request.class);
            if (optionalRequest.isPresent() && optionalRequest.get().projectName != null
                && !optionalRequest.get().projectName.isBlank())
            {
                details.requestMarkdown =
                    MessageFormat.format(Messages.RemoveMarkersTitleTemplate, optionalRequest.get().projectName);
            }
            else
            {
                details.requestMarkdown = Messages.RemoveMarkersTitle;
            }
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

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
                // Handle deletion by marker ID
                if (request.id != null)
                {
                    return deleteMarkerById(project, request.id, call, details);
                }

                var markerType = getMarkerType(request.markerType);
                if (request.relativeFilePath != null && !request.relativeFilePath.isBlank())
                {
                    // Clear markers for specific file - get file from absolute path
                    var file = root.getFile(new org.eclipse.core.runtime.Path(request.relativeFilePath));
                    if (file == null || !file.exists())
                    {
                        return messageFactory.createError(this, call,
                            "File not found: " + request.relativeFilePath);
                    }

                    deleteMarkers(file, markerType);
                }
                else
                {
                    // Clear markers for entire project
                    deleteMarkers(project, markerType);
                }

                // Add response markdown
                details.responseMarkdown = Messages.MarkersRemovedMessage;
                return messageFactory.createMessage(this, call, "Markers cleared successfully", details);
            }
            catch (CoreException | IllegalArgumentException error)
            {
                log.logError(error);
                return messageFactory.createError(this, call, "Failed to clear markers: " + error.getMessage());
            }
        });
    }

    @SuppressWarnings("nls")
    private ToolCallMessage deleteMarkerById(IProject project, long markerId, McpToolCall call,
        ToolCallMessageDetails details)
    {
        try
        {
            IMarker marker = findMarkerById(project, markerId);
            if (marker == null)
            {
                return messageFactory.createError(this, call, "Marker not found with id: " + markerId);
            }

            marker.delete();

            // Add response markdown
            details.responseMarkdown = Messages.MarkersRemovedMessage;
            return messageFactory.createMessage(this, call, "Marker with id " + markerId + " deleted successfully",
                details);
        }
        catch (CoreException e)
        {
            log.logError(e);
            return messageFactory.createError(this, call, "Failed to delete marker: " + e.getMessage());
        }
    }

    private IMarker findMarkerById(IProject project, long markerId) throws CoreException
    {
        IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
        for (IMarker marker : markers)
        {
            if (marker.getId() == markerId)
            {
                return marker;
            }
        }
        return null;
    }

    private void deleteMarkers(IResource resource, MarkerType markerType) throws CoreException
    {
        if (markerType != null)
        {
            // Delete specific marker type
            if (markerType == MarkerType.AI_MARKER)
            {
                for (var typeId : MarkerType.getAiMarkerTypeIds())
                {
                    resource.deleteMarkers(typeId, true, IResource.DEPTH_INFINITE);
                }
            }
            else
            {
                resource.deleteMarkers(markerType.getTypeId(), true, IResource.DEPTH_INFINITE);
            }
        }
        else
        {
            // Delete all non-problem markers (default behavior)
            for (var type : MarkerType.values())
            {
                if (type != MarkerType.PROBLEM)
                {
                    if (type == MarkerType.AI_MARKER)
                    {
                        for (var typeId : MarkerType.getAiMarkerTypeIds())
                        {
                            resource.deleteMarkers(typeId, true, IResource.DEPTH_INFINITE);
                        }
                    }
                    else
                    {
                        resource.deleteMarkers(type.getTypeId(), true, IResource.DEPTH_INFINITE);
                    }
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
        description.append("Removes markers from a project or file.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- By default removes all non-problem markers.");
        description.append("\n- Use `marker_type` to remove specific marker types.");
        description.append("\n- Use `id` to remove a single marker.");
        description.append("\n- `ai_marker` removes `AIError`, `AIWarning`, `AIInfo` marker types.");
        description.append("\n\nRelated tools:");
        description.append("\n- List markers: `" + GetMarkersMcpTool.TOOL_NAME + "`.");
        description.append("\n- Recreate markers: `" + SetMarkersMcpTool.TOOL_NAME + "`.");
        description.append("\n\nParameters:\n");
        description.append("- project_name: Name of target project (required)\n");
        description.append("- marker_type: Optional type of markers to remove:\n");

        // Generate supported types from MarkerType enum
        for (MarkerType type : MarkerType.values())
        {
            if (type != MarkerType.PROBLEM)
            {
                description.append("  - ").append(type.getDisplayName()).append("\n");
            }
        }
        description.append("- path: Optional relative path to file\n");
        description.append("- id: Optional ID of specific marker to delete\n\n");
        description.append("Example requests:\n");
        description.append("- Clear all non-problem markers in project:\n").append(QuestionExample);
        description.append("- Clear AI markers in specific file:\n");
        description.append(
            "  {\"project_name\":\"MyProject\",\"marker_type\":\"ai_marker\",\"path\":\"src/Module.bsl\"} // removes AIError, AIWarning, AIInfo");
        description.append("\n- Delete specific marker by ID:\n");
        description.append("  {\"project_name\":\"MyProject\",\"id\":12345}");
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

        // Property: path (optional)
        var filePathProp = new McpToolCallProperty();
        filePathProp.type = "string";
        filePathProp.description = "Relative path to target file from project root";
        properties.put("path", filePathProp);

        // Property: id (optional)
        var idProp = new McpToolCallProperty();
        idProp.type = "number";
        idProp.description = "ID of specific marker to delete";
        properties.put("id", idProp);

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

        @SerializedName("path")
        public String relativeFilePath;

        @SerializedName("id")
        public Long id;
    }
}

