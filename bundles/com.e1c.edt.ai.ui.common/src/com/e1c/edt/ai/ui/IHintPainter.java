/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintListener;

interface IHintPainter
    extends PaintListener
{
    void pinOffset(StyledText textWidget, int offset, boolean showBlank, boolean isSingleWordMode);

    int getOffset();

    String getHintText();

    String getDisplayedHintText();

    void reset();

    void setHintAt(String hintText, String nextToken, int acceptedTokens);
}
