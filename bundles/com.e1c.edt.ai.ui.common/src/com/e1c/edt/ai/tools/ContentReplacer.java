package com.e1c.edt.ai.tools;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

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
        Preconditions.checkNotNull(currentContent);
        Preconditions.checkNotNull(originContent);
        Preconditions.checkNotNull(newContent);
        Preconditions.checkNotNull(lineDelimiter);

        String detectedLineDelimiter = detectLineDelimiter(currentContent);
        if (detectedLineDelimiter == null)
        {
            detectedLineDelimiter = lineDelimiter;
        }

        boolean currentHasBOM = currentContent.startsWith(BOM);

        // Original content with original line delimiters but BOM stripped — used for line/column mapping.
        String strippedOriginal = stripBOM(currentContent);

        String normalizedCurrentContent = prepare(currentContent);
        String normalizedOriginContent = prepare(originContent);
        String normalizedNewContent = prepare(newContent);

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

        // Compute match position on the original (BOM-stripped, non-normalized) content,
        // so line/column reflect what the user sees in the editor.
        int startOffset = normalizedOffsetToStrippedOffset(strippedOriginal, searchResult.firstIndex);
        int endOffset = normalizedOffsetToStrippedOffset(strippedOriginal,
            searchResult.firstIndex + searchResult.searchCandidate.length());
        int[] start = offsetToLineColumn(strippedOriginal, startOffset);
        int[] end = offsetToLineColumn(strippedOriginal, endOffset);

        return buildResult(normalizedUpdatedContent, detectedLineDelimiter, currentHasBOM, addedLines, removedLines,
            searchResult.occurrenceCount > 1, start[0], start[1], end[0], end[1]);
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

        // Empty-origin insertion happens at the very start of the content.
        return buildResult(normalizedUpdatedContent, detectedLineDelimiter, currentHasBOM, addedLines, removedLines, false,
            1, 1, 1, 1);
    }

    /**
     * Strips BOM and normalizes line delimiters to "\n" — the canonical form used for searching.
     */
    private String prepare(String content)
    {
        return normalizeLineDelimiters(stripBOM(content));
    }

    /**
     * Common tail: denormalize line delimiters, restore BOM, package the result.
     */
    private ReplaceResult buildResult(String normalizedUpdatedContent, String detectedLineDelimiter, boolean hadBOM,
        int addedLines, int removedLines, boolean multipleOccurrences, int matchStartLine, int matchStartColumn,
        int matchEndLine, int matchEndColumn)
    {
        String updatedContent = denormalizeLineDelimiters(normalizedUpdatedContent, detectedLineDelimiter);
        updatedContent = restoreBOM(updatedContent, hadBOM);
        return new ReplaceResult(updatedContent, addedLines, removedLines, true, multipleOccurrences, matchStartLine,
            matchStartColumn, matchEndLine, matchEndColumn);
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

        return null;
    }

    private String normalizeLineDelimiters(String content)
    {
        if (content.isEmpty())
        {
            return content;
        }
        return content.replace("\r\n", "\n").replace('\r', '\n'); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String denormalizeLineDelimiters(String content, String lineDelimiter)
    {
        if (content.isEmpty() || lineDelimiter.equals(NORMALIZED_LINE_DELIMITER))
        {
            return content;
        }
        return content.replace(NORMALIZED_LINE_DELIMITER, lineDelimiter);
    }

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
     */
    private int countLinesIgnoringContext(String content, String otherContent, String lineDelimiter, boolean isRemoved)
    {
        if (content.isEmpty())
        {
            return 0;
        }

        String[] contentLines = content.split(java.util.regex.Pattern.quote(lineDelimiter), -1);
        String[] otherLines = otherContent.split(java.util.regex.Pattern.quote(lineDelimiter), -1);

        int prefixLength = 0;
        int minPrefixLength = Math.min(contentLines.length, otherLines.length);
        while (prefixLength < minPrefixLength && contentLines[prefixLength].equals(otherLines[prefixLength]))
        {
            prefixLength++;
        }

        int suffixLength = 0;
        int minSuffixLength = Math.min(contentLines.length - prefixLength, otherLines.length - prefixLength);
        while (suffixLength < minSuffixLength && contentLines[contentLines.length - 1 - suffixLength]
            .equals(otherLines[otherLines.length - 1 - suffixLength]))
        {
            suffixLength++;
        }

        int countedLines = contentLines.length - prefixLength - suffixLength;

        if (isRemoved && countedLines == 0 && contentLines.length > 0 && otherLines.length > 0)
        {
            if (contentLines.length == otherLines.length && prefixLength + suffixLength == contentLines.length - 1)
            {
                return 1;
            }
        }

        return Math.max(0, countedLines);
    }

    /**
     * Maps an offset in the normalized (LF-only, BOM-stripped) content back to an offset in the
     * BOM-stripped original content (which still uses the source line delimiter).
     */
    private int normalizedOffsetToStrippedOffset(String strippedOriginal, int normalizedOffset)
    {
        int strippedIdx = 0;
        int normIdx = 0;
        while (normIdx < normalizedOffset && strippedIdx < strippedOriginal.length())
        {
            char c = strippedOriginal.charAt(strippedIdx);
            if (c == '\r')
            {
                if (strippedIdx + 1 < strippedOriginal.length() && strippedOriginal.charAt(strippedIdx + 1) == '\n')
                {
                    strippedIdx += 2;
                }
                else
                {
                    strippedIdx += 1;
                }
                normIdx += 1;
            }
            else
            {
                strippedIdx += 1;
                normIdx += 1;
            }
        }
        return strippedIdx;
    }

    /**
     * Converts a character offset in {@code content} to a 1-based (line, column) pair.
     * Recognizes \r\n, \r and \n as line breaks.
     *
     * @return a two-element array {line, column}
     */
    private int[] offsetToLineColumn(String content, int offset)
    {
        int line = 1;
        int column = 1;
        int limit = Math.min(offset, content.length());
        for (int i = 0; i < limit; i++)
        {
            char c = content.charAt(i);
            if (c == '\r')
            {
                line++;
                column = 1;
                if (i + 1 < limit && content.charAt(i + 1) == '\n')
                {
                    i++;
                }
            }
            else if (c == '\n')
            {
                line++;
                column = 1;
            }
            else
            {
                column++;
            }
        }
        return new int[] { line, column };
    }

    private static class ReplacementSearchResult
    {
        private final boolean notFound;
        private final boolean multipleMatches;
        private final String searchCandidate;
        private final int firstIndex;
        private final int occurrenceCount;

        private ReplacementSearchResult(boolean notFound, boolean multipleMatches, String searchCandidate,
            int firstIndex, int occurrenceCount)
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
