/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterValueConversionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
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
import com.google.inject.Inject;


public class ExecuteCommandMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "ExecuteCommand"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
    "{\n"
    + "  \"command_id\": \"file_open\",\n"
    + "  \"parameters\": [\n"
    + "    {\n"
    + "      \"id\": \"file_path\",\n"
    + "      \"value\": \"/documents/report.txt\"\n"
    + "    },\n"
    + "    {\n"
    + "      \"id\": \"file_encoding\",\n"
    + "      \"value\": \"UTF-8\"\n"
    + "    }\n"
    + "  ]\n"
    + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample = "";

    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IDispatcher dispatcher;

    @Inject
    public ExecuteCommandMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IDispatcher dispatcher)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(dispatcher);
        this.json = json;
        this.messageFactory = messageFactory;
        this.dispatcher = dispatcher;
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
        details.autoCall = false;
        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = Messages.ExecuteCommandTitle;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var optionalRequest = json.deserialize(call.function.arguments, CommandDescription.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        var commandId = request.id;
        if (commandId == null || commandId.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "The command_id cannot be empty."));
        }

        var params = request.parameters;
        var paramsMap = new HashMap<String, Object>();
        if (params != null)
        {
            for (var param : params)
            {
                var id = param.id;
                var val = param.value;
                if (id != null && !id.isBlank() && val != null)
                {
                    paramsMap.put(param.id, param.value);
                }
            }
        }

        // Use supplyAsync to execute the blocking operation on a separate thread.
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the work.
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            var command = commandService.getCommand(commandId);
            if (command == null)
            {
                return messageFactory.createError(this, call, "The command was not found.");
            }

            try
            {
                var commandParameters = command.getParameters();
                if (commandParameters != null)
                {
                    var paramsError = new StringBuilder();
                    for (var commandParameter : commandParameters)
                    {
                        var id = commandParameter.getId();
                        if (id == null || id.isBlank())
                        {
                            paramsError.append("Missing required parameter id.");
                            continue;
                        }

                        var val = (String)paramsMap.get(id);
                        if (val == null)
                        {
                            if (!commandParameter.isOptional())
                            {
                                if (val == null)
                                {
                                    paramsError.append("Missing required parameter with id \"");
                                    paramsError.append(id);
                                    paramsError.append("\"\n");
                                }

                            }
                        }
                        else
                        {
                            var param = command.getParameter(id);
                            if (param != null)
                            {
                                var parameterType = command.getParameterType(id);
                                if (parameterType != null)
                                {
                                    var valueConverter = parameterType.getValueConverter();
                                    if (valueConverter != null)
                                    {
                                        try
                                        {
                                            var obj = valueConverter.convertToObject(val);
                                            paramsMap.put(id, obj);
                                        }
                                        catch (ParameterValueConversionException e)
                                        {
                                            paramsError.append("Cannot convert \"");
                                            paramsError.append(val);
                                            paramsError.append("\".");
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (paramsError.length() > 0)
                    {
                        return messageFactory.createError(this, call, paramsError.toString());
                    }
                }
            }
            catch (NotDefinedException e)
            {
                //
            }

            var parameterizedCommand = ParameterizedCommand.generateCommand(command, paramsMap);
            if (parameterizedCommand == null)
            {
                return messageFactory.createError(this, call, "Invalid command parameter format.");
            }

            var handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
            return dispatcher.dispatch(() -> executeCommand(handlerService, call, parameterizedCommand, details))
                .orElseGet(() -> messageFactory.createError(this, call, "Cannot execute the command."));
        });
    }

    @SuppressWarnings("nls")
    private ToolCallMessage executeCommand(IHandlerService handlerService, McpToolCall call,
        ParameterizedCommand parameterizedCommand, ToolCallMessageDetails details)
    {
        Object result = null;
        try
        {
            result = handlerService.executeCommand(parameterizedCommand, null);
            if (result == null)
            {
                result = "The command was executed successfully.";
            }

            var content = json.serialize(result.toString());
            // Add response markdown

            details.responseMarkdown = MessageFormat.format(Messages.ExecutedTemplate, parameterizedCommand.getId());

            return messageFactory.createMessage(this, call, content, details);
        }
        catch (ExecutionException e)
        {
            return messageFactory.createError(this, call, "Cannot execute the command. " + e.getMessage());
        }
        catch (NotDefinedException e)
        {
            return messageFactory.createError(this, call, "The command is not defined. " + e.getMessage());
        }
        catch (NotEnabledException e)
        {
            return messageFactory.createError(this, call, "The command is not enabled. " + e.getMessage());
        }
        catch (NotHandledException e)
        {
            return messageFactory.createError(this, call, "The command is not handled. " + e.getMessage());
        }
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

        description.append("Executes an IDE command by id.");
        description.append("\n\nUsage:");
        description.append("\n- Use this tool for IDE actions that require the IDE context.");
        description.append("\n- Provide required parameters for the command.");
        description.append("\n- Add a short description of what will be done.");
        description.append("\n\nRelated tools:");
        description.append("\n- Discover commands: `" + GetCommandsMcpTool.TOOL_NAME + "`.");
        description.append("\n- Discover categories: `" + GetCommandCategoriesMcpTool.TOOL_NAME + "`.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var commandIdProp = new McpToolCallProperty();
        commandIdProp.type = "string";
        commandIdProp.description = "Command id.";
        properties.put("command_id", commandIdProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("command_id");

        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }
}
