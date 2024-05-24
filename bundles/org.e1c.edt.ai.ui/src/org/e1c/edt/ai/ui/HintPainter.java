/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.IHintTextBuilder;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

import com.google.common.base.Preconditions;

public class HintPainter
    implements PaintListener, IHintPainter
{
    private static final char LINE_FEED_SIGN = '\u00b6';
    private static final int BORDER = 2;
    private boolean isActive = false;
    private ITextViewer viewer;
    private final IHintTextBuilder hintTextBuilder;
    private final IUISettings uiSettings;
    private StyledText textWidget;
    private String hintText = ""; //$NON-NLS-1$
    private int pinnedOffset = -1;
    private String labelText = ""; //$NON-NLS-1$

    public HintPainter(ITextViewer viewer, IHintTextBuilder hintTextBuilder, IUISettings uiSettings)
    {
        super();
        this.viewer = viewer;
        this.hintTextBuilder = hintTextBuilder;
        this.uiSettings = uiSettings;
        textWidget = viewer.getTextWidget();
    }

    @Override
    public void pinOffset(int offset)
    {
        pinnedOffset = offset;
        if (textWidget == null)
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
            textWidget.redraw();
        }
    }

    @Override
    public void setLabel(String labelText)
    {
        Preconditions.checkNotNull(labelText);
        this.labelText = labelText;
    }

    @Override
    public void dispose()
    {
        viewer = null;
        textWidget = null;
    }

    @Override
    public void paint(int reason)
    {
        if (textWidget == null)
        {
            return;
        }

        var document = viewer.getDocument();
        if (document == null)
        {
            deactivate(false);
            return;
        }

        if (!isActive)
        {
            isActive = true;
            textWidget.addPaintListener(this);
            textWidget.redraw();
            return;
        }

        if (reason == CONFIGURATION || reason == INTERNAL)
        {
            textWidget.redraw();
        }
    }

    @Override
    public void deactivate(boolean redraw)
    {
        if (textWidget == null)
        {
            return;
        }

        if (!isActive)
        {
            return;
        }

        isActive = false;
        textWidget.removePaintListener(this);
        if (!redraw)
        {
            return;
        }

        textWidget.redraw();
    }

    @Override
    public void setPositionManager(IPaintPositionManager manager)
    {
        // no need for a position manager
    }

    @Override
    public void paintControl(PaintEvent event)
    {
        if (textWidget == null || pinnedOffset == -1)
        {
            return;
        }

        drawHint(event.gc, getHint());
    }

    private String getHint()
    {
        var curOffset = pinnedOffset;
        if (viewer instanceof ITextViewerExtension5)
        {
            // adjust offset according folded content
            curOffset = ((ITextViewerExtension5)viewer).modelOffset2WidgetOffset(curOffset);
        }

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