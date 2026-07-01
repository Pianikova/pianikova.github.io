/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ParameterValuesException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
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
import com.google.inject.Inject;


public class GetCommandsMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "GetCommands"; //$NON-NLS-1$
    public static final Comparator<CommandDescription> COMPARATOR =
        Comparator.comparing((CommandDescription i) -> {
            if (i.parameters == null || i.parameters.size() == 0)
            {
                return 1;
            }

            if (i.parameters.size() == 1)
            {
                return 0;
            }

            return i.parameters.size();
        });

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
    "{\n"
    + "  \"category_id\": \"system\"\n"
    + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "[\n"
        + "  {\n"
        + "    \"return_is_defined\": true,\n"
        + "    \"return_type_id\": \"string\",\n"
        + "    \"is_enabled\": true,\n"
        + "    \"description\": \"Initiates system shutdown\",\n"
        + "    \"name\": \"Shutdown\",\n"
        + "    \"id\": \"shutdown\",\n"
        + "    \"parameters\": [\n"
        + "      {\n"
        + "        \"is_optional\": false,\n"
        + "        \"values\": {\n"
        + "          \"type\": \"integer\",\n"
        + "          \"min\": 0,\n"
        + "          \"max\": 60\n"
        + "        },\n"
        + "        \"name\": \"Delay (seconds)\",\n"
        + "        \"id\": \"param_delay\"\n"
        + "        \"friendly_id\": \"delay\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"is_optional\": true,\n"
        + "        \"values\": {\n"
        + "          \"type\": \"boolean\",\n"
        + "          \"default\": false\n"
        + "        },\n"
        + "        \"name\": \"Force\",\n"
        + "        \"id\": \"param_force\"\n"
        + "      }\n"
        + "    ]\n"
        + "  },\n"
        + "  {\n"
        + "    \"return_is_defined\": false,\n"
        + "    \"return_type_id\": \"void\",\n"
        + "    \"is_enabled\": true,\n"
        + "    \"description\": \"Reboots the system\",\n"
        + "    \"name\": \"Reboot\",\n"
        + "    \"id\": \"reboot\",\n"
        + "    \"parameters\": [\n"
        + "      {\n"
        + "        \"is_optional\": true,\n"
        + "        \"values\": {\n"
        + "          \"options\": [\"safe\", \"full\", \"recovery\"]\n"
        + "        },\n"
        + "        \"name\": \"Mode\",\n"
        + "        \"id\": \"param_mode\"\n"
        + "      }\n"
        + "    ]\n"
        + "  },\n"
        + "  {\n"
        + "    \"return_is_defined\": true,\n"
        + "    \"return_type_id\": \"TemperatureData\",\n"
        + "    \"is_enabled\": true,\n"
        + "    \"description\": \"Gets current CPU temperature\",\n"
        + "    \"name\": \"Get CPU Temp\",\n"
        + "    \"id\": \"cmd_cpu_temp\",\n"
        + "    \"parameters\": []\n"
        + "  }\n"
        + "]";

    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IMarkdownUtils markdownUtils;

    @Inject
    public GetCommandsMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(markdownUtils);
        this.json = json;
        this.messageFactory = messageFactory;
        this.markdownUtils = markdownUtils;
        spec = createSpecification();
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
        details.hideAfter = true;
        var optionalRequest = json.deserialize(call.function.arguments, CommandCategory.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
        }

        var request = optionalRequest.get();
        var categoryId = request.id;

        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = Messages.CommandsTitle;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.", null, ToolErrorType.RETRYABLE);
            }

            var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            var bindingService = PlatformUI.getWorkbench().getService(IBindingService.class);
            var commands = new ArrayList<CommandDescription>();
            for(var src: commandService.getDefinedCommands())
            {
                try
                {
                    if (!src.isEnabled())
                    {
                        continue;
                    }
                }
                catch (Throwable error)
                {
                    //
                    continue;
                }

                if (!categoryId.equalsIgnoreCase(GetCommandCategoriesMcpTool.UNCategorized.id))
                {
                    try
                    {
                        var catogory = src.getCategory();
                        if (!categoryId.equalsIgnoreCase(catogory.getId()))
                        {
                            continue;
                        }
                    }
                    catch (NotDefinedException e2)
                    {
                        //
                    }
                }

                var dst = new CommandDescription();
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

                try
                {
                    var returnType = src.getReturnType();
                    if(returnType != null)
                    {
                        dst.returnTypeId = returnType.getId();
                        dst.returnIsDefined = returnType.isDefined();
                    }
                }
                catch (NotDefinedException e1)
                {
                    //
                }

                try
                {
                    var params = src.getParameters();
                    if (params != null)
                    {
                        var commandParameters = new ArrayList<CommandParameter>();
                        for (var param: params)
                        {
                            var commandParameter = new CommandParameter();
                            commandParameters.add(commandParameter);
                            commandParameter.id = param.getId();
                            commandParameter.name = param.getName();
                            commandParameter.isOptional = param.isOptional();
                            try
                            {
                                var vals = param.getValues();
                                if (vals != null)
                                {
                                    commandParameter.values = vals.getParameterValues();
                                }
                            }
                            catch (ParameterValuesException e)
                            {
                                //
                            }
                        }

                        if (!commandParameters.isEmpty())
                        {
                            dst.parameters = commandParameters;
                        }
                    }
                }
                catch (NotDefinedException e)
                {
                    //
                }

                var bindings = bindingService.getActiveBindingsFor(src.getId());
                if (bindings != null && bindings.length > 0)
                {
                    dst.hotKey = bindings[0].format();
                }

                commands.add(dst);
            }

            var content = json.serialize(commands.stream().sorted(COMPARATOR).collect(Collectors.toList()));
            // Add response markdown

            int commandCount = commands.size();
            String styledCommandCount = markdownUtils.createStyledText(String.valueOf(commandCount),
                TextColor.GREEN, FontWeight.BOLD, false);
            details.responseMarkdown = MessageFormat.format(Messages.CommandsLoadedTemplate, styledCommandCount);

            return messageFactory.createMessage(this, call, content, details);
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

        description.append("Lists IDE commands and their metadata.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Use category ids to scope results.");
        description.append("\n- Add a description of what will be done when calling a command.");
        description.append("\n\nRelated tools:");
        description.append("\n- List categories: `" + GetCommandCategoriesMcpTool.TOOL_NAME + "`.");
        description.append("\n- Execute command: use `" + JShellMcpTool.TOOL_NAME
            + "` (scope `eclipse`, manual `execute_command`).");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var categoryIdProp = new McpToolCallProperty();
        categoryIdProp.type = "string";
        categoryIdProp.description = "Command caterogy id.";
        properties.put("category_id", categoryIdProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("category_id");

        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }
}
