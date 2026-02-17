/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.context.tools;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.management.IDtHostResourceManager;
import com._1c.g5.v8.dt.md.IExternalPropertyManagerRegistry;
import com._1c.g5.v8.dt.search.core.BmObjectMatch;
import com._1c.g5.v8.dt.search.core.SearchFor;
import com._1c.g5.v8.dt.search.core.SearchIn;
import com._1c.g5.v8.dt.search.core.SearchScope;
import com._1c.g5.v8.dt.search.core.SimpleSearchResultCollector;
import com._1c.g5.v8.dt.search.core.TextSearchScopeSettings;
import com._1c.g5.v8.dt.search.core.TextSearcher;
import com._1c.g5.v8.dt.search.core.refs.BmRelatedObjectMatch;
import com._1c.g5.v8.dt.search.core.refs.BslReferenceMatch;
import com._1c.g5.v8.dt.search.core.text.ITextSearchIndexProvider;
import com._1c.g5.v8.dt.search.core.text.TextSearchFileMatch;
import com._1c.g5.v8.dt.search.core.text.TextSearchModelMatch;
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
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class FindMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "1C_Find"; //$NON-NLS-1$
    private static final int DEFAULT_MAX_ELEMENTS = McpToolConstants.DEFAULT_MAX_SEARCH_ELEMENTS;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"search_query\": \"*список?контрактов*\",\n"
        + "  \"project_names\": [\"Проект_1\", \"Проект_2\"],\n"
        + "  \"in\": [\n"
        + "    \"metadata\",\n"
        + "    \"attributes\",\n"
        + "    \"forms\"\n"
        + "  ],\n"
        + "  \"for\": [\n"
        + "    \"language_elements\",\n"
        + "    \"comments\"\n"
        + "  ],\n"
        + "  \"scopes\": [\n"
        + "    \"catalogs\",\n"
        + "    \"documents\",\n"
        + "    \"constants\"\n"
        + "  ],\n"
        + "  \"match_case\": true,\n"
        + "  \"first_index\": 0,\n"
        + "  \"max_count\": 64\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"type\": \"Text file\",\n"
        + "    \"project_name\": \"MyProject\",\n"
        + "    \"path\": \"C:/Projects/MyProject/src/CommonModule/Module.bsl\",\n"
        + "    \"text_fragment\": \"Процедура ОбработкаПроведения(Отказ, Режим)\\n    Если Не Режим = РежимПроведения.Проведение Тогда\\n        Возврат;\\n    КонецЕсли;\",\n"
        + "    \"fragment_offset\": 45,\n"
        + "    \"match_length\": 8,\n"
        + "    \"file_offset\": 234,\n"
        + "    \"line_number\": 2\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IBmModelManager searchModelManager;
    private final ITextSearchIndexProvider textSearchIndexProvider;
    private final IExternalPropertyManagerRegistry externalPropertyManagerRegistry;
    private final IDtHostResourceManager hostResourceManager;
    private final IBmModelManager projectModelManager;
    private final IMarkdownUtils markdownUtils;

    @Inject
    public FindMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IBmModelManager searchModelManager, ITextSearchIndexProvider textSearchIndexProvider,
        IExternalPropertyManagerRegistry externalPropertyManagerRegistry, IDtHostResourceManager hostResourceManager,
        IBmModelManager projectModelManager, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(searchModelManager);
        Preconditions.checkNotNull(textSearchIndexProvider);
        Preconditions.checkNotNull(externalPropertyManagerRegistry);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(hostResourceManager);
        Preconditions.checkNotNull(projectModelManager);
        Preconditions.checkNotNull(markdownUtils);

        this.json = json;
        this.messageFactory = messageFactory;
        this.searchModelManager = searchModelManager;
        this.textSearchIndexProvider = textSearchIndexProvider;
        this.externalPropertyManagerRegistry = externalPropertyManagerRegistry;
        this.hostResourceManager = hostResourceManager;
        this.projectModelManager = projectModelManager;
        this.markdownUtils = markdownUtils;

        spec = createSpecification();
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
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
        }

        var request = optionalRequest.get();
        var searchSettings = new TextSearchScopeSettings();

        if (request.searchQuery == null || request.searchQuery.isBlank())
        {
            throw new ToolException("`search_query` cannot be empty.");
        }

        // Validate project names
        if (request.projectNames == null || request.projectNames.isEmpty())
        {
            throw new ToolException("At least one project must be specified in `project_names`.");
        }

        // Convert enums with proper error handling
        var searchInResult = convertEnums(request.searchIn, "in", SearchIn.class);
        if (!searchInResult.errorMessage.isEmpty())
        {
            throw new ToolException(searchInResult.errorMessage);
        }

        var searchForResult = convertEnums(request.searchFor, "for", SearchFor.class);
        if (!searchForResult.errorMessage.isEmpty())
        {
            throw new ToolException(searchForResult.errorMessage);
        }

        var searchScopesResult = convertEnums(request.searchScopes, "scopes", SearchScope.class);
        if (!searchScopesResult.errorMessage.isEmpty())
        {
            throw new ToolException(searchScopesResult.errorMessage);
        }

        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = Messages.Find1CObjectsTitle;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        // Add converted enums to search settings
        searchInResult.values.forEach(searchSettings::addSearchIn);
        searchForResult.values.forEach(searchSettings::addSearchFor);
        searchScopesResult.values.forEach(searchSettings::addSearchScope);

        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            try
            {
                var root = ResourcesPlugin.getWorkspace().getRoot();
                List<IProject> projects = new ArrayList<>();

                // Collect and validate projects
                for (var projectName : request.projectNames)
                {
                    var project = root.getProject(projectName);
                    if (project == null || !project.exists())
                    {
                        throw new ToolException("Project not found: " + projectName);
                    }

                    if (!project.isOpen())
                    {
                        try
                        {
                            project.open(new NullProgressMonitor());
                        }
                        catch (CoreException error)
                        {
                            throw new ToolException("Cannot open project \"" + projectName + "\"", error, ToolErrorType.RETRYABLE);
                        }
                    }

                    projects.add(project);
                }

                // Add projects to search settings
                projects.forEach(searchSettings::addProjects);

                var resultCollector = new SimpleSearchResultCollector();
                var monitor = new NullProgressMonitor();

                var searcher =
                    new TextSearcher(request.searchQuery, request.matchCase, searchSettings, resultCollector,
                        searchModelManager, textSearchIndexProvider, externalPropertyManagerRegistry,
                        hostResourceManager);

                searcher.search(monitor);

                // Check cancellation after search
                if (cancellationToken.isCanceled())
                {
                    throw new ToolException("Operation was cancelled during search.");
                }

                var response = createResponse(resultCollector);
                int effectiveMaxElements = request.maxCount > 0 ? request.maxCount : DEFAULT_MAX_ELEMENTS;
                int firstIndex = Math.max(0, request.firstIndex);

                if (response.size() > firstIndex + effectiveMaxElements)
                {
                    // Remove elements beyond the requested range
                    for (int i = firstIndex + effectiveMaxElements; i < response.size(); i++)
                    {
                        response.remove(i);
                    }
                    // Remove elements before the first index
                    for (int i = 0; i < firstIndex && i < response.size(); i++)
                    {
                        response.remove(0);
                    }
                }
                else if (firstIndex > 0 && response.size() > firstIndex)
                {
                    // Remove elements before the first index if we have enough elements
                    for (int i = 0; i < firstIndex && i < response.size(); i++)
                    {
                        response.remove(0);
                    }
                }

                var content = json.serialize(response);

                // Add response markdown
                int objectCount = response.size();
                String styledObjectCount =
                    markdownUtils.createStyledText(String.valueOf(objectCount), TextColor.GREEN, FontWeight.BOLD);
                details.responseMarkdown = MessageFormat.format(Messages.Found1CObjectsTemplate, styledObjectCount);
                details.hideAfter = response.size() == 0;
                return messageFactory.createMessage(this, call, content, details);
            }
            catch (OperationCanceledException error)
            {
                throw new ToolException("Search failed", error, ToolErrorType.RETRYABLE);
            }
            catch (CoreException error)
            {
                throw new ToolException("Search failed", error, ToolErrorType.RETRYABLE);
            }
        });
    }

    @SuppressWarnings("nls")
    private List<Element> createResponse(SimpleSearchResultCollector collector)
    {
        var elements = new ArrayList<Element>();
        for (var match : collector.getMatches())
        {
            var model = match.getModel();
            var project = projectModelManager.getProject(model);
            var projectName = project != null ? project.getName() : "Unknown";
            if (match instanceof TextSearchFileMatch)
            {
                var src = (TextSearchFileMatch)match;
                var dst = new TextSearchFile();
                dst.type = "Text file";
                dst.projectName = projectName;

                var file = src.getFile();
                if (file != null)
                {
                    var location = file.getRawLocation();
                    if (location != null)
                    {
                        dst.path = location.toOSString();
                    }
                }

                dst.textFragment = src.getText();
                dst.fragmentOffset = src.getTextOffset();
                dst.matchLength = src.getTextLength();
                dst.fileOffset = src.getFileOffset();
                dst.lineNumber = src.getLineNumber();
                elements.add(dst);
            }
            else if (match instanceof TextSearchModelMatch)
            {
                var src = (TextSearchModelMatch)match;
                var dst = new TextSearchModelElement();
                dst.type = "Text in 1C model";
                dst.projectName = projectName;
                dst.propertyValue = src.getText();
                dst.topObjectId = src.getTopObjectId();
                dst.objectId = src.getObjectId();
                dst.valueOffset = src.getTextOffset();
                dst.matchLength = src.getTextLength();
                elements.add(dst);
            }
            else if (match instanceof BslReferenceMatch)
            {
                var src = (BslReferenceMatch)match;
                var dst = new BslReferenceElement();
                dst.type = "Reference to 1C code";
                dst.projectName = projectName;

                var optionalTarget = src.getTarget();
                if (optionalTarget.isPresent())
                {
                    var target = optionalTarget.get();
                    dst.targetMetadataTopObjectId = target.getMetadataTopObjectId();
                    dst.targetObjectId = target.getObjectId();
                }
                elements.add(dst);
            }
            else if (match instanceof BmRelatedObjectMatch)
            {
                var src = (BmRelatedObjectMatch)match;
                var dst = new BmRelatedObjectElement();
                dst.type = "1C related object";
                dst.projectName = projectName;
                dst.objectId = src.getObjectId();

                var optionalTarget = src.getTarget();
                if (optionalTarget.isPresent())
                {
                    var target = optionalTarget.get();
                    dst.targetMetadataTopObjectId = target.getMetadataTopObjectId();
                    dst.targetObjectId = target.getObjectId();
                }
                elements.add(dst);
            }
            else if (match instanceof BmObjectMatch)
            {
                var src = (BmObjectMatch)match;
                var dst = new BmObjectElement();
                dst.type = "1C object";
                dst.projectName = projectName;
                dst.objectId = src.getObjectId();
                elements.add(dst);
            }
        }

        return elements;
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Finds 1C project elements (objects, attributes, forms, code, etc.).");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Use wildcards in `search_query` for broad search.");
        description.append("\n- Narrow by project and type to reduce noise.");
        description.append("\n\nRelated tools:");
        description.append("\n- Fetch by id: `" + GetObjectMcpTool.TOOL_NAME + "`.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var searchQueryProp = new McpToolCallProperty();
        searchQueryProp.type = "string";
        searchQueryProp.description = "Search query. Wildcards are supported.";
        properties.put("search_query", searchQueryProp);

        var projectNamesProp = new McpToolCallProperty();
        projectNamesProp.type = "array";
        projectNamesProp.description = "1C project names as a JSON array of strings.";
        properties.put("project_names", projectNamesProp);

        var searchInProp = new McpToolCallProperty();
        searchInProp.type = "array";
        searchInProp.description = "Where to search - elements as a JSON array of strings. Valid values: " + getEnumNames(SearchIn.class) + ".";
        properties.put("in", searchInProp);

        var searchForProp = new McpToolCallProperty();
        searchForProp.type = "array";
        searchForProp.description = "What to search for - elements as a JSON array of strings. Valid values: " + getEnumNames(SearchFor.class) + ".";
        properties.put("for", searchForProp);

        var searchScopesProp = new McpToolCallProperty();
        searchScopesProp.type = "array";
        searchScopesProp.description = "The scope of the search - elements as a JSON array of strings. Valid values: " + getEnumNames(SearchScope.class) + ".";
        properties.put("scopes", searchScopesProp);

        var matchCaseProp = new McpToolCallProperty();
        matchCaseProp.type = "boolean";
        matchCaseProp.description = "Case-sensitive search. Default: false";
        properties.put("match_case", matchCaseProp);

        var firstIndexProp = new McpToolCallProperty();
        firstIndexProp.type = "integer";
        firstIndexProp.description = "Index of first element to return (0-based). Default: 0";
        properties.put("first_index", firstIndexProp);

        var maxCountProp = new McpToolCallProperty();
        maxCountProp.type = "integer";
        maxCountProp.description = "Maximum number of elements to return. Default: 64";
        properties.put("max_count", maxCountProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("search_query", "project_names", "in", "for", "scopes");

        spec.function.parameters = parameters;
        return spec;
        // @formatter:on
    }

    @SuppressWarnings("nls")
    private static <TEnum extends Enum<TEnum>> EnumConversionResult<TEnum> convertEnums(List<String> source,
        String paramName, Class<TEnum> targetType)
    {
        if (source == null || source.isEmpty())
        {
            return new EnumConversionResult<>(Collections.emptyList(), "");
        }

        var convertedValues = new ArrayList<TEnum>();
        var invalidValues = new ArrayList<String>();

        for (var value : source)
        {
            var converted = convertStringToEnum(value, targetType);
            if (converted.isPresent())
            {
                convertedValues.add(converted.get());
            }
            else
            {
                invalidValues.add(value);
            }
        }

        if (!invalidValues.isEmpty())
        {
            String errorMsg = String.format("Invalid values for parameter `%s`: %s. Valid values: %s.", paramName,
                String.join(", ", invalidValues), getEnumNames(targetType));
            return new EnumConversionResult<>(Collections.emptyList(), errorMsg);
        }

        return new EnumConversionResult<>(convertedValues, "");
    }

    private static <E extends Enum<E>> Optional<E> convertStringToEnum(String source, Class<E> targetType)
    {
        if (source == null)
        {
            return Optional.empty();
        }
        try
        {
            return Optional.of(Enum.valueOf(targetType, source.toUpperCase()));
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    @SuppressWarnings("nls")
    private static <TEnum extends Enum<TEnum>> String getEnumNames(Class<TEnum> targetType)
    {
        return Arrays.stream(targetType.getEnumConstants())
            .map(Enum::name)
            .map(String::toLowerCase)
            .collect(Collectors.joining(", "));
    }

    private static class EnumConversionResult<T>
    {
        public final List<T> values;
        public final String errorMessage;

        public EnumConversionResult(List<T> values, String errorMessage)
        {
            this.values = values;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Represents a search request configuration
     */
    private static class Request
    {
        @SerializedName("search_query")
        public String searchQuery;

        @SerializedName("project_names")
        public List<String> projectNames;

        @SerializedName("in")
        public List<String> searchIn;

        @SerializedName("for")
        public List<String> searchFor;

        @SerializedName("scopes")
        public List<String> searchScopes;

        @SerializedName("match_case")
        public boolean matchCase = false;

        @SerializedName("first_index")
        public int firstIndex = 0;

        @SerializedName("max_count")
        public int maxCount = DEFAULT_MAX_ELEMENTS;
    }

    /**
     * Base element for search results
     */
    private static class Element
    {
        /**
         * Type of the search result element
         */
        @SerializedName("type")
        public String type;

        /**
         * Name of the project
         */
        @SerializedName("project_name")
        public String projectName;
    }

    /**
     * Represents a text search result within a file
     * type = "Text file"
     */
    private static class TextSearchFile
        extends Element
    {
        /**
         * Absolute path to the file
         */
        @SerializedName("path")
        public String path;

        /**
         * Text fragment containing the search match
         */
        @SerializedName("text_fragment")
        public String textFragment;

        /**
         * Offset of the match within the text fragment
         */
        @SerializedName("fragment_offset")
        public int fragmentOffset;

        /**
         * Length of the matched text
         */
        @SerializedName("match_length")
        public int matchLength;

        /**
         * Offset of the match in the file
         */
        @SerializedName("file_offset")
        public int fileOffset;

        /**
         * Line number of the match in the file
         */
        @SerializedName("line_number")
        public long lineNumber;
    }

    /**
     * Represents a metadata search result
     * type = "Text in 1C model"
     */
    private static class TextSearchModelElement
        extends Element
    {
        /**
         * Property value containing the search text
         */
        @SerializedName("property_value")
        public String propertyValue;

        /**
         * ID of the top-level metadata object
         */
        @SerializedName("top_object_id")
        public long topObjectId;

        /**
         * ID of the matched object
         */
        @SerializedName("object_id")
        public long objectId;

        /**
         * Offset of the match in property value
         */
        @SerializedName("value_offset")
        public int valueOffset;

        /**
         * Length of the matched text
         */
        @SerializedName("match_length")
        public int matchLength;
    }

    /**
     * Represents a BSL reference between objects
     * type = "Reference to 1C code"
     */
    private static class BslReferenceElement
        extends Element
    {
        /**
         * Target top-level metadata object ID
         */
        @SerializedName("target_top_object_id")
        public long targetMetadataTopObjectId;

        /**
         * Target object ID
         */
        @SerializedName("target_object_id")
        public long targetObjectId;
    }

    /**
     * Represents a related metadata object in search results
     * type = "1C reletaed object"
     */
    private static class BmRelatedObjectElement
        extends Element
    {
        /**
         * ID of the matched object
         */
        @SerializedName("object_id")
        public long objectId;

        /**
         * Target top-level metadata object ID
         */
        @SerializedName("target_top_object_id")
        public long targetMetadataTopObjectId;

        /**
         * Target object ID
         */
        @SerializedName("target_object_id")
        public long targetObjectId;
    }

    /**
     * Represents a base metadata object in search results
     * type = "1C object"
     */
    private static class BmObjectElement
        extends Element
    {
        /**
         * ID of the matched object
         */
        @SerializedName("object_id")
        public long objectId;
    }
}
