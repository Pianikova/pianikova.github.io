/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.CommandCategory;
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
    public static final CommandCategory Uncategorized = new CommandCategory();

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{ }";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"category_id\": \"system\",\n"
        + "    \"category_name\": \"System Commands\",\n"
        + "    \"category_description\": \"Commands for system management and control\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"category_id\": \"network\",\n"
        + "    \"category_description\": \"Commands related to network configuration and diagnostics\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"category_id\": \"security\",\n"
        + "    \"category_name\": \"Security Tools\",\n"
        + "  },\n"
        + "  {\n"
        + "    \"category_id\": \"data\",\n"
        + "    \"category_name\": \"Data Processing\",\n"
        + "    \"category_description\": \"Commands for data manipulation and analysis\"\n"
        + "  }\n"
        + "]";

    // @formatter:oт

    private final ILog log;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    static {
        Uncategorized.id = "uncategorized"; //$NON-NLS-1$
        Uncategorized.name = "Uncategorized"; //$NON-NLS-1$
        Uncategorized.name = "Contains commands that do not fit into any category."; //$NON-NLS-1$
    }

    @Inject
    public GetCommandCategoriesMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        this.log = log;
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
        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            var categories = new ArrayList<CommandCategory>();
            for(var src: commandService.getDefinedCategories())
            {
                var dst = new CommandCategory();
                dst.id = src.getId();
                try
                {
                    dst.name = src.getName();
                }
                catch (NotDefinedException e)
                {
                    //
                }

                try
                {
                    dst.description = src.getDescription();
                }
                catch (NotDefinedException e)
                {
                    //
                }

                categories.add(dst);
            }

            categories.add(Uncategorized);
            var content = json.serialize(categories);
            return messageFactory.createMessage(this, call, content);
        }).exceptionally(ex -> {
            var cause = ex instanceof CompletionException ? ex.getCause() : ex;
            return messageFactory.createError(this, call, "Failed to get. " + cause.getMessage());
        });
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

        description.append("Provides command categories in the IDE: id, name, description.");
        description.append("\nNOTE: add a description of what will be done when using this tool.");

        description.append("\nFor exapmple:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        parameters.properties = new HashMap<>();
        parameters.required = new ArrayList<>();
        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }
}