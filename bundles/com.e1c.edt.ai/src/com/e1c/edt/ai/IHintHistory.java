/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public interface IHintHistory
{
    boolean isEmpty();

    void clear();

    void push(Text text);

    Text pull();
}