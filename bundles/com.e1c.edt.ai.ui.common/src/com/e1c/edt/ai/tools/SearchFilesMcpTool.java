/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
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
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SearchFilesMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "SearchFiles"; //$NON-NLS-1$
    private static final int DEFAULT_MAX_FILES = McpToolConstants.DEFAULT_MAX_FILES;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"search_pattern\": \"*.bsl\",\n"
        + "  \"include_subfolders\": true,\n"
        + "  \"max_results\": 20\n"
        + "}\n\n"
        + "// Search in all projects:\n"
        + "{\n"
        + "  \"search_pattern\": \"*.xml\",\n"
        + "  \"max_results\": 50\n"
        + "}\n\n"
        + "// Search with path (determines project automatically):\n"
        + "{\n"
        + "  \"path\": \"C:/Projects/MyProject/src\",\n"
        + "  \"search_pattern\": \"*.bsl\",\n"
        + "  \"max_results\": 20\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"project_name\": \"MyProject\",\n"
        + "    \"path\": \"C:/Projects/MyProject/src/CommonModules/MainModule/Module.bsl\"\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IMarkdownUtils markdownUtils;
    private final IProjectTools projectTools;

    @Inject
    public SearchFilesMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils,
        IProjectTools projectTools)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(projectTools);
        this.json = json;
        this.messageFactory = messageFactory;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.markdownUtils = markdownUtils;
        this.projectTools = projectTools;
        spec = createSpecification();
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
        var path = request.path;
        var searchPattern = request.searchPattern != null ? request.searchPattern : "*";
        var includeSubfolders = request.includeSubfolders != null ? request.includeSubfolders : true;
        var maxResults = request.maxResults != null && request.maxResults > 0 ? request.maxResults : DEFAULT_MAX_FILES;

        if (call.callKind == ToolCallKind.RENDER)
        {
            var pattern = request.searchPattern != null ? request.searchPattern : "*";

            // Create detailed request markdown with search parameters
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.FindFilesTitleTemplate, pattern))
                .append("\n\n") //$NON-NLS-1$
                .append(Messages.SearchQuery)
                .append(": ") //$NON-NLS-1$
                .append("`") //$NON-NLS-1$
                .append(markdownUtils.escapeForMarkdown(pattern))
                .append("`"); //$NON-NLS-1$

            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var foundFiles = new ArrayList<FileInfo>();

            try
            {
                var monitor = cancellationProgressMonitor.get();
                monitor.setCancellationToken(cancellationToken);

                // If path is provided, try to determine project from it
                if (path != null && !path.isBlank())
                {
                    var determinedProject = projectTools.determineProjectName(path);

                    if (determinedProject != null)
                    {
                        // Path is within a project, search using project API
                        var project = root.getProject(determinedProject);

                        if (project == null || !project.exists())
                        {
                            throw new ToolException("The project \"" + determinedProject + "\" does not exist.");
                        }
                        if (!project.isOpen())
                        {
                            try
                            {
                                project.open(monitor);
                            }
                            catch (CoreException error)
                            {
                                throw new ToolException("Cannot open the project \"" + determinedProject + "\"", error,
                                    ToolErrorType.RETRYABLE);
                            }
                        }

                        // Get the container resource from the path to search from that specific location
                        var container = root.getContainerForLocation(new Path(path));
                        if (container != null)
                        {
                            // Search for files matching the pattern starting from the specific path
                            searchFiles(container, searchPattern, includeSubfolders, foundFiles, maxResults, monitor,
                                cancellationToken);
                        }
                        else
                        {
                            throw new ToolException("The directory \"" + path + "\" does not exist.");
                        }
                    }
                    else
                    {
                        // Path is not within any project, use IO API to search
                        searchFilesViaIO(path, searchPattern, includeSubfolders, foundFiles, maxResults,
                            cancellationToken);
                    }
                }
                else
                {
                    // Search in all projects
                    var projects = root.getProjects();
                    for (var project : projects)
                    {
                        if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults)
                        {
                            break;
                        }

                        if (!project.isOpen())
                        {
                            try
                            {
                                project.open(monitor);
                            }
                            catch (CoreException error)
                            {
                                // Skip this project and continue with others
                                continue;
                            }
                        }

                        searchFiles(project, searchPattern, includeSubfolders, foundFiles, maxResults, monitor,
                            cancellationToken);
                    }
            }
            }
            catch (CoreException e)
            {
                throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
            }
            catch (IOException e)
            {
                throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
            }

            // Prepare response
            var content = json.serialize(foundFiles);

            // Create response markdown
            var responseMarkdown = new StringBuilder();
            responseMarkdown
                .append(MessageFormat.format(Messages.FilesFoundTemplate,
                    markdownUtils.createStyledText(String.valueOf(foundFiles.size()), TextColor.GREEN,
                        FontWeight.BOLD, false)))
                .append("\n\n") //$NON-NLS-1$
                .append(Messages.SearchQuery)
                .append(": ") //$NON-NLS-1$
                .append("`") //$NON-NLS-1$
                .append(searchPattern)
                .append("`"); //$NON-NLS-1$

            responseMarkdown.append("\n\n**").append(Messages.SearchResults).append("**\n\n"); //$NON-NLS-1$ //$NON-NLS-2$

            for (var fileInfo : foundFiles)
            {
                responseMarkdown.append("- **") //$NON-NLS-1$
                    .append(markdownUtils.escapeForMarkdown(fileInfo.path))
                    .append("**\n"); //$NON-NLS-1$

                responseMarkdown.append("\n"); //$NON-NLS-1$
            }

            details.responseMarkdown = responseMarkdown.toString();
            details.hideAfter = foundFiles.size() == 0;
            return messageFactory.createMessage(this, call, content, details);
        });
    }

    private void searchFiles(IResource container, String pattern, boolean includeSubfolders, List<FileInfo> foundFiles,
        int maxResults, ICancellationProgressMonitor monitor, ICancellationToken cancellationToken) throws CoreException
    {
        if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults)
        {
            return;
        }

        try
        {
            // Check for cancellation before each directory
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
                if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults)
                {
                    return;
                }

                if (member.getType() == IResource.FILE)
                {
                    // Check if file matches the pattern
                    if (matchesPattern(member.getName(), pattern))
                    {
                        var fileInfo = new FileInfo();
                        fileInfo.projectName = member.getProject().getName();
                        fileInfo.path = member.getLocation().toOSString();

                        foundFiles.add(fileInfo);
                    }

                }
                else if (includeSubfolders && member.getType() == IResource.FOLDER)
                {
                    // Recursively search subfolders
                    searchFiles(member, pattern, includeSubfolders, foundFiles, maxResults, monitor, cancellationToken);
                }
            }
        }
        catch (CoreException e)
        {
            // Log the error but continue searching other files
            // In a real implementation, you might want to handle this differently
        }
    }

    private boolean matchesPattern(String fileName, String pattern)
    {
        // Simple wildcard pattern matching
        // Supports * (matches any number of characters) and ? (matches exactly one character)

        // Convert pattern to regex
        var regex = pattern.replace(".", "\\.") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("*", ".*") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("?", "."); //$NON-NLS-1$ //$NON-NLS-2$
        return fileName.matches(regex);
    }

    /**
     * Searches for files using Java IO API when path is not within any project.
     *
     * @param basePath the base path to start the search from
     * @param pattern the file pattern to match
     * @param includeSubfolders whether to search recursively
     * @param foundFiles list to store found files
     * @param maxResults maximum number of results
     * @param cancellationToken cancellation token
     * @throws IOException if an I/O error occurs
     */
    private void searchFilesViaIO(String basePath, String pattern, boolean includeSubfolders, List<FileInfo> foundFiles,
        int maxResults, ICancellationToken cancellationToken) throws IOException
    {
        if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults)
        {
            return;
        }

        var baseDir = new File(basePath);

        if (!baseDir.exists() || !baseDir.isDirectory())
        {
            return;
        }

        try (Stream<java.nio.file.Path> stream =
            includeSubfolders ? Files.walk(baseDir.toPath()) : Files.list(baseDir.toPath()))
        {
            stream.filter(path -> !Files.isDirectory(path)).filter(path -> {
                if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults)
                {
                    return false;
                }
                var fileName = path.getFileName().toString();
                return matchesPattern(fileName, pattern);
            }).limit(maxResults - foundFiles.size()).forEach(path -> {
                var fileInfo = new FileInfo();
                fileInfo.projectName = null; // Not in a project
                fileInfo.path = path.toAbsolutePath().toString();
                foundFiles.add(fileInfo);
            });
        }
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
        description.append("Finds files by name pattern in a project or all projects.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Supports wildcards: `*` for any characters, `?` for a single character.");
        description.append("\n- If path is specified, it attempts to determine the project via IFileSystem.");
        description.append("\n  If the path is not within any project, searches using IO API.");
        description.append("\n  If path is not specified, searches in all projects.");
        description.append("\n- Can search recursively or only in the root folder.");
        description.append("\n- Limits results to avoid overload on large projects.");
        description.append("\n\nRelated tools:");
        description.append("\n- Search by content: `" + FindMcpTool.TOOL_NAME + "`.");
        description.append("\n- Open/edit files: `" + ReadMcpTool.TOOL_NAME + "`, `" + EditMcpTool.TOOL_NAME + "`.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var pathProp = new McpToolCallProperty();
        pathProp.type = "string";
        pathProp.description = "Optional absolute path to search in. If specified, attempts to determine the project via IFileSystem. If the path is not within any project, searches using IO API.";
        properties.put("path", pathProp);

        var searchPatternProp = new McpToolCallProperty();
        searchPatternProp.type = "string";
        searchPatternProp.description = "File name search pattern. Supports wildcards (*, ?). Default: \"*\" (all files).";
        properties.put("search_pattern", searchPatternProp);

        var includeSubfoldersProp = new McpToolCallProperty();
        includeSubfoldersProp.type = "boolean";
        includeSubfoldersProp.description = "Include subfolders in search. Default: true";
        properties.put("include_subfolders", includeSubfoldersProp);

        var maxResultsProp = new McpToolCallProperty();
        maxResultsProp.type = "integer";
        maxResultsProp.description = "Maximum number of results to return. Default: " + DEFAULT_MAX_FILES;
        properties.put("max_results", maxResultsProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList();
        spec.function.parameters = parameters;

        return spec;
        // @formatter:on
    }

    private static class Request
    {
        /**
         * Optional absolute path to search in.
         */
        @SerializedName("path")
        public String path;

        /**
         * File name search pattern with wildcards.
         */
        @SerializedName("search_pattern")
        public String searchPattern;

        /**
         * Include subfolders in search.
         */
        @SerializedName("include_subfolders")
        public Boolean includeSubfolders;

        /**
         * Maximum number of results to return.
         */
        @SerializedName("max_results")
        public Integer maxResults;
    }

    private static class FileInfo
    {
        /**
         * Name of the project.
         */
        @SerializedName("project_name")
        public String projectName;

        /**
         * Absolute file system path.
         */
        @SerializedName("path")
        public String path;
    }
}
