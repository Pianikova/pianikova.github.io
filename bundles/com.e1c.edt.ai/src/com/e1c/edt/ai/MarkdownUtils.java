/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

import com.google.inject.Singleton;

/**
 * Utility class for markdown formatting operations
 */
@Singleton
public class MarkdownUtils implements IMarkdownUtils
{
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
    @SuppressWarnings("nls")
    public String createStyledText(String content, TextColor color, FontWeight weight)
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
        appendDiffLines(diff, "-", originContent, TextColor.RED, "var(--code-bg)");
        appendDiffLines(diff, "+", newContent, TextColor.GREEN, "var(--code-bg)");
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
                appendStyledLine(diff, escapeHtml(line), TextColor.GREEN, "var(--code-bg)");
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
                appendStyledLine(diff, escapeHtml(line), TextColor.RED, "var(--code-bg)");
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
}
