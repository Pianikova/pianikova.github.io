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
        public void registerInteger(StatisticsType statisticsType, int value)
        {
            //
        }

        @Override
        public Collection<StatisticsValue<String>> getValues()
        {
            return Collections.emptyList();
        }
    };

    public AutoCloseable measureDuration(StatisticsType statisticsType);

    public void registerInteger(StatisticsType statisticsType, int value);
}
