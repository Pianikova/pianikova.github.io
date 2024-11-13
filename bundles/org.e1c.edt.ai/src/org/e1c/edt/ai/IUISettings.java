/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;

public interface IUISettings
{
    int getTabWidth();

    int getCodeCompletionLinesCount();

    boolean isContinuousCodeCompletion();

    Duration getMinRequestDelay();

    Duration getTimeout();

    String getLineSeparator();

    boolean sendContext();

    String getLanguage();
}
