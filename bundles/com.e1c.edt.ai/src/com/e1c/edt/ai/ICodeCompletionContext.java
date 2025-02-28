/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public interface ICodeCompletionContext
{
    void apply(Text text, int offset);

    void rollback(int offset, int length);

    void commit(String lastSourceId, int lastOffset);
}
