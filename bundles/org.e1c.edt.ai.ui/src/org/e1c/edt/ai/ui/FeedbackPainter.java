/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class FeedbackPainter
    implements IFeedbackPainter
{
    private static final int OUT_BOUNDS_SIZE = 8;
    private static final int INNER_BOUNDS_SIZE = 2;
    private static final String REQUEST_EVALUATION =
        "Пожалуйста оцените работу AI\n• доработайте код до приемлемого качества\n• нажмите кливишу Ctrl от 1 до 5 раз\n  для оценки работы AI, где 5 - отлично"; //$NON-NLS-1$
    private static final char EMPTY_STAR = '☆';
    private static final char FILLED_STAR = '★';
    private Integer stars;

    @Override
    public void addStar()
    {
        if (stars == null)
        {
            stars = 0;
        }

        stars++;

        if (stars > 5)
        {
            stars = null;
        }
    }

    @Override
    public void paintControl(PaintEvent e)
    {
        var gc = e.gc;
        if (!(e.widget instanceof StyledText))
        {
            return;
        }

        var textWidget = (StyledText)e.widget;
        gc.setAdvanced(true);
        gc.setBackground(textWidget.getBackground());
        gc.setForeground(textWidget.getForeground());
        paintRequest(gc, stars);
    }

    private void paintRequest(GC gc, Integer stars)
    {
        var bounds = gc.getClipping();
        var sb = new StringBuilder();
        if (stars != null)
        {
            if (stars < 0)
            {
                stars = 0;
            }

            if (stars > 5)
            {
                stars = 5;
            }

            var i = 0;
            for (; i < stars; i++)
            {
                sb.append(FILLED_STAR);
            }

            for (; i < 5; i++)
            {
                sb.append(EMPTY_STAR);
            }
        }

        var starsText = sb.toString();
        var textSize = gc.textExtent(REQUEST_EVALUATION);
        var starsSize = gc.textExtent(starsText);

        var x0 = bounds.width - textSize.x - OUT_BOUNDS_SIZE;
        var y0 = bounds.height - textSize.y - starsSize.y - OUT_BOUNDS_SIZE;
        gc.setAlpha(180);
        var rect =
            new Rectangle(x0, y0, textSize.x + INNER_BOUNDS_SIZE * 2, textSize.y + starsSize.y + INNER_BOUNDS_SIZE * 3);
        gc.fillRectangle(rect);
        gc.drawText(REQUEST_EVALUATION, rect.x + INNER_BOUNDS_SIZE, rect.y + INNER_BOUNDS_SIZE);
        var dif = (textSize.x - starsSize.x) / 2;
        gc.drawText(starsText, rect.x + dif + INNER_BOUNDS_SIZE,
            rect.y + INNER_BOUNDS_SIZE + textSize.y + INNER_BOUNDS_SIZE);
        gc.drawRectangle(rect);
    }
}
