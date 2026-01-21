/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.ui.IContentSourceProvider;
import com.e1c.edt.ai.ui.IDispatcher;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class GetProjectsMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "GetProjects"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"name\": \"Склад\",\n"
        + "    \"absolute_path\": \"D:\\\\Projects\\\\_Eclipse\\\\EDT_Plugin\\\\Склад\",\n"
        + "    \"is_open\": true,\n"
        + "    \"exists\": true,\n"
        + "    \"is_current\": true,\n"
        + "    \"session_id\": \"A:dd933ffc-a55d-4fef-b6bf-37d66e184209\",\n"
        + "    \"comment\": \"\",\n"
        + "    \"build_commands\": [\n"
        + "      \"org.eclipse.xtext.ui.shared.xtextBuilder\"\n"
        + "    ],\n"
        + "    \"natures\": [\n"
        + "      \"org.eclipse.xtext.ui.shared.xtextNature\",\n"
        + "      \"com._1c.g5.v8.dt.core.V8ConfigurationNature\"\n"
        + "    ],\n"
        + "    \"open_files\": [\n"
        + "      {\n"
        + "        \"relative_file_path\": \"src/CommonModules/Математика/Module.bsl\",\n"
        + "        \"cursor_line\": 11,\n"
        + "        \"cursor_line_offset\": 1,\n"
        + "        \"selection_start_line\": 11,\n"
        + "        \"selection_start_line_offset\": 1,\n"
        + "        \"selection_end_line\": 15,\n"
        + "        \"selection_end_line_offset\": 19\n"
        + "      }\n"
        + "    ],\n"
        + "    \"details\": {\n"
        + "      \"1C project details\": {\n"
        + "        \"name\": \"Склад\",\n"
        + "        \"type\": \"Configuration\",\n"
        + "        \"script_language\": \"English\",\n"
        + "        \"version\": \"1.1.3\",\n"
        + "        \"platform_version\": \"8.3.24\",\n"
        + "        \"vendor\": \"Abc INC\",\n"
        + "        \"compatibility\": \"8.3.24\",\n"
        + "        \"comment\": \"Основной справочник для учета хранения и перемещения товаров на складах предприятия\",\n"
        + "        \"brief_information\": {\n"
        + "          \"en\": \"Управление складскими операциями и учет товарных запасов\"\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  },\n"
        + "  {\n"
        + "    \"name\": \"Склад.РасширениеСклада\",\n"
        + "    \"absolute_path\": \"D:\\\\Projects\\\\_Eclipse\\\\EDT_Plugin\\\\Склад.РасширениеСклада\",\n"
        + "    \"is_open\": true,\n"
        + "    \"exists\": true,\n"
        + "    \"is_current\": true,\n"
        + "    \"session_id\": \"A:f1f911e2-3570-4097-99c0-91f0f9d0c28b\",\n"
        + "    \"comment\": \"\",\n"
        + "    \"build_commands\": [\n"
        + "      \"org.eclipse.xtext.ui.shared.xtextBuilder\"\n"
        + "    ],\n"
        + "    \"natures\": [\n"
        + "      \"org.eclipse.xtext.ui.shared.xtextNature\",\n"
        + "      \"com._1c.g5.v8.dt.core.V8ExtensionNature\"\n"
        + "    ],\n"
        + "    \"open_files\": [\n"
        + "      {\n"
        + "        \"relative_file_path\": \"src/Configuration/Configuration.mdo\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"relative_file_path\": \"src/CommonModules/РасшСклада_ОбщийМодуль/Module.bsl\",\n"
        + "        \"cursor_line\": 7,\n"
        + "        \"cursor_line_offset\": 15\n"
        + "      }\n"
        + "    ],\n"
        + "    \"details\": {\n"
        + "      \"1C project details\": {\n"
        + "        \"name\": \"Склад.РасширениеСклада\",\n"
        + "        \"type\": \"Extension\",\n"
        + "        \"script_language\": \"Russian\",\n"
        + "        \"version\": \"1.0.0\",\n"
        + "        \"platform_version\": \"8.3.24\",\n"
        + "        \"vendor\": \"Abc Inc\",\n"
        + "        \"compatibility\": \"8.3.24\",\n"
        + "        \"comment\": \"Расширение для управления складскими операциями и учета товаров на складе\",\n"
        + "        \"brief_information\": {},\n"
        + "        \"parent_project\": \"Склад\"\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IDispatcher dispatcher;
    private final ISessionService sessionService;
    private final Set<IProjectDetailsProvider> projectDetailsProviders;
    private final IContentSourceProvider contentSourceProvider;

    @Inject
    public GetProjectsMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory,
        IDispatcher dispatcher, ISessionService sessionService, Set<IProjectDetailsProvider> projectDetailsProviders,
        IContentSourceProvider contentSourceProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(projectDetailsProviders);
        Preconditions.checkNotNull(contentSourceProvider);

        this.log = log;
        this.json = json;
        this.messageFactory = messageFactory;
        this.dispatcher = dispatcher;
        this.sessionService = sessionService;
        this.projectDetailsProviders = projectDetailsProviders;
        this.contentSourceProvider = contentSourceProvider;

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
            var projectOpenFiles =
                dispatcher.dispatch(() -> collectOpenFiles())
                    .orElseGet(() -> new HashMap<>());
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
                projectInfo.details = new HashMap<>();
                if (project.isOpen() && project.exists())
                {
                    try
                    {
                        var description = project.getDescription();
                        if (description != null)
                        {
                            projectInfo.comment = description.getComment();
                            var buildSpec = description.getBuildSpec();
                            if (buildSpec != null)
                            {
                                for (var buildConfig : buildSpec)
                                {
                                    projectInfo.buildCommands.add(buildConfig.getBuilderName());
                                }
                            }

                            var natureIds = description.getNatureIds();
                            if (natureIds != null)
                            {
                                for (var natureId : natureIds)
                                {
                                    if (project.isNatureEnabled(natureId))
                                    {
                                        projectInfo.natures.add(natureId);
                                    }
                                }
                            }
                        }

                        for (var projectDetailsProvider : projectDetailsProviders)
                        {
                            projectDetailsProvider.fill(project, projectInfo.details);
                        }
                    }
                    catch (CoreException error)
                    {
                        log.logError(error);
                    }

                    // Collect open files
                    if (openFilesList != null)
                    {
                        projectInfo.openFiles.addAll(openFilesList);
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

                        var info = new OpenFileInfo();
                        info.relativeFilePath = file.getProjectRelativePath().toString();
                        var project = file.getProject();
                        var document =
                            contentSourceProvider.getFileDocument(file).map(i -> i.getDocument()).orElse(null);
                        if (document != null)
                        {
                            var selection = Optional.ofNullable(editorRef.getEditor(false))
                                .map(editor -> editor.getEditorSite())
                                .map(editorSite -> editorSite.getSelectionProvider())
                                .map(selectionProvider -> selectionProvider.getSelection())
                                .map(s -> s instanceof ITextSelection ? (ITextSelection)s : null)
                                .orElse(null);

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
                                        var startLine = document.getLineOfOffset(offset) + 1;
                                        var startOffsetInLine = offset - document.getLineOffset(startLine - 1);
                                        var endOffset = offset + length;
                                        var endLine = document.getLineOfOffset(endOffset) + 1;
                                        var endOffsetInLine = endOffset - document.getLineOffset(endLine - 1);
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
        description.append(
            "Returns a list of all projects in the Eclipse workspace with detailed information. For each project, the following is provided:");
        description.append("\n- Project name and absolute file system path");
        description.append("\n- Project description (from .project file`s <comment> element)");
        description.append("\n- Status indicators: exists, is open");
        description
            .append("\n- `is_current`: true if the project has any open files (indicating it might be in focus)");
        description.append("\n- Build commands: list of builder names from .project file");
        description.append("\n- Project natures: list of project types from .project file");
        description.append("\n- List of currently open files in the project, each with:");
        description.append("\n  - relative_file_path: Project-relative file path");
        description.append("\n  - cursor_line: Current line number (1-based)");
        description.append("\n  - cursor_line_offset: Character offset in current line");
        description.append("\n  - selection_start_line: Start line of selection (if any)");
        description.append("\n  - selection_start_line_offset: Start offset in selection line");
        description.append("\n  - selection_end_line: End line of selection (if any)");
        description.append("\n  - selection_end_line_offset: End offset in selection line");
        description.append("\n- Additional details: provided by registered providers (e.g., 1C project details)");
        description.append(
            "\nIMPORTANT: If the scope of code review, error detection, refactoring, etc. is not specified, use the list of currently open files to determine the context.");
        description.append("\n\nNote: To get errors, warnings, bookmarks, etc. for a project or file, use the `"
            + GetMarkersMcpTool.TOOL_NAME + "` tool.");
        description.append("\n\nExample output:");
        description.append("\n").append(AnswerExample);
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
         * Комментарий к проекту.
         */
        @SerializedName("comment")
        public String comment;

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
         * Дополнительные данные проекта.
         */
        @SerializedName("details")
        public Map<String, Object> details;
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