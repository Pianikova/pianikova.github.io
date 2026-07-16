/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ui.eclipse.quickfix;

import java.util.LinkedHashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IUI;
import com.e1c.edt.ai.ui.Images;
import com.e1c.edt.ai.ui.Messages;
import com.e1c.edt.ai.ui.handlers.ExternalProblemFixer;
import com.google.inject.Inject;

/**
 * JDT quick-fix processor that offers a single "Fix with 1C:Workmate" proposal for Java problems
 * in the editor (hover quick-fix list and Ctrl+1). The JDT correction machinery consults ONLY
 * registered {@link IQuickFixProcessor}s for Java problem annotations — marker resolutions from
 * {@code org.eclipse.ui.ide.markerResolution} are never shown there (they cover the Problems
 * view), hence this dedicated processor.
 * <p>
 * The proposal reuses the "Fix code" command flow: selects the problem region, builds the context
 * from the active editor and sends one fix request with all problem messages of the location.
 */
public class AIJavaQuickFixProcessor
    implements IQuickFixProcessor
{
    @Inject
    ISettings settings;
    @Inject
    ILog log;

    public AIJavaQuickFixProcessor()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean hasCorrections(ICompilationUnit unit, int problemId)
    {
        return settings.isEnabled();
    }

    @Override
    public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
        throws CoreException
    {
        if (!settings.isEnabled() || locations == null || locations.length == 0)
        {
            return new IJavaCompletionProposal[0];
        }

        // One aggregated proposal per invocation: all problem messages of the location go into a
        // single fix request instead of one indistinguishable entry per problem.
        var messages = new LinkedHashSet<String>();
        IProblem[] problems = context.getASTRoot() != null ? context.getASTRoot().getProblems() : new IProblem[0];
        for (IProblemLocation location : locations)
        {
            for (IProblem problem : problems)
            {
                if (problem.getID() == location.getProblemId() && problem.getSourceStart() == location.getOffset())
                {
                    messages.add(problem.getMessage());
                }
            }
        }

        var primary = locations[0];
        return new IJavaCompletionProposal[] {
            new FixWithWorkmateProposal(primary.getOffset(), primary.getLength(), String.join("\n", messages)) }; //$NON-NLS-1$
    }

    /**
     * The proposal itself: delegates to the shared {@link ExternalProblemFixer} core, which
     * selects the problem region and sends one fix request through the "Fix code" flow.
     */
    private static class FixWithWorkmateProposal
        implements IJavaCompletionProposal
    {
        @Inject
        IUI ui;
        @Inject
        ILog log;

        private final ExternalProblemFixer fixer = new ExternalProblemFixer();
        private final int offset;
        private final int length;
        private final String messages;

        FixWithWorkmateProposal(int offset, int length, String messages)
        {
            this.offset = offset;
            this.length = length;
            this.messages = messages;
            BaseActivator.injectMembers(this);
        }

        @Override
        public int getRelevance()
        {
            // Below JDT's own concrete fixes, above "configure problem severity"-style entries.
            return 1;
        }

        @Override
        public String getDisplayString()
        {
            return Messages.FixProblemMarkerLabel;
        }

        @Override
        public String getAdditionalProposalInfo()
        {
            return messages == null || messages.isBlank() ? Messages.FixProblemMarkerDescription : messages;
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

        @Override
        public Point getSelection(IDocument document)
        {
            return null;
        }

        @SuppressWarnings("nls")
        @Override
        public void apply(IDocument document)
        {
            ui.getLastSourceViewer().ifPresentOrElse(viewer -> fixer.fix(viewer, offset, length, messages),
                () -> log.warning("Fix Java problem: no active source viewer", () -> ""));
        }
    }
}
