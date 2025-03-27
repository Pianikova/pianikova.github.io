/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.assistent.model.Proposal;

public interface IProposalsProvider
{
    Optional<Proposal> getProposal(ICompletionProposal proposal, int minPriority, String prefix);

    Optional<List<Proposal>> getProposals(AIContext aiCtx, SourceViewer sourceViewer, int minPriority,
        ICancellationToken cancellationToken);
}
