/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Objects;

/**
 * Key class for identifying markers based on path, start line, and message.
 * Used for deduplication of markers in {@link GetMarkersMcpTool}.
 */
public final class MarkerKey
{
    private final String path;
    private final Integer startLine;
    private final String message;

    /**
     * Constructs a new MarkerKey.
     *
     * @param path the marker file path (can be null)
     * @param startLine the marker start line (can be null)
     * @param message the marker message (can be null)
     */
    public MarkerKey(String path, Integer startLine, String message)
    {
        this.path = path;
        this.startLine = startLine;
        this.message = message;
    }

    /**
     * Gets the path.
     *
     * @return the path or null if undefined
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Gets the start line.
     *
     * @return the start line or null if undefined
     */
    public Integer getStartLine()
    {
        return startLine;
    }

    /**
     * Gets the message.
     *
     * @return the message or null if undefined
     */
    public String getMessage()
    {
        return message;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MarkerKey markerKey = (MarkerKey)obj;
        return Objects.equals(startLine, markerKey.startLine) && Objects.equals(path, markerKey.path)
            && Objects.equals(message, markerKey.message);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(path, startLine, message);
    }
}
