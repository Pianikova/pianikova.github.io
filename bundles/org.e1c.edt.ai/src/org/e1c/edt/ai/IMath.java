/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IMath
{
    ConfidenceInterval calculateConfidenceInterval(double[] sample, double confidenceLevel);
}