/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.inject.Inject;

public class HintTextBuilder implements IHintTextBuilder
{
    private final ILinePrefixMatcher linePrefixMatcher;

    @Inject
    public HintTextBuilder(ILinePrefixMatcher linePrefixMatcher)
    {
        this.linePrefixMatcher = linePrefixMatcher;
    }

    @Override
    public String build(String text, String prefix, int tabWidth, char lineFeedSing)
    {
        var lines = text.split("\n", -1); //$NON-NLS-1$
        StringBuilder visibleChar = new StringBuilder(text.length());
        for (var lineIndex = 0; lineIndex < lines.length; lineIndex++)
        {
            var line = lines[lineIndex];
            var prefixLength = linePrefixMatcher.getPrefixLength(line, prefix, tabWidth);
            if (lineIndex > 0 && prefixLength > 0)
            {
                line = line.substring(prefixLength);
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
                visibleChar.append(lineFeedSing);
            }
            else
            {
                visibleChar.append('\n');
            }
        }

        return visibleChar.toString();
    }
}
