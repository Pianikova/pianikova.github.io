/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
		"{\"code\":\"2 + 3\"}"; //$NON-NLS-1$

	private static String AnswerExample =
        "{\"return_value\":\"5\",\"repl_session_id\":\"uuid-123\",\"std_out\":\"\",\"std_err\":\"\"}"; //$NON-NLS-1$

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
    private final IJShellSessionCache sessionCache;
    private final Set<IJShellBindingProvider> bindingProviders;
    private final IMarkdownUtils markdownUtils;

	@Inject
	public JShellMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IJShellSessionCache sessionCache, Set<IJShellBindingProvider> bindingProviders, IMarkdownUtils markdownUtils)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(sessionCache);
		Preconditions.checkNotNull(bindingProviders);
        Preconditions.checkNotNull(markdownUtils);

		this.json = json;
		this.messageFactory = messageFactory;
        this.sessionCache = sessionCache;
		this.bindingProviders = bindingProviders;
        this.markdownUtils = markdownUtils;
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

		if (request.code == null || request.code.isBlank())
		{
			throw new ToolException("`code` cannot be empty.");
		}

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
            IJShellSession session = sessionCache.getOrCreateSession(request.sessionId);
            JShellExecutionResult result = session.execute(request.code);

            var content = json.serialize(result);

            var responseMarkdown = buildResponseMarkdown(request.code, result);
            details.responseMarkdown = responseMarkdown;

            return messageFactory.createMessage(this, call, content, details);
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
        md.append("```java\n").append(markdownUtils.escapeForMarkdown(code)).append("\n```\n");

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
        description.append("\n- Supports sessions - provide same repl_session_id to continue REPL state.");
        description.append(
            "\n- **Recommendation:** Use sessions to preserve state between code executions. Save the repl_session_id from the response and reuse it for subsequent calls to maintain variables and context.");
        description.append("\n- Variable state is preserved across code executions within the same session.");
        description.append("\n- If repl_session_id is not provided, a new session will be created.");
        description.append("\n- Returns execution result including repl_session_id, return value, stdout, stderr.");
		description.append("\n- Compilation and runtime errors are clearly identified.");

		// Add bindings information
		if (!bindingProviders.isEmpty())
		{
			description.append("\n\nAvailable bindings:");
			for (var provider : bindingProviders)
			{
				var descriptions = provider.getBindingDescriptions();
				if (!descriptions.isEmpty())
				{
					for (var entry : descriptions.entrySet())
					{
						description.append("\n- `").append(entry.getKey()).append("` - ")
							.append(entry.getValue());
					}
				}
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
		codeProp.description = "Java code to execute";
		properties.put("code", codeProp);

		var sessionIdProp = new McpToolCallProperty();
		sessionIdProp.type = "string";
        sessionIdProp.description =
            "Session ID for maintaining REPL state (optional, if not provided a new session will be created)";
        properties.put("repl_session_id", sessionIdProp);

		parameters.properties = properties;
		var required = new ArrayList<String>();
		required.add("code");
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

