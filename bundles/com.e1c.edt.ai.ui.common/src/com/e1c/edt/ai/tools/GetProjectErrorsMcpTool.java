/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IMarker;
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

public class GetProjectErrorsMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_get_project_errors"; //$NON-NLS-1$

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

    @Inject
    public GetProjectErrorsMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
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

    @SuppressWarnings({ "nls" })
    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
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
        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);
            if (project == null || !project.exists())
            {
                return messageFactory.createError(this, call, "Project not found: " + projectName);
            }
            if (!project.isOpen())
            {
                return messageFactory.createError(this, call, "Project is closed: " + projectName);
            }
            try
            {
                if (cancellationToken.isCanceled())
                {
                    return messageFactory.createError(this, call, "Operation cancelled during error collection");
                }

                var markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
                var response = new ArrayList<ErrorInfo>();
                for (var marker : markers)
                {
                    if (cancellationToken.isCanceled())
                    {
                        return messageFactory.createError(this, call, "Operation cancelled during marker processing");
                    }

                    String severity;
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

                    int priorityValue = marker.getAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
                    String priority;
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

                    var resource = marker.getResource();
                    var location = resource.getLocation();
                    var relativePath = resource.getProjectRelativePath().toPortableString();

                    ErrorInfo errorInfo = new ErrorInfo();
                    errorInfo.absolutePath = location != null ? location.toFile().getAbsolutePath() : "";
                    errorInfo.relativePath = relativePath;
                    errorInfo.line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                    errorInfo.message = marker.getAttribute(IMarker.MESSAGE, "");
                    errorInfo.severity = severity;
                    errorInfo.priority = priority;
                    response.add(errorInfo);
                }

                var content = json.serialize(response);
                return messageFactory.createMessage(this, call, content);
            }
            catch (CoreException e)
            {
                return messageFactory.createError(this, call, "Error retrieving project markers: " + e.getMessage());
            }
            catch (OperationCanceledException e)
            {
                return messageFactory.createError(this, call, "Operation cancelled: " + e.getMessage());
            }
        });
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;
        var description = new StringBuilder();
        description.append("Returns all errors and warnings in the specified IDE project.");
        description.append("\nResponse contains:");
        description.append("\n- absolute_path: Absolute file system path (OS-dependent format)");
        description.append("\n- relative_path: Project-relative path");
        description.append("\n- line: Line number (-1 if unknown)");
        description.append("\n- message: Error description");
        description.append("\n- severity: 'error', 'warning' or 'info'");
        description.append("\n- priority: Priority level as string ('high', 'normal', 'low')");
        description.append("\nExample request:");
        description.append("\n").append(QuestionExample);
        description.append("\nExample response:");
        description.append("\n").append(AnswerExample);
        spec.function.description = description.toString();
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

    private static class Request
    {
        @SerializedName("project_name")
        public String projectName;
    }

    private static class ErrorInfo
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