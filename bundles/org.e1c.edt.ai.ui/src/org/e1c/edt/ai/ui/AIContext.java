/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

public class AIContext
{
    private int cursorOffset;
    private String text;
    private String prefix;
    private String postfix;

    public AIContext(int cursorOffset, String text, String prefix, String postfix)
    {
        this.cursorOffset = cursorOffset;
        this.text = text;
        this.prefix = prefix;
        this.postfix = postfix;
    }

    public int getCursorOffset()
    {
        return cursorOffset;
    }

    public String getText()
    {
        return text;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String getPostfix()
    {
        return postfix;
    }
}
