/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Utility class for markdown formatting operations
 */
@Singleton
public class MarkdownUtils implements IMarkdownUtils
{
    private final ILinkProvider linkProvider;
    private final IFiles files;

    @Inject
    public MarkdownUtils(ILinkProvider linkProvider, IFiles files)
    {
        this.linkProvider = linkProvider;
        this.files = files;
    }
	/**
	 * Escapes content for markdown display by replacing backticks
	 * @param content The content to escape
	 * @return Escaped content safe for markdown display
	 */
	@Override
	@SuppressWarnings("nls")
	public String escapeForMarkdown(String content)
	{
		if (content == null)
		{
			return "";
		}
		// Replace backticks with escaped backticks to prevent markdown formatting issues
		return content.replace("`", "\\`");
	}

    @Override
    public String decodeUrl(String content)
    {
        if (content == null || content.isEmpty())
        {
            return content;
        }

        try
        {
            return java.net.URLDecoder.decode(content, java.nio.charset.StandardCharsets.UTF_8.name());
        }
        catch (Exception e)
        {
            // If decoding fails, return the original content
            return content;
        }
    }

    @Override
    public String createStyledText(String content, TextColor color, FontWeight weight)
    {
        return createStyledText(content, color, weight, null);
    }

    @Override
    @SuppressWarnings("nls")
    public String createStyledText(String content, TextColor color, FontWeight weight, Double opacity)
    {
        if (content == null)
        {
            return "";
        }

        var style = new StringBuilder();
        if (color != null)
        {
            style.append("color: ").append(color.getValue());
        }

        if (weight != null)
        {
            if (style.length() > 0)
            {
                style.append("; ");
            }
            style.append("font-weight: ").append(weight.getValue());
        }

        if (opacity != null)
        {
            if (style.length() > 0)
            {
                style.append("; ");
            }
            style.append("opacity: ").append(opacity);
        }

        if (style.length() == 0)
        {
            return content;
        }

        return String.format("<span style=\"%s\">%s</span>", style.toString(), escapeForMarkdown(content));
    }

    @Override
    @SuppressWarnings("nls")
    public String buildGitDiff(String filePath, String originContent, String newContent)
    {
        var diff = new StringBuilder();
        diff.append("<pre style=\"background: var(--code-bg); padding: 6px 8px; border-radius: 2px; ");
        diff.append("color: var(--text-color); white-space: pre; overflow: auto;\">");
        diff.append("<code>");

        // If originContent is null, show all lines as added (new file)
        if (originContent == null)
        {
            appendDiffLines(diff, "+", newContent, TextColor.GREEN, null);
        }
        // If newContent is null, show all lines as removed (file deletion)
        else if (newContent == null)
        {
            appendDiffLines(diff, "-", originContent, TextColor.RED, null);
        }
        // Otherwise, use Eclipse Compare API to compare and show only changed lines
        else
        {
            String[] originLines = originContent.split("\\r?\\n", -1);
            String[] newLines = newContent.split("\\r?\\n", -1);

            // Create range comparators
            IRangeComparator leftComparator = new LineComparator(originLines);
            IRangeComparator rightComparator = new LineComparator(newLines);

            // Use RangeDifferencer to find differences
            RangeDifference[] differences = RangeDifferencer.findDifferences(leftComparator, rightComparator);

            if (differences == null || differences.length == 0)
            {
                // No differences found - show empty diff
            }
            else
            {
                boolean contextOmitted = false;
                int lastRightEnd = 0;

                for (RangeDifference diffInfo : differences)
                {
                    // Add context placeholder if there was a gap
                    if (diffInfo.rightStart() > lastRightEnd)
                    {
                        if (contextOmitted)
                        {
                            appendContextLine(diff);
                        }
                        contextOmitted = true;
                    }

                    // Show removed lines (from left)
                    for (int i = diffInfo.leftStart(); i < diffInfo.leftEnd(); i++)
                    {
                        if (i < originLines.length)
                        {
                            appendStyledLine(diff, "-" + escapeHtml(originLines[i]), TextColor.RED, null);
                        }
                    }

                    // Show added lines (from right)
                    for (int i = diffInfo.rightStart(); i < diffInfo.rightEnd(); i++)
                    {
                        if (i < newLines.length)
                        {
                            appendStyledLine(diff, "+" + escapeHtml(newLines[i]), TextColor.GREEN, null);
                        }
                    }

                    contextOmitted = false;
                    lastRightEnd = diffInfo.rightEnd();
                }

                // Add context placeholder at the end if needed
                if (lastRightEnd < newLines.length)
                {
                    appendContextLine(diff);
                }
            }
        }

        diff.append("</code></pre>");
        return diff.toString();
    }

    @Override
    @SuppressWarnings("nls")
    public String buildUnifiedDiff(String diffText)
    {
        if (diffText == null || diffText.isBlank())
        {
            return "";
        }

        var diff = new StringBuilder();
        diff.append("<pre style=\"background: var(--code-bg); padding: 6px 8px; border-radius: 2px; ");
        diff.append("color: var(--text-color); white-space: pre; overflow: auto;\">");
        diff.append("<code>");

        var omittedContext = false;
        var omittedVisible = false;
        var lines = diffText.split("\\r?\\n", -1);
        for (var line : lines)
        {
            if (isDiffHeader(line))
            {
                if (omittedContext && omittedVisible)
                {
                    appendContextLine(diff);
                    omittedContext = false;
                    omittedVisible = false;
                }
                continue;
            }

            if (isAddedLine(line))
            {
                if (omittedContext && omittedVisible)
                {
                    appendContextLine(diff);
                    omittedContext = false;
                    omittedVisible = false;
                }
                appendStyledLine(diff, escapeHtml(line), TextColor.GREEN, null);
                continue;
            }

            if (isRemovedLine(line))
            {
                if (omittedContext && omittedVisible)
                {
                    appendContextLine(diff);
                    omittedContext = false;
                    omittedVisible = false;
                }
                appendStyledLine(diff, escapeHtml(line), TextColor.RED, null);
                continue;
            }

            omittedContext = true;
            if (containsVisibleChars(line))
            {
                omittedVisible = true;
            }
        }

        if (omittedContext && omittedVisible)
        {
            appendContextLine(diff);
        }

        diff.append("</code></pre>");
        return diff.toString();
    }

    @Override
    @SuppressWarnings("nls")
    public String buildUnifiedDiffByFile(String diffText)
    {
        if (diffText == null || diffText.isBlank())
        {
            return "";
        }

        var result = new StringBuilder();
        var current = new StringBuilder();
        String currentFileName = null;

        var lines = diffText.split("\\r?\\n", -1);
        for (var line : lines)
        {
            if (line.startsWith("diff --git "))
            {
                if (current.length() > 0)
                {
                    appendDiffSection(result, currentFileName, current.toString());
                    current.setLength(0);
                }
                currentFileName = extractFileName(line);
            }
            current.append(line).append("\n");
        }

        if (current.length() > 0)
        {
            appendDiffSection(result, currentFileName, current.toString());
        }

        return result.toString();
    }

    @Override
    @SuppressWarnings("nls")
    public String formatFilePath(String path)
    {
        if (path == null || path.isBlank())
        {
            return "";
        }

        // Get displayed file name using IFiles
        var file = new java.io.File(path);
        var displayedFileName = files.getDisplayedFileName(file);
        var link = linkProvider.file(path);

        // Escape the path and link for HTML attributes
        var escapedPath = escapeHtml(path);
        var escapedLink = escapeHtml(link);
        var escapedFileName = escapeHtml(displayedFileName);

        return "<a href=\"" + escapedLink + "\" title=\"" + escapedPath + "\">" + escapedFileName + "</a>";
    }

    @Override
    @SuppressWarnings("nls")
    public String formatFilePath(String path, int line, int column)
    {
        if (path == null || path.isBlank())
        {
            return "";
        }

        // Get displayed file name using IFiles
        var file = new java.io.File(path);
        var displayedFileName = files.getDisplayedFileName(file);

        // Use the appropriate linkProvider method based on line/column information
        String link;
        if (line >= 0 && column >= 0)
        {
            link = linkProvider.file(path, line, column);
        }
        else if (line >= 0)
        {
            link = linkProvider.file(path, line, 0);
        }
        else
        {
            link = linkProvider.file(path);
        }

        // Escape the path and link for HTML attributes
        var escapedPath = escapeHtml(path);
        var escapedLink = escapeHtml(link);
        var escapedFileName = escapeHtml(displayedFileName);

        return "<a href=\"" + escapedLink + "\" title=\"" + escapedPath + "\">" + escapedFileName + "</a>";
    }

    @Override
    @SuppressWarnings("nls")
    public String formatFilePath(String path, int line, int column, int finishLine, int finishColumn)
    {
        if (path == null || path.isBlank())
        {
            return "";
        }

        // Get displayed file name using IFiles
        var file = new java.io.File(path);
        var displayedFileName = files.getDisplayedFileName(file);

        // Use the appropriate linkProvider method based on line/column information
        String link;
        if (finishLine >= 0 && finishColumn >= 0)
        {
            // Use range-based link when both start and finish positions are available
            int startColumn = column > 0 ? column : 0;
            int endColumn = finishColumn > 0 ? finishColumn : 0;
            link = linkProvider.file(path, line, startColumn, finishLine, endColumn);
        }
        else if (line >= 0 && column >= 0)
        {
            // Use point-based link when only start position is available
            link = linkProvider.file(path, line, column);
        }
        else if (line >= 0)
        {
            // Use line-only link
            link = linkProvider.file(path, line, 0);
        }
        else
        {
            // Use file-only link
            link = linkProvider.file(path);
        }

        // Escape the path and link for HTML attributes
        var escapedPath = escapeHtml(path);
        var escapedLink = escapeHtml(link);
        var escapedFileName = escapeHtml(displayedFileName);

        return "<a href=\"" + escapedLink + "\" title=\"" + escapedPath + "\">" + escapedFileName + "</a>";
    }

    @SuppressWarnings("nls")
    private void appendDiffLines(StringBuilder diff, String prefix, String content, TextColor color, String background)
    {
        if (content == null)
        {
            return;
        }

        var lines = content.split("\\r?\\n", -1);
        for (var line : lines)
        {
            appendStyledLine(diff, prefix + escapeHtml(line), color, background);
        }
    }

    @SuppressWarnings("nls")
    private static String escapeHtml(String text)
    {
        if (text == null)
        {
            return "";
        }

        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private void appendContextLine(StringBuilder diff)
    {
        appendStyledLine(diff, escapeHtml(Messages.DiffContextPlaceholder), TextColor.GRAY, null);
    }

    @SuppressWarnings("nls")
    private void appendStyledLine(StringBuilder diff, String content, TextColor color, String background)
    {
        var styled = createStyledText(content, color, null);
        if (background == null || background.isBlank())
        {
            diff.append(styled).append("\n");
            return;
        }

        diff.append("<span style=\"background-color: ").append(background).append(";\">")
            .append(styled)
            .append("</span>\n");
    }

    @SuppressWarnings("nls")
    private static boolean isDiffHeader(String line)
    {
        return line.startsWith("diff --git") || line.startsWith("index ") || line.startsWith("--- ")
            || line.startsWith("+++ ") || line.startsWith("@@") || line.startsWith("\\ No newline at end of file");
    }

    @SuppressWarnings("nls")
    private static boolean isAddedLine(String line)
    {
        return line.startsWith("+") && !line.startsWith("+++");
    }

    @SuppressWarnings("nls")
    private static boolean isRemovedLine(String line)
    {
        return line.startsWith("-") && !line.startsWith("---");
    }

    private static boolean containsVisibleChars(String line)
    {
        if (line == null || line.isEmpty())
        {
            return false;
        }
        for (int i = 0; i < line.length(); i++)
        {
            if (!Character.isWhitespace(line.charAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    private void appendDiffSection(StringBuilder result, String fileName, String diffText)
    {
        var title = fileName != null && !fileName.isBlank() ? fileName : "file";
        result.append("<details><summary>").append(escapeHtml(title)).append("</summary>\n\n");
        result.append(buildUnifiedDiff(diffText));
        result.append("\n</details>");
    }

    @SuppressWarnings("nls")
    private static String extractFileName(String line)
    {
        var parts = line.split("\\s+");
        if (parts.length < 4)
        {
            return null;
        }

        var path = parts[3];
        if (path.startsWith("b/"))
        {
            path = path.substring(2);
        }
        var normalized = path.replace('\\', '/');
        var lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    /**
     * Line comparator for Eclipse Compare API
     */
    private static class LineComparator
        implements IRangeComparator
    {
        private final String[] lines;

        public LineComparator(String[] lines)
        {
            this.lines = lines;
        }

        @Override
        public int getRangeCount()
        {
            return lines.length;
        }

        @Override
        public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex)
        {
            if (!(other instanceof LineComparator))
            {
                return false;
            }
            String[] otherLines = ((LineComparator)other).lines;
            if (thisIndex >= lines.length || otherIndex >= otherLines.length)
            {
                return false;
            }
            return lines[thisIndex].equals(otherLines[otherIndex]);
        }

        @Override
        public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other)
        {
            return false;
        }
    }
}
