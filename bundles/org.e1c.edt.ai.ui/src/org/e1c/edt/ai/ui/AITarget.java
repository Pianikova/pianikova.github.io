/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.CodeCompletionType;
import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;

public class AITarget
{
    private final StyledText textWidget;
    private final int maxLength;
    private final CodeCompletionType complitionType;

    public AITarget(StyledText textWidget, int maxLength, CodeCompletionType complitionType)
    {
        Preconditions.checkNotNull(textWidget);
        Preconditions.checkArgument(maxLength >= 0);
        this.textWidget = textWidget;
        this.maxLength = maxLength;
        this.complitionType = complitionType;
    }

    public StyledText getTextWidget()
    {
        return textWidget;
    }

    public int getMaxLength()
    {
        return maxLength;
    }

    public CodeCompletionType getComplitionType()
    {
        return complitionType;
    }
}
