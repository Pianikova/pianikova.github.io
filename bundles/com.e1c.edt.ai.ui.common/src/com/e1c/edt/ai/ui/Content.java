/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

public class Content
{
    public final String text;
    public final int offset;
    public final String selectionText;
    public final int selectionOffset;

    public Content(String text, int offset, String selectionText, int selectionOffset)
    {
        this.text = text;
        this.offset = offset;
        this.selectionText = selectionText;
        this.selectionOffset = selectionOffset;
    }
}
