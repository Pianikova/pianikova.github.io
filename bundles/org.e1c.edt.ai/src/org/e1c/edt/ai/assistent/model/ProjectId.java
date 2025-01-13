/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.common.base.Preconditions;

public class ProjectId
{
    public final String path;

    public ProjectId(String path)
    {
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public String toString()
    {
        return path;
    }
}
