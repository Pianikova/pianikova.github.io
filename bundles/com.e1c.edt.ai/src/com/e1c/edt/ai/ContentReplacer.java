package com.e1c.edt.ai;

import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

/**
 * Handles content replacement operations using template-based line ending matching.
 *
 * <p>This class provides methods for replacing text content in strings with the ability
 * to track how many lines were added or removed during the replacement process.
 * It supports both single and multiple occurrences replacement and provides detailed
 * statistics about the operation.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Replace all occurrences of a text pattern using template-based line ending matching</li>
 *   <li>Replace single occurrence with safety checks for multiple matches</li>
 *   <li>Calculate precise line change statistics</li>
 *   <li>Preserve line breaks in unchanged content</li>
 *   <li>Work with CRLF, LF, CR, and mixed line ending formats using templates</li>
 *   <li>Robust parameter validation using Guava Preconditions</li>
 * </ul>
 *
 * @author 1C EDT AI Team
 * @version 3.0
 * @since 1.0
 */
public class ContentReplacer implements IContentReplacer
{
    /**
     * {@inheritDoc}
     *
     * <p>Replaces content in currentContent with newContent based on the replaceAll parameter.
     * This method performs content replacement with detailed statistics tracking and supports
     * both single occurrence and multiple occurrences replacement modes using template-based
     * line ending matching that works with any line endings (CRLF, LF, CR).</p>
     *
     * <p>The method replaces line breaks in originContent with templates for line ending characters,
     * then finds the occurrence in currentContent and performs the replacement without normalization.</p>
     *
     * <p>When replaceAll is true (multiple occurrences mode):</p>
     * <ol>
     *   <li>Converts line breaks in originContent to templates</li>
     *   <li>Finds and replaces all occurrences using template matching</li>
     *   <li>Preserves line breaks in unchanged content</li>
     *   <li>Calculates total lines added/removed across all replacements</li>
     *   <li>Returns updated content with detailed statistics</li>
     * </ol>
     *
     * <p>When replaceAll is false (single occurrence mode):</p>
     * <ol>
     *   <li>Converts line breaks in originContent to templates</li>
     *   <li>Finds the first occurrence using template matching</li>
     *   <li>Verifies no additional occurrences exist (safety check)</li>
     *   <li>Performs the replacement if safe, preserving line breaks</li>
     *   <li>Calculates lines added/removed for the single replacement</li>
     * </ol>
     *
     * <p>Key safety features:</p>
     * <ul>
     *   <li>Null parameter validation with descriptive error messages</li>
     *   <li>Early return for empty origin content</li>
     *   <li>Efficient string building for large content</li>
     *   <li>Accurate line change calculation</li>
     *   <li>Multiple occurrence detection in single replacement mode</li>
     * </ul>
     *
     * <p>Use cases:</p>
     * <ul>
     *   <li>replaceAll=true: Replacing common patterns, variable names, imports across entire file</li>
     *   <li>replaceAll=false: Replacing specific, unique code segments where safety is critical</li>
     * </ul>
     *
     * @param currentContent the original file content to modify
     * @param originContent the text pattern to search for and replace
     * @param newContent the replacement text
     * @param lineDelimiter the line ending format to use for consistent processing
     * @param replaceAll if true, replaces all occurrences; if false, replaces single occurrence
     * @return ReplaceResult containing updated content, line statistics, and multiple occurrences flag
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public ReplaceResult replace(String currentContent, String originContent, String newContent,
        String lineDelimiter, boolean replaceAll)
    {
        // Validate input parameters
        Preconditions.checkNotNull(currentContent, "currentContent cannot be null"); //$NON-NLS-1$
        Preconditions.checkNotNull(originContent, "originContent cannot be null"); //$NON-NLS-1$
        Preconditions.checkNotNull(newContent, "newContent cannot be null"); //$NON-NLS-1$
        Preconditions.checkNotNull(lineDelimiter, "lineDelimiter cannot be null"); //$NON-NLS-1$

        // Early return if nothing to replace - invalid operation but not an error
        if (originContent.isEmpty())
        {
            return new ReplaceResult(currentContent, 0, 0, false, false);
        }

        // Replace line breaks in originContent with templates for line ending characters
        var templatePattern = createTemplatePattern(originContent);

        // Find occurrence in currentContent and perform replacement
        if (replaceAll)
        {
            return performReplaceAllWithTemplate(currentContent, templatePattern, newContent, lineDelimiter);
        }
        else
        {
            return performReplaceOneWithTemplate(currentContent, templatePattern, newContent, lineDelimiter);
        }
    }

    /**
     * Creates a regex pattern that matches the originContent with templates for line endings.
     * Line breaks are replaced with templates that can match any line ending format.
     *
     * @param originContent the content to create a template pattern for
     * @return regex pattern that matches originContent with line ending templates
     */
    private static Pattern createTemplatePattern(String originContent)
    {
        // Build a regex that matches originContent while allowing any line ending at each break.
        var pattern = new StringBuilder();
        var length = originContent.length();
        var index = 0;

        while (index < length)
        {
            var next = index;
            while (next < length)
            {
                var ch = originContent.charAt(next);
                if (ch == '\r' || ch == '\n')
                {
                    break;
                }
                next++;
            }

            if (next > index)
            {
                pattern.append(Pattern.quote(originContent.substring(index, next)));
            }

            if (next >= length)
            {
                break;
            }

            // Consume a line ending sequence and replace it with a template that matches any line ending.
            if (originContent.charAt(next) == '\r' && next + 1 < length
                && originContent.charAt(next + 1) == '\n')
            {
                index = next + 2;
            }
            else
            {
                index = next + 1;
            }

            pattern.append("(?:\\r\\n|\\r|\\n)"); //$NON-NLS-1$
        }

        return Pattern.compile(pattern.toString());
    }

    /**
     * Performs replacement of all occurrences in the content using template-based line ending matching.
     *
     * @param currentContent original current content
     * @param templatePattern regex pattern with line ending templates
     * @param newContent replacement content
     * @param lineDelimiter line delimiter for line counting
     * @return ReplaceResult with updated content and statistics
     */
    private ReplaceResult performReplaceAllWithTemplate(String currentContent, Pattern templatePattern,
        String newContent,
        String lineDelimiter)
    {
        // Find all occurrences using template pattern
        var matcher = templatePattern.matcher(currentContent);
        var count = 0;
        var lastEnd = 0;
        var result = new StringBuilder();
        var matchedContent = ""; // Store the matched content for line counting //$NON-NLS-1$

        while (matcher.find())
        {
            count++;
            matchedContent = matcher.group(); // Store the current match

            // Append content before the current match
            result.append(currentContent, lastEnd, matcher.start());

            // Append the replacement content
            result.append(newContent);

            // Update position
            lastEnd = matcher.end();
        }

        // If no occurrences were found, return original content
        if (count == 0)
        {
            return new ReplaceResult(currentContent, 0, 0, false, false);
        }

        // Append any remaining content after the last replacement
        result.append(currentContent.substring(lastEnd));
        var updatedContent = result.toString();

        // Calculate line change statistics
        var originLines = countLinesWithAnyLineEnding(matchedContent);
        var newLines = countLinesWithAnyLineEnding(newContent);
        var addedLines = newLines * count; // Lines added across all replacements
        var removedLines = originLines * count; // Lines removed across all replacements

        return new ReplaceResult(updatedContent, addedLines, removedLines, true, false);
    }

    /**
     * Performs replacement of a single occurrence in the content with safety checks using template-based line ending matching.
     *
     * @param currentContent original current content
     * @param templatePattern regex pattern with line ending templates
     * @param newContent replacement content
     * @param lineDelimiter line delimiter for line counting
     * @return ReplaceResult with updated content and statistics
     */
    private ReplaceResult performReplaceOneWithTemplate(String currentContent, Pattern templatePattern,
        String newContent,
        String lineDelimiter)
    {
        // Find all occurrences using template pattern
        var matcher = templatePattern.matcher(currentContent);
        var occurrences = new java.util.ArrayList<String>();
        var positions = new java.util.ArrayList<int[]>();

        while (matcher.find())
        {
            occurrences.add(matcher.group());
            positions.add(new int[] { matcher.start(), matcher.end() });
        }

        // Check if any occurrences were found
        if (occurrences.isEmpty())
        {
            // No occurrence found - return original content with failure status
            return new ReplaceResult(currentContent, 0, 0, false, false);
        }

        // Check for multiple occurrences
        var hasMultiple = (occurrences.size() > 1);
        if (hasMultiple)
        {
            // Multiple occurrences found - this is unsafe for single replacement operation
            // Return original content with failure and multiple occurrences flag
            return new ReplaceResult(currentContent, 0, 0, false, true);
        }

        // Get the single occurrence position
        var position = positions.get(0);
        var start = position[0];
        var end = position[1];
        var matchedContent = occurrences.get(0);

        // Build the updated content by:
        // 1. Taking content before the match
        // 2. Adding the replacement content
        // 3. Adding content after the match
        var updatedContent =
            currentContent.substring(0, start) + newContent + currentContent.substring(end);

        // Calculate line change statistics for single replacement
        var originLines = countLinesWithAnyLineEnding(matchedContent);
        var newLines = countLinesWithAnyLineEnding(newContent);
        var addedLines = newLines; // For single replacement, this is just new content lines
        var removedLines = originLines; // For single replacement, this is just original content lines

        return new ReplaceResult(updatedContent, addedLines, removedLines, true, false);
    }

    /**
     * Counts lines in content using regular expressions that work with any line endings.
     *
     * <p>This method accurately counts the number of lines in a text string regardless of
     * line ending format (CRLF, LF, or CR). It handles various edge cases including:</p>
     * <ul>
     *   <li>Empty content (returns 0)</li>
     *   <li>Single-line content without delimiters (returns 1)</li>
     *   <li>Content ending with or without line delimiter</li>
     *   <li>Multiple consecutive delimiters</li>
     *   <li>Mixed line ending styles in the same content</li>
     * </ul>
     *
     * <p>The counting algorithm uses regex to find all line ending patterns:
     * \r\n (Windows), \r (old Mac), and \n (Unix) and counts lines appropriately,
     * including the final line that may not end with a delimiter.</p>
     *
     * @param content text to analyze (must not be null)
     * @return number of lines in the content (0 for empty content)
     * @throws NullPointerException if content is null
     */
    public static int countLinesWithAnyLineEnding(String content)
    {
        // Validate input parameter
        Preconditions.checkNotNull(content, "content cannot be null"); //$NON-NLS-1$

        // Empty content has no lines
        if (content.isEmpty())
        {
            return 0;
        }

        // Use regex to find all line ending patterns: \r\n, \r, or \n
        // The pattern \r\n|\r|\n matches any line ending, but we need to handle \r\n first
        // to avoid double-counting when \r\n is split into \r and \n
        var lineEndingPattern = Pattern.compile("\r\n|\r|\n"); //$NON-NLS-1$
        var matcher = lineEndingPattern.matcher(content);

        // Count line endings
        var lineEndingCount = 0;
        while (matcher.find())
        {
            lineEndingCount++;
        }

        // Calculate lines: if content ends with a line ending, line count = line endings count
        // If content doesn't end with a line ending, line count = line endings count + 1
        var lastChar = content.charAt(content.length() - 1);
        if (lastChar == '\r' || lastChar == '\n')
        {
            return lineEndingCount;
        }
        else
        {
            return lineEndingCount + 1;
        }
    }
}
