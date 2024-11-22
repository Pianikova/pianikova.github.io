/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ISample
{
    int getSize();

    void addValue(double value);

    double[] getValues();
}
