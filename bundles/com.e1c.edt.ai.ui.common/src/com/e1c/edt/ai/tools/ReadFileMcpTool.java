/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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

public class ReadFileMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_read_file"; //$NON-NLS-1$
    private static final int MAX_LINES = 5000; // Maximum lines to read

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
    private final IProgressMonitor monitor;
    private final IFileSystem fileSystem;

    @Inject
    public ReadFileMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider, IProgressMonitor monitor, IFileSystem fileSystem)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(monitor);
        Preconditions.checkNotNull(fileSystem);

        this.json = json;
        this.messageFactory = messageFactory;
        this.contentSourceProvider = contentSourceProvider;
        this.monitor = monitor;
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
                    "'project_name' is required."));
        }

        var relativeFilePath = request.relativeFilePath;
        if (relativeFilePath == null || relativeFilePath.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "'relative_file_path' is required."));
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
                var reader = new BufferedReader(new InputStreamReader(inputStream, charset)))
            {
                String line;
                int currentLine = 0;
                int linesToRead = 0;
                int endLine = firstLineNumber + curLinesNumber;

                // Skip lines until we reach the starting line
                while (currentLine < firstLineNumber && reader.readLine() != null)
                {
                    currentLine++;
                }

                // Read the requested number of lines
                while (currentLine < endLine && (line = reader.readLine()) != null)
                {
                    if (linesToRead > 0)
                    {
                        resultContent.append('\n'); // Add newline between lines
                    }

                    resultContent.append(line);
                    currentLine++;
                    linesToRead++;
                }
            }
            catch (IOException | NullPointerException e)
            {
                return messageFactory.createError(this, call, "Failed to read file content. " + e.getMessage());
            }

            // Prepare response
            var response = new Response();
            response.contents = resultContent.toString();
            response.charsetName = charset.name();

            var content = json.serialize(response);
            return messageFactory.createMessage(this, call, content);
        }).exceptionally(ex -> {
            var cause = ex instanceof CompletionException ? ex.getCause() : ex;
            return messageFactory.createError(this, call, "Failed to read file content. " + cause.getMessage());
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
        description.append("Reads the contents of a project file.");
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
         * File contents.
         */
        @SerializedName("contents")
        public String contents;

        /**
         * File encoding, for example, "UTF-8", "windows-1251", "KOI8-R", "UTF-16", "UTF-32", etc.
         */
        @SerializedName("charset_name")
        public String charsetName;
    }
}