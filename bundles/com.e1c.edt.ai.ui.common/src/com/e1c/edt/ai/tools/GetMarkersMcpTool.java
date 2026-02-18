/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.ui.IContentSourceProvider;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class GetMarkersMcpTool implements IMcpTool
{
    public static final String TOOL_NAME = "GetMarkers"; //$NON-NLS-1$
    private static final int DEFAULT_MAX_ELEMENTS = McpToolConstants.DEFAULT_MAX_MARKERS;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  \"first_index\": 0,\n"
        + "  \"max_count\": 5,\n"
        + "  \"marker_type\": \"ai_marker\"\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\n"
        + "  \"markers\": [\n"
        + "    {\n"
        + "    \"id\": 4002,\n"
        + "    \"path\": \"C:/Projects/MyProject/MyProject/CommonModules/AIModule/Module.bsl\",\n"
        + "    \"start_line\": 45,\n"
        + "    \"message\": \"AI error (AIError)\",\n"
        + "    \"type\": \"ai_marker\",\n"
        + "    \"severity\": \"error\",\n"
        + "    \"priority\": \"high\",\n"
        + "    \"marker_highlighted_text\": \"calculateTotal(items)\"\n"
        + "    },\n"
        + "    {\n"
        + "    \"id\": 1001,\n"
        + "    \"path\": \"C:/Projects/MyProject/MyProject/Forms/MyForm/Module.bsl\",\n"
        + "    \"start_line\": 5,\n"
        + "    \"message\": \"Syntax error: missing semicolon\",\n"
        + "    \"type\": \"problem\",\n"
        + "    \"severity\": \"error\",\n"
        + "    \"priority\": \"high\",\n"
        + "    \"marker_highlighted_text\": \"a = 1 / 0;\"\n"
        + "    },\n"
        + "    {\n"
        + "    \"id\": 4001,\n"
        + "    \"path\": \"C:/Projects/MyProject/MyProject/CommonModules/AIModule/Module.bsl\",\n"
        + "    \"start_line\": 30,\n"
        + "    \"message\": \"AI warning (AIWarning)\",\n"
        + "    \"type\": \"ai_marker\",\n"
        + "    \"severity\": \"warning\",\n"
        + "    \"priority\": \"high\",\n"
        + "    \"marker_highlighted_text\": \"calculateTotal(items)\"\n"
        + "    }\n"
        + "  ],\n"
        + "  \"total_count\": 5,\n"
        + "  \"returned_count\": 3,\n"
        + "  \"first_index\": 0,\n"
        + "  \"has_more\": true,\n"
        + "  \"next_index\": 3\n"
        + "}";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final IBuildWaiter buildWaiter;
    private final IMarkdownUtils markdownUtils;

    @Inject
    public GetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider, IBuildWaiter buildWaiter, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(buildWaiter);
        Preconditions.checkNotNull(markdownUtils);
        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.buildWaiter = buildWaiter;
        this.markdownUtils = markdownUtils;
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
        var details = new ToolCallMessageDetails();
        details.autoCall = true;
        details.hideAfter = true;

        // Deserialize request parameters
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample
                    + "\n\nRequired field: 'project_name' (string)"
                + "\nOptional fields: 'first_index' (integer), 'max_count' (integer), 'marker_type' (string)");
        }

        var request = optionalRequest.get();
        var projectName = request.projectName;
        if (projectName == null || projectName.isBlank())
        {
            throw new ToolException("Project name is required.");
        }

        int firstIndex = request.firstIndex != null ? Math.max(0, request.firstIndex) : 0;
        int maxCount = request.maxCount != null && request.maxCount > 0 ? request.maxCount : DEFAULT_MAX_ELEMENTS;

        // Parse marker type filter
        MarkerType markerTypeFilter = null;
        if (request.markerType != null && !request.markerType.isBlank())
        {
            markerTypeFilter = MarkerType.fromDisplayName(request.markerType);
            if (markerTypeFilter == null)
            {
                throw new ToolException("Invalid marker_type: " + request.markerType);
            }
        }

        // Synchronous project validation: Check if project exists and is open
        var root = ResourcesPlugin.getWorkspace().getRoot();
        var project = root.getProject(projectName);
        if (project == null || !project.exists())
        {
            throw new ToolException("Project not found: " + projectName);
        }
        if (!project.isOpen())
        {
            throw new ToolException("Project is closed: " + projectName);
        }

        // Create final copy for lambda
        final MarkerType finalMarkerTypeFilter = markerTypeFilter;

        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = Messages.MarkersTitle;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        return buildWaiter.waitForBuilds(project, cancellationToken).thenCompose(voidResult -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation cancelled after build wait");
            }

            return CompletableFuture
                .supplyAsync(() -> createResponse(project, firstIndex, maxCount, finalMarkerTypeFilter, call,
                    cancellationToken, details));
        }).exceptionally(e -> {
            Throwable cause = e.getCause();
            if (cause instanceof OperationCanceledException)
            {
                throw new ToolException("Build waiting cancelled", cause, ToolErrorType.RETRYABLE);
            }
            if (cause instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
                throw new ToolException("Build waiting interrupted", cause, ToolErrorType.RETRYABLE);
            }

            throw new ToolException("Error during build waiting", cause, ToolErrorType.RETRYABLE);
        });
    }

    @SuppressWarnings("nls")
    private ToolCallMessage createResponse(IProject project, int firstIndex, int maxCount, MarkerType markerTypeFilter,
        McpToolCall call,
        ICancellationToken cancellationToken,
        ToolCallMessageDetails details)
    {
        try
        {
            // Early cancellation check
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation cancelled during marker collection");
            }

            // Retrieve all markers in the project
            var markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
            var allMarkers = new ArrayList<MarkerInfo>();

            // Process each marker
            for (var marker : markers)
            {
                // Check cancellation during marker processing
                if (cancellationToken.isCanceled())
                {
                    throw new ToolException("Operation cancelled during marker processing");
                }

                // Determine marker type using enum
                MarkerType markerType = MarkerType.fromTypeId(marker.getType());
                if (markerType == null)
                    continue; // Skip unknown types

                // Apply type filter if specified
                if (markerTypeFilter != null && markerType != markerTypeFilter)
                    continue; // Skip markers that don't match the filter

                var resource = marker.getResource();
                var location = resource.getLocation();

                MarkerInfo markerInfo = new MarkerInfo();
                markerInfo.id = marker.getId();
                markerInfo.path = location != null ? location.toFile().getAbsolutePath() : "";
                markerInfo.startLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                markerInfo.message = marker.getAttribute(IMarker.MESSAGE, "");
                markerInfo.type = markerType.getDisplayName();

                // Set common and type-specific attributes
                setMarkerAttributes(project, marker, markerInfo, markerType, cancellationToken);
                allMarkers.add(markerInfo);
            }

            // Sort markers by importance (severity and priority)
            allMarkers.sort(createMarkerComparator());

            // Apply pagination: get sublist based on firstIndex and maxCount
            List<MarkerInfo> response;
            if (firstIndex >= allMarkers.size())
            {
                response = new ArrayList<>();
            }
            else
            {
                int endIndex = Math.min(firstIndex + maxCount, allMarkers.size());
                response = allMarkers.subList(firstIndex, endIndex);
            }

            String content = json.serialize(response);

            // Add response markdown
            int markerCount = response.size();
            String styledMarkerCount =
                markdownUtils.createStyledText(String.valueOf(markerCount), TextColor.GREEN, FontWeight.BOLD);
            details.responseMarkdown = MessageFormat.format(Messages.MarkersLoadedTemplate, styledMarkerCount);
            details.hideAfter = response.size() == 0;
            return messageFactory.createMessage(this, call, content, details);
        }
        catch (CoreException | OperationCanceledException error)
        {
            if (error instanceof CoreException)
            {
                throw new ToolException("Error retrieving project markers", error, ToolErrorType.RETRYABLE);
            }
            if (error instanceof OperationCanceledException)
            {
                throw new ToolException("Operation cancelled", error, ToolErrorType.RETRYABLE);
            }

            throw new ToolException("Unexpected error", error, ToolErrorType.RETRYABLE);
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
                    markerInfo.markerHighlightedText = readContentFromFile(file, charStart, charEnd - charStart);
                }
            }
            catch (Exception e)
            {
                // Ignore errors and leave markerHighlightedText empty
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
    private Comparator<MarkerInfo> createMarkerComparator()
    {
        return (m1, m2) -> {
            // Compare by severity first (error > warning > info)
            int severityCompare = compareSeverity(m1.severity, m2.severity);
            if (severityCompare != 0)
            {
                return severityCompare;
            }

            // If severity is equal, compare by priority (high > normal > low)
            return comparePriority(m1.priority, m2.priority);
        };
    }

    @SuppressWarnings("nls")
    private int compareSeverity(String severity1, String severity2)
    {
        int severityValue1 = getSeverityValue(severity1);
        int severityValue2 = getSeverityValue(severity2);
        return Integer.compare(severityValue2, severityValue1); // Descending order (higher value first)
    }

    @SuppressWarnings("nls")
    private int getSeverityValue(String severity)
    {
        if ("error".equals(severity))
        {
            return 3;
        }
        else if ("warning".equals(severity))
        {
            return 2;
        }
        else if ("info".equals(severity))
        {
            return 1;
        }
        return 0; // No severity or null
    }

    @SuppressWarnings("nls")
    private int comparePriority(String priority1, String priority2)
    {
        int priorityValue1 = getPriorityValue(priority1);
        int priorityValue2 = getPriorityValue(priority2);
        return Integer.compare(priorityValue2, priorityValue1); // Descending order (higher value first)
    }

    @SuppressWarnings("nls")
    private int getPriorityValue(String priority)
    {
        if ("high".equals(priority))
        {
            return 3;
        }
        else if ("normal".equals(priority))
        {
            return 2;
        }
        else if ("low".equals(priority))
        {
            return 1;
        }
        return 0; // No priority or null
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Lists markers (errors, warnings, tasks, bookmarks, etc.) for a project or file.");
        description.append("\n\nUsage:");
        description.append("\n- Use `marker_type` to filter by type.");
        description.append("\n- Use `relative_file_path` to scope to a file.");
        description.append("\n- Use pagination parameters to page through results.");
        description.append("\n- `ai_marker` includes `AIError`, `AIWarning`, `AIInfo` marker types.");
        description.append("\n- Markers are sorted by importance: severity (error > warning > info) then priority (high > normal > low).");
        description.append("\n- If not all markers are returned, the response will show total count and suggest pagination.");
        description.append("\n\nRelated tools:");
        description.append("\n- Create/update markers: `" + SetMarkersMcpTool.TOOL_NAME + "`.");
        description.append("\n- Remove markers: `" + DeleteMarkersMcpTool.TOOL_NAME + "`.");
        description.append("\n\nPossible marker_type values:");

        // Generate supported types from MarkerType enum
        for (MarkerType type : MarkerType.values())
        {
            description.append("\n- ").append(type.getDisplayName()).append(": ").append(type.getDescription());
        }
        description.append("\n- ai_marker includes AI marker types: AIError, AIWarning, AIInfo.");

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
            .append(
                "\n- marker_highlighted_text: Code fragment associated with the marker (substring of the file at the marker's position)");
        description.append("\n- source_id: Source identifier for bookmarks");
        description.append("\n\nExample request:");
        description.append("\n").append(QuestionExample);
        description.append("\nExample response:");
        description.append("\n").append(AnswerExample);
        description.append("\n\nNote: If not all markers are returned, the response markdown will include:");
        description.append("\n- Total marker count");
        description.append("\n- Number of remaining markers");
        description.append("\n- Pagination suggestion with example request to retrieve remaining markers");
        description.append("\n\nExample of pagination response markdown:");
        description.append("\n```\n**5** markers loaded (**10** total)\n");
        description.append("\n**Additional markers available:** 5 more marker(s) not shown.\n");
        description.append("\nTo retrieve remaining markers, use pagination:\n");
        description.append("- Set `first_index`: 5\n");
        description.append("- Keep or adjust `max_count`: 5\n");
        description.append("\n**Example:**```json\n{\n  \"project_name\": \"MyProject\",\n  \"first_index\": 5,\n  \"max_count\": 5,\n  \"marker_type\": \"ai_marker\"\n}```\n```");

        spec.function.description = description.toString();

        // Define function parameters
        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "Name of the IDE project to retrieve markers from";
        properties.put("project_name", projectNameProp);

        var firstIndexProp = new McpToolCallProperty();
        firstIndexProp.type = "integer";
        firstIndexProp.description = "Index of first element to return (0-based). Default: 0";
        properties.put("first_index", firstIndexProp);

        var maxCountProp = new McpToolCallProperty();
        maxCountProp.type = "integer";
        maxCountProp.description = "Maximum number of elements to return. Default: 64";
        properties.put("max_count", maxCountProp);

        var markerTypeProp = new McpToolCallProperty();
        markerTypeProp.type = "string";
        markerTypeProp.description = "Optional marker type to filter results. Possible values: ";
        boolean firstType = true;
        for (var type : MarkerType.values())
        {
            if (!firstType)
            {
                markerTypeProp.description += ", ";
            }
            markerTypeProp.description += type.getDisplayName();
            firstType = false;
        }
        properties.put("marker_type", markerTypeProp);

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

        @SerializedName("first_index")
        public Integer firstIndex = 0;

        @SerializedName("max_count")
        public Integer maxCount = DEFAULT_MAX_ELEMENTS;

        @SerializedName("marker_type")
        public String markerType;
    }

    // Marker information DTO for JSON serialization
    private static class MarkerInfo
    {
        @SerializedName("id")
        public long id;

        @SerializedName("path")
        public String path;

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

        @SerializedName("marker_highlighted_text")
        public String markerHighlightedText;

        @SerializedName("source_id")
        public String sourceId;
    }

    // Response DTO with pagination information
    private static class GetMarkersResponse
    {
        @SerializedName("markers")
        public List<MarkerInfo> markers;

        @SerializedName("total_count")
        public int totalCount;

        @SerializedName("returned_count")
        public int returnedCount;

        @SerializedName("first_index")
        public int firstIndex;

        @SerializedName("has_more")
        public boolean hasMore;

        @SerializedName("next_index")
        public Integer nextIndex;
    }
}
