/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.time.Duration;
import java.time.LocalDateTime;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class InputDelayStatistics implements IInputDelayStatistics
{
    private final Object lock = new Object();
    private final ISample sample;
    private final IClock clock;
    private final IMath math;
    private final int minSamplesSize;
    private final double minDelay;
    private final double maxDelay;
    private final double inputConfidenceLevel;
    private final double predictConfidenceLevel;
    private LocalDateTime prevTime;
    private Duration defaultDelay;

    @Inject
    public InputDelayStatistics(IClock clock, IMath math)
    {
        this(new Sample(100), clock, math, 10, Duration.ofMillis(50), Duration.ofMillis(1000), Duration.ofMillis(700),
            .70, .50);
    }

    public InputDelayStatistics(ISample sample, IClock clock, IMath math, int minSamplesSize, Duration minDelay,
        Duration maxDelay,
        Duration defaultDelay, double inputConfidenceLevel, double predictConfidenceLevel)
    {
        this.minDelay = minDelay.toMillis();
        this.maxDelay = maxDelay.toMillis();
        Preconditions.checkNotNull(sample);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(math);
        Preconditions.checkArgument(minSamplesSize > 0);
        Preconditions.checkArgument(this.minDelay > 0);
        Preconditions.checkArgument(this.minDelay < this.maxDelay);
        Preconditions.checkArgument(defaultDelay.toMillis() >= this.minDelay);
        Preconditions.checkArgument(defaultDelay.toMillis() <= this.maxDelay);
        Preconditions.checkArgument(inputConfidenceLevel >= .0 && inputConfidenceLevel <= 1.0);
        Preconditions.checkArgument(predictConfidenceLevel >= .0 && predictConfidenceLevel <= 1.0);
        this.sample = sample;
        this.clock = clock;
        this.math = math;
        this.minSamplesSize = minSamplesSize;
        this.defaultDelay = defaultDelay;
        this.inputConfidenceLevel = inputConfidenceLevel;
        this.predictConfidenceLevel = predictConfidenceLevel;
    }

    @Override
    public Duration registerAndPredictDelay()
    {
        var now = clock.now();
        synchronized (lock)
        {
            if (prevTime == null)
            {
                prevTime = now;
                return defaultDelay;
            }

            var delay = Duration.between(prevTime, now);
            prevTime = now;

            double value = delay.toMillis();
            if (value < minDelay || value > maxDelay)
            {
                return defaultDelay;
            }

            if (sample.getSize() < minSamplesSize)
            {
                sample.addValue(value);
                return defaultDelay;
            }

            var inputInterval = math.calculateConfidenceInterval(sample.getValues(), inputConfidenceLevel);
            if (!inputInterval.isMatch(value))
            {
                return defaultDelay;
            }

            sample.addValue(value);
            var predictInterval = math.calculateConfidenceInterval(sample.getValues(), predictConfidenceLevel);
            defaultDelay = Duration.ofMillis((int)predictInterval.getMax());
            return defaultDelay;
        }
    }
}