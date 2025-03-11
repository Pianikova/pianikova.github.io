/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class HintTextBuilder
    implements IHintTextBuilder
{
    @Override
    public String build(String linePrefix, String text, int tabWidth)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(tabWidth > 0);
        StringBuilder visibleChar = new StringBuilder(text.length());
        int tabCharCounter = 0;
        for (var ch : linePrefix.toCharArray())
        {
            if (ch != '\t')
            {
                tabCharCounter++;
            }
        }

        for (var ch : text.toCharArray())
        {
            switch (ch)
            {
            case ' ':
            case '\u3000': // ideographic whitespace
                visibleChar.append(' ');
                tabCharCounter++;
                break;

            case '\t':
                var cnt = tabWidth - tabCharCounter % tabWidth;
                for (int tab = 0; tab < cnt; tab++)
                {
                    visibleChar.append(' ');
                }

                tabCharCounter = 0;
                break;

            case '\r':
                tabCharCounter = 0;
                break;

            case '\n':
                visibleChar.append(ch);
                tabCharCounter = 0;
                break;

            default:
                visibleChar.append(ch);
                tabCharCounter++;
                break;
            }

        }

        return visibleChar.toString();
    }
}
