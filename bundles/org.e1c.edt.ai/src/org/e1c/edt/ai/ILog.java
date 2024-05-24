/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ILog
{
    void logError(Throwable error);

    void trace(String topic, String details);
}
