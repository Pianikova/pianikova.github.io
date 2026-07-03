/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
    private static final int DIFF_CONTEXT_LINES = 3;
    private static final int MIN_LINE_NUMBER_WIDTH = 2;
    private static final Pattern HUNK_HEADER_PATTERN =
        Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*"); //$NON-NLS-1$

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
        // Replace special Markdown characters to prevent formatting issues
        return content.replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("*", "\\*")
            .replace("_", "\\_")
            .replace("#", "\\#")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("-", "\\-")
            .replace("+", "\\+")
            .replace(".", "\\.")
            .replace("!", "\\!");
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
    public String createStyledText(String content, TextColor color, FontWeight weight, boolean escape)
    {
        return createStyledText(content, color, weight, escape, null);
    }

    @Override
    @SuppressWarnings("nls")
    public String createStyledText(String content, TextColor color, FontWeight weight, boolean escape, Double opacity)
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

        return String.format("<span style=\"%s\">%s</span>", style.toString(),
            escape ? escapeForMarkdown(content) : content);
    }

    @Override
    public String buildGitDiff(String filePath, String originContent, String newContent)
    {
        return buildGitDiff(filePath, originContent, newContent, 1, 1, true);
    }

    @Override
    public String buildGitDiff(String filePath, String originContent, String newContent, int originStartLine,
        int newStartLine)
    {
        return buildGitDiff(filePath, originContent, newContent, originStartLine, newStartLine, true);
    }

    @Override
    @SuppressWarnings("nls")
    public String buildGitDiff(String filePath, String originContent, String newContent, int originStartLine,
        int newStartLine, boolean preferNewLineNumbers)
    {
        var diff = new StringBuilder();
        diff.append("<pre style=\"background: var(--code-bg); padding: 6px 8px; border-radius: 2px; ");
        diff.append("color: var(--text-color); white-space: pre; overflow: auto;\">");
        diff.append("<code>");
        var lineNumberWidth = calculateLineNumberWidth(originContent, newContent, originStartLine, newStartLine);

        // If originContent is null, show all lines as added (new file)
        if (originContent == null)
        {
            appendDiffLines(diff, filePath, false, newStartLine, "+", newContent, TextColor.GREEN, null,
                preferNewLineNumbers, lineNumberWidth);
        }
        // If newContent is null, show all lines as removed (file deletion)
        else if (newContent == null)
        {
            appendDiffLines(diff, filePath, true, originStartLine, "-", originContent, TextColor.RED, null,
                preferNewLineNumbers, lineNumberWidth);
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

            if (differences != null && differences.length > 0)
            {
                var hunks = mergeCloseDifferences(differences, DIFF_CONTEXT_LINES);
                int shownOrigin = 0;
                for (DiffHunk hunk : hunks)
                {
                    int contextStart = java.lang.Math.max(shownOrigin, hunk.leftStart - DIFF_CONTEXT_LINES);
                    if (contextStart > shownOrigin)
                    {
                        appendDiffMarker(diff, lineNumberWidth);
                    }

                    // Unchanged context before the change
                    for (int i = contextStart; i < hunk.leftStart && i < originLines.length; i++)
                    {
                        appendNumberedDiffLine(diff, filePath, originStartLine + i,
                            newStartLine + correspondingNewIndex(hunk, i), " ", originLines[i], TextColor.GRAY,
                            preferNewLineNumbers, lineNumberWidth);
                    }

                    appendHunkChanges(diff, filePath, originLines, newLines, hunk, differences, originStartLine,
                        newStartLine, preferNewLineNumbers, lineNumberWidth);

                    // Unchanged context after the change
                    int contextEnd = java.lang.Math.min(originLines.length, hunk.leftEnd + DIFF_CONTEXT_LINES);
                    for (int i = hunk.leftEnd; i < contextEnd; i++)
                    {
                        appendNumberedDiffLine(diff, filePath, originStartLine + i,
                            newStartLine + correspondingNewIndexAfterHunk(hunk, i), " ", originLines[i],
                            TextColor.GRAY, preferNewLineNumbers, lineNumberWidth);
                    }
                    shownOrigin = contextEnd;
                }

                if (shownOrigin < originLines.length)
                {
                    appendDiffMarker(diff, lineNumberWidth);
                }
            }
        }

        diff.append("</code></pre>");
        return diff.toString();
    }

    @Override
    @SuppressWarnings("nls")
    public int[] countChangedLines(String originContent, String newContent)
    {
        if (originContent == null)
        {
            return new int[] { newContent == null ? 0 : countLines(newContent), 0 };
        }
        if (newContent == null)
        {
            return new int[] { 0, countLines(originContent) };
        }

        var originLines = originContent.split("\\r?\\n", -1);
        var newLines = newContent.split("\\r?\\n", -1);
        var differences =
            RangeDifferencer.findDifferences(new LineComparator(originLines), new LineComparator(newLines));

        var added = 0;
        var removed = 0;
        if (differences != null)
        {
            for (RangeDifference difference : differences)
            {
                removed += difference.leftEnd() - difference.leftStart();
                added += difference.rightEnd() - difference.rightStart();
            }
        }
        return new int[] { added, removed };
    }

    @Override
    public String buildUnifiedDiff(String diffText)
    {
        return buildUnifiedDiff(diffText, null);
    }

    @SuppressWarnings("nls")
    private String buildUnifiedDiff(String diffText, String filePath)
    {
        if (diffText == null || diffText.isBlank())
        {
            return "";
        }

        var diff = new StringBuilder();
        diff.append("<pre style=\"background: var(--code-bg); padding: 6px 8px; border-radius: 2px; ");
        diff.append("color: var(--text-color); white-space: pre; overflow: auto;\">");
        diff.append("<code>");

        var lines = diffText.split("\\r?\\n", -1);
        var lineNumberWidth = calculateUnifiedDiffLineNumberWidth(lines);
        var lineState = new UnifiedDiffLineState();
        for (var line : lines)
        {
            // Show hunk headers (they mark where unchanged lines were skipped between changes).
            if (line.startsWith("@@"))
            {
                lineState = parseHunkHeader(line);
                appendStyledLine(diff, escapeHtml(line), TextColor.CYAN, null);
                continue;
            }

            // Skip file-level headers (diff --git / index / --- / +++ / "\ No newline ...").
            if (isDiffHeader(line))
            {
                continue;
            }

            if (isAddedLine(line))
            {
                appendNumberedDiffLine(diff, filePath, null, lineState.nextNewLine(), "+", line.substring(1),
                    TextColor.GREEN, true, lineNumberWidth);
                continue;
            }

            if (isRemovedLine(line))
            {
                appendNumberedDiffLine(diff, filePath, lineState.nextOldLine(), null, "-", line.substring(1),
                    TextColor.RED, true, lineNumberWidth);
                continue;
            }

            // Unchanged context line — keep it so gaps between changes remain visible.
            appendNumberedDiffLine(diff, filePath, lineState.nextOldLine(), lineState.nextNewLine(), " ",
                line.startsWith(" ") ? line.substring(1) : line, TextColor.GRAY, true, lineNumberWidth);
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
        String currentFilePath = null;

        var lines = diffText.split("\\r?\\n", -1);
        for (var line : lines)
        {
            if (line.startsWith("diff --git "))
            {
                if (current.length() > 0)
                {
                    appendDiffSection(result, currentFileName, currentFilePath, current.toString());
                    current.setLength(0);
                }
                currentFileName = extractFileName(line);
                currentFilePath = extractFilePath(line);
            }
            current.append(line).append("\n");
        }

        if (current.length() > 0)
        {
            appendDiffSection(result, currentFileName, currentFilePath, current.toString());
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

    @Override
    @SuppressWarnings("nls")
    public String formatFileLink(String path, int line, int column, int finishLine, int finishColumn, String label)
    {
        if (path == null || path.isBlank())
        {
            return "";
        }

        String link;
        if (finishLine >= 0 && finishColumn >= 0)
        {
            int startColumn = column > 0 ? column : 0;
            int endColumn = finishColumn > 0 ? finishColumn : 0;
            link = linkProvider.file(path, line, startColumn, finishLine, endColumn);
        }
        else if (line >= 0 && column >= 0)
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

        var safeLabel = label == null ? "" : label;
        var escapedPath = escapeHtml(path);
        var escapedLink = escapeHtml(link);
        var escapedLabel = escapeHtml(safeLabel);

        return "<a href=\"" + escapedLink + "\" title=\"" + escapedPath + "\">" + escapedLabel + "</a>";
    }

    @Override
    @SuppressWarnings("nls")
    public String getDisplayedFileName(String path)
    {
        if (path == null || path.isBlank())
        {
            return "";
        }
        return files.getDisplayedFileName(new java.io.File(path));
    }

    @Override
    @SuppressWarnings("nls")
    public String formatDiffLink(String token, String label)
    {
        if (token == null || token.isBlank())
        {
            return "";
        }

        var safeLabel = label == null ? "" : label;
        var escapedLink = escapeHtml(linkProvider.diff(token));
        var escapedLabel = escapeHtml(safeLabel);

        return "<a href=\"" + escapedLink + "\" title=\"" + escapedLabel + "\">" + escapedLabel + "</a>";
    }

    @SuppressWarnings("nls")
    private void appendDiffLines(StringBuilder diff, String filePath, boolean oldSide, int startLine, String prefix,
        String content, TextColor color, String background, boolean preferNewLineNumbers, int lineNumberWidth)
    {
        if (content == null)
        {
            return;
        }

        var lines = content.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++)
        {
            Integer oldLine = oldSide ? startLine + i : null;
            Integer newLine = oldSide ? null : startLine + i;
            appendNumberedDiffLine(diff, filePath, oldLine, newLine, prefix, lines[i], color, preferNewLineNumbers,
                lineNumberWidth);
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

    @SuppressWarnings("nls")
    private void appendStyledLine(StringBuilder diff, String content, TextColor color, String background)
    {
        var styled = createStyledText(content, color, null, false);
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
    private void appendNumberedDiffLine(StringBuilder diff, String filePath, Integer oldLine, Integer newLine,
        String prefix, String content, TextColor color, boolean preferNewLineNumbers, int lineNumberWidth)
    {
        var line = preferNewLineNumbers ? firstPositive(newLine, oldLine) : firstPositive(oldLine, newLine);
        // The line-number column and unchanged (context) lines are rendered plain — no color span —
        // so the clickable line number sits directly next to the code. Only real changes are colored.
        var lineColumn = formatLineNumberColumn(filePath, line, lineNumberWidth);
        var body = prefix + escapeHtml(content);
        var styledBody = color == null || color == TextColor.GRAY ? body : createStyledText(body, color, null, false);
        diff.append(lineColumn).append(" ").append(styledBody).append("\n");
    }

    private static Integer firstPositive(Integer preferred, Integer fallback)
    {
        if (preferred != null && preferred > 0)
        {
            return preferred;
        }
        return fallback;
    }

    private static int calculateLineNumberWidth(String originContent, String newContent, int originStartLine,
        int newStartLine)
    {
        var maxLine = 0;
        if (originContent != null)
        {
            maxLine = java.lang.Math.max(maxLine, originStartLine + countLines(originContent) - 1);
        }
        if (newContent != null)
        {
            maxLine = java.lang.Math.max(maxLine, newStartLine + countLines(newContent) - 1);
        }
        return calculateLineNumberWidth(maxLine);
    }

    private static int calculateUnifiedDiffLineNumberWidth(String[] lines)
    {
        var maxLine = 0;
        for (var line : lines)
        {
            if (!line.startsWith("@@")) //$NON-NLS-1$
            {
                continue;
            }

            var matcher = HUNK_HEADER_PATTERN.matcher(line);
            if (!matcher.matches())
            {
                continue;
            }

            maxLine = java.lang.Math.max(maxLine, hunkEndLine(matcher.group(1), matcher.group(2)));
            maxLine = java.lang.Math.max(maxLine, hunkEndLine(matcher.group(3), matcher.group(4)));
        }
        return calculateLineNumberWidth(maxLine);
    }

    private static int hunkEndLine(String startGroup, String countGroup)
    {
        var start = Integer.parseInt(startGroup);
        var count = countGroup == null ? 1 : Integer.parseInt(countGroup);
        return start + java.lang.Math.max(0, count - 1);
    }

    private static int calculateLineNumberWidth(int maxLine)
    {
        return java.lang.Math.max(MIN_LINE_NUMBER_WIDTH, String.valueOf(java.lang.Math.max(0, maxLine)).length());
    }

    private static int countLines(String content)
    {
        return content.split("\\r?\\n", -1).length; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String formatLineNumberColumn(String filePath, Integer line, int lineNumberWidth)
    {
        if (line == null || line <= 0)
        {
            return spaces(lineNumberWidth);
        }

        var label = String.valueOf(line);
        var padding = spaces(lineNumberWidth - label.length());
        if (filePath == null || filePath.isBlank())
        {
            return padding + label;
        }

        // Bare anchor (no title attribute, padding kept outside the link) so the clickable number
        // renders as a compact "<a href=...>N</a>" with no stray spaces inside the link.
        var link = linkProvider.file(filePath, line, 1);
        return padding + "<a href=\"" + escapeHtml(link) + "\">" + label + "</a>";
    }

    @SuppressWarnings("nls")
    private void appendDiffMarker(StringBuilder diff, int lineNumberWidth)
    {
        appendStyledLine(diff, spaces(lineNumberWidth) + " ...", TextColor.GRAY, null);
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

    @SuppressWarnings("nls")
    private void appendDiffSection(StringBuilder result, String fileName, String filePath, String diffText)
    {
        var title = fileName != null && !fileName.isBlank() ? fileName : "file";
        result.append("**").append(escapeHtml(title)).append("**\n\n");
        result.append(buildUnifiedDiff(diffText, filePath));
    }

    private static String extractFileName(String line)
    {
        var path = extractFilePath(line);
        if (path == null)
        {
            return null;
        }

        var normalized = path.replace('\\', '/');
        var lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    @SuppressWarnings("nls")
    private static String extractFilePath(String line)
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
        return path;
    }

    private static List<DiffHunk> mergeCloseDifferences(RangeDifference[] differences, int contextLines)
    {
        var hunks = new ArrayList<DiffHunk>();
        for (RangeDifference difference : differences)
        {
            var next = new DiffHunk(difference.leftStart(), difference.leftEnd(), difference.rightStart(),
                difference.rightEnd());
            if (hunks.isEmpty())
            {
                hunks.add(next);
                continue;
            }

            var current = hunks.get(hunks.size() - 1);
            if (next.leftStart - current.leftEnd <= contextLines * 2)
            {
                current.leftEnd = next.leftEnd;
                current.rightEnd = next.rightEnd;
            }
            else
            {
                hunks.add(next);
            }
        }
        return hunks;
    }

    private void appendHunkChanges(StringBuilder diff, String filePath, String[] originLines, String[] newLines,
        DiffHunk hunk, RangeDifference[] differences, int originStartLine, int newStartLine,
        boolean preferNewLineNumbers, int lineNumberWidth)
    {
        int leftPosition = hunk.leftStart;
        int rightPosition = hunk.rightStart;
        for (RangeDifference difference : differences)
        {
            if (difference.leftStart() < hunk.leftStart || difference.leftStart() > hunk.leftEnd)
            {
                continue;
            }

            while (leftPosition < difference.leftStart() && leftPosition < originLines.length)
            {
                appendNumberedDiffLine(diff, filePath, originStartLine + leftPosition, newStartLine + rightPosition,
                    " ", originLines[leftPosition], TextColor.GRAY, preferNewLineNumbers, lineNumberWidth); //$NON-NLS-1$
                leftPosition++;
                rightPosition++;
            }

            for (int i = difference.leftStart(); i < difference.leftEnd() && i < originLines.length; i++)
            {
                appendNumberedDiffLine(diff, filePath, originStartLine + i, null, "-", originLines[i], TextColor.RED, //$NON-NLS-1$
                    preferNewLineNumbers, lineNumberWidth);
            }

            for (int i = difference.rightStart(); i < difference.rightEnd() && i < newLines.length; i++)
            {
                appendNumberedDiffLine(diff, filePath, null, newStartLine + i, "+", newLines[i], TextColor.GREEN, //$NON-NLS-1$
                    preferNewLineNumbers, lineNumberWidth);
            }

            leftPosition = difference.leftEnd();
            rightPosition = difference.rightEnd();
        }
    }

    private static int correspondingNewIndex(DiffHunk hunk, int originIndex)
    {
        return hunk.rightStart + (originIndex - hunk.leftStart);
    }

    private static int correspondingNewIndexAfterHunk(DiffHunk hunk, int originIndex)
    {
        return hunk.rightEnd + (originIndex - hunk.leftEnd);
    }

    private static UnifiedDiffLineState parseHunkHeader(String line)
    {
        var matcher = HUNK_HEADER_PATTERN.matcher(line);
        if (!matcher.matches())
        {
            return new UnifiedDiffLineState();
        }
        return new UnifiedDiffLineState(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(3)));
    }

    private static String spaces(int count)
    {
        return " ".repeat(java.lang.Math.max(0, count)); //$NON-NLS-1$
    }

    private static class DiffHunk
    {
        private final int leftStart;
        private int leftEnd;
        private final int rightStart;
        private int rightEnd;

        private DiffHunk(int leftStart, int leftEnd, int rightStart, int rightEnd)
        {
            this.leftStart = leftStart;
            this.leftEnd = leftEnd;
            this.rightStart = rightStart;
            this.rightEnd = rightEnd;
        }
    }

    private static class UnifiedDiffLineState
    {
        private int oldLine;
        private int newLine;

        private UnifiedDiffLineState()
        {
            this(0, 0);
        }

        private UnifiedDiffLineState(int oldLine, int newLine)
        {
            this.oldLine = oldLine;
            this.newLine = newLine;
        }

        private int nextOldLine()
        {
            return oldLine++;
        }

        private int nextNewLine()
        {
            return newLine++;
        }
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
