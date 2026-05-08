/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

public interface IWildcardMatcher
{
    boolean matches(String pattern, String value);

    boolean hasWildcard(String pattern);
}
