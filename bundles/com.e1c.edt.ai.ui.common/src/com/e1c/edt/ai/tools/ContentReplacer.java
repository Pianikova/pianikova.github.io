package com.e1c.edt.ai.tools;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Preconditions;

public class ContentReplacer implements IContentReplacer
{
    private static final String NORMALIZED_LINE_DELIMITER = "\n"; //$NON-NLS-1$
    private static final String BOM = "\uFEFF"; // Byte Order Mark (UTF-8) //$NON-NLS-1$
    private final List<IReplacementStrategy> replacementStrategies;

    @Inject
    public ContentReplacer(Set<IReplacementStrategy> strategies)
    {
        this.replacementStrategies = strategies.stream()
            .sorted(Comparator.comparingInt(IReplacementStrategy::getOrdinal))
            .collect(Collectors.toList());
    }

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
		String normalizedCurrentContent = normalizeLineDelimiters(searchCurrentContent);
		String normalizedOriginContent = normalizeLineDelimiters(searchOriginContent);
		String normalizedNewContent = normalizeLineDelimiters(searchNewContent);

        if (normalizedOriginContent.isEmpty())
        {
            return replaceWithEmptyOrigin(normalizedCurrentContent, normalizedNewContent, detectedLineDelimiter,
                currentHasBOM, replaceAll);
        }

        ReplacementSearchResult searchResult =
            findReplacement(normalizedCurrentContent, normalizedOriginContent, replaceAll);
        if (searchResult.notFound)
        {
            return new ReplaceResult(currentContent, 0, 0, false);
        }
        if (searchResult.multipleMatches)
        {
            return new ReplaceResult(currentContent, 0, 0, false, true);
        }

        int removedLines = countLinesIgnoringContext(searchResult.searchCandidate, normalizedNewContent,
            NORMALIZED_LINE_DELIMITER, true);
        int addedLines = countLinesIgnoringContext(normalizedNewContent, searchResult.searchCandidate,
            NORMALIZED_LINE_DELIMITER, false);
        if (replaceAll)
        {
            removedLines = removedLines * searchResult.occurrenceCount;
            addedLines = addedLines * searchResult.occurrenceCount;
        }

        String normalizedUpdatedContent;
        if (replaceAll)
        {
            normalizedUpdatedContent =
                normalizedCurrentContent.replace(searchResult.searchCandidate, normalizedNewContent);
        }
        else
        {
            normalizedUpdatedContent = normalizedCurrentContent.substring(0, searchResult.firstIndex)
                + normalizedNewContent
                + normalizedCurrentContent.substring(searchResult.firstIndex + searchResult.searchCandidate.length());
        }

		// Convert back to original line delimiter
		String updatedContent = denormalizeLineDelimiters(normalizedUpdatedContent, detectedLineDelimiter);

		// Restore BOM if it was present in current content
		updatedContent = restoreBOM(updatedContent, currentHasBOM);

		return new ReplaceResult(updatedContent, addedLines, removedLines, true, searchResult.occurrenceCount > 1);
	}

    private ReplacementSearchResult findReplacement(String content, String find, boolean replaceAll)
    {
        boolean foundAny = false;

        for (IReplacementStrategy strategy : replacementStrategies)
        {
            for (String candidate : strategy.findCandidates(content, find))
            {
                int firstIndex = content.indexOf(candidate);
                if (firstIndex == -1)
                {
                    continue;
                }

                foundAny = true;
                int occurrenceCount = countOccurrences(content, candidate);
                if (replaceAll)
                {
                    return ReplacementSearchResult.found(candidate, firstIndex, occurrenceCount);
                }

                int lastIndex = content.lastIndexOf(candidate);
                if (firstIndex != lastIndex)
                {
                    continue;
                }

                return ReplacementSearchResult.found(candidate, firstIndex, occurrenceCount);
            }
        }

        if (!foundAny)
        {
            return ReplacementSearchResult.notFound();
        }

        return ReplacementSearchResult.multipleMatches();
    }

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
	@SuppressWarnings("nls")
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
	private String normalizeLineDelimiters(String content)
	{
		if (content.isEmpty())
		{
			return content;
		}
		return content.replace("\r\n", "\n").replace('\r', '\n'); //$NON-NLS-1$ //$NON-NLS-2$
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

    private ReplaceResult replaceWithEmptyOrigin(String normalizedCurrentContent, String normalizedNewContent,
        String detectedLineDelimiter, boolean currentHasBOM, boolean replaceAll)
    {
        int removedLines = countLinesIgnoringContext("", normalizedNewContent, NORMALIZED_LINE_DELIMITER, true); //$NON-NLS-1$
        int addedLines = countLinesIgnoringContext(normalizedNewContent, "", NORMALIZED_LINE_DELIMITER, false); //$NON-NLS-1$

        String normalizedUpdatedContent;
        if (replaceAll)
        {
            normalizedUpdatedContent = normalizedCurrentContent.replace("", normalizedNewContent); //$NON-NLS-1$
            removedLines = 0;
            addedLines = 0;
        }
        else
        {
            normalizedUpdatedContent = normalizedCurrentContent.replaceFirst(java.util.regex.Pattern.quote(""), //$NON-NLS-1$
                java.util.regex.Matcher.quoteReplacement(normalizedNewContent));
        }

        String updatedContent = denormalizeLineDelimiters(normalizedUpdatedContent, detectedLineDelimiter);
        updatedContent = restoreBOM(updatedContent, currentHasBOM);
        return new ReplaceResult(updatedContent, addedLines, removedLines, true, false);
    }

    private static class ReplacementSearchResult
    {
        private final boolean notFound;
        private final boolean multipleMatches;
        private final String searchCandidate;
        private final int firstIndex;
        private final int occurrenceCount;

        private ReplacementSearchResult(boolean notFound, boolean multipleMatches, String searchCandidate, int firstIndex,
            int occurrenceCount)
        {
            this.notFound = notFound;
            this.multipleMatches = multipleMatches;
            this.searchCandidate = searchCandidate;
            this.firstIndex = firstIndex;
            this.occurrenceCount = occurrenceCount;
        }

        private static ReplacementSearchResult found(String searchCandidate, int firstIndex, int occurrenceCount)
        {
            return new ReplacementSearchResult(false, false, searchCandidate, firstIndex, occurrenceCount);
        }

        private static ReplacementSearchResult notFound()
        {
            return new ReplacementSearchResult(true, false, null, -1, 0);
        }

        private static ReplacementSearchResult multipleMatches()
        {
            return new ReplacementSearchResult(false, true, null, -1, 0);
        }
    }
}
