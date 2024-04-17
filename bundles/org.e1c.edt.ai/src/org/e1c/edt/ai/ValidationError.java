/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Objects;

public class ValidationError
{
    private int id;
    private String target;

    public ValidationError(int id, String target)
    {
        this.id = id;
        this.target = target;
    }

    public int getId()
    {
        return id;
    }

    public String getTarget()
    {
        return target;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, target);
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
        return id == other.id && Objects.equals(target, other.target);
    }
}
