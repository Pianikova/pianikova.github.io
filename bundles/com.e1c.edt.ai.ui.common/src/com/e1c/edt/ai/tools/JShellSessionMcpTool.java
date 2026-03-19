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

public class JShellSessionMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "JShellSession"; //$NON-NLS-1$

	private static final String QuestionExample = "{}"; //$NON-NLS-1$

	private static final String AnswerExample =
		"{\"repl_session_id\":\"uuid-123\",\"available_bindings\":[\"display\",\"workbench\",\"Math\",\"System\"]}"; //$NON-NLS-1$

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final IJShellSessionManager sessions;
	private final Set<IJShellBindingProvider> bindingProviders;

	@Inject
	public JShellSessionMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IJShellSessionManager sessions, Set<IJShellBindingProvider> bindingProviders)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(sessions);
		Preconditions.checkNotNull(bindingProviders);

		this.json = json;
		this.messageFactory = messageFactory;
		this.sessions = sessions;
		this.bindingProviders = bindingProviders;
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
            details.requestMarkdown = Messages.JShellSessionCreating;
			return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
		}

		return CompletableFuture.completedFuture(createSession(request, call, details, cancellationToken));
	}

	@SuppressWarnings("nls")
	private ToolCallMessage createSession(Request request, McpToolCall call, ToolCallMessageDetails details,
		ICancellationToken cancellationToken)
	{
		if (cancellationToken.isCanceled())
		{
			throw new ToolException("Operation was cancelled before execution.", null, ToolErrorType.RETRYABLE);
		}

		try
		{
			IJShellSession session = sessions.getOrCreateSession(request.sessionId);

			SessionResult result = new SessionResult();
			result.sessionId = session.getSessionId();
			result.availableBindings = new ArrayList<>();
            result.executionHistory = new ArrayList<>(session.getExecutionHistory());

			for (var provider : bindingProviders)
			{
				var infos = provider.getBindingInfos();
				result.availableBindings.addAll(infos.keySet());
			}

			var content = json.serialize(result);
            details.responseMarkdown = Messages.JShellSessionCreated;
			return messageFactory.createMessage(this, call, content, details);
		}
		catch (Exception e)
		{
			throw new ToolException("JShell session creation failed: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
		}
	}

	@SuppressWarnings("nls")
	private McpToolCallSpecification createSpecification()
	{
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
		description.append("Creates a new JShell REPL session or returns information about an existing session.");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
		description.append("\n- If `repl_session_id` is not provided, a new session will be created.");
		description.append("\n- If `repl_session_id` is provided, returns information about the existing session.");
		description.append("\n- Returns session ID, available bindings, and execution history.");

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

		description.append("\n\nExample:");
		description.append("\n  Q: ").append(QuestionExample);
		description.append("\n  A: ").append(AnswerExample);

		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
		var properties = new HashMap<String, McpToolCallProperty>();

		var sessionIdProp = new McpToolCallProperty();
		sessionIdProp.type = "string";
		sessionIdProp.description =
			"Optional session ID. If not provided, a new session will be created. If provided, returns info about the existing session.";
		properties.put("repl_session_id", sessionIdProp);

		parameters.properties = properties;
		var required = new ArrayList<String>();
		parameters.required = required;

		spec.function.parameters = parameters;
		return spec;
	}

	private static class Request
	{
		@SerializedName("repl_session_id")
		public String sessionId;
	}

	private static class SessionResult
	{
		@SerializedName("repl_session_id")
		public String sessionId;

		@SerializedName("available_bindings")
		public ArrayList<String> availableBindings;

		@SerializedName("execution_history")
		public ArrayList<String> executionHistory;
	}
}
