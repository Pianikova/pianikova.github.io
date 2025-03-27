/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public class ProposalExtractor implements IProposalExtractor
{
    @Override
    public Optional<String> extract(String prefix, String proposal)
    {
        if (proposal == null || proposal.isBlank())
        {
            return Optional.empty();
        }

        int i;
        for (i = 0; i < proposal.length(); i++)
        {
            if (!Character.isLetterOrDigit(proposal.charAt(i)))
            {
                break;
            }
        }

        proposal = proposal.subSequence(0, i).toString();
        for (i = proposal.length(); i >= 0; i--)
        {
            var prefixEnd = proposal.substring(0, i);
            if (prefix.endsWith(prefixEnd))
            {
                proposal = proposal.substring(i);
                break;
            }
        }

        if (proposal == null || proposal.isBlank())
        {
            return Optional.empty();
        }

        return Optional.of(proposal);
    }
}
