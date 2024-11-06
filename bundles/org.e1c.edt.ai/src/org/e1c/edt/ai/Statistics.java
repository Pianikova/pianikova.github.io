/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Statistics
    implements IStatistics
{
    private final IClock clock;
    private final ConcurrentHashMap<StatisticsType, Duration> durations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<StatisticsType, Integer> integers = new ConcurrentHashMap<>();

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
    public void registerInteger(StatisticsType statisticsType, int value)
    {
        integers.put(statisticsType, value);
    }

    @Override
    public Collection<StatisticsValue<String>> getValues()
    {
        var result = new ArrayList<StatisticsValue<String>>();
        for (var duration : durations.entrySet())
        {
            result.add(new StatisticsValue<>(duration.getKey(),
                String.format(Locale.US, "%.9f", duration.getValue().toNanos() / 1000000000d))); //$NON-NLS-1$
        }

        for (var integer : integers.entrySet())
        {
            result.add(new StatisticsValue<>(integer.getKey(), Integer.toString(integer.getValue())));
        }

        return result;
    }

    public Collection<StatisticsValue<Duration>> getDurations()
    {
        return durations.entrySet()
            .stream()
            .map(i -> new StatisticsValue<>(i.getKey(), i.getValue()))
            .collect(Collectors.toList());
    }

    public Collection<StatisticsValue<Integer>> getIntegers()
    {
        return integers.entrySet()
            .stream()
            .map(i -> new StatisticsValue<>(i.getKey(), i.getValue()))
            .collect(Collectors.toList());
    }
}
