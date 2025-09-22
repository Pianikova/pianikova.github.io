/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;

public class AITarget
{
    private final StyledText textWidget;
    private final boolean limitSize;
    private final boolean preferSelection;

    public AITarget(StyledText textWidget, boolean limitSize, boolean preferSelection)
    {
        Preconditions.checkNotNull(textWidget);
        this.textWidget = textWidget;
        this.limitSize = limitSize;
        this.preferSelection = preferSelection;
    }

    public StyledText getTextWidget()
    {
        return textWidget;
    }

    public boolean getLimitSize()
    {
        return limitSize;
    }

    public boolean isPreferSelection()
    {
        return preferSelection;
    }
}
