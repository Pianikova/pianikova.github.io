/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Implementation of IEditorPositionManager for handling editor operations
 */
public class EditorPositionManager
	implements IEditorPositionManager
{
	private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$
	private final ILog log;
	private final IDispatcher dispatcher;
	private final ISpecializedEditorOpener specializedEditorOpener;

	@Inject
    public EditorPositionManager(ILog log, IDispatcher dispatcher,
        ISpecializedEditorOpener specializedEditorOpener)
	{
		Preconditions.checkNotNull(log);
		Preconditions.checkNotNull(dispatcher);
		Preconditions.checkNotNull(specializedEditorOpener);
		this.log = log;
		this.dispatcher = dispatcher;
		this.specializedEditorOpener = specializedEditorOpener;
	}

	@Override
	@SuppressWarnings("nls")
	public void openFileInEditor(String filePath, IEdtLinkHandler.CursorPositionInfo cursorPosition,
		IEdtLinkHandler.SelectionInfo selection)
	{
		IEditorPart editor = null;
		try
		{
			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			// First try to find file in workspace (relative path)
			var root = ResourcesPlugin.getWorkspace().getRoot();
            try
            {
                var file = root.getFile(new Path(filePath));
                if (file != null && file.exists())
                {
                    editor = specializedEditorOpener.openInSpecializedEditor(page, file);
                    if (editor == null)
                    {
                        editor = IDE.openEditor(page, file, true);
                    }
                }
			}
            catch (Exception e)
            {
                log.logError("workspace-relative branch threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (editor == null)
            {
                // Try to map an absolute filesystem path back to a workspace IFile so that
                // EDT's content-type-bound editors (BSL Xtext, form, MD) are used.
                try
                {
                    var osPath = Path.fromOSString(filePath);
                    var fileForLocation = root.getFileForLocation(osPath);

                    if (fileForLocation == null)
                    {
                        // Fallback: try findFilesForLocationURI for linked / nested resources
                        var found = root.findFilesForLocationURI(new File(filePath).toURI());
                        for (var f : found)
                        {
                            if (fileForLocation == null && f.exists())
                            {
                                fileForLocation = f;
                            }
                        }
                    }

                    if (fileForLocation != null && fileForLocation.exists())
                    {
                        editor = specializedEditorOpener.openInSpecializedEditor(page, fileForLocation);
                        if (editor == null)
                        {
                            editor = IDE.openEditor(page, fileForLocation, true);
                        }
                    }
                }
                catch (Exception e)
                {
                    log.logError(
                        "workspace-absolute branch threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }

            if (editor == null)
			{
				// If not found in workspace, try as absolute path
				var externalFile = new File(filePath);
				if (externalFile.exists() && externalFile.isFile())
				{
					IFileStore fileStore = EFS.getLocalFileSystem().getStore(externalFile.toURI());
					editor = IDE.openEditorOnFileStore(page, fileStore);
				}
				else
				{
					// File not found
					log.logError("File not found: " + filePath);
					return;
				}
			}

			// Restore cursor position and selection if available
			if (editor != null)
			{
				final var editorRef = editor;
				dispatcher.dispatchAsync(() ->
				{
					// Try to restore selection first
					if (selection != null)
					{
						restoreSelection(editorRef, selection);
					}
					// If no selection, try to restore cursor position
					else if (cursorPosition != null)
					{
						restoreCursorPosition(editorRef, cursorPosition);
					}
				});
			}
		}
		catch (PartInitException e)
		{
			log.logError(e);
		}
		catch (Exception e)
		{
			log.logError(e);
		}
	}

	@Override
	@SuppressWarnings("nls")
	public void restoreCursorPosition(IEditorPart editor, IEdtLinkHandler.CursorPositionInfo cursorPosition)
	{
		log.trace(TracingSources.CHAT, AI_CHAT,
			() -> "restoreCursorPosition: editor=" + editor.getClass().getSimpleName());

		IDocument document = null;
		ITextOperationTarget sourceViewer = null;

		// First try to get document from ITextEditor
		if (editor instanceof ITextEditor)
		{
			var textEditor = (ITextEditor)editor;
			var documentProvider = textEditor.getDocumentProvider();
			if (documentProvider != null)
			{
				document = documentProvider.getDocument(textEditor.getEditorInput());
			}
		}

		// If not found via ITextEditor, try to get via adapter (for editors that don't implement ITextEditor)
		if (document == null)
		{
			sourceViewer = editor.getAdapter(ITextOperationTarget.class);
			if (sourceViewer != null && sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
			{
				var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
				document = viewer.getDocument();
				log.trace(TracingSources.CHAT, AI_CHAT, () -> "Got document from SourceViewer adapter");
			}
		}

		if (document == null)
		{
			log.trace(TracingSources.CHAT, AI_CHAT, () -> "Cannot get document from editor");
			return;
		}

		try
		{
			var numberOfLines = document.getNumberOfLines();
			log.logError("Document has " + numberOfLines + " lines");

			// Convert line/column to offset
			var line = Math.max(0, cursorPosition.getLine() - 1);
			var column = Math.max(0, cursorPosition.getColumn() - 1);

			// Check if line is within document bounds
			if (line >= numberOfLines)
			{
				log.logError("Cursor line out of bounds: line=" + line + ", numberOfLines=" + numberOfLines);

				// Clamp to last line
				line = numberOfLines - 1;
				column = 0; // Reset column when clamping to last line
			}

			final var offset = Math.max(0, Math.min(document.getLineOffset(line) + column, document.getLength()));

			log.logError("Setting cursor position: line=" + (line + 1) + ", column=" + (column + 1) + ", offset="
				+ offset + ", documentLength=" + document.getLength());

			var textSelection = new TextSelection(offset, 0);

			// Set selection on the appropriate target
			if (editor instanceof ITextEditor)
			{
				var textEditor = (ITextEditor)editor;
				textEditor.getSelectionProvider().setSelection(textSelection);
			}
			else if (sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
			{
				var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
				viewer.getSelectionProvider().setSelection(textSelection);
			}
		}
		catch (org.eclipse.jface.text.BadLocationException e)
		{
			log.logError("BadLocationException when restoring cursor position: " + e.getMessage());
			// Try to restore cursor to document start as fallback
			try
			{
				var textSelection = new TextSelection(0, 0);

				if (editor instanceof ITextEditor)
				{
					var textEditor = (ITextEditor)editor;
					textEditor.getSelectionProvider().setSelection(textSelection);
				}
				else if (sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
				{
					var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
					viewer.getSelectionProvider().setSelection(textSelection);
				}
			}
			catch (Exception ex)
			{
				log.logError("Failed to set fallback cursor position: " + ex.getMessage());
			}
		}
	}

	@Override
	@SuppressWarnings("nls")
	public void restoreSelection(IEditorPart editor, IEdtLinkHandler.SelectionInfo selection)
	{
		log.trace(TracingSources.CHAT, AI_CHAT, () -> "restoreSelection: editor=" + editor.getClass().getSimpleName());

		IDocument document = null;
		ITextOperationTarget sourceViewer = null;

		// First try to get document from ITextEditor
		if (editor instanceof ITextEditor)
		{
			var textEditor = (ITextEditor)editor;
			var documentProvider = textEditor.getDocumentProvider();
			if (documentProvider != null)
			{
				document = documentProvider.getDocument(textEditor.getEditorInput());
			}
		}

		// If not found via ITextEditor, try to get via adapter (for editors that don't implement ITextEditor)
		if (document == null)
		{
			sourceViewer = editor.getAdapter(ITextOperationTarget.class);
			if (sourceViewer != null && sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
			{
				var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
				document = viewer.getDocument();
				log.trace(TracingSources.CHAT, AI_CHAT, () -> "Got document from SourceViewer adapter");
			}
		}

		if (document == null)
		{
			log.trace(TracingSources.CHAT, AI_CHAT, () -> "Cannot get document from editor");
			return;
		}

		try
		{
			var numberOfLines = document.getNumberOfLines();
			log.logError("Document has " + numberOfLines + " lines");

			// Convert line/column to offset
            var startLine = Math.max(0, selection.getStartLine() - 1);
            var startColumn = Math.max(0, selection.getStartColumn() - 1);
            var endLine = Math.max(0, selection.getEndLine() - 1);
            var endColumn = Math.max(0, selection.getEndColumn() - 1);
            if (endLine == numberOfLines - 1)
            {
                endColumn++;
            }

			// Check if lines are within document bounds
			if (startLine >= numberOfLines || endLine >= numberOfLines)
			{
				log.logError("Selection lines out of bounds: startLine=" + startLine + ", endLine=" + endLine
					+ ", numberOfLines=" + numberOfLines);

				// Try to clamp to document bounds
				startLine = Math.min(startLine, numberOfLines - 1);
				endLine = Math.min(endLine, numberOfLines - 1);

                // If clamped to the last line, set endColumn to the end of that line
                if (endLine == numberOfLines - 1)
                {
                    try
                    {
                        var lineLength = document.getLineLength(endLine);
                        endColumn = lineLength;
                    }
                    catch (Exception e)
                    {
                        log.logError("Error getting line length: " + e.getMessage());
                        endColumn = 0;
                    }
                }
			}

            // Проверка: столбцы не должны выходить за пределы длины соответствующих строк
            // Это предотвращает ситуацию, когда offset(endLine)+endColumn может указывать на следующую строку или за пределы документа
            try
            {
                var startLineLength = document.getLineLength(startLine);
                startColumn = Math.min(startColumn, startLineLength);

                var endLineLength = document.getLineLength(endLine);
                endColumn = Math.min(endColumn, endLineLength);
            }
            catch (Exception e)
            {
                log.logError("Error getting line length for column bounds check: " + e.getMessage());
            }

			final var startOffset =
				Math.max(0, Math.min(document.getLineOffset(startLine) + startColumn, document.getLength()));
			final var endOffset =
				Math.max(0, Math.min(document.getLineOffset(endLine) + endColumn, document.getLength()));

			var length = endOffset - startOffset;

			// If length is negative, swap start and end
			final var finalStartOffset = length >= 0 ? startOffset : endOffset;
			final var finalLength = Math.abs(length);

			log.logError("Setting selection: startOffset=" + finalStartOffset + ", length=" + finalLength
				+ ", documentLength=" + document.getLength());

			// Set selection on the appropriate target
			if (finalLength >= 0)
			{
				var textSelection = new TextSelection(finalStartOffset, finalLength);

				if (editor instanceof ITextEditor)
				{
					var textEditor = (ITextEditor)editor;
					textEditor.getSelectionProvider().setSelection(textSelection);
				}
				else if (sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
				{
					var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
					viewer.getSelectionProvider().setSelection(textSelection);
				}
			}
		}
		catch (org.eclipse.jface.text.BadLocationException e)
		{
			log.logError("BadLocationException when restoring selection: " + e.getMessage());
			// Try to restore cursor to document start as fallback
			try
			{
				var textSelection = new TextSelection(0, 0);

				if (editor instanceof ITextEditor)
				{
					var textEditor = (ITextEditor)editor;
					textEditor.getSelectionProvider().setSelection(textSelection);
				}
				else if (sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
				{
					var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
					viewer.getSelectionProvider().setSelection(textSelection);
				}
			}
			catch (Exception ex)
			{
				log.logError("Failed to set fallback cursor position: " + ex.getMessage());
			}
		}
	}
}
