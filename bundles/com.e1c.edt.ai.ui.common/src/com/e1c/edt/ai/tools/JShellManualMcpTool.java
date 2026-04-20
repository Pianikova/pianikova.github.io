/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

/**
 * Scenario-oriented manual for writing JShell code.
 */
public class JShellManualMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "JShellManual"; //$NON-NLS-1$

    private final IJson json;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final Set<IJShellManualProvider> manualProviders;
    private final IMarkdownUtils markdownUtils;
    private final McpToolCallSpecification specification;

    @Inject
    public JShellManualMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Set<IJShellManualProvider> manualProviders, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(manualProviders);
        Preconditions.checkNotNull(markdownUtils);

        this.json = json;
        this.messageFactory = messageFactory;
        this.manualProviders = manualProviders;
        this.markdownUtils = markdownUtils;
        this.specification = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return true;
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return specification;
    }

    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = false;

        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException(
                "Cannot deserialize arguments. Use {} to list scenarios or provide {\"scenario\":\"create_catalog\"}."); //$NON-NLS-1$
        }

        var request = optionalRequest.get();
        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = buildRequestMarkdown(request);
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        return CompletableFuture.supplyAsync(() -> execute(request, call, details, cancellationToken));
    }

    private ToolCallMessage execute(Request request, McpToolCall call, ToolCallMessageDetails details,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            throw new ToolException("Operation was cancelled before execution.", null, ToolErrorType.RETRYABLE); //$NON-NLS-1$
        }

        var allEntries = manualProviders.stream()
            .flatMap(provider -> provider.getManualEntries().stream())
            .sorted(Comparator.comparing(JShellManualEntry::getScope).thenComparing(JShellManualEntry::getId))
            .collect(Collectors.toList());

        var matchedEntries = findMatches(allEntries, request);
        if (request.scenario != null && !request.scenario.isBlank() && matchedEntries.isEmpty())
        {
            throw new ToolException("No JShell manual scenario matched: " + request.scenario); //$NON-NLS-1$
        }

        var response = new Response();
        response.query = request.scenario;
        response.apiScope = normalizeScope(request.apiScope);
        response.availableScenarios = allEntries.stream().map(this::toSummary).collect(Collectors.toList());
        response.matchedScenarios = matchedEntries.stream().map(this::toDetails).collect(Collectors.toList());

        details.responseMarkdown = buildResponseMarkdown(request, matchedEntries, allEntries);
        return messageFactory.createMessage(this, call, json.serialize(response), details);
    }

    private List<JShellManualEntry> findMatches(List<JShellManualEntry> allEntries, Request request)
    {
        var scope = normalizeScope(request.apiScope);
        var maxResults = request.maxResults != null && request.maxResults.intValue() > 0
            ? request.maxResults.intValue()
            : 5;
        var query = normalize(request.scenario);

        var filtered = allEntries.stream()
            .filter(entry -> "both".equals(scope) || scope.equals(entry.getScope())) //$NON-NLS-1$
            .collect(Collectors.toList());
        if (query.isEmpty())
        {
            return filtered.stream().limit(maxResults).collect(Collectors.toList());
        }

        return filtered.stream()
            .map(entry -> new RankedEntry(entry, score(entry, query)))
            .filter(rank -> rank.score > 0)
            .sorted(Comparator.comparingInt(RankedEntry::score).reversed()
                .thenComparing(rank -> rank.entry().getTitle()))
            .limit(maxResults)
            .map(RankedEntry::entry)
            .collect(Collectors.toList());
    }

    private int score(JShellManualEntry entry, String query)
    {
        if (entry.getId().equalsIgnoreCase(query))
        {
            return 200;
        }

        var title = normalize(entry.getTitle());
        var summary = normalize(entry.getSummary());
        if (title.equals(query))
        {
            return 180;
        }
        if (title.contains(query))
        {
            return 140;
        }
        if (summary.contains(query))
        {
            return 80;
        }

        var score = 0;
        for (var keyword : entry.getKeywords())
        {
            var normalizedKeyword = normalize(keyword);
            if (normalizedKeyword.equals(query))
            {
                score = Math.max(score, 160);
            }
            else if (normalizedKeyword.contains(query) || query.contains(normalizedKeyword))
            {
                score = Math.max(score, 110);
            }
        }
        return score;
    }

    private String buildRequestMarkdown(Request request)
    {
        if (request.scenario == null || request.scenario.isBlank())
        {
            return "Collecting JShell manual scenarios."; //$NON-NLS-1$
        }
        return "Preparing JShell manual for scenario `" + request.scenario + "`."; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @SuppressWarnings("nls")
    private String buildResponseMarkdown(Request request, List<JShellManualEntry> matchedEntries,
        List<JShellManualEntry> allEntries)
    {
        var markdown = new StringBuilder();
        var scope = normalizeScope(request.apiScope);
        if (request.scenario == null || request.scenario.isBlank())
        {
            markdown.append("## JShellManual\n\n");
            markdown.append("Use this tool before `").append(JShellMcpTool.TOOL_NAME)
                .append("` when you need scenario-specific guidance for Eclipse or EDT code.\n\n");
            markdown.append("### Available scenarios\n");
            for (var entry : allEntries)
            {
                if ("both".equals(scope) || scope.equals(entry.getScope()))
                {
                    markdown.append("- `").append(entry.getId()).append("` (`").append(entry.getScope()).append("`): ")
                        .append(entry.getSummary()).append("\n");
                }
            }
            markdown.append("\nExample: `{\"scenario\":\"create_catalog\"}`");
            return markdown.toString();
        }

        markdown.append("## JShellManual Results\n\n");
        for (var entry : matchedEntries)
        {
            markdown.append("### ").append(markdownUtils.escapeForMarkdown(entry.getTitle())).append("\n");
            markdown.append("- Scenario: `").append(entry.getId()).append("`\n");
            markdown.append("- Scope: `").append(entry.getScope()).append("`\n");
            if (!entry.getRecommendedBindings().isEmpty())
            {
                markdown.append("- Recommended bindings: ");
                markdown.append(entry.getRecommendedBindings().stream()
                    .map(binding -> "`" + binding + "`")
                    .collect(Collectors.joining(", ")));
                markdown.append("\n");
            }
            markdown.append("\n");
            markdown.append(entry.getGuide()).append("\n\n");
        }
        return markdown.toString();
    }

    private Summary toSummary(JShellManualEntry entry)
    {
        var summary = new Summary();
        summary.id = entry.getId();
        summary.scope = entry.getScope();
        summary.title = entry.getTitle();
        summary.summary = entry.getSummary();
        return summary;
    }

    private Details toDetails(JShellManualEntry entry)
    {
        var details = new Details();
        details.id = entry.getId();
        details.scope = entry.getScope();
        details.title = entry.getTitle();
        details.summary = entry.getSummary();
        details.recommendedBindings = new ArrayList<>(entry.getRecommendedBindings());
        details.guideMarkdown = entry.getGuide();
        return details;
    }

    private String normalizeScope(String scope)
    {
        var normalized = normalize(scope);
        if ("edt".equals(normalized) || "eclipse".equals(normalized))
        {
            return normalized;
        }
        return "both"; //$NON-NLS-1$
    }

    private String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Provides scenario-oriented guidance for writing Java code for ")
            .append(JShellMcpTool.TOOL_NAME).append(".\n\n");
        description.append("You MUST use this tool before ").append(JShellMcpTool.TOOL_NAME)
            .append(" when working with EDT metadata API or Eclipse platform API.\n");
        description.append("Use it for create/edit/delete/look-up scenarios, for workbench/editor/workspace code, and for any code that uses bindings like `mdFactory`, `modelManager`, `projectManager`, `workspaceRoot`, `workbench`, `resourceLookup`, or `fqnGenerator`.\n\n");
        description.append("What it returns:\n");
        description.append("- recommended bindings\n");
        description.append("- workflow and safety rules\n");
        description.append("- scenario-specific code templates\n");
        description.append("- common mistakes and fixes\n\n");
        description.append("Suggested workflow:\n");
        description.append("1. Call ").append(TOOL_NAME).append(" with a scenario like `create_catalog`.\n");
        description.append("2. Create or reuse a session with ").append(JShellSessionMcpTool.TOOL_NAME).append(".\n");
        description.append("3. Execute the generated code with ").append(JShellMcpTool.TOOL_NAME).append(".\n");
        description.append("4. If ").append(JShellMcpTool.TOOL_NAME)
            .append(" returns an EDT/Eclipse preflight error, come back to ").append(TOOL_NAME)
            .append(" with a better matching scenario.\n");

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var scenarioProperty = new McpToolCallProperty();
        scenarioProperty.type = "string";
        scenarioProperty.description =
            "Optional scenario identifier or topic, for example create_catalog, document attribute, active editor."; //$NON-NLS-1$
        properties.put("scenario", scenarioProperty); //$NON-NLS-1$

        var scopeProperty = new McpToolCallProperty();
        scopeProperty.type = "string";
        scopeProperty.description = "Optional scope filter: edt, eclipse, or both."; //$NON-NLS-1$
        properties.put("api_scope", scopeProperty); //$NON-NLS-1$

        var maxResultsProperty = new McpToolCallProperty();
        maxResultsProperty.type = "integer";
        maxResultsProperty.description = "Optional maximum number of matched scenarios to return."; //$NON-NLS-1$
        properties.put("max_results", maxResultsProperty); //$NON-NLS-1$

        parameters.properties = properties;
        parameters.required = new ArrayList<>();
        spec.function.parameters = parameters;
        return spec;
    }

    private static class RankedEntry
    {
        private final JShellManualEntry entry;
        private final int score;

        private RankedEntry(JShellManualEntry entry, int score)
        {
            this.entry = entry;
            this.score = score;
        }

        private JShellManualEntry entry()
        {
            return entry;
        }

        private int score()
        {
            return score;
        }
    }

    private static class Request
    {
        @SerializedName("scenario")
        public String scenario;

        @SerializedName("api_scope")
        public String apiScope;

        @SerializedName("max_results")
        public Integer maxResults;
    }

    private static class Response
    {
        @SerializedName("query")
        public String query;

        @SerializedName("api_scope")
        public String apiScope;

        @SerializedName("matched_scenarios")
        public List<Details> matchedScenarios;

        @SerializedName("available_scenarios")
        public List<Summary> availableScenarios;
    }

    private static class Summary
    {
        @SerializedName("id")
        public String id;

        @SerializedName("scope")
        public String scope;

        @SerializedName("title")
        public String title;

        @SerializedName("summary")
        public String summary;
    }

    private static class Details extends Summary
    {
        @SerializedName("recommended_bindings")
        public List<String> recommendedBindings;

        @SerializedName("guide_markdown")
        public String guideMarkdown;
    }
}
