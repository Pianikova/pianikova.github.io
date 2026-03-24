package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

public class LineTrimmedReplacer implements IReplacementStrategy
{
	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		List<String> matches = new ArrayList<>();
		String[] originalLines = ReplacementStrategyUtils.splitLines(content);
		String[] searchLines = ReplacementStrategyUtils.removeTrailingEmptyLine(ReplacementStrategyUtils.splitLines(find));

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
			matches.add(ReplacementStrategyUtils.blockByLineRange(content, originalLines, i, endLine));
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 1;
	}
}
