/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import static com.e1c.edt.ai.tools.StreamUtils.distinctBy;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.OperationCanceledException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.MarkerInfo;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
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
        + "  \"max_count\": 3,\n"
        + "  \"marker_type\": \"ai_marker\"\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\n"
        + "  \"markers\": [\n"
        + "    {\n"
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
        + "  \"total_markers\": 5\n"
        + "}";

    @SuppressWarnings("nls")
    private static String QuestionExampleWithPath =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  \"path\": \"src/MyModule/Module.bsl\",\n"
        + "  \"max_count\": 5\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExampleWithPath =
        "{\n"
        + "  \"markers\": [\n"
        + "    {\n"
        + "    \"id\": 1001,\n"
        + "    \"path\": \"C:/Projects/MyProject/MyProject/src/MyModule/Module.bsl\",\n"
        + "    \"start_line\": 5,\n"
        + "    \"message\": \"Syntax error: missing semicolon\",\n"
        + "    \"type\": \"problem\",\n"
        + "    \"severity\": \"error\",\n"
        + "    \"priority\": \"high\",\n"
        + "    \"location\": \"line: 5 /MyProject/src/MyModule/Module.bsl\",\n"
        + "    \"marker_highlighted_text\": \"a = 1 / 0;\"\n"
        + "    },\n"
        + "    {\n"
        + "    \"id\": 4002,\n"
        + "    \"path\": \"C:/Projects/MyProject/MyProject/src/MyModule/Module.bsl\",\n"
        + "    \"start_line\": 15,\n"
        + "    \"message\": \"Variable 'x' is never used\",\n"
        + "    \"type\": \"problem\",\n"
        + "    \"severity\": \"warning\",\n"
        + "    \"priority\": \"normal\",\n"
        + "    \"location\": \"line: 15 /MyProject/src/MyModule/Module.bsl\",\n"
        + "    \"marker_highlighted_text\": \"var x = 10;\"\n"
        + "    }\n"
        + "  ],\n"
        + "  \"total_markers\": 2\n"
        + "}";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IBuildWaiter buildWaiter;
    private final IMarkdownUtils markdownUtils;
    private final IProjectTools projectTools;
    private final Set<IMarkersProvider> markersProviders;

    @Inject
    public GetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IBuildWaiter buildWaiter,
        IMarkdownUtils markdownUtils, IProjectTools projectTools, Set<IMarkersProvider> markersProviders)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(buildWaiter);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(markersProviders);

        this.json = json;
        this.messageFactory = messageFactory;
        this.buildWaiter = buildWaiter;
        this.markdownUtils = markdownUtils;
        this.projectTools = projectTools;
        this.markersProviders = markersProviders;

        this.spec = createSpecification();
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
                .supplyAsync(() -> createResponse(project, firstIndex, maxCount, finalMarkerTypeFilter, request.path,
                    call, cancellationToken, details));
        });
    }

    @SuppressWarnings("nls")
    private ToolCallMessage createResponse(IProject project, int firstIndex, int maxCount, MarkerType markerTypeFilter,
        String path,
        McpToolCall call,
        ICancellationToken cancellationToken,
        ToolCallMessageDetails details)
    {
        IFile file = null;
        if (path != null && !path.isBlank())
        {
            file = projectTools.getProjectFile(project, path).orElse(null);
            if (file == null)
            {
                throw new ToolException("The file \"" + path + "\" does not exist in the project.");
            }
            if (!file.exists())
            {
                throw new ToolException("The file \"" + path + "\" does not exist.");
            }
        }

        try
        {
            Stream<MarkerInfo> allMarkers = Stream.empty();
            for (var markersProvider : markersProviders)
            {
                var markers =
                    markersProvider.getMarkers(project, file).filter(applyMarkerFilters(markerTypeFilter, path));

                allMarkers = Stream.concat(allMarkers, markers);
            }

            // Collect all markers to get total count
            var allMarkersList = distinctBy(
                allMarkers.sorted(new MarkerInfoComparator()).takeWhile(i -> !cancellationToken.isCanceled()),
                marker -> new MarkerKey(marker.path, marker.startLine, marker.message))
                        .collect(Collectors.toList());

            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation cancelled during marker collection");
            }

            // Get the requested page of markers
            var markersPage = allMarkersList.stream().skip(firstIndex).limit(maxCount).collect(Collectors.toList());

            // Create response object
            var response = new GetMarkersResponse();
            response.markers = markersPage;
            response.totalMarkers = allMarkersList.size();
            response.requiredNextStep = buildRequiredNextStep(markerTypeFilter, path, markersPage, allMarkersList.size(),
                firstIndex, maxCount);

            var content = json.serialize(response);

            // Add response markdown
            var styledMarkerCount =
                markdownUtils.createStyledText(String.format("%d/%d", markersPage.size(), allMarkersList.size()),
                    TextColor.GREEN, FontWeight.BOLD, true);
            details.responseMarkdown = MessageFormat.format(Messages.MarkersLoadedTemplate, styledMarkerCount);
            details.hideAfter = markersPage.size() == 0;
            return messageFactory.createMessage(this, call, content, details);
        }
        catch (OperationCanceledException error)
        {
            throw new ToolException("Operation cancelled", error, ToolErrorType.RETRYABLE);
        }
    }

    @SuppressWarnings("nls")
    private String buildRequiredNextStep(MarkerType markerTypeFilter, String path, List<MarkerInfo> returnedMarkers,
        int totalCount, int firstIndex, int maxCount)
    {
        if (totalCount == 0)
        {
            return null;
        }

        var next = new StringBuilder();
        if (markerTypeFilter == MarkerType.M1C)
        {
            next.append("1C markers remain. Do not report success. Fix all relevant markers for the changed ")
                .append("entity/top object, including errors, warnings, and infos. For SU45/type markers, set a fresh ")
                .append("TypeDescription instance on every BasicFeature child before adding it to the parent; do not ")
                .append("reuse the same TypeDescription object across multiple attributes, dimensions, or resources.");
            if (path == null || path.isBlank())
            {
                next.append(" If this GetMarkers call follows JShell CRUD and the changed top-level .mdo paths are ")
                    .append("known or derivable, call GetMarkers again with path for each changed entity and fix only ")
                    .append("markers relevant to those entities. Do not repair unrelated project-wide markers.");
            }
            append1CMarkerSpecificHints(next, returnedMarkers);
        }
        else
        {
            next.append("Markers remain. Inspect and fix all relevant returned markers before reporting success.");
        }

        var nextIndex = firstIndex + returnedMarkers.size();
        if (nextIndex < totalCount)
        {
            next.append(" Pagination is incomplete: call GetMarkers again with first_index ")
                .append(nextIndex)
                .append(" and max_count ")
                .append(maxCount)
                .append(" to inspect the remaining markers.");
        }
        return next.toString();
    }

    @SuppressWarnings("nls")
    private void append1CMarkerSpecificHints(StringBuilder next, List<MarkerInfo> markers)
    {
        if (markers == null || markers.isEmpty())
        {
            return;
        }

        var messages = markers.stream()
            .map(marker -> marker.message == null ? "" : marker.message)
            .collect(Collectors.joining("\n"));

        if (messages.contains("Совпадает с именем стандартного реквизита")
            || messages.contains("standard"))
        {
            next.append(" If a document attribute name duplicates a standard document property, remove the custom ")
                .append("attribute: documents already have built-in Date/Дата, Number/Номер, Posted/Проведен, ")
                .append("Ref/Ссылка, and DeletionMark/ПометкаУдаления.");
        }
        if (messages.contains("Точность числа не может быть больше его длины"))
        {
            next.append(" For number qualifiers, TypeDescriptionBuilder uses setNumberQualifiers(scale, precision, ")
                .append("nonNegative): for Number(10,2) call setNumberQualifiers(2, 10, ...), not (10, 2, ...).");
        }
        if (messages.contains("Переменная длина строки должна быть внутри диапазона"))
        {
            next.append(" For string qualifiers, use a valid finite length for this EDT version, usually ")
                .append("setStringQualifiers(100, false); avoid large values such as 1000.");
        }
        if (messages.contains("Некорректный состав регистраторов регистра")
            || messages.contains("регистратором для регистра"))
        {
            next.append(" For register registrar markers, link at least one document to the register via ")
                .append("document.getRegisterRecords().add(register); registrars are configured on documents, not on the register.");
        }
    }

    private static Predicate<MarkerInfo> applyMarkerFilters(MarkerType markerTypeFilter,
        String path)
    {
        return new Predicate<>()
        {
            @Override
            public boolean test(MarkerInfo marker)
            {
                return (markerTypeFilter == null
                    || (marker.type != null && MarkerType.fromTypeId(marker.type) == markerTypeFilter))
                    && (path == null || path.isBlank() || java.util.Objects.equals(path, marker.path));
            }
        };
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
        description.append("\n- Use `path` (absolute or project-relative) to scope to a specific file.");
        description.append("\n- Use pagination parameters to page through results.");
        description.append("\n- `ai_marker` includes `AIError`, `AIWarning`, `AIInfo` marker types.");
        description.append("\n- `marker_type: \"1c\"` returns 1C validation markers of all severities. After 1C metadata CRUD, pass `path` to each changed top-level `.mdo` file when known or derivable, inspect only markers relevant to the changed entities/top objects, including errors, warnings, and infos, and do not check only errors.");
        description.append("\n- After JShell CRUD, do not use project-wide results to fix unrelated existing markers. Use project-wide `GetMarkers` only for delete, rename, registrar links, references, command interfaces, configuration-level changes, or when the changed `.mdo` path cannot be derived; even then, filter fixes to the changed entities and directly affected references.");
        description.append("\n- If returned 1C markers contain SU45/type-required messages for attributes, dimensions, or resources, the next action is to fix the metadata with JShell. Each BasicFeature child needs its own fresh TypeDescription instance; do not reuse one TypeDescription across several children.");
        description.append("\n- Non-empty marker responses include JSON field `required_next_step`; follow it before reporting success.");
        description.append("\n- After changing one 1C entity, prefer `path` to the changed `.mdo` file when known; otherwise use project scope only as a fallback. For references, delete, rename, registrars, and command interfaces, use project scope but fix only affected markers.");
        description.append("\n- For 1C CRUD validation, use `max_count` large enough (for example 200) and paginate until all relevant markers are inspected.");
        description.append("\n- Markers are sorted by importance: severity (error > warning > info) then priority (high > normal > low).");
        description.append("\n- If not all markers are returned, the response will show total count and suggest pagination.");
        description.append("\n- After a JShell metadata/resource change, wait for the JShell result before calling this tool for the same project.");
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
        description.append("\n- markers: Array of marker objects");
        description.append("\n  - id: Unique marker identifier (long number)");
        description.append("\n  - absolute_path: Absolute file system path (OS-dependent format)");
        description.append("\n  - relative_path: Project-relative path");
        description.append(
            "\n  - start_line: Line number (-1 if unknown). An integer value indicating the line number for a marker. It is 1-relative.");
        description.append("\n  - message: Marker description");
        description.append("\n  - type: Marker type (");
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
        description.append("\n  - severity: For problems and AI markers (error, warning, info)");
        description.append("\n  - priority: For problems, tasks and AI markers (high, normal, low)");
        description.append("\n  - done: For bookmarks and tasks (true/false)");
        description.append(
            "\n  - location: Human-readable location string. The location is a human-readable (localized) string which can be used to distinguish between markers on a resource. As such it should be concise and aimed at users.");
        description
            .append(
                "\n  - marker_highlighted_text: Code fragment associated with the marker (substring of the file at the marker's position)");
        description.append("\n  - source_id: Source identifier for bookmarks");
        description.append("\n- total_markers: Total number of markers available");
        description.append("\n\nExample request:");
        description.append("\n").append(QuestionExample);
        description.append("\nExample response:");
        description.append("\n").append(AnswerExample);
        description.append("\n\nExample request with path parameter:");
        description.append("\n").append(QuestionExampleWithPath);
        description.append("\nExample response with path parameter:");
        description.append("\n").append(AnswerExampleWithPath);
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
        maxCountProp.description = "Maximum number of elements to return. Default: 32";
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

        var pathProp = new McpToolCallProperty();
        pathProp.type = "string";
        pathProp.description = "Optional project-relative path to file";
        properties.put("path", pathProp);

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

        @SerializedName("path")
        public String path;
    }

    // Response DTO with pagination information
    private static class GetMarkersResponse
    {
        @SerializedName("markers")
        public List<MarkerInfo> markers;

        @SerializedName("total_markers")
        public int totalMarkers;

        @SerializedName("required_next_step")
        public String requiredNextStep;
    }
}
