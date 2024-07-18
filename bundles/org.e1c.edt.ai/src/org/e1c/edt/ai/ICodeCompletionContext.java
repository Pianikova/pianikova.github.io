/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ICodeCompletionContext
{
    void apply(Text text, int offset);

    void rollback(int offset, int length);

    void commit();
}
