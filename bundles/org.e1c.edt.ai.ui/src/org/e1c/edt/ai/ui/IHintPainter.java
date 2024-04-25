/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.jface.text.IPainter;

public interface IHintPainter
    extends IPainter
{
    void pinOffset(int offset);

    int getOffset();

    String getHintText();

    void reset();

    void setHintAt(int offset, String hintText);
}
