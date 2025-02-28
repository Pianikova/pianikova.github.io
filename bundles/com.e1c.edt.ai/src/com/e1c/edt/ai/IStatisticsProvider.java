/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Collection;

public interface IStatisticsProvider
{
    Collection<StatisticsValue<String>> getValues();
}
