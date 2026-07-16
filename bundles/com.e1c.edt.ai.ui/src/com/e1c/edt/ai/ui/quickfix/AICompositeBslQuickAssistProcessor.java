/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.quickfix;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.xtext.ui.editor.quickfix.XtextQuickAssistProcessor;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.Images;
import com.e1c.edt.ai.ui.handlers.AIMarkerResolutionGenerator;
import com.e1c.edt.ai.ui.handlers.ExternalProblemFixer;
import com.e1c.edt.ai.ui.handlers.ExternalProblemMarkerResolutionGenerator;

/**
 * Adds Workmate proposals to the native BSL quick-assist processor without replacing its
 * proposals. The same instance is installed into the source viewer and the annotation hover, so
 * Ctrl+1 and the hover list are built from one proposal set.
 */
public class AICompositeBslQuickAssistProcessor
    extends XtextQuickAssistProcessor
{
    private final IQuickAssistProcessor delegate;
    private final AIMarkerResolutionGenerator aiGenerator = new AIMarkerResolutionGenerator();
    private final ExternalProblemMarkerResolutionGenerator problemGenerator =
        new ExternalProblemMarkerResolutionGenerator();
    private final ExternalProblemFixer problemFixer = new ExternalProblemFixer();
    private final ILog log;

    public AICompositeBslQuickAssistProcessor(IQuickAssistProcessor delegate, ILog log)
    {
        this.delegate = delegate;
        this.log = log;
    }

    @Override
    public boolean canFix(Annotation annotation)
    {
        return canFixWithWorkmate(annotation) || delegate != null && delegate.canFix(annotation);
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext context)
    {
        return hasWorkmateAnnotation(context) || delegate != null && delegate.canAssist(context);
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context)
    {
        // Workmate proposals are inserted first. If the legacy @Fix provider produced the same
        // label, the native duplicate is discarded and the proposal with the Workmate icon wins.
        Map<String, ICompletionProposal> proposals = new LinkedHashMap<>();
        addWorkmateProposals(context, proposals);
        if (delegate != null)
        {
            try
            {
                ICompletionProposal[] nativeProposals = delegate.computeQuickAssistProposals(context);
                if (nativeProposals != null)
                {
                    for (ICompletionProposal proposal : nativeProposals)
                    {
                        proposals.putIfAbsent(proposal.getDisplayString(), proposal);
                    }
                }
            }
            catch (Exception e)
            {
                log.logError(e);
            }
        }
        return proposals.values().toArray(new ICompletionProposal[proposals.size()]);
    }

    @Override
    public String getErrorMessage()
    {
        return delegate != null ? delegate.getErrorMessage() : null;
    }

    private boolean hasWorkmateAnnotation(IQuickAssistInvocationContext context)
    {
        if (context == null || context.getSourceViewer() == null)
        {
            return false;
        }
        IAnnotationModel model = context.getSourceViewer().getAnnotationModel();
        if (model == null)
        {
            return false;
        }
        Iterator<?> iterator = model.getAnnotationIterator();
        while (iterator.hasNext())
        {
            Annotation annotation = (Annotation)iterator.next();
            Position position = model.getPosition(annotation);
            if (isApplicable(position, context) && canFixWithWorkmate(annotation))
            {
                return true;
            }
        }
        return false;
    }

    private void addWorkmateProposals(IQuickAssistInvocationContext context,
        Map<String, ICompletionProposal> proposals)
    {
        if (context == null || !(context.getSourceViewer() instanceof SourceViewer))
        {
            return;
        }
        SourceViewer viewer = (SourceViewer)context.getSourceViewer();
        IAnnotationModel model = viewer.getAnnotationModel();
        if (model == null)
        {
            return;
        }

        Annotation primaryProblem = null;
        Position primaryPosition = null;
        LinkedHashSet<String> problemMessages = new LinkedHashSet<>();
        Iterator<?> iterator = model.getAnnotationIterator();
        while (iterator.hasNext())
        {
            Annotation annotation = (Annotation)iterator.next();
            Position position = model.getPosition(annotation);
            if (!isApplicable(position, context))
            {
                continue;
            }
            if (annotation instanceof MarkerAnnotation)
            {
                IMarker marker = ((MarkerAnnotation)annotation).getMarker();
                if (marker == null || !marker.exists())
                {
                    continue;
                }
                IMarkerResolution[] aiResolutions = aiGenerator.getResolutions(marker);
                if (aiResolutions.length > 0)
                {
                    for (IMarkerResolution resolution : aiResolutions)
                    {
                        ICompletionProposal proposal = new MarkerResolutionProposal(marker, resolution);
                        proposals.putIfAbsent(proposal.getDisplayString(), proposal);
                    }
                    continue;
                }
                if (!problemGenerator.hasResolutions(marker))
                {
                    continue;
                }
            }
            else if (!problemFixer.canFix(annotation))
            {
                continue;
            }

            if (annotation.getText() != null && !annotation.getText().isBlank())
            {
                problemMessages.add(annotation.getText());
            }
            if (primaryProblem == null)
            {
                primaryProblem = annotation;
                primaryPosition = position;
            }
        }

        if (primaryProblem != null && primaryPosition != null)
        {
            String messages = String.join("\n", problemMessages); //$NON-NLS-1$
            ICompletionProposal proposal = new StandardProblemProposal(viewer, primaryPosition, messages);
            proposals.putIfAbsent(proposal.getDisplayString(), proposal);
        }
    }

    private boolean canFixWithWorkmate(Annotation annotation)
    {
        if (annotation instanceof MarkerAnnotation)
        {
            IMarker marker = ((MarkerAnnotation)annotation).getMarker();
            return marker != null && marker.exists()
                && (aiGenerator.hasResolutions(marker) || problemGenerator.hasResolutions(marker));
        }
        return problemFixer.canFix(annotation);
    }

    private static boolean isApplicable(Position position, IQuickAssistInvocationContext context)
    {
        if (position == null)
        {
            return false;
        }
        int start = context.getOffset();
        int end = start + Math.max(0, context.getLength());
        int positionStart = position.getOffset();
        int positionEnd = positionStart + Math.max(0, position.getLength());
        return start <= positionEnd && end >= positionStart;
    }

    private final class StandardProblemProposal
        implements ICompletionProposal
    {
        private final SourceViewer viewer;
        private final Position position;
        private final String messages;

        StandardProblemProposal(SourceViewer viewer, Position position, String messages)
        {
            this.viewer = viewer;
            this.position = position;
            this.messages = messages;
        }

        @Override
        public void apply(IDocument document)
        {
            problemFixer.fix(viewer, position.getOffset(), position.getLength(), messages);
        }

        @Override
        public Point getSelection(IDocument document)
        {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo()
        {
            return messages == null || messages.isBlank() ? problemFixer.getDescription() : messages;
        }

        @Override
        public String getDisplayString()
        {
            return problemFixer.getLabel();
        }

        @Override
        public Image getImage()
        {
            return BaseActivator.getImage(Images.AI);
        }

        @Override
        public IContextInformation getContextInformation()
        {
            return null;
        }
    }

    private static final class MarkerResolutionProposal
        implements ICompletionProposal
    {
        private final IMarker marker;
        private final IMarkerResolution resolution;

        MarkerResolutionProposal(IMarker marker, IMarkerResolution resolution)
        {
            this.marker = marker;
            this.resolution = resolution;
        }

        @Override
        public void apply(IDocument document)
        {
            resolution.run(marker);
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
    }
}
