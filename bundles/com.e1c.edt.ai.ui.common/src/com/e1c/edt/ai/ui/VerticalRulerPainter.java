/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;

import com.e1c.edt.ai.Range;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class VerticalRulerPainter
    implements IVerticalRulerPainter
{
    private final IGCTools gcTools;
    private Range pixelRange = Range.EMPTY;

    @Inject
    public VerticalRulerPainter(IGCTools gcTools)
    {
        Preconditions.checkNotNull(gcTools);
        this.gcTools = gcTools;
    }

    @Override
    public void pin(StyledText textWidget, String hintText)
    {
        if (hintText == null || hintText.isEmpty())
        {
            pixelRange = Range.EMPTY;
            return;
        }

        var lineCount = 0;
        var currentOffset = 0;
        while (currentOffset < hintText.length())
        {
            int nextLineOffset = hintText.indexOf('\n', currentOffset);
            if (nextLineOffset == -1)
            {
                lineCount++;
                break;
            }

            lineCount++;
            currentOffset = nextLineOffset + 1;
        }

        if (lineCount < 2)
        {
            pixelRange = Range.EMPTY;
            return;
        }

        var hintOffset = textWidget.getCaretOffset();
        var y = textWidget.getLocationAtOffset(hintOffset).y + textWidget.getLineHeight();
        var h = (textWidget.getLineHeight() - 1) * (lineCount - 1);
        pixelRange = new Range(y, h);
    }

    @Override
    public void reset()
    {
        this.pixelRange = Range.EMPTY;
    }

    @Override
    public void paintControl(PaintEvent e)
    {
        if (pixelRange == Range.EMPTY)
        {
            return;
        }

        var gc = e.gc;
        if (gc.isDisposed())
        {
            return;
        }

        var bounds = gc.getClipping();
        var y = pixelRange.getStart();
        var h = pixelRange.getLength();
        if (bounds.height > y)
        {
            gcTools.copyArea(gc, 0, y, bounds.width, bounds.height - y, 0, y + h);
        }

        gc.fillRectangle(0, y, bounds.width, h);
    }
}
