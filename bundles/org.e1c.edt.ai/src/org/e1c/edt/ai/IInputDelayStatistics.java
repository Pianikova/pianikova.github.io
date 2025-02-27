/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;

public interface IInputDelayStatistics
{
    Duration registerAndPredictDelay();
}
