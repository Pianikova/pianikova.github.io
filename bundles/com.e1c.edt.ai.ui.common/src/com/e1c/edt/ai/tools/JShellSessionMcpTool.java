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
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class JShellSessionMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "JShellSession"; //$NON-NLS-1$

	private static final String AnswerExample =
        "{\"repl_session_id\":\"550e8400-e29b-41d4-a716-446655440000\",\"available_bindings\":[\"workbench\"]}"; //$NON-NLS-1$

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

	@Override
	public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
	{
		var details = new ToolCallMessageDetails();
		details.autoCall = false;
        if (call.callKind == ToolCallKind.RENDER)
		{
            details.requestMarkdown = Messages.JShellSessionCreating;
			return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
		}

        return CompletableFuture.supplyAsync(() -> createSession(call, details, cancellationToken));
	}

	@SuppressWarnings("nls")
    private ToolCallMessage createSession(McpToolCall call, ToolCallMessageDetails details,
		ICancellationToken cancellationToken)
	{
		if (cancellationToken.isCanceled())
		{
			throw new ToolException("Operation was cancelled before execution.", null, ToolErrorType.RETRYABLE);
		}

		try
		{
            var session = sessions.getOrCreateSession(null);
            var result = session.getSessionResult();
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
		description.append("Creates a new JShell REPL session.");
		description.append("\n\n**Purpose:**");
		description.append("\n- JShell provides access to APIs exposed by installed binding providers");
		description.append("\n- Use when you need to perform operations not available via other tools");
		description.append("\n- Session preserves state between JShell calls");

		description.append("\n\n**When to use:**");
        description.append("\n- Before calling ").append(JShellMcpTool.TOOL_NAME).append(" for execution");
        description.append("\n- After calling ").append(JShellManualMcpTool.TOOL_NAME)
            .append(" to get a scenario-specific template");
        description.append("\n- To check available bindings");
        description.append("\n- To get a fresh `repl_session_id` for JShell and JShellReflection calls");

        description.append("\n\n**Usage:**");
        description.append("\n- Takes no parameters");
        description.append("\n- Does not accept `code`, `scope`, `request_description`, or `response_description`");
        description.append("\n- Always creates a new session or returns a pre-warmed fresh session");
        description.append("\n- Returns: `repl_session_id` and available bindings");
        description.append("\n- Use the returned `repl_session_id` in subsequent ")
            .append(JShellMcpTool.TOOL_NAME)
            .append(" and ")
            .append(JShellReflectionMcpTool.TOOL_NAME)
            .append(" calls");
        description.append("\n- Pass `scope`, descriptions, and code only to ")
            .append(JShellMcpTool.TOOL_NAME);
        description.append("\n- Available JShell scopes from binding providers: ")
            .append(bindingProviders.stream()
                .map(IJShellBindingProvider::getScope)
                .filter(scope -> scope != null && !scope.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", ")));

		description.append("\n\n### Available bindings:");
		description.append("\nPre-configured objects available in JShell sessions:");
		if (!bindingProviders.isEmpty())
		{
			for (var provider : bindingProviders)
			{
				var descriptions = provider.getBindings();
				if (!descriptions.isEmpty())
				{
					description.append("\n");
					for (var entry : descriptions.entrySet())
					{
                        var bindingName = entry.getKey();
                        var bindingInfo = entry.getValue();
                        var bindingDesc = bindingInfo.getDescription();
                        var bindingExample = bindingInfo.getExample();
                        var bindingRestriction = bindingInfo.getRestriction();

						description.append("\n**`").append(bindingName).append("`**");
						description.append("\n").append(bindingDesc);

                        if (bindingRestriction != null && !bindingRestriction.isEmpty())
                        {
                            description.append("\n\n").append(bindingRestriction);
                        }

						if (bindingExample != null && !bindingExample.isEmpty())
						{
							description.append("\n```java\n").append(bindingExample).append("\n```");
						}
					}
				}
			}
		}

		description.append("\n\n**Examples:**");
        description.append("\n  Create session: Q: {}");
		description.append("\n  A: ").append(AnswerExample);

		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
        parameters.properties = new HashMap<>();
        parameters.required = new ArrayList<>();

		spec.function.parameters = parameters;
		return spec;
	}
}
