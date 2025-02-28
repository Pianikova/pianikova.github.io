/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class StatisticsValue<T>
{
    private final StatisticsType statisticsType;
    private final T value;

    public StatisticsValue(StatisticsType statisticsType, T value)
    {
        Preconditions.checkNotNull(statisticsType);
        Preconditions.checkNotNull(value);
        this.statisticsType = statisticsType;
        this.value = value;
    }

    public StatisticsType getStatisticsType()
    {
        return statisticsType;
    }

    public T getValue()
    {
        return value;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(value, statisticsType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StatisticsValue<T> other = (StatisticsValue<T>)obj;
        return Objects.equals(value, other.value) && statisticsType == other.statisticsType;
    }
}
