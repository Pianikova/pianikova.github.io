/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.concurrent.ConcurrentHashMap;

public class HintTextBuilder implements IHintTextBuilder
{
    private final ConcurrentHashMap<Integer, String> tabs = new ConcurrentHashMap<>();

    @Override
    public String build(String text, String prefix, int tabWidth, char lineFeedSing)
    {
        var tab = tabs.computeIfAbsent(tabWidth, this::createTab);
        prefix = prefix.replace("\t", tab); //$NON-NLS-1$
        var lines = text.split("\n"); //$NON-NLS-1$
        StringBuilder visibleChar = new StringBuilder(text.length());
        for (var lineIndex = 0; lineIndex < lines.length; lineIndex++)
        {
            var line = lines[lineIndex].replace("\t", tab); //$NON-NLS-1$
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
                    visibleChar.append(tab);
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

    private String createTab(int tabWith)
    {
        var tabBuilder = new StringBuilder(tabWith);
        for(var i = 0; i < tabWith; i++)
        {
            tabBuilder.append(' ');
        }

        return tabBuilder.toString();
    }
}
