package com.e1c.edt.ai.tools;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContentReplacer implements IContentReplacer
{
    private static final String NORMALIZED_LINE_DELIMITER = "\n"; //$NON-NLS-1$
    private static final String BOM = "\uFEFF"; // Byte Order Mark (UTF-8) //$NON-NLS-1$

    /**
     * A fuzzy (non-literal) candidate is rejected when it is more than this many times longer than the
     * requested old content. Guards against an anchor strategy matching first+last line and swallowing
     * the whole span in between, which would delete a large, unintended block.
     */
    private static final int MAX_FUZZY_MATCH_GROWTH_FACTOR = 3;

    /**
     * Minimum token-overlap (Jaccard) score for a line in the file to be offered as a "closest match"
     * hint on a not-found failure. Below this, an unrelated line would mislead more than help.
     */
    private static final double MIN_HINT_SIMILARITY = 0.4;

    /** Lines of context shown before/after the best-matching line in a not-found hint. */
    private static final int HINT_CONTEXT_LINES = 2;

    /** Only the first few lines of a multi-line `old_content` are considered as the anchor to search for. */
    private static final int MAX_ANCHOR_CANDIDATE_LINES = 10;

    /** An anchor line shorter than this is too generic (e.g. a lone brace) to search for reliably. */
    private static final int MIN_ANCHOR_LENGTH = 8;

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
            String hint = findNearestMatchHint(normalizedCurrentContent, normalizedOriginContent);
            return new ReplaceResult(currentContent, 0, 0, false, hint);
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

        // For a single contiguous match we can apply a minimal-region replace on the document instead of
        // resetting its whole content — this keeps Xtext's re-lexing bounded to the changed span.
        boolean regionReplaceable = !replaceAll;
        int bomShift = currentHasBOM ? BOM.length() : 0;
        int replaceOffset = startOffset + bomShift;
        int replaceLength = endOffset - startOffset;
        String replacementText = denormalizeLineDelimiters(normalizedNewContent, detectedLineDelimiter);

        return buildResult(normalizedUpdatedContent, detectedLineDelimiter, currentHasBOM, addedLines, removedLines,
            searchResult.occurrenceCount > 1, start[0], start[1], end[0], end[1], regionReplaceable, replaceOffset,
            replaceLength, replacementText);
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
        // Keep the whole-document path here (regionReplaceable = false) — handling the multi-insert
        // semantics of an empty origin as a minimal region replace is out of scope.
        return buildResult(normalizedUpdatedContent, detectedLineDelimiter, currentHasBOM, addedLines, removedLines, false,
            1, 1, 1, 1, false, 0, 0, null);
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
        int matchEndLine, int matchEndColumn, boolean regionReplaceable, int replaceOffset, int replaceLength,
        String replacementText)
    {
        String updatedContent = denormalizeLineDelimiters(normalizedUpdatedContent, detectedLineDelimiter);
        updatedContent = restoreBOM(updatedContent, hadBOM);
        return new ReplaceResult(updatedContent, addedLines, removedLines, true, multipleOccurrences, matchStartLine,
            matchStartColumn, matchEndLine, matchEndColumn, regionReplaceable, replaceOffset, replaceLength,
            replacementText);
    }

    private ReplacementSearchResult findReplacement(String content, String find, boolean replaceAll)
    {
        for (IReplacementStrategy strategy : replacementStrategies)
        {
            // Distinct match regions this strategy resolves, keyed by offset+length so that two
            // candidates covering the same span count once, while two candidates sharing a start
            // offset but differing in length count as an ambiguity. Value is the candidate substring.
            Map<Long, String> regions = new LinkedHashMap<>();

            for (String candidate : strategy.findCandidates(content, find))
            {
                // An empty candidate (e.g. a whitespace-only `old_content` trimmed away by a fuzzy
                // strategy) is never a meaningful match here - `find` is guaranteed non-empty at this
                // point (the empty-origin case is handled earlier by replaceWithEmptyOrigin). Beyond
                // being meaningless, String.indexOf("", n) never returns -1, so scanning occurrences
                // of an empty candidate below would loop forever.
                if (candidate.isEmpty())
                {
                    continue;
                }

                int firstIndex = content.indexOf(candidate);
                if (firstIndex == -1)
                {
                    continue;
                }

                // "Replace all" is only safe at the strictest, literal level: the candidate must be
                // exactly the requested old content. On fuzzy levels the candidate is a normalized /
                // trimmed / re-anchored variant, so replacing every occurrence of it is unsafe -> a
                // single unique match is required instead (regardless of the replaceAll flag).
                if (replaceAll && candidate.equals(find))
                {
                    return ReplacementSearchResult.found(candidate, firstIndex, countOccurrences(content, candidate));
                }

                // Reject a fuzzy candidate that ballooned far beyond the requested old content (e.g. an
                // anchor strategy matching first+last line and swallowing the whole span in between):
                // replacing it would delete a large, unintended block. The literal candidate
                // (candidate.equals(find)) is never subject to this guard.
                if (!candidate.equals(find) && !find.isEmpty()
                    && candidate.length() > (long)find.length() * MAX_FUZZY_MATCH_GROWTH_FACTOR)
                {
                    continue;
                }

                // Record every occurrence of this candidate as a distinct region.
                int step = Math.max(1, candidate.length());
                for (int index = firstIndex; index != -1; index = content.indexOf(candidate, index + step))
                {
                    long key = ((long)index << 32) | (candidate.length() & 0xffffffffL);
                    regions.putIfAbsent(key, candidate);
                }
            }

            if (regions.size() == 1)
            {
                Map.Entry<Long, String> only = regions.entrySet().iterator().next();
                int offset = (int)(only.getKey() >> 32);
                return ReplacementSearchResult.found(only.getValue(), offset, 1);
            }
            if (regions.size() > 1)
            {
                // This precision level found more than one place the old content could go. Do NOT fall
                // through to a looser strategy (which might uniquely match a different, wrong span);
                // report ambiguity so the caller can refine old_content.
                return ReplacementSearchResult.multipleMatches();
            }
            // regions empty -> nothing at this level, try the next (looser) strategy.
        }

        return ReplacementSearchResult.notFound();
    }

    /**
     * When {@code old_content} matches nothing in the file, finds the line most similar to it and
     * returns a short excerpt around that line — so the caller can correct `old_content` in one
     * follow-up call instead of blindly re-guessing or re-reading the whole file.
     *
     * @return the excerpt (1-based line numbers), or {@code null} when no line is a confident enough
     *         match to be worth surfacing.
     */
    private String findNearestMatchHint(String normalizedCurrentContent, String normalizedOriginContent)
    {
        String anchor = pickAnchorLine(normalizedOriginContent);
        if (anchor == null)
        {
            return null;
        }

        Set<String> anchorTokens = tokenize(anchor);
        if (anchorTokens.isEmpty())
        {
            return null;
        }

        String[] lines = normalizedCurrentContent.split(NORMALIZED_LINE_DELIMITER, -1);
        int bestIndex = -1;
        double bestScore = 0;
        for (int i = 0; i < lines.length; i++)
        {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty())
            {
                continue;
            }

            double score = jaccard(anchorTokens, tokenize(trimmed));
            if (score > bestScore)
            {
                bestScore = score;
                bestIndex = i;
            }
        }

        if (bestIndex < 0 || bestScore < MIN_HINT_SIMILARITY)
        {
            return null;
        }

        int from = Math.max(0, bestIndex - HINT_CONTEXT_LINES);
        int to = Math.min(lines.length - 1, bestIndex + HINT_CONTEXT_LINES);
        StringBuilder hint = new StringBuilder();
        for (int i = from; i <= to; i++)
        {
            hint.append(i + 1).append(": ").append(lines[i]);
            if (i < to)
            {
                hint.append('\n');
            }
        }
        return hint.toString();
    }

    /**
     * Picks the most distinctive line among the first few lines of `old_content` to search for: the
     * longest one, since longer lines carry more identifying tokens than e.g. a lone brace.
     */
    private String pickAnchorLine(String normalizedOriginContent)
    {
        String[] originLines = normalizedOriginContent.split(NORMALIZED_LINE_DELIMITER, -1);
        String bestLine = null;
        for (int i = 0; i < Math.min(originLines.length, MAX_ANCHOR_CANDIDATE_LINES); i++)
        {
            String trimmed = originLines[i].trim();
            if (trimmed.length() >= MIN_ANCHOR_LENGTH && (bestLine == null || trimmed.length() > bestLine.length()))
            {
                bestLine = trimmed;
            }
        }
        return bestLine;
    }

    @SuppressWarnings("nls")
    private Set<String> tokenize(String line)
    {
        Set<String> tokens = new HashSet<>();
        for (String token : line.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}_]+"))
        {
            if (token.length() >= 2)
            {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private double jaccard(Set<String> a, Set<String> b)
    {
        if (a.isEmpty() || b.isEmpty())
        {
            return 0;
        }

        int intersection = 0;
        for (String token : a)
        {
            if (b.contains(token))
            {
                intersection++;
            }
        }

        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0 : (double)intersection / union;
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
