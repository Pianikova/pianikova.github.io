/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
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
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
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
    private final IMarkdownUtils markdownUtils;
    private final IEditingSupport editingSupport;

    @Inject
    public DeleteMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem,
        IMarkdownUtils markdownUtils, IEditingSupport editingSupport)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(editingSupport);

        this.json = json;
        this.messageFactory = messageFactory;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
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
            return CompletableFuture.completedFuture(messageFactory.createError(this, call,
                "Cannot deserialize arguments. Use this example: " + QuestionExample
                    + "\n\nRequired field: 'path' (string)"));
        }

        var request = optionalRequest.get();
        var path = request.path;
        if (path == null || path.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "`path` is required."));
        }

        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.DeleteTitleTemplate, markdownUtils.formatFilePath(path)));
            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

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
                        return messageFactory.createError(this, call, "The file \"" + path + "\" does not exist.");
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
                    return messageFactory.createError(this, call, "Failed to delete file. " + error.getMessage());
                }
            }

            // File is part of a project - use Eclipse APIs
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(finalProjectName);
            if (project == null || !project.exists())
            {
                return messageFactory.createError(this, call,
                    "The project \"" + finalProjectName + "\" does not exist.");
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

            // Check if the file can be deleted using editingSupport
            if (!editingSupport.canDelete(projectFile))
            {
                var filePathForError = projectFile.getProjectRelativePath().toOSString();
                return messageFactory.createError(this, call, "The file \"" + filePathForError
                    + "\" cannot be deleted. Deletion is not supported for this file type or the file is locked.");
            }

            if (!projectFile.exists())
            {
                var filePathForError = projectFile.getProjectRelativePath().toOSString();
                return messageFactory.createError(this, call,
                    "The file \"" + filePathForError + "\" does not exist within the IDE project context. "
                        + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope.");
            }

            try
            {
                var monitor = cancellationProgressMonitor.get();
                monitor.setCancellationToken(cancellationToken);

                // Get the path for response before deletion
                var displayPath = projectFile.getProjectRelativePath().toPortableString();

                // Delete the file
                projectFile.delete(true, monitor);

                // Refresh the parent folder
                if (projectFile.getParent() != null)
                {
                    projectFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
                }

                var response = new StringBuilder();
                response.append("File deleted: \"").append(displayPath).append("\".\n");
                response.append(
                    "ACTION REQUIRED: verify project errors and warnings. Use `" + GetMarkersMcpTool.TOOL_NAME + "` tool.");

                // Add response markdown
                var changes = new StringBuilder();
                changes.append(markdownUtils.createStyledText(Messages.Deleted, TextColor.RED, FontWeight.BOLD));
                details.responseMarkdown =
                    MessageFormat.format(Messages.DeletedTemplate, markdownUtils.formatFilePath(path), changes);

                return messageFactory.createMessage(this, call, response.toString(), details);
            }
            catch (CoreException error)
            {
                return messageFactory.createError(this, call, "Failed to delete file: " + error.getMessage());
            }
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
