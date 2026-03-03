/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
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
        + "  // project_name is optional - can be determined from absolute file paths\n"
        + "  // \"project_name\": \"MyProject\",\n"
        + "  \"markers\": [\n"
        + "    {\n"
        + "      \"type\": \"bookmark\",\n"
        + "      \"path\": \"C:/Projects/MyProject/Forms/MyForm/Module.bsl\",\n"
        + "      \"marker_line\": 10,\n"
        + "      \"marker_highlighted_text\": \"Important code section\",\n"
        + "      \"message\": \"Important code section that requires attention\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"task\",\n"
        + "      \"path\": \"C:/Projects/MyProject/CommonModules/MyModule/Module.bsl\",\n"
        + "      \"marker_line\": 25,\n"
        + "      \"marker_highlighted_text\": \"RefactorThisCode()\",\n"
        + "      \"message\": \"TODO: Refactor this code\",\n"
        + "      \"priority\": \"normal\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"problem\",\n"
        + "      \"path\": \"C:/Projects/MyProject/CommonModules/AnotherModule/Module.bsl\",\n"
        + "      \"marker_line\": 42,\n"
        + "      \"marker_highlighted_text\": \"a = 1 / 0\",\n"
        + "      \"message\": \"Syntax error\",\n"
        + "      \"severity\": \"error\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"text\",\n"
        + "      \"path\": \"C:/Projects/MyProject/CommonModules/TextModule/Module.bsl\",\n"
        + "      \"marker_line\": 15,\n"
        + "      \"marker_highlighted_text\": \"Important text note\",\n"
        + "      \"message\": \"Important text note\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"ai_marker\",\n"
        + "      \"path\": \"C:/Projects/MyProject/CommonModules/AIModule/Module.bsl\",\n"
        + "      \"marker_line\": 30,\n"
        + "      \"marker_highlighted_text\": \"calculateTotal(items)\",\n"
        + "      \"message\": \"AI warning (AIWarning)\",\n"
        + "      \"severity\": \"warning\",\n"
        + "      \"action_prompt\": \"Please refactor this code to use modern patterns\",\n"
        + "      \"action_title\": \"Refactor code\",\n"
        + "      \"action_description\": \"Update code to use modern design patterns\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"ai_marker\",\n"
        + "      \"path\": \"C:/Projects/MyProject/CommonModules/AIModule/Module.bsl\",\n"
        + "      \"marker_line\": 45,\n"
        + "      \"marker_highlighted_text\": \"calculateTotal(items)\",\n"
        + "      \"message\": \"AI error (AIError)\",\n"
        + "      \"severity\": \"error\",\n"
        + "      \"action_prompt\": \"Fix the error in calculateTotal\",\n"
        + "      \"action_title\": \"Fix error\",\n"
        + "      \"action_description\": \"Correct the issue to prevent runtime failure\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"type\": \"ai_marker\",\n"
        + "      \"path\": \"C:/Projects/MyProject/CommonModules/AIModule/Module.bsl\",\n"
        + "      \"marker_line\": 60,\n"
        + "      \"marker_highlighted_text\": \"calculateTotal(items)\",\n"
        + "      \"message\": \"AI info (AIInfo)\",\n"
        + "      \"severity\": \"info\",\n"
        + "      \"action_prompt\": \"Consider adding a null check\",\n"
        + "      \"action_title\": \"Improve safety\",\n"
        + "      \"action_description\": \"Add a null check to make the code safer\"\n"
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
    private final IProjectTools projectTools;

    @Inject
    public SetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider, IFileSystem fileSystem, IProjectTools projectTools)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(projectTools);

        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.fileSystem = fileSystem;
        this.projectTools = projectTools;
        this.spec = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return false;
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
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Invalid request format. Example: " + QuestionExample);
        }

        var request = optionalRequest.get();
        if (request.markers == null || request.markers.isEmpty())
        {
            throw new ToolException("At least one marker is required");
        }

        var projectName = request.projectName;
        if (call.callKind == ToolCallKind.RENDER)
        {
            if (projectName != null && !projectName.isBlank())
            {
                details.requestMarkdown =
                    MessageFormat.format(Messages.CreateMarkersTitleTemplate, projectName);
            }
            else
            {
                details.requestMarkdown = Messages.CreateMarkersTitle;
            }

            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }



        return CompletableFuture.supplyAsync(() -> {
            var root = ResourcesPlugin.getWorkspace().getRoot();

            // Determine project name - either from request or auto-determine from file path
            var deteminedProjectName = projectName;
            if (projectName == null || projectName.isBlank())
            {
                // Auto-determine project from first marker's file path
                var firstMarker = request.markers.get(0);
                deteminedProjectName = projectTools.determineProjectName(firstMarker.absoluteFilePath);
                if (deteminedProjectName == null)
                {
                    throw new ToolException("Cannot determine project from file path: " + firstMarker.absoluteFilePath
                        + ". Please specify project_name explicitly.");
                }
            }

            var project = root.getProject(deteminedProjectName);
            if (!project.exists())
            {
                throw new ToolException("Project not found: " + deteminedProjectName);
            }

            if (!project.isOpen())
            {
                throw new ToolException("Project is closed: " + deteminedProjectName);
            }

            var markersSet = 0;
            var createdMarkers = new ArrayList<IMarker>();
            var errors = new StringBuilder();
            for (int i = 0; i < request.markers.size(); i++)
            {
                if (cancellationToken.isCanceled())
                {
                    throw new ToolException("Operation cancelled after setting " + markersSet + " markers");
                }
                var markerReq = request.markers.get(i);
                markerReq.projectName = project.getName();
                try
                {
                    createdMarkers.add(createMarker(project, call, markerReq));
                    markersSet++;
                }
                catch (CoreException | IllegalArgumentException | BadLocationException error)
                {
                    errors.append("Marker [").append(i).append("] error: ").append(error.getMessage()).append("; ");
                }
            }
            if (errors.length() > 0)
            {
                for (var marker : createdMarkers)
                {
                    try
                    {
                        marker.delete();
                    }
                    catch (CoreException deleteError)
                    {
                        errors.append("Rollback error: ").append(deleteError.getMessage()).append("; ");
                    }
                }
                throw new ToolException("Completed with errors: " + errors + ". Markers set: " + markersSet);
            }

            // Add response markdown
            details.responseMarkdown = Messages.MarkersCreatedMessage;
            return messageFactory.createMessage(this, call, "Successfully created " + markersSet + " markers", details);
        });
    }

    @SuppressWarnings("nls")
    private IMarker createMarker(IProject project, McpToolCall call, MarkerRequest markerReq)
        throws CoreException, IllegalArgumentException, BadLocationException
    {
        // Validate required fields
        if (markerReq.absoluteFilePath == null || markerReq.absoluteFilePath.isBlank())
        {
            throw new IllegalArgumentException("path is required");
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
            throw new IllegalArgumentException("marker_line must be a positive integer");
        }
        if (markerReq.markerHighlightedText == null || markerReq.markerHighlightedText.isBlank())
        {
            throw new IllegalArgumentException("marker_highlighted_text is required");
        }
        // Validate action fields for ai_marker type when provided
        if ("ai_marker".equalsIgnoreCase(markerReq.type))
        {
            var hasActionPrompt = markerReq.actionPrompt != null && !markerReq.actionPrompt.isBlank();
            var hasActionTitle = markerReq.actionTitle != null && !markerReq.actionTitle.isBlank();
            var hasActionDescription = markerReq.actionDescription != null && !markerReq.actionDescription.isBlank();
            var hasAnyAction = hasActionPrompt || hasActionTitle || hasActionDescription;
            if (hasAnyAction && !(hasActionPrompt && hasActionTitle && hasActionDescription))
            {
                throw new IllegalArgumentException(
                    "action_prompt, action_title, and action_description must be provided together for ai_marker");
            }
        }

        // Get file from absolute path using IFileSystem
        var file = projectTools.getProjectFile(project, markerReq.absoluteFilePath);
        if (!file.isPresent())
        {
            throw new IllegalArgumentException("File not found: " + markerReq.absoluteFilePath);
        }

        var actualFile = file.get();

        // Calculate char positions from target_content using ReadMcpTool approach
        var positions = calculateCharPositions(actualFile, markerReq.startLine, markerReq.markerHighlightedText);
        markerReq.lineOffset = positions[0];
        markerReq.charStart = positions[1];
        markerReq.charEnd = positions[2];

        // Convert request type to MarkerType enum
        var markerType = MarkerType.fromDisplayName(markerReq.type);
        if (markerType == null)
        {
            throw new IllegalArgumentException("Unknown marker type: " + markerReq.type);
        }
        var markerTypeId = markerType.getTypeId();
        if (markerType == MarkerType.AI_MARKER)
        {
            markerTypeId = convertAISeverity(markerReq.severity);
        }

        var marker = actualFile.createMarker(markerTypeId);
        markerReq.id = marker.getId();
        setMarkerAttributes(marker, call, markerReq, markerType);
        return marker;
    }

    @SuppressWarnings("nls")
    private int[] calculateCharPositions(IFile file, int startLine, String markedText)
        throws CoreException, IllegalArgumentException, BadLocationException
    {
        var optionalDocument = contentSourceProvider.getFileDocument(file);
        if (optionalDocument.isEmpty())
        {
            throw new IllegalArgumentException("File content not available: " + file.getFullPath());
        }

        var document = optionalDocument.get();
        var content = new StringBuilder();
        var maxLinesCount = markedText.lines().count() + 1;
        var charStart = -1;
        for (var line : fileSystem.getLines(document, startLine - 1, (int)maxLinesCount))
        {
            content.append(line);
            charStart = content.indexOf(markedText);
            if (charStart >= 0)
            {
                break;
            }
        }

        if (charStart == -1)
        {
            throw new IllegalArgumentException(
                "Target content not found in line " + startLine);
        }

        var charEnd = charStart + markedText.length();
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
            marker.setAttribute(IMarker.SEVERITY, convertSeverity(markerReq.severity));
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
        {
            return IMarker.SEVERITY_INFO;
        }
        var normalized = severity.trim().toLowerCase();
        switch (normalized)
        {
        case "error":
            return IMarker.SEVERITY_ERROR;
        case "warn":
        case "warning":
            return IMarker.SEVERITY_WARNING;
        case "info":
        case "information":
        default:
            return IMarker.SEVERITY_INFO;
        }
    }

    @SuppressWarnings("nls")
    private static String convertAISeverity(String severity)
    {
        if (severity == null)
        {
            return MarkerType.AI_MARKER_INFO;
        }

        var normalized = severity.trim().toLowerCase();
        switch (normalized)
        {
        case "error":
            return MarkerType.AI_MARKER_ERROR;
        case "warn":
        case "warning":
            return MarkerType.AI_MARKER_WARNING;
        case "info":
        case "information":
        default:
            return MarkerType.AI_MARKER_INFO;
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
        description.append("Creates markers in project files (issues, tasks, bookmarks, etc.).");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Do NOT wrap JSON in Markdown or send arrays; no trailing commas or comments.");
        description.append("\n- `markers` must be an array of marker objects (see required fields below).");
        description.append(
            "\n- Use `" + MarkerType.AI_MARKER.getDisplayName()
                + "` for issues, problems, errors, warnings.");
        description.append(
            "\n- For `ai_marker`, severity maps to marker types: `AIError`, `AIWarning`, `AIInfo`.");
        description.append(
            "\n- Use `" + MarkerType.TASK.getDisplayName()
                + "` for plans, schedules, proposals, tasks, TODO.");
        description.append("\n- Use `" + MarkerType.BOOKMARK.getDisplayName() + "` for summaries or reports.");
        description.append("\n- To update a marker, delete it with `" + DeleteMarkersMcpTool.TOOL_NAME + "` and create a new one.");
        description.append("\n\nRelated tools:");
        description.append("\n- Inspect existing markers: `" + GetMarkersMcpTool.TOOL_NAME + "`.");
        description.append("\n- Remove markers: `" + DeleteMarkersMcpTool.TOOL_NAME + "`.");
        description.append("\n\nSupported marker types:");

        // Generate supported types from MarkerType enum
        for (MarkerType type : MarkerType.values())
        {
            description.append("\n- ").append(type.getDisplayName()).append(": ").append(type.getDescription());
        }

        description.append("\n\nCommon properties for all markers:");
        description.append("\n- type: Marker type (required)");
        description.append("\n- path: File path relative to project root (required)");
        description.append("\n- message: Marker description (required)");
        description.append(
            "\n- marker_line: Line number (required). An integer value indicating the line number for a marker. It is 1-relative. Take the line number from the line prefix using the `"
                + ReadMcpTool.TOOL_NAME + "` tool. Alias: start_line");
        description.append(
            "\n- marker_highlighted_text: Code fragment associated with the marker (required). ALWAYS minimize to the smallest possible size that maintains context. ALWAYS exclude extra suffix and prefix.");
        description.append("\n- action_prompt: AI prompt to execute when marker is activated (optional for ai_marker type)");
        description.append("\n- action_title: Short title for the quick fix action (optional for ai_marker type)");
        description.append("\n- action_description: Detailed description of the quick fix action (optional for ai_marker type)");

        description.append("\n\nType-specific properties:");
        description.append("\n- bookmark: done");
        description.append("\n- task: done, priority");
        description.append("\n- problem: severity, priority");
        description.append("\n- ai_marker: severity, priority");

        description.append("\n\nQuick fix actions:");
        description
            .append("\n- For `ai_marker` with action fields: adds quick fix action prompt");
        description.append(
            "\n- IMPORTANT: if you provide any action_* fields, you must provide all three action fields");
        description.append("\n- If type is not `ai_marker`, omit action_* fields.");

        description.append("\n\nExample request:\n").append(QuestionExample);
        description.append("\nExample response:\n").append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        // Project name (optional - can be determined from absolute file paths)
        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "Target project name. Optional - can be determined from absolute file paths.";
        properties.put("project_name", projectNameProp);

        // Markers array
        var markersProp = new McpToolCallProperty();
        markersProp.type = "array";
        markersProp.description =
            "List of marker objects. Each marker must include: type, path, marker_line, "
                + "marker_highlighted_text, message. "
                + "If you provide any action_* fields for ai_marker, all three are required.";
        properties.put("markers", markersProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("markers");
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

        @SerializedName("path")
        public String absoluteFilePath;

        @SerializedName("message")
        public String message;

        @SerializedName("marker_line")
        public Integer startLine;

        @SerializedName("marker_highlighted_text")
        public String markerHighlightedText;

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
