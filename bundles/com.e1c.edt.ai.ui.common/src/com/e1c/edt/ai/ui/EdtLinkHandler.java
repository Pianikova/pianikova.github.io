/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Implementation of EDT link handler for edt-file:// protocol
 */
public class EdtLinkHandler implements IEdtLinkHandler
{
	private static final String EDT_FILE_PROTOCOL = "edt-file://"; //$NON-NLS-1$
    private static final String POSTFIX_SEPARATOR = ":"; //$NON-NLS-1$
	private static final String POSITION_SEPARATOR = ":"; //$NON-NLS-1$
	private static final String COLON_ESCAPE = "%3A"; //$NON-NLS-1$
	private static final String COLON_CHAR = ":"; //$NON-NLS-1$

	private final IUI ui;
	private final ILog log;

	@Inject
	public EdtLinkHandler(IUI ui, ILog log)
	{
		Preconditions.checkNotNull(ui);
		Preconditions.checkNotNull(log);
		this.ui = ui;
		this.log = log;
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
		// Escape colons in the path to avoid conflicts with position separator
		normalizedPath = normalizedPath.replace(COLON_CHAR, COLON_ESCAPE);

		var document = ctx.getDocument();
		if (document == null)
		{
			return EDT_FILE_PROTOCOL + normalizedPath;
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
				return String.format("%s%s%s%d%s%d%s%d%s%d", EDT_FILE_PROTOCOL, normalizedPath,
					POSTFIX_SEPARATOR, startPosition.line, POSITION_SEPARATOR, startPosition.column,
					POSITION_SEPARATOR, endPosition.line, POSITION_SEPARATOR, endPosition.column);
			}

			var caretPosition = getPosition(document, ctx.getSourceOffset());
			return String.format("%s%s%s%d%s%d", EDT_FILE_PROTOCOL, normalizedPath,
				POSTFIX_SEPARATOR, caretPosition.line, POSITION_SEPARATOR, caretPosition.column);
		}
		catch (BadLocationException error)
		{
			log.logError(error);
		}

		return EDT_FILE_PROTOCOL + normalizedPath;
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
		if (!href.startsWith(EDT_FILE_PROTOCOL))
		{
			return "";
		}

		var filePath = href.substring(EDT_FILE_PROTOCOL.length());

        // Remove special postfix
		var colonIndex = filePath.indexOf(POSTFIX_SEPARATOR);
		if (colonIndex > 0)
		{
			filePath = filePath.substring(0, colonIndex);
		}

		filePath = filePath.replace(COLON_ESCAPE, COLON_CHAR);

		return filePath;
	}

	@Override
	public boolean isRecognizedHref(String href)
	{
		if (href == null || href.isBlank())
		{
			return false;
		}

		return href.startsWith(EDT_FILE_PROTOCOL);
	}

	@SuppressWarnings("nls")
	@Override
	public Optional<CursorPositionInfo> extractCursorPosition(String href)
	{
		if (!isRecognizedHref(href))
		{
			return Optional.empty();
		}

		var filePath = href.substring(EDT_FILE_PROTOCOL.length());
		var colonIndex = filePath.indexOf(POSTFIX_SEPARATOR);
		if (colonIndex < 0)
		{
			return Optional.empty();
		}

		var positionPart = filePath.substring(colonIndex + 1);
		String[] parts = positionPart.split("\\" + POSITION_SEPARATOR);

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

		var filePath = href.substring(EDT_FILE_PROTOCOL.length());
		var colonIndex = filePath.indexOf(POSTFIX_SEPARATOR);
		if (colonIndex < 0)
		{
			return Optional.empty();
		}

		var positionPart = filePath.substring(colonIndex + 1);
		String[] parts = positionPart.split("\\" + POSITION_SEPARATOR);

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
