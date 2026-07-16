/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.ui.handlers;

import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.Images;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Quick fix of an AI marker: the fix request is rendered from the {@code quick-fix-ai-marker}
 * skill (skills/quick-fix-ai-marker/SKILL.md) — the marker's {@code action_prompt}, its JSON
 * details, the other problems of the same line and the read/edit/re-check-markers procedure —
 * and sent as a continuation of the SOURCE conversation (the one that created the marker), so
 * the model keeps the original review context.
 */
public class AIMarkerResolution
    implements IMarkerResolution2
{
    private static final String SKILL_NAME = "quick-fix-ai-marker"; //$NON-NLS-1$
    private static final String UNKNOWN_VALUE = "?"; //$NON-NLS-1$
    private static final String NO_ADDITIONAL_PROBLEMS = "нет"; //$NON-NLS-1$

    private final String sourceChatId;
    private final SetMarkersMcpTool.MarkerRequest markerRequest;
    // Messages of OTHER problems reported on the same line (e.g. standard BSL validation
    // errors); passed into the skill so a single fix request covers the whole line.
    private String additionalProblems;

    @Inject
    IChat chat;
    @Inject
    IJson json;
    @Inject
    ISkillExecutor skillExecutor;
    @Inject
    IDispatcher dispatcher;
    @Inject
    ILog log;

    public AIMarkerResolution(String sourceChatId, SetMarkersMcpTool.MarkerRequest markerRequest)
    {
        Preconditions.checkNotNull(sourceChatId);
        Preconditions.checkNotNull(markerRequest);
        this.sourceChatId = sourceChatId;
        this.markerRequest = markerRequest;
        BaseActivator.injectMembers(this);
    }

    @Override
    public String getLabel()
    {
        return markerRequest.actionTitle != null ? markerRequest.actionTitle : "Apply AI Suggestion"; //$NON-NLS-1$
    }

    /**
     * Sets messages of other problems on the marker's line to include into the fix request.
     */
    public void setAdditionalProblems(String additionalProblems)
    {
        this.additionalProblems = additionalProblems;
    }

    @SuppressWarnings("nls")
    @Override
    public void run(IMarker marker)
    {
        // @formatter:off
        var skillRequest = new SkillExecutionRequest(SKILL_NAME,
            Map.of("action_prompt", markerRequest.actionPrompt == null ? "" : markerRequest.actionPrompt,
                   "marker_details", json.serialize(markerRequest),
                   "marker_id", String.valueOf(markerRequest.id),
                   "absolute_file_path", markerRequest.absoluteFilePath == null
                       ? UNKNOWN_VALUE : markerRequest.absoluteFilePath,
                   "line", markerRequest.startLine != null ? String.valueOf(markerRequest.startLine) : UNKNOWN_VALUE,
                   "additional_problems", additionalProblems == null || additionalProblems.isBlank()
                       ? NO_ADDITIONAL_PROBLEMS : additionalProblems));
        // @formatter:on
        // The skill render completes on a background thread, while chat.continueChat must run on
        // the UI thread: it opens the chat view, and getActivePage() is empty off-UI.
        skillExecutor.executeAsync(skillRequest, CancellationTokens.NONE)
            .thenAccept(result -> dispatcher.dispatchAsync(() -> chat.continueChat(sourceChatId, result.getPrompt())))
            .exceptionally(error -> {
                log.logError(error);
                return null;
            });
    }

    @Override
    public String getDescription()
    {
        return markerRequest.actionDescription != null ? markerRequest.actionDescription
            : "Execute AI-assisted code transformation"; //$NON-NLS-1$
    }

    @Override
    public Image getImage()
    {
        return BaseActivator.getImage(Images.AI);
    }
}
