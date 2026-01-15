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
        + "  \"origin_content\": \"Сообщить(\\\"Привет, мир!\\\");\",\n"
        + "  \"new_content\": \"Сообщить(\\\"Hello, world!\\\");\",\n"
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

    @Inject
    public EditMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
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

        var originContent = request.originContent;
        if (originContent == null || originContent.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "`origin_content` must not be empty."));
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
            var optionalFileContent = contentSourceProvider.getFileContent(projectFile);
            if (optionalFileContent.isEmpty())
            {
                return messageFactory.createError(this, call,
                    "The file \"" + relativeFilePath + "\" does not exist. Use the `" + WriteMcpTool.TOOL_NAME
                        + "` tool to create a new file.");
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
                    java.util.regex.Matcher.quoteReplacement(originContent),
                    java.util.regex.Matcher.quoteReplacement(newContent));

                if (updatedContent.equals(currentContent))
                {
                    return messageFactory.createError(this, call,
                        "Original content not found in file. Verify the `origin_content`.");
                }
            }
            else
            {
                int firstIndex = currentContent.indexOf(originContent);
                if (firstIndex == -1)
                {
                    return messageFactory.createError(this, call,
                        "Original content not found in file. Verify the `origin_content`.");
                }

                int secondIndex = currentContent.indexOf(originContent, firstIndex + originContent.length());
                if (secondIndex != -1)
                {
                    return messageFactory.createError(this, call,
                        "Multiple matches found for original content. Change the `origin_content` to avoid multiple matches.");
                }

                updatedContent = currentContent.substring(0, firstIndex) + newContent
                    + currentContent.substring(firstIndex + originContent.length());
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

            var response = new StringBuilder();
            var projectRelativePath = projectFile.getProjectRelativePath();
            response.append("File updated: \"").append(projectRelativePath.toPortableString()).append("\".\n");
            response.append("ACTION REQUIRED: verify project errors and warnings. Use `"
                + GetErrorsMcpTool.TOOL_NAME + "` tool.");
            return messageFactory.createMessage(this, call, response.toString());
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
        description.append("Performs exact string replacements in project files.");
        description.append("\n\nUsage:");
        description.append("\n- You must use your `" + ReadMcpTool.TOOL_NAME + "` tool at least once in the conversation before editing. This tool will error if you attempt an edit without reading the file.");
        description.append("\n- When editing text from `" + ReadMcpTool.TOOL_NAME + "` tool output, ensure you preserve the exact indentation (tabs/spaces).");
        description.append("\n- ALWAYS prefer editing existing files in the codebase. NEVER write new files unless explicitly required.");
        description.append("\n- Only use emojis if the user explicitly requests it. Avoid adding emojis to files unless asked.");
        description.append("\n- The edit will FAIL if `origin_content` is not unique in the file. ");
        description.append("Either provide a larger string with more surrounding context to make it unique or use `replace_all` to change every instance of `origin_content`.");
        description.append("\n- Use `replace_all` for replacing and renaming strings across the file. This parameter is useful if you want to rename a variable for instance.");

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

        var originContentProp = new McpToolCallProperty();
        originContentProp.type = "string";
        originContentProp.description = "The fragment of the file content that will be replaced.";
        properties.put("origin_content", originContentProp);

        var newContentProp = new McpToolCallProperty();
        newContentProp.type = "string";
        newContentProp.description = "The content fragment that will replace the original (`origin_content`).";
        properties.put("new_content", newContentProp);

        var replaceAllProp = new McpToolCallProperty();
        replaceAllProp.type = "boolean";
        replaceAllProp.description = "If true, all occurrences of the `origin_content` fragment will be replaced. "
            + "If false, only the single occurrence will be replaced. "
            + "If no fragments are found, or more than one is found, the request will fail. False by default.";
        properties.put("replace_all", replaceAllProp); // Исправлено: newContentProp -> replaceAllProp

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
         * Relative path to the file which content should be replaced. Must start with the project name, for example, "src/MyModule.bsl".
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        /**
         * The fragment of the file content that will be replaced.
         */
        @SerializedName("origin_content")
        public String originContent;

        /**
         * The content fragment that will replace the original (`origin_content`).
         */
        @SerializedName("new_content")
        public String newContent;

        /**
         * If true, all occurrences of the `origin_content` fragment will be replaced.
         * If false, only the single occurrence will be replaced. If no fragments are found, or more than one is found, the request will fail.
         * False by default.
         */
        @SerializedName("replace_all")
        public Boolean replaceAll;
    }

}