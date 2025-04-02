/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.events.PaintListener;

interface IHintPainter
    extends PaintListener
{
    void pinOffset(int offset, boolean showEmpty, boolean isSingleWordMode);

    int getOffset();

    String getHintText();

    void reset();

    void setHintAt(int offset, String hintText, String nextToken, int acceptedTokens);
}
