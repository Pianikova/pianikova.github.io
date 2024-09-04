/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Objects;

public class Entity
{
    public String ref;

    public String uuid;

    int start;

    int finish;

    @Override
    public int hashCode()
    {
        return Objects.hash(finish, ref, start, uuid);
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
        Entity other = (Entity)obj;
        return finish == other.finish && Objects.equals(ref, other.ref) && start == other.start
            && Objects.equals(uuid, other.uuid);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return uuid + " - " + ref + " [" + start + ", " + finish + "]";
    }
}
