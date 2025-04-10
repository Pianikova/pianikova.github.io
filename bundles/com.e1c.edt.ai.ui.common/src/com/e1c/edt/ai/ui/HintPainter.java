/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

import com.e1c.edt.ai.IHintTextBuilder;
import com.e1c.edt.ai.IUISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class HintPainter
    implements IHintPainter
{
    private static final char CONTINUATION_SIGN = '…';
    private static final int BORDER = 1;
    private static final int TEXT_EXTENT_FLAGS =
        SWT.DRAW_TRANSPARENT | SWT.DRAW_DELIMITER | SWT.DRAW_TAB | SWT.DRAW_MNEMONIC;

    private final IHintTextBuilder hintTextBuilder;
    private final IUISettings uiSettings;
    private final IUserActions userActions;
    private String hintText = ""; //$NON-NLS-1$
    private String nextToken = ""; //$NON-NLS-1$
    private String displayedHintText = ""; //$NON-NLS-1$
    private String suffix = ""; //$NON-NLS-1$
    private String prefix = ""; //$NON-NLS-1$
    private int acceptedTokens;
    private StyledText textWidget;
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
    public synchronized void pinOffset(StyledText textWidget, int offset, boolean showEmpty, boolean isSingleWordMode)
    {
        this.textWidget = textWidget;
        pinnedOffset = offset;
        this.showEmpty = showEmpty;
        this.isSingleWordMode = isSingleWordMode;
    }

    @Override
    public synchronized String getHintText()
    {
        return hintText;
    }

    @Override
    public synchronized String getDisplayedHintText()
    {
        return displayedHintText;
    }

    @Override
    public synchronized int getOffset()
    {
        return pinnedOffset;
    }

    @Override
    public synchronized void reset()
    {
        pinnedOffset = -1;
        setHintAt(null, "", 0); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public synchronized void setHintAt(String hintText, String nextToken, int acceptedTokens)
    {
        if (hintText == null)
        {
            pinnedOffset = -1;
            displayedHintText = "";
            suffix = "";
            prefix = "";
            this.hintText = "";
            this.nextToken = "";
            this.acceptedTokens = 0;
            return;
        }

        this.hintText = hintText;
        this.nextToken = nextToken;
        this.acceptedTokens = acceptedTokens;
        if (pinnedOffset >= 0 && hintText != null && textWidget != null)
        {
            var hint = hintText;
            int line;
            var text = textWidget.getText();
            if (pinnedOffset < text.length())
            {
                line = textWidget.getLineAtOffset(pinnedOffset);
            }
            else
            {
                line = textWidget.getLineCount() - 1;
            }

            var lineOffset = textWidget.getOffsetAtLine(line);
            var lineText = textWidget.getLine(line);
            prefix = lineOffset < pinnedOffset ? textWidget.getText(lineOffset, pinnedOffset - 1) : ""; //$NON-NLS-1$
            var suffixEnd = lineOffset + lineText.length();
            var totalLength = text.length();
            if (suffixEnd >= totalLength)
            {
                suffixEnd = totalLength - 1;
            }

            suffix = lineOffset < pinnedOffset && suffixEnd > 0 && pinnedOffset < suffixEnd
                ? textWidget.getText(pinnedOffset, suffixEnd) : lineText;

            if (!isSingleWordMode || hint.length() == 0)
            {
                hint = hintTextBuilder.build(prefix, hint, uiSettings.getTabWidth()) + CONTINUATION_SIGN;
            }

            displayedHintText = hint;
        }
    }

    @Override
    public synchronized void paintControl(PaintEvent event)
    {
        Preconditions.checkNotNull(event);
        if (pinnedOffset == -1)
        {
            return;
        }

        if (textWidget == null || textWidget.isDisposed())
        {
            return;
        }

        if (!showEmpty && displayedHintText.isEmpty())
        {
            return;
        }

        var firstLineFinish = displayedHintText.indexOf('\n');
        String firstLine = ""; //$NON-NLS-1$
        String otherLines = ""; //$NON-NLS-1$
        if (firstLineFinish >= 0)
        {
            firstLine = displayedHintText.substring(0, firstLineFinish);
            otherLines = displayedHintText.substring(firstLineFinish + 1);
        }
        else
        {
            firstLine = displayedHintText;
        }

        var token = hintTextBuilder.build(prefix, this.nextToken, uiSettings.getTabWidth());
        token = firstLine.startsWith(token) ? token : ""; //$NON-NLS-1$
        drawHint(event.gc, textWidget, token, firstLine, otherLines, suffix);
    }

    private void drawHint(GC gc, StyledText textWidget, String nextToken, String firstLine, String otherLines,
        String suffix)
    {
        var zeroLocation = textWidget.getLocationAtOffset(0);
        var caretLocation = textWidget.getLocationAtOffset(pinnedOffset);
        var x = caretLocation.x;
        var y = caretLocation.y;
        gc.setAdvanced(true);
        gc.setBackground(textWidget.getBackground());
        gc.setForeground(textWidget.getForeground());

        var font = textWidget.getFont();
        var fontData = font.getFontData()[0];
        fontData.setStyle(SWT.ITALIC);
        var hintFont = new Font(font.getDevice(), fontData);
        fontData.setStyle(SWT.ITALIC | SWT.BOLD);
        var firstTokenFont = new Font(font.getDevice(), fontData);
        fontData.setStyle(SWT.NORMAL);
        fontData.setHeight((int)(fontData.getHeight() * .75));
        var labelFont = new Font(font.getDevice(), fontData);
        try
        {
            var bounds = gc.getClipping();
            var boundsWidth = bounds.width - BORDER * 2 - 1;
            gc.setFont(hintFont);

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

            gc.setFont(labelFont);

            var codeCompletionLabels =
                Integer.toString(acceptedTokens) + '┆' + userActions.getCodeCompletionLabels('┆');
            var labelSize = gc.textExtent(codeCompletionLabels, TEXT_EXTENT_FLAGS);
            var labelX = BORDER + 1;
            var labelY = firstLineY + firstLineH + otherLinesH;
            var labelW = labelSize.x + BORDER * 4;
            var labelH = labelSize.y;

            var l = Integer.max(Integer.max(otherLinesX + otherLinesW, labelX + labelW), boundsWidth);
            otherLinesW = l - otherLinesX;
            labelW = l - labelX;
            labelX = labelW - labelSize.x - BORDER;

            if (!bounds.intersects(firstLineX, firstLineY, otherLinesX + otherLinesW, otherLinesY + otherLinesH))
            {
                return;
            }

            if (firstLine.length() > 0 && !suffix.isBlank())
            {
                gc.copyArea(firstLineX, firstLineY, bounds.width - firstLineX, firstLineH, firstLineX + firstLineW,
                    firstLineY, true);
            }

            if (otherLines.length() > 0)
            {
                gc.copyArea(bounds.x, otherLinesY, bounds.width, bounds.height, bounds.x, otherLinesY + otherLinesH,
                    true);
            }

            gc.fillRectangle(firstLineX, firstLineY, firstLineW, firstLineH);
            if (otherLines.length() > 0)
            {
                gc.fillRectangle(otherLinesX, otherLinesY, otherLinesW, otherLinesH);
                gc.fillRectangle(labelX, labelY, labelW, labelH);
            }

            if (!nextToken.isEmpty())
            {
                gc.setAlpha(180);
                fontData.setStyle(SWT.BOLD);
                gc.setFont(firstTokenFont);
                gc.drawText(nextToken, firstLineX + BORDER * 2, firstLineY, true);

                gc.setFont(hintFont);
                gc.setAlpha(150);
                var nextTokenSize = gc.stringExtent(nextToken);
                gc.drawText(firstLine.substring(nextToken.length()),
                    firstLineX + nextTokenSize.x,
                    firstLineY, true);

                // underline
                if (nextToken.length() > 0 && nextToken.charAt(0) != CONTINUATION_SIGN)
                {
                    gc.setAlpha(150);
                    gc.drawLine(firstLineX + BORDER * 3, firstLineY + firstLineSize.y - 1,
                        firstLineX + nextTokenSize.x, firstLineY + firstLineSize.y - 1);
                }
            }
            else
            {
                gc.setAlpha(150);
                gc.setFont(firstTokenFont);
                gc.drawText(firstLine, firstLineX + BORDER * 2, firstLineY, true);
            }

            gc.setAlpha(120);
            gc.setFont(hintFont);
            gc.drawText(otherLines, otherLinesX + zeroLocation.x, otherLinesY, true);

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
                gc.setFont(labelFont);
                gc.drawText(codeCompletionLabels, labelX + BORDER * 2, labelY + BORDER, true);

                gc.drawPolyline(new int[] {
                    otherLinesX, otherLinesY,

                    firstLineX, otherLinesY,

                    firstLineX, firstLineY,

                    firstLineX + firstLineW, firstLineY,

                    firstLineX + firstLineW, otherLinesY,

                    otherLinesX + otherLinesW, otherLinesY
                });

                gc.drawPolyline(new int[] {
                    labelX, otherLinesY + otherLinesH,

                    otherLinesX + otherLinesW, otherLinesY + otherLinesH,

                    otherLinesX + otherLinesW, otherLinesY + otherLinesH + labelH,

                    labelX, otherLinesY + otherLinesH + labelH,

                    labelX, labelY,

                    otherLinesX, labelY
                });
            }
            // @formatter:on
        }
        finally
        {
            hintFont.dispose();
            firstTokenFont.dispose();
            labelFont.dispose();
            gc.setFont(font);
        }
    }
}