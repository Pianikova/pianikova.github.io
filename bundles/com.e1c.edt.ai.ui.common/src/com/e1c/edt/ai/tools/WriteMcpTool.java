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
    private final IMarkdownUtils markdownUtils;
    private final IEditingSupport editingSupport;

    @Inject
    public WriteMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
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
                "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        var path = request.path;
        if (path == null || path.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "`path` is required."));
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
                    .append(markdownUtils.formatFilePath(path))
                    .append("</summary>\n\n");
                requestMarkdown.append(markdownUtils.buildGitDiff(path, null, request.content));
                requestMarkdown.append("\n</details>");
            }

            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var content = request.content;
        if (content == null)
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call, "`content` is required."));
        }

        var charsetName = request.charsetName != null && !request.charsetName.isBlank() ? request.charsetName : "UTF-8";
        byte[] data;
        try
        {
            data = content.getBytes(charsetName);
        }
        catch (UnsupportedEncodingException error)
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call,
                "Unsupported charset: \"" + charsetName + "\". " + error.getMessage()));
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
                    if (fileSystem.fileExists(path))
                    {
                        return messageFactory.createError(this, call,
                            "The file \"" + path + "\" already exists. Use the `" + EditMcpTool.TOOL_NAME
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
                    return messageFactory.createError(this, call, "Failed to write file. " + error.getMessage());
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
                    return messageFactory.createError(this, call,
                        "Cannot open the project \"" + finalProjectName + "\". " + error.getMessage());
                }
            }

            var projectFile = fileSystem.getProjectFile(project, path);

            // Check if the file can be edited using editingSupport
            if (!editingSupport.canEdit(projectFile.orElse(null)))
            {
                var filePathForError = projectFile.map(f -> f.getProjectRelativePath().toOSString()).orElse(path);
                return messageFactory.createError(this, call, "The file \"" + filePathForError
                    + "\" cannot be created. Writing is not supported for this file type or the location is restricted.");
            }

            if (projectFile.isPresent() && projectFile.get().exists())
            {
                var filePathForError = projectFile.map(f -> f.getProjectRelativePath().toOSString()).orElse(path);
                return messageFactory.createError(this, call, "The file \"" + filePathForError
                    + "\" already exists. Use the `" + EditMcpTool.TOOL_NAME + "` tool to modify this file.");
            }

            // Since the file doesn't exist (or is not in the project), we need to create it
            // Re-get the file to ensure we have a proper IFile reference for creation
            var actualFile = projectFile.orElse(project.getFile(new org.eclipse.core.runtime.Path(path)));

            try
            {
                createParentFolders(actualFile, monitor);
                try (ByteArrayInputStream source = new ByteArrayInputStream(data))
                {
                    actualFile.create(source, true, monitor);
                    actualFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
                    if (actualFile.getParent() != null)
                    {
                        actualFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
                    }
                }
            }
            catch (CoreException | IOException error)
            {
                return messageFactory.createError(this, call, "Failed to write file. " + error.getMessage());
            }

            var response = new StringBuilder();
            var displayPath = actualFile.getProjectRelativePath().toPortableString();
            response.append("File written: \"").append(displayPath).append("\".\n");

            var fileExt = actualFile.getFileExtension();
            if (fileExt != null)
            {
                fileExt = fileExt.toLowerCase();
                switch (fileExt)
                {
                case "bsl":
                    response.append("ACTION REQUIRED: check that corresponding \"")
                        .append(actualFile.getProjectRelativePath().removeFileExtension().addFileExtension("mdo").toPortableString())
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

            response.append(
                "ACTION REQUIRED: verify project errors and warnings. Use `" + GetMarkersMcpTool.TOOL_NAME + "` tool.");

            // Add response markdown with content details
            var newLines = content.split("\\r?\\n", -1).length;
            var changes = new StringBuilder();
            changes.append(markdownUtils.createStyledText("+" + newLines, TextColor.GREEN, FontWeight.BOLD));

            var responseMarkdown = new StringBuilder();
            responseMarkdown
                .append(MessageFormat.format(Messages.WrittenTemplate, markdownUtils.formatFilePath(path), changes));
            responseMarkdown.append("\n\n");
            responseMarkdown.append("<details><summary>")
                .append(Messages.WriteDetailsSummary)
                .append("</summary>\n\n");
            responseMarkdown.append(markdownUtils.buildGitDiff(path, null, content));
            responseMarkdown.append("\n</details>");
            details.responseMarkdown = responseMarkdown.toString();

            return messageFactory.createMessage(this, call, response.toString(), details);
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
        description.append("\n- Fails if the file already exists; use `" + EditMcpTool.TOOL_NAME + "` to modify files.");
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


