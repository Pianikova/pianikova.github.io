/**
 *
 */
package com.e1c.edt.ai.tools;

import org.eclipse.core.resources.IMarker;

/**
 * Represents different types of Eclipse markers
 */
public enum MarkerType
{
    /**
     * Base marker type
     */
    MARKER(IMarker.MARKER, "General marker."), //$NON-NLS-1$

    /**
     * Task marker type for TODO items and tasks
     */
    TASK(IMarker.TASK, "ALWAYS use it when planning plans, schedules, proposals, tasks, TODO, etc."), //$NON-NLS-1$

    /**
     * Problem marker type for errors and warnings
     */
    PROBLEM(IMarker.PROBLEM, "Contains information about build issues."), //$NON-NLS-1$

    /**
     * Text marker type for text annotations
     */
    TEXT(IMarker.TEXT, "Text annotation marker."), //$NON-NLS-1$

    /**
     * Bookmark marker type for user bookmarks
     */
    BOOKMARK(IMarker.BOOKMARK,
        "ALWAYS use it for summaries, reports."), //$NON-NLS-1$

    /**
     * Custom AI-generated marker type
     */
    AI_MARKER("com.e1c.edt.ai.AIMarker", //$NON-NLS-1$
        "ALWAYS use it to show any issues, problems, errors, warnings, etc."); //$NON-NLS-1$

    public static final String AI_MARKER_BASE = "com.e1c.edt.ai.AIMarker"; //$NON-NLS-1$
    public static final String AI_MARKER_ERROR = "com.e1c.edt.ai.AIError"; //$NON-NLS-1$
    public static final String AI_MARKER_WARNING = "com.e1c.edt.ai.AIWarning"; //$NON-NLS-1$
    public static final String AI_MARKER_INFO = "com.e1c.edt.ai.AIInfo"; //$NON-NLS-1$
    public static final String LEGACY_AI_MARKER_BASE = "com.e1c.edt.ai.marker"; //$NON-NLS-1$

    private final String typeId;
    private final String description;

    /**
     * Constructs a MarkerType enum instance
     *
     * @param typeId The Eclipse marker type identifier
     * @param description The display-friendly name of the marker type
     */
    MarkerType(String typeId, String description)
    {
        this.typeId = typeId;
        this.description = description;
    }

    /**
     * Gets the Eclipse marker type identifier
     *
     * @return The full marker type ID string
     */
    public String getTypeId()
    {
        return typeId;
    }

    /**
     * Gets the display-friendly name of the marker type
     * @return Short, human-readable description for the marker type
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Gets the display-friendly name of the marker type
     *
     * @return Short, human-readable name for the marker type
     */
    @SuppressWarnings("nls")
    public String getDisplayName()
    {
        switch (this)
        {
        case MARKER:
            return "marker";
        case TASK:
            return "task";
        case PROBLEM:
            return "problem";
        case TEXT:
            return "text";
        case BOOKMARK:
            return "bookmark";
        case AI_MARKER:
            return "ai_marker";
        default:
            return name().toLowerCase();
        }
    }

    /**
     * Finds the MarkerType by its Eclipse type identifier
     *
     * @param typeId The full marker type ID to search for
     * @return Matching MarkerType or null if not found
     */
    public static MarkerType fromTypeId(String typeId)
    {
        if (typeId != null && (typeId.startsWith(AI_MARKER_BASE) || typeId.startsWith(LEGACY_AI_MARKER_BASE)))
        {
            return MarkerType.AI_MARKER;
        }
        for (MarkerType type : values())
        {
            if (type.typeId.equals(typeId))
            {
                return type;
            }
        }

        return MarkerType.PROBLEM;
    }

    /**
     * Finds the MarkerType by its display name
     *
     * @param displayName The user-friendly name to search for
     * @return Matching MarkerType or null if not found
     */
    public static MarkerType fromDisplayName(String displayName)
    {
        if (displayName == null)
            return null;

        for (MarkerType type : values())
        {
            if (type.getDisplayName().equalsIgnoreCase(displayName))
            {
                return type;
            }
        }

        return null;
    }

    @SuppressWarnings("nls")
    public static String getAiMarkerTypeId(String severity)
    {
        if (severity == null)
        {
            return AI_MARKER_INFO;
        }
        var normalized = severity.trim().toLowerCase();
        switch (normalized)
        {
        case "error":
            return AI_MARKER_ERROR;
        case "warn":
        case "warning":
            return AI_MARKER_WARNING;
        case "info":
        case "information":
        default:
            return AI_MARKER_INFO;
        }
    }

    public static String[] getAiMarkerTypeIds()
    {
        return new String[] { AI_MARKER_ERROR, AI_MARKER_WARNING, AI_MARKER_INFO,
            LEGACY_AI_MARKER_BASE + ".error", LEGACY_AI_MARKER_BASE + ".warning", LEGACY_AI_MARKER_BASE + ".info" };
    }
}
