/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class Math
    implements IMath
{
    @Override
    public ConfidenceInterval calculateConfidenceInterval(double[] sample, double confidenceLevel)
    {
        Preconditions.checkNotNull(sample);
        Preconditions.checkArgument(confidenceLevel >= .0 && confidenceLevel <= 1.0);
        if (confidenceLevel >= .70)
        {
            return calculateTDistributionConfidenceInterval(sample, confidenceLevel);
        }

        return calculateLinearConfidenceInterval(sample, confidenceLevel);
    }

    private static double calculateSum(double[] sample)
    {
        Preconditions.checkNotNull(sample);
        double sum = 0;
        for (var value : sample)
        {
            sum += value;
        }

        return sum;
    }

    private static ConfidenceInterval calculateLinearConfidenceInterval(double[] sample, double confidenceLevel)
    {
        var mean = calculateMean(sample);
        var t = mean * (1.0 - confidenceLevel);
        return new ConfidenceInterval(mean - t, mean + t, ConfidenceIntervalType.Linear);
    }

    private static ConfidenceInterval calculateTDistributionConfidenceInterval(double[] sample, double confidenceLevel)
    {
        var mean = calculateMean(sample);
        var standardDeviation = calculateStandardDeviation(sample);
        var t = calculateTDistributionConfidenceCoefficient(confidenceLevel)
            * (standardDeviation / java.lang.Math.sqrt(sample.length));
        return new ConfidenceInterval(mean - t, mean + t, ConfidenceIntervalType.TDistribution);
    }

    private static double calculateMean(double[] sample)
    {
        return calculateSum(sample) / sample.length;
    }

    private static double calculateStandardDeviation(double[] sample)
    {
        var average = calculateMean(sample);
        double acc = 0;
        for (var i = 0; i < sample.length; i++)
        {
            var dif = average - sample[i];
            acc += dif * dif;
        }

        return java.lang.Math.sqrt(acc / sample.length);
    }

    private static double calculateTDistributionConfidenceCoefficient(double confidenceLevel)
    {
        Preconditions.checkArgument(confidenceLevel >= .70 && confidenceLevel <= 1.0);
        if (confidenceLevel >= .99999)
        {
            return 4.417;
        }

        if (confidenceLevel >= .9999)
        {
            return 3.891;
        }

        if (confidenceLevel >= .999)
        {
            return 3.291;
        }

        if (confidenceLevel >= .99)
        {
            return 2.576;
        }

        if (confidenceLevel >= .98)
        {
            return 2.326;
        }

        if (confidenceLevel >= .95)
        {
            return 1.960;
        }

        if (confidenceLevel >= .90)
        {
            return 1.645;
        }

        if (confidenceLevel >= .85)
        {
            return 1.440;
        }

        if (confidenceLevel >= .80)
        {
            return 1.282;
        }

        if (confidenceLevel >= .75)
        {
            return 1.150;
        }

        if (confidenceLevel >= .70)
        {
            return 1.036;
        }

        return 1.036;
    }
}