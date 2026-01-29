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
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ReadMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Read"; //$NON-NLS-1$
    private static final int MAX_LINES = McpToolConstants.MAX_READ_LINES;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"AccountingSystem\",\n"
        + "  \"relative_file_path\": \"src/MainModule.bsl\",\n"
        + "  \"first_line_number\": 50,\n"
        + "  \"lines_number\": 100\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\n"
        + "  \"content\": \"     51: Procedure Test()\\n     52:     Message(\\\"Hello\\\");\\n     53: EndProcedure\",\n"
        + "  \"charset_name\": \"UTF-8\"\n"
        + "}";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;
    private final IMarkdownUtils markdownUtils;

    @Inject
    public ReadMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(markdownUtils);
        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
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

        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. JSON must be a single object with double-quoted keys and strings. "
                        + "Escape newlines as \\n and quotes as \\\". "
                        + "Use this example: " + QuestionExample));
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
            details.requestMarkdown = MessageFormat.format(Messages.ReadTitleTemplate, fileName.getName());
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var projectName = request.projectName;
        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "`project_name` is required."));
        }

        // Validate and set default values for line parameters
        int firstLineNumber =
            request.firstLineNumber != null && request.firstLineNumber > 0 ? request.firstLineNumber : 1;
        int linesNumber = request.linesNumber != null && request.linesNumber > 0 ? request.linesNumber : McpToolConstants.DEFAULT_READ_LINES;

        // Apply maximum lines limit
        if (linesNumber > MAX_LINES)
        {
            linesNumber = MAX_LINES;
        }

        var curLinesNumber = linesNumber;

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);

            // Validate project existence and accessibility
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
            var optionalDocument = contentSourceProvider.getFileDocument(projectFile);
            if (optionalDocument.isEmpty())
            {
                return messageFactory.createError(this, call, "The file \"" + relativeFilePath + "\" does not exist.");
            }

            var document = optionalDocument.get();
            var resultContent = new StringBuilder();
            var lineNumber = firstLineNumber;
            for (var line : fileSystem.getLines(document, firstLineNumber - 1, curLinesNumber))
            {
                var prefix = String.format("%7d:", lineNumber);
                resultContent.append(prefix).append(line);
                lineNumber++;
            }

            // Prepare response
            var response = new Response();
            response.content = resultContent.toString();
            response.charsetName = document.getCharset().name();
            var content = json.serialize(response);

            // Add response markdown
            String styledLineNumber = markdownUtils.createStyledText(String.valueOf(lineNumber), TextColor.GREEN, FontWeight.BOLD);
            details.responseMarkdown =
                MessageFormat.format(Messages.ReadTemplate, fileName.getName(), styledLineNumber);
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
        description.append("Reads file content from a project.");
        description.append("\n\nUsage:");
        description.append("\n- `project_name` and `relative_file_path` are required; path is project-relative.");
        description.append("\n- Arguments must be a single JSON object with double-quoted keys/strings.");
        description.append("\n- Do NOT wrap JSON in Markdown or send arrays; no trailing commas or comments.");
        description.append("\n- Escape newlines as \\n and quotes as \\\".");
        description.append("\n- Send only the raw JSON object (no code fences or extra text).");
        description.append("\n- Example (single line is OK): {\"project_name\":\"MyProject\",\"relative_file_path\":\"src/MainModule.bsl\",\"first_line_number\":1,\"lines_number\":200}");
        description.append("\n- Defaults: `first_line_number` = 1, `lines_number` = 2000; capped at " + MAX_LINES + ".");
        description.append("\n- Each line is prefixed with a 7-digit line number and colon (e.g., `    123:`); strip it before editing.");
        description.append("\n- Preserve exact whitespace and line endings (\\r, \\n, \\t).");
        description.append("\n- If you plan to edit, read a larger chunk to make `origin_content` unique.");
        description.append("\n\nRelated tools:");
        description.append("\n- Locate files: `" + SearchFilesMcpTool.TOOL_NAME + "`, `" + FindMcpTool.TOOL_NAME + "`.");
        description.append("\n- Modify files: `" + EditMcpTool.TOOL_NAME + "`, `" + WriteMcpTool.TOOL_NAME + "`.");
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

        var firstLineNumberProp = new McpToolCallProperty();
        firstLineNumberProp.type = "integer";
        firstLineNumberProp.description = "Number of the first line of the file to be read. It is 1-relative. The default is 1";
        properties.put("first_line_number", firstLineNumberProp);

        var linesNumberProp = new McpToolCallProperty();
        linesNumberProp.type = "integer";
        linesNumberProp.description = "Number of lines to read. Default is 2000, maximum is " + MAX_LINES + ".";
        properties.put("lines_number", linesNumberProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "relative_file_path");
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
         * Relative path to the file. For example, "/src/MyModule.bsl".
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        /**
         * Number of the first line of the file to be read. It is 1-relative. The default is 1.
         */
        @SerializedName("first_line_number")
        public Integer firstLineNumber;

        /**
         * Number of lines to read. By default, reads up to 2000 lines.
         */
        @SerializedName("lines_number")
        public Integer linesNumber;
    }

    private static class Response
    {
        /**
         * File content with line number prefixes (8-character header with colon).
         */
        @SerializedName("content")
        public String content;

        /**
         * File encoding, for example, "UTF-8", "windows-1251", "KOI8-R", "UTF-16", "UTF-32", etc.
         */
        @SerializedName("charset_name")
        public String charsetName;
    }
}


