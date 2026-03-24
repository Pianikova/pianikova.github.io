package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class WhitespaceNormalizedReplacer implements IReplacementStrategy
{
	private final IReplacements replacements;

	@Inject
	public WhitespaceNormalizedReplacer(IReplacements replacements)
	{
		Preconditions.checkNotNull(replacements);
		this.replacements = replacements;
	}

	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		List<String> matches = new ArrayList<>();
		String normalizedFind = normalizeWhitespace(find);
		String[] lines = replacements.splitLines(content);

		for (String line : lines)
		{
			String normalizedLine = normalizeWhitespace(line);
			if (normalizedLine.equals(normalizedFind))
			{
				matches.add(line);
			}
			else if (normalizedLine.contains(normalizedFind))
			{
				String trimmedFind = find.trim();
				if (!trimmedFind.isEmpty())
				{
					String[] words = trimmedFind.split("\\s+"); //$NON-NLS-1$
					String pattern = Stream.of(words)
						.map(Pattern::quote)
						.collect(Collectors.joining("\\s+")); //$NON-NLS-1$
					try
					{
						Matcher matcher = Pattern.compile(pattern).matcher(line);
						if (matcher.find())
						{
							matches.add(matcher.group());
						}
					}
					catch (PatternSyntaxException e)
					{
						// Ignore invalid pattern and continue.
					}
				}
			}
		}

		String[] findLines = replacements.splitLines(find);
		if (findLines.length > 1)
		{
			for (int i = 0; i <= lines.length - findLines.length; i++)
			{
				String block = String.join("\n", slice(lines, i, i + findLines.length)); //$NON-NLS-1$
				if (normalizeWhitespace(block).equals(normalizedFind))
				{
					matches.add(block);
				}
			}
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 3;
	}

	private String normalizeWhitespace(String text)
	{
		return text.replaceAll("\\s+", " ").trim(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String[] slice(String[] array, int fromInclusive, int toExclusive)
	{
		String[] result = new String[toExclusive - fromInclusive];
		System.arraycopy(array, fromInclusive, result, 0, result.length);
		return result;
	}
}
