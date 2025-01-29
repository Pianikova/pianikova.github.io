/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.custom.StyledText;

public interface IProposalsProvider
{
    Optional<String> getProposal(String content, ICompletionProposal proposal);

    Optional<List<String>> getProposals(AIContext aiCtx, StyledText textWidget, ICancellationToken cancellationToken);
}
