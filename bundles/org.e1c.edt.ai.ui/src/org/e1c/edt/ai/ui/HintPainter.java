/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.IHintTextBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class HintPainter
    implements PaintListener, IHintPainter
{
    private static final String labelText = "Tab → ← Esc"; //$NON-NLS-1$
    private static final char LINE_FEED_SIGN = '\u00b6';
    private static final int BORDER = 2;
    private final IHintTextBuilder hintTextBuilder;
    private final IUISettings uiSettings;
    private StyledText textWidget;
    private String hintText = ""; //$NON-NLS-1$
    private int pinnedOffset = -1;

    @Inject
    public HintPainter(IHintTextBuilder hintTextBuilder, IUISettings uiSettings)
    {
        Preconditions.checkNotNull(hintTextBuilder);
        Preconditions.checkNotNull(uiSettings);
        this.hintTextBuilder = hintTextBuilder;
        this.uiSettings = uiSettings;
    }

    @Override
    public void pinOffset(StyledText textWidget, int offset)
    {
        Preconditions.checkNotNull(textWidget);
        this.textWidget = textWidget;
        pinnedOffset = offset;
        if (textWidget == null || textWidget.isDisposed())
        {
            return;
        }

        textWidget.redraw();
    }

    @Override
    public String getHintText()
    {
        return hintText;
    }

    @Override
    public int getOffset()
    {
        return pinnedOffset;
    }

    @Override
    public void reset()
    {
        setHintAt(-1, ""); //$NON-NLS-1$
    }

    @Override
    public void setHintAt(int offset, String hintText)
    {
        var changed = false;
        if (hintText == null || offset == -1)
        {
            pinnedOffset = -1;
            changed = true;
        }
        else
        {
            changed = !hintText.equals(this.hintText);
        }

        if (textWidget == null)
        {
            return;
        }

        if (changed)
        {
            this.hintText = hintText;
            if (!textWidget.isDisposed())
            {
                textWidget.redraw();
            }
        }
    }

    @Override
    public void paintControl(PaintEvent event)
    {
        Preconditions.checkNotNull(event);
        if (textWidget == null || textWidget.isDisposed() || pinnedOffset == -1)
        {
            return;
        }

        drawHint(event.gc, getHint());
    }

    private String getHint()
    {
        var curOffset = pinnedOffset;
        var content = textWidget.getContent();
        var line = content.getLineAtOffset(curOffset);
        var lineStartOffset = content.getOffsetAtLine(line);
        var lineContent = content.getTextRange(lineStartOffset, curOffset - lineStartOffset);
        var trimmedPrefix = lineContent.stripLeading();
        var prefix = ""; //$NON-NLS-1$
        if (trimmedPrefix.length() < lineContent.length())
        {
            prefix = lineContent.substring(0, lineContent.length() - trimmedPrefix.length());
        }

        return hintTextBuilder.build(getHintText(), prefix, uiSettings.getTabWidth(), LINE_FEED_SIGN);
    }

    private void drawHint(GC gc, String hint)
    {
        var caretLocation = textWidget.getCaret().getLocation();
        var x = caretLocation.x;
        var y = caretLocation.y;
        gc.setAdvanced(true);
        gc.setBackground(textWidget.getBackground());
        gc.setForeground(textWidget.getForeground());
        var font = textWidget.getFont();
        var fontData = font.getFontData()[0];
        fontData.setStyle(SWT.ITALIC);
        var italicFont = new Font(font.getDevice(), fontData);
        try
        {
            gc.setFont(italicFont);
            var textSize = gc.textExtent(hint);
            gc.fillRectangle(x - BORDER, y, textSize.x + BORDER * 4, textSize.y);
            gc.setAlpha(160);
            gc.drawText(hint, x + BORDER * 2, y);
            gc.setAlpha(80);
            gc.drawRectangle(x - BORDER, y, textSize.x + BORDER * 4, textSize.y);

            if (!labelText.isBlank())
            {
                fontData.setHeight((int)(fontData.getHeight() * .75));
                var smalFont = new Font(font.getDevice(), fontData);
                try
                {
                    gc.setFont(smalFont);
                    var labelTextSize = gc.textExtent(labelText);
                    var hintX = x;
                    x = x + textSize.x - labelTextSize.x;
                    if (x < hintX)
                    {
                        x = hintX;
                    }

                    y = y + textSize.y;
                    gc.setAlpha(255);
                    gc.fillRectangle(x - BORDER, y, labelTextSize.x + BORDER * 4, labelTextSize.y);
                    gc.setAlpha(80);
                    gc.drawText(labelText, x + BORDER * 2, y);
                    gc.drawRectangle(x - BORDER, y, labelTextSize.x + BORDER * 4, labelTextSize.y);
                }
                finally
                {
                    smalFont.dispose();
                }
            }
        }
        finally
        {
            italicFont.dispose();
        }
    }
}