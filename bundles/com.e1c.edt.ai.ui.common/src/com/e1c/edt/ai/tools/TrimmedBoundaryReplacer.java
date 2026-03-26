package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class TrimmedBoundaryReplacer implements IReplacementStrategy
{
	private final IReplacements replacements;

	@Inject
	public TrimmedBoundaryReplacer(IReplacements replacements)
	{
		Preconditions.checkNotNull(replacements);
		this.replacements = replacements;
	}

	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		List<String> matches = new ArrayList<>();
		String trimmedFind = find.trim();

		if (trimmedFind.equals(find))
		{
			return matches;
		}

		if (content.contains(trimmedFind))
		{
			matches.add(trimmedFind);
		}

		String[] lines = replacements.splitLines(content);
		String[] findLines = replacements.splitLines(find);
		for (int i = 0; i <= lines.length - findLines.length; i++)
		{
			String block = String.join("\n", slice(lines, i, i + findLines.length)); //$NON-NLS-1$
			if (block.trim().equals(trimmedFind))
			{
				matches.add(block);
			}
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 6;
	}

	private String[] slice(String[] array, int fromInclusive, int toExclusive)
	{
		String[] result = new String[toExclusive - fromInclusive];
		System.arraycopy(array, fromInclusive, result, 0, result.length);
		return result;
	}
}
