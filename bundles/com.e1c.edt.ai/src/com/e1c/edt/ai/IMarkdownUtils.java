/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

/**
 * Interface for markdown formatting operations
 */
public interface IMarkdownUtils
{
	/**
	 * Escapes content for markdown display by replacing backticks
	 * @param content The content to escape
	 * @return Escaped content safe for markdown display
	 */
	String escapeForMarkdown(String content);

	/**
     * Decodes URL-encoded characters in the given string.
     * Handles URL encoding like %3A (colon), %D0 (Cyrillic), etc.
     * @param content The URL-encoded string
     * @return The decoded string
     */
    String decodeUrl(String content);

    /**
     * Creates styled text with specified color and weight for HTML display
     * @param content The content to style
     * @param color The text color
     * @param weight The font weight
     * @return Styled HTML text
     */
    String createStyledText(String content, TextColor color, FontWeight weight);

	/**
     * Creates styled text with specified color, weight, and opacity for HTML display
     * @param content The content to style
     * @param color The text color
     * @param weight The font weight
     * @param opacity The opacity value (0.0 to 1.0), or null to not set opacity
     * @return Styled HTML text
     */
    String createStyledText(String content, TextColor color, FontWeight weight, Double opacity);

	/**
	 * Builds a markdown-friendly diff view between original and new content.
	 * @param filePath The file path to display in diff headers
	 * @param originContent The original content
	 * @param newContent The new content
	 * @return A markdown-safe diff view
	 */
	String buildGitDiff(String filePath, String originContent, String newContent);

	/**
	 * Builds a markdown-friendly diff view from unified diff text.
	 * @param diffText Unified diff text
	 * @return A markdown-safe diff view
	 */
	String buildUnifiedDiff(String diffText);

	/**
	 * Builds a markdown-friendly diff view grouped by file name.
	 * @param diffText Unified diff text
	 * @return A markdown-safe diff view grouped by file
	 */
	String buildUnifiedDiffByFile(String diffText);

	/**
	 * Formats a file path for markdown display with styled file name.
	 * @param path The file path to format
	 * @return A markdown-formatted string with styled file name
	 */
	String formatFilePath(String path);

    /**
     * Formats a file path with line and column information for markdown display with styled file name.
     * @param path The file path to format
     * @param line The line number (1-relative, or 0/less if unknown)
     * @param column The column number (1-relative, or 0/less if unknown)
     * @return A markdown-formatted string with styled file name and link with position
     */
    String formatFilePath(String path, int line, int column);

    /**
     * Formats a file path with line range (start and finish positions) for markdown display with styled file name.
     * @param path The file path to format
     * @param line The start line number (1-relative, or 0/less if unknown)
     * @param column The start column number (1-relative, or 0/less if unknown)
     * @param finishLine The finish line number (1-relative, or 0/less if unknown)
     * @param finishColumn The finish column number (1-relative, or 0/less if unknown)
     * @return A markdown-formatted string with styled file name and link with range
     */
    String formatFilePath(String path, int line, int column, int finishLine, int finishColumn);
}
