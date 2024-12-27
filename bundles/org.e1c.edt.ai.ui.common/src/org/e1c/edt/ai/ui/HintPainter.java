/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.IHintTextBuilder;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class HintPainter
    implements PaintListener, IHintPainter
{
    private static final char CONTINUATION_SIGN = '…';
    private static final int BORDER = 1;
    private static final int TEXT_EXTENT_FLAGS =
        SWT.DRAW_TRANSPARENT | SWT.DRAW_DELIMITER | SWT.DRAW_TAB | SWT.DRAW_MNEMONIC;

    private final IHintTextBuilder hintTextBuilder;
    private final IUISettings uiSettings;
    private final IUserActions userActions;
    private StyledText textWidget;
    private String hintText = ""; //$NON-NLS-1$
    private String nextToken = ""; //$NON-NLS-1$
    private int pinnedOffset = -1;
    private boolean showEmpty;
    private boolean isSingleWordMode;

    @Inject
    public HintPainter(IHintTextBuilder hintTextBuilder, IUISettings uiSettings, IUserActions userActions)
    {
        Preconditions.checkNotNull(hintTextBuilder);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(userActions);
        this.hintTextBuilder = hintTextBuilder;
        this.uiSettings = uiSettings;
        this.userActions = userActions;
    }

    @Override
    public void pinOffset(StyledText textWidget, int offset, boolean showEmpty, boolean isSingleWordMode)
    {
        Preconditions.checkNotNull(textWidget);
        this.textWidget = textWidget;
        pinnedOffset = offset;
        this.showEmpty = showEmpty;
        this.isSingleWordMode = isSingleWordMode;
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
        setHintAt(-1, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void setHintAt(int offset, String hintText, String nextToken)
    {
        var changed = false;
        if (hintText == null || offset == -1)
        {
            pinnedOffset = -1;
            changed = true;
        }
        else
        {
            changed = !hintText.equals(this.hintText) || !nextToken.equals(this.nextToken);
        }

        if (textWidget == null)
        {
            return;
        }

        if (changed)
        {
            this.hintText = hintText;
            this.nextToken = nextToken;
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

        if (!showEmpty && getHintText().isEmpty())
        {
            return;
        }

        var text = getHintText();
        if (!isSingleWordMode || text.length() == 0)
        {
            text = hintTextBuilder.build(text, uiSettings.getTabWidth(), CONTINUATION_SIGN);
        }

        var firstLineFinish = text.indexOf('\n');
        String firstLine = ""; //$NON-NLS-1$
        String otherLines = ""; //$NON-NLS-1$
        if (firstLineFinish >= 0)
        {
            firstLine = text.substring(0, firstLineFinish);
            otherLines = text.substring(firstLineFinish + 1);
        }
        else
        {
            firstLine = text;
        }

        drawHint(event.gc, firstLine.startsWith(this.nextToken) ? this.nextToken : "", //$NON-NLS-1$
            firstLine, otherLines);
    }

    private void drawHint(GC gc, String nextToken, String firstLine, String otherLines)
    {
        var caretLocation = textWidget.getLocationAtOffset(pinnedOffset);
        var x = caretLocation.x;
        var y = caretLocation.y;
        gc.setAdvanced(true);
        gc.setBackground(textWidget.getBackground());
        gc.setForeground(textWidget.getForeground());

        var font = textWidget.getFont();
        var fontData = font.getFontData()[0];
        fontData.setStyle(SWT.ITALIC);
        var italicFont = new Font(font.getDevice(), fontData);
        fontData.setStyle(SWT.ITALIC | SWT.BOLD);
        var boldFont = new Font(font.getDevice(), fontData);
        fontData.setStyle(SWT.NORMAL);
        fontData.setHeight((int)(fontData.getHeight() * .75));
        var smalFont = new Font(font.getDevice(), fontData);
        try
        {
            var bounds = gc.getClipping();
            var boundsWidth = bounds.width - BORDER * 2 - 1;
            gc.setFont(italicFont);

            var firstLineSize = gc.textExtent(firstLine, TEXT_EXTENT_FLAGS);
            var firstLineX = x - BORDER + 1;
            var firstLineY = y;
            var firstLineW = firstLineSize.x + BORDER * 4;
            var firstLineH = firstLineSize.y + 1;

            var otherLinesSize = gc.textExtent(otherLines, TEXT_EXTENT_FLAGS);
            var otherLinesX = BORDER + 1;
            var otherLinesY = firstLineY + firstLineH;
            var otherLinesW = boundsWidth;
            var otherLinesH = otherLinesSize.y;
            if (otherLines.isEmpty())
            {
                otherLinesX = firstLineX;
                otherLinesY = firstLineY;
                otherLinesW = 0;
                otherLinesH = 0;
            }

            gc.setFont(smalFont);

            var codeCompletionLabels = userActions.getCodeCompletionLabels(' ');
            var labelSize = gc.textExtent(codeCompletionLabels, TEXT_EXTENT_FLAGS);
            var labelX = BORDER + 1;
            var labelY = firstLineY + firstLineH + otherLinesH;
            var labelW = labelSize.x + BORDER * 4;
            var labelH = labelSize.y;

            var l = Integer.max(Integer.max(otherLinesX + otherLinesW, labelX + labelW), boundsWidth);
            otherLinesW = l - otherLinesX;
            labelW = l - labelX;
            labelX = labelW - labelSize.x - BORDER;

            gc.copyArea(firstLineX, firstLineY, firstLineX + firstLineW, firstLineH, firstLineX + firstLineW,
                firstLineY, true);
            gc.copyArea(bounds.x, otherLinesY, bounds.width, bounds.height, bounds.x, otherLinesY + otherLinesH, true);

            gc.fillRectangle(firstLineX, firstLineY, firstLineW, firstLineH);
            gc.fillRectangle(otherLinesX, otherLinesY, otherLinesW, otherLinesH);
            gc.fillRectangle(labelX, labelY, labelW, labelH);

            if (!nextToken.isEmpty())
            {
                gc.setAlpha(180);
                fontData.setStyle(SWT.BOLD);
                gc.setFont(boldFont);
                gc.drawText(nextToken, firstLineX + BORDER * 2, firstLineY, true);

                gc.setFont(italicFont);
                gc.setAlpha(160);
                var nextTokenSize = gc.stringExtent(nextToken);
                gc.drawText(firstLine.substring(nextToken.length()), firstLineX + BORDER * 2 + nextTokenSize.x,
                    firstLineY, true);
            }
            else
            {
                gc.setAlpha(160);
                gc.setFont(italicFont);
                gc.drawText(firstLine, firstLineX + BORDER * 2, firstLineY, true);
            }

            gc.setAlpha(120);
            gc.setFont(italicFont);
            gc.drawText(otherLines, otherLinesX + BORDER * 2, otherLinesY, true);

            gc.setLineStyle(SWT.LINE_DOT);

            // @formatter:off
            if (otherLines.isEmpty())
            {
                gc.drawPolyline(new int[] {
                    firstLineX, firstLineY,

                    firstLineX + firstLineW, firstLineY,

                    firstLineX + firstLineW, firstLineY + firstLineH,

                    firstLineX, firstLineY + firstLineH,

                    firstLineX, firstLineY
                });
            }
            else
            {
                gc.setFont(smalFont);
                gc.drawText(codeCompletionLabels, labelX + BORDER * 2, labelY, true);

                gc.drawPolyline(new int[] {
                    firstLineX, firstLineY,

                    firstLineX + firstLineW, firstLineY,

                    firstLineX + firstLineW, otherLinesY,

                    otherLinesX + otherLinesW, otherLinesY,

                    otherLinesX + otherLinesW, otherLinesY + otherLinesH + labelH,

                    labelX, otherLinesY + otherLinesH + labelH,

                    labelX, labelY,

                    otherLinesX, labelY,

                    otherLinesX, otherLinesY,

                    firstLineX, otherLinesY,

                    firstLineX, firstLineY
                });
            }
            // @formatter:on
        }
        finally
        {
            italicFont.dispose();
            boldFont.dispose();
            smalFont.dispose();
            gc.setFont(font);
        }
    }
}