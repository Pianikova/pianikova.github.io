package com.e1c.edt.ai.tools;

import java.util.Arrays;

public class Replacements
    implements IReplacements
{

    @Override
    public String[] splitLines(String text)
    {
        return text.split("\n", -1); //$NON-NLS-1$
    }

    @Override
    public String[] removeTrailingEmptyLine(String[] lines)
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

    @Override
    public String blockByLineRange(String content, String[] lines, int startLine, int endLine)
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
