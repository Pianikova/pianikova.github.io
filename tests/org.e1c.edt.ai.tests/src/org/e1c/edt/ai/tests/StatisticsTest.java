/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;

import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.Statistics;
import org.e1c.edt.ai.StatisticsType;
import org.e1c.edt.ai.StatisticsValue;
import org.junit.Assert;
import org.junit.Test;

public class StatisticsTest
{
    private static final LocalDateTime Start1 = LocalDateTime.of(2024, 10, 26, 14, 15, 0);
    private static final LocalDateTime Time1 = LocalDateTime.of(2024, 10, 26, 14, 15, 30);

    private static final LocalDateTime Start2 = LocalDateTime.of(2024, 10, 26, 17, 15, 0);
    private static final LocalDateTime Time2 = LocalDateTime.of(2024, 10, 26, 17, 15, 10);

    private final IClock clock = mock(IClock.class);

    @SuppressWarnings("resource")
    @Test
    public void shouldMeasure() throws Exception
    {
        // Given
        var statistics = createInstance();

        // When
        when(clock.now()).thenReturn(Start1);
        var mesurement = statistics.measureDuration(StatisticsType.FORM_DURATUION);

        when(clock.now()).thenReturn(Time1);
        mesurement.close();

        var result = statistics.getDurations();

        // Then
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(
            result.contains(new StatisticsValue<>(StatisticsType.FORM_DURATUION, Duration.ofSeconds(30))));
    }

    @SuppressWarnings("resource")
    @Test
    public void shouldMeasureWhenTiwce() throws Exception
    {
        // Given
        var statistics = createInstance();

        // When
        when(clock.now()).thenReturn(Start1);
        var mesurement = statistics.measureDuration(StatisticsType.FORM_DURATUION);

        when(clock.now()).thenReturn(Time1);
        mesurement.close();

        when(clock.now()).thenReturn(Start2);
        mesurement = statistics.measureDuration(StatisticsType.FORM_DURATUION);

        when(clock.now()).thenReturn(Time2);
        mesurement.close();

        var result = statistics.getDurations();

        // Then
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(
            result.contains(new StatisticsValue<>(StatisticsType.FORM_DURATUION, Duration.ofSeconds(40))));
    }

    @SuppressWarnings("resource")
    @Test
    public void shouldMeasureDifferent() throws Exception
    {
        // Given
        var statistics = createInstance();

        // When
        when(clock.now()).thenReturn(Start1);
        var mesurement = statistics.measureDuration(StatisticsType.FORM_DURATUION);

        when(clock.now()).thenReturn(Time1);
        mesurement.close();

        when(clock.now()).thenReturn(Start2);
        mesurement = statistics.measureDuration(StatisticsType.META_DURATUION);

        when(clock.now()).thenReturn(Time2);
        mesurement.close();

        var result = statistics.getDurations();

        // Then
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(
            result.contains(new StatisticsValue<>(StatisticsType.FORM_DURATUION, Duration.ofSeconds(30))));
        Assert.assertTrue(
            result.contains(new StatisticsValue<>(StatisticsType.META_DURATUION, Duration.ofSeconds(10))));
    }

    @Test
    public void shouldRegisterInteger() throws Exception
    {
        // Given
        var statistics = createInstance();

        // When
        statistics.registerInteger(StatisticsType.UNPROCESSED_ITEMS, 3);

        var result = statistics.getIntegers();

        // Then
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.contains(new StatisticsValue<>(StatisticsType.UNPROCESSED_ITEMS, 3)));
    }

    private Statistics createInstance()
    {
        return new Statistics(clock);
    }
}
