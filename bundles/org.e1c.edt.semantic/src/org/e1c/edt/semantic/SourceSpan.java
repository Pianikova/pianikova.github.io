/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

public class SourceSpan
{
    private final String path;
    private final int start;
    private final int finish;

    public SourceSpan(String path, int start, int finish)
    {
        this.path = path;
        this.start = start;
        this.finish = finish;
    }

    public String getPath()
    {
        return path;
    }

    public int getStart()
    {
        return start;
    }

    public int getFinish()
    {
        return finish;
    }
}
