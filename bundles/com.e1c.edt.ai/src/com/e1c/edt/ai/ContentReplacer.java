package com.e1c.edt.ai;

import java.io.PrintStream;
import java.lang.Math;

import com.google.common.base.Preconditions;

public class ContentReplacer implements IContentReplacer
{
    private static final String NORMALIZED_LINE_DELIMITER = "\n"; //$NON-NLS-1$
    private static final String BOM = "\uFEFF"; // Byte Order Mark (UTF-8) //$NON-NLS-1$
    private static final PrintStream OUT = System.out;

	@Override
	public ReplaceResult replace(String currentContent, String originContent, String newContent,
		String lineDelimiter, boolean replaceAll)
	{
		// Validate input parameters
		Preconditions.checkNotNull(currentContent);
		Preconditions.checkNotNull(originContent);
		Preconditions.checkNotNull(newContent);
		Preconditions.checkNotNull(lineDelimiter);

		// Determine line delimiter from currentContent, fallback to provided argument
		String detectedLineDelimiter = detectLineDelimiter(currentContent);
		if (detectedLineDelimiter == null)
		{
			detectedLineDelimiter = lineDelimiter;
		}

		// Check if currentContent has BOM and remember it
		boolean currentHasBOM = currentContent.startsWith(BOM);

		// Remove BOM from all content for searching/comparison (ignore BOM)
		String searchCurrentContent = stripBOM(currentContent);
		String searchOriginContent = stripBOM(originContent);
		String searchNewContent = stripBOM(newContent);

		// Normalize all content to use \n as line delimiter
		String normalizedCurrentContent = normalizeLineDelimiters(searchCurrentContent, detectedLineDelimiter);
		String normalizedOriginContent = normalizeLineDelimiters(searchOriginContent, detectedLineDelimiter);
		String normalizedNewContent = normalizeLineDelimiters(searchNewContent, detectedLineDelimiter);

		// Check if originContent exists in currentContent (BOM ignored)
		if (!normalizedCurrentContent.contains(normalizedOriginContent))
		{
			// Log with invisible characters visible (BOM stripped)
			OUT.println("ContentReplacer: Origin content not found in current content (BOM ignored)");
			OUT.println("Normalized current content: " + makeInvisibleVisible(normalizedCurrentContent));
			OUT.println("Normalized origin content: " + makeInvisibleVisible(normalizedOriginContent));
			OUT.println("Normalized new content: " + makeInvisibleVisible(normalizedNewContent));
			OUT.println("Detected line delimiter: " + makeInvisibleVisible(detectedLineDelimiter));
			OUT.println("Current has BOM: " + currentHasBOM);

            // No occurrence found - return failure result
			return new ReplaceResult(currentContent, 0, 0, false);
		}

		// Count occurrences
		int occurrenceCount = countOccurrences(normalizedCurrentContent, normalizedOriginContent);
		boolean multipleOccurrences = occurrenceCount > 1;

		// Count lines in origin and new content, ignoring common context
		int removedLines = countLinesIgnoringContext(normalizedOriginContent, normalizedNewContent,
			NORMALIZED_LINE_DELIMITER, true);
		int addedLines = countLinesIgnoringContext(normalizedNewContent, normalizedOriginContent,
			NORMALIZED_LINE_DELIMITER, false);

		// Perform replacement on normalized content
		String normalizedUpdatedContent;
		if (replaceAll)
		{
			normalizedUpdatedContent = normalizedCurrentContent.replace(normalizedOriginContent,
				normalizedNewContent);
			// Adjust line counts for multiple occurrences
			removedLines = removedLines * occurrenceCount;
			addedLines = addedLines * occurrenceCount;
		}
		else
		{
			// Replace only first occurrence
			normalizedUpdatedContent = normalizedCurrentContent.replaceFirst(
				java.util.regex.Pattern.quote(normalizedOriginContent),
				java.util.regex.Matcher.quoteReplacement(normalizedNewContent));
		}

		// Convert back to original line delimiter
		String updatedContent = denormalizeLineDelimiters(normalizedUpdatedContent, detectedLineDelimiter);

		// Restore BOM if it was present in current content
		updatedContent = restoreBOM(updatedContent, currentHasBOM);

		return new ReplaceResult(updatedContent, addedLines, removedLines, true, multipleOccurrences);
	}

	/**
	 * Makes invisible characters in string visible for debugging
	 *
	 * @param content the content to process
	 * @return string with invisible characters replaced with visible representations
	 */
	private String makeInvisibleVisible(String content)
	{
		if (content == null)
		{
			return "null";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < content.length(); i++)
		{
			char c = content.charAt(i);
			switch (c)
			{
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case ' ':
					sb.append("␣");
					break;
				default:
					if (c < 32 || (c >= 127 && c < 160))
					{
						// Other control characters
						sb.append(String.format("\\u%04x", (int)c));
					}
					else
					{
						sb.append(c);
					}
					break;
			}
		}
		return sb.toString();
	}

	/**
	 * Removes BOM (Byte Order Mark) from the beginning of content if present
	 *
	 * @param content the content to process
	 * @return content without BOM at the beginning
	 */
	private String stripBOM(String content)
	{
		if (content == null || content.isEmpty())
		{
			return content;
		}
		if (content.startsWith(BOM))
		{
			return content.substring(BOM.length());
		}
		return content;
	}

	/**
	 * Adds BOM to the beginning of content if it was originally present
	 *
	 * @param content the content to process
	 * @param hadBOM whether the original content had a BOM
	 * @return content with BOM at the beginning if hadBOM is true
	 */
	private String restoreBOM(String content, boolean hadBOM)
	{
		if (hadBOM && (content == null || !content.startsWith(BOM)))
		{
			return BOM + content;
		}
		return content;
	}

	/**
	 * Detects the line delimiter used in content
	 *
	 * @param content the content to detect line delimiter from
	 * @return the detected line delimiter (\r\n, \r, or \n), or null if not determinable
	 */
	private String detectLineDelimiter(String content)
	{
		if (content.isEmpty())
		{
			return null;
		}

		int crCount = 0;
		int lfCount = 0;
		int crlfCount = 0;

		for (int i = 0; i < content.length(); i++)
		{
			char c = content.charAt(i);
			if (c == '\r')
			{
				if (i + 1 < content.length() && content.charAt(i + 1) == '\n')
				{
					crlfCount++;
					i++; // Skip the next character (\n)
				}
				else
				{
					crCount++;
				}
			}
			else if (c == '\n')
			{
				lfCount++;
			}
		}

		// Prioritize CRLF over CR/LF individually
		if (crlfCount > 0)
		{
			return "\r\n";
		}
		if (crCount > 0)
		{
			return "\r";
		}
		if (lfCount > 0)
		{
			return "\n";
		}

		// No line delimiters found
		return null;
	}

	/**
	 * Normalizes line delimiters in content to \n
	 *
	 * @param content the content to normalize
	 * @param lineDelimiter the current line delimiter
	 * @return normalized content with \n line delimiters
	 */
	private String normalizeLineDelimiters(String content, String lineDelimiter)
	{
		if (content.isEmpty() || lineDelimiter.equals(NORMALIZED_LINE_DELIMITER))
		{
			return content;
		}
		return content.replace(lineDelimiter, NORMALIZED_LINE_DELIMITER);
	}

	/**
	 * Denormalizes line delimiters from \n back to the original line delimiter
	 *
	 * @param content the content with \n line delimiters
	 * @param lineDelimiter the target line delimiter
	 * @return content with original line delimiters
	 */
	private String denormalizeLineDelimiters(String content, String lineDelimiter)
	{
		if (content.isEmpty() || lineDelimiter.equals(NORMALIZED_LINE_DELIMITER))
		{
			return content;
		}
		return content.replace(NORMALIZED_LINE_DELIMITER, lineDelimiter);
	}

	/**
	 * Counts the number of occurrences of a substring in a string
	 *
	 * @param str the string to search in
	 * @param sub the substring to search for
	 * @return the number of occurrences
	 */
	private int countOccurrences(String str, String sub)
	{
		if (str.isEmpty() || sub.isEmpty())
		{
			return 0;
		}

		int count = 0;
		int idx = 0;

		while ((idx = str.indexOf(sub, idx)) != -1)
		{
			count++;
			idx += sub.length();
		}

		return count;
	}

	/**
	 * Counts the number of lines in a string based on the line delimiter
	 *
	 * @param content the content to count lines in
	 * @param lineDelimiter the line delimiter
	 * @return the number of lines
	 */
	private int countLines(String content, String lineDelimiter)
	{
		if (content.isEmpty())
		{
			return 0;
		}

		int count = 0;
		int idx = 0;

		while ((idx = content.indexOf(lineDelimiter, idx)) != -1)
		{
			count++;
			idx += lineDelimiter.length();
		}

		// If content contains at least one line delimiter, count lines properly
		if (count > 0)
		{
			// If content ends with line delimiter, return the count
			// Otherwise, add one for the last line
			return content.endsWith(lineDelimiter) ? count : count + 1;
		}

		// No line delimiters found - count as 1 line for any non-empty content
		return 1;
	}

	/**
	 * Counts the number of lines in content, ignoring common prefix and suffix with other content
	 *
	 * @param content the content to count lines in
	 * @param otherContent the other content to compare against
	 * @param lineDelimiter the line delimiter
	 * @param isRemoved true if this is removed content, false if added
	 * @return the number of lines
	 */
	private int countLinesIgnoringContext(String content, String otherContent, String lineDelimiter, boolean isRemoved)
	{
		if (content.isEmpty())
		{
			return 0;
		}

		// Split content into lines
		String[] contentLines = content.split(java.util.regex.Pattern.quote(lineDelimiter), -1);
		String[] otherLines = otherContent.split(java.util.regex.Pattern.quote(lineDelimiter), -1);

		// Find common prefix
		int prefixLength = 0;
		int minPrefixLength = Math.min(contentLines.length, otherLines.length);
		while (prefixLength < minPrefixLength && contentLines[prefixLength].equals(otherLines[prefixLength]))
		{
			prefixLength++;
		}

		// Find common suffix
		int suffixLength = 0;
		int minSuffixLength = Math.min(contentLines.length - prefixLength, otherLines.length - prefixLength);
		while (suffixLength < minSuffixLength
			&& contentLines[contentLines.length - 1 - suffixLength].equals(otherLines[otherLines.length - 1 - suffixLength]))
		{
			suffixLength++;
		}

		// Count lines excluding common prefix and suffix
		int countedLines = contentLines.length - prefixLength - suffixLength;

		// Special case: if removing, and there's only one line being replaced (middle line)
		// We should count it as 1 removed line
		if (isRemoved && countedLines == 0 && contentLines.length > 0 && otherLines.length > 0)
		{
			// Check if this is a case where a single line is being replaced within context
			// e.g., "Abc\nLine2\nXyz\n" -> "Abc\nNewLine\nXyz\n"
			// Here, Line2 is replaced by NewLine, but Abc and Xyz are context
			if (contentLines.length == otherLines.length && prefixLength + suffixLength == contentLines.length - 1)
			{
				return 1;
			}
		}

		return Math.max(0, countedLines);
	}
}
