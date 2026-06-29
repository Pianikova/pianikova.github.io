/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

/**
 * Immutable payload for a diff preview: the snapshot taken at RENDER time of an {@code Edit} tool
 * call. Holds the current ("original") and proposed ("new") full file contents so the IDE side can
 * open a read-only compare view without re-reading or re-computing anything.
 */
public class DiffPreview
{
    private final String filePath;
    private final String displayName;
    private final String originalContent;
    private final String proposedContent;

    /**
     * @param filePath absolute path of the edited file (for labels/tooltips and type detection)
     * @param displayName human-friendly name (EDT breadcrumb) used as the compare tab title and
     *            the compare element name
     * @param originalContent current full content of the file (left side)
     * @param proposedContent proposed full content after the edit (right side)
     */
    public DiffPreview(String filePath, String displayName, String originalContent, String proposedContent)
    {
        this.filePath = filePath;
        this.displayName = displayName;
        this.originalContent = originalContent;
        this.proposedContent = proposedContent;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getOriginalContent()
    {
        return originalContent;
    }

    public String getProposedContent()
    {
        return proposedContent;
    }
}
