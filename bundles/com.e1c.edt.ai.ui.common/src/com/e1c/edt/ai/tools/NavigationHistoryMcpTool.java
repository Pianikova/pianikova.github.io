/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.INavigationHistory;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
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

public class NavigationHistoryMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "NavigationHistory"; //$NON-NLS-1$
    private static final int DEFAULT_MAX_ENTRIES = McpToolConstants.DEFAULT_MAX_NAVIGATION_ENTRIES;

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"max_entries\": 20\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"index\": 5,\n"
        + "    \"text\": \"Module.bsl - line 120\",\n"
        + "    \"project_name\": \"MyProject\",\n"
        + "    \"relative_file_path\": \"src/CommonModules/Module.bsl\",\n"
        + "    \"input\": \"Module.bsl\",\n"
        + "    \"is_current\": true\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IDispatcher dispatcher;
    private final IMarkdownUtils markdownUtils;

    @Inject
    public NavigationHistoryMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IDispatcher dispatcher, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(markdownUtils);
        this.json = json;
        this.messageFactory = messageFactory;
        this.dispatcher = dispatcher;
        this.markdownUtils = markdownUtils;
        spec = createSpecification();
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

    @SuppressWarnings({ "nls" })
    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = true;

        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        var maxEntries = request.maxEntries != null && request.maxEntries > 0 ? request.maxEntries : DEFAULT_MAX_ENTRIES;

        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = MessageFormat.format(Messages.NavigationHistoryTitleTemplate, maxEntries);
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        return CompletableFuture.supplyAsync(() ->
        {
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var entries = dispatcher.dispatch(() -> collectHistory(maxEntries))
                .orElseGet(() -> new ArrayList<>());

            var content = json.serialize(entries);

            var responseMarkdown = new StringBuilder();
            responseMarkdown.append(MessageFormat.format(Messages.NavigationHistoryFoundTemplate,
                markdownUtils.createStyledText(String.valueOf(entries.size()), TextColor.GREEN, FontWeight.BOLD)));

            if (!entries.isEmpty())
            {
                responseMarkdown.append("\n\n<details><summary>").append(Messages.ViewNavigationHistory)
                    .append("</summary>\n\n");

                for (var entry : entries)
                {
                    responseMarkdown.append("### **")
                        .append(markdownUtils.escapeForMarkdown(String.valueOf(entry.index)))
                        .append("**");

                    if (entry.isCurrent != null && entry.isCurrent)
                    {
                        responseMarkdown.append(" ").append(Messages.Current);
                    }

                    if (entry.text != null && !entry.text.isBlank())
                    {
                        responseMarkdown.append(" - ").append(markdownUtils.escapeForMarkdown(entry.text));
                    }

                    responseMarkdown.append("\n\n");

                    if (entry.projectName != null && entry.relativeFilePath != null)
                    {
                        responseMarkdown.append("**")
                            .append(Messages.Location)
                            .append(":** ")
                            .append(markdownUtils.escapeForMarkdown(entry.projectName + "/" + entry.relativeFilePath))
                            .append("\n");
                    }

                    if (entry.input != null && !entry.input.isBlank())
                    {
                        responseMarkdown.append("**")
                            .append(Messages.NavigationInput)
                            .append(":** ")
                            .append(markdownUtils.escapeForMarkdown(entry.input))
                            .append("\n");
                    }

                    responseMarkdown.append("\n---\n\n");
                }

                responseMarkdown.append("</details>");
            }
            else
            {
                responseMarkdown.append("\n\n").append(Messages.NoNavigationHistoryFound);
            }

            details.responseMarkdown = responseMarkdown.toString();
            return messageFactory.createMessage(this, call, content, details);
        }).exceptionally(throwable ->
        {
            return messageFactory.createError(this, call, throwable.getMessage());
        });
    }

    private List<NavigationEntry> collectHistory(int maxEntries)
    {
        var workbench = PlatformUI.getWorkbench();
        if (workbench == null)
        {
            throw new RuntimeException("Workbench is not available.");
        }

        var window = workbench.getActiveWorkbenchWindow();
        if (window == null)
        {
            throw new RuntimeException("Active workbench window is not available.");
        }

        var page = window.getActivePage();
        if (page == null)
        {
            throw new RuntimeException("Active workbench page is not available.");
        }

        INavigationHistory history = page.getNavigationHistory();
        if (history == null)
        {
            throw new RuntimeException("Navigation history is not available.");
        }

        INavigationLocation current = history.getCurrentLocation();
        INavigationLocation[] locations = history.getLocations();
        if (locations == null || locations.length == 0)
        {
            return new ArrayList<>();
        }

        int total = locations.length;
        int start = maxEntries > 0 ? Math.max(0, total - maxEntries) : 0;

        var entries = new ArrayList<NavigationEntry>();
        for (int i = start; i < total; i++)
        {
            var location = locations[i];
            if (location == null)
            {
                continue;
            }

            var entry = new NavigationEntry();
            entry.index = i;
            entry.text = location.getText();
            entry.isCurrent = location == current;

            Object input = location.getInput();
            entry.input = input != null ? input.toString() : null;

            Optional<IFile> file = resolveFile(input);
            if (file.isPresent())
            {
                var fileValue = file.get();
                entry.projectName = fileValue.getProject().getName();
                entry.relativeFilePath = fileValue.getProjectRelativePath().toString();
            }

            entries.add(entry);
        }

        return entries;
    }

    private Optional<IFile> resolveFile(Object input)
    {
        if (input instanceof IFileEditorInput)
        {
            return Optional.ofNullable(((IFileEditorInput)input).getFile());
        }

        if (input instanceof IAdaptable)
        {
            return Optional.ofNullable(((IAdaptable)input).getAdapter(IFile.class));
        }

        return Optional.empty();
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Lists IDE navigation history for the active workbench page.");
        description.append("\n\nUsage:");
        description.append("\n- Returns recent navigation locations in editor history.");
        description.append("\n- Each entry includes text plus file info when available.");
        description.append("\n\nRelated tools:");
        description.append("\n- Read file: `" + ReadMcpTool.TOOL_NAME + "`.");
        description.append("\n- Open files in context: `" + GetProjectsMcpTool.TOOL_NAME + "`.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var maxEntriesProp = new McpToolCallProperty();
        maxEntriesProp.type = "integer";
        maxEntriesProp.description = "Maximum number of history entries to return. Default: " + DEFAULT_MAX_ENTRIES;
        properties.put("max_entries", maxEntriesProp);

        parameters.properties = properties;
        parameters.required = new ArrayList<>();
        spec.function.parameters = parameters;
        return spec;
    }

    private static class Request
    {
        @SerializedName("max_entries")
        public Integer maxEntries;
    }

    private static class NavigationEntry
    {
        @SerializedName("index")
        public Integer index;

        @SerializedName("text")
        public String text;

        @SerializedName("input")
        public String input;

        @SerializedName("project_name")
        public String projectName;

        @SerializedName("relative_file_path")
        public String relativeFilePath;

        @SerializedName("is_current")
        public Boolean isCurrent;
    }
}
