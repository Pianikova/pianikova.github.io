package com.e1c.edt.ai.tools;

import org.eclipse.core.resources.IMarker;

/**
 * Represents different types of Eclipse markers
 */
public enum MarkerType
{
    UNKNOWN(null, "Unknown marker type.", "unknown"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Base marker type
     */
    MARKER(IMarker.MARKER, "General marker.", "marker"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Task marker type for TODO items and tasks
     */
    TASK(IMarker.TASK, "ALWAYS use it when planning plans, schedules, proposals, tasks, TODO, etc.", "task"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Problem marker type for errors and warnings
     */
    PROBLEM(IMarker.PROBLEM,
        "Eclipse build problems only. Does NOT include 1C validation errors/warnings (those are type `1c`)," //$NON-NLS-1$
            + " so an empty result here does not mean the 1C project is clean.", //$NON-NLS-1$
        "problem"), //$NON-NLS-1$

    /**
     * Text marker type for text annotations
     */
    TEXT(IMarker.TEXT, "Text annotation marker.", "text"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Bookmark marker type for user bookmarks
     */
    BOOKMARK(IMarker.BOOKMARK,
        "ALWAYS use it for summaries, reports.", "bookmark"), //$NON-NLS-1$ //$NON-NLS-2$

    M1C("1c", //$NON-NLS-1$
        "1C validation marker (metadata and built-in language checks). This is the type to use when" //$NON-NLS-1$
            + " verifying that a 1C configuration has no errors.", //$NON-NLS-1$
        "1c"), //$NON-NLS-1$

    /**
     * Custom AI-generated marker type
     */
    AI_MARKER("com.e1c.edt.ai.AIMarker", //$NON-NLS-1$
        "ALWAYS use it to show any issues, problems, errors, warnings, etc.", "ai_marker"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final String AI_MARKER_BASE = "com.e1c.edt.ai.AIMarker"; //$NON-NLS-1$
    public static final String AI_MARKER_ERROR = "com.e1c.edt.ai.AIError"; //$NON-NLS-1$
    public static final String AI_MARKER_WARNING = "com.e1c.edt.ai.AIWarning"; //$NON-NLS-1$
    public static final String AI_MARKER_INFO = "com.e1c.edt.ai.AIInfo"; //$NON-NLS-1$
    public static final String M1C_MARKER_BASE = "com._1c.g5.v8.dt.bsl.ui.bsl."; //$NON-NLS-1$
    public static final String M1C_MARKER_INFO = "1c"; //$NON-NLS-1$

    private final String typeId;
    private final String description;
    private final String displayName;

    /**
     * Constructs a MarkerType enum instance
     *
     * @param typeId The Eclipse marker type identifier
     * @param description The display-friendly name of the marker type
     * @param displayName The display-friendly name for the marker type
     */
    MarkerType(String typeId, String description, String displayName)
    {
        this.typeId = typeId;
        this.description = description;
        this.displayName = displayName;
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
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Finds the MarkerType by its Eclipse type identifier
     *
     * @param typeId The full marker type ID to search for
     * @return Matching MarkerType or null if not found
     */
    public static MarkerType fromTypeId(String typeId)
    {
        if (typeId != null)
        {
            // AIError/AIWarning/AIInfo do not share the AIMarker prefix, so match them explicitly
            if (typeId.startsWith(AI_MARKER_BASE) || isAiMarkerTypeId(typeId))
            {
                return MarkerType.AI_MARKER;
            }

            if (typeId.startsWith(M1C_MARKER_BASE))
            {
                return MarkerType.M1C;
            }
        }

        for (var type : values())
        {
            if (typeId.equals(type.typeId))
            {
                return type;
            }
        }

        return MarkerType.UNKNOWN;
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
        {
            return MarkerType.UNKNOWN;
        }

        for (MarkerType type : values())
        {
            if (type.getDisplayName().equalsIgnoreCase(displayName))
            {
                return type;
            }
        }

        return MarkerType.UNKNOWN;
    }

    public static String[] getAiMarkerTypeIds()
    {
        return new String[] { AI_MARKER_ERROR, AI_MARKER_WARNING, AI_MARKER_INFO };
    }

    private static boolean isAiMarkerTypeId(String typeId)
    {
        for (var aiTypeId : getAiMarkerTypeIds())
        {
            if (aiTypeId.equals(typeId))
            {
                return true;
            }
        }
        return false;
    }
}
