/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.HashMap;

@SuppressWarnings("nls")
class JavaScript implements IJavaScript
{
    private static HashMap<Character, String> SpecialChars = new HashMap<>();

    static
    {
        SpecialChars.put(' ', " ");
        SpecialChars.put('\r', "\\r");
        SpecialChars.put('\n', "\\n");
        SpecialChars.put('\t', "\\t");
        SpecialChars.put('^', "\\^");
        SpecialChars.put('$', "\\$");
        SpecialChars.put('\\', "\\\\");
        SpecialChars.put('.', "\\.");
        SpecialChars.put('*', "\\*");
        SpecialChars.put('+', "\\+");
        SpecialChars.put('?', "\\?");
        SpecialChars.put('(', "\\(");
        SpecialChars.put(')', "\\)");
        SpecialChars.put('[', "\\[");
        SpecialChars.put(']', "\\]");
        SpecialChars.put('{', "\\{");
        SpecialChars.put('}', "\\}");
        SpecialChars.put('|', "\\|");
        SpecialChars.put('/', "\\/");
    }

    @Override
    public String escape(String text)
    {
        if (text == null || text.isBlank())
        {
            return text;
        }

        var str = new StringBuilder();
        for (var ch : text.chars().toArray())
        {
            if (Character.isLetterOrDigit(ch))
            {
                str.append((char)ch);
            }
            else
            {
                var chText = SpecialChars.get((char)ch);
                if (chText != null)
                {
                    str.append(chText);
                }
                else
                {
                    str.append(String.format("\\u%04X", ch));
                }
            }
        }

        return str.toString();
    }
}
