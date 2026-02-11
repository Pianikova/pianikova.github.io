/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.File;
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
        + "  \"path\": \"C:/Projects/AccountingSystem/src/MainModule.bsl\",\n"
        + "  \"first_line\": 50,\n"
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
                        + "Use this example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        var path = request.path;
        if (path == null || path.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "`path` is required."));
        }

        var fileName = new File(path);
        if (call.callKind == ToolCallKind.RENDER)
        {
            int lineNumber = request.firstLine != null && request.firstLine > 0 ? request.firstLine : 1;
            int linesNumber = request.linesNumber != null && request.linesNumber > 0 ? request.linesNumber : McpToolConstants.DEFAULT_READ_LINES;
            int finishLineNumber = lineNumber + linesNumber;

            details.requestMarkdown =
                MessageFormat.format(Messages.ReadTitleTemplate,
                    markdownUtils.formatFilePath(path, lineNumber, 0, finishLineNumber, 0));
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        // Validate and set default values for line parameters
        int firstLineNumber =
            request.firstLine != null && request.firstLine > 0 ? request.firstLine : 1;
        int linesNumber = request.linesNumber != null && request.linesNumber > 0 ? request.linesNumber : McpToolConstants.DEFAULT_READ_LINES;

        // Apply maximum lines limit
        if (linesNumber > MAX_LINES)
        {
            linesNumber = MAX_LINES;
        }

        final int finalLinesNumber = linesNumber;
        final int finalFirstLineNumber = firstLineNumber;

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            // Determine project name from absolute path
            String detectedProjectName = fileSystem.determineProjectName(path);
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
                        return messageFactory.createError(this, call,
                            "The file \"" + path + "\" does not exist.");
                    }

                    byte[] fileData = fileSystem.readAllBytes(path);
                    String content = removeBOM(new String(fileData, StandardCharsets.UTF_8));

                    int endLine = Math.min(finalFirstLineNumber + finalLinesNumber, countLines(content));
                    Iterable<String> lineIterator = createLineIterator(content, finalFirstLineNumber, endLine);

                    var resultContent = new StringBuilder();
                    resultContent.append(readLinesWithPrefix(lineIterator, finalFirstLineNumber, endLine, null));

                    var response = new HashMap<String, Object>();
                    response.put("content", resultContent.toString());
                    response.put("charset_name", "UTF-8");

                    // Add response markdown
                    String styledLineNumber = markdownUtils.createStyledText(String.valueOf(finalFirstLineNumber),
                        TextColor.GREEN, FontWeight.BOLD);

                    // Use endLine directly for the link range
                    details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate,
                        markdownUtils.formatFilePath(path, finalFirstLineNumber, 0, endLine, 0), styledLineNumber);

                    return messageFactory.createMessage(this, call, resultContent.toString(), details);
                }
                catch (IOException error)
                {
                    return messageFactory.createError(this, call, "Failed to read file. " + error.getMessage());
                }
            }

            // File is part of a project - use Eclipse APIs
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(finalProjectName);

            // Validate project existence and accessibility
            if (project == null || !project.exists())
            {
                return messageFactory.createError(this, call, "The project \"" + finalProjectName + "\" does not exist.");
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
                        "Cannot open the project \"" + finalProjectName + "\". " + error.getMessage());
                }
            }

            var projectFile = fileSystem.getProjectFile(project, path);
            var optionalDocument = contentSourceProvider.getFileDocument(projectFile);
            if (optionalDocument.isEmpty())
            {
                var filePathForError = projectFile.getProjectRelativePath().toOSString();
                return messageFactory.createError(this, call,
                    "The file \"" + filePathForError + "\" does not exist within the IDE project context. "
                        + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope.");
            }

            var document = optionalDocument.get();
            var idocument = document.getDocument();

            var resultContent = new StringBuilder();
            var lineNumber = finalFirstLineNumber;
            var lines = fileSystem.getLines(document, finalFirstLineNumber - 1, finalLinesNumber);
            int endLine = Math.min(finalFirstLineNumber + finalLinesNumber, idocument.getNumberOfLines());

            resultContent.append(readLinesWithPrefix(lines, lineNumber, endLine, null));

            // Prepare response
            var response = new Response();
            response.content = resultContent.toString();
            response.charsetName = document.getCharset().name();
            var content = json.serialize(response);

            // Add response markdown
            String styledLineNumber = markdownUtils.createStyledText(String.valueOf(lineNumber), TextColor.GREEN, FontWeight.BOLD);

            // Use endLine directly for the link range
            details.responseMarkdown =
                MessageFormat.format(Messages.ReadTemplate,
                    markdownUtils.formatFilePath(path, lineNumber, 0, endLine, 0), styledLineNumber);
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
    }

    /**
     * Removes BOM (Byte Order Mark) from the beginning of the content if present.
     *
     * @param content the content to process
     * @return the content without BOM
     */
    private static String removeBOM(String content)
    {
        if (content == null || content.isEmpty())
        {
            return content;
        }

        // UTF-8 BOM is EF BB BF
        if (content.startsWith("\uFEFF")) //$NON-NLS-1$
        {
            return content.substring(1);
        }

        return content;
    }

    /**
     * Counts the number of lines in the content by counting line endings.
     *
     * @param content the content to count lines in
     * @return the number of lines
     */
    private static int countLines(String content)
    {
        if (content == null || content.isEmpty())
        {
            return 0;
        }

        int count = 1;
        int length = content.length();
        for (int i = 0; i < length; i++)
        {
            char c = content.charAt(i);
            if (c == '\r')
            {
                if (i + 1 < length && content.charAt(i + 1) == '\n')
                {
                    i++; // Skip \n after \r
                }
                count++;
            }
            else if (c == '\n')
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Creates an iterator that reads lines from content with their original line endings preserved.
     * Each line returned includes its line ending (except possibly the last line).
     *
     * @param content the content to read from
     * @param startLine the starting line number (1-based)
     * @param endLine the ending line number (exclusive)
     * @return an iterable of lines with their original line endings
     */
    private static Iterable<String> createLineIterator(String content, int startLine, int endLine)
    {
        return () -> new java.util.Iterator<>() {
            int currentLine = startLine;
            int position = 0;
            int length = content != null ? content.length() : 0;
            boolean hasNextLine = content != null && !content.isEmpty();

            {
                // Skip to the starting line
                for (int line = 1; line < startLine && hasNextLine; line++)
                {
                    advanceToNextLine();
                }
            }

            private void advanceToNextLine()
            {
                while (position < length)
                {
                    char c = content.charAt(position);
                    if (c == '\r')
                    {
                        position++;
                        if (position < length && content.charAt(position) == '\n')
                        {
                            position++;
                        }
                        return;
                    }
                    else if (c == '\n')
                    {
                        position++;
                        return;
                    }
                    position++;
                }
                hasNextLine = false;
            }

            @Override
            public boolean hasNext()
            {
                return hasNextLine && currentLine < endLine;
            }

            @Override
            public String next()
            {
                if (!hasNext())
                {
                    throw new java.util.NoSuchElementException();
                }

                var lineBuilder = new StringBuilder();
                int lineStart = position;
                boolean foundLineEnd = false;

                while (position < length)
                {
                    char c = content.charAt(position);

                    if (c == '\r')
                    {
                        lineBuilder.append(content, lineStart, position);
                        position++;
                        if (position < length && content.charAt(position) == '\n')
                        {
                            lineBuilder.append("\r\n"); //$NON-NLS-1$
                            position++;
                        }
                        else
                        {
                            lineBuilder.append('\r');
                        }
                        foundLineEnd = true;
                        break;
                    }
                    else if (c == '\n')
                    {
                        lineBuilder.append(content, lineStart, position);
                        lineBuilder.append('\n');
                        position++;
                        foundLineEnd = true;
                        break;
                    }
                    position++;
                }

                if (!foundLineEnd && lineStart < length)
                {
                    // Last line without line ending
                    lineBuilder.append(content, lineStart, length);
                    hasNextLine = false;
                }

                currentLine++;
                return lineBuilder.toString();
            }
        };
    }

    /**
     * Reads lines from an iterable and adds line number prefixes.
     * Lines are assumed to already include their line endings (if applicable).
     *
     * @param lines the iterable of lines to read
     * @param startLineNumber the starting line number (1-based)
     * @param endLine the ending line number (exclusive)
     * @param lineSeparator the line separator to use (ignored if null, lines must already contain line endings)
     * @return the formatted content with line number prefixes
     */
    @SuppressWarnings("nls")
    private static String readLinesWithPrefix(Iterable<String> lines, int startLineNumber, int endLine, String lineSeparator)
    {
        var resultContent = new StringBuilder();
        var lineNumber = startLineNumber;

        for (var line : lines)
        {
            var prefix = String.format("%7d:", lineNumber);
            resultContent.append(prefix).append(" ").append(line);
            lineNumber++;
        }

        return resultContent.toString();
    }
}



