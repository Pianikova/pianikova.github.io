/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;

interface ITextWidgetInfoUpdater
{
    void setLastMouseOffset(StyledText textWidget, int offset);

    void reset();
}
