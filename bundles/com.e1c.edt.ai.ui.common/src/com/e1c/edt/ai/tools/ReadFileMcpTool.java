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
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ReadFileContentRequest;
import com.e1c.edt.ai.assistent.model.ReadFileContentResponse;
import com.e1c.edt.ai.ui.IContentSourceProvider;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;


public class ReadFileMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_read_file"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"AccountingSystem\",\n"
        + "  \"relative_file_path\": \"/src/MainModule.bsl\",\n"
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

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IContentSourceProvider contentSourceProvider;
    private final IProgressMonitor monitor;
    private final IFileSystem fileSystem;

    @Inject
    public ReadFileMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory,
        IContentSourceProvider contentSourceProvider, IProgressMonitor monitor, IFileSystem fileSystem)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(monitor);
        Preconditions.checkNotNull(fileSystem);
        this.log = log;
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
        var optionalCallArgs = json.deserialize(call.function.arguments, ReadFileContentRequest.class);
        if (optionalCallArgs.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var callArgs = optionalCallArgs.get();
        var projectName = callArgs.projectName;
        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "'project_name' is required."));
        }

        var relativeFilePath = callArgs.relativeFilePath;
        if (relativeFilePath == null || relativeFilePath.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "'relative_file_path' is required."));
        }

        var firstLineNumber = callArgs.firstLineNumber;
        if (firstLineNumber == null)
        {
            firstLineNumber = 0;
        }

        var actualFirstLineNumber = firstLineNumber;

        var linesNumber = callArgs.linesNumber;
        if (linesNumber == null)
        {
            linesNumber = 2000;
        }

        var actualLinesNumber = linesNumber;

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);
            if (project == null)
            {
                return messageFactory.createError(this, call, "Cannot get the project \"" + projectName + "\".");
            }

            if (!project.exists())
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
                    return messageFactory.createError(this, call, "Cannot open the project \"" + projectName + "\". " + error.getMessage());
                }
            }

            if (!project.isOpen())
            {
                return messageFactory.createError(this, call, "Cannot open the project \"" + projectName + "\". ");
            }

            var projectFile = fileSystem.getProjectFile(project, relativeFilePath);
            var optionalFileContent = contentSourceProvider.getFileContent(projectFile);
            if (optionalFileContent.isEmpty())
            {
                return messageFactory.createError(this, call, "The file \"" + relativeFilePath + "\" does not exist.");
            }

            var fileContent = optionalFileContent.get();

            var resultContent = new StringBuilder();
            int targetEndLine = actualFirstLineNumber + actualLinesNumber - 1;
            int currentLine = 0;
            var optionalInpiutStream = fileContent.getInputStream();
            if (optionalInpiutStream.isEmpty())
            {
                return messageFactory.createError(this, call, "Cannot read the file \"" + relativeFilePath + "\".");
            }

            try (var is = optionalInpiutStream.get();
                 var isr = new InputStreamReader(is, fileContent.getCharset());
                 var reader = new BufferedReader(isr)) {
                int c;
                while ((c = reader.read()) != -1)
                {
                    // Check if current line is within target range
                    var inTarget = (currentLine >= actualFirstLineNumber && currentLine <= targetEndLine);
                    if (c == '\r')
                    {
                        // Handle carriage return (possible Windows line ending)
                        reader.mark(1);
                        var next = reader.read();
                        if (next == '\n')
                        {
                            // CRLF sequence (Windows)
                            if (inTarget)
                            {
                                resultContent.append((char) c);
                                resultContent.append((char) next);
                            }

                            currentLine++;
                            if (currentLine > targetEndLine) break;
                        }
                        else
                        {
                            // Single CR (Mac/old systems)
                            if (next != -1) reader.reset();  // Put back non-LF character
                            if (inTarget)
                            {
                                resultContent.append((char) c);
                            }

                            currentLine++;
                            if (currentLine > targetEndLine)
                            {
                                break;
                            }
                        }
                    }
                    else
                        if (c == '\n')
                        {
                            // LF sequence (Unix/Linux)
                            if (inTarget)
                            {
                                resultContent.append((char) c);
                            }

                            currentLine++;
                            if (currentLine > targetEndLine) {
                                break; }
                    } else {
                        // Regular character
                        if (inTarget) {
                            resultContent.append((char) c);
                        }
                    }
                }
            }
            catch (IOException e) {
                return messageFactory.createError(this, call, "Failed to get file content. " + e.getMessage());
            }

            var result = new ReadFileContentResponse();
            result.contents = resultContent.toString();
            result.charsetName = fileContent.getCharset().name();
            var content = json.serialize(result);
            return messageFactory.createMessage(this, call, content);
        }).exceptionally(ex -> {
            var cause = ex instanceof CompletionException ? ex.getCause() : ex;
            return messageFactory.createError(this, call, "Failed to get. " + cause.getMessage());
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

        description.append("Reads the contents of a text file in the project.");

        description.append("\nFor exapmple:");
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
        relativeFilePathProp.description = "Project relative path to the file. For example, \"/src/MyModule.bsl\".";
        properties.put("relative_file_path", relativeFilePathProp);

        var firsLlineNumberProp = new McpToolCallProperty();
        firsLlineNumberProp.type = "integer";
        firsLlineNumberProp.description = "Number of the first line of the file to be read. Numbering starts at 0. The default is 0.";
        properties.put("first_line_number", relativeFilePathProp);

        var linesNumberProp = new McpToolCallProperty();
        linesNumberProp.type = "integer";
        linesNumberProp.description = "Number of lines to read. By default, reads up to 2000 lines.";
        properties.put("lines_number", relativeFilePathProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "relative_file_path");

        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }
}