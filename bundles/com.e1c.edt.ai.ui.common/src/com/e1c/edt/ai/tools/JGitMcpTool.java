/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
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
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class JGitMcpTool
    implements IMcpTool
{
    @SuppressWarnings("nls")
    public static final String TOOL_NAME = "JGit";

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"working_directory\": \"C:\\\\Projects\",\n"
        + "  \"args\": [\"status\"]\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\n"
        + "  \"exit_code\": 0,\n"
        + "  \"std_out\": \"On branch main\\\\nYour branch is up to date with 'origin/main'.\\\\n\\\\nnothing to commit, working tree clean\\\\n\",\n"
        + "  \"std_err\": \"\"\n"
        + "}";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IMarkdownUtils markdownUtils;
    private final IJGitCommonHelper commonHelper;
    private final Map<String, IJGitCommand> commands;

    @Inject
    public JGitMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils,
        IJGitCommonHelper commonHelper, Set<IJGitCommand> commands)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(commonHelper);
        Preconditions.checkNotNull(commands);
        this.json = json;
        this.messageFactory = messageFactory;
        this.markdownUtils = markdownUtils;
        this.commonHelper = commonHelper;
        this.commands = commands.stream().collect(Collectors.toMap(IJGitCommand::getName, i -> i));
        spec = createSpecification();
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return spec;
    }

    @SuppressWarnings("nls")
    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = true;

        var optionalRequest = json.deserialize(call.function.arguments, GitRequest.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample,
                ToolErrorType.RETRYABLE);
        }

        var request = optionalRequest.get();

        if (call.callKind == ToolCallKind.RENDER)
        {
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.JGitTitleTemplate,
                request.args != null && !request.args.isEmpty() ? request.args.get(0) : ""));

            if (request.workingDirectory != null)
            {
                requestMarkdown.append("\n\n")
                    .append(Messages.JGitWorkingDir)
                    .append(": `")
                    .append(request.workingDirectory)
                    .append("`");
            }
            if (request.args != null && !request.args.isEmpty())
            {
                requestMarkdown.append("\n\n")
                    .append(Messages.JGitArguments)
                    .append(": `")
                    .append(String.join(" ", request.args))
                    .append("`");
            }
            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        return CompletableFuture.supplyAsync(() ->
        {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            var workingDir = request.workingDirectory;
            if (workingDir == null || workingDir.isBlank())
            {
                var root = ResourcesPlugin.getWorkspace().getRoot();
                var project = root.getProjects();
                if (project.length > 0)
                {
                    var location = project[0].getLocation();
                    if (location != null)
                    {
                        workingDir = location.toOSString();
                    }
                }
            }

            if (workingDir == null || workingDir.isBlank())
            {
                throw new ToolException("No working directory specified and no active project found.");
            }

            try
            {
                var result = executeGitCommand(workingDir, request.args);
                var response = new GitResponse();
                response.exitCode = result.exitCode;
                response.stdOut = result.stdOut;
                response.stdErr = result.stdErr;

                var content = json.serialize(response);

                var responseMarkdown = new StringBuilder();
                responseMarkdown.append(MessageFormat.format(Messages.JGitExecutedTemplate,
                    request.args != null && !request.args.isEmpty() ? request.args.get(0) : ""));

                if (!result.stdOut.isEmpty())
                {
                    responseMarkdown.append("\n\n")
                        .append(Messages.JGitOutput)
                        .append(":\n```\n")
                        .append(result.stdOut)
                        .append("\n```");
                }

                if (!result.stdErr.isEmpty())
                {
                    responseMarkdown.append("\n\n")
                        .append(Messages.JGitErrors)
                        .append(":\n```\n")
                        .append(result.stdErr)
                        .append("\n```");
                }

                details.responseMarkdown = responseMarkdown.toString();
                return messageFactory.createMessage(this, call, content, details);
            }
            catch (Exception e)
            {
                var response = new GitResponse();
                response.exitCode = 1;
                response.stdErr = e.getMessage();

                var content = json.serialize(response);

                var responseMarkdown = new StringBuilder();
                responseMarkdown.append(Messages.JGitFailed)
                    .append(": ")
                    .append(markdownUtils.escapeForMarkdown(e.getMessage()));
                details.responseMarkdown = responseMarkdown.toString();

                return messageFactory.createMessage(this, call, content, details);
            }
        }).exceptionally(throwable ->
        {
            if (throwable.getCause() instanceof ToolException)
            {
                throw (ToolException) throwable.getCause();
            }
            throw new ToolException(throwable.getMessage(), throwable, ToolErrorType.RETRYABLE);
        });
    }

    @Override
    public CompletableFuture<Boolean> getIsAvailable()
    {
        return CompletableFuture.completedFuture(true);
    }

    @SuppressWarnings("nls")
    private GitCommandResult executeGitCommand(String workingDirectory, List<String> args)
        throws IOException, GitAPIException, URISyntaxException, ToolException
    {
        if (args == null || args.isEmpty())
        {
            throw new ToolException("No Git command specified.");
        }

        var command = args.get(0);
        var commandArgs = args.size() > 1 ? args.subList(1, args.size()) : new ArrayList<String>();

        var commandHandler = commands.get(command);
        if (commandHandler == null)
        {
            return new GitCommandResult(1, "", "jgit: '" + command + "' is not a jgit command. See 'jgit --help'.");
        }

        // Handle commands that don't require an existing repository
        if (command.equals("clone"))
        {
            try
            {
                if (commandHandler instanceof JGitClone)
                {
                    ((JGitClone)commandHandler).setWorkingDirectory(workingDirectory);
                }
                return commandHandler.run(null, commandArgs);
            }
            catch (Exception e)
            {
                return new GitCommandResult(1, "", e.getMessage());
            }
        }
        if (command.equals("init"))
        {
            try
            {
                if (commandHandler instanceof JGitInit)
                {
                    ((JGitInit)commandHandler).setWorkingDirectory(workingDirectory);
                }
                return commandHandler.run(null, commandArgs);
            }
            catch (Exception e)
            {
                return new GitCommandResult(1, "", e.getMessage());
            }
        }

        var repository = commonHelper.openRepository(workingDirectory);
        if (repository == null)
        {
            return new GitCommandResult(128, "", "fatal: not a git repository (or any of the parent directories): .git");
        }

        try (Git git = new Git(repository))
        {
            try
            {
                return commandHandler.run(git, commandArgs);
            }
            catch (Exception e)
            {
                return new GitCommandResult(1, "", e.getMessage());
            }
        }
    }

    // @formatter:off
    @SuppressWarnings("nls")
    private McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Executes Git commands using JGit API (no Git executable required).");
        description.append("\n\nSupported Commands:\n");

        var sortedCommands = new ArrayList<>(commands.entrySet());
        sortedCommands.sort((e1, e2) -> e1.getKey().compareTo(e2.getKey()));

        for (var entry : sortedCommands)
        {
            var cmd = entry.getValue();
            var cmdDesc = cmd.getDescription();

            description.append("\n**").append(cmd.getName()).append("**\n");
            description.append("  ").append(cmdDesc.getDescription()).append("\n");

            var params = cmdDesc.getParameters();
            if (!params.isEmpty())
            {
                description.append("  Parameters:\n");
                for (var param : params)
                {
                    description.append("    - `").append(param.getName()).append("`: ").append(param.getDescription()).append("\n");
                }
            }

            var notes = cmdDesc.getNotes();
            if (notes != null && !notes.isBlank())
            {
                description.append("  NOTE: ").append(notes).append("\n");
            }

            var examples = cmdDesc.getExamples();
            if (examples != null && !examples.isEmpty())
            {
                description.append("  Examples:\n");
                for (var ex : examples)
                {
                    description.append("    ").append(ex).append("\n");
                }
            }
        }

        description.append("\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Does NOT support all Git options - see JGit documentation for supported features.");
        description.append("\n- Working directory is optional - defaults to the active project directory.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var workingDirProp = new McpToolCallProperty();
        workingDirProp.type = "string";
        workingDirProp.description = "Working directory path. If not specified, uses the active project directory.";
        properties.put("working_directory", workingDirProp);

        var argsProp = new McpToolCallProperty();
        argsProp.type = "array";
        argsProp.description = "Git command and arguments. Example: [\"status\"] or [\"add\", \".\"] or [\"commit\", \"-m\", \"message\"]";
        properties.put("args", argsProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("args");
        spec.function.parameters = parameters;

        return spec;
    }
    // @formatter:on

    private static class GitRequest
    {
        @SerializedName("working_directory")
        public String workingDirectory;

        @SerializedName("args")
        public List<String> args;
    }

    private static class GitResponse
    {
        @SerializedName("exit_code")
        public int exitCode;

        @SerializedName("std_out")
        public String stdOut;

        @SerializedName("std_err")
        public String stdErr;
    }
}
