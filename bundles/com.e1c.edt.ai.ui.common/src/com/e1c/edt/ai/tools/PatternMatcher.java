/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import com.google.inject.Singleton;

/**
 * Default implementation of glob pattern matching.
 * Supports:
 * - * - matches any characters within a segment
 * - ? - matches any single character within a segment
 * - ** - matches any number of directory segments (including zero)
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

			if (matchGlob(pathParts, pathIndex, patternParts, patternIndex + 1))
			{
				return true;
			}

			return matchGlob(pathParts, pathIndex + 1, patternParts, patternIndex);
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
				default:
					regex.append(c);
			}
		}
		return segment.matches(regex.toString());
	}
}
