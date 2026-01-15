/**
* Copyright (C) 2025, 1C
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
        + "  \"project_name\": \"MyProject\"\n"
        + "  // \"relative_file_path\": \"optional/file/path.bsl\"\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample = "Success";
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
                var filePath = request.relativeFilePath;
                if (filePath != null && !filePath.isBlank())
                {
                    // Clear markers for specific file
                    var resource = project.findMember(filePath);
                    if (resource == null || !resource.exists())
                    {
                        return messageFactory.createError(this, call, "File not found in project: " + filePath);
                    }

                    resource.deleteMarkers(SetMarkersMcpTool.AI_MARKER_TYPE, true, IResource.DEPTH_ZERO);
                    return messageFactory.createMessage(this, call, "AI markers cleared for file: " + filePath);
                }
                else
                {
                    // Clear all markers in project
                    project.deleteMarkers(SetMarkersMcpTool.AI_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
                    return messageFactory.createMessage(this, call, "AI markers cleared for entire project");
                }
            }
            catch (CoreException error)
            {
                log.logError(error);
                return messageFactory.createError(this, call, "Failed to clear AI markers: " + error.getMessage());
            }
        });
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
        description.append("Removes AI-generated markers. ");
        description.append("If `relative_file_path` is provided, clears markers only for that file. ");
        description.append("Otherwise clears markers for entire project.\n\n");
        description.append("Parameters:\n");
        description.append("- project_name: Name of target project (required)\n");
        description.append("- relative_file_path: Relative path to file (optional)\n\n");
        description.append("Example request without file path:\n").append(QuestionExample);
        description.append(
            "\nExample request with file path:\n{\"project_name\":\"MyProject\",\"relative_file_path\":\"src/Module.bsl\"}");
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

        @SerializedName("relative_file_path")
        public String relativeFilePath;
    }
}