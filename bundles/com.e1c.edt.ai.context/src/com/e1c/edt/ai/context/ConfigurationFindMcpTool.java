/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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


public class ConfigurationFindMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "1с_ide_configuration_find"; //$NON-NLS-1$

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
        + "  \"match_case\": true\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"type\": \"Text file\",\n"
        + "    \"absolute_file_path\": \"C:/Projects/MyProject/src/CommonModule/Module.bsl\",\n"
        + "    \"project_relative_file_path\": \"src/CommonModule/Module.bsl\",\n"
        + "    \"text_fragment\": \"Процедура ОбработкаПроведения(Отказ, Режим)\\n    Если Не Режим = РежимПроведения.Проведение Тогда\\n        Возврат;\\n    КонецЕсли;\",\n"
        + "    \"fragment_offset\": 45,\n"
        + "    \"match_length\": 8,\n"
        + "    \"file_offset\": 234,\n"
        + "    \"line_number\": 2\n"
        + "  },\n"
        + "  {\n"
        + "    \"type\": \"Text in 1C model\",\n"
        + "    \"property_value\": \"Сумма: 15000 руб.\",\n"
        + "    \"top_object_id\": 1000001,\n"
        + "    \"object_id\": 2000001,\n"
        + "    \"value_offset\": 7,\n"
        + "    \"match_length\": 5\n"
        + "  },\n"
        + "  {\n"
        + "    \"type\": \"Reference to 1C code\",\n"
        + "    \"target_top_object_id\": 5000001,\n"
        + "    \"target_object_id\": 6000001\n"
        + "  },\n"
        + "  {\n"
        + "    \"type\": \"1C model reference\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"type\": \"1C reletaed object\",\n"
        + "    \"object_id\": 3000001,\n"
        + "    \"target_top_object_id\": 4000001,\n"
        + "    \"target_object_id\": 5000002\n"
        + "  },\n"
        + "  {\n"
        + "    \"type\": \"1C object\",\n"
        + "    \"object_id\": 7000001\n"
        + "  },\n"
        + "  {\n"
        + "    \"type\": \"Text file\",\n"
        + "    \"absolute_file_path\": \"C:/Projects/Accounting/src/Reports/Report.bsl\",\n"
        + "    \"project_relative_file_path\": \"src/Reports/Report.bsl\",\n"
        + "    \"text_fragment\": \"Функция ПодготовитьДанные()\\n    Данные = Новый Массив;\\n    Данные.Добавить(Новый Структура(\\\"Наименование, Количество\\\", \\\"Товар А\\\", 100));\",\n"
        + "    \"fragment_offset\": 85,\n"
        + "    \"match_length\": 7,\n"
        + "    \"file_offset\": 345,\n"
        + "    \"line_number\": 3\n"
        + "  },\n"
        + "  {\n"
        + "    \"type\": \"Text in 1C model\",\n"
        + "    \"property_value\": \"ВидДвижения: Приход\",\n"
        + "    \"top_object_id\": 1000002,\n"
        + "    \"object_id\": 2000002,\n"
        + "    \"value_offset\": 13,\n"
        + "    \"match_length\": 6\n"
        + "  }\n"
        + "]";

    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IBmModelManager bmModelManager;
    private final ITextSearchIndexProvider textSearchIndexProvider;
    private final IExternalPropertyManagerRegistry externalPropertyManagerRegistry;
    private final IDtHostResourceManager hostResourceManager;

    @Inject
    public ConfigurationFindMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IBmModelManager bmModelManager, ITextSearchIndexProvider textSearchIndexProvider,
        IExternalPropertyManagerRegistry externalPropertyManagerRegistry, IDtHostResourceManager hostResourceManager)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(bmModelManager);
        Preconditions.checkNotNull(textSearchIndexProvider);
        Preconditions.checkNotNull(externalPropertyManagerRegistry);
        Preconditions.checkNotNull(hostResourceManager);
        this.json = json;
        this.messageFactory = messageFactory;
        this.bmModelManager = bmModelManager;
        this.textSearchIndexProvider = textSearchIndexProvider;
        this.externalPropertyManagerRegistry = externalPropertyManagerRegistry;
        this.hostResourceManager = hostResourceManager;
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
        var optionalCallArgs = json.deserialize(call.function.arguments, CallArguments.class);
        if (optionalCallArgs.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var callArgs = optionalCallArgs.get();
        var searchSettings = new TextSearchScopeSettings();
        if (callArgs.searchQuery == null || callArgs.searchQuery.isBlank())
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call, "'search_query' cannot be empty."));
        }

        var searchIn = convertEnums(callArgs.searchIn, "search_in", SearchIn.class);
        if (!searchIn.ErrorMessage.isBlank())
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call, searchIn.ErrorMessage));
        }

        searchIn.Values.forEach(searchSettings::addSearchIn);

        var searchFor = convertEnums(callArgs.searchFor, "search_for", SearchFor.class);
        if (!searchFor.ErrorMessage.isBlank())
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call, searchFor.ErrorMessage));
        }

        searchFor.Values.forEach(searchSettings::addSearchFor);

        var searchScopes = convertEnums(callArgs.searchScopes, "search_scopes", SearchScope.class);
        if (!searchScopes.ErrorMessage.isBlank())
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call, searchScopes.ErrorMessage));
        }

        searchScopes.Values.forEach(searchSettings::addSearchScope);

        // Return a CompletableFuture that will be completed asynchronously
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the heavy operation
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var monitor = new ProgressMonitor();
            var root = ResourcesPlugin.getWorkspace().getRoot();
            for(var projectName: callArgs.projectNames)
            {
                var project = root.getProject(projectName);
                if (project == null)
                {
                    return messageFactory.createError(this, call, "Cannot get the project \"" + projectName + "\".");
                }

                if (!project.exists())
                {
                    return messageFactory.createError(this, call, "The project \"" + projectName + "\" does not exist.");
                }

                if (!project.isOpen())
                {
                    try
                    {
                        project.open(monitor);
                    }
                    catch (CoreException error)
                    {
                        return messageFactory.createError(this, call, "Cannot open the project \"" + projectName + "\". " + error.getMessage());
                    }
                }

                if (!project.isOpen())
                {
                    return messageFactory.createError(this, call, "Cannot open the project \"" + projectName + "\". ");
                }

                searchSettings.addProjects(project);
            }

            var resultCollector = new SimpleSearchResultCollector();
            var searcher = new TextSearcher(callArgs.searchQuery, callArgs.matchCase, searchSettings, resultCollector, bmModelManager,
                textSearchIndexProvider,
                externalPropertyManagerRegistry,
                hostResourceManager);

            try
            {
                searcher.search(monitor);
            }
            catch (OperationCanceledException | CoreException error)
            {
                return messageFactory.createError(this, call, "Cannot search. " + error.getMessage());
            }

            var elements = new ArrayList<Element>();
            for (var match : resultCollector.getMatches())
            {
                if (match instanceof TextSearchFileMatch)
                {
                    var src = (TextSearchFileMatch)match;
                    var dst = new TextSearchFile();
                    dst.type = "Text file";
                    var file = src.getFile();
                    if (file != null)
                    {
                        var location = file.getRawLocation();
                        if (location != null)
                        {
                            dst.absoluteFilePath = location.toOSString();
                        }

                        var relativePath = file.getProjectRelativePath();
                        if (relativePath != null)
                        {
                            dst.projectRelativeFilePath = relativePath.toOSString();
                        }
                    }


                    dst.textFragment = src.getText();
                    dst.fragmentOffset = src.getTextOffset();
                    dst.matchLength = src.getTextLength();
                    dst.fileOffset = src.getFileOffset();
                    dst.lineNumber = src.getLineNumber();
                    elements.add(dst);
                }

                if (match instanceof TextSearchModelMatch)
                {
                    var src = (TextSearchModelMatch)match;
                    var dst = new TextSearchModelElement();
                    dst.type = "Text in 1C model";
                    dst.propertyValue = src.getText();
                    dst.topObjectId = src.getTopObjectId();
                    dst.objectId = src.getObjectId();
                    dst.valueOffset = src.getTextOffset();
                    dst.matchLength = src.getTextLength();
                    elements.add(dst);
                }

                if (match instanceof BslReferenceMatch)
                {
                    var src = (BslReferenceMatch)match;
                    var dst = new BslReferenceElement();
                    dst.type = "Reference to 1C code";
                    // dst.metadataTopObjectId = src.getMetadataTopObjectId();
                    var optionalTarget = src.getTarget();
                    if(optionalTarget.isPresent())
                    {
                        var target = optionalTarget.get();
                        dst.targetMetadataTopObjectId = target.getMetadataTopObjectId();
                        dst.targetObjectId = target.getObjectId();
                    }

                    elements.add(dst);
                }

                /*if (match instanceof BmReferenceMatch)
                {
                    var src = (BmReferenceMatch)match;
                    var dst = new BmReferenceElement();
                    dst.type = "1C model reference";
                    elements.add(dst);
                }*/

                if (match instanceof BmRelatedObjectMatch)
                {
                    var src = (BmRelatedObjectMatch)match;
                    var dst = new BmRelatedObjectElement();
                    dst.type = "1C reletaed object";
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

                if (match instanceof BmObjectMatch)
                {
                    var src = (BmObjectMatch)match;
                    var dst = new BmObjectElement();
                    dst.type = "1C object";
                    dst.objectId = src.getObjectId();
                    elements.add(dst);
                }
            }

            return elements;
        }).handle((result, ex) -> {
            if (ex != null)
            {
                // Handle exceptions from the async block
                String errorMessage = ex.getCause() instanceof CoreException || ex.getCause() instanceof OperationCanceledException
                    ? "Cannot search. " + ex.getMessage()
                    : ex.getMessage();

                return messageFactory.createError(this, call, errorMessage);
            }

            // Handle successful result
            var content = json.serialize(result);
            return messageFactory.createMessage(this, call, content);
        });
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
     // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name =TOOL_NAME;

        var description = new StringBuilder();

        description.append("Finds elements (objects, attributes, forms, code, etc) in 1C projects.");
        description.append("\nIMPORTANT: use wildcards (in 'search_query') for a broad search.");
        description.append("\nIMPORTANT: use " + GetObjectByIdMcpTool.TOOL_NAME + " tool to get an object by its id.");
        description.append("\nNOTE: add a description of what will be done when using this tool.");

        description.append("\nFor exapmple:");
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
        projectNamesProp.type = "object";
        projectNamesProp.description = "1C project names as a JSON array of strings.";
        properties.put("search_project_names", projectNamesProp);

        var searchInProp = new McpToolCallProperty();
        searchInProp.type = "object";
        searchInProp.description = "Where search for the search query - elements as a JSON array of strings. Valid values are: " + getEnumNames(SearchIn.class) + ".";
        properties.put("search_in", searchInProp);

        var searchForProp = new McpToolCallProperty();
        searchForProp.type = "object";
        searchForProp.description = "What to search for - elements as a JSON array of strings. Valid values are: " + getEnumNames(SearchFor.class) + ".";
        properties.put("search_for", searchForProp);

        var searchScopesProp = new McpToolCallProperty();
        searchScopesProp.type = "object";
        searchScopesProp.description = "The scope of the search - elements as a JSON array of strings. Valid values are: " + getEnumNames(SearchScope.class) + ".";
        properties.put("search_scopes", searchScopesProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("search_query", "search_project_names", "search_in", "search_for", "search_scopes");

        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    @SuppressWarnings("nls")
    private static <TEnum extends Enum<TEnum>> ConvertEnumResult<TEnum> convertEnums(List<String> source,
        String sourceName, Class<TEnum> targetType)
    {
        if (source != null && !source.isEmpty())
        {
            var convertedValues = new ArrayList<TEnum>();
            var errorMessage = new StringBuilder();
            for (var value : source)
            {
                var optionalConvertedValue = convertStringToEnum(value, targetType);
                if (optionalConvertedValue.isEmpty())
                {
                    errorMessage.append("Value '");
                    errorMessage.append(value);
                    errorMessage.append("' is not a valid for field '");
                    errorMessage.append(sourceName);
                    errorMessage.append("'.");
                    continue;
                }

                convertedValues.add(optionalConvertedValue.get());
            }

            if (errorMessage.length() > 0)
            {
                errorMessage.append("Valid values are: ");
                errorMessage.append(getEnumNames(targetType));
                errorMessage.append('.');
            }

            return new ConvertEnumResult<>(convertedValues, errorMessage.toString());
        }

        return new ConvertEnumResult<>(new ArrayList<>(), ""); //$NON-NLS-1$
    }

    private static <E1 extends Enum<E1>, E2 extends Enum<E2>> Optional<E2> convertStringToEnum(String source,
        Class<E2> targetType)
        throws IllegalArgumentException
    {
        if (source == null)
        {
            return Optional.empty();
        }
        try
        {
            return Optional.ofNullable(Enum.valueOf(targetType, source.toUpperCase()));
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
            .map(i -> '"' + i.name().toLowerCase() + '"')
            .collect(Collectors.joining(", "));
    }

    private static class ConvertEnumResult<T>
    {
        public final Iterable<T> Values;

        public final String ErrorMessage;

        public ConvertEnumResult(Iterable<T> values, String errorMessage)
        {
            Preconditions.checkNotNull(values);
            Preconditions.checkNotNull(errorMessage);
            Values = values;
            ErrorMessage = errorMessage;
        }
    }

    /**
     * Represents a search request configuration
     */
    private static class CallArguments
    {
        /**
         * The search query
         */
        @SerializedName("search_query")
        public String searchQuery;

        /**
         * Projects to search in
         */
        @SerializedName("search_project_names")
        public List<String> projectNames = new ArrayList<>();

        /**
         * Defines where to search for the search query.
         * @see SearchInType
         */
        @SerializedName("search_in")
        public List<String> searchIn = new ArrayList<>();

        /**
         * Defines what to search for.
         * @see SearchForType
         */
        @SerializedName("search_for")
        public List<String> searchFor = new ArrayList<>();

        /**
         * Defines the scope of the search.
         * @see SearchScopeType
         */
        @SerializedName("search_scopes")
        public List<String> searchScopes = new ArrayList<>();

        /**
         * Defines the case sensitivity of the search.
         */
        @SerializedName("match_case")
        public boolean matchCase = false;
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
        @SerializedName("absolute_file_path")
        public String absoluteFilePath;

        /**
         * Project-relative path to the file
         */
        @SerializedName("project_relative_file_path")
        public String projectRelativeFilePath;

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
     * Represents a base metadata reference element (currently without additional fields)
     * type = "1C model reference"
     */
    /*private static class BmReferenceElement
        extends Element
    {
        // Intentionally empty - used as base for metadata references
    }*/

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

    private static class ProgressMonitor
        implements IProgressMonitor
    {
        @Override
        public void beginTask(String name, int totalWork)
        {
            //
        }

        @Override
        public void done()
        {
            //
        }

        @Override
        public void internalWorked(double work)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean isCanceled()
        {
            return false;
        }

        @Override
        public void setCanceled(boolean value)
        {
            //

        }

        @Override
        public void setTaskName(String name)
        {
            //
        }

        @Override
        public void subTask(String name)
        {
            //
        }

        @Override
        public void worked(int work)
        {
            //
        }
    }
}