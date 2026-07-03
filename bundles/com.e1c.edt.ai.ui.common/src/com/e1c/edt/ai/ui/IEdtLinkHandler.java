/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import com.e1c.edt.ai.AIContext;

/**
 * Interface for handling EDT-specific links (edt-file:// protocol)
 */
public interface IEdtLinkHandler
{
	/**
	 * Formats a path as an EDT file link with optional position information
	 *
	 * @param ctx the AI context containing document information
	 * @param path the file path to format
	 * @return the formatted EDT file link
	 */
	String formatInsertCodePath(AIContext ctx, String path);

	/**
	 * Extracts the full file path from an AI context
	 *
	 * @param ctx the AI context
	 * @return the full file path or empty if not available
	 */
	String getFullPathForInsertCode(AIContext ctx);

	/**
	 * Extracts the file path from an EDT link, removing protocol prefix and postfix
	 *
	 * @param href the EDT link (e.g., "edt-file://path/to/file.txt:line:col" or "edt-file://path/to/file.txt")
	 * @return the extracted file path, or empty string if not a valid EDT link
	 */
	String extractFilePath(String href);

	/**
	 * Checks if the given href is a recognized EDT link
	 *
	 * @param href the href to check
	 * @return true if the href is a recognized EDT link, false otherwise
	 */
	boolean isRecognizedHref(String href);

	/**
	 * Checks if the given href is a diff-preview link (edt-diff:// protocol)
	 *
	 * @param href the href to check
	 * @return true if the href is a recognized diff-preview link, false otherwise
	 */
	boolean isDiffHref(String href);

	/**
	 * Extracts the diff-preview token from a diff-preview link
	 *
	 * @param href the diff-preview link (e.g., "edt-diff://&lt;token&gt;")
	 * @return the extracted token, or empty string if not a valid diff-preview link
	 */
	String extractDiffToken(String href);

	/**
	 * Extracts cursor position information from an EDT link
	 *
	 * @param href the EDT link
	 * @return Optional containing CursorPositionInfo with line and column, or empty if no position information
	 */
	Optional<CursorPositionInfo> extractCursorPosition(String href);

	/**
	 * Extracts selection information from an EDT link
	 *
	 * @param href the EDT link
	 * @return Optional containing SelectionInfo with start and end positions, or empty if no selection information
	 */
	Optional<SelectionInfo> extractSelection(String href);

	/**
	 * Class representing cursor position information
	 */
	class CursorPositionInfo
	{
		private final int line;
		private final int column;

		public CursorPositionInfo(int line, int column)
		{
			this.line = line;
			this.column = column;
		}

		public int getLine()
		{
			return line;
		}

		public int getColumn()
		{
			return column;
		}
	}

	/**
	 * Class representing selection information
	 */
	class SelectionInfo
	{
		private final int startLine;
		private final int startColumn;
		private final int endLine;
		private final int endColumn;

		public SelectionInfo(int startLine, int startColumn, int endLine, int endColumn)
		{
			this.startLine = startLine;
			this.startColumn = startColumn;
			this.endLine = endLine;
			this.endColumn = endColumn;
		}

		public int getStartLine()
		{
			return startLine;
		}

		public int getStartColumn()
		{
			return startColumn;
		}

		public int getEndLine()
		{
			return endLine;
		}

		public int getEndColumn()
		{
			return endColumn;
		}
	}
}
