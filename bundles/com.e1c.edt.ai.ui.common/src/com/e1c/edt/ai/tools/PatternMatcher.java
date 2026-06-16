/**
 * Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;
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
 * - [!abc] / [^abc] - negated character class
 * - {a,b,c} - matches any of the comma-separated alternatives (supports nesting and wildcards)
 * - Path separators can be / or \
 *
 * Anchoring (gitignore-style): a pattern that contains no path separator is NOT
 * anchored to the base directory - it is matched against the file name (the last
 * path segment) at any depth. So "*abc*" matches "a/b/MyAbcFile.bsl". A pattern
 * that contains a "/" stays anchored and is matched segment-by-segment (use "**"
 * to span directories), so "src/*.bsl" only matches files directly under "src".
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

		// gitignore-style semantics: a separator-less pattern is matched against the
		// file name (last segment) at any depth, so "*abc*" finds nested files too.
		if (normalizedPattern.indexOf('/') < 0)
		{
			String[] pathParts = normalizedPath.split("/");
			String fileName = pathParts.length == 0 ? normalizedPath : pathParts[pathParts.length - 1];
			return matchSegment(fileName, normalizedPattern);
		}

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


	private boolean matchSegment(String segment, String pattern)
	{
		return segment.matches(translateSegmentToRegex(pattern));
	}

	@SuppressWarnings("nls")
	private String translateSegmentToRegex(String pattern)
	{
		StringBuilder regex = new StringBuilder();
		int i = 0;
		while (i < pattern.length())
		{
			char c = pattern.charAt(i);
			switch (c)
			{
				case '*':
					regex.append(".*");
					i++;
					break;
				case '?':
					regex.append(".");
					i++;
					break;
				case '.':
					regex.append("\\.");
					i++;
					break;
				case '[':
				{
					int end = pattern.indexOf(']', i);
					if (end == -1)
					{
						regex.append(Pattern.quote("["));
						i++;
					}
					else
					{
						regex.append(processCharacterClass(pattern.substring(i + 1, end)));
						i = end + 1;
					}
					break;
				}
				case '{':
				{
					int end = findMatchingBrace(pattern, i);
					if (end == -1)
					{
						regex.append(Pattern.quote("{"));
						i++;
					}
					else
					{
						String content = pattern.substring(i + 1, end);
						List<String> alternatives = splitTopLevelCommas(content);
						if (alternatives.size() < 2)
						{
							// No top-level commas -> treat literally, including the braces
							regex.append(Pattern.quote(pattern.substring(i, end + 1)));
						}
						else
						{
							regex.append("(");
							for (int k = 0; k < alternatives.size(); k++)
							{
								if (k > 0)
								{
									regex.append("|");
								}
								regex.append(translateSegmentToRegex(alternatives.get(k)));
							}
							regex.append(")");
						}
						i = end + 1;
					}
					break;
				}
				default:
					regex.append(Pattern.quote(String.valueOf(c)));
					i++;
			}
		}
		return regex.toString();
	}

	private int findMatchingBrace(String s, int start)
	{
		int depth = 0;
		for (int i = start; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '{')
			{
				depth++;
			}
			else if (c == '}')
			{
				depth--;
				if (depth == 0)
				{
					return i;
				}
			}
		}
		return -1;
	}

	private List<String> splitTopLevelCommas(String s)
	{
		List<String> parts = new ArrayList<>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '{' || c == '[')
			{
				depth++;
			}
			else if (c == '}' || c == ']')
			{
				depth--;
			}
			else if (c == ',' && depth == 0)
			{
				parts.add(s.substring(start, i));
				start = i + 1;
			}
		}
		parts.add(s.substring(start));
		return parts;
	}

	@SuppressWarnings("nls")
	private String processCharacterClass(String content)
	{
		if (content.isEmpty())
		{
			return Pattern.quote("[]");
		}

		boolean negate = content.charAt(0) == '!' || content.charAt(0) == '^';
		int start = negate ? 1 : 0;

		StringBuilder regex = new StringBuilder();
		regex.append(negate ? "[^" : "[");

		int i = start;
		while (i < content.length())
		{
			char c = content.charAt(i);
			if (c == '-' && i > start && i + 1 < content.length())
			{
				char prev = content.charAt(i - 1);
				char next = content.charAt(i + 1);
				if (prev < next)
				{
					regex.append("-").append(escapeForCharClass(next));
					i += 2;
					continue;
				}
				regex.append("\\-");
				i++;
				continue;
			}
			regex.append(escapeForCharClass(c));
			i++;
		}

		regex.append("]");
		return regex.toString();
	}

	@SuppressWarnings("nls")
	private String escapeForCharClass(char c)
	{
		switch (c)
		{
			case ']':
			case '\\':
			case '^':
			case '-':
			case '[':
				return "\\" + c;
			default:
				return String.valueOf(c);
		}
	}
}
