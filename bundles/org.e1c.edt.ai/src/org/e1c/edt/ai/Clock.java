/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.time.LocalDateTime;

class Clock
    implements IClock
{
    @Override
    public LocalDateTime now()
    {
        return LocalDateTime.now();
    }
}
