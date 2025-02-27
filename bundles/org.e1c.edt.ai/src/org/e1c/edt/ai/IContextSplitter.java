/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

public interface IContextSplitter
{
    ContextParts split(String text, int offset);
}
