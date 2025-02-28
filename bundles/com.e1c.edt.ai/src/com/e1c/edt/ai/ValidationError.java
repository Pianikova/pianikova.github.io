/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class ValidationError
{
    private WellknownError error;
    private String target;

    public ValidationError(WellknownError error, String target)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(target);
        this.error = error;
        this.target = target;
    }

    public WellknownError getError()
    {
        return error;
    }

    public String getTarget()
    {
        return target;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(error, target);
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
        ValidationError other = (ValidationError)obj;
        return error == other.error && Objects.equals(target, other.target);
    }
}
