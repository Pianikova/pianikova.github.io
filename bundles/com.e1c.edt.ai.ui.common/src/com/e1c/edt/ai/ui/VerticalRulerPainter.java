/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.OS;
import com.e1c.edt.ai.Range;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class VerticalRulerPainter
    implements IVerticalRulerPainter
{
    private final IGCTools gcTools;
    private final IEnvironment environment;
    StyledText textWidget;
    String hintText;
    private Range range = Range.EMPTY;
    private int lineCount;

    @Inject
    public VerticalRulerPainter(IGCTools gcTools, IEnvironment environment)
    {
        Preconditions.checkNotNull(gcTools);
        Preconditions.checkNotNull(environment);
        this.gcTools = gcTools;
        this.environment = environment;
    }

    @Override
    public synchronized void pin(StyledText textWidget, String hintText)
    {
        this.textWidget = textWidget;
        this.hintText = hintText;
        updateRange();

        if (range == Range.EMPTY)
        {
            return;
        }

        // scroll if hint is out of view. 1 line can be hidden under the side scrollbar
        var y = range.getStart();
        var h = range.getLength();
        var bounds = textWidget.getBounds();
        if (y + h >= bounds.height)
        {
            textWidget.setTopIndex(textWidget.getTopIndex() + lineCount + 1);
        }
    }

    @Override
    public synchronized void updateRange()
    {
        if (textWidget == null || hintText == null || environment.getOS() != OS.WINDOWS || hintText.isEmpty())
        {
            range = Range.EMPTY;
            return;
        }

        lineCount = 0;
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
            range = Range.EMPTY;
            return;
        }

        var hintOffset = textWidget.getCaretOffset();
        var y = textWidget.getLocationAtOffset(hintOffset).y + textWidget.getLineHeight();
        var h = (textWidget.getLineHeight() - 1) * (lineCount - 1);
        if (h <= 0)
        {
            range = Range.EMPTY;
            return;
        }

        range = new Range(y, h);
    }

    @Override
    public synchronized void reset()
    {
        this.range = Range.EMPTY;
    }

    @Override
    public synchronized void paintControl(PaintEvent e)
    {
        if (range == Range.EMPTY)
        {
            return;
        }

        var gc = e.gc;
        if (gc.isDisposed())
        {
            return;
        }

        var bounds = gc.getClipping();
        if (bounds == null)
        {
            return;
        }

        var y = range.getStart();
        var h = range.getLength();
        if (y >= 0)
        {
            gcTools.copyArea(gc, bounds.x, y, bounds.width, bounds.height - y - h, bounds.x, y + h);
        }

        gc.fillRectangle(bounds.x, y, bounds.width, h);
    }
}
