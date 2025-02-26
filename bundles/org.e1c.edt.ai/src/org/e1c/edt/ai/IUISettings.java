/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;

public interface IUISettings
{
    boolean isCodeCompletion();

    int getTabWidth();

    int getCodeCompletionLinesCount();

    boolean isContinuousCodeCompletion();

    Duration getMinRequestDelay();

    Duration getTimeout();

    String getLineSeparator();

    boolean sendContext();

    boolean sendGlobalContext();

    String getLanguage();

    String getTheme();

    boolean traceMode();
}
