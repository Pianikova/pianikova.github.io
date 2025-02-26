/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.util.Collection;

public interface IStatisticsProvider
{
    Collection<StatisticsValue<String>> getValues();
}
