/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.ITextSelection;
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
    private int hintOffset = -1;
    private Image pinImage;

    public HintPainter(ITextViewer textViewer)
    {
        super();
        viewer = textViewer;
        textWidget = textViewer.getTextWidget();
    }

    @Override
    public void pinOffset()
    {
        hintOffset = getCurrentOffset();
        textWidget.redraw();
    }

    @Override
    public int getOffset()
    {
        return hintOffset;
    }

    @Override
    public String getHintText()
    {
        return hintText;
    }

    @Override
    public void setHintText(String hintText)
    {
        if (hintText.isEmpty())
        {
            hintText = DEFAULT_HINT_TEXT;
            hintOffset = -1;
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

        if (hintOffset == -1)
        {
            return;
        }

        var widgetOffset = getCurrentOffset();
        if (hintOffset != widgetOffset)
        {
            hintOffset = -1;
            return;
        }

        var gc = event.gc;
        var text = getHintText();
        var textSize = gc.stringExtent(text);
        var isLastChar = false;
        if (widgetOffset >= textWidget.getCharCount())
        {
            widgetOffset = textWidget.getCharCount() - 1;
            isLastChar = true;
        }

        var bounds = textWidget.getTextBounds(widgetOffset, widgetOffset);
        var x = bounds.x;
        var y = bounds.y;
        var currentChar = textWidget.getContent().getTextRange(widgetOffset, 1);
        if (System.lineSeparator().endsWith(currentChar))
        {
            x = 0;
            y += bounds.height;
        }

        if (isLastChar || System.lineSeparator().startsWith(currentChar))
        {
            x += bounds.width;
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

    private int getCurrentOffset()
    {
        var selectionProvider = viewer.getSelectionProvider();
        var selection = selectionProvider.getSelection();
        var widgetOffset = -1;
        if (selection instanceof ITextSelection)
        {
            widgetOffset = ((ITextSelection)selection).getOffset();
        }

        if (viewer instanceof ITextViewerExtension5)
        {
            // adjust offset according folded content
            widgetOffset = ((ITextViewerExtension5)viewer).modelOffset2WidgetOffset(widgetOffset);
        }

        if (widgetOffset < 0)
        {
            widgetOffset = 0;
        }

        var charCount = textWidget.getCharCount();
        if (widgetOffset > charCount)
        {
            widgetOffset = charCount - 1;
        }

        return widgetOffset;
    }
}