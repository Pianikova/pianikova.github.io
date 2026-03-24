package com.e1c.edt.ai.tools;

public interface IContentReplacer
{
    /**
     * Replaces content in the current content with option to replace all or single occurrence
     *
     * @param currentContent original file content
     * @param originContent content to replace
     * @param newContent replacement content
     * @param lineDelimiter line delimiter to use
     * @param replaceAll if true, replaces all occurrences; if false, replaces single occurrence
     * @return result with updated content and statistics
     */
    ReplaceResult replace(String currentContent, String originContent, String newContent, String lineDelimiter, boolean replaceAll);
}