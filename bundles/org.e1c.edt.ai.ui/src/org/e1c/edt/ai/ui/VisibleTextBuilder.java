/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class VisibleTextBuilder
{
    private static final char LINE_FEED_SIGN = '\u00b6';

    public static String build(String text, String prefix)
    {
        var lines = text.split("\n"); //$NON-NLS-1$
        StringBuilder visibleChar = new StringBuilder(text.length());
        for (var lineIndex = 0; lineIndex < lines.length; lineIndex++)
        {
            var line = lines[lineIndex];
            if (lineIndex > 0 && !prefix.isEmpty() && line.startsWith(prefix))
            {
                line = line.substring(prefix.length());
            }

            for (var i = 0; i < line.length(); i++)
            {
                var ch = line.charAt(i);
                switch (ch)
                {
                case ' ':
                case '\u3000': // ideographic whitespace
                    visibleChar.append(' ');
                    break;

                case '\t':
                    var tabWidth = EditorsUI.getPreferenceStore()
                        .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
                    for (int tab = 0; tab < tabWidth; tab++)
                    {
                        visibleChar.append(' ');
                    }

                    break;

                case '\r':
                case '\n':
                    break;

                default:
                    visibleChar.append(ch);
                    break;
                }
            }

            if (lineIndex == lines.length - 1)
            {
                visibleChar.append(LINE_FEED_SIGN);
            }
            else
            {
                visibleChar.append('\n');
            }
        }

        return visibleChar.toString();
    }
}
