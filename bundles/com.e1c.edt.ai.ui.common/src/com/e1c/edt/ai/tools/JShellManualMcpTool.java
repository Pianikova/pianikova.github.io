/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * <p>
 * The tool description is generated dynamically from the registered scenarios
 * so the LLM sees a categorized list of available scenario IDs in the system
 * prompt — no discovery call required for the common case.
 * <p>
 * Response shape adapts to query precision:
 * <ul>
 *   <li>Exact match (score &ge; 180): returns only {@code matched_scenarios}.</li>
 *   <li>Fuzzy match (score 1–179): returns matches plus a compact
 *       {@code available_scenarios} ({id, category} pairs only).</li>
 *   <li>No match: returns top-3 near-misses as suggestions instead of throwing.</li>
 *   <li>Browse by {@code category}: returns category contents as matches,
 *       no available_scenarios.</li>
 * </ul>
 */
public class JShellManualMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "JShellManual"; //$NON-NLS-1$
    private static final int EXACT_MATCH_THRESHOLD = 180;
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final int NEAR_MISS_LIMIT = 3;

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

    @SuppressWarnings("nls")
    private ToolCallMessage execute(Request request, McpToolCall call, ToolCallMessageDetails details,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            throw new ToolException("Operation was cancelled before execution.", null, ToolErrorType.RETRYABLE);
        }

        var allEntries = collectAllEntries();
        var scope = normalizeScope(request.apiScope);
        var category = normalize(request.category);
        var query = normalize(request.scenario);
        var maxResults = request.maxResults != null && request.maxResults.intValue() > 0
            ? request.maxResults.intValue()
            : DEFAULT_MAX_RESULTS;

        var scoped = allEntries.stream()
            .filter(e -> "both".equals(scope) || scope.equals(e.getScope()))
            .filter(e -> category.isEmpty() || category.equals(e.getCategory()))
            .collect(Collectors.toList());

        var response = new Response();
        response.query = request.scenario;
        response.apiScope = scope;
        response.category = request.category;

        // Mode A: browse by category, no scenario query.
        if (query.isEmpty() && !category.isEmpty())
        {
            response.status = "browse";
            response.matchedScenarios = scoped.stream().map(this::toDetails).collect(Collectors.toList());
            details.responseMarkdown = buildResponseMarkdown(request, scoped, allEntries);
            return messageFactory.createMessage(this, call, json.serialize(response), details);
        }

        // Mode B: empty query — short directory of all entries (compact).
        if (query.isEmpty())
        {
            response.status = "directory";
            response.matchedScenarios = List.of();
            response.availableScenarios = scoped.stream().map(this::toCompactSummary).collect(Collectors.toList());
            details.responseMarkdown = buildResponseMarkdown(request, List.of(), allEntries);
            return messageFactory.createMessage(this, call, json.serialize(response), details);
        }

        // Mode C: scenario query with scoring.
        var ranked = scoped.stream()
            .map(e -> new RankedEntry(e, score(e, query)))
            .filter(r -> r.score > 0)
            .sorted(Comparator.comparingInt(RankedEntry::score).reversed()
                .thenComparing(r -> r.entry().getTitle()))
            .collect(Collectors.toList());

        if (ranked.isEmpty())
        {
            // Mode C.1: no matches — emit suggestions instead of throwing.
            response.status = "not_matched";
            response.matchedScenarios = List.of();
            response.suggestions = scoped.stream()
                .filter(e -> isCloseLexically(e, query))
                .limit(NEAR_MISS_LIMIT)
                .map(this::toCompactSummary)
                .collect(Collectors.toList());
            response.availableScenarios = scoped.stream().map(this::toCompactSummary).collect(Collectors.toList());
            details.responseMarkdown = buildResponseMarkdown(request, List.of(), allEntries);
            return messageFactory.createMessage(this, call, json.serialize(response), details);
        }

        var topScore = ranked.get(0).score;
        var matched = ranked.stream().limit(maxResults).map(RankedEntry::entry).collect(Collectors.toList());
        response.matchedScenarios = matched.stream().map(this::toDetails).collect(Collectors.toList());

        if (topScore >= EXACT_MATCH_THRESHOLD)
        {
            // Mode C.2: confident hit — drop directory entirely.
            response.status = "exact";
        }
        else
        {
            // Mode C.3: fuzzy — give compact directory so the LLM can refine.
            response.status = "fuzzy";
            response.availableScenarios = scoped.stream().map(this::toCompactSummary).collect(Collectors.toList());
        }

        details.responseMarkdown = buildResponseMarkdown(request, matched, allEntries);
        return messageFactory.createMessage(this, call, json.serialize(response), details);
    }

    private List<JShellManualEntry> collectAllEntries()
    {
        return manualProviders.stream()
            .flatMap(provider -> provider.getManualEntries().stream())
            .sorted(Comparator.comparing(JShellManualEntry::getScope).thenComparing(JShellManualEntry::getId))
            .collect(Collectors.toList());
    }

    private boolean isCloseLexically(JShellManualEntry entry, String query)
    {
        var id = normalize(entry.getId());
        var title = normalize(entry.getTitle());
        // word-overlap heuristic: any whitespace-separated token of query appears in id/title
        for (var token : query.split("\\s+|_")) //$NON-NLS-1$
        {
            if (token.length() < 3)
            {
                continue;
            }
            if (id.contains(token) || title.contains(token))
            {
                return true;
            }
        }
        return false;
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
        return Messages.JShellManualRequest;
    }

    private String buildResponseMarkdown(Request request, List<JShellManualEntry> matchedEntries,
        List<JShellManualEntry> allEntries)
    {
        return Messages.JShellManualResponse;
    }

    private Details toDetails(JShellManualEntry entry)
    {
        var details = new Details();
        details.id = entry.getId();
        details.scope = entry.getScope();
        details.category = entry.getCategory();
        details.title = entry.getTitle();
        details.summary = entry.getSummary();
        details.recommendedBindings = new ArrayList<>(entry.getRecommendedBindings());
        details.guideMarkdown = entry.getGuide();
        return details;
    }

    private CompactSummary toCompactSummary(JShellManualEntry entry)
    {
        var summary = new CompactSummary();
        summary.id = entry.getId();
        summary.category = entry.getCategory();
        return summary;
    }

    @SuppressWarnings("nls")
    private String normalizeScope(String scope)
    {
        var normalized = normalize(scope);
        if ("edt".equals(normalized) || "eclipse".equals(normalized))
        {
            return normalized;
        }
        return "both";
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

        spec.function.description = buildDescription();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var scenarioProperty = new McpToolCallProperty();
        scenarioProperty.type = "string";
        scenarioProperty.description = "Scenario id (e.g. `create_catalog`) or free-text topic. "
            + "Naming convention: <verb>_<entity>. See tool description for the categorized list of ids.";
        properties.put("scenario", scenarioProperty);

        var categoryProperty = new McpToolCallProperty();
        categoryProperty.type = "string";
        categoryProperty.description = "Optional category filter: create | edit | delete | composite | enhanced "
            + "| reference | configuration. Combine with empty `scenario` to browse a category.";
        properties.put("category", categoryProperty);

        var scopeProperty = new McpToolCallProperty();
        scopeProperty.type = "string";
        scopeProperty.description = "Optional scope filter: edt, eclipse, or both.";
        properties.put("api_scope", scopeProperty);

        var maxResultsProperty = new McpToolCallProperty();
        maxResultsProperty.type = "integer";
        maxResultsProperty.description = "Optional max number of matched scenarios. Default 5.";
        properties.put("max_results", maxResultsProperty);

        parameters.properties = properties;
        parameters.required = new ArrayList<>();
        spec.function.parameters = parameters;
        return spec;
    }

    @SuppressWarnings("nls")
    private String buildDescription()
    {
        var sb = new StringBuilder();
        sb.append("Provides scenario-oriented guidance for writing Java code for ").append(JShellMcpTool.TOOL_NAME)
            .append(".\n\n");
        sb.append("You MUST use this tool before ").append(JShellMcpTool.TOOL_NAME)
            .append(" when working with EDT metadata API or Eclipse platform API.\n");
        sb.append("This manual gives workflow guidance, not proof of exact Java signatures. ")
            .append("Do not invent methods, overloads, enum constants, packages, or type names from the manual text. ")
            .append("Use ").append(JShellReflectionMcpTool.TOOL_NAME)
            .append(" to confirm exact API before executing JShell code.\n");
        sb.append("Use it for create/edit/delete/look-up scenarios, for workbench/editor/workspace code, and ")
            .append("for any code that uses bindings like `mdFactory`, `modelManager`, `projectManager`, ")
            .append("`workspaceRoot`, `workbench`, `resourceLookup`, or `fqnGenerator`.\n\n");

        sb.append("Naming convention: scenario ids are `<verb>_<entity>`, e.g. `create_catalog`, ")
            .append("`edit_information_register`, `delete_attribute`, `add_tabular_section`. ")
            .append("Pass the convention-matching id directly; you can also pass free text and the tool will fuzzy-match.\n\n");

        sb.append("Available scenarios by category:\n\n");
        sb.append(buildCategoryListing());

        sb.append("\n\nResponse shape:\n");
        sb.append("- `matched_scenarios`: hits with full guide_markdown.\n");
        sb.append("- `available_scenarios`: compact `{id, category}` directory; present only when match is fuzzy or absent.\n");
        sb.append("- `suggestions`: did-you-mean list when no match (instead of error).\n");
        sb.append("- `status`: exact | fuzzy | not_matched | browse | directory.\n\n");

        sb.append("Suggested workflow:\n");
        sb.append("1. Call ").append(TOOL_NAME).append(" with a convention-matching scenario id.\n");
        sb.append("2. If status is `not_matched` or `fuzzy`, refine using `suggestions` or `available_scenarios`.\n");
        sb.append("3. Create or reuse a session with ").append(JShellSessionMcpTool.TOOL_NAME).append(".\n");
        sb.append("4. Use ").append(JShellReflectionMcpTool.TOOL_NAME)
            .append(" to verify exact packages, types, enum constants, methods, fields, constructors, and signatures ")
            .append("for every API call that is not already proven by tool output.\n");
        sb.append("5. Execute the generated code with ").append(JShellMcpTool.TOOL_NAME).append(".\n");
        sb.append("6. After code changes project resources or metadata, call ").append(GetMarkersMcpTool.TOOL_NAME)
            .append(" for the affected project. Use `marker_type: \"problem\"` for build/validation issues, ")
            .append("`marker_type: \"1c\"` for 1C/BSL markers, and `marker_type: \"ai_marker\"` ")
            .append("for AIError/AIWarning/AIInfo markers.\n");
        sb.append("7. If ").append(JShellMcpTool.TOOL_NAME)
            .append(" returns an EDT/Eclipse preflight error, return to ").append(TOOL_NAME)
            .append(" with a better matching scenario id.\n");
        return sb.toString();
    }

    @SuppressWarnings("nls")
    private String buildCategoryListing()
    {
        var entries = collectAllEntries();
        var byCategory = new LinkedHashMap<String, List<String>>();
        // canonical category order
        for (var c : List.of("create", "edit", "delete", "composite", "enhanced", "reference", "configuration", "misc"))
        {
            byCategory.put(c, new ArrayList<>());
        }
        for (var e : entries)
        {
            var c = e.getCategory();
            byCategory.computeIfAbsent(c, k -> new ArrayList<>()).add(e.getId());
        }
        var sb = new StringBuilder();
        for (var bucket : byCategory.entrySet())
        {
            var ids = bucket.getValue();
            if (ids.isEmpty())
            {
                continue;
            }
            sb.append("- **").append(bucket.getKey()).append("** (")
                .append(ids.size()).append("): ");
            sb.append(String.join(", ", ids.stream().sorted().limit(12).collect(Collectors.toList())));
            if (ids.size() > 12)
            {
                sb.append(", … (").append(ids.size() - 12).append(" more)");
            }
            sb.append("\n");
        }
        return sb.toString();
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

        @SerializedName("category")
        public String category;

        @SerializedName("max_results")
        public Integer maxResults;
    }

    private static class Response
    {
        @SerializedName("status")
        public String status;

        @SerializedName("query")
        public String query;

        @SerializedName("api_scope")
        public String apiScope;

        @SerializedName("category")
        public String category;

        @SerializedName("matched_scenarios")
        public List<Details> matchedScenarios;

        @SerializedName("suggestions")
        public List<CompactSummary> suggestions;

        @SerializedName("available_scenarios")
        public List<CompactSummary> availableScenarios;
    }

    private static class CompactSummary
    {
        @SerializedName("id")
        public String id;

        @SerializedName("category")
        public String category;
    }

    private static class Details
    {
        @SerializedName("id")
        public String id;

        @SerializedName("scope")
        public String scope;

        @SerializedName("category")
        public String category;

        @SerializedName("title")
        public String title;

        @SerializedName("summary")
        public String summary;

        @SerializedName("recommended_bindings")
        public List<String> recommendedBindings;

        @SerializedName("guide_markdown")
        public String guideMarkdown;
    }
}
