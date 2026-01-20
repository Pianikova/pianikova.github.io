/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
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
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class SetMarkersMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "SetMarkers"; //$NON-NLS-1$
    public static final String ACTION_CALL_ATTRIBUTE = "action_call"; //$NON-NLS-1$
    public static final String ACTION_DETAILS_ATTRIBUTE = "action_details"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  \"极markers\": [\n"
        + "    {\n"
        + "      \"type\": \"bookmark\",\n"
        + "      \"relative_file_path\": \"Forms/MyForm/Module.bsl\",\n"
        + "      \"start_line\": 10,\n"
        + "      \"target_content\": \"Important code section\",\n"
        + "      \"message\": \"Important code section\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"task\",\n"
        + "      \"relative_file_path\": \"CommonModules/MyModule/Module.bsl\",\n"
        + "      \"start_line\": 25,\n"
        + "      \"target_content\": \"TODO: Refactor this code\",\n"
        + "      \"message\": \"TODO: Refactor this code\",\n"
        + "      \"priority\": \"normal\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"problem\",\n"
        + "      \"relative_file_path\": \"CommonModules/AnotherModule/Module.bsl\",\n"
        + "      \"start_line\": 42,\n"
        + "      \"target_content\": \"Syntax error\",\n"
        + "      \"message\": \"Syntax error\",\n"
        + "      \"severity\": \"error\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"text\",\n"
        + "      \"relative_file_path\": \"CommonModules/TextModule/Module.bsl\",\n"
        + "      \"start_line\": 15,\n"
        + "      \"target_content\": \"Important text note\",\n"
        + "      \"message\": \"Important text note\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"ai_marker\",\n"
        + "      \"relative_file_path\": \"CommonModules/AIModule/Module.bsl\",\n"
        + "      \"start_line\": 30,\n"
        + "      \"target_content\": \"AI suggestion\",\n"
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
    private final IContentSourceProvider contentSourceProvider;
    private final IFileSystem fileSystem;

    @Inject
    public SetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider, IFileSystem fileSystem)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(fileSystem);

        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.fileSystem = fileSystem;
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
                catch (CoreException | IllegalArgumentException | BadLocationException error)
                {
                    errors.append("Marker [").append(i).append("] error: ").append(error.getMessage()).append("; ");
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
        throws CoreException, IllegalArgumentException, BadLocationException
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
        if (markerReq.startLine == null || markerReq.startLine < 1)
        {
            throw new IllegalArgumentException("start_line must be a positive integer");
        }
        if (markerReq.targetContent == null || markerReq.targetContent.isBlank())
        {
            throw new IllegalArgumentException("target_content is required");
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

        // Calculate char positions from target_content using ReadMcpTool approach
        var positions = calculateCharPositions(file, markerReq.startLine, markerReq.targetContent);
        markerReq.lineOffset = positions[0];
        markerReq.charStart = positions[1];
        markerReq.charEnd = positions[2];

        // Convert request type to MarkerType enum
        var markerType = MarkerType.fromDisplayName(markerReq.type);
        if (markerType == null)
        {
            throw new IllegalArgumentException("Unknown marker type: " + markerReq.type);
        }
        var marker = file.createMarker(markerType.getTypeId());
        markerReq.id = marker.getId();
        setMarkerAttributes(marker, call, markerReq, markerType);
    }

    @SuppressWarnings("nls")
    private int[] calculateCharPositions(IFile file, int startLine, String targetContent)
        throws CoreException, IllegalArgumentException, BadLocationException
    {
        var optionalDocument = contentSourceProvider.getFileDocument(file);
        if (optionalDocument.isEmpty())
        {
            throw new IllegalArgumentException("File content not available: " + file.getFullPath());
        }

        var document = optionalDocument.get();
        var content = new StringBuilder();
        var maxLinesCount = targetContent.lines().count() + 1;
        var charStart = -1;
        for (var line : fileSystem.getLines(document, startLine - 1, (int)maxLinesCount))
        {
            content.append(line);
            charStart = content.indexOf(targetContent);
            if (charStart >= 0)
            {
                break;
            }
        }

        if (charStart == -1)
        {
            throw new IllegalArgumentException(
                "Target content not found in line " + startLine + ": '" + targetContent + "'");
        }

        var charEnd = charStart + targetContent.length();
        return new int[] { document.getDocument().getLineOffset(startLine - 1), charStart, charEnd };
    }

    @SuppressWarnings("nls")
    private void setMarkerAttributes(IMarker marker, McpToolCall call, MarkerRequest markerReq, MarkerType markerType)
        throws CoreException
    {
        // Common attributes for all marker types
        marker.setAttribute(IMarker.MESSAGE, markerReq.message);
        marker.setAttribute(IMarker.TRANSIENT, true);

        var location = new StringBuilder();
        if (markerReq.startLine != null && markerReq.startLine > 0)
        {
            marker.setAttribute(IMarker.LINE_NUMBER, markerReq.startLine);
            location.append("Line ");
            location.append(markerReq.startLine);
        }

        if (markerReq.charStart != null && markerReq.charEnd != null)
        {
            marker.setAttribute(IMarker.CHAR_START, markerReq.lineOffset + markerReq.charStart);
            marker.setAttribute(IMarker.CHAR_END, markerReq.lineOffset + markerReq.charEnd);
            location.append(" [");
            location.append(markerReq.charStart + 1);
            location.append(':');
            location.append(markerReq.charEnd + 1);
            location.append(']');
        }

        if (location.length() > 0)
        {
            marker.setAttribute(IMarker.LOCATION, location.toString());
        }

        // Action attributes - only for AI markers
        if (markerType == MarkerType.AI_MARKER && markerReq.actionPrompt != null && !markerReq.actionPrompt.isBlank())
        {
            marker.setAttribute(ACTION_CALL_ATTRIBUTE, call);
            marker.setAttribute(ACTION_DETAILS_ATTRIBUTE, markerReq);
        }

        // Type-specific attributes
        switch (markerType)
        {
        case BOOKMARK:
            setBooleanAttribute(marker, IMarker.DONE, markerReq.done);
            setBooleanAttribute(marker, IMarker.USER_EDITABLE, true);
            break;

        case TASK:
            setBooleanAttribute(marker, IMarker.DONE, markerReq.done);
            setBooleanAttribute(marker, IMarker.USER_EDITABLE, true);
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
        description.append("\n- To update markers, first delete it using the `" + DeleteMarkersMcpTool.TOOL_NAME
            + "` tool and then create a new one if necessary.");
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
            "\n- start_line: Line number (required). An integer value indicating the line number for a marker. It is 1-relative. Take the line number from the line prefix using the `"
                + ReadMcpTool.TOOL_NAME + "` tool");
        description.append(
            "\n- target_content: Text to mark (required). ALWAYS minimize this text to the smallest possible size that still allows to understand the context. ALWAYS exclude extra suffix and prefix.");
        description.append(
            "\n- action_prompt: AI prompt to execute when marker is activated (required for ai_marker)");
        description.append(
            "\n- action_title: Short title for the quick fix action (required for ai_marker)");
        description.append(
            "\n- action_description: Detailed description of the quick fix action (required for ai_marker)");

        description.append("\n\nType-specific properties:");
        description.append("\n- bookmark: done");
        description.append("\n- task: done, priority");
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

        @SerializedName("start_line")
        public Integer startLine;

        @SerializedName("target_content")
        public String targetContent;

        @SerializedName("severity")
        public String severity;

        @SerializedName("priority")
        public String priority;

        @SerializedName("done")
        public Boolean done;

        @SerializedName("action_prompt")
        public String actionPrompt;

        @SerializedName("action_title")
        public String actionTitle;

        @SerializedName("action_description")
        public String actionDescription;

        public transient long id;
        public transient Integer lineOffset;
        public transient Integer charStart;
        public transient Integer charEnd;
    }
}