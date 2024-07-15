/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IUISettings
{
    int getTabWidth();

    int getCodeCompletionLinesCount();

    boolean isContinuousCodeCompletion();

    int getMaxAssistantTextSize();
}
