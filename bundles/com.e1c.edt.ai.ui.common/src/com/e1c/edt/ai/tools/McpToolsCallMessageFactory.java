/**
 *
 */
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.TextColor;
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
    private final IMarkdownUtils markdownUtils;

    @Inject
    public McpToolsCallMessageFactory(ISettings settings, IJson json, IMarkdownUtils markdownUtils)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(markdownUtils);

        this.settings = settings;
        this.json = json;
        this.markdownUtils = markdownUtils;
    }

    @Override
    public ToolCallMessage createMessage(IMcpTool tool, McpToolCall call, String content,
        ToolCallMessageDetails details)
    {
        return createMessage(tool, call, content, details, true);
    }

    @Override
    public ToolCallMessage createRawMessage(IMcpTool tool, McpToolCall call, String content,
        ToolCallMessageDetails details)
    {
        return createMessage(tool, call, content, details, null);
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
        if (isLogLevel(Verbosity.TRACE))
        {
            responseMarkdown.append("```").append(System.lineSeparator());
            if (call != null && call.function != null && call.function.arguments != null)
            {
                responseMarkdown.append(Messages.ErrorArguments).append(":").append(System.lineSeparator());
                responseMarkdown.append(json.formatJson(call.function.arguments)).append(System.lineSeparator());
                responseMarkdown.append(System.lineSeparator());
            }

            responseMarkdown.append(Messages.ErrorContent).append(":").append(System.lineSeparator());
            responseMarkdown.append(errorMessage).append(System.lineSeparator());
            responseMarkdown.append("```").append(System.lineSeparator());
        }

        details.responseMarkdown = responseMarkdown.toString();
        return createMessage(tool, call, "Error: \"" + errorMessage + "\"", details, false);
    }

    @SuppressWarnings("nls")
    private ToolCallMessage createMessage(IMcpTool tool, McpToolCall call, String content,
        ToolCallMessageDetails details, Boolean isDone)
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
            // Debug - detailed request/response information on errors only
            if (isLogLevel(Verbosity.DEBUG))
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

            if (isDone != null && isDone == true)
            {
                responseMarkdown.append(System.lineSeparator());
                responseMarkdown.append(System.lineSeparator());
                responseMarkdown.append(styleStatusMessage(Messages.ToolDone, TextColor.GREEN, true));
            }

            if (isDone != null && isDone == false)
            {
                responseMarkdown.append(System.lineSeparator());
                responseMarkdown.append(System.lineSeparator());
                responseMarkdown.append(styleStatusMessage(Messages.ToolFailed, TextColor.RED, true));
            }
        }

        details.requestMarkdown = requestMarkdown.length() > 0 ? requestMarkdown.toString() : null;
        details.responseMarkdown = responseMarkdown.length() > 0 ? responseMarkdown.toString() : null;
        message.details = details;
        return message;
    }

    @SuppressWarnings("nls")
    private String styleStatusMessage(String status, TextColor color, boolean dimText)
    {
        if (status == null || status.isEmpty())
        {
            return "";
        }

        var iconEnd = -1;
        for (int i = 0; i < status.length(); i++)
        {
            if (Character.isWhitespace(status.charAt(i)))
            {
                iconEnd = i;
                break;
            }
        }

        if (iconEnd <= 0)
        {
            return markdownUtils.createStyledText(status, color, FontWeight.NORMAL, 0.3);
        }

        var icon = status.substring(0, iconEnd);
        var rest = status.substring(iconEnd);
        var styledIcon = markdownUtils.createStyledText(icon, color, FontWeight.NORMAL, 0.3);
        if (!dimText)
        {
            return styledIcon + rest;
        }

        var styledRest = markdownUtils.createStyledText(rest, null, FontWeight.NORMAL, 0.3);
        return styledIcon + styledRest;
    }

    private boolean isLogLevel(Verbosity verbosity)
    {
        return settings.getVerbosity().getLevel() >= verbosity.getLevel();
    }
}
