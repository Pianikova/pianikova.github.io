/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
        "{\"scope\":\"eclipse\",\"request_description\":\"Read active Eclipse window title.\",\"response_description\":\"Active Eclipse window title was printed.\",\"code\":\"var window = workbench.getActiveWorkbenchWindow();\\nif (window != null) { System.out.println(\\\"Active window: \\\" + window.getShell().getText()); }\",\"repl_session_id\":\"550e8400-e29b-41d4-a716-446655440000\",\"manual_ids\":[\"active_workbench\"]}"; //$NON-NLS-1$

	private static String AnswerExample =
        "{\"return_value\":null,\"repl_session_id\":\"550e8400-e29b-41d4-a716-446655440000\",\"std_out\":\"Active window: Eclipse\\n\",\"std_err\":\"\"}"; //$NON-NLS-1$

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final IJShellSessionManager sessions;
	private final Set<IJShellBindingProvider> bindingProviders;
	private final IRestrictedTypesProvider restrictedTypesProvider;
    private final IJShellReflectionQuerySuggester reflectionQuerySuggester;
    private final IDispatcher dispatcher;
    private final Set<IJShellManualProvider> manualProviders;
    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

	@Inject
	public JShellMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
		IJShellSessionManager sessions, Set<IJShellBindingProvider> bindingProviders,
        IRestrictedTypesProvider restrictedTypesProvider, IJShellReflectionQuerySuggester reflectionQuerySuggester,
        IDispatcher dispatcher, Set<IJShellManualProvider> manualProviders)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(sessions);
		Preconditions.checkNotNull(bindingProviders);
		Preconditions.checkNotNull(restrictedTypesProvider);
        Preconditions.checkNotNull(reflectionQuerySuggester);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(manualProviders);

		this.json = json;
		this.messageFactory = messageFactory;
		this.sessions = sessions;
		this.bindingProviders = bindingProviders;
		this.restrictedTypesProvider = restrictedTypesProvider;
        this.reflectionQuerySuggester = reflectionQuerySuggester;
        this.dispatcher = dispatcher;
        this.manualProviders = manualProviders;
		this.spec = createSpecification();
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
        validateRequest(request);
		if (call.callKind == ToolCallKind.RENDER)
		{
            details.requestMarkdown = request.requestDescription + buildCodeDetailsBlock(request.code);
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

            var sessionLock = sessionLocks.computeIfAbsent(session.getSessionId(), id -> new Object());
            synchronized (sessionLock)
            {
                var optionalResult = dispatcher.dispatch(() -> session.execute(request.code));
                if (optionalResult.isEmpty())
                {
                    throw new ToolException("Can't execute code.", null, ToolErrorType.RETRYABLE);
                }

                var result = optionalResult.get();
                if (!result.compilationErrors.isEmpty())
                {
                    result.suggestedReflectionQueries =
                        reflectionQuerySuggester.suggestForCompilationErrors(request.code, result.compilationErrors, 12);
                }
                result.requiredNextStep = buildRequiredNextStep(request, result);
                var content = json.serialize(result);
                var responseMarkdown = buildResponseMarkdown(request, result);
                details.responseMarkdown = responseMarkdown;
                return messageFactory.createMessage(this, call, content, details);
            }
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
    private void validateRequest(Request request)
    {
        if (isBlank(request.sessionId))
        {
            throw new ToolException("Missing required parameter `repl_session_id`. Use "
                + JShellSessionMcpTool.TOOL_NAME + " tool to create a session first.", null, ToolErrorType.RETRYABLE);
        }
        if (isBlank(request.code))
        {
            throw new ToolException("Missing required parameter `code`.", null, ToolErrorType.RETRYABLE);
        }
        if (isBlank(request.scope))
        {
            throw new ToolException("Missing required parameter `scope`. Allowed values: "
                + String.join(", ", getAllowedScopes()) + ".", null, ToolErrorType.RETRYABLE);
        }
        if (isBlank(request.requestDescription))
        {
            throw new ToolException("Missing required parameter `request_description`.", null, ToolErrorType.RETRYABLE);
        }
        if (isBlank(request.responseDescription))
        {
            throw new ToolException("Missing required parameter `response_description`.", null, ToolErrorType.RETRYABLE);
        }
        if (!getAllowedScopes().contains(normalizeScope(request.scope)))
        {
            throw new ToolException("Unsupported `scope`: " + request.scope + ". Allowed values: "
                + String.join(", ", getAllowedScopes()) + ".", null, ToolErrorType.RETRYABLE);
        }
        validateManualIds(request);
    }

    @SuppressWarnings("nls")
    private void validateManualIds(Request request)
    {
        if (request.manualIds == null || request.manualIds.isEmpty())
        {
            throw new ToolException(
                "Missing required parameter `manual_ids`. Call " + JShellManualMcpTool.TOOL_NAME
                    + " first and pass back the `manual_id` value(s) you received."
                    + " This parameter forces you to load the scenario-specific guide"
                    + " before any EDT metadata or project CRUD via " + TOOL_NAME + ".",
                null, ToolErrorType.RETRYABLE);
        }
        var knownIds = manualProviders.stream()
            .flatMap(provider -> provider.getManualEntries().stream())
            .map(JShellManualEntry::getId)
            .collect(Collectors.toSet());
        var unknown = request.manualIds.stream()
            .filter(id -> id == null || id.isBlank() || !knownIds.contains(id))
            .collect(Collectors.toList());
        if (!unknown.isEmpty())
        {
            throw new ToolException(
                "Unknown or blank `manual_ids`: " + unknown
                    + ". Each id must be an exact `manual_id` returned by " + JShellManualMcpTool.TOOL_NAME
                    + ". Call " + JShellManualMcpTool.TOOL_NAME
                    + " to obtain the catalog of valid `manual_id` values, then retry.",
                null, ToolErrorType.RETRYABLE);
        }
    }

    private boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private String normalizeScope(String scope)
    {
        return scope == null ? "" : scope.trim().toLowerCase(java.util.Locale.ROOT); //$NON-NLS-1$
    }

    private ArrayList<String> getAllowedScopes()
    {
        return bindingProviders.stream()
            .map(IJShellBindingProvider::getScope)
            .filter(scope -> scope != null && !scope.isBlank())
            .map(this::normalizeScope)
            .distinct()
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
    }

	private String buildResponseMarkdown(Request request, JShellExecutionResult result)
	{
        if (hasExecutionErrors(result))
        {
            var md = new StringBuilder();
            md.append(Messages.JShellErrorTemplate);
            md.append(buildExecutionDetailsBlock(request.code, result));
            return md.toString();
        }

		var md = new StringBuilder();
        md.append(request.responseDescription);
        md.append(buildExecutionDetailsBlock(request.code, result));

		return md.toString();
	}

    @SuppressWarnings("nls")
    private String buildExecutionDetailsBlock(String code, JShellExecutionResult result)
    {
        var sb = new StringBuilder();
        sb.append("\n\n**Details**\n\n");
        sb.append("```java\n").append(code).append("\n```");
        if (result != null && result.stdOut != null && !result.stdOut.isEmpty())
        {
            sb.append("\n\n```text\n").append(result.stdOut).append("\n```");
        }
        if (result != null && result.stdErr != null && !result.stdErr.isEmpty())
        {
            sb.append("\n\n```text\n").append(result.stdErr).append("\n```");
        }
        return sb.toString();
    }

    @SuppressWarnings("nls")
    private String buildCodeDetailsBlock(String code)
    {
        var sb = new StringBuilder();
        sb.append("\n\n**Details**\n\n");
        sb.append("```java\n").append(code).append("\n```");
        return sb.toString();
    }

    private boolean hasExecutionErrors(JShellExecutionResult result)
    {
        return result != null
            && (result.compilationErrors != null && !result.compilationErrors.isEmpty()
                || result.runtimeErrors != null && !result.runtimeErrors.isEmpty());
    }

    private String buildRequiredNextStep(Request request, JShellExecutionResult result)
    {
        if (hasExecutionErrors(result))
        {
            return null;
        }

        var context = new JShellExecutionContext();
        context.scope = normalizeScope(request.scope);
        context.requestDescription = request.requestDescription;
        context.responseDescription = request.responseDescription;
        context.code = request.code;
        context.result = result;

        var requiredNextStep = bindingProviders.stream()
            .filter(provider -> context.scope.equals(normalizeScope(provider.getScope())))
            .map(provider -> provider.getRequiredNextStep(context))
            .filter(nextStep -> nextStep != null && !nextStep.isBlank())
            .collect(Collectors.joining("\n\n")); //$NON-NLS-1$
        return requiredNextStep.isBlank() ? null : requiredNextStep;
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
        description.append("\n- The `manual_ids` parameter is required and validated against the live scenario catalog. ")
            .append("Call ").append(JShellManualMcpTool.TOOL_NAME)
            .append(" first, read the `manual_id` from its response, and pass it back here. Calls with empty `manual_ids`")
            .append(" or unknown ids are rejected before execution. This applies to every EDT metadata or project CRUD")
            .append(" via ").append(TOOL_NAME)
            .append(" — create, edit, rename, delete, move, attach, detach, link, unlink — on Catalog,")
            .append(" Document, Enum, register, ChartOf*, BusinessProcess, Task, ExchangePlan, CommonModule,")
            .append(" CommonAttribute, Subsystem, DefinedType, Constant, configuration project, configuration, etc.")
            .append(" Do not improvise rename/delete from a `create_*` pattern: load `rename_object` or the matching")
            .append(" delete-refactoring scenario first");
        description.append("\n- Use ").append(JShellReflectionMcpTool.TOOL_NAME)
            .append(" before execution only when unsure about packages, types, enum constants, methods, fields, constructors, or signatures not already covered by an exact ")
            .append(JShellManualMcpTool.TOOL_NAME).append(" guide");
        description.append("\n- NEVER invent Java API calls, method overloads, enum constants, package names, or type names. ")
            .append("If the exact API is not already proven by tool output, call ")
            .append(JShellReflectionMcpTool.TOOL_NAME).append(" first");
        description.append("\n- If a previous execution failed with `cannot find symbol`, `method not found`, or `package does not exist`, use ")
            .append(JShellReflectionMcpTool.TOOL_NAME).append(" with `suggested_reflection_queries` instead of guessing APIs");
        description.append("\n- Choose `scope` from the allowed values listed in the `scope` parameter. ")
            .append("Scope-specific required next steps are returned in JSON field `required_next_step` by the matching `IJShellBindingProvider`");
        description.append("\n- Scope providers pre-import common safe API classes. Do not add redundant wildcard imports; ")
            .append("when a missing type is truly needed, add an explicit import or use a fully-qualified class name");
        description.append("\n- You MUST call ").append(JShellSessionMcpTool.TOOL_NAME).append(" tool first to create or get a valid session ID");
        description.append("\n- This tool will fail with error if you provide an invalid or non-existent session ID");

		description.append("\n\n**When to use:**");
		description.append("\n- Use when other IDE tools (" + GitMcpTool.TOOL_NAME + " , " + ReadMcpTool.TOOL_NAME + ", etc.) cannot accomplish the task");

        description.append("\n\n**IDE commands (PREFERRED path):**");
        description.append("\n- IDE commands are executed by JAVA CODE via this tool (bindings `commandService`/`handlerService`),");
        description.append(" discovered via " + GetCommandCategoriesMcpTool.TOOL_NAME + " and " + GetCommandsMcpTool.TOOL_NAME + ".");
        description.append("\n- Before executing a command, call ").append(JShellManualMcpTool.TOOL_NAME)
            .append(" and prefer the matching command scenario instead of improvising code:");
        description.append("\n  - workflow of discovering and calling any command -> `eclipse_command_workflow`; invocation patterns -> `execute_command`");
        description.append("\n  - save/close/refresh/build -> `eclipse_commands_files_build`");
        description.append("\n  - undo/copy/paste/rename/find/views -> `eclipse_commands_edit_navigate`");
        description.append("\n  - run last/debug/terminate -> `eclipse_commands_run_debug`");
        description.append("\n  - in 1C:EDT (scope `edt`): launch 1C client -> `edt_commands_launch`; infobase update/dump/restore/Designer -> `edt_commands_infobase`;");
        description.append(" XML/external export-import -> `edt_commands_export_import`; validation -> `edt_commands_validate`");

		description.append("\n\n**Key requirements:**");
		description.append("\n- Use ONLY complete statements with `;` (e.g., `int x = 10;`)");
		description.append("\n- NO expressions like `x`, `2+2` - use `System.out.println()` instead");
        description.append("\n- Output MUST be in main thread for result capture");
        description.append("\n- DO NOT use without a value `return;` - always return any value (e.g., `return null;`)");
        description.append("\n- Non-trivial snippets SHOULD be wrapped in `{ ... }` to keep local variables local in persistent JShell sessions");
        description.append("\n- Calls with the same `repl_session_id` are executed sequentially; wait for the previous result before relying on changed session state");
        description.append("\n- `request_description` describes what will be done and is shown as request markdown");
        description.append("\n- `response_description` describes what was done and is shown as response markdown");
        description.append("\n- `request_description` and `response_description` must match the actual user request and executed code");

		description.append("\n\n**Available bindings:**");
		if (!bindingProviders.isEmpty())
		{
            description.append("\n- Pre-configured objects are available in JShell");
			description.append("\n- See ").append(JShellSessionMcpTool.TOOL_NAME)
                .append(" for the full binding list, descriptions, restrictions, and examples");
			String bindingExamples = getBindingVariableNamesExample();
            if (!bindingExamples.isEmpty())
            {
                description.append("\n- Bindings are already available as variables, for example ")
                    .append(bindingExamples).append(".");
            }
            description.append("\n- DO NOT use `JShellObjectBridge.retrieve()` - it is for internal use only.");
		}

		description.append("\n\n**Workflow:**");
        description.append("\n1. **MUST** call ").append(JShellManualMcpTool.TOOL_NAME)
            .append(" for every EDT metadata or project CRUD scenario before generating code (create, edit, rename, delete, move, attach, detach, link, unlink on any metadata object or configuration). For non-EDT-CRUD code, still call it when a scenario-specific guide is available");
		description.append("\n2. Call ").append(JShellSessionMcpTool.TOOL_NAME).append(" to create/get session and ID");
        description.append("\n3. If the manual exact guide does not cover needed APIs, call ").append(JShellReflectionMcpTool.TOOL_NAME)
            .append(" once with all uncertain Java API names/signatures before writing calls that depend on them");
        description.append("\n4. Use ").append(TOOL_NAME).append(" with that ID to execute code");
        description.append("\n5. Follow JSON field `required_next_step` when it is returned by the active binding provider");
		description.append("\n6. Reuse same ID to maintain state");

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
        description.append("\n- `scope` (required): Execution scope. Allowed values: ")
            .append(String.join(", ", getAllowedScopes()));
        description.append("\n- `request_description` (required): Short user-visible description of what will be done");
        description.append("\n- `response_description` (required): Short user-visible description of what was done");
		description.append("\n- `code` (required): Complete Java statements ending with `;`");
		description.append("\n- `repl_session_id` (required): Session ID from JShellSession tool");
        description.append("\n- `manual_ids` (required): Non-empty array of `manual_id` strings from ")
            .append(JShellManualMcpTool.TOOL_NAME)
            .append(". Validated against the live catalog — invented ids are rejected");

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

        var scopeProp = new McpToolCallProperty();
        scopeProp.type = "string";
        scopeProp.description = "Required execution scope. Allowed values: "
            + String.join(", ", getAllowedScopes())
            + ". Choose the scope that owns the bindings and workflow hints needed for this code.";
        properties.put("scope", scopeProp);

        var requestDescriptionProp = new McpToolCallProperty();
        requestDescriptionProp.type = "string";
        requestDescriptionProp.description = "Required user-visible request markdown: what will be done.";
        properties.put("request_description", requestDescriptionProp);

        var responseDescriptionProp = new McpToolCallProperty();
        responseDescriptionProp.type = "string";
        responseDescriptionProp.description = "Required user-visible response markdown: what was done. For EDT CRUD, include changed top-level entities and known .mdo paths.";
        properties.put("response_description", responseDescriptionProp);

		var sessionIdProp = new McpToolCallProperty();
        sessionIdProp.type = "string";
		sessionIdProp.description = "Session ID (UUID string) from " + JShellSessionMcpTool.TOOL_NAME + " tool (required)";
		properties.put("repl_session_id", sessionIdProp);

		var manualIdsProp = new McpToolCallProperty();
		manualIdsProp.type = "array";
		manualIdsProp.description = "Required non-empty array of `manual_id` strings returned by "
		    + JShellManualMcpTool.TOOL_NAME
		    + " for the scenario(s) you loaded for this call. Each value MUST be the exact `manual_id` from a real "
		    + JShellManualMcpTool.TOOL_NAME
		    + " response (it is validated against the live scenario catalog). Calls with empty `manual_ids` or unknown ids are rejected before execution. This parameter exists to force you to call "
		    + JShellManualMcpTool.TOOL_NAME
		    + " before any EDT metadata or project CRUD via " + TOOL_NAME
		    + " — create, edit, rename, delete, move, attach, detach, link, unlink. Do not invent ids.";
		properties.put("manual_ids", manualIdsProp);

		parameters.properties = properties;
		var required = new ArrayList<String>();
		required.add("code");
		required.add("repl_session_id");
        required.add("scope");
        required.add("request_description");
        required.add("response_description");
        required.add("manual_ids");
		parameters.required = required;

		spec.function.parameters = parameters;
		return spec;
	}

	private static class Request
	{
        @SerializedName("scope")
        public String scope;

        @SerializedName("request_description")
        public String requestDescription;

        @SerializedName("response_description")
        public String responseDescription;

		@SerializedName("code")
		public String code;

		@SerializedName("repl_session_id")
        public String sessionId;

		@SerializedName("manual_ids")
		public List<String> manualIds;
	}
}


