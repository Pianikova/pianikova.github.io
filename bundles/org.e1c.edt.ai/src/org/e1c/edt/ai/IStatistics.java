/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Collection;
import java.util.Collections;

public interface IStatistics
    extends IStatisticsProvider
{
    public static final IStatistics Empty = new IStatistics()
    {
        @Override
        public AutoCloseable measureDuration(StatisticsType statisticsType)
        {
            return Closeables.Empty;
        }

        @Override
        public Collection<StatisticsData> get()
        {
            return Collections.emptyList();
        }
    };

    public AutoCloseable measureDuration(StatisticsType statisticsType);
}
