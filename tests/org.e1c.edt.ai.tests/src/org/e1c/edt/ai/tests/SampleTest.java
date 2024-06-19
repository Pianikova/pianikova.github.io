/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import java.util.Arrays;

import org.e1c.edt.ai.Sample;
import org.junit.Assert;
import org.junit.Test;

public class SampleTest
{
    @Test
    public void shouldAddValues()
    {
        // Given
        var sample = new Sample(3);

        // When
        sample.addValue(33);
        sample.addValue(99);

        // Then
        Assert.assertEquals(2, sample.getSize());
        Assert.assertTrue(Arrays.equals(new double[] { 33.0, 99.0 }, sample.getValues()));
    }

    @Test
    public void shouldAddValuesWhenFull()
    {
        // Given
        var sample = new Sample(3);

        // When
        sample.addValue(33);
        sample.addValue(99);
        sample.addValue(11);

        // Then
        Assert.assertEquals(3, sample.getSize());
        Assert.assertTrue(Arrays.equals(new double[] { 33.0, 99.0, 11.0 }, sample.getValues()));
    }

    @Test
    public void shouldAddValuesWhenOverflowed()
    {
        // Given
        var sample = new Sample(3);

        // When
        sample.addValue(33);
        sample.addValue(99);
        sample.addValue(11);
        sample.addValue(44);
        sample.addValue(77);

        // Then
        Assert.assertEquals(3, sample.getSize());
        Assert.assertTrue(Arrays.equals(new double[] { 44.0, 77.0, 11.0 }, sample.getValues()));
    }
}
