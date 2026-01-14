/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
public class WriteFileMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_write_file"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"AccountingSystem\",\n"
        + "  \"relative_file_path\": \"src/MainModule.bsl\",\n"
        + "  \"contents\": \"Процедура Тест()\\n    Сообщить(\\\"Привет, мир!\\\");\\nКонецПроцедуры\"\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "File written: \"src/MainModule.bsl\"";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IProgressMonitor monitor;
    private final IFileSystem fileSystem;

    @Inject
    public WriteFileMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IProgressMonitor monitor, IFileSystem fileSystem)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(monitor);
        Preconditions.checkNotNull(fileSystem);

        this.json = json;
        this.messageFactory = messageFactory;
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

        var contents = request.contents;
        if (contents == null)
        {
            return CompletableFuture.completedFuture(messageFactory.createError(this, call, "'contents' is required."));
        }

        var charsetName =
            request.charsetName != null && !request.charsetName.isBlank() ? request.charsetName : "UTF-8";
        byte[] data;
        try
        {
            data = contents.getBytes(charsetName);
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
            if (projectFile.exists())
            {
                return messageFactory.createError(this, call,
                    "The file \"" + relativeFilePath + "\" already exists. Use the '" + EditFileMcpTool.TOOL_NAME
                        + "' tool to modify this file.");
            }

            try
            {
                createParentFolders(projectFile, monitor);
                try (ByteArrayInputStream source = new ByteArrayInputStream(data))
                {
                    projectFile.create(source, true, monitor);
                    projectFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
                    if (projectFile.getParent() != null)
                    {
                        projectFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
                    }
                }
            }
            catch (CoreException | IOException error)
            {
                return messageFactory.createError(this, call, "Failed to write file. " + error.getMessage());
            }

            var response = new StringBuilder();
            var projectRelativePath = projectFile.getProjectRelativePath();
            response.append("File written: \"").append(projectRelativePath.toPortableString()).append("\".\n");

            var fileExt = projectFile.getFileExtension();
            if (fileExt != null)
            {
                fileExt = fileExt.toLowerCase();
                switch (fileExt)
                {
                case "bsl":
                    response.append("ACTION REQUIRED: check that corresponding \"")
                        .append(projectRelativePath.removeFileExtension().addFileExtension("mdo").toPortableString())
                        .append("\" file exists or create it.\n");
                    break;
                case "mdo":
                case "form":
                    response.append(
                        "ACTION REQUIRED: verify that the file \"src/Configuration/Configuration.mdo\" has been updated with the new configuration item. Use '"
                            + EditFileMcpTool.TOOL_NAME + "' tool.");
                    break;
                }
            }

            return messageFactory.createMessage(this, call, response.toString());
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
        description.append("Writes the contents of a project file. Creates a new file.");
        description.append("\nIMPORTANT: analyze the project structure: directories, other files before writing a file.");
        description.append("\nNOTE: some files require additional files to be processed correctly. For example, .bsl files require an .mdo file in the corresponding directory.");
        description.append("\nNOTE: To edit or update an existing file, use the '" + EditFileMcpTool.TOOL_NAME + "' tool.");
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

        var contentsProp = new McpToolCallProperty();
        contentsProp.type = "string";
        contentsProp.description = "Contents to write to file.";
        properties.put("contents", contentsProp);

        var charsetNameProp = new McpToolCallProperty();
        charsetNameProp.type = "string";
        charsetNameProp.description = "File encoding, for example, \"UTF-8\", \"windows-1251\", \"KOI8-R\", \"UTF-16\", \"UTF-32\", etc. By default, \"UTF-8\".";
        properties.put("charset_name", charsetNameProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "relative_file_path", "contents");

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
         * Relative path to the file. Must start with the project name, for example, "src/MyModule.bsl".
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        /**
         * Contents to write to file.
         */
        @SerializedName("contents")
        public String contents;

        /**
         * File encoding, for example, "UTF-8", "windows-1251", "KOI8-R", "UTF-16", "UTF-32", etc. By default, UTF-8.
         */
        @SerializedName("charset_name")
        public String charsetName;
    }

}