package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class LineTrimmedReplacer implements IReplacementStrategy
{
    private final IReplacements replacements;

    @Inject
	public LineTrimmedReplacer(IReplacements replacements)
	{
		Preconditions.checkNotNull(replacements);
		this.replacements = replacements;
	}

    @Override
    public Iterable<String> findCandidates(String content, String find)
    {
        List<String> matches = new ArrayList<>();
        String[] originalLines = replacements.splitLines(content);
        String[] searchLines = replacements.removeTrailingEmptyLine(replacements.splitLines(find));

        if (searchLines.length == 0)
        {
            return matches;
        }

		for (int i = 0; i <= originalLines.length - searchLines.length; i++)
		{
			boolean isMatch = true;

			for (int j = 0; j < searchLines.length; j++)
			{
				if (!originalLines[i + j].trim().equals(searchLines[j].trim()))
				{
					isMatch = false;
					break;
				}
			}

			if (!isMatch)
			{
				continue;
			}

			int endLine = i + searchLines.length - 1;
			matches.add(replacements.blockByLineRange(content, originalLines, i, endLine));
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 1;
	}
}
