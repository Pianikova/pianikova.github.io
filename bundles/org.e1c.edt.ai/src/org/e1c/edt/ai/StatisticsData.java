/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;
import java.util.Objects;

import com.google.common.base.Preconditions;

public class StatisticsData
{
    private final StatisticsType statisticsType;
    private final Duration duration;

    public StatisticsData(StatisticsType statisticsType, Duration duration)
    {
        Preconditions.checkNotNull(statisticsType);
        Preconditions.checkNotNull(duration);
        this.statisticsType = statisticsType;
        this.duration = duration;
    }

    public StatisticsType getStatisticsType()
    {
        return statisticsType;
    }

    public Duration getDuration()
    {
        return duration;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(duration, statisticsType);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StatisticsData other = (StatisticsData)obj;
        return Objects.equals(duration, other.duration) && statisticsType == other.statisticsType;
    }
}
