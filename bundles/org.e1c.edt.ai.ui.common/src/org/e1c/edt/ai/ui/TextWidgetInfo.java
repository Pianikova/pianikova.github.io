/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.swt.custom.StyledText;


class TextWidgetInfo
    implements ITextWidgetInfoUpdater, ITextWidgetInfoProvider
{
    private StyledText textWidget;
    private int lastMouseOffset;

    @Override
    public void setLastMouseOffset(StyledText textWidget, int offset)
    {
        this.textWidget = textWidget;
        this.lastMouseOffset = offset;
    }

    @Override
    public void reset()
    {
        this.textWidget = null;
        this.lastMouseOffset = 0;
    }

    @Override
    public Optional<Integer> getLastMouseOffset(StyledText textWidget)
    {
        if (textWidget == this.textWidget)
        {
            return Optional.of(lastMouseOffset);
        }

        return Optional.empty();
    }
}
