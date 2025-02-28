/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public interface ISample
{
    int getSize();

    void addValue(double value);

    double[] getValues();
}
