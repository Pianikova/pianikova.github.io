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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFiles;
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
import com.google.inject.Provider;

public class ReadMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Read"; //$NON-NLS-1$
    private static final int MAX_LINES = McpToolConstants.MAX_READ_LINES;
    private static final int LINE_NUMBER_WIDTH = 7;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"path\": \"C:/Projects/MyProject/src/Catalogs/Nomencaltura/Module.bsl\",\n"
        + "  \"first_line\": 1,\n"
        + "  \"lines_number\": 10\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\n"
        + "  \"content\": \"     1: // Module\\n     2: Procedure Test()\\n     3:     Message(\\\"Hello\\\");\\n     4: EndProcedure\\n     5:\\n     6: Procedure AnotherTest()\\n     7:     Var x = 10;\\n     8: EndProcedure\",\n"
        + "  \"charset_name\": \"UTF-8\",\n"
        + "  \"total_lines\": 42,\n"
        + "  \"note\": \"There are more lines in the file that were not read. Increase lines_number parameter to read more content.\"\n"
        + "}";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;
    private final IProjectTools projectTools;
    private final IMarkdownUtils markdownUtils;
    private final IFiles files;

    @Inject
    public ReadMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem,
        IProjectTools projectTools, IMarkdownUtils markdownUtils, IFiles files)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(files);

        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
        this.projectTools = projectTools;
        this.markdownUtils = markdownUtils;
        this.files = files;
        spec = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return false;
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
            throw new ToolException("Cannot deserialize arguments. JSON must be a single object with double-quoted keys and strings. "
                + "Use this example: " + QuestionExample);
        }

        var request = optionalRequest.get();
        var path = request.path;
        if (path == null || path.isBlank())
        {
            throw new ToolException("`path` is required.");
        }

        // Validate and set default values for line parameters
        var firstLineNumber =
            request.firstLine != null && request.firstLine > 0 ? request.firstLine : 1;
        var linesNumber = request.linesNumber != null && request.linesNumber > 0 ? request.linesNumber
            : McpToolConstants.DEFAULT_READ_LINES;

        // Apply maximum lines limit
        if (linesNumber > MAX_LINES)
        {
            linesNumber = MAX_LINES;
        }

        if (call.callKind == ToolCallKind.RENDER)
        {
            var href = markdownUtils.formatFilePath(path, firstLineNumber - 1, 0, firstLineNumber + linesNumber - 1, 0);
            details.requestMarkdown = MessageFormat.format(Messages.ReadTitleTemplate, href);
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        final int finalLinesNumber = linesNumber;
        final int finalFirstLineNumber = firstLineNumber;

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            // Determine project name from absolute path
            var detectedProjectName = projectTools.determineProjectName(path);
            final var finalProjectName = detectedProjectName;

            // Check if file is part of a project
            var isProjectFile = finalProjectName != null && !finalProjectName.isBlank();

            if (!isProjectFile)
            {
                // File is not part of any project - use Java file I/O
                try
                {
                    if (!fileSystem.fileExists(path))
                    {
                        var response = new HashMap<String, Object>();
                        response.put("content", "");
                        response.put("note", "The file \"" + path + "\" does not exist.");

                        details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate,
                            files.getDisplayedFileName(new File(path)),
                            markdownUtils.createStyledText("0/0", TextColor.RED, FontWeight.BOLD));

                        return messageFactory.createMessage(this, call, json.serialize(response), details);
                    }

                    // Stream lines from file, preserving original line separators and handling BOM
                    try (var fileInputStream = new FileInputStream(path);
                        var inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                        var reader = new BufferedReader(inputStreamReader))
                    {
                        var endLineNumber = finalFirstLineNumber + finalLinesNumber - 1;
                        var totalLines = 0;
                        var linesRead = 0;
                        var lastLineSize = 0;
                        var resultContent = new StringBuilder();
                        for (var line : fileSystem.getLines(reader))
                        {
                            totalLines++;
                            if (totalLines >= finalFirstLineNumber && totalLines <= endLineNumber)
                            {
                                resultContent.append(formatLineWithNumberPrefix(totalLines, line));
                                lastLineSize = line.length();
                                linesRead++;
                            }
                        }

                        var response = new HashMap<String, Object>();
                        response.put("content", resultContent.toString());
                        response.put("charset_name", "UTF-8");
                        response.put("total_lines", totalLines);

                        // Check if there are more lines based on totalLines
                        if (endLineNumber < totalLines)
                        {
                            response.put("note",
                                "There are more lines in the file that were not read. Increase lines_number parameter to read more content.");
                        }

                        // Add response markdown
                        var styledLineNumber = markdownUtils.createStyledText(
                            String.valueOf(String.format("%d/%d", linesRead, totalLines)),
                            TextColor.GREEN, FontWeight.BOLD);

                        var href = markdownUtils.formatFilePath(path, finalFirstLineNumber - 1, 0,
                            finalFirstLineNumber + linesRead - 1, lastLineSize);
                        details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate, href, styledLineNumber);
                        return messageFactory.createMessage(this, call, json.serialize(response), details);
                    }
                }
                catch (IOException error)
                {
                    throw new ToolException("Failed to read file", error, ToolErrorType.RETRYABLE);
                }
            }

            // File is part of a project - use Eclipse APIs
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(finalProjectName);

            // Validate project existence and accessibility
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
                    throw new ToolException("Cannot open the project \"" + finalProjectName + "\"", error,
                        ToolErrorType.RETRYABLE);
                }
            }

            var optionalDocument =
                projectTools.getProjectFile(project, path).flatMap(file -> contentSourceProvider.getFileDocument(file));

            if (optionalDocument.isEmpty())
            {
                var response = new HashMap<String, Object>();
                response.put("content", "");
                response.put("note",
                    "The file \"" + path + "\" does not exist within the IDE project context.");

                details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate,
                    files.getDisplayedFileName(new File(path)),
                    markdownUtils.createStyledText("0/0", TextColor.RED, FontWeight.BOLD));

                return messageFactory.createMessage(this, call, json.serialize(response), details);
            }

            var document = optionalDocument.get();
            var doc = document.getDocument();

            var resultContent = new StringBuilder();
            var linesRead = 0;
            var lastLineSize = 0;
            for (var line : fileSystem.getLines(document, finalFirstLineNumber - 1, finalLinesNumber))
            {
                resultContent.append(formatLineWithNumberPrefix(finalFirstLineNumber + linesRead, line));
                lastLineSize = line.length();
                linesRead++;
            }

            // Prepare response
            var response = new Response();
            response.content = resultContent.toString();
            response.charsetName = document.getCharset().name();
            int totalLines = doc.getNumberOfLines();
            response.totalLines = totalLines;

            // Check if there are more lines based on totalLines
            int endLineNumber = finalFirstLineNumber + finalLinesNumber - 1;
            if (totalLines > endLineNumber)
            {
                response.note =
                    "There are more lines in the file that were not read. Increase lines_number parameter to read more content.";
            }

            var content = json.serialize(response);

            // Add response markdown
            String styledLineNumber =
                markdownUtils.createStyledText(String.format("%d/%d", linesRead, totalLines), TextColor.GREEN,
                    FontWeight.BOLD);

            var href = markdownUtils.formatFilePath(path, finalFirstLineNumber - 1, 0,
                finalFirstLineNumber + linesRead - 1,
                lastLineSize);
            details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate, href, styledLineNumber);
            details.hideAfter = totalLines == 0;
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
        description.append("Reads file content from a project or from the file system.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- `path` is required and must be an absolute path. The system will auto-detect the project from the absolute path.");
        description.append("\n- IMPORTANT: Each line is prefixed with a 7-digit line number and colon (e.g., `    123:`); strip it before editing.");
        description.append("\n- Send only the raw JSON object (no code fences or extra text).");
        description.append("\n- Example (single line is OK): {\"path\":\"C:/Projects/MyProject/src/MainModule.bsl\",\"first_line\":1,\"lines_number\":200}");
        description.append("\n- Defaults: `first_line` = 1, `lines_number` = 2000; capped at " + MAX_LINES + ".");
        description.append("\n- Preserve exact whitespace and line endings (\\r, \\n, \\t).");
        description.append("\n- If you plan to edit, read a larger chunk to make `origin_content` unique.");
        description.append("\n\nRelated tools:");
        description.append("\n- Locate files: `" + FindMcpTool.TOOL_NAME + "`.");
        description.append("\n- Modify files: `" + EditMcpTool.TOOL_NAME + "`, `" + WriteMcpTool.TOOL_NAME + "`.");
        description.append("\n- MUST use `" + DeleteMarkersMcpTool.TOOL_NAME + "` and `" + SetMarkersMcpTool.TOOL_NAME
            + "` to update issues, plans, schedules, proposals, tasks, TODO, bookmarks, etc.");
        description.append("\n\nFile references in responses should use HTML `<a>` tags with `edt-file://` URLs and include a `title` attribute:");
        description.append("\n- Format: `<a href=\"edt-file://full_path:line:column:finish_line:finish_column\" title=\"description\">text</a>`");
        description.append("\n- Line and column numbers are 0-based integers.");
        description.append("\n- Example: `<a href=\"edt-file://C:/Projects/MyProject/src/Module.bsl:10:0:20:50\">Procedure in Module.bsl</a>`");
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

        var firstLineNumberProp = new McpToolCallProperty();
        firstLineNumberProp.type = "integer";
        firstLineNumberProp.description = "Number of the first line of the file to be read. It is 1-relative. The default is 1";
        properties.put("first_line", firstLineNumberProp);

        var linesNumberProp = new McpToolCallProperty();
        linesNumberProp.type = "integer";
        linesNumberProp.description = "Number of lines to read. Default is 2000, maximum is " + MAX_LINES + ".";
        properties.put("lines_number", linesNumberProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("path");
        spec.function.parameters = parameters;

        return spec;
        // @formatter:on
    }

    /**
     * Formats a line with a line number prefix.
     *
     * @param lineNumber the line number (1-relative)
     * @param lineContent the line content
     * @return the formatted line with prefix
     */
    @SuppressWarnings("nls")
    private static String formatLineWithNumberPrefix(int lineNumber, String lineContent)
    {
        return String.format("%" + LINE_NUMBER_WIDTH + "d:", lineNumber) + lineContent;
    }

    private static class Request
    {
        /**
         * Absolute path to the file. Required.
         */
        @SerializedName("path")
        public String path;

        /**
         * Number of the first line of the file to be read. It is 1-relative. The default is 1.
         */
        @SerializedName("first_line")
        public Integer firstLine;

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

        /**
         * Optional note with additional information.
         */
        @SerializedName("note")
        public String note;

        /**
         * Total number of lines in the file.
         */
        @SerializedName("total_lines")
        public Integer totalLines;
    }
}



