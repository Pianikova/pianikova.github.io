package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class IndentationFlexibleReplacer implements IReplacementStrategy
{
	private static final Pattern LEADING_WHITESPACE_PATTERN = Pattern.compile("^(\\s*)"); //$NON-NLS-1$

	private final IReplacements replacements;

	@Inject
	public IndentationFlexibleReplacer(IReplacements replacements)
	{
		Preconditions.checkNotNull(replacements);
		this.replacements = replacements;
	}

	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		List<String> matches = new ArrayList<>();
		String normalizedFind = removeIndentation(find);
		String[] contentLines = replacements.splitLines(content);
		String[] findLines = replacements.splitLines(find);

		for (int i = 0; i <= contentLines.length - findLines.length; i++)
		{
			String block = String.join("\n", slice(contentLines, i, i + findLines.length)); //$NON-NLS-1$
			if (removeIndentation(block).equals(normalizedFind))
			{
				matches.add(block);
			}
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 4;
	}

	private String removeIndentation(String text)
	{
		String[] lines = replacements.splitLines(text);

		int minIndent = Integer.MAX_VALUE;
		boolean hasNonEmptyLines = false;
		for (String line : lines)
		{
			if (line.trim().isEmpty())
			{
				continue;
			}
			hasNonEmptyLines = true;

			Matcher matcher = LEADING_WHITESPACE_PATTERN.matcher(line);
			int indentLength = 0;
			if (matcher.find())
			{
				indentLength = matcher.group(1).length();
			}
			minIndent = Math.min(minIndent, indentLength);
		}

		if (!hasNonEmptyLines)
		{
			return text;
		}

		String[] normalized = new String[lines.length];
		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i];
			if (line.trim().isEmpty())
			{
				normalized[i] = line;
			}
			else
			{
				int from = Math.min(minIndent, line.length());
				normalized[i] = line.substring(from);
			}
		}

		return String.join("\n", normalized); //$NON-NLS-1$
	}

	private String[] slice(String[] array, int fromInclusive, int toExclusive)
	{
		String[] result = new String[toExclusive - fromInclusive];
		System.arraycopy(array, fromInclusive, result, 0, result.length);
		return result;
	}
}
