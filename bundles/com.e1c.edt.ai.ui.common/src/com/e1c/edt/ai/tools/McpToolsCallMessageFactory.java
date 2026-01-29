/**
 *
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class McpToolsCallMessageFactory
    implements IMcpToolsCallMessageFactory
{
    private final ISettings settings;
    private final IJson json;

    @Inject
    public McpToolsCallMessageFactory(ISettings settings, IJson json)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(json);

        this.settings = settings;
        this.json = json;
    }

    @Override
    public ToolCallMessage createMessage(IMcpTool tool, McpToolCall call, String content,
        ToolCallMessageDetails details)
    {
        return createMessage(tool, call, content, details, true);
    }

    @SuppressWarnings("nls")
    @Override
    public ToolCallMessage createError(IMcpTool tool, McpToolCall call, String errorMessage)
    {
        var details = new ToolCallMessageDetails();
        var responseMarkdown = new StringBuilder();

        // Add tool name if available
        if (call != null && call.function != null && call.function.name != null)
        {
            responseMarkdown.append(MessageFormat.format(Messages.ToolNameTemplate, call.function.name));
        }

        responseMarkdown.append(System.lineSeparator());
        responseMarkdown.append(System.lineSeparator());

        // Add error details section
        responseMarkdown.append("<details><summary>")
            .append(Messages.ErrorDetails)
            .append("</summary>")
            .append(System.lineSeparator())
            .append(System.lineSeparator());

        // Add error content
        responseMarkdown.append("__").append(Messages.ErrorContent).append(":__").append(System.lineSeparator());
        responseMarkdown.append("```").append(System.lineSeparator());
        responseMarkdown.append(errorMessage).append(System.lineSeparator());
        responseMarkdown.append("```").append(System.lineSeparator());

        responseMarkdown.append("</details>");

        details.responseMarkdown = responseMarkdown.toString();

        return createMessage(tool, call, "Error: \"" + errorMessage + "\"", details, false);
    }

    @SuppressWarnings("nls")
    private ToolCallMessage createMessage(IMcpTool tool, McpToolCall call, String content,
        ToolCallMessageDetails details, boolean isDone)
    {
        var message = new ToolCallMessage();
        message.role = "tool"; //$NON-NLS-1$
        message.content = content;

        if (call != null)
        {
            message.call = call;
            message.tool_call_id = call.id;
        }

        if (tool != null)
        {
            message.specification = tool.getSpecification();
        }

        if (details == null)
        {
            details = new ToolCallMessageDetails();
        }

        var requestMarkdown = new StringBuilder();
        if (details.requestMarkdown != null)
        {
            requestMarkdown.append(details.requestMarkdown);
        }

        var responseMarkdown = new StringBuilder();
        if (details.responseMarkdown != null)
        {
            responseMarkdown.append(details.responseMarkdown);
        }

        if (call == null || call.callKind == ToolCallKind.CALL)
        {
            // Tracing
            if (isTracing())
            {
                if (requestMarkdown.length() == 0)
                {
                    requestMarkdown.append(MessageFormat.format(Messages.ToolNameTemplate, call.function.name));
                }

                requestMarkdown.append(System.lineSeparator());
                requestMarkdown.append(System.lineSeparator());
                requestMarkdown.append("```json");
                requestMarkdown.append(System.lineSeparator());
                requestMarkdown.append(json.formatJson(call.function.arguments));
                requestMarkdown.append(System.lineSeparator());
                requestMarkdown.append("```");
                details.requestMarkdown = requestMarkdown.toString();

                if (responseMarkdown.length() > 0)
                {
                    responseMarkdown.append(System.lineSeparator());
                    responseMarkdown.append(System.lineSeparator());
                }

                responseMarkdown.append("```");
                responseMarkdown.append(System.lineSeparator());
                responseMarkdown.append(json.formatJson(content));
                responseMarkdown.append(System.lineSeparator());
                responseMarkdown.append("```");
            }

            // Status
            if (requestMarkdown.length() == 0 && responseMarkdown.length() == 0)
            {
                responseMarkdown.append(MessageFormat.format(Messages.ToolNameTemplate, call.function.name));
            }

            responseMarkdown.append(System.lineSeparator());
            responseMarkdown.append(System.lineSeparator());
            if (isDone)
            {
                responseMarkdown.append(Messages.ToolDone);
            }
            else
            {
                responseMarkdown.append(Messages.ToolFailed);
            }
        }

        details.requestMarkdown = requestMarkdown.toString();
        details.responseMarkdown = responseMarkdown.toString();
        message.details = details;
        return message;
    }

    private boolean isTracing()
    {
        return settings.getVerbosity().getLevel() >= Verbosity.TRACE.getLevel();
    }
}
