/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Statistics
    implements IStatistics
{
    private final IClock clock;
    private final ConcurrentHashMap<StatisticsType, Duration> durations = new ConcurrentHashMap<>();

    @Inject
    public Statistics(IClock clock)
    {
        Preconditions.checkNotNull(clock);
        this.clock = clock;
    }

    @Override
    public AutoCloseable measureDuration(StatisticsType statisticsType)
    {
        Preconditions.checkNotNull(statisticsType);
        var startTime = clock.now();
        return Closeables.create(() -> {
            var newDuration = Duration.between(startTime, clock.now());
            durations.compute(statisticsType,
                (key, lastDuration) -> lastDuration == null ? newDuration : lastDuration.plus(newDuration));
        });
    }

    @Override
    public Collection<StatisticsData> get()
    {
        return durations.entrySet()
            .stream()
            .map(i -> new StatisticsData(i.getKey(), i.getValue()))
            .collect(Collectors.toList());
    }
}
