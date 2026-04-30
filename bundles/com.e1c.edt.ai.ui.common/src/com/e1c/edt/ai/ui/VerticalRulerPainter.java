/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.internal.DPIUtil;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.OS;
import com.e1c.edt.ai.Range;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
class VerticalRulerPainter
    implements IVerticalRulerPainter
{
    private final IGCTools gcTools;
    private final IEnvironment environment;
    StyledText textWidget;
    String hintText;
    private Range range = Range.EMPTY;

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
    }

    @SuppressWarnings("nls")
    @Override
    public synchronized void updateRange()
    {
        if (textWidget == null || hintText == null || environment.getOS() != OS.WINDOWS || hintText.isEmpty())
        {
            range = Range.EMPTY;
            return;
        }

        var lines = hintText.split("\n");
        var linesCount = lines.length;
        if (linesCount < 2)
        {
            range = Range.EMPTY;
            return;
        }

        var hintOffset = textWidget.getCaretOffset();
        var y = textWidget.getLocationAtOffset(hintOffset).y + textWidget.getLineHeight();
        var h = (textWidget.getLineHeight() + 1) * (linesCount - 1);
        if (h <= 0)
        {
            range = Range.EMPTY;
            return;
        }

        range = new Range(y, (int)(h * (DPIUtil.getDeviceZoom() >= 200 ? .9 : 1.0)));
    }

    @SuppressWarnings("nls")
    @Override
    public synchronized void reset()
    {
        hintText = "";
        range = Range.EMPTY;
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
        // gc.drawRectangle(bounds.x, y, bounds.width, h);
        gc.setAlpha(200);
        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
