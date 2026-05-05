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
    private final int matchStartLine;
    private final int matchStartColumn;
    private final int matchEndLine;
    private final int matchEndColumn;

    public ReplaceResult(String updatedContent, int addedLines, int removedLines, boolean success)
    {
        this(updatedContent, addedLines, removedLines, success, false);
    }

    public ReplaceResult(String updatedContent, int addedLines, int removedLines, boolean success,
        boolean multipleOccurrences)
    {
        this(updatedContent, addedLines, removedLines, success, multipleOccurrences, 0, 0, 0, 0);
    }

    public ReplaceResult(String updatedContent, int addedLines, int removedLines, boolean success,
        boolean multipleOccurrences, int matchStartLine, int matchStartColumn, int matchEndLine, int matchEndColumn)
    {
        this.updatedContent = updatedContent;
        this.addedLines = addedLines;
        this.removedLines = removedLines;
        this.success = success;
        this.multipleOccurrences = multipleOccurrences;
        this.matchStartLine = matchStartLine;
        this.matchStartColumn = matchStartColumn;
        this.matchEndLine = matchEndLine;
        this.matchEndColumn = matchEndColumn;
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

    /**
     * @return 1-based start line of the matched original fragment in the source content,
     *         or 0 if position is unknown / not applicable.
     */
    public int getMatchStartLine()
    {
        return matchStartLine;
    }

    /**
     * @return 1-based start column of the matched original fragment, or 0 if unknown.
     */
    public int getMatchStartColumn()
    {
        return matchStartColumn;
    }

    /**
     * @return 1-based end line of the matched original fragment, or 0 if unknown.
     */
    public int getMatchEndLine()
    {
        return matchEndLine;
    }

    /**
     * @return 1-based end column (exclusive — one past the last matched character),
     *         or 0 if unknown.
     */
    public int getMatchEndColumn()
    {
        return matchEndColumn;
    }
}
