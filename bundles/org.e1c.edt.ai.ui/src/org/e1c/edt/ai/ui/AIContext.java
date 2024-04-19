/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

public class AIContext
{
    private final String input;
    private final int cursorOffset;

    public AIContext(String input, int cursorOffset)
    {
        this.input = input;
        this.cursorOffset = cursorOffset;
    }

    public String getInput()
    {
        return input;
    }

    public int getCursorOffset()
    {
        return cursorOffset;
    }
}
