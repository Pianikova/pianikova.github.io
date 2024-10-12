/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public class TextNormilizer
    implements ITextNormilizer
{
    @Override
    public String normalize(String text)
    {
        var result = new StringBuilder(text.length());
        var retCh = false;
        for (var ch : text.toCharArray())
        {
            switch (ch)
            {
            case '\r':
                if (!retCh)
                {
                    retCh = true;
                }
                else
                {
                    result.append('\r');
                    result.append('\n');
                    retCh = false;
                }

                break;

            case '\n':
                if (retCh)
                {
                    result.append('\r');
                    retCh = false;
                }

                result.append(ch);
                retCh = false;
                break;

            default:
                if (retCh)
                {
                    result.append('\n');
                    retCh = false;
                }

                result.append(ch);
                break;
            }
        }

        if (retCh)
        {
            result.append('\n');
        }

        return result.toString();
    }
}
