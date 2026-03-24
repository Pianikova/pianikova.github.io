package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

public class ContextAwareReplacer implements IReplacementStrategy
{
	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		List<String> matches = new ArrayList<>();

		String[] findLines = ReplacementStrategyUtils.splitLines(find);
		if (findLines.length < 3)
		{
			return matches;
		}

		findLines = ReplacementStrategyUtils.removeTrailingEmptyLine(findLines);
		if (findLines.length == 0)
		{
			return matches;
		}

		String[] contentLines = ReplacementStrategyUtils.splitLines(content);
		String firstLine = findLines[0].trim();
		String lastLine = findLines[findLines.length - 1].trim();

		for (int i = 0; i < contentLines.length; i++)
		{
			if (!contentLines[i].trim().equals(firstLine))
			{
				continue;
			}

			for (int j = i + 2; j < contentLines.length; j++)
			{
				if (!contentLines[j].trim().equals(lastLine))
				{
					continue;
				}

				String[] blockLines = slice(contentLines, i, j + 1);
				if (blockLines.length == findLines.length && hasReasonableSimilarity(blockLines, findLines))
				{
					matches.add(String.join("\n", blockLines)); //$NON-NLS-1$
					break;
				}
				break;
			}
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 7;
	}

	private boolean hasReasonableSimilarity(String[] blockLines, String[] findLines)
	{
		int matchingLines = 0;
		int totalNonEmptyLines = 0;

		for (int i = 1; i < blockLines.length - 1; i++)
		{
			String blockLine = blockLines[i].trim();
			String findLine = findLines[i].trim();
			if (!blockLine.isEmpty() || !findLine.isEmpty())
			{
				totalNonEmptyLines++;
				if (blockLine.equals(findLine))
				{
					matchingLines++;
				}
			}
		}

		return totalNonEmptyLines == 0 || ((double)matchingLines / totalNonEmptyLines) >= 0.5;
	}

	private String[] slice(String[] array, int fromInclusive, int toExclusive)
	{
		String[] result = new String[toExclusive - fromInclusive];
		System.arraycopy(array, fromInclusive, result, 0, result.length);
		return result;
	}
}
