/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public class LinePrefixMatcher
    implements ILinePrefixMatcher
{
    @Override
    public int getPrefixLength(String line, String prefix, int tabWidth)
    {
        var prefixSize = 0;
        var pos = 0;
        while (pos < prefix.length())
        {
            var ch = prefix.charAt(pos++);
            if (ch == '\t')
            {
                prefixSize += tabWidth;
                continue;
            }

            if (Character.isWhitespace(ch))
            {
                prefixSize++;
            }
        }

        pos = 0;
        var linePrefixSize = 0;
        while (pos < line.length() && linePrefixSize < prefixSize)
        {
            var ch = line.charAt(pos);
            if (ch == '\t')
            {
                linePrefixSize += tabWidth;
                pos++;
                continue;
            }

            if (Character.isWhitespace(ch))
            {
                linePrefixSize++;
            }

            pos++;
        }

        if (linePrefixSize == prefixSize)
        {
            return pos;
        }

        return 0;
    }
}
