/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
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
        + "    \"name\": \"Warehouse\",\n"
        + "    \"path\": \"D:\\\\Projects\\\\EDT_Plugin\\\\Warehouse\",\n"
        + "    \"is_open\": true,\n"
        + "    \"exists\": true,\n"
        + "    \"is_current\": true,\n"
        + "    \"session_id\": \"A:dd933ffc-a55d-4fef-b6bf-37d66e184209\",\n"
        + "    \"comment\": \"Sample project\",\n"
        + "    \"build_commands\": [\n"
        + "      \"org.eclipse.xtext.ui.shared.xtextBuilder\"\n"
        + "    ],\n"
        + "    \"natures\": [\n"
        + "      \"org.eclipse.xtext.ui.shared.xtextNature\",\n"
        + "      \"com._1c.g5.v8.dt.core.V8ConfigurationNature\"\n"
        + "    ],\n"
        + "    \"details\": {\n"
        + "      \"1C project details\": {\n"
        + "        \"name\": \"Warehouse\",\n"
        + "        \"type\": \"Configuration\",\n"
        + "        \"script_language\": \"English\",\n"
        + "        \"version\": \"1.1.3\",\n"
        + "        \"platform_version\": \"8.3.24\",\n"
        + "        \"vendor\": \"Abc Inc\",\n"
        + "        \"compatibility\": \"8.3.24\",\n"
        + "        \"comment\": \"Sample configuration\",\n"
        + "        \"brief_information\": {\n"
        + "          \"en\": \"Warehouse operations and stock accounting\"\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final ISessionService sessionService;
    private final Set<IProjectDetailsProvider> projectDetailsProviders;
    private final IMarkdownUtils markdownUtils;

    @Inject
    public GetProjectsMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory,
        ISessionService sessionService, Set<IProjectDetailsProvider> projectDetailsProviders,
        IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(projectDetailsProviders);
        Preconditions.checkNotNull(markdownUtils);

        this.log = log;
        this.json = json;
        this.messageFactory = messageFactory;
        this.sessionService = sessionService;
        this.projectDetailsProviders = projectDetailsProviders;
        this.markdownUtils = markdownUtils;

        spec = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return false;
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
        details.autoCall = true;
        details.hideAfter = true;
        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = Messages.ProjectsTitle;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        // Execute the operation asynchronously
        return CompletableFuture.supplyAsync(() -> {
            // Check cancellation status before starting
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var projects = root.getProjects();
            var response = new ArrayList<Project>();
            var projectCount = 0;
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
                projectInfo.buildCommands = new ArrayList<>();
                projectInfo.natures = new ArrayList<>();
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

                }

                // Current project is not inferred from open files in this tool
                projectInfo.isCurrent = false;
                try
                {
                    var optionalSession = sessionFeature.get();
                    optionalSession.ifPresent(session -> projectInfo.sessionId = session.sessionId);
                }
                catch (InterruptedException | ExecutionException error)
                {
                    log.logError(error);
                }

                projectCount++;
            }

            // Create detailed response markdown with project information
            var responseMarkdown = new StringBuilder();
            responseMarkdown.append(MessageFormat.format(Messages.ProjectsLoadedTemplate,
                markdownUtils.createStyledText(String.valueOf(projectCount), TextColor.GREEN, FontWeight.BOLD, false)));

            // Add project details in collapsible section
            responseMarkdown.append("\n\n<details><summary>").append(Messages.ProjectsDetailsTitle).append("</summary>\n\n");

            for (var projectInfo : response)
            {
                // Add status indicators
                if (projectInfo.exists != null && projectInfo.exists)
                {
                    responseMarkdown.append("- [x]");
                }
                else
                {
                    responseMarkdown.append("- [ ]");
                }

                responseMarkdown.append(" **").append(markdownUtils.escapeForMarkdown(projectInfo.name)).append("**");

                // Add comment if available
                if (projectInfo.comment != null && !projectInfo.comment.trim().isEmpty())
                {
                    responseMarkdown.append(" - ").append(markdownUtils.escapeForMarkdown(projectInfo.comment.trim()));
                }

                responseMarkdown.append("\n");
            }

            responseMarkdown.append("</details>");

            details.responseMarkdown = responseMarkdown.toString();

            // Serialize and return the response
            var content = json.serialize(response);
            return messageFactory.createMessage(this, call, content, details);
        });
    }

    /**
     * Collects information about open files across all projects
     *
     * @return Map containing projects and their open files information
     */
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
        description.append("Lists projects in the workspace with key metadata and editor context.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Use this tool to choose a target project or infer scope from open files.");
        description.append("\n- `is_current` indicates a project with open files (likely in focus).");
        description.append("\n\nResponse includes:");
        description.append("\n- Project name and absolute path");
        description.append("\n- Description from .project <comment>");
        description.append("\n- Status flags (exists, open, is_current)");
        description.append("\n- Build commands and project natures");
        description.append("\n- Additional provider details (e.g., 1C project info)");
        description.append("\n\nRelated tools:");
        description.append("\n- Project issues/markers: `" + GetMarkersMcpTool.TOOL_NAME + "`.");
        description.append("\n- Locate files: `" + FindMcpTool.TOOL_NAME + "`.");
        description.append("\n- File history: `" + LocalHistoryMcpTool.TOOL_NAME + "`, `" + LocalChangesMcpTool.TOOL_NAME + "`.");
        description.append("\n- Navigation history: `" + NavigationHistoryMcpTool.TOOL_NAME + "`.");
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
        /* Project name. */
        @SerializedName("name")
        public String name;

        /* Absolute path to the project. */
        @SerializedName("path")
        public String absolutePath;

        /* Project is open. */
        @SerializedName("is_open")
        public Boolean isOpen;

        /* Project exists. */
        @SerializedName("exists")
        public Boolean exists;

        /* Current project. */
        @SerializedName("is_current")
        public Boolean isCurrent;

        /* Project description. */
        @SerializedName("description")
        public String description;

        /* Session id. */
        @SerializedName("session_id")
        public String sessionId;

        /* Project comment. */
        @SerializedName("comment")
        public String comment;

        /* Build commands for the project. */
        @SerializedName("build_commands")
        public List<String> buildCommands;

        /* Project natures. */
        @SerializedName("natures")
        public List<String> natures;

        /* Additional project details. */
        @SerializedName("details")
        public Map<String, Object> details;
    }
}
