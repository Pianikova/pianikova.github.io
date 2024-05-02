/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

public class LinesCounter implements ILinesCounter
{
    private int otherChars;
    private int count;

    @Override
    public int acceptAndGetLinesCount(char ch)
    {
        var isDelimiter = isDelimiter(ch);
        if (!isDelimiter)
        {
            otherChars++;
        }

        if (isDelimiter)
        {
            if (otherChars > 0)
            {
                count++;
                otherChars = 0;
            }
        }

        return count + (otherChars > 0 ? 1 : 0);
    }

    private Boolean isDelimiter(char ch)
    {
        return (ch == '\n') || (ch == '\r');
    }
}