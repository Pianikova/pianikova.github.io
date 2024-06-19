/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ICodeCompletionContext
{
    void replace(int start, int replaceLength, String text);
}
