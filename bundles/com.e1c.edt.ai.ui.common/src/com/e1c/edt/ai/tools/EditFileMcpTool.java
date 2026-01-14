/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

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
public class EditFileMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_edit_file"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"AccountingSystem\",\n"
        + "  \"relative_file_path\": \"src/MainModule.bsl\",\n"
        + "  \"origin_contents\": \"Сообщить(\\\"Привет, мир!\\\");\",\n"
        + "  \"new_contents\": \"Сообщить(\\\"Hello, world!\\\");\",\n"
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
    private final IProgressMonitor monitor;
    private final IFileSystem fileSystem;

    @Inject
    public EditFileMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
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

        var originContents = request.originContents;
        if (originContents == null || originContents.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "'origin_contents' must not be empty."));
        }

        var newContents = request.newContents;
        if (newContents == null)
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "'new_contents' is required."));
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
                return messageFactory.createError(this, call,
                    "The file \"" + relativeFilePath + "\" does not exist. Use the '" + WriteFileMcpTool.TOOL_NAME
                        + "' tool to create a new file.");
            }

            var fileContent = optionalFileContent.get();

            // Read current file content
            String currentContent;
            try (var input = fileContent.getInputStream().orElseThrow())
            {
                currentContent = new String(input.readAllBytes(), fileContent.getCharset());
            }
            catch (IOException | NoSuchElementException e)
            {
                return messageFactory.createError(this, call, "Failed to read file content: " + e.getMessage());
            }

            // Perform content replacement
            String updatedContent;
            if (replaceAll)
            {
                updatedContent = currentContent.replace(
                    java.util.regex.Matcher.quoteReplacement(originContents),
                    java.util.regex.Matcher.quoteReplacement(newContents));

                if (updatedContent.equals(currentContent))
                {
                    return messageFactory.createError(this, call,
                        "Original content not found in file. Verify the 'origin_contents'.");
                }
            }
            else
            {
                int firstIndex = currentContent.indexOf(originContents);
                if (firstIndex == -1)
                {
                    return messageFactory.createError(this, call,
                        "Original content not found in file. Verify the 'origin_contents'.");
                }

                int secondIndex = currentContent.indexOf(originContents, firstIndex + originContents.length());
                if (secondIndex != -1)
                {
                    return messageFactory.createError(this, call,
                        "Multiple matches found for original content. Change the 'origin_contents' to avoid multiple matches.");
                }

                updatedContent = currentContent.substring(0, firstIndex) + newContents
                    + currentContent.substring(firstIndex + originContents.length());
            }

            // Write updated content
            try (var output = fileContent.getOutputStream().orElseThrow())
            {
                output.write(updatedContent.getBytes(fileContent.getCharset()));
            }
            catch (IOException | NoSuchElementException e)
            {
                return messageFactory.createError(this, call, "Failed to write file content: " + e.getMessage());
            }

            var content = new StringBuilder();
            var projectRelativePath = projectFile.getProjectRelativePath();
            content.append("File updated: \"").append(projectRelativePath.toPortableString()).append("\".\n");

            return messageFactory.createMessage(this, call, content.toString());
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
        description.append("Edits/updates/modifies the contents of an existing project file.");
        description.append("\nFor example:"); // Исправлено: exapmple -> example
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

        var originContentsProp = new McpToolCallProperty();
        originContentsProp.type = "string";
        originContentsProp.description = "The fragment of the file content that will be replaced.";
        properties.put("origin_contents", originContentsProp);

        var newContentsProp = new McpToolCallProperty();
        newContentsProp.type = "string";
        newContentsProp.description = "The content fragment that will replace the original ('origin_contents').";
        properties.put("new_contents", newContentsProp);

        var replaceAllProp = new McpToolCallProperty();
        replaceAllProp.type = "boolean";
        replaceAllProp.description = "If true, all occurrences of the 'origin_contents' fragment will be replaced. "
            + "If false, only the single occurrence will be replaced. "
            + "If no fragments are found, or more than one is found, the request will fail. False by default.";
        properties.put("replace_all", replaceAllProp); // Исправлено: newContentsProp -> replaceAllProp

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "relative_file_path", "origin_contents", "new_contents");

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
         * Relative path to the file which contents should be replaced. Must start with the project name, for example, "src/MyModule.bsl".
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        /**
         * The fragment of the file content that will be replaced.
         */
        @SerializedName("origin_contents")
        public String originContents;

        /**
         * The content fragment that will replace the original ('origin_contents').
         */
        @SerializedName("new_contents")
        public String newContents;

        /**
         * If true, all occurrences of the 'origin_contents' fragment will be replaced.
         * If false, only the single occurrence will be replaced. If no fragments are found, or more than one is found, the request will fail.
         * False by default.
         */
        @SerializedName("replace_all")
        public Boolean replaceAll;
    }

}