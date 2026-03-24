package com.e1c.edt.ai.tools;

/**
 * Contains result of replacement operation
 */
public class ReplaceResult
{
    private final String updatedContent;
    private final int addedLines;
    private final int removedLines;
    private final boolean success;
    private final boolean multipleOccurrences;

    public ReplaceResult(String updatedContent, int addedLines, int removedLines, boolean success)
    {
        this(updatedContent, addedLines, removedLines, success, false);
    }

    public ReplaceResult(String updatedContent, int addedLines, int removedLines, boolean success,
        boolean multipleOccurrences)
    {
        this.updatedContent = updatedContent;
        this.addedLines = addedLines;
        this.removedLines = removedLines;
        this.success = success;
        this.multipleOccurrences = multipleOccurrences;
    }

    public String getUpdatedContent()
    {
        return updatedContent;
    }

    public int getAddedLines()
    {
        return addedLines;
    }

    public int getRemovedLines()
    {
        return removedLines;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public boolean hasMultipleOccurrences()
    {
        return multipleOccurrences;
    }
}