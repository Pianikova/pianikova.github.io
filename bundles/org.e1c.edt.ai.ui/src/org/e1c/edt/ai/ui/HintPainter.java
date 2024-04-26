/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;

public class HintPainter
    implements PaintListener, IHintPainter
{
    private static final int BORDER = 2;
    private static final String DEFAULT_HINT_TEXT = ""; //$NON-NLS-1$
    private boolean isActive = false;
    private ITextViewer viewer;
    private StyledText textWidget;
    private String hintText = DEFAULT_HINT_TEXT;
    private int pinnedOffset = -1;
    private int offset = -1;
    private Image pinImage;

    public HintPainter(ITextViewer textViewer)
    {
        super();
        viewer = textViewer;
        textWidget = textViewer.getTextWidget();
    }

    @Override
    public void pinOffset(int offset)
    {
        pinnedOffset = offset;
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
        this.offset = offset;
        if (hintText.isEmpty())
        {
            hintText = DEFAULT_HINT_TEXT;
            pinnedOffset = -1;
            textWidget.redraw();
        }
        else
        {
            this.hintText = hintText;
        }

        textWidget.redraw();
    }

    public Image getPinImage()
    {
        return pinImage;
    }

    public void setPinImage(Image pinImage)
    {
        this.pinImage = pinImage;
        textWidget.redraw();
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
        IDocument document = viewer.getDocument();
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
        if (textWidget == null)
        {
            return;
        }

        if (pinnedOffset == -1)
        {
            return;
        }

        if (pinnedOffset != offset && offset != -1)
        {
            pinnedOffset = -1;
            return;
        }

        var curOffset = pinnedOffset;
        if (viewer instanceof ITextViewerExtension5)
        {
            // adjust offset according folded content
            curOffset = ((ITextViewerExtension5)viewer).modelOffset2WidgetOffset(curOffset);
        }

        var gc = event.gc;
        var text = VisibleTextBuilder.build(getHintText());
        var textSize = gc.stringExtent(text);
        var isLastChar = false;
        if (curOffset >= textWidget.getCharCount())
        {
            curOffset = textWidget.getCharCount() - 1;
            isLastChar = true;
        }

        var bounds = textWidget.getTextBounds(curOffset, curOffset);
        var x = bounds.x;
        var y = bounds.y;
        var content = textWidget.getContent();
        var currentChar = content.getTextRange(curOffset, 1);
        var isLineSeparator =
            System.lineSeparator().endsWith(currentChar) || System.lineSeparator().startsWith(currentChar);
        if (isLastChar && isLineSeparator)
        {
            x = 0;
            y += bounds.height;
        }

        if (isLineSeparator)
        {
            x += bounds.width;
        }

        if (x < 0)
        {
            x = 0;
        }

        if (y < 0)
        {
            y = 0;
        }

        if (textSize.x <= 0 || textSize.y <= 0)
        {
            if (pinImage != null)
            {
                var imageBounds = pinImage.getBounds();
                gc.drawImage(pinImage, 0, 0, imageBounds.width, imageBounds.height, x + BORDER, y + BORDER,
                    bounds.height - BORDER * 2, bounds.height - BORDER * 2);
            }

            return;
        }

        gc.setBackground(textWidget.getBackground());
        gc.setForeground(textWidget.getForeground());
        gc.setFont(textWidget.getFont());
        var width = textSize.x + BORDER * 2;
        var height = textSize.y + 1;
        gc.fillRectangle(x, y, width, height);
        gc.drawRectangle(x, y, width, height);
        gc.setAlpha(160);
        gc.drawString(text, x + BORDER, y, true);
    }
}