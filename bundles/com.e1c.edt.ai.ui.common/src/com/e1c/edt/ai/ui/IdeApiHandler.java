/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
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
    private boolean isReady;

    @Inject
    public IdeApiHandler(ILog log, IUI ui, IDispatcher dispatcher, ITextPreprocessor textPreprocessor,
        Provider<IChat> chatProvider, IJson json,
        IMcpTools mcpTools, IEdtLinkHandler linkHandler)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(textPreprocessor);
        Preconditions.checkNotNull(chatProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(linkHandler);
        this.log = log;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.textPreprocessor = textPreprocessor;
        this.chatProvider = chatProvider;
        this.json = json;
        this.mcpTools = mcpTools;
        this.linkHandler = linkHandler;
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
        if (!linkHandler.isRecognizedHref(safeHref))
        {
            return false;
        }

        var safeTitle = title != null ? title.trim() : "";
        if (safeTitle.isEmpty() && safeHref.isEmpty())
        {
            return false;
        }

        var filePath = linkHandler.extractFilePath(safeHref);
        if (filePath.isEmpty())
        {
            return false;
        }

        dispatcher.dispatchAsync(() -> {
            openFileInEditor(filePath);
        });

        return true;
    }

    @SuppressWarnings("nls")
    private void openFileInEditor(String filePath)
    {
        try
        {
            var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

            // First try to find file in workspace (relative path)
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var file = root.getFile(new Path(filePath));
            if (file != null && file.exists())
            {
                IDE.openEditor(page, file);
                return;
            }

            // If not found in workspace, try as absolute path
            var externalFile = new File(filePath);
            if (externalFile.exists() && externalFile.isFile())
            {
                IFileStore fileStore = EFS.getLocalFileSystem().getStore(externalFile.toURI());
                IDE.openEditorOnFileStore(page, fileStore);
                return;
            }

            // File not found
            log.logError("File not found: " + filePath);
        }
        catch (PartInitException e)
        {
            log.logError(e);
        }
        catch (Exception e)
        {
            log.logError(e);
        }
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
