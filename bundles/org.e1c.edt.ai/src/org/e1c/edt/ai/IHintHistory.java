/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IHintHistory
{
    boolean isEmpty();

    void clear();

    void push(String text);

    String pull();
}