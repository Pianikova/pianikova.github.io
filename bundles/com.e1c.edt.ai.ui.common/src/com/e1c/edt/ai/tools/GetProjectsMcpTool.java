/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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
import com.e1c.edt.ai.assistent.model.ProjectInfo;
import com.e1c.edt.ai.assistent.model.ProjectsInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;


public class GetProjectsMcpTool
    implements IMcpTool
{
    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{ }";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\n"
        + "  \"projects\": [\n"
        + "    {\n"
        + "      \"name\": \"Управление торговлей 11.5\",\n"
        + "      \"absolute_path\": \"C:\\\\1C_Projects\\\\УТ115\\\\Configuration.cf\",\n"
        + "      \"is_open\": true,\n"
        + "      \"exists\": true,\n"
        + "      \"is_current\": false\n"
        + "    },\n"
        + "    {\n"
        + "      \"name\": \"Бухгалтерия предприятия 3.0\",\n"
        + "      \"absolute_path\": \"D:\\\\Конфигурации\\\\БП30\\\\Configuration.cfe\",\n"
        + "      \"is_open\": true,\n"
        + "      \"exists\": true,\n"
        + "      \"is_current\": true\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    // @formatter:oт

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
        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var currentProjects = new HashSet<IProject>();
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
                            if (file != null && file.exists())
                            {
                                currentProjects.add(file.getProject());
                            }
                        }
                        catch (PartInitException error)
                        {
                            log.logError(error);
                        }
                    }
                }
            }


            var root = ResourcesPlugin.getWorkspace().getRoot();
            var projects = root.getProjects();
            var projectsInfo = new ProjectsInfo();
            projectsInfo.projects = new ArrayList<>();
            for (var project : projects)
            {
                // Check for cancellation periodically inside the loop.
                if (cancellationToken.isCanceled())
                {
                    return messageFactory.createError(this, call, "Operation was cancelled during execution.");
                }

                var projectInfo = new ProjectInfo();
                projectInfo.name = project.getName();
                var location = project.getRawLocation();
                projectInfo.absolutePath = location != null ? location.toOSString() : null;
                projectInfo.isOpen = project.isOpen();
                projectInfo.exists = project.exists();
                projectInfo.isOpen = project.isOpen();
                projectInfo.isCurrent = currentProjects.contains(project);
                projectsInfo.projects.add(projectInfo);
            }

            var content = json.serialize(projectsInfo);
            return messageFactory.createMessage(this, call, content);
        }).exceptionally(ex -> {
            var cause = ex instanceof CompletionException ? ex.getCause() : ex;
            return messageFactory.createError(this, call, "Failed to get. " + cause.getMessage());
        });
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
     // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = "get_projects";

        var description = new StringBuilder();

        description.append("Provides information about projects: name, absolute path, exists, is open, is current, etc.");
        description.append("\nNOTE: add a description of what will be done when using this tool.");

        description.append("\nFor exapmple:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        parameters.properties = new HashMap<>();
        parameters.required = new ArrayList<>();
        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }
}