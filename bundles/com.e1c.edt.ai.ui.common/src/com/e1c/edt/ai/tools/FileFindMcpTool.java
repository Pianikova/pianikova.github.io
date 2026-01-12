/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.MatchEvent;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.FileFindRequest;
import com.e1c.edt.ai.assistent.model.FoundElement;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;


public class FileFindMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_file_find"; //$NON-NLS-1$

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
        + "    \"relative_file_path\": \"core-api/src/services/TestUserService.bsl\",\n"
        + "    \"filePath\": \"/projects/core-api/src/services/TestUserService.bsl\",\n"
        + "    \"absolute_file_path\": \"/home/user/workspace/projects/core-api/src/services/TestUserService.bsl\",\n"
        + "    \"offset\": 243,\n"
        + "    \"length\": 16,\n"
        + "    \"line_offset\": 15,\n"
        + "    \"line_length\": 16,\n"
        + "    \"line_number\": 12,\n"
        + "    \"line_contents\": \"function TestUserService()\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"relative_file_path\": \"backend/modules/TestPaymentService.mdo\",\n"
        + "    \"filePath\": \"/projects/backend/modules/TestPaymentService.mdo\",\n"
        + "    \"absolute_file_path\": \"/home/user/workspace/projects/backend/modules/TestPaymentService.mdo\",\n"
        + "    \"offset\": 187,\n"
        + "    \"length\": 19,\n"
        + "    \"line_offset\": 8,\n"
        + "    \"line_length\": 19,\n"
        + "    \"line_number\": 7,\n"
        + "    \"line_contents\": \"  var TestPaymentService =\"\n"
        + "  }"
        + "]";

    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IProgressMonitor monitor;

    @Inject
    public FileFindMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IProgressMonitor monitor)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(monitor);
        this.json = json;
        this.messageFactory = messageFactory;
        this.monitor = monitor;
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
        var optionalCallArgs = json.deserialize(call.function.arguments, FileFindRequest.class);
        if (optionalCallArgs.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var callArgs = optionalCallArgs.get();
        if (callArgs.searchQuery == null || callArgs.searchQuery.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "'search_query' cannot be empty."));
        }

        var searchQuery = callArgs.searchQuery;
        var isCaseSensitiveSearch = callArgs.isCaseSensitiveSearch != null ? callArgs.isCaseSensitiveSearch : false;
        var isReqularExpressionSearch =
            callArgs.isReqularExpressionSearch != null ? callArgs.isReqularExpressionSearch : false;
        var fileNamePatterns = callArgs.fileNamePatterns != null && !callArgs.fileNamePatterns.isEmpty()
            ? callArgs.fileNamePatterns.toArray(new String[0]) : null;
        var includeDerived = callArgs.includeDerived != null ? callArgs.includeDerived : true;

        // Return a CompletableFuture that will be completed asynchronously
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the heavy operation
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var roots = new ArrayList<IResource>();
            var root = ResourcesPlugin.getWorkspace().getRoot();
            if (callArgs.projectNames != null)
            {
                for (var projectName : callArgs.projectNames)
                {
                    var project = root.getProject(projectName);
                    if (project == null)
                    {
                        return messageFactory.createError(this, call,
                            "Cannot get the project \"" + projectName + "\".");
                    }

                    if (!project.exists())
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

                    if (!project.isOpen())
                    {
                        return messageFactory.createError(this, call,
                            "Cannot open the project \"" + projectName + "\". ");
                    }

                    roots.add(project);
                }
            }

            if (roots.isEmpty())
            {
                roots.add(root);
            }

            var scope =
                FileTextSearchScope.newSearchScope(roots.toArray(new IResource[0]), fileNamePatterns, includeDerived);

            ISearchQuery query;
            try
            {
                query = TextSearchQueryProvider.getPreferred().createQuery(new TextSearchInput()
                {
                    @Override
                    public String getSearchText()
                    {
                        return searchQuery;
                    }

                    @Override
                    public boolean isRegExSearch()
                    {
                        return isReqularExpressionSearch;
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

            var result = query.getSearchResult();
            var elements = new ArrayList<>();
            result.addListener(new ISearchResultListener()
            {

                @SuppressWarnings("restriction")
                @Override
                public void searchResultChanged(SearchResultEvent e)
                {
                    if (e instanceof MatchEvent)
                    {
                        var matchEvent = (MatchEvent)e;
                        var matches = matchEvent.getMatches();
                        if (matches != null)
                        {
                            for (var match : matches)
                            {
                                var element = new FoundElement();
                                element.offset = match.getOffset();
                                element.length = match.getLength();
                                var el = match.getElement();
                                if (el instanceof IFile)
                                {
                                    var file = (IFile)el;
                                    if (file != null)
                                    {
                                        var location = file.getRawLocation();
                                        if (location != null)
                                        {
                                            element.absoluteFilePath = location.toOSString();
                                        }

                                        var relativePath = file.getProjectRelativePath();
                                        if (relativePath != null)
                                        {
                                            element.relativeFilePath = "/" + relativePath.toPortableString();
                                            element.filePath = "/" + file.getProject().getName() + "/"
                                                + relativePath.toPortableString();
                                        }

                                    }
                                }

                                if (match instanceof FileMatch)
                                {
                                    var fileMatch = (FileMatch)match;
                                    var line = fileMatch.getLineElement();
                                    if (line != null)
                                    {
                                        element.lineOffset = line.getOffset();
                                        element.lineLength = line.getLength();
                                        element.lineNumber = line.getLine();
                                        element.lineContents = line.getContents();
                                    }
                                }

                                elements.add(element);
                            }
                        }
                    }
                }
            });

            query.run(monitor);
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

        description.append("Finds files in IDE.");
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
        searchQueryProp.description = "Text or regular expression to search . The search text represents a regular expression or a pattern using '*' and '?' as wildcards. The empty search text signals a file name search.";
        properties.put("search_query", searchQueryProp);

        var isCaseSensitiveSearchProp = new McpToolCallProperty();
        isCaseSensitiveSearchProp.type = "boolean";
        isCaseSensitiveSearchProp.description = "Specifies whether the pattern should be case-sensitive. Defaults to false.";
        properties.put("is_case_sensitive_search", isCaseSensitiveSearchProp);

        var isReqularExpressionSearchProp = new McpToolCallProperty();
        isReqularExpressionSearchProp.type = "boolean";
        isReqularExpressionSearchProp.description = "Specifies whether the search text contains a regular expression or not. Defaults to false.";
        properties.put("is_case_sensitive_search", isReqularExpressionSearchProp);

        var projectNamesProp = new McpToolCallProperty();
        projectNamesProp.type = "object";
        projectNamesProp.description = "IDE project names. If not specified, all projects will be searched.";
        properties.put("search_project_names", projectNamesProp);

        var fileNamePatternsProp = new McpToolCallProperty();
        fileNamePatternsProp.type = "object";
        fileNamePatternsProp.description = "Filename patterns that all files must match. If not specified, then all file names must be included.";
        properties.put("file_name_patterns", fileNamePatternsProp);

        var includeDerivedProp = new McpToolCallProperty();
        includeDerivedProp.type = "boolean";
        includeDerivedProp.description = "True means including derived files and files inside derived containers. False means excluding them. The default value is True.";
        properties.put("include_derived", includeDerivedProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("search_query");

        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }
}