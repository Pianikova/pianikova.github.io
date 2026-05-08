/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.regex.Pattern;

import com.google.inject.Singleton;

@Singleton
public class WildcardMatcher
    implements IWildcardMatcher
{
    @Override
    public boolean matches(String pattern, String value)
    {
        if (pattern == null || value == null)
        {
            return false;
        }
        if (!hasWildcard(pattern))
        {
            return pattern.equals(value);
        }

        var regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++)
        {
            var ch = pattern.charAt(i);
            if (ch == '*')
            {
                regex.append(".*"); //$NON-NLS-1$
            }
            else
            {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return value.matches(regex.toString());
    }

    @Override
    public boolean hasWildcard(String pattern)
    {
        return pattern != null && pattern.indexOf('*') >= 0;
    }
}
