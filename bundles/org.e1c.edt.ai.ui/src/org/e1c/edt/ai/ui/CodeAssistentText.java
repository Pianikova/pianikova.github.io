/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ISettingsProvider;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class CodeAssistentText implements ICodeAssistentText
{
    private ISettingsProvider settingsProvider;

    public CodeAssistentText(ISettingsProvider settingsProvider)
    {
        this.settingsProvider = settingsProvider;
    }

    @Override
    public String get(IDocument document, int cursorOffset)
    {
        cursorOffset = normalizeCursorOffset(document, cursorOffset);
        var start = cursorOffset - settingsProvider.getSettings().getMaxAssistantTextSize();
        if (start < 0)
        {
            start = 0;
        }

        try
        {
            var text = document.get(start, cursorOffset - start);
            if (start > 0)
            {
                var linePosition = text.indexOf(System.lineSeparator());
                if (linePosition >= 0 && linePosition < text.length() - 1)
                {
                    var textPart = text.substring(linePosition);
                    if (!textPart.isBlank())
                    {
                        text = textPart;
                    }
                }
            }

            return text;
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
        }

        return ""; //$NON-NLS-1$
    }


    @Override
    public void set(IDocument document, int cursorOffset, String text)
    {
        try
        {
            cursorOffset = normalizeCursorOffset(document, cursorOffset);
            document.replace(cursorOffset, 0, text + System.lineSeparator());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
        }
    }

    private int normalizeCursorOffset(IDocument document, int cursorOffset)
    {
        var size = document.getLength();
        if (cursorOffset >= size)
        {
            cursorOffset = size - 1;
        }

        return cursorOffset;
    }
}