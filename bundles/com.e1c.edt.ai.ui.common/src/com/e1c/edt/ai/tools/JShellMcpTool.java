/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

public class JShellMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "JShell"; //$NON-NLS-1$

	private static String QuestionExample =
        "{\"code\":\"2 + 3\",\"repl_session_id\":\"uuid-123\"}"; //$NON-NLS-1$

	private static String AnswerExample =
        "{\"return_value\":\"5\",\"std_out\":\"\",\"std_err\":\"\"}"; //$NON-NLS-1$

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
    private final IJShellSessionManager sessions;
    private final Set<IJShellBindingProvider> bindingProviders;
    private final IMarkdownUtils markdownUtils;
    private final IRestrictedTypesProvider restrictedTypesProvider;

	@Inject
	public JShellMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IJShellSessionManager sessions, Set<IJShellBindingProvider> bindingProviders, IMarkdownUtils markdownUtils,
        IRestrictedTypesProvider restrictedTypesProvider)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(sessions);
		Preconditions.checkNotNull(bindingProviders);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(restrictedTypesProvider);

		this.json = json;
		this.messageFactory = messageFactory;
        this.sessions = sessions;
		this.bindingProviders = bindingProviders;
        this.markdownUtils = markdownUtils;
        this.restrictedTypesProvider = restrictedTypesProvider;
		this.spec = createSpecification();
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

	@SuppressWarnings("nls")
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

		if (call.callKind == ToolCallKind.RENDER)
		{
            var requestMarkdown = new StringBuilder();
            requestMarkdown.append(Messages.JShellExecutingTemplate);
            requestMarkdown.append("\n\n");
            requestMarkdown.append("```java\n").append(request.code).append("\n```");
            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
		}

        return CompletableFuture.completedFuture(executeCode(request, call, details, cancellationToken));
    }

    @SuppressWarnings("nls")
    private ToolCallMessage executeCode(Request request, McpToolCall call, ToolCallMessageDetails details,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
		{
            throw new ToolException("Operation was cancelled before execution.", null, ToolErrorType.RETRYABLE);
        }

        try
		{
            IJShellSession session = sessions.getSession(request.sessionId);
            if (session == null)
            {
                throw new ToolException(
                    "Session not found. Use " + JShellSessionMcpTool.TOOL_NAME + " tool to create a session first.",
                    null,
                    ToolErrorType.RETRYABLE);
            }

            JShellExecutionResult result = session.execute(request.code);
            String responseMarkdown = buildResponseMarkdown(request.code, result);

            var content = json.serialize(result);
            details.responseMarkdown = responseMarkdown;

            return messageFactory.createMessage(this, call, content, details);
        }
        catch (ToolException e)
        {
            throw e;
        }
        catch (Exception e)
		{
            throw new ToolException("JShell execution failed: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
		}
	}

    @SuppressWarnings("nls")
	private String buildResponseMarkdown(String code, JShellExecutionResult result)
	{
		var md = new StringBuilder();

        if (!result.compilationErrors.isEmpty() || !result.runtimeErrors.isEmpty())
        {
            md.append(Messages.JShellErrorTemplate);
        }
        else
        {
            md.append(Messages.JShellExecutedTemplate);
        }

        md.append("\n\n");

        // Show the executed code
        md.append("```java\n").append(code).append("\n```\n");

        // Show return value if any
		if (result.returnValue != null && !result.returnValue.isEmpty())
		{
            md.append(markdownUtils.escapeForMarkdown(result.returnValue));
            md.append("\n");
		}

		return md.toString();
	}

	@SuppressWarnings("nls")
	private McpToolCallSpecification createSpecification()
	{
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
		description.append("Executes Java code using JShell REPL (Read-Eval-Print Loop).");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- **IMPORTANT:** Both `code` and `repl_session_id` parameters are **required**.");
        description.append("\n- Use `")
            .append(JShellSessionMcpTool.TOOL_NAME)
            .append("` tool first to create a REPL session and get session ID.");
        description.append("\n- Variable state is preserved across code executions within the same session.");
        description.append("\n- Returns execution result including return value, stdout, stderr.");
		description.append("\n- Compilation and runtime errors are clearly identified.");

		// Recommended workflow
        description.append("\n\n### Recommended workflow:");
        description.append(
            "\n1. First call `")
            .append(JShellSessionMcpTool.TOOL_NAME)
            .append(
                "` tool to create a session - returns session ID and available bindings (like `display`, `workbench`, `Math`, `System`)");
        description.append("\n2. Save `repl_session_id` from the response");
        description.append(
            "\n3. Use `")
            .append(TOOL_NAME)
            .append(
                "` tool: `{\"repl_session_id\": \"...\", \"code\": \"...\"}` - execute code using available bindings");
        description.append("\n4. Always reuse the same `repl_session_id` to maintain state between executions");

		// Add bindings information
		if (!bindingProviders.isEmpty())
		{
			description.append("\n\n### Available bindings:");
			for (var provider : bindingProviders)
			{
				var infos = provider.getBindingInfos();
				if (!infos.isEmpty())
				{
					for (var entry : infos.entrySet())
					{
						String bindingName = entry.getKey();
						JShellBindingDescription bindingInfo = entry.getValue();
						String bindingDesc = bindingInfo.getDescription();
						String bindingExample = bindingInfo.getExample();

						description.append("\n\n**`").append(bindingName).append("`**");
						description.append("\n- ").append(bindingDesc);

						if (bindingExample != null && !bindingExample.isEmpty())
						{
							description.append("\n- **Example usage:**");
							description.append("\n```java\n").append(bindingExample).append("\n```");
						}
					}
				}
			}
		}

        // Add restricted types information
        Set<String> restrictedTypes = restrictedTypesProvider.getRestrictedTypes();
        if (!restrictedTypes.isEmpty())
        {
            description.append("\n\n### ⚠️ **RESTRICTED TYPES** ⚠️");
            description.append("\n**The following types are STRICTLY FORBIDDEN** and will cause ToolException if used:");
            for (String type : restrictedTypes.stream().sorted().collect(Collectors.toList()))
            {
                description.append("- `").append(type).append("`\n");
            }
        }

		description.append("\n\nExample:");
		description.append("\n  Q: ").append(QuestionExample);
		description.append("\n  A: ").append(AnswerExample);

		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
		var properties = new HashMap<String, McpToolCallProperty>();

		var codeProp = new McpToolCallProperty();
		codeProp.type = "string";
        codeProp.description = "Java code to execute (required)";
		properties.put("code", codeProp);

		var sessionIdProp = new McpToolCallProperty();
		sessionIdProp.type = "string";
        sessionIdProp.description =
            "Session ID for maintaining REPL state (required, must be obtained from " + JShellSessionMcpTool.TOOL_NAME
                + " tool)";
        properties.put("repl_session_id", sessionIdProp);

		parameters.properties = properties;
		var required = new ArrayList<String>();
        required.add("code");
        required.add("repl_session_id");
		parameters.required = required;

		spec.function.parameters = parameters;
		return spec;
	}

	private static class Request
	{
		@SerializedName("code")
		public String code;

        @SerializedName("repl_session_id")
		public String sessionId;
	}
}

