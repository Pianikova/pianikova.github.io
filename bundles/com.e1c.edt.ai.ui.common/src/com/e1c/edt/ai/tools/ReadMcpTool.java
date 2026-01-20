/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

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
    private static final int MAX_LINES = 4000; // Maximum lines to read
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
        + "  \"content\": \"Процедура Пример()\\n    Сообщить(\\\"Привет, мир!\\\");\\nКонецПроцедуры\",\n"
        + "  \"charset_name\": \"UTF-8\"\n"
        + "}";
    // @formatter:on
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;

    @Inject
    public ReadMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
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
        var projectName = request.projectName;
        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "`project_name` is required."));
        }
        var relativeFilePath = request.relativeFilePath;
        if (relativeFilePath == null || relativeFilePath.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "`relative_file_path` is required."));
        }

        // Validate and set default values for line parameters
        int firstLineNumber =
            request.firstLineNumber != null && request.firstLineNumber >= 0 ? request.firstLineNumber : 0;
        int linesNumber = request.linesNumber != null && request.linesNumber > 0 ? request.linesNumber : 2000;

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
            var optionalFileContent = contentSourceProvider.getFileContent(projectFile);
            if (optionalFileContent.isEmpty())
            {
                return messageFactory.createError(this, call, "The file \"" + relativeFilePath + "\" does not exist.");
            }
            var fileContent = optionalFileContent.get();
            var resultContent = new StringBuilder();
            var charset = fileContent.getCharset();

            try (var inputStream = fileContent.getInputStream().orElse(null);
                var scanner = new Scanner(new BufferedInputStream(inputStream), charset.name()))
            {
                // Use custom delimiter to preserve original line separators
                scanner.useDelimiter("(?<=(\r\n|\n|\r))");

                int currentLine = 0;
                // Skip lines until we reach the starting line
                while (currentLine < firstLineNumber && scanner.hasNext())
                {
                    scanner.next();
                    currentLine++;
                }

                // Read the requested number of lines preserving separators
                int linesRead = 0;
                while (linesRead < curLinesNumber && scanner.hasNext())
                {
                    String lineWithSeparator = scanner.next();
                    resultContent.append(lineWithSeparator);
                    linesRead++;
                }
            }
            catch (IOException | NullPointerException e)
            {
                return messageFactory.createError(this, call, "Failed to read file content. " + e.getMessage());
            }

            // Prepare response
            var response = new Response();
            response.content = resultContent.toString();
            response.charsetName = charset.name();
            var content = json.serialize(response);
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
        description.append("Reads the content of a project file. It is okay to read a file that does not exist; an error will be returned.");
        description.append("\n\nUsage:");
        description.append("\n- The file_path parameter must be a project relative path, not an absolute path.");
        description.append("\n- By default, it reads up to 2000 lines starting from the beginning of the file.");
        description.append("\n- You can optionally specify a line offset and limit (especially handy for long files), but it's recommended to read the whole file by not providing these parameters.");
        description.append("\n- Any lines longer than " + MAX_LINES + " characters will be truncated.");
        description.append("\n- The edit will FAIL if `origin_content` is not unique in the file. ");
        description.append("Either provide a larger string with more surrounding context to make it unique or use `replace_all` to change every instance of `origin_content`.");
        description.append("\n- Use `replace_all` for replacing and renaming strings across the file. This parameter is useful if you want to rename a variable for instance.");
        description.append("\n- You must use `" + DeleteMarkersMcpTool.TOOL_NAME + "` and '" + SetMarkersMcpTool.TOOL_NAME + "' tools to update issues, plans, schedules, proposals, tasks, TODO, bookmarks, etc.");
        description.append("\nFor example:"); // Fixed typo: exapmple -> example
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
        // Fixed property assignments
        var firstLineNumberProp = new McpToolCallProperty();
        firstLineNumberProp.type = "integer";
        firstLineNumberProp.description = "Number of the first line to read (0-based index). Default is 0.";
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
         * Number of the first line of the file to be read. Numbering starts at 0. The default is 0.
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
         * File content.
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