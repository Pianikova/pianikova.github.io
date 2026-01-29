/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
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
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
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
        + "  \"project_name\": \"AccountingSystem\",\n"
        + "  \"relative_file_path\": \"src/MainModule.bsl\",\n"
        + "  \"origin_content\": \"Procedure Test()\\n    Message(\\\"Hello\\\");\\nEndProcedure\",\n"
        + "  \"new_content\": \"Procedure Test()\\n    Message(\\\"Hi\\\");\\nEndProcedure\",\n"
        + "  \"replace_all\": false\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "File updated: \"src/MainModule.bsl\"";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;
    private final IDispatcher dispatcher;
    private final IContentReplacer contentReplacer;
    private final IMarkdownUtils markdownUtils;
    private final IEditingSupport editingSupport;

    @Inject
    public EditMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem,
        IDispatcher dispatcher, IContentReplacer contentReplacer, IMarkdownUtils markdownUtils,
        IEditingSupport editingSupport)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(contentReplacer);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(editingSupport);

        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
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
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. JSON must be a single object with double-quoted keys and strings. "
                        + "Escape newlines as \\n and quotes as \\\". "
                        + "Use this example: " + QuestionExample
                        + "\n\nRequired fields: 'project_name' (string), 'relative_file_path' (string), "
                        + "'origin_content' (string), 'new_content' (string)"
                        + "\nOptional field: 'replace_all' (boolean)"));
        }

        var request = optionalRequest.get();
        var relativeFilePath = request.relativeFilePath;
        if (relativeFilePath == null || relativeFilePath.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "`relative_file_path` is required."));
        }

        var fileName = new File(relativeFilePath);
        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(Messages.EditTitle);

            // Add diff details
            if (request.originContent != null && request.newContent != null)
            {
                requestMarkdown.append("\n\n");
                requestMarkdown.append("<details><summary>").append(fileName.getName()).append("</summary>\n\n");
                requestMarkdown.append(
                    markdownUtils.buildGitDiff(relativeFilePath, request.originContent, request.newContent));
                requestMarkdown.append("\n</details>");
            }

            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var projectName = request.projectName;
        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "`project_name` is required."));
        }

        var originContent = request.originContent;
        if (originContent == null)
        {
            return CompletableFuture
                .completedFuture(
                    messageFactory.createError(this, call, "`origin_content` is required and cannot be null."));
        }

        var newContent = request.newContent;
        if (newContent == null)
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "`new_content` is required."));
        }

        var replaceAll = request.replaceAll != null ? request.replaceAll : false;

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);
            if (project == null || !project.exists())
            {
                return messageFactory.createError(this, call, "The project \"" + projectName + "\" does not exist.");
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
                    return messageFactory.createError(this, call,
                        "Cannot open the project \"" + projectName + "\". " + error.getMessage());
                }
            }

            var projectFile = fileSystem.getProjectFile(project, relativeFilePath);

            // Check if the file can be edited using editingSupport
            if (!editingSupport.canEdit(projectFile))
            {
                return messageFactory.createError(this, call, "The file \"" + relativeFilePath
                    + "\" cannot be edited. Editing is not supported for this file type or the file is locked.");
            }

            var optionalDocument = contentSourceProvider.getFileDocument(projectFile);
            if (optionalDocument.isEmpty())
            {
                return messageFactory.createError(this, call,
                    "The file \"" + relativeFilePath + "\" does not exist. Use the `" + WriteMcpTool.TOOL_NAME
                        + "` tool to create a new file.");
            }

            var fileDocument = optionalDocument.get();
            var document = fileDocument.getDocument();

            // Read current file content
            var optionalCurrentContent = dispatcher.dispatch(() -> document.get());
            if (optionalCurrentContent.isEmpty())
            {
                return messageFactory.createError(this, call, "Cannot read the file \"" + relativeFilePath + "\". ");
            }
            var currentContent = optionalCurrentContent.get();

            // Handle BOM (Byte Order Mark) if present
            String bom = "";
            if (currentContent.startsWith("\uFEFF"))
            {
                bom = "\uFEFF";
                currentContent = currentContent.substring(1);
            }

            // Use ContentReplacer to perform the replacement
            var replaceResult =
                contentReplacer.replace(currentContent, originContent, newContent, System.lineSeparator(), replaceAll);

            if (!replaceResult.isSuccess())
            {
                if (replaceAll)
                {
                    return messageFactory.createError(this, call,
                        "Original content not found in file. Verify the `origin_content`.");
                }
                else
                {
                    if (replaceResult.hasMultipleOccurrences())
                    {
                        return messageFactory.createError(this, call,
                            "Multiple matches found for original content. Change the `origin_content` to avoid multiple matches. Provide a larger `origin_content` with more surrounding lines (minimum 3).");
                    }
                    else
                    {
                        return messageFactory.createError(this, call,
                            "Original content not found in file. Verify the `origin_content`.");
                    }
                }
            }

            var updatedContent = bom + replaceResult.getUpdatedContent();
            var addedLines = replaceResult.getAddedLines();
            var removedLines = replaceResult.getRemovedLines();

            // Write updated content
            var optionalError = dispatcher.dispatch(() ->
            {
                try
                {
                    document.set(updatedContent);
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
                return messageFactory.createError(this, call,
                    "Failed to save file: " + optionalError.get().getMessage());
            }

            var response = new StringBuilder();
            var projectRelativePath = projectFile.getProjectRelativePath();
            response.append("File updated: \"").append(projectRelativePath.toPortableString()).append("\".\n");
            response.append(
                "ACTION REQUIRED: verify project errors and warnings. Use `" + GetMarkersMcpTool.TOOL_NAME + "` tool.");

            // Add response markdown
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

            details.responseMarkdown =
                MessageFormat.format(Messages.EditedTemplate, fileName.getName(), changes);

            return messageFactory.createMessage(this, call, response.toString(), details);
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
        description.append("Edits an existing file by exact string replacement.");
        description.append("\n\nUsage:");
        description.append("\n- MUST read the file first with `" + ReadMcpTool.TOOL_NAME + "`.");
        description.append("\n- Provide a unique `origin_content` with enough surrounding lines (min 3) to avoid ambiguity.");
        description.append("\n- Arguments must be a single JSON object with double-quoted keys/strings.");
        description.append("\n- Do NOT wrap JSON in Markdown or send arrays; no trailing commas or comments.");
        description.append("\n- Escape newlines as \\n and quotes as \\\".");
        description.append("\n- Remove line-number prefixes from `" + ReadMcpTool.TOOL_NAME + "` output before using `origin_content` or `new_content`.");
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

        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "Project name in IDE. For example, \"MyProject\".";
        properties.put("project_name", projectNameProp);

        var relativeFilePathProp = new McpToolCallProperty();
        relativeFilePathProp.type = "string";
        relativeFilePathProp.description = "Project relative path to the file. For example, \"src/MyModule.bsl\".";
        properties.put("relative_file_path", relativeFilePathProp);

        var originContentProp = new McpToolCallProperty();
        originContentProp.type = "string";
        originContentProp.description = "The fragment of the file content that will be replaced. Provide a larger `origin_content` with more surrounding lines (minimum 3).";
        properties.put("origin_content", originContentProp);

        var newContentProp = new McpToolCallProperty();
        newContentProp.type = "string";
        newContentProp.description = "The content fragment that will replace the original (`origin_content`). Can be empty to delete content.";
        properties.put("new_content", newContentProp);

        var replaceAllProp = new McpToolCallProperty();
        replaceAllProp.type = "boolean";
        replaceAllProp.description = "If true, all occurrences of the `origin_content` fragment will be replaced. "
            + "If false, only the single occurrence will be replaced. "
            + "If no fragments are found, or more than one is found, the request will fail. False by default.";
        properties.put("replace_all", replaceAllProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "relative_file_path", "origin_content", "new_content");
        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    private static class Request
    {
        /**
         * Project name in IDE.
         */
        @SerializedName("project_name")
        public String projectName;

        /**
         * Relative path to the file which content should be replaced.
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        /**
         * The fragment of the file content that will be replaced.
         */
        @SerializedName("origin_content")
        public String originContent;

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

