/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ILinePrefixMatcher
{
    int getPrefixLength(String line, String prefix, int tabWidth);
}
