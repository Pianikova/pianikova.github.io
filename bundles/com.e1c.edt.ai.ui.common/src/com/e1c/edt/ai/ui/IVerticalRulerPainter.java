/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintListener;

interface IVerticalRulerPainter
    extends PaintListener
{
    void pin(StyledText textWidget, String hintText);

    void updateRange();

    void reset();
}
