/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;

public class InputDelayStatisticsTest
{
    private static final LocalDateTime START_TIME = LocalDateTime.of(1977, 11, 16, 17, 35, 00);
    private static final Duration MIN_DELAY = Duration.ofSeconds(10);
    private static final Duration DEFAULT_DELAY = Duration.ofSeconds(70);
    private static final Duration MAX_DELAY = Duration.ofSeconds(100);
    private static final double INPUT_CONFIDENCE_LEVEL = .90;
    private static final double PREDICT_CONFIDENCE_LEVEL = .50;
    private static double[] someValues = new double[] { 1, 2, 3 };

    private final ISample sample = mock(ISample.class);
    private final IClock clock = mock(IClock.class);
    private final IMath math = mock(IMath.class);
    private final IUISettings uiSettings = mock(IUISettings.class);
    private int sampleSize = 0;
    private double[] sampleValues = new double[0];
    private LocalDateTime now = START_TIME;

    public InputDelayStatisticsTest()
    {
        when(sample.getSize()).thenAnswer(i -> sampleSize);
        when(sample.getValues()).thenAnswer(i -> sampleValues);
        when(clock.now()).thenAnswer(i -> now);
        when(uiSettings.getCodeCompletionPolicy()).thenAnswer(i -> CodeCompletionPolicy.CREATIVITY);
    }

    @Test
    public void shouldNotRegisterFirstOne()
    {
        // Given
        var statistics = createInstance();

        // When
        var actualDelay = statistics.registerAndPredictDelay();

        // Then
        Assert.assertEquals(DEFAULT_DELAY, actualDelay);
        verify(sample, times(0)).addValue(any(double.class));
    }

    @Test
    public void shouldValidateMin()
    {
        // Given
        var statistics = createInstance();
        statistics.registerAndPredictDelay();

        // When
        delay(5);
        var actualDelay = statistics.registerAndPredictDelay();

        // Then
        Assert.assertEquals(DEFAULT_DELAY, actualDelay);
        verify(sample, times(0)).addValue(any(double.class));
    }

    @Test
    public void shouldValidateMax()
    {
        // Given
        var statistics = createInstance();
        statistics.registerAndPredictDelay();

        // When
        delay(1055);
        var actualDelay = statistics.registerAndPredictDelay();

        // Then
        Assert.assertEquals(DEFAULT_DELAY, actualDelay);
        verify(sample, times(0)).addValue(any(double.class));
    }

    @Test
    public void shouldRegisterSecondOne()
    {
        // Given
        var statistics = createInstance();
        statistics.registerAndPredictDelay();

        // When
        delay(40);
        var actualDelay = statistics.registerAndPredictDelay();

        // Then
        Assert.assertEquals(DEFAULT_DELAY, actualDelay);
        verify(sample).addValue(40000);
    }

    @Test
    public void shouldRegisterSecondOneAndReturnDefaultWhenSampleSizeIsLessThenMinSamplesSize()
    {
        // Given
        var statistics = createInstance();
        statistics.registerAndPredictDelay();

        // When
        delay(40);
        sampleSize = 1;
        sampleValues = someValues;
        var actualDelay = statistics.registerAndPredictDelay();

        // Then
        verify(sample).addValue(40000);
        verify(math, times(0)).calculateConfidenceInterval(someValues, INPUT_CONFIDENCE_LEVEL);
        Assert.assertEquals(DEFAULT_DELAY, actualDelay);
    }

    @Test
    public void shouldValidateInputConfidenceLevel()
    {
        // Given
        var statistics = createInstance();
        statistics.registerAndPredictDelay();
        var inputiConfidenceInterval = new ConfidenceInterval(37000, 39000, ConfidenceIntervalType.TDistribution);
        when(math.calculateConfidenceInterval(someValues, INPUT_CONFIDENCE_LEVEL))
            .thenReturn(inputiConfidenceInterval);

        // When
        delay(40);
        sampleSize = someValues.length;
        sampleValues = someValues;
        var actualDelay = statistics.registerAndPredictDelay();

        // Then
        Assert.assertEquals(DEFAULT_DELAY, actualDelay);
        verify(sample, times(0)).addValue(any(double.class));
    }

    @Test
    public void shouldRegisterAndReturnPrediction()
    {
        // Given
        var statistics = createInstance();
        statistics.registerAndPredictDelay();
        var inputConfidenceInterval = new ConfidenceInterval(39000, 41000, ConfidenceIntervalType.TDistribution);
        when(math.calculateConfidenceInterval(someValues, INPUT_CONFIDENCE_LEVEL)).thenReturn(inputConfidenceInterval);
        var predictConfidenceInterval = new ConfidenceInterval(20000, 30000, ConfidenceIntervalType.Linear);
        when(math.calculateConfidenceInterval(someValues, PREDICT_CONFIDENCE_LEVEL))
            .thenReturn(predictConfidenceInterval);

        // When
        delay(40);
        sampleSize = someValues.length;
        sampleValues = someValues;
        var actualDelay = statistics.registerAndPredictDelay();

        // Then
        verify(sample).addValue(40000);
        Assert.assertEquals(Duration.ofSeconds(30), actualDelay);
    }

    private InputDelayStatistics createInstance()
    {
        return new InputDelayStatistics(sample, clock, math, uiSettings, 2, MIN_DELAY, MAX_DELAY, DEFAULT_DELAY,
            INPUT_CONFIDENCE_LEVEL, PREDICT_CONFIDENCE_LEVEL, PREDICT_CONFIDENCE_LEVEL);
    }

    private void delay(int seconds)
    {
        now = now.plusSeconds(seconds);
    }
}