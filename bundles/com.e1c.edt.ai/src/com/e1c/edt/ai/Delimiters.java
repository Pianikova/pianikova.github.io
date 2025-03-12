/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.HashSet;

public class Delimiters
{
    private static final HashSet<Character> TextDelimiters;

    static
    {
        TextDelimiters = new HashSet<>();
        TextDelimiters.add(' ');
        TextDelimiters.add('|');
        TextDelimiters.add('~');
        TextDelimiters.add(':');
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
        return isLineDelimiter(ch) || TextDelimiters.contains(ch);
    }
}
