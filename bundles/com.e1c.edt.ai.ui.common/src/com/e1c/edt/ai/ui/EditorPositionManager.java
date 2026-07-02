/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;
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
	public void openFileInEditor(String filePath, String tabTitle, IEdtLinkHandler.CursorPositionInfo cursorPosition,
		IEdtLinkHandler.SelectionInfo selection)
	{
		IEditorPart editor = null;
		try
		{
			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			var root = ResourcesPlugin.getWorkspace().getRoot();

			// Resolve the link target to a workspace file (relative path, then absolute location).
			var file = resolveWorkspaceFile(root, filePath);

			if (file != null && file.exists())
			{
				// Reuse an already-open editor for this file instead of opening a second one. The
				// platform's own reuse can miss domain/metadata editors (e.g. a Document editor) that
				// use a model-based input, so match by the file the editor input resolves to.
				editor = findOpenEditorForFile(file);
				if (editor != null)
				{
					editor.getSite().getPage().activate(editor);
				}
				else
				{
					editor = specializedEditorOpener.openInSpecializedEditor(page, file);
					if (editor == null)
					{
						editor = IDE.openEditor(page, file, true);
					}
				}
			}

			if (editor == null)
			{
				// Not in the workspace — open as an external file.
				var externalFile = new File(filePath);
				if (externalFile.exists() && externalFile.isFile())
				{
					IFileStore fileStore = EFS.getLocalFileSystem().getStore(externalFile.toURI());
					editor = IDE.openEditorOnFileStore(page, fileStore);
				}
				else
				{
					log.logError("File not found: " + filePath);
					return;
				}
			}

			// If a descriptive tab title is provided, show it on the tab and move the original tab
			// label (the file name) to the tooltip.
			if (editor != null)
			{
				applyTabTitle(editor, tabTitle);
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

	/**
	 * Resolves a link target path to a workspace {@link IFile}: first as a workspace-relative path,
	 * then by mapping an absolute filesystem location (including linked/nested resources). Returns
	 * {@code null} if it does not correspond to a workspace file.
	 */
	@SuppressWarnings("nls")
	private IFile resolveWorkspaceFile(IWorkspaceRoot root, String filePath)
	{
		try
		{
			var relative = root.getFile(new Path(filePath));
			if (relative != null && relative.exists())
			{
				return relative;
			}
		}
		catch (Exception e)
		{
			log.logError("workspace-relative resolve threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		try
		{
			var fileForLocation = root.getFileForLocation(Path.fromOSString(filePath));
			if (fileForLocation != null && fileForLocation.exists())
			{
				return fileForLocation;
			}

			for (var f : root.findFilesForLocationURI(new File(filePath).toURI()))
			{
				if (f.exists())
				{
					return f;
				}
			}
		}
		catch (Exception e)
		{
			log.logError("workspace-absolute resolve threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		return null;
	}

	/**
	 * Finds an already-open editor that edits the given file, searching all workbench windows/pages.
	 * Matches by the {@link IFile} the editor input resolves to (via {@link ResourceUtil}), so it also
	 * catches domain/metadata editors whose input is not a plain {@code IFileEditorInput}. Returns
	 * {@code null} if the file is not open in any editor.
	 */
	private IEditorPart findOpenEditorForFile(IFile file)
	{
		var workbench = PlatformUI.getWorkbench();
		if (workbench == null)
		{
			return null;
		}

		for (var window : workbench.getWorkbenchWindows())
		{
			for (var page : window.getPages())
			{
				for (IEditorReference ref : page.getEditorReferences())
				{
					try
					{
						if (file.equals(ResourceUtil.getFile(ref.getEditorInput())))
						{
							return ref.getEditor(true);
						}
					}
					catch (PartInitException e)
					{
						// Input could not be restored — skip this editor.
					}
				}
			}
		}

		return null;
	}

	/**
	 * Replaces the editor tab label with {@code tabTitle}, moving the original tab label (the file name)
	 * to the tooltip. Does nothing when {@code tabTitle} is null/blank, so the default Eclipse tab name is
	 * kept. Uses the E4 {@link MPart} of the editor, so it works uniformly for workspace, specialized EDT
	 * and external (file-store) editors without substituting the editor input.
	 *
	 * @param editor the opened editor part
	 * @param tabTitle the descriptive tab title, or {@code null}/blank to keep the default
	 */
	@SuppressWarnings("nls")
	private void applyTabTitle(IEditorPart editor, String tabTitle)
	{
		// Keep the default tab name for blank titles and for line-number/line-range links (their
		// visible text is just a number like "23" or "34-45"); renaming a tab to a bare number is wrong.
		if (tabTitle == null || tabTitle.isBlank() || tabTitle.trim().matches("\\d+(-\\d+)?"))
		{
			return;
		}
		try
		{
			var part = editor.getSite().getService(MPart.class);
			if (part != null)
			{
				// Copy whatever was shown on the tab into the tooltip before overwriting the label.
				// Fall back to the editor input name if the part label is not populated yet.
				var previousLabel = part.getLabel();
				if (previousLabel == null || previousLabel.isBlank())
				{
					previousLabel = editor.getEditorInput().getName();
				}
				// Drop a trailing line-range suffix (e.g. "Модуль 34-45" → "Модуль") so the tab shows
				// the object name, not the edited line range.
				var label = tabTitle.trim().replaceFirst("\\s+\\d+(-\\d+)?$", "");
				part.setLabel(label.isBlank() ? tabTitle.trim() : label);
				part.setTooltip(previousLabel);
			}
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
			log.trace(TracingSources.CHAT, AI_CHAT, () -> "Document has " + numberOfLines + " lines");

			// Convert line/column to offset
			var line = Math.max(0, cursorPosition.getLine() - 1);
			var column = Math.max(0, cursorPosition.getColumn() - 1);

			// Check if line is within document bounds
			if (line >= numberOfLines)
			{
				final var outOfBoundsLine = line;
				log.warning(AI_CHAT,
					() -> "Cursor line out of bounds: line=" + outOfBoundsLine + ", numberOfLines=" + numberOfLines);

				// Clamp to last line
				line = numberOfLines - 1;
				column = 0; // Reset column when clamping to last line
			}

			final var offset = Math.max(0, Math.min(document.getLineOffset(line) + column, document.getLength()));

			final var finalLine = line;
			final var finalColumn = column;
			final var documentLength = document.getLength();
			log.trace(TracingSources.CHAT, AI_CHAT, () -> "Setting cursor position: line=" + (finalLine + 1)
				+ ", column=" + (finalColumn + 1) + ", offset=" + offset + ", documentLength=" + documentLength);

			// Set the caret and scroll it into view (selectAndReveal / revealRange).
			if (editor instanceof ITextEditor)
			{
				((ITextEditor)editor).selectAndReveal(offset, 0);
			}
			else if (sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
			{
				var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
				viewer.setSelectedRange(offset, 0);
				viewer.revealRange(offset, 0);
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
			log.trace(TracingSources.CHAT, AI_CHAT, () -> "Document has " + numberOfLines + " lines");

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
				final var outOfBoundsStartLine = startLine;
				final var outOfBoundsEndLine = endLine;
				log.warning(AI_CHAT, () -> "Selection lines out of bounds: startLine=" + outOfBoundsStartLine
					+ ", endLine=" + outOfBoundsEndLine + ", numberOfLines=" + numberOfLines);

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
                        log.warning(AI_CHAT, () -> "Error getting line length: " + e.getMessage());
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
                log.warning(AI_CHAT, () -> "Error getting line length for column bounds check: " + e.getMessage());
            }

			// Select whole lines (columns are ignored): from the start of startLine to the end of the
			// text on endLine (excluding the trailing line delimiter).
			var endLineInfo = document.getLineInformation(endLine);
			final var startOffset = document.getLineOffset(startLine);
			final var endOffset = Math.min(endLineInfo.getOffset() + endLineInfo.getLength(), document.getLength());

			var length = endOffset - startOffset;

			// If length is negative, swap start and end
			final var finalStartOffset = length >= 0 ? startOffset : endOffset;
			final var finalLength = Math.abs(length);

			final var documentLength = document.getLength();
			log.trace(TracingSources.CHAT, AI_CHAT, () -> "Setting selection: startOffset=" + finalStartOffset
				+ ", length=" + finalLength + ", documentLength=" + documentLength);

			// Set selection and scroll it into view (selectAndReveal / revealRange).
			if (finalLength >= 0)
			{
				if (editor instanceof ITextEditor)
				{
					((ITextEditor)editor).selectAndReveal(finalStartOffset, finalLength);
				}
				else if (sourceViewer instanceof org.eclipse.jface.text.source.SourceViewer)
				{
					var viewer = (org.eclipse.jface.text.source.SourceViewer)sourceViewer;
					viewer.setSelectedRange(finalStartOffset, finalLength);
					viewer.revealRange(finalStartOffset, finalLength);
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
