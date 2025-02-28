/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class ConfidenceInterval
{
    private final double min;
    private final double max;
    private final ConfidenceIntervalType type;

    public ConfidenceInterval(double min, double max, ConfidenceIntervalType type)
    {
        Preconditions.checkArgument(min <= max);
        this.min = min;
        this.max = max;
        this.type = type;
    }

    public double getMin()
    {
        return min;
    }

    public double getMax()
    {
        return max;
    }

    boolean isMatch(double value)
    {
        return value >= min && value <= max;
    }

    public ConfidenceIntervalType getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return type + " [" + min + ", " + max + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(max, min, type);
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
        ConfidenceInterval other = (ConfidenceInterval)obj;
        return Double.doubleToLongBits(max) == Double.doubleToLongBits(other.max)
            && Double.doubleToLongBits(min) == Double.doubleToLongBits(other.min) && type == other.type;
    }
}
