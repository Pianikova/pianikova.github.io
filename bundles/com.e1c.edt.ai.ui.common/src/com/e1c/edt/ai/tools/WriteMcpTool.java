/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
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

public class WriteMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Write"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"path\": \"C:/Projects/AccountingSystem/src/MainModule.bsl\",\n"
        + "  \"content\": \"Procedure Test()\\n    Message(\\\"Hello\\\");\\nEndProcedure\"\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "File written: \"C:/Projects/AccountingSystem/src/MainModule.bsl\"";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;
    private final IProjectTools projectTools;
    private final IMarkdownUtils markdownUtils;
    private final IEditingSupport editingSupport;
    private final ILog log;

    @Inject
    public WriteMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem,
        IProjectTools projectTools, IMarkdownUtils markdownUtils, IEditingSupport editingSupport, ILog log)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(editingSupport);
        Preconditions.checkNotNull(log);

        this.json = json;
        this.messageFactory = messageFactory;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
        this.projectTools = projectTools;
        this.markdownUtils = markdownUtils;
        this.editingSupport = editingSupport;
        this.log = log;

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
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
        }

        var request = optionalRequest.get();
        var path = request.path;
        if (path == null || path.isBlank())
        {
            throw new ToolException("`path` is required.");
        }

        var content = request.content;
        if (content == null)
        {
            throw new ToolException("`content` is required.");
        }

        var charsetName = request.charsetName != null && !request.charsetName.isBlank() ? request.charsetName : "UTF-8";
        byte[] data;
        try
        {
            data = content.getBytes(charsetName);
        }
        catch (UnsupportedEncodingException error)
        {
            throw new ToolException("Unsupported charset: \"" + charsetName + "\"", error, ToolErrorType.RETRYABLE);
        }

        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.WriteTitleTemplate, markdownUtils.formatFilePath(path)));

            // Add content details
            if (request.content != null)
            {
                requestMarkdown.append("\n\n");
                requestMarkdown.append("<details><summary>")
                    .append(Messages.WriteDetailsSummary)
                    .append("</summary>\n\n");
                requestMarkdown.append(markdownUtils.buildGitDiff(path, null, request.content));
                requestMarkdown.append("\n</details>");
            }

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
            if (isProjectFile)
            {
                // File is part of a project - use Eclipse APIs
                var root = ResourcesPlugin.getWorkspace().getRoot();
                var project = root.getProject(finalProjectName);
                if (project == null || !project.exists())
                {
                    throw new ToolException("The project \"" + finalProjectName + "\" does not exist.");
                }

                var monitor = cancellationProgressMonitor.get();
                monitor.setCancellationToken(cancellationToken);
                if (!project.isOpen())
                {
                    try
                    {
                        project.open(monitor);
                    }
                    catch (CoreException error)
                    {
                        throw new ToolException("Cannot open the project \"" + finalProjectName + "\"", error,
                            ToolErrorType.RETRYABLE);
                    }
                }

                var optionalProjectFile = projectTools.getProjectFile(project, path);
                if (optionalProjectFile.isPresent())
                {
                    var projectFile = optionalProjectFile.get();
                    if (projectFile.exists())
                    {
                        // Check if file is empty
                        try
                        {
                            if (projectFile.getLocation() != null && projectFile.getLocation().toFile().length() > 0)
                            {
                                throw new ToolException("The file \"" + path + "\" already exists and is not empty. Use the `"
                                    + EditMcpTool.TOOL_NAME + "` tool to modify this file.");
                            }
                        }
                        catch (Exception error)
                        {
                            throw new ToolException("The file \"" + path + "\" already exists. Use the `"
                                + EditMcpTool.TOOL_NAME + "` tool to modify this file.");
                        }
                    }

                    // Check if the file can be edited using editingSupport
                    if (!editingSupport.canEdit(projectFile))
                    {
                        throw new ToolException("The file \"" + path
                            + "\" cannot be created. Writing is not supported for this file type or the location is restricted.");
                    }

                    try
                    {
                        createParentFolders(projectFile, monitor);
                        try (ByteArrayInputStream source = new ByteArrayInputStream(data))
                        {
                            if (projectFile.exists())
                            {
                                // File exists and is empty - use setContents to overwrite
                                projectFile.setContents(source, IResource.FORCE, monitor);
                            }
                            else
                            {
                                // File doesn't exist - use create
                                projectFile.create(source, true, monitor);
                            }

                            refreshResources(projectFile, monitor);
                        }
                    }
                    catch (CoreException | IOException error)
                    {
                        // Ensure resources are refreshed even on error
                        refreshResourcesSafe(projectFile, monitor);
                        throw new ToolException("Failed to write file", error, ToolErrorType.RETRYABLE);
                    }

                    var response = new StringBuilder();
                    response.append("File written: \"").append(path).append("\".\n");

                    var fileExt = projectFile.getFileExtension();
                    if (fileExt != null)
                    {
                        fileExt = fileExt.toLowerCase();
                        switch (fileExt)
                        {
                        case "bsl":
                            response.append("ACTION REQUIRED: check that corresponding \"")
                                .append(projectFile.getProjectRelativePath()
                                    .removeFileExtension()
                                    .addFileExtension("mdo")
                                    .toPortableString())
                                .append("\" file exists or create it.\n");
                            break;
                        case "mdo":
                        case "form":
                            response.append(
                                "ACTION REQUIRED: verify that the file \"src/Configuration/Configuration.mdo\" has been updated with the new configuration item. Use `"
                                    + EditMcpTool.TOOL_NAME + "` tool.");
                            break;
                        }
                    }

                    response.append("ACTION REQUIRED: verify project errors and warnings. Use `"
                        + GetMarkersMcpTool.TOOL_NAME + "` tool.");

                    // Add response markdown with content details
                    var newLines = content.split("\\r?\\n", -1).length;
                    var changes = new StringBuilder();
                    changes.append(markdownUtils.createStyledText("+" + newLines, TextColor.GREEN, FontWeight.BOLD));

                    var responseMarkdown = new StringBuilder();
                    responseMarkdown.append(
                        MessageFormat.format(Messages.WrittenTemplate, markdownUtils.formatFilePath(path), changes));
                    responseMarkdown.append("\n\n");
                    responseMarkdown.append("<details><summary>")
                        .append(Messages.WriteDetailsSummary)
                        .append("</summary>\n\n");
                    responseMarkdown.append(markdownUtils.buildGitDiff(path, null, content));
                    responseMarkdown.append("\n</details>");
                    details.responseMarkdown = responseMarkdown.toString();

                    return messageFactory.createMessage(this, call, response.toString(), details);
                }
            }

            // File is not part of any project - use Java file I/O
            try
            {
                if (fileSystem.fileExists(path) && !fileSystem.isFileEmpty(path))
                {
                    throw new ToolException("The file \"" + path + "\" already exists and is not empty. Use the `"
                        + EditMcpTool.TOOL_NAME
                        + "` tool to modify this file.");
                }

                fileSystem.writeAllBytes(path, data);

                var response = new StringBuilder();
                response.append("File written: \"").append(path).append("\".\n");
                response.append("⚠️ WARNING: File not part of project. Changes to non-project files may have irreversible consequences.\n");

                // Add response markdown with content details
                var newLines = content.split("\\r?\\n", -1).length;
                var changes = new StringBuilder();
                changes.append(markdownUtils.createStyledText("+" + newLines, TextColor.GREEN, FontWeight.BOLD));

                var responseMarkdown = new StringBuilder();
                responseMarkdown.append(
                    MessageFormat.format(Messages.WrittenTemplate, markdownUtils.formatFilePath(path), changes));
                responseMarkdown.append("\n\n");
                responseMarkdown.append("<details><summary>")
                    .append(Messages.WriteDetailsSummary)
                    .append("</summary>\n\n");
                responseMarkdown.append(markdownUtils.buildGitDiff(path, null, content));
                responseMarkdown.append("\n</details>");
                details.responseMarkdown = responseMarkdown.toString();

                return messageFactory.createMessage(this, call, response.toString(), details);
            }
            catch (IOException error)
            {
                throw new ToolException("Failed to write file", error, ToolErrorType.RETRYABLE);
            }
        });
    }

    private void createParentFolders(IFile file, IProgressMonitor monitor) throws CoreException
    {
        IContainer container = file.getParent();
        if (container instanceof IFolder && !container.exists())
        {
            createFolderRecursive((IFolder)container, monitor);
        }
    }

    private void createFolderRecursive(IFolder folder, IProgressMonitor monitor) throws CoreException
    {
        if (folder == null || folder.exists())
        {
            return;
        }

        IContainer parent = folder.getParent();
        if (parent instanceof IFolder)
        {
            createFolderRecursive((IFolder)parent, monitor);
        }

        if (!folder.exists())
        {
            folder.create(true, true, monitor);
        }
    }

    /**
     * Refreshes the file and its parent folder in the workspace.
     *
     * @param file the file to refresh
     * @param monitor the progress monitor
     * @throws CoreException if refresh fails
     */
    private void refreshResources(IFile file, IProgressMonitor monitor) throws CoreException
    {
        file.refreshLocal(IResource.DEPTH_ZERO, monitor);
        if (file.getParent() != null)
        {
            file.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
        }
    }

    /**
     * Safely refreshes the file and its parent folder. Errors are logged but not thrown.
     *
     * @param file the file to refresh
     * @param monitor the progress monitor
     */
    private void refreshResourcesSafe(IFile file, IProgressMonitor monitor)
    {
        try
        {
            refreshResources(file, monitor);
        }
        catch (CoreException error)
        {
            log.logError(error);
        }
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
        description.append("Creates a new project file.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Fails if the file already exists and is not empty; empty files can be overwritten. Use `" + EditMcpTool.TOOL_NAME + "` to modify existing non-empty files.");
        description.append("\n- Verify the target folder and naming patterns before creating files.");
        description.append("\n- Some file types require companions (e.g., .bsl needs a matching .mdo).");
        description.append("\n- Avoid creating docs (*.md/README) unless the user explicitly asks.");
        description.append("\n- Avoid emojis unless explicitly requested.");
        description.append("\n\nRelated tools:");
        description.append("\n- Check existence and context: `" + ReadMcpTool.TOOL_NAME + "`.");
        description.append("\n- Update existing files: `" + EditMcpTool.TOOL_NAME + "`.");
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

        var contentProp = new McpToolCallProperty();
        contentProp.type = "string";
        contentProp.description = "Content to write to file.";
        properties.put("content", contentProp);

        var charsetNameProp = new McpToolCallProperty();
        charsetNameProp.type = "string";
        charsetNameProp.description = "File encoding, for example, \"UTF-8\", \"windows-1251\", \"KOI8-R\", \"UTF-16\", \"UTF-32\", etc. By default, \"UTF-8\".";
        properties.put("charset_name", charsetNameProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("path", "content");

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
         * Content to write to file.
         */
        @SerializedName("content")
        public String content;

        /**
         * File encoding, for example, "UTF-8", "windows-1251", "KOI8-R", "UTF-16", "UTF-32", etc. By default, UTF-8.
         */
        @SerializedName("charset_name")
        public String charsetName;
    }
}


