/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.lang.reflect.Field;
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
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.assistent.model.Proposal;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ProposalsProvider
    implements IProposalsProvider
{
    private static Field contentAssistantField;
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IUISettings uiSettings;
    private final IClock clock;

    static
    {
        var fields = SourceViewer.class.getDeclaredFields();
        for (var field : fields)
        {
            if ("fContentAssistant".equals(field.getName())) //$NON-NLS-1$
            {
                field.setAccessible(true);
                try
                {
                    contentAssistantField = field;
                    break;
                }
                catch (Exception e)
                {
                    //
                }
            }
        }
    }

    @Inject
    public ProposalsProvider(ILog log, IDispatcher dispatcher, IUISettings uiSettings, IClock clock)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(clock);
        this.log = log;
        this.dispatcher = dispatcher;
        this.uiSettings = uiSettings;
        this.clock = clock;
    }

    @Override
    public Optional<Proposal> getProposal(ICompletionProposal proposal, int minPriority)
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
        if (text == null || text.length() == 0)
        {
            return Optional.empty();
        }

        int i;
        for (i = 0; i < text.length() - 1; i++)
        {
            if (!Character.isLetterOrDigit(text.charAt(i)))
            {
                break;
            }
        }

        prop.displayString = proposal.getDisplayString();
        prop.text = text.toString();
        prop.prefix = text.subSequence(0, i).toString();
        return Optional.of(prop);
    }


    @Override
    public Optional<List<Proposal>> getProposals(AIContext aiCtx, SourceViewer sourceViewer, int minPriority,
        ICancellationToken cancellationToken)
    {
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
                            var result = assistProcessor.computeCompletionProposals(sourceViewer, offset);
                            // skip second page of context helper
                            assistProcessor.computeCompletionProposals(sourceViewer, offset);
                            return result;
                        });
                    }
                    catch (Exception error)
                    {
                        return Optional.empty();
                    }
                })
                .flatMap(proposals -> getProposals(proposals, minPriority, ct));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    private Optional<IContentAssistant> getContentAssistant(SourceViewer sourceViewer)
    {
        try
        {
            if (contentAssistantField != null)
            {
                var contentAssistant = contentAssistantField.get(sourceViewer);
                if (contentAssistant instanceof IContentAssistant)
                {
                    return Optional.ofNullable((IContentAssistant)contentAssistant);
                }
            }
        }
        catch (Exception e)
        {
            //
        }

        return Optional.empty();
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

    private Optional<List<Proposal>> getProposals(ICompletionProposal[] proposals, int minPriority,
        ICancellationToken cancellationToken)
    {
        var result = new ArrayList<Proposal>();
        for (var proposal : proposals)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var optionalProposal = getProposal(proposal, minPriority);
            if (optionalProposal.isEmpty())
            {
                break;
            }

            result.add(optionalProposal.get());
        }

        return Optional.of(result);
    }
}
