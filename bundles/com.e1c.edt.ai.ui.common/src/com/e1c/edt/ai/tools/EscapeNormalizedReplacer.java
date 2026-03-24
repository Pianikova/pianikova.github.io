package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

public class EscapeNormalizedReplacer implements IReplacementStrategy
{
	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		List<String> matches = new ArrayList<>();
		String unescapedFind = unescape(find);

		if (content.contains(unescapedFind))
		{
			matches.add(unescapedFind);
		}

		String[] lines = ReplacementStrategyUtils.splitLines(content);
		String[] findLines = ReplacementStrategyUtils.splitLines(unescapedFind);

		for (int i = 0; i <= lines.length - findLines.length; i++)
		{
			String block = String.join("\n", slice(lines, i, i + findLines.length)); //$NON-NLS-1$
			if (unescape(block).equals(unescapedFind))
			{
				matches.add(block);
			}
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 5;
	}

	private String unescape(String text)
	{
		StringBuilder result = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++)
		{
			char current = text.charAt(i);
			if (current == '\\' && i + 1 < text.length())
			{
				char next = text.charAt(i + 1);
				Character mapped = mapEscape(next);
				if (mapped != null)
				{
					result.append(mapped.charValue());
					i++;
					continue;
				}
			}

			result.append(current);
		}

		return result.toString();
	}

	private Character mapEscape(char c)
	{
		switch (c)
		{
		case 'n':
			return Character.valueOf('\n');
		case 't':
			return Character.valueOf('\t');
		case 'r':
			return Character.valueOf('\r');
		case '\'':
			return Character.valueOf('\'');
		case '\"':
			return Character.valueOf('\"');
		case '`':
			return Character.valueOf('`');
		case '\\':
			return Character.valueOf('\\');
		case '\n':
			return Character.valueOf('\n');
		case '$':
			return Character.valueOf('$');
		default:
			return null;
		}
	}

	private String[] slice(String[] array, int fromInclusive, int toExclusive)
	{
		String[] result = new String[toExclusive - fromInclusive];
		System.arraycopy(array, fromInclusive, result, 0, result.length);
		return result;
	}
}
