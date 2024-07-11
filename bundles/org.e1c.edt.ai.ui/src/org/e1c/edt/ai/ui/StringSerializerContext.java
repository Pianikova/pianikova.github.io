/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.nodemodel.ILeafNode;

import com.google.common.base.Preconditions;

public class StringSerializerContext
{
    private final StringBuilder text = new StringBuilder();
    private final int cursorNodeOffset;
    private int offset;
    private final int maxLength;
    private boolean achiveCursor;

    public StringSerializerContext(ILeafNode cursorNode, int offset, int maxLength)
    {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(maxLength > 0);
        this.maxLength = maxLength;
        if (cursorNode != null)
        {
            cursorNodeOffset = cursorNode.getTotalOffset();
            this.offset = offset - cursorNodeOffset;
        }
        else
        {
            cursorNodeOffset = 0;
            this.offset = 0;
        }
    }

    public boolean serialize(ILeafNode node)
    {
        if (!achiveCursor && cursorNodeOffset == node.getTotalOffset())
        {
            offset += text.length();
            achiveCursor = true;
        }

        text.append(node.getText());
        return text.length() <= maxLength;
    }

    public String getText()
    {
        return text.toString();
    }

    public int getOffset()
    {
        return achiveCursor ? offset : text.length();
    }
}
