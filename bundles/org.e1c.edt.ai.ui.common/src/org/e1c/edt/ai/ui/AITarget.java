/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;

public class AITarget
{
    private final StyledText textWidget;
    private final int maxLength;
    private final boolean preferSelection;

    public AITarget(StyledText textWidget, int maxLength, boolean preferSelection)
    {
        Preconditions.checkNotNull(textWidget);
        Preconditions.checkArgument(maxLength >= 0);
        this.textWidget = textWidget;
        this.maxLength = maxLength;
        this.preferSelection = preferSelection;
    }

    public StyledText getTextWidget()
    {
        return textWidget;
    }

    public int getMaxLength()
    {
        return maxLength;
    }

    public boolean isPreferSelection()
    {
        return preferSelection;
    }
}
