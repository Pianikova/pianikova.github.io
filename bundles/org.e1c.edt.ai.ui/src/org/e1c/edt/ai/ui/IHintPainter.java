/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintListener;

public interface IHintPainter
    extends PaintListener
{
    void pinOffset(StyledText textWidget, int offset, boolean showEmpty);

    int getOffset();

    String getHintText();

    void reset();

    void setHintAt(int offset, String hintText, String nextToken);
}
