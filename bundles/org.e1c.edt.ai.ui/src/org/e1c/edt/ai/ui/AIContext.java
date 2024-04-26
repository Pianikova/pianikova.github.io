/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

public class AIContext
{
    private int cursorOffset;
    private String text;
    private String context;

    public AIContext(int cursorOffset, String text, String context)
    {
        this.cursorOffset = cursorOffset;
        this.text = text;
        this.context = context;
    }

    public int getCursorOffset()
    {
        return cursorOffset;
    }

    public String getText()
    {
        return text;
    }

    public String getContext()
    {
        return context;
    }
}