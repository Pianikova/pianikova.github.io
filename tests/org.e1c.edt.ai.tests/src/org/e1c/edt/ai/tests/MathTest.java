/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import org.e1c.edt.ai.ConfidenceIntervalType;
import org.junit.Assert;
import org.junit.Test;

public class MathTest
{
    @Test
    public void shouldCalculateTDistributionConfidenceInterval()
    {
        // Given
        double[] sample = { 10.0, 12, 23, 23, 16, 23, 21, 16 };
        var math = new org.e1c.edt.ai.Math();

        // When
        var actualResult = math.calculateConfidenceInterval(sample, .9);

        // Then
        Assert.assertEquals(java.lang.Math.round((18 - 2.849)), java.lang.Math.round(actualResult.getMin()));
        Assert.assertEquals(java.lang.Math.round((18 + 2.849)), java.lang.Math.round(actualResult.getMax()));
        Assert.assertEquals(ConfidenceIntervalType.TDistribution, actualResult.getType());
    }

    @Test
    public void shouldCalculateLinearConfidenceInterval()
    {
        // Given
        double[] sample = { 10.0, 12, 23, 23, 16, 23, 21, 16 };
        var math = new org.e1c.edt.ai.Math();

        // When
        var actualResult = math.calculateConfidenceInterval(sample, .6);

        // Then
        Assert.assertEquals(java.lang.Math.round((18 - 18 * .4)), java.lang.Math.round(actualResult.getMin()));
        Assert.assertEquals(java.lang.Math.round((18 + 18 * .4)), java.lang.Math.round(actualResult.getMax()));
        Assert.assertEquals(ConfidenceIntervalType.Linear, actualResult.getType());
    }
}
