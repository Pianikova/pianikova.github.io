/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.ArrayList;

import com.google.common.base.Preconditions;

public class StringNormalizer
    implements IStringNormalizer
{
    @SuppressWarnings("nls")
    @Override
    public String normalize(String text, boolean cleanLines)
    {
        Preconditions.checkNotNull(text);
        text = text.replace("\r", "");
        if (!cleanLines)
        {
            return text;
        }

        var lines = new ArrayList<String>();
        String blankLine = null;
        for (var line : text.split("\n", -1))
        {
           if (line.isBlank())
           {
               blankLine = line;
           }
           else
           {
               if (blankLine != null)
               {
                   lines.add(blankLine);
                   blankLine = null;
               }

               lines.add(line);
           }
        }

        if (blankLine != null)
        {
            lines.add(blankLine);
        }

        return String.join("\n", lines);
    }

}
