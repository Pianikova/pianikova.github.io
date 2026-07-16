/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ui.eclipse.quickfix;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.handlers.AIMarkerResolutionGenerator;
import com.e1c.edt.ai.ui.handlers.ExternalProblemMarkerResolutionGenerator;
import com.google.inject.Inject;

/** Adds quick assists for AI and non-JDT problem markers in a Java editor. */
public class AIMarkerJavaQuickAssistProcessor
    implements IQuickAssistProcessor
{
    private static final String JDT_PROBLEM_MARKER = "org.eclipse.jdt.core.problem"; //$NON-NLS-1$

    private final AIMarkerResolutionGenerator aiGenerator = new AIMarkerResolutionGenerator();
    private final ExternalProblemMarkerResolutionGenerator problemGenerator =
        new ExternalProblemMarkerResolutionGenerator();

    @Inject
    ILog log;

    public AIMarkerJavaQuickAssistProcessor()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean hasAssists(IInvocationContext context) throws CoreException
    {
        return !getProposals(context).isEmpty();
    }

    @Override
    public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
        throws CoreException
    {
        Map<String, IJavaCompletionProposal> proposals = getProposals(context);
        return proposals.values().toArray(new IJavaCompletionProposal[proposals.size()]);
    }

    private Map<String, IJavaCompletionProposal> getProposals(IInvocationContext context) throws CoreException
    {
        Map<String, IJavaCompletionProposal> proposals = new LinkedHashMap<>();
        if (context == null || context.getCompilationUnit() == null)
        {
            return proposals;
        }
        IResource resource = context.getCompilationUnit().getResource();
        if (resource == null || !resource.exists())
        {
            return proposals;
        }
        for (IMarker marker : resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO))
        {
            if (!marker.exists() || JDT_PROBLEM_MARKER.equals(marker.getType()) || !isApplicable(marker, context))
            {
                continue;
            }
            IMarkerResolution[] resolutions = aiGenerator.getResolutions(marker);
            if (resolutions.length == 0)
            {
                resolutions = problemGenerator.getResolutions(marker);
            }
            for (IMarkerResolution resolution : resolutions)
            {
                IJavaCompletionProposal proposal = new MarkerProposal(marker, resolution);
                proposals.putIfAbsent(proposal.getDisplayString(), proposal);
            }
        }
        return proposals;
    }

    private static boolean isApplicable(IMarker marker, IInvocationContext context)
    {
        int markerStart = marker.getAttribute(IMarker.CHAR_START, -1);
        int markerEnd = marker.getAttribute(IMarker.CHAR_END, markerStart);
        if (markerStart < 0)
        {
            int markerLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
            if (markerLine < 1)
            {
                return false;
            }
            try
            {
                String source = context.getCompilationUnit().getBuffer().getContents();
                int selectionLine = new Document(source).getLineOfOffset(context.getSelectionOffset()) + 1;
                return markerLine == selectionLine;
            }
            catch (Exception e)
            {
                return false;
            }
        }
        int selectionStart = context.getSelectionOffset();
        int selectionEnd = selectionStart + Math.max(0, context.getSelectionLength());
        return selectionStart <= markerEnd && selectionEnd >= markerStart;
    }

    private final class MarkerProposal
        implements IJavaCompletionProposal
    {
        private final IMarker marker;
        private final IMarkerResolution resolution;

        MarkerProposal(IMarker marker, IMarkerResolution resolution)
        {
            this.marker = marker;
            this.resolution = resolution;
        }

        @Override
        public void apply(IDocument document)
        {
            try
            {
                resolution.run(marker);
            }
            catch (Exception e)
            {
                log.logError(e);
            }
        }

        @Override
        public Point getSelection(IDocument document)
        {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo()
        {
            return resolution instanceof IMarkerResolution2 ? ((IMarkerResolution2)resolution).getDescription() : null;
        }

        @Override
        public String getDisplayString()
        {
            return resolution.getLabel();
        }

        @Override
        public Image getImage()
        {
            return resolution instanceof IMarkerResolution2 ? ((IMarkerResolution2)resolution).getImage() : null;
        }

        @Override
        public IContextInformation getContextInformation()
        {
            return null;
        }

        @Override
        public int getRelevance()
        {
            return 1;
        }
    }
}
