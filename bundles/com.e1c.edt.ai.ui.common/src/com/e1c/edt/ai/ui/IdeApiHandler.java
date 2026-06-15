/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IEditRollback;
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
    private final IEditRollback editRollback;
    private final IWorkmateLocations locations;
    private boolean isReady;

    @Inject
    public IdeApiHandler(ILog log, IUI ui, IDispatcher dispatcher, ITextPreprocessor textPreprocessor,
        Provider<IChat> chatProvider, IJson json,
        IMcpTools mcpTools, IEdtLinkHandler linkHandler, IEditorPositionManager editorPositionManager,
        IMarkdownUtils markdownUtils, IWeb web, IEditRollback editRollback, IWorkmateLocations locations)
    {
        Preconditions.checkNotNull(locations);
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
        Preconditions.checkNotNull(editRollback);
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
        this.editRollback = editRollback;
        this.locations = locations;
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

        // For relative paths like "/chat-list", return false
        if (safeHref.startsWith("/"))
        {
            return false;
        }

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

    /**
     * Starts recording from the default microphone.
     * Streams PCM chunks to JS via {@code window.onVoiceChunk(base64, durationMs)}.
     * Called from JavaScript via {@code window.ideApi.startVoiceRecording()}.
     */
    public void startVoiceRecording()
    {
        log.trace(TracingSources.CHAT, AI_CHAT, () -> "ideApi.startVoiceRecording() called from JS"); //$NON-NLS-1$
        chatProvider.get().startVoiceRecording();
    }

    /**
     * Stops voice recording. Sends last chunk and notifies JS via
     * {@code window.onVoiceStateChange('stopped')}.
     * Called from JavaScript via {@code window.ideApi.stopVoiceRecording()}.
     *
     * @return "stopped" for JS compatibility
     */
    public String stopVoiceRecording()
    {
        log.trace(TracingSources.CHAT, AI_CHAT, () -> "ideApi.stopVoiceRecording() called from JS"); //$NON-NLS-1$
        return chatProvider.get().stopVoiceRecording();
    }

    public boolean isReady()
    {
        return this.isReady;
    }

    public void reset()
    {
        this.isReady = false;
    }

    /**
     * Reverts a single Edit operation by applying the inverse text replacement to the file's
     * current content. Arguments mirror those of the original {@code Edit} tool call recorded in
     * the chat history, so the operation is fully stateless and works after IDE restart.
     * <p>
     * If the file has been further modified since the original edit and the post-edit fragment
     * can no longer be located unambiguously, the rollback refuses and returns {@code false} —
     * intermediate work is never silently destroyed.
     *
     * @return {@code true} on successful revert; {@code false} on validation/IO failure or when
     *         the file has diverged.
     */
    public boolean rollbackEdit(String path, String oldContent, String newContent, boolean replaceAll)
    {
        return editRollback.rollback(path, oldContent, newContent, replaceAll);
    }

    // --- WORKMATE.md rules for the chat (window.ideApi.*) -------------------------------------------
    // Paths are computed via IWorkmateLocations (same source as the navigator/opener). Content getters
    // return the raw per-level file, or null when it does not exist (the chat decides what to inject;
    // the bundled default is intentionally NOT returned here).

    private static final String WORKMATE_MD = "WORKMATE.md"; //$NON-NLS-1$

    /** @return path to {@code ~/.workmate/WORKMATE.md} (user level), or {@code null}. */
    public String getCommonRulesPath()
    {
        return rulesPath(locations.userHome());
    }

    /** @return content of the user-level rules file, or {@code null} if missing/empty. */
    public String getCommonRulesContent()
    {
        return rulesContent(locations.userHome());
    }

    /** @return JSON {@code {"path","content"}} for the user-level rules, or {@code null}. */
    public String getCommonRules()
    {
        return rulesJson(getCommonRulesPath(), getCommonRulesContent());
    }

    /** @return path to {@code <workspace>/.workmate/WORKMATE.md}, or {@code null}. */
    public String getWorkspaceRulesPath()
    {
        return rulesPath(locations.workspaceRoot());
    }

    /** @return content of the workspace-level rules file, or {@code null} if missing/empty. */
    public String getWorkspaceRulesContent()
    {
        return rulesContent(locations.workspaceRoot());
    }

    /** @return JSON {@code {"path","content"}} for the workspace-level rules, or {@code null}. */
    public String getWorkspaceRules()
    {
        return rulesJson(getWorkspaceRulesPath(), getWorkspaceRulesContent());
    }

    /**
     * @param projectName the project name.
     * @return path to {@code <project>/.workmate/WORKMATE.md}, or {@code null} if the project is unknown.
     */
    public String getProjectRulesPath(String projectName)
    {
        var project = project(projectName);
        return project == null ? null : rulesPath(locations.projectRoot(project));
    }

    /**
     * @param projectName the project name.
     * @return content of the project-level rules file, or {@code null} if missing/empty/unknown project.
     */
    public String getProjectRulesContent(String projectName)
    {
        var project = project(projectName);
        return project == null ? null : rulesContent(locations.projectRoot(project));
    }

    /**
     * @param projectName the project name.
     * @return JSON {@code {"path","content"}} for the project-level rules, or {@code null}.
     */
    public String getProjectRules(String projectName)
    {
        return rulesJson(getProjectRulesPath(projectName), getProjectRulesContent(projectName));
    }

    /**
     * @return JSON {@code {"<project>": {"path","content"}, ...}} for every project that has a non-empty
     *     rules file, or {@code null} when none do.
     */
    @SuppressWarnings("nls")
    public String getProjectsRules()
    {
        var result = new HashMap<String, Object>();
        for (var project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
        {
            var path = getProjectRulesPath(project.getName());
            var content = getProjectRulesContent(project.getName());
            if (path != null && content != null)
            {
                var info = new HashMap<String, String>();
                info.put("path", path);
                info.put("content", content);
                result.put(project.getName(), info);
            }
        }
        return result.isEmpty() ? null : json.serialize(result);
    }

    private static String rulesPath(java.util.Optional<Path> base)
    {
        return base.map(root -> IWorkmateLocations.resolve(root, List.of(IWorkmateLocations.WORKMATE_DIR, WORKMATE_MD))
            .toAbsolutePath().toString()).orElse(null);
    }

    private String rulesContent(java.util.Optional<Path> base)
    {
        return base.map(root -> readFileContent(IWorkmateLocations.resolve(root, //
            List.of(IWorkmateLocations.WORKMATE_DIR, WORKMATE_MD)))).orElse(null);
    }

    @SuppressWarnings("nls")
    private String rulesJson(String path, String content)
    {
        if (path == null || content == null)
        {
            return null;
        }
        var result = new HashMap<String, String>();
        result.put("path", path);
        result.put("content", content);
        return json.serialize(result);
    }

    private IProject project(String projectName)
    {
        if (projectName == null)
        {
            return null;
        }
        var project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        return project != null && project.exists() ? project : null;
    }

    private String readFileContent(Path filePath)
    {
        if (filePath == null || !Files.exists(filePath) || filePath.toFile().length() == 0)
        {
            return null;
        }
        try
        {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }
        catch (Exception error)
        {
            log.logError(error);
            return null;
        }
    }
}
