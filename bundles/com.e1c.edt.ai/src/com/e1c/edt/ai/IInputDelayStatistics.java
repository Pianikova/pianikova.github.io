/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.time.Duration;

public interface IInputDelayStatistics
{
    Duration registerAndPredictDelay();
}
