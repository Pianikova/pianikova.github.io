/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ITextPreprocessor;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class IdeApiHandler
{
    private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$
    private final ILog log;
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final ITextPreprocessor textPreprocessor;
    private final Provider<IChat> chatProvider;
    private final IJson json;
    private final IMcpTools mcpTools;
    private final IEdtLinkHandler linkHandler;
    private final IEditorPositionManager editorPositionManager;
    private final IMarkdownUtils markdownUtils;
    private final IWeb web;
    private boolean isReady;

    @Inject
    public IdeApiHandler(ILog log, IUI ui, IDispatcher dispatcher, ITextPreprocessor textPreprocessor,
        Provider<IChat> chatProvider, IJson json,
        IMcpTools mcpTools, IEdtLinkHandler linkHandler, IEditorPositionManager editorPositionManager,
        IMarkdownUtils markdownUtils, IWeb web)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(textPreprocessor);
        Preconditions.checkNotNull(chatProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(linkHandler);
        Preconditions.checkNotNull(editorPositionManager);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(web);
        this.log = log;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.textPreprocessor = textPreprocessor;
        this.chatProvider = chatProvider;
        this.json = json;
        this.mcpTools = mcpTools;
        this.linkHandler = linkHandler;
        this.editorPositionManager = editorPositionManager;
        this.markdownUtils = markdownUtils;
        this.web = web;
    }

    public void wink(String parameter)
    {
        Preconditions.checkNotNull(parameter);
        isReady = true;
        log.trace(TracingSources.CHAT, AI_CHAT, () -> "winked: " + parameter); //$NON-NLS-1$
    }

    public void paste_code(String code)
    {
        if (code == null)
        {
            return;
        }

        final var processedCode = textPreprocessor.process(code);
        ui.getLastSourceViewer().ifPresent(sourceViewer -> {
            var selection = sourceViewer.getSelection();
            var textWidget = sourceViewer.getTextWidget();
            var content = textWidget.getContent();
            if (selection instanceof TextSelection)
            {
                var textSelection = (TextSelection)selection;
                if (textSelection.getLength() > 0)
                {
                    var shellOptional = ui.getShell();
                    if (shellOptional.isPresent())
                    {
                        if (!MessageDialog.openQuestion(shellOptional.get(), Messages.AIName, Messages.ReplaceCode))
                        {
                            return;
                        }
                    }
                }

                content.replaceTextRange(sourceViewer.modelOffset2WidgetOffset(textSelection.getOffset()),
                    textSelection.getLength(), processedCode);
                return;
            }

            content.replaceTextRange(textWidget.getCaretOffset(), 0, processedCode);
        });
    }

    public void callTools(String chatId, String messageId, String callToolsJson)
    {
        var job = dispatcher.createJob(Messages.ChatInteractionJobName, jobCtx -> {
            var callToolsOptional = json.deserialize(callToolsJson, McpToolCalls.class);
            if (callToolsOptional.isEmpty())
            {
                log.logError("Cannot deserialize calls: " + callToolsJson); //$NON-NLS-1$
                return;
            }

            var calls = callToolsOptional.get();
            for (var call : calls)
            {
                call.sourceChatId = chatId;
                call.sourceMessageId = messageId;
                call.callKind = ToolCallKind.CALL;
            }

            mcpTools.callTools(calls, CancellationTokens.NONE).whenComplete((result, error) -> {
                if (error != null)
                {
                    log.logError(error);
                    return;
                }

                var chat = chatProvider.get();
                chat.addToolsResult(chatId, messageId, result);
            });
        }, true, CancellationTokens.NONE);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    public String renderTools(String chatId, String messageId, String callToolsJson)
    {
        var callToolsOptional = json.deserialize(callToolsJson, McpToolCalls.class);
        if (callToolsOptional.isEmpty())
        {
            log.logError("Cannot deserialize calls: " + callToolsJson); //$NON-NLS-1$
            return null;
        }

        var calls = callToolsOptional.get();
        for (var call : calls)
        {
            call.sourceChatId = chatId;
            call.sourceMessageId = messageId;
            call.callKind = ToolCallKind.RENDER;
        }

        try
        {
            var result = mcpTools.callTools(calls, CancellationTokens.NONE).get();
            var messagesJson = result.messages != null ? json.serialize(result.messages) : null;
            return messagesJson;
        }
        catch (InterruptedException | ExecutionException error)
        {
            log.logError(error);
        }

        return null;
    }

    public void trace(String message)
    {
        // Chat tracing
        log.trace(TracingSources.CHAT, AI_CHAT, () -> message);
    }

    @SuppressWarnings("nls")
    public boolean link(String title, String href)
    {
        var safeHref = href != null ? href.trim() : "";

        // Decode URL-encoded characters (e.g., %3A -> :, %D0 -> Cyrillic letters)
        var decodedHref = markdownUtils.decodeUrl(safeHref);

        // After decoding, colons in Windows paths (D:\, C:\, etc.) need to be re-escaped
        // because extractFilePath expects %3A (COLON_ESCAPE) and will replace it with :
        // If we pass a decoded URL with colons, extractFilePath will incorrectly parse it
        var processedHref = escapeColonsInPath(decodedHref);

        if (!linkHandler.isRecognizedHref(processedHref))
        {
            web.browse(decodedHref);
            return true;
        }

        var safeTitle = title != null ? title.trim() : "";
        if (safeTitle.isEmpty() && processedHref.isEmpty())
        {
            return false;
        }

        var filePath = linkHandler.extractFilePath(processedHref);
        if (filePath.isEmpty())
        {
            return false;
        }

        dispatcher.dispatchAsync(() -> {
            // Extract position information from href
            var selection = linkHandler.extractSelection(processedHref).orElse(null);
            var cursorPosition = linkHandler.extractCursorPosition(processedHref).orElse(null);
            editorPositionManager.openFileInEditor(filePath, cursorPosition, selection);
        });

        return true;
    }

    /**
     * Escapes colons in file paths (e.g., D:\ -> D:%3A) for proper parsing by extractFilePath.
     * Only escapes colons that appear after the protocol and before any path separators.
     *
     * @param href the decoded href
     * @return the href with colons escaped
     */
    @SuppressWarnings("nls")
    private static String escapeColonsInPath(String href)
    {
        if (href == null || href.isEmpty())
        {
            return href;
        }

        // Find the protocol end (edt-file://)
        int protocolEnd = href.indexOf("://");
        if (protocolEnd < 0)
        {
            return href;
        }

        String pathPart = href.substring(protocolEnd + 3);

        // Escape colons in the drive letter (D:\ -> D:%3A) or other path separators
        // We need to find the first colon that's part of a drive letter or path separator
        int firstColon = pathPart.indexOf(':');
        if (firstColon > 0 && firstColon < 3)
        {
            // This is likely a Windows drive letter (e.g., D:\)
            pathPart = pathPart.substring(0, firstColon) + "%3A" + pathPart.substring(firstColon + 1);
        }

        return href.substring(0, protocolEnd + 3) + pathPart;
    }

    public boolean isReady()
    {
        return this.isReady;
    }

    public void reset()
    {
        this.isReady = false;
    }
}
