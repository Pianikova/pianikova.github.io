/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class GetProjectsMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_get_projects"; //$NON-NLS-1$

    // Example of input parameters (empty JSON object)
    @SuppressWarnings("nls")
    private static String QuestionExample = "{}";

    // Example of output structure
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n" + "  {\n" + "    \"name\": \"Управление торговлей 11.5\",\n"
            + "    \"absolute_path\": \"C:\\\\1C_Projects\\\\УТ115\",\n" + "    \"is_open\": true,\n"
            + "    \"exists\": true,\n" + "    \"is_current\": false,\n" + "    \"open_files\": [\n"
            + "      \"src/main/MainModule.bsl\",\n" + "      \"src/test/TestModule.bsl\"\n" + "    ],\n"
            + "    \"directories\": [\n" + "      \"src\",\n" + "      \"src/main\",\n" + "      \"src/test\",\n"
            + "      \"lib\"\n" + "    ]\n" + "  }\n" + "]";

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public GetProjectsMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        this.log = log;
        this.json = json;
        this.messageFactory = messageFactory;
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
        // Execute the operation asynchronously
        return CompletableFuture.supplyAsync(() -> {
            // Check cancellation status before starting
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            // Collect information about currently open files in all projects
            Map<IProject, Set<IPath>> projectOpenFiles = collectOpenFiles();

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var projects = root.getProjects();
            var response = new ArrayList<Project>();

            // Process each project in the workspace
            for (var project : projects)
            {
                // Check cancellation status periodically
                if (cancellationToken.isCanceled())
                {
                    return messageFactory.createError(this, call, "Operation was cancelled during execution.");
                }

                var projectInfo = new Project();
                response.add(projectInfo);

                // Basic project information
                projectInfo.name = project.getName();
                var location = project.getLocation();
                projectInfo.absolutePath = location != null ? location.toOSString() : null;
                projectInfo.isOpen = project.isOpen();
                projectInfo.exists = project.exists();

                // Check if project has open files
                Set<IPath> openFiles = projectOpenFiles.get(project);
                projectInfo.isCurrent = openFiles != null && !openFiles.isEmpty();

                // Store open file paths
                projectInfo.openFiles = new ArrayList<>();
                if (openFiles != null)
                {
                    for (IPath path : openFiles)
                    {
                        // Convert to relative path string
                        projectInfo.openFiles.add(path.toString());
                    }
                }

                // Collect directory structure if project is open
                if (project.isOpen() && project.exists())
                {
                    try
                    {
                        projectInfo.directories = new ArrayList<>();
                        collectDirectories(project, projectInfo.directories);
                    }
                    catch (CoreException error)
                    {
                        log.logError(error);
                        projectInfo.directories = new ArrayList<>();
                    }
                }
                else
                {
                    projectInfo.directories = new ArrayList<>();
                }
            }

            // Serialize and return the response
            var content = json.serialize(response);
            return messageFactory.createMessage(this, call, content);
        }).exceptionally(ex -> {
            // Handle exceptions from the async operation
            var cause = ex instanceof CompletionException ? ex.getCause() : ex;
            return messageFactory.createError(this, call, "Failed to get projects: " + cause.getMessage());
        });
    }

    /**
     * Collects information about open files across all projects
     *
     * @return Map containing projects and their open files (relative paths)
     */
    private Map<IProject, Set<IPath>> collectOpenFiles()
    {
        Map<IProject, Set<IPath>> projectOpenFiles = new HashMap<>();
        var workbench = PlatformUI.getWorkbench();

        // Iterate through all workbench windows
        for (var window : workbench.getWorkbenchWindows())
        {
            // Process each page in the window
            for (var page : window.getPages())
            {
                // Check all editor references
                for (var editorRef : page.getEditorReferences())
                {
                    try
                    {
                        // Get file associated with editor input
                        var input = editorRef.getEditorInput();
                        var file = input.getAdapter(IFile.class);
                        if (file != null && file.exists())
                        {
                            IProject project = file.getProject();
                            IPath relativePath = file.getProjectRelativePath();

                            // Store relative path per project
                            projectOpenFiles.computeIfAbsent(project, k -> new HashSet<>()).add(relativePath);
                        }
                    }
                    catch (PartInitException error)
                    {
                        log.logError(error);
                    }
                }
            }
        }
        return projectOpenFiles;
    }

    /**
     * Recursively collects all directories in a container
     *
     * @param container Root container to scan
     * @param directories List to accumulate directory paths
     */
    private void collectDirectories(IContainer container, List<String> directories) throws CoreException {
        for (var resource : container.members()) {
            if (resource instanceof IContainer && resource.exists()) {
                var dir = (IContainer) resource;
                // Get relative path as string
                var relativePath = dir.getProjectRelativePath().toString();
                directories.add(relativePath);
                // Recurse into subdirectories
                collectDirectories(dir, directories);
            }
        }
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        // Tool specification definition
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        // Detailed tool description
        var description = new StringBuilder();
        description.append("Provides comprehensive information about IDE projects including:");
        description.append("\n- Project name and absolute file system path");
        description.append("\n- Status indicators (exists, is open)");
        description.append("\n- 'is_current' flag indicating if project has open files");
        description.append("\n- List of currently open files (project-relative paths)");
        description.append("\n- Recursive list of all directories in the project");
        description.append("\n\nExample usage:");
        description.append("\n  Q: ").append(QuestionExample);
        description.append("\n  A: ").append(AnswerExample);
        spec.function.description = description.toString();

        // Input parameters (none required)
        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        parameters.properties = new HashMap<>();
        parameters.required = new ArrayList<>();

        spec.function.parameters = parameters;
        return spec;
    }

    /**
     * Data structure representing project information
     */
    private static class Project
    {
        @SerializedName("name")
        public String name;

        @SerializedName("absolute_path")
        public String absolutePath;

        @SerializedName("is_open")
        public Boolean isOpen;

        @SerializedName("exists")
        public Boolean exists;

        @SerializedName("is_current")
        public Boolean isCurrent;

        @SerializedName("open_files")
        public List<String> openFiles;

        @SerializedName("directories")
        public List<String> directories;
    }
}