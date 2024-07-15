/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ICodeCompletionContext
{
    boolean isSingleWordMode();

    void replace(int start, int replaceLength, String text);
}
