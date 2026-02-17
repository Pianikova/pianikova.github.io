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

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
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
import com.google.inject.Provider;

public class FindMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Find"; //$NON-NLS-1$
    private static final int DEFAULT_MAX_ELEMENTS = McpToolConstants.DEFAULT_MAX_SEARCH_ELEMENTS;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"search_query\": \"Test.*Service\",\n"
        + "  \"is_case_sensitive_search\": true,\n"
        + "  \"is_regular_expression_search\": true,\n"
        + "  \"search_project_names\": [\"core-api\", \"backend\"],\n"
        + "  \"file_name_patterns\": [\"*.bsl\", \"*.mdo\"],\n"
        + "  \"include_derived\": false,\n"
        + "  \"first_index\": 0,\n"
        + "  \"max_count\": 64\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"project_name\": \"core-api\",\n"
        + "    \"path\": \"/home/user/workspace/projects/core-api/src/services/TestUserService.bsl\",\n"
        + "    \"offset\": 243,\n"
        + "    \"length\": 16,\n"
        + "    \"line_offset\": 15,\n"
        + "    \"line_length\": 16,\n"
        + "    \"line_number\": 12,\n"
        + "    \"line_content\": \"function TestUserService()\"\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IMarkdownUtils markdownUtils;

    @Inject
    public FindMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(markdownUtils);
        this.json = json;
        this.messageFactory = messageFactory;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
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
            throw new ToolException("Cannot deserialize arguments. JSON format is invalid or missing required fields. "
                    + "Use this example: " + QuestionExample
                    + "\n\nRequired field: 'search_query' (string)"
                    + "\nOptional fields: 'is_case_sensitive_search' (boolean), 'is_regular_expression_search' (boolean), "
                        + "'search_project_names' (array), 'file_name_patterns' (array), 'include_derived' (boolean), "
                        + "'first_index' (integer), 'max_count' (integer)");
        }

        var request = optionalRequest.get();
        if (request.searchQuery == null || request.searchQuery.isBlank())
        {
            request.searchQuery = "*";
        }

        var searchQuery = request.searchQuery;
        // Check if search_query contains wildcard patterns for file name search
        boolean isFileNameSearch = isWildcardPattern(searchQuery)
            && (request.isRegularExpressionSearch == null || !request.isRegularExpressionSearch);

        var isCaseSensitiveSearch = request.isCaseSensitiveSearch != null ? request.isCaseSensitiveSearch : false;
        var isRegularExpressionSearch =
            request.isRegularExpressionSearch != null ? request.isRegularExpressionSearch : false;
        var fileNamePatterns = request.fileNamePatterns != null && !request.fileNamePatterns.isEmpty()
            ? request.fileNamePatterns.toArray(new String[0]) : null;
        var includeDerived = request.includeDerived != null ? request.includeDerived : true;
        var includeSubfolders = request.includeSubfolders != null ? request.includeSubfolders : true;
        List<String> projectNames = request.projectNames != null ? request.projectNames : List.of();
        int firstIndex = request.firstIndex != null ? Math.max(0, request.firstIndex) : 0;
        int maxCount = request.maxCount != null && request.maxCount > 0 ? request.maxCount : DEFAULT_MAX_ELEMENTS;

        if (call.callKind == ToolCallKind.RENDER)
        {
            // Create detailed request markdown with search parameters
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.SearchTitleTemplate, searchQuery));

            // Add file patterns only for content search (not for file name search)
            if (!isFileNameSearch)
            {
                requestMarkdown.append("\n\n") //$NON-NLS-1$
                    .append(Messages.FileNamePatterns)
                    .append(": ") //$NON-NLS-1$
                    .append(formatFileNamePatterns(fileNamePatterns));
            }

            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        // For file name search, project_name is required
        if (isFileNameSearch && projectNames.isEmpty())
        {
            throw new ToolException(
                "For file name search (when search_query contains wildcards), `search_project_names` is required.");
        }

        // If file name search, perform file search instead of content search
        if (isFileNameSearch)
        {
            return performFileSearch(call, cancellationToken, details, projectNames, searchQuery, includeSubfolders,
                maxCount);
        }

        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
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
                        throw new ToolException("The project \"" + projectName + "\" does not exist.");
                    }

                    if (!project.isOpen())
                    {
                        try
                        {
                            project.open(monitor);
                        }
                        catch (CoreException error)
                        {
                            throw new ToolException("Cannot open the project \"" + projectName + "\"", error, ToolErrorType.RETRYABLE);
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
                throw new ToolException("Cannot create search query", error, ToolErrorType.RETRYABLE);
            }

            final List<Element> allElements = new ArrayList<>();
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
                                if (allElements.size() >= firstIndex + maxCount)
                                {
                                    return;
                                }

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
                                            element.path = location.toOSString();
                                        }

                                        var line = fileMatch.getLineElement();
                                        if (line != null)
                                        {
                                            element.lineOffset = line.getOffset();
                                            element.lineLength = line.getLength();
                                            element.lineNumber = line.getLine();
                                        }

                                        allElements.add(element);
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
                throw new ToolException("Search was cancelled", e, ToolErrorType.RETRYABLE);
            }
            catch (Exception e)
            {
                throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
            }
            finally
            {
                query.getSearchResult().removeListener(listener);
            }

            // Apply pagination: get sublist based on firstIndex and maxCount
            List<Element> elements;
            if (firstIndex >= allElements.size())
            {
                elements = new ArrayList<>();
            }
            else
            {
                int endIndex = Math.min(firstIndex + maxCount, allElements.size());
                elements = allElements.subList(firstIndex, endIndex);
            }

            var content = json.serialize(elements);

            // Create detailed response markdown with search result information
            var responseMarkdown = new StringBuilder();
            responseMarkdown.append(MessageFormat.format(Messages.FindTemplate,
                markdownUtils.createStyledText(String.valueOf(elements.size()), TextColor.GREEN, FontWeight.BOLD)))
                .append("\n\n") //$NON-NLS-1$
                .append(Messages.SearchQuery)
                .append(": ") //$NON-NLS-1$
                .append("`") //$NON-NLS-1$
                .append(markdownUtils.escapeForMarkdown(searchQuery))
                .append("`") //$NON-NLS-1$
                .append("\n\n") //$NON-NLS-1$
                .append(Messages.FileNamePatterns)
                .append(": ") //$NON-NLS-1$
                .append(formatFileNamePatterns(fileNamePatterns));

            if (elements.size() > 0)
            {
                // Add search results in collapsible section
                responseMarkdown.append("\n\n<details><summary>")
                    .append(Messages.SearchResults)
                    .append("</summary>\n\n");

                // Group results by project for better organization
                var projectGroups = new HashMap<String, List<Element>>();
                for (var element : elements)
                {
                    projectGroups.computeIfAbsent(element.projectName, k -> new ArrayList<>()).add(element);
                }

                for (var entry : projectGroups.entrySet())
                {
                    var projectName = entry.getKey();
                    var projectElements = entry.getValue();

                    responseMarkdown.append("**").append(markdownUtils.escapeForMarkdown(projectName)).append("**");
                    responseMarkdown.append(" (")
                        .append(projectElements.size())
                        .append(" ")
                        .append(Messages.Matches)
                        .append(")\n\n");

                    for (var element : projectElements)
                    {
                        String formattedPath;
                        if (element.lineNumber > 0)
                        {
                            formattedPath = markdownUtils.formatFilePath(element.path, element.lineNumber, 0);
                        }
                        else
                        {
                            formattedPath = markdownUtils.formatFilePath(element.path);
                        }

                        responseMarkdown.append("- **").append(formattedPath).append("**");

                        if (element.lineNumber > 0)
                        {
                            responseMarkdown.append(" - ").append(Messages.Line).append(" ").append(element.lineNumber);
                        }

                        responseMarkdown.append("\n");
                    }

                    responseMarkdown.append("\n");
                }

                responseMarkdown.append("</details>");
            }

            details.responseMarkdown = responseMarkdown.toString();
            details.hideAfter = elements.size() == 0;

            return messageFactory.createMessage(this, call, content, details);
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
        description.append("Finds files by content pattern or file name pattern in the IDE.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Provide a search pattern in `search_query`.");
        description.append("\n- If `search_query` contains wildcard patterns (*, ?), performs file name search.");
        description.append("\n- For file name search, `search_project_names` is required.");
        description.append("\n- For content search, searches all projects if `search_project_names` is not specified.");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Narrow scope with project/file parameters to reduce noise.");
        description.append("\n\nRelated tools:");
        description.append("\n- Open/edit results: `" + ReadMcpTool.TOOL_NAME + "`, `" + EditMcpTool.TOOL_NAME + "`.");
        description.append("\n\nExample (content search):");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var searchQueryProp = new McpToolCallProperty();
        searchQueryProp.type = "string";
        searchQueryProp.description = "Text or regular expression to search. Supports wildcards (*, ?) when not using regex. "
            + "If contains wildcards, performs file name search (requires search_project_names).";
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
        projectNamesProp.description = "Project names to search in. Searches all projects if empty (for content search). "
            + "Required for file name search (when search_query contains wildcards).";
        properties.put("search_project_names", projectNamesProp);

        var fileNamePatternsProp = new McpToolCallProperty();
        fileNamePatternsProp.type = "array";
        fileNamePatternsProp.description = "File name patterns (e.g., [\"*.bsl\", \"*.mdo\"]). Used for content search only.";
        properties.put("file_name_patterns", fileNamePatternsProp);

        var includeDerivedProp = new McpToolCallProperty();
        includeDerivedProp.type = "boolean";
        includeDerivedProp.description = "Include derived resources. Default: true. Used for content search only.";
        properties.put("include_derived", includeDerivedProp);

        var includeSubfoldersProp = new McpToolCallProperty();
        includeSubfoldersProp.type = "boolean";
        includeSubfoldersProp.description = "Include subfolders in search. Default: true. Used for file name search only.";
        properties.put("include_subfolders", includeSubfoldersProp);

        var firstIndexProp = new McpToolCallProperty();
        firstIndexProp.type = "integer";
        firstIndexProp.description = "Index of first element to return (0-based). Default: 0";
        properties.put("first_index", firstIndexProp);

        var maxCountProp = new McpToolCallProperty();
        maxCountProp.type = "integer";
        maxCountProp.description = "Maximum number of elements to return. Default: 64";
        properties.put("max_count", maxCountProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("search_query");

        spec.function.parameters = parameters;
        return spec;
        // @formatter:on
    }

    private static class Request
    {
        /**
         * Text or regular expression to search. The search text represents a regular expression or a pattern using `*` and `?` as wildcards.
         * If the search_query contains wildcard patterns (*, *?, ?**), it triggers file name search instead of content search.
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
         * For file name search (when search_query contains wildcards), this is required.
         */
        @SerializedName("search_project_names")
        public List<String> projectNames;

        /**
         * Filename patterns that all files must match. If not specified, then all file names must be included.
         * This is used for content search only.
         */
        @SerializedName("file_name_patterns")
        public List<String> fileNamePatterns;

        /**
         * True means including derived files and files inside derived containers. False means excluding them. The default value is True.
         * This is used for content search only.
         */
        @SerializedName("include_derived")
        public Boolean includeDerived;

        /**
         * Include subfolders in search. This is used for file name search only. Default: true
         */
        @SerializedName("include_subfolders")
        public Boolean includeSubfolders;

        @SerializedName("first_index")
        public Integer firstIndex = 0;

        @SerializedName("max_count")
        public Integer maxCount = DEFAULT_MAX_ELEMENTS;
    }

    private static class Element
    {
        /**
         * Name of the project
         */
        @SerializedName("project_name")
        public String projectName;

        @SerializedName("path")
        public String path;

        public int offset;

        public int length;

        @SerializedName("line_offset")
        public int lineOffset;

        @SerializedName("line_length")
        public int lineLength;

        @SerializedName("line_number")
        public int lineNumber;
    }

    /**
     * Checks if the given string is a wildcard pattern for file name search.
     * Returns true if the pattern contains wildcard characters like *, *?, or ?**.
     */
    private boolean isWildcardPattern(String pattern)
    {
        if (pattern == null || pattern.isEmpty())
        {
            return false;
        }
        // Check for patterns that indicate file name search: *, *?, ?**
        return pattern.contains("*") || pattern.contains("?");
    }

    /**
     * Performs file name search based on wildcard patterns.
     * This method is called when search_query contains wildcard patterns.
     */
    @SuppressWarnings("nls")
    private CompletableFuture<ToolCallMessage> performFileSearch(McpToolCall call, ICancellationToken cancellationToken,
        ToolCallMessageDetails details, List<String> projectNames, String searchPattern, boolean includeSubfolders,
        int maxCount)
    {
        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            final List<Element> allElements = new ArrayList<>();

            // Check if searchPattern is the default file search pattern
            boolean isDefaultSearch = "*".equals(searchPattern);

            for (var projectName : projectNames)
            {
                if (cancellationToken.isCanceled() || allElements.size() >= maxCount)
                {
                    break;
                }

                var project = root.getProject(projectName);
                if (project == null || !project.exists())
                {
                    throw new ToolException("The project \"" + projectName + "\" does not exist.");
                }

                if (!project.isOpen())
                {
                    try
                    {
                        var monitor = cancellationProgressMonitor.get();
                        monitor.setCancellationToken(cancellationToken);
                        project.open(monitor);
                    }
                    catch (CoreException error)
                    {
                        throw new ToolException("Cannot open the project \"" + projectName + "\"", error, ToolErrorType.RETRYABLE);
                    }
                }

                try
                {
                    var monitor = cancellationProgressMonitor.get();
                    monitor.setCancellationToken(cancellationToken);
                    searchFilesByName(project, searchPattern, includeSubfolders, allElements, maxCount, monitor,
                        cancellationToken);
                }
                catch (CoreException e)
                {
                    throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
                }
            }

            var content = json.serialize(allElements);

            // Create response markdown
            var responseMarkdown = new StringBuilder();
            responseMarkdown.append(MessageFormat.format(Messages.FilesFoundTemplate,
                markdownUtils.createStyledText(String.valueOf(allElements.size()), TextColor.GREEN, FontWeight.BOLD)))
                .append("\n\n") //$NON-NLS-1$
                .append(Messages.SearchQuery)
                .append(": ") //$NON-NLS-1$
                .append("`") //$NON-NLS-1$
                .append(markdownUtils.escapeForMarkdown(isDefaultSearch ? "*" : searchPattern)) //$NON-NLS-1$
                .append("`") //$NON-NLS-1$
                .append("\n\n") //$NON-NLS-1$
                .append(Messages.FileNamePatterns)
                .append(": ") //$NON-NLS-1$
                .append("`") //$NON-NLS-1$
                .append(markdownUtils.escapeForMarkdown(isDefaultSearch ? "*" : searchPattern)) //$NON-NLS-1$
                .append("`"); //$NON-NLS-1$

            // Add search results in collapsible section
            responseMarkdown.append("\n\n<details><summary>").append(Messages.SearchResults).append("</summary>\n\n"); //$NON-NLS-1$ //$NON-NLS-2$

            for (var element : allElements)
            {
                String formattedPath;
                if (element.lineNumber > 0)
                {
                    formattedPath = markdownUtils.formatFilePath(element.path, element.lineNumber, 0);
                }
                else
                {
                    formattedPath = markdownUtils.formatFilePath(element.path);
                }

                responseMarkdown.append("- **") //$NON-NLS-1$
                    .append(formattedPath)
                    .append("**\n"); //$NON-NLS-1$
            }

            responseMarkdown.append("</details>"); //$NON-NLS-1$

            details.responseMarkdown = responseMarkdown.toString();

            return messageFactory.createMessage(this, call, content, details);
        });
    }

    /**
     * Recursively searches for files matching the given pattern.
     */
    private void searchFilesByName(IResource container, String pattern, boolean includeSubfolders,
        List<Element> foundElements, int maxCount, ICancellationProgressMonitor monitor,
        ICancellationToken cancellationToken) throws CoreException
    {
        if (cancellationToken.isCanceled() || foundElements.size() >= maxCount)
        {
            return;
        }

        try
        {
            if (cancellationToken.isCanceled())
            {
                return;
            }

            IResource[] members;
            if (container.getType() == IResource.PROJECT || container.getType() == IResource.FOLDER)
            {
                members = ((org.eclipse.core.resources.IContainer)container).members();
            }
            else
            {
                return;
            }

            for (IResource member : members)
            {
                if (cancellationToken.isCanceled() || foundElements.size() >= maxCount)
                {
                    return;
                }

                if (member.getType() == IResource.FILE)
                {
                    // Check if file matches the pattern
                    if (matchesPattern(member.getName(), pattern))
                    {
                        var element = new Element();
                        element.projectName = member.getProject().getName();
                        element.path = member.getLocation().toOSString();
                        element.offset = 0;
                        element.length = 0;
                        element.lineOffset = 0;
                        element.lineLength = 0;
                        element.lineNumber = 0;
                        foundElements.add(element);
                    }
                }
                else if (includeSubfolders && member.getType() == IResource.FOLDER)
                {
                    // Recursively search subfolders
                    searchFilesByName(member, pattern, includeSubfolders, foundElements, maxCount, monitor,
                        cancellationToken);
                }
            }
        }
        catch (CoreException e)
        {
            // Log the error but continue searching other files
        }
    }

    /**
     * Checks if a file name matches a wildcard pattern.
     * Supports * (matches any number of characters) and ? (matches exactly one character).
     */
    private boolean matchesPattern(String fileName, String pattern)
    {
        // Convert pattern to regex
        var regex = pattern.replace(".", "\\.") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("*", ".*") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("?", "."); //$NON-NLS-1$ //$NON-NLS-2$
        return fileName.matches(regex);
    }

    /**
     * Formats file name patterns array for display.
     * Uses backticks for values and removes brackets for single element.
     */
    private String formatFileNamePatterns(String[] patterns)
    {
        if (patterns == null || patterns.length == 0)
        {
            return Messages.AllFiles;
        }

        if (patterns.length == 1)
        {
            return "`" + patterns[0] + "`"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        var result = new StringBuilder();
        for (int i = 0; i < patterns.length; i++)
        {
            if (i > 0)
            {
                result.append(", "); //$NON-NLS-1$
            }
            result.append("`").append(patterns[i]).append("`"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return result.toString();
    }

}

