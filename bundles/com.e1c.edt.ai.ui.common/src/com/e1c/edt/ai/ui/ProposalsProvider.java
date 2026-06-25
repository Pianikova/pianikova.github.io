/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProposalExtractor;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Proposal;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ProposalsProvider
    implements IProposalsProvider
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final ISettings uiSettings;
    private final IClock clock;
    private final IProposalExtractor proposalExtractor;
    private final IReflection reflection;

    @Inject
    public ProposalsProvider(ILog log, IDispatcher dispatcher, ISettings uiSettings, IClock clock,
        IProposalExtractor proposalExtractor, IReflection reflection)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(proposalExtractor);
        Preconditions.checkNotNull(reflection);
        this.log = log;
        this.dispatcher = dispatcher;
        this.uiSettings = uiSettings;
        this.clock = clock;
        this.proposalExtractor = proposalExtractor;
        this.reflection = reflection;
    }

    @Override
    public Optional<Proposal> getProposal(ICompletionProposal proposal, int minPriority, String prefix)
    {
        if (!(proposal instanceof ICompletionProposalExtension3))
        {
            return Optional.empty();
        }

        var prop = new Proposal();
        if (proposal instanceof ConfigurableCompletionProposal)
        {
            var completionProposal = ((ConfigurableCompletionProposal)proposal);
            prop.priority = completionProposal.getPriority();
            if (prop.priority < minPriority)
            {
                return Optional.empty();
            }

            var info = completionProposal.getAdditionalProposalInfo(new NullProgressMonitor());
            if (info != null)
            {
                prop.description = info.toString();
            }
        }

        var text = ((ICompletionProposalExtension3)proposal).getPrefixCompletionText(null, 0);
        prop.prefix = proposalExtractor.extract(prefix, text.toString()).orElse(null);
        if (prop.prefix == null)
        {
            return Optional.empty();
        }

        prop.displayString = proposal.getDisplayString();
        prop.text = text.toString();
        return Optional.of(prop);
    }


    @Override
    public Optional<List<Proposal>> getProposals(AIContext aiCtx, SourceViewer sourceViewer, int minPriority,
        ICancellationToken cancellationToken)
    {
        if (!uiSettings.isExperimental())
        {
            return Optional.empty();
        }

        if (sourceViewer.getDocument().getLength() > Consts.NORMAL_CODE_SIZE)
        {
            return Optional.empty();
        }

        var expirationDate = clock.now().plus(uiSettings.getMinRequestDelay());
        var ct = CancellationTokens.expiresAt(cancellationToken, clock, expirationDate);
        try
        {
            return getContentAssistant(sourceViewer)
                .flatMap(assistant -> getPartitionType(aiCtx, sourceViewer)
                    .map(partitionType -> assistant.getContentAssistProcessor(partitionType)))
                .flatMap(assistProcessor -> {
                    var offset = aiCtx.getSourceOffset();
                    try
                    {
                        return dispatcher.dispatch(() -> {
                            try
                            {
                                var result = assistProcessor.computeCompletionProposals(sourceViewer, offset);
                                // skip second page of context helper
                                assistProcessor.computeCompletionProposals(sourceViewer, offset);
                                return result;
                            }
                            catch (Throwable error)
                            {
                                // a third-party completion computer registered in the editor may fail with an
                                // Error (e.g. Recommenders -> NoClassDefFoundError: javax/annotation/PostConstruct);
                                // such foreign failures must not escape into the SWT event loop
                                log.logError(error);
                                return null;
                            }
                        });
                    }
                    catch (Exception error)
                    {
                        return Optional.empty();
                    }
                })
                .flatMap(proposals -> getProposals(proposals, minPriority, aiCtx.getPrefix(), ct));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    private Optional<IContentAssistant> getContentAssistant(SourceViewer sourceViewer)
    {
        return reflection.getField(SourceViewer.class, sourceViewer, "fContentAssistant", IContentAssistant.class); //$NON-NLS-1$
    }

    private Optional<String> getPartitionType(AIContext aiCtx, SourceViewer sourceViewer)
    {
        try
        {
            var document = sourceViewer.getDocument();
            if (document != null)
            {
                return Optional.ofNullable(document.getPartition(aiCtx.getSourceOffset()).getType());
            }
        }
        catch (BadLocationException error)
        {
            log.logError(error);
        }

        return Optional.empty();
    }

    private Optional<List<Proposal>> getProposals(ICompletionProposal[] proposals, int minPriority, String prefix,
        ICancellationToken cancellationToken)
    {
        var result = new ArrayList<Proposal>();
        for (var proposal : proposals)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var optionalProposal = getProposal(proposal, minPriority, prefix);
            if (optionalProposal.isEmpty())
            {
                break;
            }

            result.add(optionalProposal.get());
        }

        return Optional.of(result);
    }
}
