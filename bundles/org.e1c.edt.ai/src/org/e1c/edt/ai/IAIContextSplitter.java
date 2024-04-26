/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IAIContextSplitter
{
    AIContextParts split(String text, int offset, int maxLength);
}
