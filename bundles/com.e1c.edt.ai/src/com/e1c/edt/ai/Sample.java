/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public class Sample
    implements ISample
{
    private final double[] sample;
    private int size = 0;
    private int position = 0;

    public Sample(int size)
    {
        sample = new double[size];
    }

    @Override
    public int getSize()
    {
        return size;
    }

    @Override
    public void addValue(double value)
    {
        if (size < sample.length)
        {
            size++;
        }

        if (position < sample.length)
        {
            sample[position] = value;
            position++;
        }
        else
        {
            sample[0] = value;
            position = 1;
        }
    }

    @Override
    public double[] getValues()
    {
        if (sample.length == size)
        {
            return sample;
        }

        var values = new double[size];
        System.arraycopy(sample, 0, values, 0, size);
        return values;
    }
}
