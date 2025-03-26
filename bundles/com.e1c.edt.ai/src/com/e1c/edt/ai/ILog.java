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

    void trace(String topic, Supplier<String> details);

    void debug(String topic, Supplier<String> details);
}
