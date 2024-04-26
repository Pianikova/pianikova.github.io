/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class VisibleTextBuilder
{
    private static final char SPACE_SIGN = ' ';
    private static final char TAB_SIGN = '\u00bb';
    private static final char LINE_FEED_SIGN = '\u21B5';

    public static String build(String text)
    {
        var tabWidth =
            EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        StringBuilder visibleChar = new StringBuilder(text.length());
        for (var i = 0; i < text.length(); i++)
        {
            var ch = text.charAt(i);
            switch (ch)
            {
            case ' ':
            case '\u3000': // ideographic whitespace
                visibleChar.append(SPACE_SIGN);
                break;

            case '\t':
                for (int tab = 0; tab < tabWidth; tab++)
                {
                    char tabChar;
                    if (tab == 0 || tab == tabWidth - 1)
                    {
                        tabChar = TAB_SIGN;
                    }
                    else
                    {
                        tabChar = SPACE_SIGN;
                    }

                    visibleChar.append(tabChar);
                }

                break;

            case '\r':
                break;

            case '\n':
                visibleChar.append(LINE_FEED_SIGN);
                break;

            default:
                visibleChar.append(ch);
                break;
            }
        }

        return visibleChar.toString();
    }
}
