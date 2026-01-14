/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.MatchEvent;
import org.eclipse.search.ui.text.TextSearchQueryProvider;

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
import com.google.inject.Provider;

public class FindFileMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_find_file"; //$NON-NLS-1$
    private static final int MAX_RESULTS = 100;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"search_query\": \"Test.*Service\",\n"
        + "  \"is_case_sensitive_search\": true,\n"
        + "  \"is_regular_expression_search\": true,\n"
        + "  \"search_project_names\": [\"core-api\", \"backend\"],\n"
        + "  \"file_name_patterns\": [\"*.bsl\", \"*.mdo\"],\n"
        + "  \"include_derived\": false\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"project_name\": \"core-api\",\n"
        + "    \"relative_file_path\": \"src/services/TestUserService.bsl\",\n"
        + "    \"absolute_file_path\": \"/home/user/workspace/projects/core-api/src/services/TestUserService.bsl\",\n"
        + "    \"offset\": 243,\n"
        + "    \"length\": 16,\n"
        + "    \"line_offset\": 15,\n"
        + "    \"line_length\": 16,\n"
        + "    \"line_number\": 12,\n"
        + "    \"line_contents\": \"function TestUserService()\"\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;

    @Inject
    public FindFileMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        this.json = json;
        this.messageFactory = messageFactory;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
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
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        if (request.searchQuery == null || request.searchQuery.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "'search_query' cannot be empty."));
        }

        var searchQuery = request.searchQuery;
        var isCaseSensitiveSearch = request.isCaseSensitiveSearch != null ? request.isCaseSensitiveSearch : false;
        var isRegularExpressionSearch =
            request.isRegularExpressionSearch != null ? request.isRegularExpressionSearch : false;
        var fileNamePatterns = request.fileNamePatterns != null && !request.fileNamePatterns.isEmpty()
            ? request.fileNamePatterns.toArray(new String[0]) : null;
        var includeDerived = request.includeDerived != null ? request.includeDerived : true;
        List<String> projectNames = request.projectNames != null ? request.projectNames : List.of();

        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            List<IResource> roots = new ArrayList<>();
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var monitor = cancellationProgressMonitor.get();
            monitor.setCancellationToken(cancellationToken);

            if (!projectNames.isEmpty())
            {
                for (var projectName : projectNames)
                {
                    var project = root.getProject(projectName);
                    if (project == null || !project.exists())
                    {
                        return messageFactory.createError(this, call,
                            "The project \"" + projectName + "\" does not exist.");
                    }

                    if (!project.isOpen())
                    {
                        try
                        {
                            project.open(monitor);
                        }
                        catch (CoreException error)
                        {
                            return messageFactory.createError(this, call,
                                "Cannot open the project \"" + projectName + "\". " + error.getMessage());
                        }
                    }
                    roots.add(project);
                }
            }
            else
            {
                roots.add(root);
            }

            var scope =
                FileTextSearchScope.newSearchScope(roots.toArray(new IResource[0]), fileNamePatterns, includeDerived);

            ISearchQuery query;
            try
            {
                query = TextSearchQueryProvider.getPreferred().createQuery(new TextSearchQueryProvider.TextSearchInput()
                {
                    @Override
                    public String getSearchText()
                    {
                        return searchQuery;
                    }

                    @Override
                    public boolean isRegExSearch()
                    {
                        return isRegularExpressionSearch;
                    }

                    @Override
                    public boolean isCaseSensitiveSearch()
                    {
                        return isCaseSensitiveSearch;
                    }

                    @Override
                    public FileTextSearchScope getScope()
                    {
                        return scope;
                    }
                    }
                );
            }
            catch (CoreException error)
            {
                return messageFactory.createError(this, call, "Cannot create search query. " + error.getMessage());
            }

            final List<Element> elements = new ArrayList<>();
            final Object lock = new Object();

            ISearchResultListener listener = new ISearchResultListener()
            {
                @SuppressWarnings("restriction")
                @Override
                public void searchResultChanged(SearchResultEvent e)
                {
                    if (e instanceof MatchEvent)
                    {
                        MatchEvent matchEvent = (MatchEvent)e;
                        synchronized (lock)
                        {
                            for (Match match : matchEvent.getMatches())
                            {
                                if (elements.size() >= MAX_RESULTS)
                                    return;

                                if (match instanceof FileMatch)
                                {
                                    FileMatch fileMatch = (FileMatch)match;
                                    var file = fileMatch.getFile();

                                    var element = new Element();
                                    element.offset = fileMatch.getOffset();
                                    element.length = fileMatch.getLength();

                                    if (file != null)
                                    {
                                        element.projectName = file.getProject().getName();
                                        var location = file.getRawLocation();
                                        if (location != null)
                                        {
                                            element.absoluteFilePath = location.toOSString();
                                        }

                                        var relativePath = file.getProjectRelativePath();
                                        if (relativePath != null)
                                        {
                                            element.relativeFilePath = relativePath.toPortableString();
                                        }

                                        var line = fileMatch.getLineElement();
                                        if (line != null)
                                        {
                                            element.lineOffset = line.getOffset();
                                            element.lineLength = line.getLength();
                                            element.lineNumber = line.getLine();
                                            element.lineContents = line.getContents();
                                        }

                                        elements.add(element);
                                    }
                                }
                            }
                        }
                    }
                }
            };

            query.getSearchResult().addListener(listener);

            try
            {
                query.run(monitor);
            }
            catch (OperationCanceledException e)
            {
                return messageFactory.createError(this, call, "Search was cancelled.");
            }
            catch (Exception e)
            {
                return messageFactory.createError(this, call, "Search failed: " + e.getMessage());
            }
            finally
            {
                query.getSearchResult().removeListener(listener);
            }

            var content = json.serialize(elements);
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
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Finds files in IDE based on content patterns.");
        description.append("\nFor example:"); // Исправлено: exapmple -> example
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var searchQueryProp = new McpToolCallProperty();
        searchQueryProp.type = "string";
        searchQueryProp.description = "Text or regular expression to search. Supports wildcards (*, ?) when not using regex.";
        properties.put("search_query", searchQueryProp);

        var isCaseSensitiveSearchProp = new McpToolCallProperty();
        isCaseSensitiveSearchProp.type = "boolean";
        isCaseSensitiveSearchProp.description = "Case-sensitive search. Default: false";
        properties.put("is_case_sensitive_search", isCaseSensitiveSearchProp);

        var isRegularExpressionSearchProp = new McpToolCallProperty();
        isRegularExpressionSearchProp.type = "boolean";
        isRegularExpressionSearchProp.description = "Treat search query as regular expression. Default: false";
        properties.put("is_regular_expression_search", isRegularExpressionSearchProp);

        var projectNamesProp = new McpToolCallProperty();
        projectNamesProp.type = "array";
        projectNamesProp.description = "Project names to search in. Searches all projects if empty.";
        properties.put("search_project_names", projectNamesProp);

        var fileNamePatternsProp = new McpToolCallProperty();
        fileNamePatternsProp.type = "array";
        fileNamePatternsProp.description = "File name patterns (e.g., [\"*.bsl\", \"*.mdo\"])";
        properties.put("file_name_patterns", fileNamePatternsProp);

        var includeDerivedProp = new McpToolCallProperty();
        includeDerivedProp.type = "boolean";
        includeDerivedProp.description = "Include derived resources. Default: true";
        properties.put("include_derived", includeDerivedProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("search_query");

        spec.function.parameters = parameters;
        return spec;
        // @formatter:on
    }

    private static class Request
    {
        /**
         * Text or regular expression to search . The search text represents a regular expression or a pattern using '*' and '?' as wildcards. The empty search text signals a file name search.
         */
        @SerializedName("search_query")
        public String searchQuery;

        /**
         * Specifies whether the pattern should be case-sensitive. Defaults to false.
         */
        @SerializedName("is_case_sensitive_search")
        public Boolean isCaseSensitiveSearch;

        /**
         * Specifies whether the search text contains a regular expression or not. Defaults to false.
         */
        @SerializedName("is_regular_expression_search")
        public Boolean isRegularExpressionSearch;

        /**
         * IDE project names. If not specified, all projects will be searched.
         */
        @SerializedName("search_project_names")
        public List<String> projectNames;

        /**
         * Filename patterns that all files must match. If not specified, then all file names must be included.
         */
        @SerializedName("file_name_patterns")
        public List<String> fileNamePatterns;

        /**
         * True means including derived files and files inside derived containers. False means excluding them. The default value is True.
         */
        @SerializedName("include_derived")
        public Boolean includeDerived;
    }

    private static class Element
    {
        /**
         * Name of the project
         */
        @SerializedName("project_name")
        public String projectName;

        /**
         * Project relative path to the file.
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        @SerializedName("absolute_file_path")
        public String absoluteFilePath;

        public int offset;

        public int length;

        @SerializedName("line_offset")
        public int lineOffset;

        @SerializedName("line_length")
        public int lineLength;

        @SerializedName("line_number")
        public int lineNumber;

        @SerializedName("line_contents")
        public String lineContents;
    }

}