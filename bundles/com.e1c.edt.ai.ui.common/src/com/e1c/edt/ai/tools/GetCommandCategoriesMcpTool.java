/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class GetCommandCategoriesMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ide_get_command_categories"; //$NON-NLS-1$
    public static final CommandCategory UNCategorized = createUncategorizedCategory();

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample = "{}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"id\": \"system\",\n"
        + "    \"name\": \"System Commands\",\n"
        + "    \"description\": \"Commands for system management and control\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"id\": \"network\",\n"
        + "    \"name\": \"Network\",\n"
        + "    \"description\": \"Commands related to network configuration\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"id\": \"security\",\n"
        + "    \"name\": \"Security Tools\",\n"
        + "    \"description\": \"Security-related commands\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"id\": \"uncategorized\",\n"
        + "    \"name\": \"Uncategorized\",\n"
        + "    \"description\": \"Commands without category\"\n"
        + "  }\n"
        + "]";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public GetCommandCategoriesMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        this.json = json;
        this.messageFactory = messageFactory;
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
        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            try
            {
                var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
                var definedCategories = commandService.getDefinedCategories();
                var categories = new ArrayList<CommandCategory>(definedCategories.length + 1);

                for (var src : definedCategories)
                {
                    var dst = new CommandCategory();
                    dst.id = src.getId();

                    try
                    {
                        dst.name = src.getName();
                    }
                    catch (NotDefinedException e)
                    {
                        dst.name = "Undefined name";
                    }

                    try
                    {
                        dst.description = src.getDescription();
                    }
                    catch (NotDefinedException e)
                    {
                        dst.description = "No description available";
                    }

                    categories.add(dst);
                }

                // Добавляем uncategorized только если он еще не существует
                boolean hasUncategorized = categories.stream().anyMatch(c -> "uncategorized".equalsIgnoreCase(c.id));

                if (!hasUncategorized)
                {
                    categories.add(UNCategorized);
                }

                return messageFactory.createMessage(this, call, json.serialize(categories));
            }
            catch (Exception e)
            {
                return messageFactory.createError(this, call,
                    "Failed to retrieve command categories: " + e.getMessage());
            }
        }).exceptionally(ex -> {
            var cause = ex instanceof CompletionException ? ex.getCause() : ex;
            return messageFactory.createError(this, call, "Internal error: " + cause.getMessage());
        });
    }

    @SuppressWarnings("nls")
    private static CommandCategory createUncategorizedCategory()
    {
        var category = new CommandCategory();
        category.id = "uncategorized";
        category.name = "Uncategorized";
        category.description = "Commands without specific category";
        return category;
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
        // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Retrieves all available command categories in the IDE.");
        description.append("\nReturns list of categories with id, name and description.");
        description.append("\nFor example:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        parameters.properties = new HashMap<>();
        parameters.required = Collections.emptyList();

        spec.function.parameters = parameters;
        return spec;
        // @formatter:on
    }
}