/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.SourceViewer;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IUI;
import com.e1c.edt.ai.ui.Messages;
import com.google.inject.Inject;

/**
 * Shared core of the "Fix with 1C:Workmate" quick fix for STANDARD editor problems. All entry
 * points — the Problems-view resolution ({@link ExternalProblemMarkerResolution}), the EDT BSL
 * hover button and the JDT quick-fix proposal — delegate here: the problem region is selected in
 * the editor, and one fix request goes through the standard "Fix code" flow
 * ({@code createContextForTarget(FIX)} + {@code chat.fixCode}).
 * <p>
 * The request text is rendered from the {@code quick-fix-problem} skill
 * (skills/quick-fix-problem/SKILL.md): it carries the problem messages, the line number, the
 * file's absolute path and an explicit instruction for the model to READ the file around the
 * problem line first — the selected snippet alone usually lacks the surrounding context needed
 * for a correct fix, and in the interactive chat the model has the Read tool to fetch it. The
 * rendered prompt is sent as a plain message of a new conversation ({@code chat.askQuestion}).
 */
public class ExternalProblemFixer
{
    private static final String SKILL_NAME = "quick-fix-problem"; //$NON-NLS-1$
    private static final String UNKNOWN_LINE = "?"; //$NON-NLS-1$

    @Inject
    IUI ui;
    @Inject
    IChat chat;
    @Inject
    ICodeTools codeTools;
    @Inject
    ISettings settings;
    @Inject
    IEditingSupport editingSupport;
    @Inject
    ISkillExecutor skillExecutor;
    @Inject
    IDispatcher dispatcher;
    @Inject
    ILog log;

    public ExternalProblemFixer()
    {
        BaseActivator.injectMembers(this);
    }

    /**
     * A fix is offered for visible error/warning/info annotations with a message. Auxiliary
     * annotations (spelling, breakpoints, diffs, ...) are skipped by the type suffix.
     */
    @SuppressWarnings("nls")
    public boolean canFix(Annotation annotation)
    {
        if (annotation == null || annotation.isMarkedDeleted() || !settings.isEnabled())
        {
            return false;
        }
        var type = annotation.getType();
        if (type == null || !(type.endsWith("error") || type.endsWith("warning") || type.endsWith("info")))
        {
            return false;
        }
        var text = annotation.getText();
        return text != null && !text.isBlank();
    }

    public String getLabel()
    {
        return Messages.FixProblemMarkerLabel;
    }

    public String getDescription()
    {
        return Messages.FixProblemMarkerDescription;
    }

    /**
     * Entry point for editor problem annotations (with or without a backing marker): resolves the
     * region from the active editor's annotation model and delegates to the core fix.
     *
     * @param combinedMessages when the line carries several problems, their messages joined into
     *     one text — used instead of the single annotation's message; may be {@code null}
     */
    public void fix(Annotation annotation, String combinedMessages)
    {
        var optionalViewer = ui.getLastSourceViewer();
        if (optionalViewer.isEmpty())
        {
            log.warning("External problem fix: no active source viewer", () -> String.valueOf(annotation)); //$NON-NLS-1$
            return;
        }
        SourceViewer viewer = optionalViewer.get();
        var model = viewer.getAnnotationModel();
        var position = model != null ? model.getPosition(annotation) : null;
        int offset = position != null ? position.getOffset() : -1;
        int length = position != null ? position.getLength() : 0;
        var messages = combinedMessages != null && !combinedMessages.isBlank() ? combinedMessages
            : annotation.getText();
        fix(viewer, offset, length, messages);
    }

    /**
     * Core fix: runs on the UI thread. Selects the problem region (or its whole line when the
     * region is empty), builds the details and sends one fix request.
     *
     * @param offset problem offset in the document, or {@code -1} when unknown
     * @param length problem length; non-positive selects the whole line of {@code offset}
     */
    @SuppressWarnings("nls")
    public void fix(SourceViewer viewer, int offset, int length, String messages)
    {
        try
        {
            IFile file = ui.getFile(viewer).orElse(null);
            if (file != null && editingSupport.isReadOnly(file.getProject()))
            {
                return;
            }

            int line = -1;
            var document = viewer.getDocument();
            if (document != null && offset >= 0)
            {
                line = document.getLineOfOffset(offset) + 1;
                int selectionOffset = offset;
                int selectionLength = length;
                if (selectionLength <= 0)
                {
                    var lineInfo = document.getLineInformationOfOffset(offset);
                    selectionOffset = lineInfo.getOffset();
                    selectionLength = lineInfo.getLength();
                }
                viewer.setSelectedRange(selectionOffset, selectionLength);
                viewer.revealRange(selectionOffset, selectionLength);
            }

            // The context must be built HERE, on the UI thread, from the live selection —
            // the skill rendering below completes on a background thread.
            var optionalContext = codeTools.createContextForTarget(viewer, CodeAction.FIX);
            if (optionalContext.isEmpty())
            {
                log.warning("External problem fix: no fix context", () -> String.valueOf(messages));
                return;
            }
            var ctx = optionalContext.get();

            var location = file != null ? file.getLocation() : null;
            // @formatter:off
            var skillRequest = new SkillExecutionRequest(SKILL_NAME,
                Map.of("problem_messages", messages == null ? "" : messages,
                       "absolute_file_path", location != null ? location.toOSString() : UNKNOWN_LINE,
                       "line", line > 0 ? String.valueOf(line) : UNKNOWN_LINE));
            // @formatter:on
            // The skill render completes on a background thread, while chat.askQuestion must run
            // on the UI thread: it opens the chat view, and getActivePage() is empty off-UI.
            skillExecutor.executeAsync(skillRequest, CancellationTokens.NONE)
                .thenAccept(result -> dispatcher.dispatchAsync(() -> chat.askQuestion(ctx, result.getPrompt())))
                .exceptionally(error -> {
                    log.logError(error);
                    return null;
                });
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }
}
