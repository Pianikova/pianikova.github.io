/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.ILog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.xtext.ui.editor.XtextEditor;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ProposalsProvider
    implements IProposalsProvider
{
    private final ILog log;
    private final IUI ui;
    private final IDispatcher dispatcher;

    @Inject
    public ProposalsProvider(ILog log, IUI ui, IDispatcher dispatcher)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        this.log = log;
        this.ui = ui;
        this.dispatcher = dispatcher;
    }

    @Override
    public Optional<String> getProposal(String content, ICompletionProposal proposal)
    {
        var proposalDoc = new Document(content);
        proposal.apply(proposalDoc);
        var newContent = proposalDoc.get();
        var min = Integer.min(content.length(), newContent.length());
        int start;
        for (start = 0; start < min; start++)
        {
            if (content.charAt(start) != newContent.charAt(start))
            {
                break;
            }
        }

        if (start == min)
        {
            return Optional.empty();
        }

        int finish;
        var max = Integer.max(content.length(), newContent.length());
        for (finish = max - 1; finish > start; finish--)
        {
            if (content.charAt(finish - (max - content.length())) != newContent
                .charAt(finish - (max - newContent.length())))
            {
                break;
            }
        }

        var result = newContent.substring(start, finish + 1);
        if (!result.isBlank() && !proposal.getDisplayString().startsWith(result))
        {
            for (finish = 0; finish < result.length(); finish++)
            {
                if (!Character.isLetterOrDigit(result.charAt(finish)))
                {
                    finish++;
                    break;
                }
            }

            if (finish > result.length())
            {
                finish = result.length();
            }

            result = result.substring(0, finish);
        }

        if (result.isBlank())
        {
            return Optional.empty();
        }

        return Optional.of(result);
    }


    @Override
    public Optional<List<String>> getProposals(AIContext aiCtx, StyledText textWidget,
        ICancellationToken cancellationToken)
    {
        return dispatcher.dispatch(() -> ui.getSourceViewer(textWidget).flatMap(sourceViewer -> {
            return ui.getEditor(sourceViewer)
                .map(editor -> editor.getAdapter(XtextEditor.class))
                .map(editor -> editor.getXtextSourceViewerConfiguration())
                .map(config -> config.getContentAssistant(sourceViewer))
                .flatMap(assistant -> getPartitionType(aiCtx, sourceViewer)
                    .map(partitionType -> assistant.getContentAssistProcessor(partitionType)))
                .map(assistProcessor -> {
                    var offset = aiCtx.getSourceOffset();
                    var proposals = assistProcessor.computeCompletionProposals(sourceViewer, offset);
                    // skip second page of context helper
                    assistProcessor.computeCompletionProposals(sourceViewer, offset);
                    return proposals;
                })
                .flatMap(proposals -> getProposals(aiCtx, proposals));
        })).flatMap(i -> i);
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

    private Optional<List<String>> getProposals(AIContext aiCtx, ICompletionProposal[] proposals)
    {
        var result = new ArrayList<String>();
        for (var proposal : proposals)
        {
            getProposal(aiCtx.getSource(), proposal).ifPresent(prop -> result.add(prop));
        }

        return Optional.of(result);
    }
}
