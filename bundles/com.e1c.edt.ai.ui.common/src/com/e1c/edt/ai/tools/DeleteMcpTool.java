/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
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
        + "  \"project_name\": \"AccountingSystem\",\n"
        + "  \"relative_file_path\": \"src/MainModule.bsl\"\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "File deleted: \"src/MainModule.bsl\"";
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
                + "\n\nRequired field: 'project_name' (string), 'relative_file_path' (string)"));
        }

        var request = optionalRequest.get();
        var relativeFilePath = request.relativeFilePath;
        if (relativeFilePath == null || relativeFilePath.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "`relative_file_path` is required."));
        }

        var fileName = new File(relativeFilePath).getName();
        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.DeleteTitleTemplate, fileName));
            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var projectName = request.projectName;
        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "`project_name` is required."));
        }

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

            // Check if the file can be deleted using editingSupport
            if (!editingSupport.canDelete(projectFile))
            {
                return messageFactory.createError(this, call, "The file \"" + relativeFilePath
                    + "\" cannot be deleted. Deletion is not supported for this file type or the file is locked.");
            }

            if (!projectFile.exists())
            {
                return messageFactory.createError(this, call,
                    "The file \"" + relativeFilePath + "\" does not exist.");
            }

            try
            {
                var monitor = cancellationProgressMonitor.get();
                monitor.setCancellationToken(cancellationToken);

                // Get the relative path for response before deletion
                var projectRelativePath = projectFile.getProjectRelativePath().toPortableString();

                // Delete the file
                projectFile.delete(true, monitor);

                // Refresh the parent folder
                if (projectFile.getParent() != null)
                {
                    projectFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
                }

                var response = new StringBuilder();
                response.append("File deleted: \"").append(projectRelativePath).append("\".\n");
                response.append(
                    "ACTION REQUIRED: verify project errors and warnings. Use `" + GetMarkersMcpTool.TOOL_NAME + "` tool.");

                // Add response markdown
                var changes = new StringBuilder();
                changes.append(markdownUtils.createStyledText(Messages.Deleted, TextColor.RED, FontWeight.BOLD));
                details.responseMarkdown = MessageFormat.format(Messages.DeletedTemplate, fileName, changes);

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
        description.append("\n- Verify the file exists before deletion.");
        description.append("\n- This operation cannot be undone.");
        description.append("\n- Review file content and impact before deleting.");
        description.append("\n\nRelated tools:");
        description.append("\n- Read file: `" + ReadMcpTool.TOOL_NAME + "`.");
        description.append("\n- Locate files: `" + SearchFilesMcpTool.TOOL_NAME + "`.");
        description.append("\n- Review history: `" + GitDiffMcpTool.TOOL_NAME + "`, `" + LocalHistoryMcpTool.TOOL_NAME + "`.");
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
         * Relative path to the file to delete.
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;
    }
}
