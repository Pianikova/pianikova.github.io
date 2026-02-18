/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEditingSupport;
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

public class DeleteMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Delete"; //$NON-NLS-1$
    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"path\": \"C:/Projects/AccountingSystem/src/MainModule.bsl\"\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "File deleted: \"C:/Projects/AccountingSystem/src/MainModule.bsl\"";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;
    private final IProjectTools projectTools;
    private final IMarkdownUtils markdownUtils;
    private final IEditingSupport editingSupport;

    @Inject
    public DeleteMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem,
        IProjectTools projectTools, IMarkdownUtils markdownUtils, IEditingSupport editingSupport)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(editingSupport);

        this.json = json;
        this.messageFactory = messageFactory;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
        this.projectTools = projectTools;
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
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample
                + "\n\nRequired field: 'path' (string)");
        }

        var request = optionalRequest.get();
        var path = request.path;
        if (path == null || path.isBlank())
        {
            throw new ToolException("`path` is required.");
        }

        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();
            requestMarkdown
                .append(MessageFormat.format(Messages.DeleteTitleTemplate, markdownUtils.formatFilePath(path)));
            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

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

                    fileSystem.deleteFile(path);

                    var response = new StringBuilder();
                    response.append("File deleted: \"").append(path).append("\".\n");
                    response.append(
                        "⚠️ WARNING: File not part of project. Changes to non-project files may have irreversible consequences.\n");

                    var changes = markdownUtils.createStyledText("-1", TextColor.RED, FontWeight.BOLD);
                    details.responseMarkdown =
                        MessageFormat.format(Messages.DeletedTemplate, markdownUtils.formatFilePath(path), changes);

                    return messageFactory.createMessage(this, call, response.toString(), details);
            }
                catch (IOException error)
            {
                    throw new ToolException("Failed to delete file", error, ToolErrorType.RETRYABLE);
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
                    throw new ToolException("Cannot open the project \"" + finalProjectName + "\"", error,
                        ToolErrorType.RETRYABLE);
            }
            }

            var projectFile = projectTools.getProjectFile(project, path);
            if (!projectFile.isPresent())
            {
                throw new ToolException("The file \"" + path + "\" does not exist within the IDE project context. "
                    + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope.");
            }

            // Check if the file can be deleted using editingSupport
            if (!editingSupport.canDelete(projectFile.get()))
            {
                var filePathForError = projectFile.map(f -> f.getProjectRelativePath().toOSString()).orElse(path);
                throw new ToolException("The file \"" + filePathForError
                    + "\" cannot be deleted. Deletion is not supported for this file type or the file is locked.");
            }

            var actualFile = projectFile.get();

            try
            {
                var monitor = cancellationProgressMonitor.get();
                monitor.setCancellationToken(cancellationToken);

                // Get the path for response before deletion
                var displayPath = actualFile.getProjectRelativePath().toPortableString();

                // Delete the file
                actualFile.delete(true, monitor);

                // Refresh the parent folder
                if (actualFile.getParent() != null)
            {
                    actualFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
            }

                var response = new StringBuilder();
                response.append("File deleted: \"").append(displayPath).append("\".\n");
                response.append("ACTION REQUIRED: verify project errors and warnings. Use `"
                    + GetMarkersMcpTool.TOOL_NAME + "` tool.");

                // Add response markdown
                var changes = new StringBuilder();
                changes.append(markdownUtils.createStyledText(Messages.Deleted, TextColor.RED, FontWeight.BOLD));
                details.responseMarkdown =
                    MessageFormat.format(Messages.DeletedTemplate, markdownUtils.formatFilePath(path), changes);

                return messageFactory.createMessage(this, call, response.toString(), details);
            }
            catch (CoreException error)
            {
                throw new ToolException("Failed to delete file", error, ToolErrorType.RETRYABLE);
            }
        }).handle((result, error) -> {
            if (error != null)
            {
                Throwable cause = error;
                if (cause instanceof ToolException)
                {
                    return messageFactory.createError(this, call, ((ToolException)cause).getMessage());
                }
                return messageFactory.createError(this, call, "Unexpected error: " + cause.getMessage());
            }
            return result;
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
        description.append("Deletes a project file.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Verify the file exists before deletion.");
        description.append("\n- This operation cannot be undone.");
        description.append("\n- Review file content and impact before deleting.");
        description.append("\n\nRelated tools:");
        description.append("\n- Read file: `" + ReadMcpTool.TOOL_NAME + "`.");
        description.append("\n- Review history: `" + GitDiffMcpTool.TOOL_NAME + "`, `" + LocalHistoryMcpTool.TOOL_NAME + "`.");
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

        parameters.properties = properties;
        parameters.required = Arrays.asList("path");
        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    private static class Request
    {
        /**
         * Absolute path to the file to delete. Required.
         */
        @SerializedName("path")
        public String path;
    }
}
