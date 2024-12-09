/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.HashSet;

class Delimiters
{
    private static final HashSet<Character> TextDelimiters;

    static
    {
        TextDelimiters = new HashSet<>();
        TextDelimiters.add(' ');
        TextDelimiters.add('|');
        TextDelimiters.add('~');
        TextDelimiters.add(':');
        TextDelimiters.add(';');
        TextDelimiters.add('(');
        TextDelimiters.add(')');
        TextDelimiters.add('[');
        TextDelimiters.add(']');
        TextDelimiters.add(',');
        TextDelimiters.add('"');
        TextDelimiters.add('\'');
        TextDelimiters.add('.');
        TextDelimiters.add('+');
        TextDelimiters.add('-');
        TextDelimiters.add('*');
        TextDelimiters.add('/');
        TextDelimiters.add('>');
        TextDelimiters.add('<');
        TextDelimiters.add('=');
    }

    public static Boolean isLineDelimiter(char ch)
    {
        return (ch == '\n') || (ch == '\r');
    }

    public static Boolean isTokenDelimiter(char ch)
    {
        return TextDelimiters.contains(ch);
    }
}
