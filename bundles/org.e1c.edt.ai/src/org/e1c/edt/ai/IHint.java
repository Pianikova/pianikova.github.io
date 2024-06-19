/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IHint
{
    void append(String text);

    boolean isEmpty();

    boolean isBlank();

    boolean startsWith(char ch);

    String getText(HintPart part);

    void clear();
}
