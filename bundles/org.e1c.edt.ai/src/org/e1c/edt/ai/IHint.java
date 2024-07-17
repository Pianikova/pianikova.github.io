/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IHint
{
    void append(Text text);

    boolean isEmpty();

    boolean isBlank();

    boolean startsWith(char ch);

    Text getText(HintPart part);

    void clear();
}
