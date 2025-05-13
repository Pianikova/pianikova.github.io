/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.time.Duration;

import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Verbosity;

public interface IUISettings
{
    CodeCompletionPolicy getCodeCompletionPolicy();

    void setCodeCompletionPolicy(CodeCompletionPolicy policy);

    int getTabWidth();

    int getCodeCompletionLinesCount();

    Duration getMinRequestDelay();

    Duration getTimeout();

    String getLineSeparator();

    boolean sendExtendedContext();

    boolean sendGlobalContext();

    String getLanguage();

    String getTheme();

    Verbosity getVerbosity();
}
