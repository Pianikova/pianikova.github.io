/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for ContentReplacer using template-based line ending matching.
 *
 * <p>This test class verifies that ContentReplacer correctly replaces content
 * using template patterns that can match any line ending format (CRLF, LF, CR)
 * without normalization.</p>
 *
 * @author 1C EDT AI Team
 */
@SuppressWarnings("nls")
public class ContentReplacerTest
{
	private final ContentReplacer replacer = new ContentReplacer();
    private static final String LF = "\n";
    private static final String CRLF = "\r\n";
    private static final String CR = "\r";

	// ========== Template-Based Line Ending Matching Tests ==========

    @Test
	public void shouldReplaceContentWithLFOriginsInLFContent()
	{
		// Given
        var currentContent = "line1\nline2\nline3\n";
        var originContent = "line2";
        var newContent = "modified_line2";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "line1\nmodified_line2\nline3\n", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
	}

	@Test
	public void shouldReplaceContentWithCRLForiginsInCRLFContent()
	{
		// Given
        var currentContent = "line1\r\nline2\r\nline3\r\n";
        var originContent = "line2";
        var newContent = "modified_line2";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, CRLF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "line1\r\nmodified_line2\r\nline3\r\n",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
	}

	@Test
	public void shouldReplaceContentWithMROriginsInCRContent()
	{
		// Given
        var currentContent = "line1\rline2\rline3\r";
        var originContent = "line2";
        var newContent = "modified_line2";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, CR, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "line1\rmodified_line2\rline3\r", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
	}

	@Test
	public void shouldReplaceMultiLineContentWithDifferentLineEndings()
	{
		// Given
		var currentContent = "function test() {\n\treturn true;\n}";
		var originContent = "function test() {\n\treturn true;\n}";
		var newContent = "function test() {\n\treturn false;\n}";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "function test() {\n\treturn false;\n}",
            result.getUpdatedContent());
		Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
		Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
	}

	@Test
	public void shouldHandleMixedLineEndingsInCurrentContent()
	{
		// Given
		var currentContent = "line1\nline2\r\nline3\rline4\n";
		var originContent = "line3";
		var newContent = "modified_line3";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "line1\nline2\r\nmodified_line3\rline4\n",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
	}

	// ========== Single Occurrence Replacement Tests ==========

	@Test
	public void shouldReplaceSingleOccurrenceSuccessfully()
	{
		// Given
		var currentContent = "var x = 10;\nvar y = 20;\nvar z = 30;";
		var originContent = "var y = 20;";
		var newContent = "var y = 25;";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, false);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
		Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertEquals("Content should be modified", "var x = 10;\nvar y = 25;\nvar z = 30;",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
	}

	@Test
	public void shouldFailWhenMultipleOccurrencesFoundForSingleReplacement()
	{
		// Given
        var currentContent = "foo\nbar\nfoo";
        var originContent = "foo";
        var newContent = "baz";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, false);

		// Then
        Assert.assertFalse("Replacement should fail due to multiple occurrences of 'foo'", result.isSuccess());
		Assert.assertTrue("Should detect multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertEquals("Content should remain unchanged", currentContent, result.getUpdatedContent());
        Assert.assertEquals("Should not remove any lines", 0, result.getRemovedLines());
        Assert.assertEquals("Should not add any lines", 0, result.getAddedLines());
	}

	@Test
	public void shouldFailWhenNoOccurrencesFound()
	{
		// Given
		var currentContent = "var x = 10;\nvar y = 20;";
		var originContent = "var z = 30;";
		var newContent = "var z = 35;";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, false);

		// Then
		Assert.assertFalse("Replacement should fail when content not found", result.isSuccess());
		Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertEquals("Content should remain unchanged", currentContent, result.getUpdatedContent());
        Assert.assertEquals("Should not remove any lines", 0, result.getRemovedLines());
        Assert.assertEquals("Should not add any lines", 0, result.getAddedLines());
	}

	// ========== Multiple Occurrence Replacement Tests ==========

	@Test
	public void shouldReplaceAllOccurrencesSuccessfully()
	{
		// Given
		var currentContent = "foo bar foo baz foo";
		var originContent = "foo";
		var newContent = "qux";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "qux bar qux baz qux", result.getUpdatedContent());
        Assert.assertEquals("Should remove 3 lines (each 'foo' counts as 1 line)", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines (each 'qux' counts as 1 line)", 3, result.getAddedLines());
	}

	@Test
	public void shouldReplaceAllMultiLineOccurrences()
	{
		// Given
		var currentContent = "if (condition) {\n\tdoSomething();\n}\nif (condition) {\n\tdoSomething();\n}";
		var originContent = "if (condition) {\n\tdoSomething();\n}";
		var newContent = "if (condition) {\n\tdoSomethingElse();\n}";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified",
            "if (condition) {\n\tdoSomethingElse();\n}\nif (condition) {\n\tdoSomethingElse();\n}",
            result.getUpdatedContent());
		Assert.assertEquals("Should remove 6 lines", 6, result.getRemovedLines());
		Assert.assertEquals("Should add 6 lines", 6, result.getAddedLines());
	}

	@Test
	public void shouldReturnOriginalContentWhenNoOccurrencesFoundForReplaceAll()
	{
		// Given
		var currentContent = "var x = 10;\nvar y = 20;";
		var originContent = "var z = 30;";
		var newContent = "var z = 35;";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
		Assert.assertFalse("Replacement should fail when content not found", result.isSuccess());
        Assert.assertEquals("Content should remain unchanged", currentContent, result.getUpdatedContent());
        Assert.assertEquals("Should not remove any lines", 0, result.getRemovedLines());
        Assert.assertEquals("Should not add any lines", 0, result.getAddedLines());
	}

	// ========== Edge Cases and Error Conditions ==========

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenCurrentContentIsNull()
	{
		// Given
		String currentContent = null;
        var originContent = "test";
        var newContent = "modified";

		// When
		replacer.replace(currentContent, originContent, newContent, LF, true);
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenOriginContentIsNull()
	{
		// Given
		var currentContent = "test content";
		String originContent = null;
        var newContent = "modified";

		// When
		replacer.replace(currentContent, originContent, newContent, LF, true);
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenNewContentIsNull()
	{
		// Given
		var currentContent = "test content";
        var originContent = "test";
		String newContent = null;

		// When
		replacer.replace(currentContent, originContent, newContent, LF, true);
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenLineDelimiterIsNull()
	{
		// Given
		var currentContent = "test content";
        var originContent = "test";
        var newContent = "modified";
		String lineDelimiter = null;

		// When
		replacer.replace(currentContent, originContent, newContent, lineDelimiter, true);
	}

	@Test
	public void shouldHandleEmptyOriginContent()
	{
		// Given
		var currentContent = "test content";
		var originContent = "";
        var newContent = "modified";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
		Assert.assertFalse("Replacement should fail with empty origin content", result.isSuccess());
        Assert.assertEquals("Content should remain unchanged", currentContent, result.getUpdatedContent());
        Assert.assertEquals("Should not remove any lines", 0, result.getRemovedLines());
        Assert.assertEquals("Should not add any lines", 0, result.getAddedLines());
	}

	@Test
	public void shouldHandleEmptyCurrentContent()
	{
		// Given
		var currentContent = "";
        var originContent = "test";
        var newContent = "modified";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
		Assert.assertFalse("Replacement should fail with empty current content", result.isSuccess());
        Assert.assertEquals("Content should remain unchanged", "", result.getUpdatedContent());
        Assert.assertEquals("Should not remove any lines", 0, result.getRemovedLines());
        Assert.assertEquals("Should not add any lines", 0, result.getAddedLines());
	}

	@Test
	public void shouldHandleEmptyNewContent()
	{
		// Given
		var currentContent = "test content";
        var originContent = "test";
		var newContent = "";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
		Assert.assertTrue("Replacement should succeed with empty new content", result.isSuccess());
        Assert.assertEquals("Content should be modified", " content", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines()); // "test" counts as 1 line
        Assert.assertEquals("Should add 0 lines", 0, result.getAddedLines()); // "" counts as 0 lines (empty)
	}

	@Test
	public void shouldHandleMultiLineContentWithMixedEndings()
	{
		// Given
		var currentContent = "line1\r\nline2\nline3\rline4\r\n";
		var originContent = "line3\r";
		var newContent = "modified_line3\n";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "line1\r\nline2\nmodified_line3\nline4\r\n",
            result.getUpdatedContent());
	}

	@Test
	public void shouldHandleSpecialCharactersInContent()
	{
		// Given
		var currentContent = "function test() {\n\treturn \"string with \\\"quotes\\\"\";\n}";
		var originContent = "\treturn \"string with \\\"quotes\\\"\";";
		var newContent = "\treturn \"new string with \\\"quotes\\\"\";";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified",
            "function test() {\n\treturn \"new string with \\\"quotes\\\"\";\n}", result.getUpdatedContent());
	}

	@Test
	public void shouldHandleUnicodeCharacters()
	{
		// Given
		var currentContent = "переменная = 10;\nтестовая строка;\nеще переменная = 20;";
		var originContent = "тестовая строка;";
		var newContent = "измененная строка;";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
        Assert.assertEquals("Content should be modified", "переменная = 10;\nизмененная строка;\nеще переменная = 20;",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
	}

	@Test
	public void shouldHandleVeryLargeContent()
	{
		// Given
		var largeContent = new StringBuilder();
		for (int i = 0; i < 1000; i++)
		{
			largeContent.append("line ").append(i).append("\n");
		}
		var currentContent = largeContent.toString();
		var originContent = "line 500\n";
		var newContent = "modified line 500\n";

		// When
		var result = replacer.replace(currentContent, originContent, newContent, LF, true);

		// Then
        Assert.assertTrue("Replacement should succeed", result.isSuccess());
		Assert.assertTrue("Content should contain modified line", result.getUpdatedContent().contains("modified line 500"));
        Assert.assertFalse("Content should not contain original line 500 with newline",
            result.getUpdatedContent().contains("\nline 500\n"));
        Assert.assertTrue("Content should contain context around line 500",
            result.getUpdatedContent().contains("line 499\nmodified line 500\nline 501"));
        Assert.assertFalse("Content should not contain original context",
            result.getUpdatedContent().contains("line 499\nline 500\nline 501"));
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
	}

	@Test
	public void shouldHandleConsecutiveReplacements()
	{
		// Given
		var currentContent = "a b c d e";

		// When - first replacement
		var result1 = replacer.replace(currentContent, "b", "x", LF, true);

		// Then - first replacement result
		Assert.assertTrue("First replacement should succeed", result1.isSuccess());
		Assert.assertEquals("First replacement content should be correct", "a x c d e", result1.getUpdatedContent());

		// When - second replacement on result
		var result2 = replacer.replace(result1.getUpdatedContent(), "x", "y", LF, true);

		// Then - second replacement result
		Assert.assertTrue("Second replacement should succeed", result2.isSuccess());
		Assert.assertEquals("Second replacement content should be correct", "a y c d e", result2.getUpdatedContent());
	}

    // ===== Complex Mixed Line Ending Tests =====

    @Test
    public void shouldReplaceWhenOriginHasCRLFAndCurrentHasLF()
    {
        // Given - origin uses CRLF, current uses LF
        var currentContent = "function test() {\n\treturn true;\n}";
        var originContent = "function test() {\r\n\treturn true;\r\n}";
        var newContent = "function updated() {\n\treturn false;\n}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed across line ending formats", result.isSuccess());
        Assert.assertEquals("Content should be replaced correctly", "function updated() {\n\treturn false;\n}",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWhenOriginHasLFAndCurrentHasCRLF()
    {
        // Given - origin uses LF, current uses CRLF
        var currentContent = "function test() {\r\n\treturn true;\r\n}";
        var originContent = "function test() {\n\treturn true;\n}";
        var newContent = "function updated() {\r\n\treturn false;\r\n}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, CRLF, true);

        // Then
        Assert.assertTrue("Replacement should succeed across line ending formats", result.isSuccess());
        Assert.assertEquals("Content should be replaced correctly", "function updated() {\r\n\treturn false;\r\n}",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWhenOriginHasCRAndCurrentHasLF()
    {
        // Given - origin uses CR, current uses LF
        var currentContent = "function test() {\n\treturn true;\n}";
        var originContent = "function test() {\r\treturn true;\r}";
        var newContent = "function updated() {\n\treturn false;\n}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed across line ending formats", result.isSuccess());
        Assert.assertEquals("Content should be replaced correctly", "function updated() {\n\treturn false;\n}",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWhenOriginHasMixedEndingsAndCurrentIsUniform()
    {
        // Given - origin uses mixed endings, current uses LF only
        var currentContent = "a\nb\nc\nd";
        var originContent = "a\r\nb\nc\rd";
        var newContent = "x\ny\nz\nw";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed across mixed origin endings", result.isSuccess());
        Assert.assertEquals("Content should be replaced correctly", "x\ny\nz\nw", result.getUpdatedContent());
        Assert.assertEquals("Should remove 4 lines", 4, result.getRemovedLines());
        Assert.assertEquals("Should add 4 lines", 4, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWithMixedLineEndingsInCurrentContent()
    {
        // Given - current content has mixed line endings, origin has consistent LF
        var currentContent = "line1\nline2\r\nline3\rline4\nline5";
        var originContent = "line3";
        var newContent = "modified_line3";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with mixed line endings", result.isSuccess());
        Assert.assertEquals("Mixed line endings should be preserved", "line1\nline2\r\nmodified_line3\rline4\nline5",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldReplaceMultiLineWithDifferentLineEndingsBetweenOriginAndNew()
    {
        // Given - origin uses LF, new content uses CRLF
        var currentContent = "if (condition) {\n\tdoSomething();\n}";
        var originContent = "if (condition) {\n\tdoSomething();\n}";
        var newContent = "if (condition) {\r\n\tdoSomethingElse();\r\n}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with different line endings", result.isSuccess());
        Assert.assertEquals("New content line endings should be preserved",
            "if (condition) {\r\n\tdoSomethingElse();\r\n}", result.getUpdatedContent());
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWhenNewContentHasDifferentLineEndingsThanCurrent()
    {
        // Given - current uses LF, new content uses CR
        var currentContent = "var x = 10;\nvar y = 20;\nvar z = 30;";
        var originContent = "var y = 20;";
        var newContent = "var y = 25;\rvar x = 15;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with CR line endings in new content", result.isSuccess());
        Assert.assertEquals("CR line endings should be preserved in new content",
            "var x = 10;\nvar y = 25;\rvar x = 15;\nvar z = 30;", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 2 lines", 2, result.getAddedLines());
    }

    @Test
    public void shouldReplaceComplexMultiLineWithMixedEndings()
    {
        // Given - complex scenario with multiple line ending types
        var currentContent =
            "class Test {\n\tpublic void method1() {\r\n\t\t// LF and CRLF mixed\r\n\t}\r\n\tpublic void method2() {\n\t\t// More mixed endings\r\n\t}\r}";
        var originContent = "\tpublic void method1() {\r\n\t\t// LF and CRLF mixed\r\n\t}";
        var newContent = "\tpublic void method1() {\n\t\t// Updated with LF only\n\t}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Replacement should succeed in complex mixed scenario", result.isSuccess());
        Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertTrue("Should preserve mixed line endings correctly",
            result.getUpdatedContent().contains("method1() {\n\t\t// Updated with LF only\n\t}"));
        Assert.assertTrue("Should preserve other methods with their original endings",
            result.getUpdatedContent().contains("method2() {\n\t\t// More mixed endings\r\n\t}"));
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWithOriginEndingInDifferentLineEndingThanCurrentStart()
    {
        // Given - origin ends with CRLF but is followed by content starting with different ending
        var currentContent = "line1\nline2\r\nline3";
        var originContent = "line2\r\n";
        var newContent = "modified_line2\n";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should handle boundary line endings", result.isSuccess());
        Assert.assertEquals("Should replace and preserve line endings correctly", "line1\nmodified_line2\nline3",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldReplaceMultipleOccurrencesWithDifferentLineEndings()
    {
        // Given - clearer separation between occurrences with different line endings
        var currentContent = "first\nstart\nblock1\nend\nsecond\nstart\nblock1\nend\rthird\nstart\nblock1\nend";
        var originContent = "start\nblock1\nend";
        var newContent = "START\r\nBLOCK1\r\nEND";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should find all three occurrences", result.isSuccess());
        // All three occurrences should be found regardless of surrounding line endings
        Assert.assertEquals("Should replace all three occurrences with clear separators",
            "first\nSTART\r\nBLOCK1\r\nEND\nsecond\nSTART\r\nBLOCK1\r\nEND\rthird\nSTART\r\nBLOCK1\r\nEND",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 9 lines (3 occurrences × 3 lines each)", 9, result.getRemovedLines());
        Assert.assertEquals("Should add 9 lines (3 occurrences × 3 lines each)", 9, result.getAddedLines());
    }

    @Test
    public void shouldHandleVeryComplexMixedEndingScenario()
    {
        // Given - extremely complex mixed scenario
        var currentContent =
            "// File header\r\npackage test;\n\nimport java.util.*;\r\n\r\nclass Test {\n\tpublic Test() {\r\n\t\t// Constructor\n\t}\n\n\tpublic void method() {\r\n\t\tSystem.out.println(\"test\");\n\t}\r\n}";
        var originContent = "\tpublic void method() {\r\n\t\tSystem.out.println(\"test\");\n\t}";
        var newContent = "\tpublic void method() {\n\t\tSystem.out.println(\"updated\");\r\n\t\treturn;\n\t}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Replacement should succeed in very complex scenario", result.isSuccess());
        Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertTrue("Should preserve file structure",
            result.getUpdatedContent().startsWith("// File header\r\npackage test;"));
        Assert.assertTrue("Should update method correctly",
            result.getUpdatedContent().contains("System.out.println(\"updated\");"));
        Assert.assertTrue("Should preserve class ending", result.getUpdatedContent().endsWith("}\r\n}"));
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 4 lines", 4, result.getAddedLines());
    }
}
