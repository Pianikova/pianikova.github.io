/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
public class GetProjectsMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_get_projects"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample = "{}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n" +
        "  {\n" +
        "    \"name\": \"Управление торговлей 11.5\",\n" +
        "    \"absolute_path\": \"C:\\\\1C_Projects\\\\УТ115\",\n" +
        "    \"is_open\": true,\n" +
        "    \"exists\": true,\n" +
        "    \"is_current\": false,\n" +
        "    \"description\": \"Project for developing Trade Management configuration\",\n" +
        "    \"session_id\": \"session-12345\",\n" +
        "    \"build_commands\": [\"org.eclipse.xtext.ui.shared.xtextBuilder\"],\n" +
        "    \"natures\": [\"com._1c.g5.v8.dt.core.V8ConfigurationNature\"],\n" +
        "    \"open_files\": [\n" +
        "      {\n" +
        "        \"relative_file_path\": \"src/main/MainModule.bsl\",\n" +
        "        \"cursor_line\": 10,\n" +
        "        \"cursor_line_offset\": 5\n" +
        "      },\n" +
        "      {\n" +
        "        \"relative_file_path\": \"src/test/TestModule.bsl\",\n" +
        "        \"cursor_line\": 20,\n" +
        "        \"cursor_line_offset\": 3,\n" +
        "        \"selection_start_line\": 15,\n" +
        "        \"selection_start_line_offset\": 10,\n" +
        "        \"selection_end_line\": 20,\n" +
        "        \"selection_end_line_offset\": 3\n" +
        "      }\n" +
        "    ],\n" +
        "    \"directories\": [\n" +
        "      \"src\",\n" +
        "      \"src/main\",\n" +
        "      \"src/test\",\n" +
        "      \"lib\"\n" +
        "    ]\n" +
        "  }\n" +
        "]";
    // @formatter:on

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final ISessionService sessionService;

    @Inject
    public GetProjectsMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory,
        ISessionService sessionService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(sessionService);

        this.log = log;
        this.json = json;
        this.messageFactory = messageFactory;
        this.sessionService = sessionService;

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
            var projectOpenFiles = collectOpenFiles();
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

                var sessionFeature = sessionService.getSessionAsync(new ProjectId(project));
                var projectInfo = new Project();
                response.add(projectInfo);
                projectInfo.name = project.getName();
                var location = project.getLocation();
                projectInfo.absolutePath = location != null ? location.toOSString() : null;
                projectInfo.isOpen = project.isOpen();
                projectInfo.exists = project.exists();
                var openFilesList = projectOpenFiles.get(project);
                projectInfo.buildCommands = new ArrayList<>();
                projectInfo.natures = new ArrayList<>();
                projectInfo.openFiles = new ArrayList<>();
                projectInfo.directories = new ArrayList<>();
                // Read metadata only for open and existing projects
                if (project.isOpen() && project.exists())
                {
                    // Read project metadata from .project file
                    readProjectMetadata(project, projectInfo);
                    // Collect open files
                    if (openFilesList != null)
                    {
                        projectInfo.openFiles.addAll(openFilesList);
                    }
                    // Collect directory structure
                    try
                    {
                        collectDirectories(project, projectInfo.directories);
                    }
                    catch (CoreException error)
                    {
                        log.logError(error);
                    }
                }

                // Set current project flag based on open files
                projectInfo.isCurrent = !projectInfo.openFiles.isEmpty();
                try
                {
                    var optionalSession = sessionFeature.get();
                    optionalSession.ifPresent(session -> projectInfo.sessionId = session.sessionId);
                }
                catch (InterruptedException | ExecutionException error)
                {
                    log.logError(error);
                }
            }

            // Serialize and return the response
            var content = json.serialize(response);
            return messageFactory.createMessage(this, call, content);
        });
    }

    /**
     * Collects information about open files across all projects
     *
     * @return Map containing projects and their open files information
     */
    private Map<IProject, List<OpenFileInfo>> collectOpenFiles()
    {
        var projectOpenFiles = new HashMap<IProject, List<OpenFileInfo>>();
        var workbench = PlatformUI.getWorkbench();
        for (var window : workbench.getWorkbenchWindows())
        {
            for (var page : window.getPages())
            {
                for (var editorRef : page.getEditorReferences())
                {
                    try
                    {
                        var input = editorRef.getEditorInput();
                        var file = input.getAdapter(IFile.class);
                        if (file == null || !file.exists())
                        {
                            continue;
                        }
                        var editor = editorRef.getEditor(false);
                        if (editor == null)
                        {
                            continue;
                        }
                        var project = file.getProject();
                        var info = new OpenFileInfo();
                        info.relativeFilePath = file.getProjectRelativePath().toString();
                        // Process text editors only
                        if (editor instanceof ITextEditor)
                        {
                            var textEditor = (ITextEditor)editor;
                            var provider = textEditor.getDocumentProvider();
                            var document = provider.getDocument(input);
                            var selection = (ITextSelection)textEditor.getSelectionProvider().getSelection();
                            if (selection != null)
                            {
                                int offset = selection.getOffset();
                                int length = selection.getLength();
                                try
                                {
                                    // Calculate cursor position
                                    int cursorLine = document.getLineOfOffset(offset) + 1;
                                    int cursorLineOffset = offset - document.getLineOffset(cursorLine - 1);
                                    info.cursorLine = cursorLine;
                                    info.cursorLineOffset = cursorLineOffset;
                                    // Calculate selection if present
                                    if (length > 0)
                                    {
                                        int startLine = document.getLineOfOffset(offset) + 1;
                                        int startOffsetInLine = offset - document.getLineOffset(startLine - 1);
                                        int endOffset = offset + length;
                                        int endLine = document.getLineOfOffset(endOffset) + 1;
                                        int endOffsetInLine = endOffset - document.getLineOffset(endLine - 1);
                                        info.selectionStartLine = startLine;
                                        info.selectionStartLineOffset = startOffsetInLine;
                                        info.selectionEndLine = endLine;
                                        info.selectionEndLineOffset = endOffsetInLine;
                                    }
                                }
                                catch (Exception e)
                                {
                                    log.logError(e);
                                }
                            }
                        }

                        // Add to project's open files list
                        projectOpenFiles.computeIfAbsent(project, k -> new ArrayList<>()).add(info);
                    }
                    catch (PartInitException e)
                    {
                        log.logError(e);
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

    /**
     * Reads project metadata from .project file
     *
     * @param project Eclipse project instance
     * @param projectInfo Project info object to populate
     */
    @SuppressWarnings("nls")
    private void readProjectMetadata(IProject project, Project projectInfo)
    {
        try
        {
            // Locate .project file in project root
            var projectFile = project.getFile(".project");
            if (projectFile == null || !projectFile.exists())
            {
                return;
            }

            // Parse XML content of .project file
            try (InputStream input = projectFile.getContents())
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(input);
                // Extract description from <comment> element
                NodeList comments = doc.getElementsByTagName("comment");
                if (comments.getLength() > 0)
                {
                    projectInfo.description = comments.item(0).getTextContent().trim();
                }

                // Extract build commands
                var buildCommands = doc.getElementsByTagName("buildCommand");
                for (int i = 0; i < buildCommands.getLength(); i++)
                {
                    var commandNode = buildCommands.item(i);
                    if (commandNode.getNodeType() == Node.ELEMENT_NODE)
                    {
                        var commandElement = (Element)commandNode;
                        var names = commandElement.getElementsByTagName("name");
                        if (names.getLength() > 0)
                        {
                            String commandName = names.item(0).getTextContent().trim();
                            projectInfo.buildCommands.add(commandName);
                        }
                    }
                }

                // Extract project natures
                var natures = doc.getElementsByTagName("nature");
                for (int i = 0; i < natures.getLength(); i++)
                {
                    var nature = natures.item(i).getTextContent().trim();
                    projectInfo.natures.add(nature);
                }
            }
        }
        catch (Exception error)
        {
            log.logError(error);
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
        // Detailed tool description with all fields
        var description = new StringBuilder();
        description.append("Provides comprehensive information about IDE projects including:");
        description.append("\n- Project name and absolute file system path");
        description.append("\n- Project description (from .project file's <comment> element)");
        description.append("\n- Status indicators (exists, is open)");
        description.append("\n- 'is_current' flag indicating if project has open files");
        description.append("\n- Build commands (list of builder names from .project file)");
        description.append("\n- Project natures (list of project types from .project file)");
        description.append("\n- List of currently open files with cursor position and selection information:");
        description.append("\n  - relative_file_path: Project-relative file path");
        description.append("\n  - cursor_line: Current line number (1-based)");
        description.append("\n  - cursor_line_offset: Character offset in current line");
        description.append("\n  - selection_start_line: Start line of selection (if any)");
        description.append("\n  - selection_start_line_offset: Start offset in selection line");
        description.append("\n  - selection_end_line: End line of selection (if any)");
        description.append("\n  - selection_end_line_offset: End offset in selection line");
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
        /*
         * Имя проекта.
         */
        @SerializedName("name")
        public String name;

        /*
         * Абсолютный путь к проекту.
         */
        @SerializedName("absolute_path")
        public String absolutePath;

        /*
         * Открыт проект или нет.
         */
        @SerializedName("is_open")
        public Boolean isOpen;

        /*
         * Существует ли проект.
         */
        @SerializedName("exists")
        public Boolean exists;

        /*
         * Текущий проект или нет.
         */
        @SerializedName("is_current")
        public Boolean isCurrent;

        /*
         * Описание проекта.
         */
        @SerializedName("description")
        public String description;

        /*
         * Идентификатор сессии.
         */
        @SerializedName("session_id")
        public String sessionId;

        /*
         * Список команд сборки проекта.
         */
        @SerializedName("build_commands")
        public List<String> buildCommands;

        /*
         * Список признаков проекта.
         */
        @SerializedName("natures")
        public List<String> natures;

        /*
         * Список открытых файлов в IDE.
         */
        @SerializedName("open_files")
        public List<OpenFileInfo> openFiles;

        /*
         * Список директорий проекта.
         */
        @SerializedName("directories")
        public List<String> directories;
    }

    /**
     * Data structure for open file information
     */
    private static class OpenFileInfo
    {
        /*
         * Относительный путь к файлу.
         */
        @SerializedName("relative_file_path")
        public String relativeFilePath;

        /*
         * Номер строки курсора.
         */
        @SerializedName("cursor_line")
        public Integer cursorLine;

        /*
         * Смещение строки курсора.
         */
        @SerializedName("cursor_line_offset")
        public Integer cursorLineOffset;

        /*
         * Номер строки выделения.
         */
        @SerializedName("selection_start_line")
        public Integer selectionStartLine;

        /*
         * Смещение строки выделения.
         */
        @SerializedName("selection_start_line_offset")
        public Integer selectionStartLineOffset;

        /*
         * Номер строки конца выделения.
         */
        @SerializedName("selection_end_line")
        public Integer selectionEndLine;

        /*
         * Смещение строки конца выделения.
         */
        @SerializedName("selection_end_line_offset")
        public Integer selectionEndLineOffset;
    }
}