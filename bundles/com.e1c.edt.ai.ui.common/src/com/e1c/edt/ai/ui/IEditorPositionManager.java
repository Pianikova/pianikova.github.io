/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.ui.IEditorPart;

/**
 * Interface for managing editor opening and restoring cursor positions and selections
 */
public interface IEditorPositionManager
{
	/**
	 * Opens a file in an editor and optionally restores cursor position or selection
	 *
	 * @param filePath the path to the file to open
	 * @param cursorPosition the cursor position information (optional, can be null)
	 * @param selection the selection information (optional, can be null)
	 */
	void openFileInEditor(String filePath, IEdtLinkHandler.CursorPositionInfo cursorPosition,
		IEdtLinkHandler.SelectionInfo selection);

	/**
	 * Restores cursor position in the editor
	 *
	 * @param editor the editor part
	 * @param cursorPosition the cursor position information
	 */
	void restoreCursorPosition(IEditorPart editor, IEdtLinkHandler.CursorPositionInfo cursorPosition);

	/**
	 * Restores selection in the editor
	 *
	 * @param editor the editor part
	 * @param selection the selection information
	 */
	void restoreSelection(IEditorPart editor, IEdtLinkHandler.SelectionInfo selection);
}
