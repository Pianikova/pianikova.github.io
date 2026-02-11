/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ILinkProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Implementation of EDT link handler for edt-file:// protocol
 */
public class EdtLinkHandler implements IEdtLinkHandler
{
	private final IUI ui;
	private final ILog log;
	private final ILinkProvider linkProvider;

	@Inject
	public EdtLinkHandler(IUI ui, ILog log, ILinkProvider linkProvider)
	{
		Preconditions.checkNotNull(ui);
		Preconditions.checkNotNull(log);
		Preconditions.checkNotNull(linkProvider);
		this.ui = ui;
		this.log = log;
		this.linkProvider = linkProvider;
	}

    @SuppressWarnings("nls")
    @Override
	public String formatInsertCodePath(AIContext ctx, String path)
	{
		if (ctx == null || path == null || path.isBlank())
		{
			return path;
		}

		var normalizedPath = path.replace('\\', '/');

		var document = ctx.getDocument();
		if (document == null)
		{
			return linkProvider.file(normalizedPath);
		}

		var hasSelection = !ctx.getText().equals(ctx.getSource());
		try
		{
			if (hasSelection)
			{
				var selectionStart = ctx.getSourceOffset() - ctx.getTextOffset();
				var selectionFinish = selectionStart + ctx.getText().length();
				var startPosition = getPosition(document, selectionStart);
				var endPosition = getPosition(document, selectionFinish);
				return linkProvider.file(normalizedPath, startPosition.line, startPosition.column,
					endPosition.line, endPosition.column);
			}

			var caretPosition = getPosition(document, ctx.getSourceOffset());
			return linkProvider.file(normalizedPath, caretPosition.line, caretPosition.column);
		}
		catch (BadLocationException error)
		{
			log.logError(error);
		}

		return linkProvider.file(normalizedPath);
	}

    @SuppressWarnings("nls")
    @Override
	public String getFullPathForInsertCode(AIContext ctx)
	{
		if (ctx == null)
		{
			return "";
		}

		var path = ctx.getPath();
		if (path == null || path.isBlank())
		{
			return "";
		}

		if (path.matches("^[A-Za-z]:[/\\\\].*") || path.startsWith("/") || path.startsWith("\\\\"))
		{
			return path;
		}

		return ui.getLastSourceViewer()
			.flatMap(sourceViewer -> ui.getFile(sourceViewer))
			.map(file -> file.getLocation().toPortableString())
			.orElse("");
	}

    @SuppressWarnings("nls")
    @Override
	public String extractFilePath(String href)
	{
		if (!isRecognizedHref(href))
		{
			return "";
		}

		var filePath = href.substring(linkProvider.getFileProtocol().length());

        // Remove special postfix
		var colonIndex = filePath.indexOf(ILinkProvider.POSTFIX_SEPARATOR);
		if (colonIndex > 0)
		{
			filePath = filePath.substring(0, colonIndex);
		}

		filePath = filePath.replace(ILinkProvider.COLON_ESCAPE, ILinkProvider.COLON_CHAR);

		return filePath;
	}

	@Override
	public boolean isRecognizedHref(String href)
	{
		if (href == null || href.isBlank())
		{
			return false;
		}

		return href.startsWith(linkProvider.getFileProtocol());
	}

	@SuppressWarnings("nls")
	@Override
	public Optional<CursorPositionInfo> extractCursorPosition(String href)
	{
		if (!isRecognizedHref(href))
		{
			return Optional.empty();
		}

		var filePath = href.substring(linkProvider.getFileProtocol().length());
		var colonIndex = filePath.indexOf(ILinkProvider.POSTFIX_SEPARATOR);
		if (colonIndex < 0)
		{
			return Optional.empty();
		}

		var positionPart = filePath.substring(colonIndex + 1);
		String[] parts = positionPart.split("\\" + ILinkProvider.POSITION_SEPARATOR);

		if (parts.length < 2)
		{
			return Optional.empty();
		}

		try
		{
			int line = Integer.parseInt(parts[0]);
			int column = Integer.parseInt(parts[1]);
			return Optional.of(new CursorPositionInfo(line, column));
		}
		catch (NumberFormatException e)
		{
			return Optional.empty();
		}
	}

	@SuppressWarnings("nls")
	@Override
	public Optional<SelectionInfo> extractSelection(String href)
	{
		if (!isRecognizedHref(href))
		{
			return Optional.empty();
		}

		var filePath = href.substring(linkProvider.getFileProtocol().length());
		var colonIndex = filePath.indexOf(ILinkProvider.POSTFIX_SEPARATOR);
		if (colonIndex < 0)
		{
			return Optional.empty();
		}

		var positionPart = filePath.substring(colonIndex + 1);
		String[] parts = positionPart.split("\\" + ILinkProvider.POSITION_SEPARATOR);

		if (parts.length < 4)
		{
			return Optional.empty();
		}

		try
		{
			int startLine = Integer.parseInt(parts[0]);
			int startColumn = Integer.parseInt(parts[1]);
			int endLine = Integer.parseInt(parts[2]);
			int endColumn = Integer.parseInt(parts[3]);
            if (startLine > endLine)
            {
                return Optional.empty();
            }

            if (startLine == endLine)
            {
                if (startColumn > endColumn)
                {
                    return Optional.empty();
                }
            }

			return Optional.of(new SelectionInfo(startLine, startColumn, endLine, endColumn));
		}
		catch (NumberFormatException e)
		{
			return Optional.empty();
		}
	}

	private Position getPosition(IDocument document, int offset) throws BadLocationException
	{
		var safeOffset = Math.max(0, Math.min(offset, document.getLength()));
		var line = document.getLineOfOffset(safeOffset);
		var lineOffset = document.getLineOffset(line);
		return new Position(line + 1, safeOffset - lineOffset + 1);
	}

	private static class Position
	{
		private final int line;
		private final int column;

		private Position(int line, int column)
		{
			this.line = line;
			this.column = column;
		}
	}
}
