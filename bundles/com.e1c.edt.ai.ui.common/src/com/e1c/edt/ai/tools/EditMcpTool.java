/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContentReplacer;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.ReplaceResult;
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
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class EditMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Edit"; //$NON-NLS-1$
    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"path\": \"C:/Projects/AccountingSystem/src/MainModule.bsl\",\n"
        + "  \"old_content\": \"Procedure Test()\\n    Message(\\\"Hello\\\");\\nEndProcedure\",\n"
        + "  \"new_content\": \"Procedure Test()\\n    Message(\\\"Hi\\\");\\nEndProcedure\",\n"
        + "  \"replace_all\": false\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "File updated: \"C:/Projects/AccountingSystem/src/MainModule.bsl\"";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;
    private final IProjectTools projectTools;
    private final IDispatcher dispatcher;
    private final IContentReplacer contentReplacer;
    private final IMarkdownUtils markdownUtils;
    private final IEditingSupport editingSupport;

    @Inject
    public EditMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem,
        IProjectTools projectTools, IDispatcher dispatcher, IContentReplacer contentReplacer, IMarkdownUtils markdownUtils,
        IEditingSupport editingSupport)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(contentReplacer);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(editingSupport);

        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
        this.projectTools = projectTools;
        this.dispatcher = dispatcher;
        this.contentReplacer = contentReplacer;
        this.markdownUtils = markdownUtils;
        this.editingSupport = editingSupport;

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
        details.autoCall = false;

        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. JSON must be a single object with double-quoted keys and strings. "
                + "Use this example: " + QuestionExample
                + "\n\nRequired fields: 'path' (string), "
                + "'old_content' (string), 'new_content' (string)"
                + "\nOptional field: 'replace_all' (boolean)");
        }

        var request = optionalRequest.get();
        var path = request.path;
        if (path == null || path.isBlank())
        {
            throw new ToolException("`path` is required.");
        }

        var oldContent = request.oldContent;
        if (oldContent == null)
        {
            throw new ToolException("`old_content` is required and cannot be null.");
        }

        var newContent = request.newContent;
        if (newContent == null)
        {
            throw new ToolException("`new_content` is required.");
        }

        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();

            requestMarkdown.append(MessageFormat.format(Messages.EditTitleTemplate, markdownUtils.formatFilePath(path)));

            // Add diff details
            if (request.oldContent != null && request.newContent != null)
            {
                requestMarkdown.append("\n\n");
                requestMarkdown.append("<details><summary>")
                    .append(Messages.EditDetailsSummary)
                    .append("</summary>\n\n");
                requestMarkdown.append(
                    markdownUtils.buildGitDiff(path, request.oldContent, request.newContent));
                requestMarkdown.append("\n</details>");
            }

            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var replaceAll = request.replaceAll != null ? request.replaceAll : false;

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            // Determine project name from absolute path
            String detectedProjectName = projectTools.determineProjectName(path);
            final String finalProjectName = detectedProjectName;

            // Check if file is part of a project
            boolean isProjectFile = finalProjectName != null && !finalProjectName.isBlank();

            if (!isProjectFile)
            {
                // File is not part of any project - use Java file I/O
                try
                {
                    if (!fileSystem.fileExists(path))
                    {
                        throw new ToolException("The file \"" + path + "\" does not exist.");
                    }

                    byte[] fileData = fileSystem.readAllBytes(path);
                    var content = new String(fileData, StandardCharsets.UTF_8);

                    // Perform replacement using helper method
                    var replaceResult =
                        contentReplacer.replace(content, oldContent, newContent, System.lineSeparator(), replaceAll);
                    if (!replaceResult.isSuccess())
                    {
                        String errorMessage = getReplacementErrorMessage(replaceAll, replaceResult);
                        throw new ToolException(errorMessage);
                    }

                    var replacementResult = new ReplacementResult();
                    replacementResult.updatedContent = replaceResult.getUpdatedContent();
                    replacementResult.addedLines = replaceResult.getAddedLines();
                    replacementResult.removedLines = replaceResult.getRemovedLines();

                    // Write back
                    var updatedData = replacementResult.updatedContent.getBytes(StandardCharsets.UTF_8);
                    fileSystem.writeAllBytes(path, updatedData);

                    var response = new StringBuilder();
                    response.append("File updated: \"").append(path).append("\".\n");
                    response.append("⚠️ WARNING: File not part of project. Changes to non-project files may have irreversible consequences.\n");

                    // Add diff details to response markdown
                    var responseMarkdown = new StringBuilder();
                    responseMarkdown
                        .append(MessageFormat.format(Messages.EditedTemplate, markdownUtils.formatFilePath(path),
                        createChangesString(replacementResult.addedLines, replacementResult.removedLines)));
                    responseMarkdown.append("\n\n");
                    responseMarkdown.append("<details><summary>")
                        .append(Messages.EditDetailsSummary)
                        .append("</summary>\n\n");
                    responseMarkdown.append(markdownUtils.buildGitDiff(path, oldContent, newContent));
                    responseMarkdown.append("\n</details>");
                    details.responseMarkdown = responseMarkdown.toString();

                    return messageFactory.createMessage(this, call, response.toString(), details);
                }
                catch (IOException error)
                {
                    throw new ToolException("Failed to edit file", error, ToolErrorType.RETRYABLE);
                }
            }

            // File is part of a project - use Eclipse APIs
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(finalProjectName);
            if (project == null || !project.exists())
            {
                throw new ToolException("The project \"" + finalProjectName + "\" does not exist.");
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
                    throw new ToolException("Cannot open the project \"" + finalProjectName + "\"", error, ToolErrorType.RETRYABLE);
                }
            }

            var projectFile = projectTools.getProjectFile(project, path);
            if (!projectFile.isPresent())
            {
                throw new ToolException("The file \"" + path + "\" does not exist within the IDE project context. "
                    + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope. "
                    + "Use the `" + WriteMcpTool.TOOL_NAME + "` tool to create a new file.");
            }

            // Check if the file can be edited using editingSupport
            if (!editingSupport.canEdit(projectFile.orElse(null)))
            {
                throw new ToolException("The file \"" + path
                    + "\" cannot be edited. Editing is not supported for this file type or the file is locked.");
            }

            var actualFile = projectFile.get();
            var optionalDocument = contentSourceProvider.getFileDocument(actualFile);
            if (optionalDocument.isEmpty())
            {
                var filePathForError = actualFile.getProjectRelativePath().toOSString();
                throw new ToolException("The file \"" + filePathForError + "\" does not exist within the IDE project context. "
                    + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope. "
                    + "Use the `" + WriteMcpTool.TOOL_NAME + "` tool to create a new file.");
            }

            var fileDocument = optionalDocument.get();
            var document = fileDocument.getDocument();

            // Read current file content
            var optionalCurrentContent = dispatcher.dispatch(() -> document.get());
            if (optionalCurrentContent.isEmpty())
            {
                throw new ToolException("Cannot read the file \"" + path + "\".");
            }
            var currentContent = optionalCurrentContent.get();

            // Perform replacement using helper method
            var replaceResult =
                contentReplacer.replace(currentContent, oldContent, newContent, System.lineSeparator(), replaceAll);
            if (!replaceResult.isSuccess())
            {
                String errorMessage = getReplacementErrorMessage(replaceAll, replaceResult);
                throw new ToolException(errorMessage);
            }

            var replacementResult = new ReplacementResult();
            replacementResult.updatedContent = replaceResult.getUpdatedContent();
            replacementResult.addedLines = replaceResult.getAddedLines();
            replacementResult.removedLines = replaceResult.getRemovedLines();

            // Write updated content
            var optionalError = dispatcher.dispatch(() ->
            {
                try
                {
                    fileDocument.setContent(replacementResult.updatedContent);
                    fileDocument.save();
                    return null;
                }
                catch (Exception error)
                {
                    return error;
                }
            });

            if (optionalError.isPresent())
            {
                throw new ToolException("Failed to save file", optionalError.get(), ToolErrorType.RETRYABLE);
            }

            var response = new StringBuilder();
            var displayPath = actualFile.getProjectRelativePath().toPortableString();
            response.append("File updated: \"").append(displayPath).append("\".\n");
            response.append(
                "ACTION REQUIRED: verify project errors and warnings. Use `" + GetMarkersMcpTool.TOOL_NAME + "` tool.");

            // Add response markdown with diff details
            var responseMarkdown = new StringBuilder();
            responseMarkdown.append(MessageFormat.format(Messages.EditedTemplate, markdownUtils.formatFilePath(path),
                createChangesString(replacementResult.addedLines, replacementResult.removedLines)));
            responseMarkdown.append("\n\n");
            responseMarkdown.append("<details><summary>").append(Messages.EditDetailsSummary).append("</summary>\n\n");
            responseMarkdown.append(markdownUtils.buildGitDiff(path, oldContent, newContent));
            responseMarkdown.append("\n</details>");
            details.responseMarkdown = responseMarkdown.toString();

            return messageFactory.createMessage(this, call, response.toString(), details);
        });
    }

    /**
     * Wrapper class for replacement result.
     */
    private static class ReplacementResult
    {
        String updatedContent;
        int addedLines;
        int removedLines;
    }

    /**
     * Generates a user-friendly error message for replacement failures.
     *
     * @param replaceAll whether replace_all was true
     * @param replaceResult the replacement result
     * @return the error message
     */
    @SuppressWarnings("nls")
    private String getReplacementErrorMessage(boolean replaceAll, ReplaceResult replaceResult)
    {
        if (replaceAll)
        {
            return "Original content not found in file. Verify the `old_content`.";
        }
        else
        {
            if (replaceResult.hasMultipleOccurrences())
            {
                return "Multiple matches found for original content. Change the `old_content` to avoid multiple matches. Provide a larger `old_content` with more surrounding lines (minimum 3).";
            }
            else
            {
                return "Original content not found in file. Verify the `old_content`.";
            }
        }
    }

    /**
     * Creates a changes string for the response markdown.
     *
     * @param addedLines number of added lines
     * @param removedLines number of removed lines
     * @return the formatted changes string
     */
    @SuppressWarnings("nls")
    private String createChangesString(int addedLines, int removedLines)
    {
        var changes = new StringBuilder();
        if (addedLines > 0)
        {
            changes.append(markdownUtils.createStyledText("+" + addedLines, TextColor.GREEN, FontWeight.BOLD));
        }

        if (removedLines > 0)
        {
            if (changes.length() > 0)
            {
                changes.append(' ');
            }

            changes.append(markdownUtils.createStyledText("-" + removedLines, TextColor.RED, FontWeight.BOLD));
        }

        return changes.toString();
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
        description.append("Edits an existing file by exact string replacement.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- MUST read the file first with `" + ReadMcpTool.TOOL_NAME + "`!!!");
        description.append("\n- MUST remove line-number prefixes from `" + ReadMcpTool.TOOL_NAME + "` output before using `old_content` or `new_content`.");
        description.append("\n- MUST specify `old_content` exactly, including spaces, tabs, and line separators, but without line-number prefixes.");
        description.append("\n- Provide a unique `old_content` with enough surrounding lines (min 3) to avoid ambiguity.");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Use `replace_all` only when you want to replace every match.");
        description.append("\n- To delete content, set `new_content` to an empty string.");
        description.append("\n- Avoid emojis unless explicitly requested.");
        description.append("\n\nRelated tools:");
        description.append("\n- Create new files: `" + WriteMcpTool.TOOL_NAME + "`.");
        description.append("\n- Delete files: `" + DeleteMcpTool.TOOL_NAME + "`.");
        description.append("\n- MUST use `" + DeleteMarkersMcpTool.TOOL_NAME + "` and `" + SetMarkersMcpTool.TOOL_NAME
            + "` to update issues, plans, schedules, proposals, tasks, TODO, bookmarks, etc.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var pathProp = new McpToolCallProperty();
        pathProp.type = "string";
        pathProp.description = "Absolute path to the file. The system will auto-detect the project from the absolute path.";
        properties.put("path", pathProp);

        var oldContentProp = new McpToolCallProperty();
        oldContentProp.type = "string";
        oldContentProp.description = "The fragment of the file content that will be replaced. Provide a larger `old_content` with more surrounding lines (minimum 3).";
        properties.put("old_content", oldContentProp);

        var newContentProp = new McpToolCallProperty();
        newContentProp.type = "string";
        newContentProp.description = "The content fragment that will replace the original (`old_content`). Can be empty to delete content.";
        properties.put("new_content", newContentProp);

        var replaceAllProp = new McpToolCallProperty();
        replaceAllProp.type = "boolean";
        replaceAllProp.description = "If true, all occurrences of the `old_content` fragment will be replaced. "
            + "If false, only the single occurrence will be replaced. "
            + "If no fragments are found, or more than one is found, the request will fail. False by default.";
        properties.put("replace_all", replaceAllProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("path", "old_content", "new_content");
        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    private static class Request
    {
        /**
         * Absolute path to the file. Required.
         */
        @SerializedName("path")
        public String path;

        /**
         * The fragment of the file content that will be replaced.
         */
        @SerializedName("old_content")
        public String oldContent;

        /**
         * The content fragment that will replace the original content.
         */
        @SerializedName("new_content")
        public String newContent;

        /**
         * If true, all occurrences will be replaced.
         * If false, only single occurrence will be replaced.
         */
        @SerializedName("replace_all")
        public Boolean replaceAll;
    }

}

