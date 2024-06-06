/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class HintTextBuilder implements IHintTextBuilder
{
    @Override
    public String build(String text, int tabWidth, char lineFeedSing)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(tabWidth > 0);
        StringBuilder visibleChar = new StringBuilder(text.length());
        var hasLine = false;
        for (var ch : text.toCharArray())
        {
            switch (ch)
            {
            case ' ':
            case '\u3000': // ideographic whitespace
                visibleChar.append(' ');
                break;

            case '\t':
                for (int tab = 0; tab < tabWidth; tab++)
                {
                    visibleChar.append(' ');
                }

                break;

            case '\r':
                break;

            case '\n':
                hasLine = true;
                visibleChar.append(ch);
                break;

            default:
                visibleChar.append(ch);
                break;
            }
        }

        if (text.isEmpty() || hasLine)
        {
            visibleChar.append(lineFeedSing);
        }

        return visibleChar.toString();
    }
}
