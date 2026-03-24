package com.e1c.edt.ai.tools;

import java.util.Arrays;

final class ReplacementStrategyUtils
{
    private ReplacementStrategyUtils()
    {
    }

    static String[] splitLines(String text)
    {
        return text.split("\n", -1); //$NON-NLS-1$
    }

    static String[] removeTrailingEmptyLine(String[] lines)
    {
        if (lines.length == 0)
        {
            return lines;
        }

        if (lines[lines.length - 1].isEmpty())
        {
            return Arrays.copyOf(lines, lines.length - 1);
        }

        return lines;
    }

    static String blockByLineRange(String content, String[] lines, int startLine, int endLine)
    {
        int startIndex = 0;
        for (int i = 0; i < startLine; i++)
        {
            startIndex += lines[i].length() + 1;
        }

        int endIndex = startIndex;
        for (int i = startLine; i <= endLine; i++)
        {
            endIndex += lines[i].length();
            if (i < endLine)
            {
                endIndex += 1;
            }
        }

        return content.substring(startIndex, endIndex);
    }
}
