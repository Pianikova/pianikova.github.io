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
import com.e1c.edt.ai.ui.IDispatcher;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class JShellMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "JShell"; //$NON-NLS-1$

	private static String QuestionExample =
        "{\"code\":\"var window = workbench.getActiveWorkbenchWindow();\\nif (window != null) { System.out.println(\\\"Active window: \\\" + window.getShell().getText()); }\",\"repl_session_id\":123}"; //$NON-NLS-1$

	private static String AnswerExample =
        "{\"return_value\":null,\"repl_session_id\":123,\"std_out\":\"Active window: Eclipse\\n\",\"std_err\":\"\"}"; //$NON-NLS-1$

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final IJShellSessionManager sessions;
	private final Set<IJShellBindingProvider> bindingProviders;
	private final IMarkdownUtils markdownUtils;
	private final IRestrictedTypesProvider restrictedTypesProvider;
    private final IDispatcher dispatcher;

	@Inject
	public JShellMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
		IJShellSessionManager sessions, Set<IJShellBindingProvider> bindingProviders, IMarkdownUtils markdownUtils,
        IRestrictedTypesProvider restrictedTypesProvider, IDispatcher dispatcher)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(sessions);
		Preconditions.checkNotNull(bindingProviders);
		Preconditions.checkNotNull(markdownUtils);
		Preconditions.checkNotNull(restrictedTypesProvider);
        Preconditions.checkNotNull(dispatcher);

		this.json = json;
		this.messageFactory = messageFactory;
		this.sessions = sessions;
		this.bindingProviders = bindingProviders;
		this.markdownUtils = markdownUtils;
		this.restrictedTypesProvider = restrictedTypesProvider;
        this.dispatcher = dispatcher;
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

        return CompletableFuture.supplyAsync(() -> executeCode(request, call, details, cancellationToken));
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
            var session = sessions.getSession(request.sessionId);
			if (session == null)
			{
				throw new ToolException(
					"Session not found. Use " + JShellSessionMcpTool.TOOL_NAME + " tool to create a session first.",
					null,
					ToolErrorType.RETRYABLE);
			}

            var optionalResult = dispatcher.dispatch(() -> session.execute(request.code));
            if (optionalResult.isEmpty())
            {
                throw new ToolException("Can't execute code.", null, ToolErrorType.RETRYABLE);
            }

            var result = optionalResult.get();
			var content = json.serialize(result);
			var responseMarkdown = buildResponseMarkdown(request.code, result);
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
		if (!result.compilationErrors.isEmpty())
		{
			md.append(Messages.JShellErrorTemplate);
		}
		else
		{
			md.append(Messages.JShellExecutedTemplate);
		}

		md.append("\n\n");
		md.append("```java\n").append(code).append("\n```\n");
		if (result.stdOut != null && !result.stdOut.isEmpty())
		{
            md.append("```\n").append(result.stdOut).append("\n```\n");
		}

		return md.toString();
	}

	@SuppressWarnings("nls")
	private String getBindingVariableNamesExample()
	{
		var bindingNames = new ArrayList<String>();
        for (var provider : bindingProviders)
		{
            var bindings = provider.getBindings();
			if (bindings != null)
			{
				bindingNames.addAll(bindings.keySet());
			}
		}

		if (bindingNames.isEmpty())
		{
			return "";
		}

		// Sort and limit to first 3-4 variable names
		bindingNames.sort(String::compareTo);
		var exampleNames = new ArrayList<String>();
		for (int i = 0; i < Math.min(3, bindingNames.size()); i++)
		{
			exampleNames.add("`" + bindingNames.get(i) + "`");
		}

		return String.join(", ", exampleNames);
	}

    private String getProviderUseCases(IJShellBindingProvider provider)
    {
        return provider.getUseCases();
    }

    @SuppressWarnings("nls")
	private McpToolCallSpecification createSpecification()
	{
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
        description.append("Executes Java code using JShell REPL. Preserves state across executions.");
		description.append("\n\n**IMPORTANT:**");
        description.append("\n- You MUST call ").append(JShellSessionMcpTool.TOOL_NAME).append(" tool first to create or get a valid session ID");
        description.append("\n- This tool will fail with error if you provide an invalid or non-existent session ID");

		description.append("\n\n**When to use:**");
		description.append("\n- Use when other IDE tools (" + GitMcpTool.TOOL_NAME + " , " + ReadMcpTool.TOOL_NAME + ", etc.) cannot accomplish the task");

		description.append("\n\n**Key requirements:**");
		description.append("\n- Use ONLY complete statements with `;` (e.g., `int x = 10;`)");
		description.append("\n- NO expressions like `x`, `2+2` - use `System.out.println()` instead");
        description.append("\n- Output MUST be in main thread for result capture");
        description.append("\n- DO NOT use `return;` - always return a value (e.g., `return \"\";`)");

		description.append("\n\n**Available bindings:**");
		if (!bindingProviders.isEmpty())
		{
            description.append("\n- Pre-configured objects are available in JShell");
			description.append("\n- See ").append(JShellSessionMcpTool.TOOL_NAME).append(" tool for detailed binding documentation with examples");
			String bindingExamples = getBindingVariableNamesExample();
			description.append("\\n- **IMPORTANT:** Bindings are already available as variables (e.g., ").append(bindingExamples).append("). DO NOT use `JShellObjectBridge.retrieve()` - it's for internal use only.");

            description.append("\n\n**Binding providers:**");
            for (var provider : bindingProviders)
            {
                var descriptions = provider.getBindings();
                if (!descriptions.isEmpty())
                {
                    description.append("\n\n**");
                    description.append(provider.getDescription());
                    description.append("**");
                    var useCases = getProviderUseCases(provider);
                    if (!useCases.isEmpty())
                    {
                        description.append("\n\n");
                        description.append(useCases);
                    }

                    description.append("\n\nAvailable bindings:");
                    var count = 0;
                    for (var entry : descriptions.entrySet())
                    {
                        if (count < 3)
                        {
                            var bindingName = entry.getKey();
                            var bindingInfo = entry.getValue();
                            var bindingRestriction = bindingInfo.getRestriction();
                            description.append("\n- `")
                                .append(bindingName)
                                .append("`: ")
                                .append(bindingInfo.getDescription());
                            if (bindingRestriction != null && !bindingRestriction.isEmpty())
                            {
                                description.append(" (has restriction)");
                            }
                            count++;
                        }
                        else
                        {
                            description.append("\n- ... and more (see JShellSession tool)");
                            break;
                        }
                    }
                }
            }
		}

		description.append("\n\n**Workflow:**");
		description.append("\n1. Call ").append(JShellSessionMcpTool.TOOL_NAME).append(" to create/get session and ID");
        description.append("\n2. Use ").append(TOOL_NAME).append(" with that ID to execute code");
		description.append("\n3. Reuse same ID to maintain state");

		// Add restricted types information
        var restrictedTypes = restrictedTypesProvider.getRestrictedTypes();
		if (!restrictedTypes.isEmpty())
		{
			description.append("\n\n**⚠️ RESTRICTED TYPES (security restrictions):**");
			description.append("\nThe following types are NOT ALLOWED:");
            for (var type : restrictedTypes.stream().sorted().collect(Collectors.toList()))
			{
				description.append("\n- `").append(type).append("`");
			}
		}

		description.append("\n\n**Parameters:**");
		description.append("\n- `code` (required): Complete Java statements ending with `;`");
		description.append("\n- `repl_session_id` (required): Session ID from JShellSession tool");

		description.append("\n\nExample: `").append(QuestionExample).append("`");
		description.append("\nResponse: `").append(AnswerExample).append("`");

		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
		var properties = new HashMap<String, McpToolCallProperty>();

		var codeProp = new McpToolCallProperty();
		codeProp.type = "string";
		codeProp.description = "Java code to execute (required)";
		properties.put("code", codeProp);

		var sessionIdProp = new McpToolCallProperty();
        sessionIdProp.type = "integer";
		sessionIdProp.description = "Session ID from " + JShellSessionMcpTool.TOOL_NAME + " tool (required)";
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
        public int sessionId;
	}
}


