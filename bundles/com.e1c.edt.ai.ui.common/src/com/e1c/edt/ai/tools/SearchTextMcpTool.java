/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.quicksearch.internal.core.LineItem;
import org.eclipse.text.quicksearch.internal.core.QuickTextQuery;
import org.eclipse.text.quicksearch.internal.core.QuickTextSearchRequestor;
import org.eclipse.text.quicksearch.internal.core.QuickTextSearcher;
import org.eclipse.text.quicksearch.internal.core.pathmatch.ResourceMatcher;
import org.eclipse.text.quicksearch.internal.core.priority.PriorityFunction;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
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
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

@SuppressWarnings({ "nls", "restriction" })
public class SearchTextMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "SearchText";
    private static final int DEFAULT_MAX_ELEMENTS = McpToolConstants.DEFAULT_MAX_SEARCH_ELEMENTS;
    private static final int MAX_LINE_LEN = 3000;
    private static final int MAX_RESULTS = 3000;

    // @formatter:off
    private static String QuestionExample =
        "{\n"
        + "  \"search_query\": \"Test*Service\",\n"
        + "  \"file_path_patterns\": [\"src/**/*.bsl\", \"*.mdo\", \"**/config/Configuration.xml\"],\n"
        + "  \"first_index\": 0,\n"
        + "  \"max_count\": 64\n"
        + "}";
    private static String AnswerExample =
        "{\n"
        + "  \"results\": [\n"
        + "    {\n"
        + "      \"project_name\": \"core-api\",\n"
        + "      \"path\": \"/home/user/workspace/projects/core-api/src/services/TestUserService.bsl\",\n"
        + "      \"offset\": 243,\n"
        + "      \"length\": 16,\n"
        + "      \"line_offset\": 15,\n"
        + "      \"line_length\": 16,\n"
        + "      \"line_number\": 12,\n"
        + "      \"line_content\": \"function TestUserService()\"\n"
        + "    }\n"
        + "  ],\n"
        + "  \"total_results\": 245\n"
        + "}";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IMarkdownUtils markdownUtils;
    private final IProjectTools projectTools;
    private final IFileSystem fileSystem;
    private final IPatternMatcher patternMatcher;

    @Inject
    public SearchTextMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils,
        IProjectTools projectTools, IFileSystem fileSystem, IPatternMatcher patternMatcher)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(patternMatcher);
        this.json = json;
        this.messageFactory = messageFactory;
        this.markdownUtils = markdownUtils;
        this.projectTools = projectTools;
        this.fileSystem = fileSystem;
        this.patternMatcher = patternMatcher;
        spec = createSpecification();
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return spec;
    }

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
                    + "\nOptional fields: 'file_path_patterns' (array), 'first_index' (integer), 'max_count' (integer)");
        }

        var request = optionalRequest.get();
        if (request.searchQuery == null || request.searchQuery.isBlank())
        {
            throw new ToolException("Field 'search_query' is required and cannot be empty.");
        }

        var searchQuery = request.searchQuery;
        var filePathPatterns = request.filePathPatterns;
        var path = request.path;
        int firstIndex = request.firstIndex != null ? Math.max(0, request.firstIndex) : 0;
        int maxCount = request.maxCount != null && request.maxCount > 0 ? request.maxCount : DEFAULT_MAX_ELEMENTS;

        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.SearchTitleTemplate, searchQuery));

            if (filePathPatterns != null && !filePathPatterns.isEmpty())
            {
                requestMarkdown.append("\n\n")
                    .append(Messages.FileNamePatterns)
                    .append(": ")
                    .append(formatFilePathPatterns(filePathPatterns));
            }

            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            final int maxTotalElements = Math.min(firstIndex + maxCount, MAX_RESULTS);

            if (path != null && !path.isBlank())
            {
                var determinedProject = projectTools.determineProjectName(path);
                if (determinedProject == null || determinedProject.isBlank())
                {
                    var allElements =
                        searchFileSystem(path, searchQuery, filePathPatterns, maxTotalElements, cancellationToken);
                    return buildResponse(call, details, searchQuery, filePathPatterns, allElements, firstIndex,
                        maxCount);
                }
            }

            final List<Element> allElements = new ArrayList<>();
            final ReadWriteLock lock = new ReentrantReadWriteLock();
            var query = new QuickTextQuery(searchQuery, false);

            var searcher = new QuickTextSearcher(query,
                new PriorityFunction()
                {
                    @Override
                    public double priority(IResource resource)
                    {
                        return 0.0;
                    }
                },
                MAX_LINE_LEN,
                new QuickTextSearchRequestor()
                {
                    @Override
                    public void add(LineItem match)
                    {
                        lock.writeLock().lock();
                        try
                        {
                            if (!cancellationToken.isCanceled() && allElements.size() < maxTotalElements)
                            {
                                var element = createElement(match);
                                if (element != null)
                                {
                                    allElements.add(element);
                                }
                            }
                        }
                        finally
                        {
                            lock.writeLock().unlock();
                        }
                    }

                    @Override
                    public void clear()
                    {
                        lock.writeLock().lock();
                        try
                        {
                            allElements.clear();
                        }
                        finally
                        {
                            lock.writeLock().unlock();
                        }
                    }

                    @Override
                    public void revoke(LineItem match)
                    {
                        String path = match.getFile().getFullPath().toString();
                        int lineNumber = match.getLineNumber();
                        int offset = match.getOffset();

                        lock.writeLock().lock();
                        try
                        {
                            var iterator = allElements.iterator();
                            while (iterator.hasNext())
                            {
                                var element = iterator.next();
                                if (isSameElement(element, path, lineNumber, offset))
                                {
                                    iterator.remove();
                                }
                            }
                        }
                        finally
                        {
                            lock.writeLock().unlock();
                        }
                    }

                    @Override
                    public void update(LineItem match)
                    {
                        String path = match.getFile().getFullPath().toString();
                        int lineNumber = match.getLineNumber();
                        int offset = match.getOffset();

                        lock.writeLock().lock();
                        try
                        {
                            for (int i = 0; i < allElements.size(); i++)
                            {
                                var element = allElements.get(i);
                                if (isSameElement(element, path, lineNumber, offset))
                                {
                                    var updatedElement = createElement(match);
                                    if (updatedElement != null)
                                    {
                                        allElements.set(i, updatedElement);
                                    }
                                    return;
                                }
                            }
                        }
                        finally
                        {
                            lock.writeLock().unlock();
                        }
                    }
                });

            if (filePathPatterns != null && !filePathPatterns.isEmpty())
            {
                searcher.setPathMatcher(createResourceMatcher(filePathPatterns));
            }

            try
            {
                while (searcher.isActive() && !cancellationToken.isCanceled())
                {
                    Thread.sleep(50);
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            finally
            {
                searcher.cancel();
            }

            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Search was cancelled");
            }

            List<Element> allElementsSnapshot;
            lock.readLock().lock();
            try
            {
                allElementsSnapshot = new ArrayList<>(allElements);
            }
            finally
            {
                lock.readLock().unlock();
            }

            return buildResponse(call, details, searchQuery, filePathPatterns, allElementsSnapshot, firstIndex,
                maxCount);
        });
    }

    @SuppressWarnings("nls")
    private ToolCallMessage buildResponse(McpToolCall call, ToolCallMessageDetails details, String searchQuery,
        List<String> filePathPatterns, List<Element> allElements, int firstIndex, int maxCount)
    {
        // Check pagination limit
        if (firstIndex >= MAX_RESULTS)
        {
            throw new ToolException("Parameter 'first_index' cannot be greater than or equal to " + MAX_RESULTS
                + ". Maximum pagination depth is " + MAX_RESULTS + " results.");
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
            elements = new ArrayList<>(allElements.subList(firstIndex, endIndex));
        }
        int totalResults = allElements.size();

        // Create SearchTextResponse with paginated results and total count
        SearchTextResponse response = new SearchTextResponse();
        response.results = elements;
        response.totalResults = totalResults;

        var content = json.serialize(response);

        var responseMarkdown = new StringBuilder();
        String resultCountText;
        if (response.totalResults > maxCount || firstIndex > 0)
        {
            resultCountText = response.totalResults + "/" + elements.size();
        }
        else
        {
            resultCountText = String.valueOf(elements.size());
        }

        responseMarkdown.append(MessageFormat.format(Messages.FindTemplate,
            markdownUtils.createStyledText(resultCountText, TextColor.GREEN, FontWeight.BOLD, false)))
            .append("\n\n")
            .append(Messages.SearchQuery)
            .append(": `")
            .append(searchQuery)
            .append("`");

        if (filePathPatterns != null && !filePathPatterns.isEmpty())
        {
            responseMarkdown.append("\n\n")
                .append(Messages.FileNamePatterns)
                .append(": ")
                .append(formatFilePathPatterns(filePathPatterns));
        }

        if (!elements.isEmpty())
        {
            responseMarkdown.append("\n\n**")
                .append(Messages.SearchResults)
                .append("**\n\n");

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
                    String formattedPath = markdownUtils.formatFilePath(element.path, element.lineNumber, 0);

                    responseMarkdown.append("- **").append(formattedPath).append("**");
                    responseMarkdown.append(" - ").append(Messages.Line).append(" ").append(element.lineNumber);
                    responseMarkdown.append("\n");
                }

                responseMarkdown.append("\n");
            }
        }

        details.responseMarkdown = responseMarkdown.toString();
        details.hideAfter = elements.size() == 0;

        return messageFactory.createMessage(this, call, content, details);
    }

    private ResourceMatcher createResourceMatcher(List<String> filePathPatterns)
    {
        return resource -> {
            var resourcePath = resource.getFullPath().toString();
            for (var pattern : filePathPatterns)
            {
                if (patternMatcher.matches(resourcePath, pattern))
                {
                    return true;
                }
            }
            return false;
        };
    }

    @SuppressWarnings("nls")
    private List<Element> searchFileSystem(String path, String searchQuery, List<String> filePathPatterns,
        int maxTotalElements, ICancellationToken cancellationToken)
    {
        var baseFile = new File(path);
        if (!baseFile.exists())
        {
            throw new ToolException("The path \"" + path + "\" does not exist.");
        }

        var query = new QuickTextQuery(searchQuery, false);
        var allElements = new ArrayList<Element>();

        try
        {
            if (baseFile.isDirectory())
            {
                var basePath = baseFile.toPath();
                try (Stream<Path> stream = Files.walk(basePath))
                {
                    var files = stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString))
                        .collect(Collectors.toList());
                    for (var file : files)
                    {
                        if (cancellationToken.isCanceled() || allElements.size() >= maxTotalElements)
                        {
                            break;
                        }

                        var relativePath = basePath.relativize(file).toString().replace("\\", "/");
                        if (filePathPatterns != null && !filePathPatterns.isEmpty()
                            && filePathPatterns.stream().noneMatch(pattern -> patternMatcher.matches(relativePath, pattern)))
                        {
                            continue;
                        }

                        searchInFile(file.toFile(), query, allElements, maxTotalElements, cancellationToken);
                    }
                }
            }
            else
            {
                searchInFile(baseFile, query, allElements, maxTotalElements, cancellationToken);
            }
        }
        catch (IOException e)
        {
            throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
        }

        return allElements;
    }

    private void searchInFile(File file, QuickTextQuery query, List<Element> allElements, int maxTotalElements,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled() || allElements.size() >= maxTotalElements)
        {
            return;
        }

        try (var fileInputStream = new FileInputStream(file);
            var inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            var reader = new BufferedReader(inputStreamReader))
        {
            var lineNumber = 0;
            for (var line : fileSystem.getLines(reader))
            {
                lineNumber++;
                if (cancellationToken.isCanceled() || allElements.size() >= maxTotalElements)
                {
                    break;
                }

                if (!query.matchItem(line))
                {
                    continue;
                }

                var element = new Element();
                element.projectName = Messages.OutsideProject;
                element.path = file.getAbsolutePath();
                element.lineNumber = lineNumber;
                element.lineContent = line;

                IRegion range = query.findFirst(line);
                if (range != null)
                {
                    element.offset = range.getOffset();
                    element.length = range.getLength();
                    element.lineOffset = range.getOffset();
                    element.lineLength = range.getLength();
                }

                allElements.add(element);
            }
        }
        catch (IOException e)
        {
            // Skip files that cannot be read (binary content, permission issues, etc.).
        }
    }

    private Element createElement(LineItem match)
    {
        var file = match.getFile();
        if (file == null)
        {
            return null;
        }

        var element = new Element();
        element.projectName = file.getProject().getName();
        element.path = file.getFullPath().toString();
        element.offset = match.getOffset();
        element.length = match.getText() != null ? match.getText().length() : 0;
        element.lineOffset = match.getOffset();
        element.lineLength = match.getText() != null ? match.getText().length() : 0;
        element.lineNumber = match.getLineNumber();
        element.lineContent = match.getText();

        return element;
    }

    private boolean isSameElement(Element element, String path, int lineNumber, int offset)
    {
        return path.equals(element.path) && lineNumber == element.lineNumber && offset == element.offset;
    }

    private McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Searches for text in files using the quick search API.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Provide a search pattern in `search_query`. Wildcards only (\"*\", \"?\"); regular expressions are not supported (e.g. \".*\" is matched literally, not as \"any characters\"). Matching is case-sensitive, including Cyrillic (\"НДС\" does not match \"ндс\").");
        description.append("\n- Optionally use `file_path_patterns` to filter by file types (e.g., [\"*.bsl\", \"*.mdo\"] or directory patterns like \"src/**/*.bsl\"). Same glob syntax as `" + GlobMcpTool.TOOL_NAME + "`: a pattern without \"/\" matches the name at any depth; a pattern with \"/\" is anchored to the root and needs \"**\" to span multiple directories (e.g. \"config/Configuration.xml\" only matches at the root, use \"**/config/Configuration.xml\" to match at any depth). Common 1C filters: [\"**/CommonModules/**/*.bsl\"], [\"**/Documents/**/*.bsl\"], [\"**/AccumulationRegisters/**/*.bsl\"], or by extension: [\"*.bsl\"], [\"*.mdo\"], [\"*.form\"].");
        description.append("\n- Use `first_index` and `max_count` for pagination. Response includes `total_results` for all matches.");
        description.append("\n- Searches all open projects by default.");
        description.append("\n- Optionally set `path` to an absolute directory or file path. If the path belongs to an open project, project-wide search is used as usual. If the path is outside any open project, the file system is searched directly (useful for files/folders not part of the IDE workspace).");
        description.append("\n\nRelated tools:");
        description.append("\n- Open/edit results: `").append(ReadMcpTool.TOOL_NAME).append("`, `").append(EditMcpTool.TOOL_NAME).append("`.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var searchQueryProp = new McpToolCallProperty();
        searchQueryProp.type = "string";
        searchQueryProp.description = "Text pattern to search for. Supports wildcard patterns (*, ?) only, not regular expressions. Matching is case-sensitive.";
        properties.put("search_query", searchQueryProp);

        var filePathPatternsProp = new McpToolCallProperty();
        filePathPatternsProp.type = "array";
        filePathPatternsProp.description = "File path patterns (e.g., [\"*.bsl\", \"*.mdo\", \"src/**/*.bsl\"]). A pattern without \"/\" matches at any depth; a pattern with \"/\" is anchored to the root unless it contains \"**\". If not specified, searches all files.";
        properties.put("file_path_patterns", filePathPatternsProp);

        var pathProp = new McpToolCallProperty();
        pathProp.type = "string";
        pathProp.description = "Absolute directory or file path to search. If omitted, searches all open projects. If the path is outside any open project, searches the file system directly.";
        properties.put("path", pathProp);

        var firstIndexProp = new McpToolCallProperty();
        firstIndexProp.type = "integer";
        firstIndexProp.description = "Index of first element to return (0-based). Use for pagination with max_count. Response includes total_results for all matches. Default: 0";
        properties.put("first_index", firstIndexProp);

        var maxCountProp = new McpToolCallProperty();
        maxCountProp.type = "integer";
        maxCountProp.description = "Maximum number of elements to return. Use for pagination with first_index. Response includes total_results for all matches. Default: 64";
        properties.put("max_count", maxCountProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("search_query");

        spec.function.parameters = parameters;
        return spec;
    }

    private static class Request
    {
        @SerializedName("search_query")
        public String searchQuery;

        @SerializedName("file_path_patterns")
        public List<String> filePathPatterns;

        @SerializedName("path")
        public String path;

        @SerializedName("first_index")
        public Integer firstIndex = 0;

        @SerializedName("max_count")
        public Integer maxCount = DEFAULT_MAX_ELEMENTS;
    }

    @SuppressWarnings("unused")
    private static class Element
    {
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

        @SerializedName("line_content")
        public String lineContent;
    }

    private static class SearchTextResponse
    {
        @SerializedName("total_results")
        public int totalResults;

        // Large field last so it is dropped first if the response is truncated.
        @SerializedName("results")
        public List<Element> results;
    }

    private String formatFilePathPatterns(List<String> patterns)
    {
        if (patterns == null || patterns.isEmpty())
        {
            return Messages.AllFiles;
        }

        if (patterns.size() == 1)
        {
            return "`" + patterns.get(0) + "`";
        }

        var result = new StringBuilder();
        for (int i = 0; i < patterns.size(); i++)
        {
            if (i > 0)
            {
                result.append(", ");
            }
            result.append("`").append(patterns.get(i)).append("`");
        }
        return result.toString();
    }

}
