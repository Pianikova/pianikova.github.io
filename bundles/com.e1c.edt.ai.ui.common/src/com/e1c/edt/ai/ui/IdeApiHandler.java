/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ITextPreprocessor;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
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
    private boolean isReady;

    @Inject
    public IdeApiHandler(ILog log, IUI ui, IDispatcher dispatcher, ITextPreprocessor textPreprocessor,
        Provider<IChat> chatProvider, IJson json,
        IMcpTools mcpTools)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(textPreprocessor);
        Preconditions.checkNotNull(chatProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(mcpTools);
        this.log = log;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.textPreprocessor = textPreprocessor;
        this.chatProvider = chatProvider;
        this.json = json;
        this.mcpTools = mcpTools;
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
            mcpTools.callTools(calls, CancellationTokens.NONE).whenComplete((result, error) -> {
                if (error != null)
                {
                    log.logError(error);
                    return;
                }

                var chat = chatProvider.get();
                chat.addToolsResult(chatId, messageId, result);
            });
        }, CancellationTokens.NONE);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    public void trace(String message)
    {
        // Chat tracing
        log.trace(TracingSources.CHAT, AI_CHAT, () -> message);
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
