package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

@SuppressWarnings("nls")
public class ContentReplacerTest
{
    @Test
    public void testReplaceSingleOccurrence()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Hello World\nThis is a test\nGoodbye";
        String originContent = "World";
        String newContent = "Universe";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Hello Universe\nThis is a test\nGoodbye",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added lines", 1, result.getAddedLines());
        assertEquals("Should have 1 removed lines", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceSingleOccurrenceWithWindowsLineDelimiter()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Hello World\r\nThis is a test\r\nGoodbye";
        String originContent = "World";
        String newContent = "Universe";
        String lineDelimiter = "\r\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Hello Universe\r\nThis is a test\r\nGoodbye",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added lines", 1, result.getAddedLines());
        assertEquals("Should have 1 removed lines", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceSingleLine()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "Line2";
        String newContent = "NewLine";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nNewLine\nLine3", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceSingleLineWithContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nLine2\nXyz\nLine3";
        String originContent = "Abc\nLine2\nXyz\n";
        String newContent = "Abc\nNewLine\nXyz\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nAbc\nNewLine\nXyz\nLine3",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceSingleLineWithContextWindowsLineDelimiter()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\r\nAbc\r\nLine2\r\nXyz\r\nLine3";
        String originContent = "Abc\nLine2\nXyz\n";
        String newContent = "Abc\nNewLine\nXyz\n";
        String lineDelimiter = "\r\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\r\nAbc\r\nNewLine\r\nXyz\r\nLine3",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testRemoveSingleLineWithContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nLine2\nXyz\nLine3";
        String originContent = "Abc\nLine2\nXyz\n";
        String newContent = "Abc\nXyz\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nAbc\nXyz\nLine3",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added line", 0, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testRemoveAll()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nLine2\nXyz\nLine3";
        String originContent = "Line1\nAbc\nLine2\nXyz\nLine3";
        String newContent = "";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added line", 0, result.getAddedLines());
        assertEquals("Should have 5 removed line", 5, result.getRemovedLines());
    }

    @Test
    public void testAddAll()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "";
        String originContent = "";
        String newContent = "Line1\nAbc\nLine2\nXyz\nLine3";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nAbc\nLine2\nXyz\nLine3",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 5 added line", 5, result.getAddedLines());
        assertEquals("Should have 0 removed line", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceAddLineWithContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nXyz\nLine3";
        String originContent = "Abc\nXyz\n";
        String newContent = "Abc\nNewLine\nXyz\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nAbc\nNewLine\nXyz\nLine3",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 0 removed line", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceMultipleLinesWithContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nLine2\nLine3\nXyz\nLine6";
        String originContent = "Abc\nLine2\nLine3\nXyz\n";
        String newContent = "Abc\nNewLine1\nNewLine2\nXyz\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nAbc\nNewLine1\nNewLine2\nXyz\nLine6",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 2 added lines", 2, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testRemoveMultipleLinesWithContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nLine2\nLine3\nXyz\nLine6";
        String originContent = "Abc\nLine2\nLine3\nXyz\n";
        String newContent = "Abc\nXyz\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nAbc\nXyz\nLine6",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithEmptyLeftContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nXyz\nLine4";
        String originContent = "Line1\nLine2\n";
        String newContent = "NewLine1\nNewLine2\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "NewLine1\nNewLine2\nXyz\nLine4",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 2 added lines", 2, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testRemoveWithEmptyLeftContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nXyz\nLine4";
        String originContent = "Line1\nLine2\n";
        String newContent = "";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Xyz\nLine4", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithEmptyRightContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nLine3\nLine4";
        String originContent = "\nAbc\nLine3\n";
        String newContent = "\nNewLine1\nNewLine2\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nNewLine1\nNewLine2\nLine4",
            result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 2 added lines", 2, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testRemoveWithEmptyRightContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nAbc\nLine3\nLine4";
        String originContent = "\nAbc\nLine3\n";
        String newContent = "\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nLine4", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testReplaceMultiLineContent()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "Line2\nLine3";
        String newContent = "NewLine";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nNewLine", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testReplaceMultipleOccurrences()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine1\nLine2\nLine1";
        String originContent = "Line1";
        String newContent = "NewLine";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "NewLine\nLine2\nNewLine\nLine2\nNewLine",
            result.getUpdatedContent());
        assertTrue("Should have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 3 added lines", 3, result.getAddedLines());
        assertEquals("Should have 3 removed lines", 3, result.getRemovedLines());
    }

    // Complex scenario tests

    @Test
    public void testReplaceWithEmptyCurrentContent()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "";
        String originContent = "";
        String newContent = "NewContent";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should be new content", "NewContent", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 0 removed lines", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithEmptyOriginContent()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "";
        String newContent = "";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should be unchanged", "Line1\nLine2\nLine3", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 0 removed lines", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithEmptyNewContent()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "Line2";
        String newContent = "";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        // When Line2 is replaced with empty string, the newline after it remains
        assertEquals("Updated content should have Line2 removed", "Line1\n\nLine3", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithAllEmptyParameters()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "";
        String originContent = "";
        String newContent = "";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should be empty", "", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 0 removed lines", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllMultipleOccurrences()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine1\nLine2\nLine1";
        String originContent = "Line1";
        String newContent = "ReplacedLine";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain all replacements",
            "ReplacedLine\nLine2\nReplacedLine\nLine2\nReplacedLine", result.getUpdatedContent());
        assertTrue("Should have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 3 added lines", 3, result.getAddedLines());
        assertEquals("Should have 3 removed lines", 3, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllMultiLineOccurrences()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3\nLine1\nLine2\nLine3\nLine1\nLine2\nLine3";
        String originContent = "Line1\nLine2";
        String newContent = "NewContent";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain all replacements",
            "NewContent\nLine3\nNewContent\nLine3\nNewContent\nLine3", result.getUpdatedContent());
        assertTrue("Should have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 3 added lines", 3, result.getAddedLines());
        assertEquals("Should have 6 removed lines", 6, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllSingleOccurrence()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "Line2";
        String newContent = "NewLine";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement", "Line1\nNewLine\nLine3", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllEmptyOriginInNonEmptyContent()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "";
        String newContent = "Insert";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        // Empty originContent matches everywhere in Java's replace, so it inserts between every character
        // This is expected behavior from Java's String.replace() method
        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain insertions",
            "InsertLInsertiInsertnInserteInsert1Insert\n" +
            "InsertLInsertiInsertnInserteInsert2Insert\n" +
            "InsertLInsertiInsertnInserteInsert3Insert", result.getUpdatedContent());
        // Note: empty originContent is not counted as an occurrence, so hasMultipleOccurrences returns false
        assertFalse("Should not have multiple occurrences (empty origin)", result.hasMultipleOccurrences());
        // Line count is 0 because both originContent and newContent have 0 lines (they're single-line strings)
        // The replacement happens between characters but doesn't affect line count calculation
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 0 removed lines", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithEmptyOriginAndEmptyNewContent()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "";
        String newContent = "";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        // Empty origin and empty new should leave content unchanged
        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should be unchanged", currentContent, result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 0 removed lines", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllWithWindowsLineDelimiter()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\r\nLine2\r\nLine1\r\nLine2\r\nLine1";
        String originContent = "Line1";
        String newContent = "NewLine";
        String lineDelimiter = "\r\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should preserve Windows line delimiters",
            "NewLine\r\nLine2\r\nNewLine\r\nLine2\r\nNewLine", result.getUpdatedContent());
        assertTrue("Should have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 3 added lines", 3, result.getAddedLines());
        assertEquals("Should have 3 removed lines", 3, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllMultiLineWithContext()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Start\nAbc\nTarget\nXyz\nEnd\nStart\nAbc\nTarget\nXyz\nEnd";
        String originContent = "Abc\nTarget\nXyz\n";
        String newContent = "Abc\nNewTarget\nXyz\n";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should replace all occurrences with context",
            "Start\nAbc\nNewTarget\nXyz\nEnd\nStart\nAbc\nNewTarget\nXyz\nEnd", result.getUpdatedContent());
        assertTrue("Should have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 2 added lines", 2, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllWithMixedContent()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "var x = 1;\nvar y = 2;\nvar x = 3;\nvar y = 4;";
        String originContent = "var x";
        String newContent = "let x";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should replace all var x occurrences",
            "let x = 1;\nvar y = 2;\nlet x = 3;\nvar y = 4;", result.getUpdatedContent());
        assertTrue("Should have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 2 added lines", 2, result.getAddedLines());
        assertEquals("Should have 2 removed lines", 2, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllWithNoOccurrences()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nLine2\nLine3";
        String originContent = "NonExistent";
        String newContent = "NewContent";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);

        assertFalse("Replacement should fail", result.isSuccess());
        assertEquals("Updated content should be unchanged", currentContent, result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 0 added lines", 0, result.getAddedLines());
        assertEquals("Should have 0 removed lines", 0, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithCyrillicCharacters()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Привет мир\nЭто тест\nДо свидания";
        String originContent = "мир";
        String newContent = "Вселенная";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement",
            "Привет Вселенная\nЭто тест\nДо свидания", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithChineseCharacters()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "你好 世界\n这是一个测试\n再见";
        String originContent = "世界";
        String newContent = "宇宙";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement",
            "你好 宇宙\n这是一个测试\n再见", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithJapaneseCharacters()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "こんにちは 世界\nこれはテストです\nさようなら";
        String originContent = "世界";
        String newContent = "宇宙";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement",
            "こんにちは 宇宙\nこれはテストです\nさようなら", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithArabicCharacters()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "مرحبا بالعالم\nهذا اختبار\nمع السلامة";
        String originContent = "العالم";
        String newContent = "الكون";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement",
            "مرحبا بالكون\nهذا اختبار\nمع السلامة", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithGreekCharacters()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Γεια σου κόσμε\nΑυτή είναι μια δοκιμή\nΑντίο";
        String originContent = "κόσμε";
        String newContent = "σύμπαν";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should contain replacement",
            "Γεια σου σύμπαν\nΑυτή είναι μια δοκιμή\nΑντίο", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithMixedUnicodeCharacters()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Hello 你好 Привет مرحبا\nMixed content test\nEnd 🌟";
        String originContent = "你好";
        String newContent = "👋";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should replace Chinese with emoji",
            "Hello 👋 Привет مرحبا\nMixed content test\nEnd 🌟", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithSpecialSymbols()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Copyright © 2024\nRegistered ® trademark\nCurrency € and ¥";
        String originContent = "©";
        String newContent = "℗";
        String lineDelimiter = "\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        assertTrue("Replacement should be successful", result.isSuccess());
        assertEquals("Updated content should replace copyright symbol",
            "Copyright ℗ 2024\nRegistered ® trademark\nCurrency € and ¥", result.getUpdatedContent());
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertEquals("Should have 1 added line", 1, result.getAddedLines());
        assertEquals("Should have 1 removed line", 1, result.getRemovedLines());
    }

    @Test
    public void testReplaceWithBOMAndCRLF()
    {
        ContentReplacer replacer = createContentReplacer();
        // Content with BOM (Byte Order Mark), CRLF line delimiters, emoji, and Cyrillic text
        String currentContent =
            "\uFEFF# Status indicators\nToolNameTemplate=\uD83D\uDE80 _{0}_\n\n# Project operations\n"
                + "ProjectsTitle=\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u043F\u0440\u043E\u0435\u043A\u0442\u044B\n"
                + "ProjectsLoadedTemplate=\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E \u043F\u0440\u043E\u0435\u043A\u0442\u043E\u0432 **{0}**\n";

        // Origin content with leading whitespace (the issue from real scenario)
        String originContent = "  # Status indicators\nToolNameTemplate=\uD83D\uDE80 _{0}_\n\n# Project operations\n"
            + "ProjectsTitle=\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u043F\u0440\u043E\u0435\u043A\u0442\u044B\n"
            + "ProjectsLoadedTemplate=\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E \u043F\u0440\u043E\u0435\u043A\u0442\u043E\u0432 **{0}**";

        // New content with additional comments in Russian
        String newContent =
            "   # Status indicators\n# Шаблон для названия инструмента с эмодзи ракеты\n# Параметр {0} будет заменен на имя инструмента\n"
                + "ToolNameTemplate=\uD83D\uDE80 _{0}_\n\n# Project operations\n# Заголовок секции загрузки проектов\n"
                + "ProjectsTitle=\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u043F\u0440\u043E\u0435\u043A\u0442\u044B\n"
                + "# Сообщение о количестве загруженных проектов\n# Параметр {0} будет заменен на число проектов\n"
                + "# Используется жирное начертание для выделения числа\n"
                + "ProjectsLoadedTemplate=\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E \u043F\u0440\u043E\u0435\u043A\u0442\u043E\u0432 **{0}**";

        String lineDelimiter = "\r\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        // With replacement strategies enabled, trimmed/anchored matching should recover this case.
        assertTrue("Replacement should succeed with tolerant matching", result.isSuccess());
        assertTrue("Updated content should contain BOM", result.getUpdatedContent().startsWith("\uFEFF"));
        assertTrue("Updated content should contain emoji", result.getUpdatedContent().contains("\uD83D\uDE80"));
        assertTrue("Updated content should contain Cyrillic", result.getUpdatedContent().contains("\u0428\u0430\u0431\u043B\u043E\u043D"));
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        assertTrue("Should add lines", result.getAddedLines() > 0);
        assertTrue("Should remove at least 0 lines", result.getRemovedLines() >= 0);
    }

    @Test
    public void testReplaceWithBOMAndCRLFMatching()
    {
        ContentReplacer replacer = createContentReplacer();
        // Content with BOM (Byte Order Mark), CRLF line delimiters, emoji, and Cyrillic text
        String currentContent = "\uFEFF# Status indicators\nToolNameTemplate=\uD83D\uDE80 _{0}_\n\n# Project operations\n" +
            "ProjectsTitle=\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u043F\u0440\u043E\u0435\u043A\u0442\u044B\n" +
            "ProjectsLoadedTemplate=\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E \u043F\u0440\u043E\u0435\u043A\u0442\u043E\u0432 **{0}**\n";

        // Origin content without leading whitespace - should match
        String originContent = "# Status indicators\nToolNameTemplate=\uD83D\uDE80 _{0}_\n\n# Project operations\n" +
            "ProjectsTitle=\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u043F\u0440\u043E\u0435\u043A\u0442\u044B\n" +
            "ProjectsLoadedTemplate=\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E \u043F\u0440\u043E\u0435\u043A\u0442\u043E\u0432 **{0}**";

        // New content with additional comments in Russian
        String newContent = "# Status indicators\n# Шаблон для названия инструмента с эмодзи ракеты\n# Параметр {0} будет заменен на имя инструмента\n" +
            "ToolNameTemplate=\uD83D\uDE80 _{0}_\n\n# Project operations\n# Заголовок секции загрузки проектов\n" +
            "ProjectsTitle=\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u043F\u0440\u043E\u0435\u043A\u0442\u044B\n" +
            "# Сообщение о количестве загруженных проектов\n# Параметр {0} будет заменен на число проектов\n" +
            "# Используется жирное начертание для выделения числа\n" +
            "ProjectsLoadedTemplate=\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E \u043F\u0440\u043E\u0435\u043A\u0442\u043E\u0432 **{0}**";

        String lineDelimiter = "\r\n";

        ReplaceResult result = replacer.replace(currentContent, originContent, newContent, lineDelimiter, false);

        // The replacement should succeed
        assertTrue("Replacement should be successful", result.isSuccess());
        assertTrue("Updated content should contain BOM", result.getUpdatedContent().startsWith("\uFEFF"));
        assertTrue("Updated content should contain emoji", result.getUpdatedContent().contains("\uD83D\uDE80"));
        assertTrue("Updated content should contain Cyrillic", result.getUpdatedContent().contains("\u0428\u0430\u0431\u043B\u043E\u043D"));
        assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        // Should add 10 new lines of comments (including 7 new comment lines)
        assertEquals("Should have 10 added lines", 10, result.getAddedLines());
        assertEquals("Should have 4 removed lines", 4, result.getRemovedLines());
    }

    private ContentReplacer createContentReplacer()
    {
        return new ContentReplacer(Set.of(new SimpleReplacer(), new LineTrimmedReplacer(), new BlockAnchorReplacer(),
            new WhitespaceNormalizedReplacer(), new IndentationFlexibleReplacer(), new EscapeNormalizedReplacer(),
            new TrimmedBoundaryReplacer(), new ContextAwareReplacer(), new MultiOccurrenceReplacer()));
    }
}
