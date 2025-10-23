/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.function.Supplier;

public interface ILog
{
    void logError(Throwable error);

    void logError(String error);

    void warning(String topic, Supplier<String> details);

    boolean isTracingEnabled(String tracingSource);

    void trace(String tracingSource, String topic, Supplier<String> details);
}
