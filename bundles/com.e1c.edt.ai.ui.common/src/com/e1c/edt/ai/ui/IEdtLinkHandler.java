/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.text.IDocument;

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
}
