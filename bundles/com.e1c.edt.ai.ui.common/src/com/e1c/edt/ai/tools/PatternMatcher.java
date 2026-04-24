/**
 * Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.regex.Pattern;
import com.google.inject.Singleton;

/**
 * Default implementation of glob pattern matching.
 * Supports:
 * - * - matches any characters within a segment
 * - ? - matches any single character within a segment
 * - ** - matches any number of directory segments (including zero)
 * - [abc] - matches any character in the set
 * - [a-z] - matches any character in the range
 * - {a,b,c} - matches any of the comma-separated alternatives
 * - Path separators can be / or \
 */
@Singleton
public class PatternMatcher
	implements IPatternMatcher
{
	@Override
	@SuppressWarnings("nls")
	public boolean matches(String path, String pattern)
	{
		String normalizedPath = path.replace("\\", "/");
		String normalizedPattern = pattern.replace("\\", "/");

		String[] pathParts = normalizedPath.split("/");
		String[] patternParts = normalizedPattern.split("/");

		return matchGlob(pathParts, 0, patternParts, 0);
	}

	@SuppressWarnings("nls")
	private boolean matchGlob(String[] pathParts, int pathIndex, String[] patternParts, int patternIndex)
	{
		if (patternIndex == patternParts.length)
		{
			return pathIndex == pathParts.length;
		}

		if (pathIndex == pathParts.length)
		{
			for (int i = patternIndex; i < patternParts.length; i++)
			{
				if (!patternParts[i].equals("**"))
				{
					return false;
				}
			}
			return true;
		}

		String currentPattern = patternParts[patternIndex];

		if (currentPattern.equals("**"))
		{
			if (patternIndex + 1 == patternParts.length)
			{
				return true;
			}

			// Option 1: ** matches zero segments, move to next pattern segment
			if (matchGlob(pathParts, pathIndex, patternParts, patternIndex + 1))
			{
				return true;
			}

			// Option 2: ** matches one or more path segments
			// Try consuming path segments one by one until we find a match
			for (int i = pathIndex; i < pathParts.length; i++)
			{
				if (matchGlob(pathParts, i + 1, patternParts, patternIndex + 1))
				{
					return true;
				}
			}

			return false;
		}

		if (matchSegment(pathParts[pathIndex], currentPattern))
		{
			return matchGlob(pathParts, pathIndex + 1, patternParts, patternIndex + 1);
		}

		return false;
	}


	@SuppressWarnings("nls")
	private boolean matchSegment(String segment, String pattern)
	{
		StringBuilder regex = new StringBuilder();
		for (int i = 0; i < pattern.length(); i++)
		{
			char c = pattern.charAt(i);
			switch (c)
			{
				case '*':
					regex.append(".*");
					break;
				case '?':
					regex.append(".");
					break;
				case '.':
					regex.append("\\.");
					break;
				case '[':
					regex.append(processCharacterClass(pattern, i));
					int closingBracket = pattern.indexOf(']', i);
					if (closingBracket != -1)
					{
						i = closingBracket;
					}
					break;
				case '{':
					regex.append(processBraceExpansion(pattern, i));
					int closingBrace = pattern.indexOf('}', i);
					if (closingBrace != -1)
					{
						i = closingBrace;
					}
					break;
				default:
					regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return segment.matches(regex.toString());
	}

	@SuppressWarnings("nls")
	private String processCharacterClass(String pattern, int startIndex)
	{
		int endIndex = pattern.indexOf(']', startIndex);
		if (endIndex == -1)
		{
			return Pattern.quote("[");
		}

		String content = pattern.substring(startIndex + 1, endIndex);
		StringBuilder regex = new StringBuilder("[");
		
		for (int i = 0; i < content.length(); i++)
		{
			char c = content.charAt(i);
			if (c == '-' && i > 0 && i < content.length() - 1)
			{
				char prev = content.charAt(i - 1);
				char next = content.charAt(i + 1);
				if (Character.isLetterOrDigit(prev) && Character.isLetterOrDigit(next) && prev < next)
				{
					regex.append(prev).append("-").append(next);
					i++;
				}
				else
				{
					regex.append("\\-");
				}
			}
			else
			{
				regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		
		regex.append("]");
		return regex.toString();
	}

	@SuppressWarnings("nls")
	private String processBraceExpansion(String pattern, int startIndex)
	{
		int endIndex = pattern.indexOf('}', startIndex);
		if (endIndex == -1)
		{
			return Pattern.quote("{");
		}

		String content = pattern.substring(startIndex + 1, endIndex);
		
		// Only treat as brace expansion if there's a comma (separator)
		if (!content.contains(","))
		{
			return Pattern.quote(pattern.substring(startIndex, endIndex + 1));
		}
		
		String[] alternatives = content.split(",");
		StringBuilder regex = new StringBuilder("(");
		
		for (int i = 0; i < alternatives.length; i++)
		{
			if (i > 0)
			{
				regex.append("|");
			}
			for (int j = 0; j < alternatives[i].length(); j++)
			{
				char c = alternatives[i].charAt(j);
				switch (c)
				{
					case '*':
						regex.append(".*");
						break;
					case '?':
						regex.append(".");
						break;
					case '.':
						regex.append("\\.");
						break;
					default:
						regex.append(Pattern.quote(String.valueOf(c)));
				}
			}
		}
		
		regex.append(")");
		return regex.toString();
	}
}
