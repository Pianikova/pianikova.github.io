/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.quickfix;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.ui.editor.model.edit.IModification;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com._1c.g5.v8.dt.bsl.ui.quickfix.AbstractExternalQuickfixProvider;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.tools.MarkerType;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IUI;
import com.e1c.edt.ai.ui.Messages;
import com.e1c.edt.ai.ui.handlers.AIMarkerResolution;
import com.e1c.edt.ai.ui.handlers.ExternalProblemFixer;
import com.google.inject.Inject;

/**
 * Native BSL-editor quick fixes (Ctrl+1 / light bulb / hover) dispatched through the
 * {@code com._1c.g5.v8.dt.bsl.ui.externalQuickfixProvider} extension point. Two families:
 * <ul>
 * <li><b>AI markers</b> — every ai-marker carries the Xtext issue code
 * {@link SetMarkersMcpTool#AI_QUICKFIX_ISSUE_CODE} (stamped by {@code SetMarkersMcpTool}); the fix
 * reuses {@link AIMarkerResolution} built from the marker's {@code action_*} attributes.</li>
 * <li><b>Standard problems with FIXED Xtext codes</b> — syntax and linking diagnostics; the fix
 * goes through the shared {@link ExternalProblemFixer} core (same as Problems view / hover).
 * EDT check-system issues have a per-check code, so they cannot be covered by {@code @Fix}
 * dispatch — for them use the hover button or the Problems view.</li>
 * </ul>
 */
public class AIExternalQuickfixProvider
    extends AbstractExternalQuickfixProvider
{
    private static final String[] AI_MARKER_TYPES =
        { MarkerType.AI_MARKER_ERROR, MarkerType.AI_MARKER_WARNING, MarkerType.AI_MARKER_INFO };

    // Fixed Xtext diagnostic codes (org.eclipse.xtext.diagnostics.Diagnostic constants,
    // duplicated as literals: @Fix needs compile-time constants and this bundle should not
    // depend on the Xtext core class for three strings).
    private static final String XTEXT_SYNTAX_CODE = "org.eclipse.xtext.diagnostics.Diagnostic.Syntax"; //$NON-NLS-1$
    private static final String XTEXT_SYNTAX_RANGE_CODE = "org.eclipse.xtext.diagnostics.Diagnostic.Syntax.Range"; //$NON-NLS-1$
    private static final String XTEXT_LINKING_CODE = "org.eclipse.xtext.diagnostics.Diagnostic.Linking"; //$NON-NLS-1$

    @Inject
    IJson json;
    @Inject
    ISettings settings;
    @Inject
    IUI ui;
    @Inject
    ILog log;

    private final ExternalProblemFixer fixer = new ExternalProblemFixer();

    public AIExternalQuickfixProvider()
    {
        BaseActivator.injectMembers(this);
    }

    @Fix(XTEXT_SYNTAX_CODE)
    public void fixSyntaxProblem(Issue issue, IssueResolutionAcceptor acceptor)
    {
        acceptStandardProblemFix(issue, acceptor);
    }

    @Fix(XTEXT_SYNTAX_RANGE_CODE)
    public void fixSyntaxRangeProblem(Issue issue, IssueResolutionAcceptor acceptor)
    {
        acceptStandardProblemFix(issue, acceptor);
    }

    @Fix(XTEXT_LINKING_CODE)
    public void fixLinkingProblem(Issue issue, IssueResolutionAcceptor acceptor)
    {
        acceptStandardProblemFix(issue, acceptor);
    }

    /**
     * One "Fix with 1C:Workmate" proposal for a standard problem with a fixed Xtext code. The
     * modification runs the shared {@link ExternalProblemFixer} core against the editor viewer —
     * the same flow as the Problems view and the hover button.
     */
    private void acceptStandardProblemFix(Issue issue, IssueResolutionAcceptor acceptor)
    {
        if (!settings.isEnabled() || issue.getMessage() == null || issue.getMessage().isBlank())
        {
            return;
        }
        acceptor.accept(issue, fixer.getLabel(), fixer.getDescription(), null, (IModification)context -> {
            var viewer = getTextViewer(context);
            var sourceViewer = viewer instanceof SourceViewer ? (SourceViewer)viewer
                : ui.getLastSourceViewer().orElse(null);
            if (sourceViewer == null)
            {
                log.warning("Fix standard problem: no source viewer", () -> String.valueOf(issue)); //$NON-NLS-1$
                return;
            }
            int offset = issue.getOffset() != null ? issue.getOffset().intValue() : -1;
            int length = issue.getLength() != null ? issue.getLength().intValue() : 0;
            fixer.fix(sourceViewer, offset, length, issue.getMessage());
        });
    }

    @Fix(SetMarkersMcpTool.AI_QUICKFIX_ISSUE_CODE)
    public void fixWithWorkmate(Issue issue, IssueResolutionAcceptor acceptor)
    {
        try
        {
            IMarker marker = findMarker(issue);
            if (marker == null)
            {
                return;
            }
            var chatId = marker.getAttribute(SetMarkersMcpTool.ACTION_CHAT_ID_ATTRIBUTE, null);
            var details = marker.getAttribute(SetMarkersMcpTool.ACTION_DETAILS_ATTRIBUTE, null);
            if (chatId == null || details == null)
            {
                return; // marker without a prepared fix action (e.g. info-level finding)
            }
            var optionalRequest = json.deserialize(details, SetMarkersMcpTool.MarkerRequest.class);
            if (optionalRequest.isEmpty())
            {
                return;
            }
            var resolution = new AIMarkerResolution(chatId, optionalRequest.get());
            acceptor.accept(issue, resolution.getLabel(), resolution.getDescription(), null,
                (IModification)context -> resolution.run(marker));
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }

    @Fix(SetMarkersMcpTool.AI_QUICKFIX_ISSUE_CODE)
    public void dismiss(Issue issue, IssueResolutionAcceptor acceptor)
    {
        IMarker marker = findMarker(issue);
        if (marker == null)
        {
            return;
        }
        acceptor.accept(issue, Messages.DismissAIMarker, Messages.DismissAIMarkerDescription, null,
            (IModification)context -> marker.delete());
    }

    /**
     * Resolves the issue back to its ai-marker: the issue was created from the marker's
     * attributes, so the offset equals the marker's {@code CHAR_START}; the line number is the
     * fallback when char positions are absent.
     */
    private IMarker findMarker(Issue issue)
    {
        try
        {
            var uri = issue.getUriToProblem();
            if (uri == null || !uri.isPlatformResource())
            {
                return null;
            }
            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(uri.toPlatformString(true)));
            if (file == null || !file.exists())
            {
                return null;
            }
            IMarker lineMatch = null;
            for (String type : AI_MARKER_TYPES)
            {
                for (IMarker marker : file.findMarkers(type, false, IResource.DEPTH_ZERO))
                {
                    var charStart = marker.getAttribute(IMarker.CHAR_START, -1);
                    if (issue.getOffset() != null && charStart == issue.getOffset().intValue())
                    {
                        return marker;
                    }
                    var line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                    if (lineMatch == null && issue.getLineNumber() != null && line == issue.getLineNumber().intValue())
                    {
                        lineMatch = marker;
                    }
                }
            }
            return lineMatch;
        }
        catch (Exception e)
        {
            log.logError(e);
            return null;
        }
    }
}
