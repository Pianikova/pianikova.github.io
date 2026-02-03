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

    // ===== Invisible Character Handling Tests =====

    @Test
    public void shouldReplaceContentWithDifferentSpacing()
    {
        // Given - origin has spaces, current has tabs
        var currentContent = "\tvar\tx\t=\t10;\n";
        var originContent = "var x = 10;";
        var newContent = "var y = 20;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with different spacing", result.isSuccess());
        // Note: The leading tab from currentContent is preserved since it's not part of the match
        Assert.assertEquals("Content should be modified", "\tvar y = 20;\n", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldReplaceContentWithMixedSpacesAndTabs()
    {
        // Given - mixed spaces and tabs in current content
        var currentContent = " \t var \t x \t = \t 10; \t \n";
        var originContent = "var x = 10;";
        var newContent = "var y = 20;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with mixed spacing", result.isSuccess());
        // Note: Leading " \t " and trailing " \t " are preserved since they're not part of the match
        Assert.assertEquals("Content should be modified", " \t var y = 20; \t \n", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWhenOriginHasExtraSpaces()
    {
        // Given - origin has extra spaces
        var currentContent = "var x = 10;";
        var originContent = "var  x  =  10;";
        var newContent = "var y = 20;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with extra spaces in origin", result.isSuccess());
        Assert.assertEquals("Content should be modified", "var y = 20;", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWhenCurrentHasExtraSpaces()
    {
        // Given - current has extra spaces
        var currentContent = "var  x  =  10;";
        var originContent = "var x = 10;";
        var newContent = "var y = 20;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with extra spaces in current", result.isSuccess());
        // Note: The replacement uses newContent as-is, so it doesn't preserve extra spaces from currentContent
        Assert.assertEquals("Content should be modified", "var y = 20;", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldReplaceMultiLineContentWithDifferentIndentation()
    {
        // Given - origin has different indentation than current
        var currentContent = "\tfunction test() {\n\t\treturn true;\n\t}\n";
        var originContent = "  function test() {\n    return true;\n  }"; // spaces instead of tabs
        var newContent = "  function test() {\n    return false;\n  }";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with different indentation", result.isSuccess());
        // Note: The replacement uses newContent as-is (with spaces), not preserving current content's indentation
        Assert.assertEquals("Content should be modified", "  function test() {\n    return false;\n  }\n",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
    }

    @Test
    public void shouldMatchContentWithDifferentWhitespaceTypes()
    {
        // Given - origin has spaces, current has tabs, both should match
        var currentContent = "\tvar\tx\t=\t10;\n";
        var originContent = "var x = 10;";
        var newContent = "var x = 20;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with different whitespace types", result.isSuccess());
        // Note: The leading tab from currentContent is preserved since it's not part of the match
        Assert.assertEquals("Content should be modified with newContent", "\tvar x = 20;\n", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldMatchContentWithMultipleSpacesVersusSingleTab()
    {
        // Given - origin has multiple spaces, current has single tab
        var currentContent = "\tvalue\n";
        var originContent = "    value"; // 4 spaces
        var newContent = "newValue";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with different whitespace amounts", result.isSuccess());
        Assert.assertEquals("Content should be modified", "newValue\n", result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceWithWhitespaceFlexibilityInComplexScenario()
    {
        // Given - realistic scenario where origin and current have different whitespace
        var currentContent = "if (x > 0) {\n\t\t doSomething();\n\t\t}\n";
        var originContent = "if (x > 0) {\n  doSomething();\n\t\t}";
        var newContent = "if (x > 0) {\n  doSomethingElse();\n}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with flexible whitespace", result.isSuccess());
        // Note: The replacement uses newContent as-is (with spaces), not preserving current content's indentation
        Assert.assertEquals("Content should be modified", "if (x > 0) {\n  doSomethingElse();\n}\n",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 3 lines", 3, result.getRemovedLines());
        Assert.assertEquals("Should add 3 lines", 3, result.getAddedLines());
    }

    @Test
    public void shouldReplaceSingleOccurrenceWithDifferentWhitespace()
    {
        // Given - origin has spaces, current has tabs, single occurrence replacement
        var currentContent = "\tvar\tx\t=\t10;\nvar y = 20;\n";
        var originContent = "var x = 10;";
        var newContent = "var x = 15;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Replacement should succeed with different whitespace", result.isSuccess());
        Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        // Note: The leading tab from currentContent is preserved since it's not part of the match
        Assert.assertEquals("Content should be modified", "\tvar x = 15;\nvar y = 20;\n", result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldNotMatchWhenWhitespaceDifferenceIsSignificant()
    {
        // Given - origin has whitespace, current doesn't (no whitespace at all)
        var currentContent = "varx=10;\n";
        var originContent = "var x = 10;";
        var newContent = "var x = 20;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertFalse("Replacement should fail when no whitespace in current", result.isSuccess());
        Assert.assertEquals("Content should remain unchanged", "varx=10;\n", result.getUpdatedContent());
        Assert.assertEquals("Should not remove any lines", 0, result.getRemovedLines());
        Assert.assertEquals("Should not add any lines", 0, result.getAddedLines());
    }

    @Test
    public void shouldReplaceWithMixedWhitespaceInMultiLineContent()
    {
        // Given - complex multi-line with different whitespace patterns
        var currentContent = "class Test {\n\t\tvoid method1() {\r\n\t\t\t// code\n\t\t}\n}";
        var originContent = "class Test {\n  void method1() {\n    // code\n  }\n}";
        var newContent = "class Test {\n  void method2() {\n    // new code\n  }\n}";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Replacement should succeed with mixed multi-line whitespace", result.isSuccess());
        Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertTrue("Content should contain new method name",
            result.getUpdatedContent().contains("void method2()"));
    }

    // ===== Special Regex Characters Handling Tests =====

    @Test
    public void shouldReplaceContentWithDollarSign()
    {
        // Given - content contains dollar sign (end-of-line anchor in regex)
        var currentContent = "var price = $10.00;\nvar total = $50.00;\n";
        var originContent = "var price = $10.00;";
        var newContent = "var cost = $15.00;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with dollar sign", result.isSuccess());
        Assert.assertEquals("Content should be modified", "var cost = $15.00;\nvar total = $50.00;\n",
            result.getUpdatedContent());
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldReplaceContentWithCaretSign()
    {
        // Given - content contains caret sign (start-of-line anchor in regex)
        var currentContent = "var ^value = 100;\nvar ^count = 5;\n";
        var originContent = "var ^value = 100;";
        var newContent = "var ^result = 200;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with caret sign", result.isSuccess());
        Assert.assertEquals("Content should be modified", "var ^result = 200;\nvar ^count = 5;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithAsterisk()
    {
        // Given - content contains asterisk (quantifier in regex)
        var currentContent = "int* ptr = null;\nString* str = null;\n";
        var originContent = "int* ptr = null;";
        var newContent = "float* ptr = null;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with asterisk", result.isSuccess());
        Assert.assertEquals("Content should be modified", "float* ptr = null;\nString* str = null;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithPlusSign()
    {
        // Given - content contains plus sign (quantifier in regex)
        var currentContent = "x + y = z;\n";
        var originContent = "x + y = z;";
        var newContent = "a + b = c;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with plus sign", result.isSuccess());
        Assert.assertEquals("Content should be modified", "a + b = c;\n", result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithQuestionMark()
    {
        // Given - content contains question mark (quantifier in regex)
        var currentContent = "int? value = null;\n";
        var originContent = "int? value = null;";
        var newContent = "String? value = null;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with question mark", result.isSuccess());
        Assert.assertEquals("Content should be modified", "String? value = null;\n", result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithDot()
    {
        // Given - content contains dot (any character in regex)
        var currentContent = "obj.method();\nobj.property = value;\n";
        var originContent = "obj.method();";
        var newContent = "obj.call();";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with dot", result.isSuccess());
        Assert.assertEquals("Content should be modified", "obj.call();\nobj.property = value;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithSquareBrackets()
    {
        // Given - content contains square brackets (character class in regex)
        var currentContent = "arr[index] = value;\nlist[0] = item;\n";
        var originContent = "arr[index] = value;";
        var newContent = "map[key] = value;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with square brackets", result.isSuccess());
        Assert.assertEquals("Content should be modified", "map[key] = value;\nlist[0] = item;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithCurlyBraces()
    {
        // Given - content contains curly braces (quantifier in regex)
        var currentContent = "regex = \"a{3}\";\npattern = \"b{2}\";\n";
        var originContent = "regex = \"a{3}\";";
        var newContent = "regex = \"x{5}\";";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with curly braces", result.isSuccess());
        Assert.assertEquals("Content should be modified", "regex = \"x{5}\";\npattern = \"b{2}\";\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithParentheses()
    {
        // Given - content contains parentheses (grouping in regex)
        var currentContent = "func(a, b);\ncall(x, y);\n";
        var originContent = "func(a, b);";
        var newContent = "func(p, q);";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with parentheses", result.isSuccess());
        Assert.assertEquals("Content should be modified", "func(p, q);\ncall(x, y);\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithPipe()
    {
        // Given - content contains pipe (alternation in regex)
        var currentContent = "value = a | b;\nresult = x | y;\n";
        var originContent = "value = a | b;";
        var newContent = "value = c | d;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with pipe", result.isSuccess());
        Assert.assertEquals("Content should be modified", "value = c | d;\nresult = x | y;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithBackslash()
    {
        // Given - content contains backslash (escape character in regex)
        var currentContent = "path = \"C:\\\\Users\\\\test\";\nfile = \"D:\\\\Data\\\\file.txt\";\n";
        var originContent = "path = \"C:\\\\Users\\\\test\";";
        var newContent = "path = \"C:\\\\Users\\\\new\";";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with backslash", result.isSuccess());
        Assert.assertEquals("Content should be modified", "path = \"C:\\\\Users\\\\new\";\nfile = \"D:\\\\Data\\\\file.txt\";\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithMultipleSpecialCharacters()
    {
        // Given - content contains multiple special regex characters
        var currentContent = "regex = \"a*b+c?d{2}e[f]g|h\\\\i\";\npattern = \"x*y+z?w{2}v[u]t|s\\\\r\";\n";
        var originContent = "regex = \"a*b+c?d{2}e[f]g|h\\\\i\";";
        var newContent = "regex = \"1*2+3?4{5}6[7]8|9\\\\0\";";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with multiple special characters", result.isSuccess());
        Assert.assertEquals("Content should be modified", "regex = \"1*2+3?4{5}6[7]8|9\\\\0\";\npattern = \"x*y+z?w{2}v[u]t|s\\\\r\";\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithSpecialCharactersAndWhitespace()
    {
        // Given - content contains special regex characters with flexible whitespace
        var currentContent = "\tvar\tx\t=\t$10.00;\n";
        var originContent = "var x = $10.00;";
        var newContent = "var y = $20.00;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with special chars and whitespace", result.isSuccess());
        // Note: newContent is used as-is (with spaces), but leading tab is preserved
        Assert.assertEquals("Content should be modified", "\tvar y = $20.00;\n", result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceSingleOccurrenceWithSpecialCharacters()
    {
        // Given - content with special regex characters, single occurrence
        var currentContent = "obj.prop = value;\nobj.prop = other;\n";
        var originContent = "obj.prop = value;";
        var newContent = "obj.attr = data;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Replacement should succeed with special characters", result.isSuccess());
        Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertEquals("Content should be modified", "obj.attr = data;\nobj.prop = other;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceAllOccurrencesWithSpecialCharacters()
    {
        // Given - content with special regex characters, multiple occurrences
        var currentContent = "obj.method().call();\nobj.method().get();\nobj.method().set();\n";
        var originContent = "obj.method()";
        var newContent = "obj.action()";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed for all occurrences", result.isSuccess());
        Assert.assertEquals("Content should replace all occurrences",
            "obj.action().call();\nobj.action().get();\nobj.action().set();\n",
            result.getUpdatedContent());
    }

    // ===== Unicode and Emoji Handling Tests =====

    @Test
    public void shouldReplaceContentWithBasicEmojis()
    {
        // Given - content contains basic emojis
        var currentContent = "status = \"✅ success\";\nresult = \"❌ failed\";\n";
        var originContent = "status = \"✅ success\";";
        var newContent = "status = \"⚠️ warning\";";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with basic emojis", result.isSuccess());
        Assert.assertEquals("Content should be modified", "status = \"⚠️ warning\";\nresult = \"❌ failed\";\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithUnicodeCyrillic()
    {
        // Given - content contains Cyrillic characters
        var currentContent = "переменная = 10;\nзначение = 20;\n";
        var originContent = "переменная = 10;";
        var newContent = "переменная = 15;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with Cyrillic", result.isSuccess());
        Assert.assertEquals("Content should be modified", "переменная = 15;\nзначение = 20;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithUnicodeGreek()
    {
        // Given - content contains Greek characters
        var currentContent = "α = 1;\nβ = 2;\nγ = 3;\n";
        var originContent = "α = 1;";
        var newContent = "α = 10;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with Greek", result.isSuccess());
        Assert.assertEquals("Content should be modified", "α = 10;\nβ = 2;\nγ = 3;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithUnicodeChinese()
    {
        // Given - content contains Chinese characters
        var currentContent = "变量 = 100;\n结果 = 200;\n";
        var originContent = "变量 = 100;";
        var newContent = "变量 = 150;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with Chinese", result.isSuccess());
        Assert.assertEquals("Content should be modified", "变量 = 150;\n结果 = 200;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithUnicodeArabic()
    {
        // Given - content contains Arabic characters
        var currentContent = "متغير = 10;\nقيمة = 20;\n";
        var originContent = "متغير = 10;";
        var newContent = "متغير = 15;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with Arabic", result.isSuccess());
        Assert.assertEquals("Content should be modified", "متغير = 15;\nقيمة = 20;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithSimpleEmoji()
    {
        // Given - content contains simple emojis without ZWJ
        var currentContent = "status = \"✅ OK\";\nresult = \"❌ FAIL\";\n";
        var originContent = "status = \"✅ OK\";";
        var newContent = "status = \"⚠️ WARN\";";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with simple emojis", result.isSuccess());
        Assert.assertEquals("Content should be modified", "status = \"⚠️ WARN\";\nresult = \"❌ FAIL\";\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithMathematicalSymbols()
    {
        // Given - content contains mathematical Unicode symbols
        var currentContent = "∑ = 0;\n∞ = 100;\nπ = 3.14;\n";
        var originContent = "∑ = 0;";
        var newContent = "∑ = 10;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with mathematical symbols", result.isSuccess());
        Assert.assertEquals("Content should be modified", "∑ = 10;\n∞ = 100;\nπ = 3.14;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithCurrencySymbols()
    {
        // Given - content contains currency symbols
        var currentContent = "price = €100;\ncost = $50;\namount = ¥1000;\n";
        var originContent = "price = €100;";
        var newContent = "price = €150;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with currency symbols", result.isSuccess());
        Assert.assertEquals("Content should be modified", "price = €150;\ncost = $50;\namount = ¥1000;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithArrows()
    {
        // Given - content contains arrow symbols
        var currentContent = "pointer → target;\narrow ← source;\n";
        var originContent = "pointer → target;";
        var newContent = "pointer → destination;";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with arrows", result.isSuccess());
        Assert.assertEquals("Content should be modified", "pointer → destination;\narrow ← source;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithBoxDrawing()
    {
        // Given - content contains box drawing characters
        var currentContent = "border = \"┌───┐\";\nframe = \"│   │\";\n";
        var originContent = "border = \"┌───┐\";";
        var newContent = "border = \"┌─────┐\";";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with box drawing", result.isSuccess());
        Assert.assertEquals("Content should be modified", "border = \"┌─────┐\";\nframe = \"│   │\";\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithMixedUnicodeAndEmoji()
    {
        // Given - content contains mixed Unicode and emoji
        var currentContent = "переменная = 10; ✅\nзначение = 20; ❌\n";
        var originContent = "переменная = 10; ✅";
        var newContent = "переменная = 15; ⚠️";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with mixed content", result.isSuccess());
        Assert.assertEquals("Content should be modified", "переменная = 15; ⚠️\nзначение = 20; ❌\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceSingleOccurrenceWithEmoji()
    {
        // Given - content with emoji, single occurrence
        var currentContent = "status = \"✅ success\";\nstatus = \"✅ success\";\n";
        var originContent = "status = \"✅ success\";";
        var newContent = "status = \"⚠️ warning\";";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Replacement should fail with multiple occurrences", !result.isSuccess());
        Assert.assertTrue("Should detect multiple occurrences", result.hasMultipleOccurrences());
    }

    @Test
    public void shouldReplaceAllOccurrencesWithEmoji()
    {
        // Given - content with emoji, multiple occurrences
        var currentContent = "✅ task1;\n✅ task2;\n✅ task3;\n";
        var originContent = "✅";
        var newContent = "⚠️";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed for all emoji", result.isSuccess());
        Assert.assertEquals("Content should replace all emojis",
            "⚠️ task1;\n⚠️ task2;\n⚠️ task3;\n",
            result.getUpdatedContent());
    }

    @Test
    public void shouldReplaceContentWithEmojiAndWhitespace()
    {
        // Given - emoji with flexible whitespace
        var currentContent = "\t✅\tsuccess\t\n\t❌\tfail\t\n";
        var originContent = "✅ success";
        var newContent = "⚠️ warning";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Replacement should succeed with emoji and whitespace", result.isSuccess());
        // Note: newContent is used as-is, but leading/trailing whitespace is preserved
        Assert.assertEquals("Content should be modified", "\t⚠️ warning\t\n\t❌\tfail\t\n",
            result.getUpdatedContent());
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

    // ===== Editing Tests with Markers =====

    @Test
    public void shouldEditMarkerMessage()
    {
        // Given - JSON-like content with marker message to edit
        var currentContent = "    {\n" + "      \"message\": \"AI error (AIError)\",\n"
            + "      \"severity\": \"error\",\n" + "      \"priority\": \"high\"\n" + "    }";
        var originContent = "      \"message\": \"AI error (AIError)\",";
        var newContent = "      \"message\": \"AI warning (AIWarning)\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Marker message edit should succeed", result.isSuccess());
        Assert.assertFalse("Should not have multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertTrue("Updated content should contain new message",
            result.getUpdatedContent().contains("AI warning (AIWarning)"));
        Assert.assertFalse("Updated content should not contain old message",
            result.getUpdatedContent().contains("AI error (AIError)"));
        Assert.assertEquals("Should remove 1 line", 1, result.getRemovedLines());
        Assert.assertEquals("Should add 1 line", 1, result.getAddedLines());
    }

    @Test
    public void shouldEditMarkerSeverity()
    {
        // Given - edit marker severity level
        var currentContent = "    {\n" + "    \"id\": 4002,\n" + "    \"severity\": \"error\",\n"
            + "    \"priority\": \"high\"\n" + "    }";
        var originContent = "    \"severity\": \"error\",";
        var newContent = "    \"severity\": \"warning\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Severity edit should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain warning severity",
            result.getUpdatedContent().contains("\"severity\": \"warning\""));
        Assert.assertFalse("Updated content should not contain error severity",
            result.getUpdatedContent().contains("\"severity\": \"error\""));
    }

    @Test
    public void shouldEditMarkerId()
    {
        // Given - edit marker ID
        var currentContent = "    {\n" + "    \"id\": 4002,\n" + "    \"message\": \"AI error\",\n"
            + "    \"type\": \"ai_marker\"\n" + "    }";
        var originContent = "    \"id\": 4002,";
        var newContent = "    \"id\": 5001,";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("ID edit should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain new ID", result.getUpdatedContent().contains("\"id\": 5001"));
        Assert.assertFalse("Updated content should not contain old ID",
            result.getUpdatedContent().contains("\"id\": 4002"));
    }

    @Test
    public void shouldEditMarkerType()
    {
        // Given - edit marker type from ai_marker to problem
        var currentContent = "    {\n" + "    \"id\": 4002,\n" + "    \"message\": \"AI error\",\n"
            + "    \"type\": \"ai_marker\",\n" + "    \"severity\": \"error\"\n" + "    }";
        var originContent = "    \"type\": \"ai_marker\",";
        var newContent = "    \"type\": \"problem\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Marker type edit should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain problem type",
            result.getUpdatedContent().contains("\"type\": \"problem\""));
        Assert.assertFalse("Updated content should not contain ai_marker type",
            result.getUpdatedContent().contains("\"type\": \"ai_marker\""));
    }

    @Test
    public void shouldEditMarkerPriority()
    {
        // Given - edit marker priority
        var currentContent = "    {\n" + "    \"severity\": \"error\",\n" + "    \"priority\": \"high\",\n"
            + "    \"message\": \"AI error\"\n" + "    }";
        var originContent = "    \"priority\": \"high\",";
        var newContent = "    \"priority\": \"normal\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Priority edit should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain normal priority",
            result.getUpdatedContent().contains("\"priority\": \"normal\""));
        Assert.assertFalse("Updated content should not contain high priority",
            result.getUpdatedContent().contains("\"priority\": \"high\""));
    }

    @Test
    public void shouldEditMarkerStartLine()
    {
        // Given - edit marker line number
        var currentContent = "    {\n" + "    \"id\": 4002,\n" + "    \"start_line\": 45,\n"
            + "    \"message\": \"AI error\"\n" + "    }";
        var originContent = "    \"start_line\": 45,";
        var newContent = "    \"start_line\": 50,";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Start line edit should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain new line number",
            result.getUpdatedContent().contains("\"start_line\": 50"));
        Assert.assertFalse("Updated content should not contain old line number",
            result.getUpdatedContent().contains("\"start_line\": 45"));
    }

    @Test
    public void shouldEditMultipleMarkersWithSameField()
    {
        // Given - multiple markers with severity field, edit all
        var currentContent = "  \"markers\": [\n" + "    {\n" + "    \"id\": 4002,\n" + "    \"severity\": \"error\"\n"
            + "    },\n" + "    {\n" + "    \"id\": 1001,\n" + "    \"severity\": \"error\"\n" + "    },\n" + "    {\n"
            + "    \"id\": 4001,\n" + "    \"severity\": \"warning\"\n" + "    }\n" + "  ]";
        var originContent = "    \"severity\": \"error\"";
        var newContent = "    \"severity\": \"info\"";

        // When - replace all occurrences
        var result = replacer.replace(currentContent, originContent, newContent, LF, true);

        // Then
        Assert.assertTrue("Multiple marker edit should succeed", result.isSuccess());
        Assert.assertEquals("Should have 2 info markers now", 2,
            countOccurrences(result.getUpdatedContent(), "\"severity\": \"info\""));
        Assert.assertEquals("Should have no error markers now", 0,
            countOccurrences(result.getUpdatedContent(), "\"severity\": \"error\""));
        Assert.assertEquals("Should preserve warning marker", 1,
            countOccurrences(result.getUpdatedContent(), "\"severity\": \"warning\""));
        Assert.assertEquals("Should remove 2 lines", 2, result.getRemovedLines());
        Assert.assertEquals("Should add 2 lines", 2, result.getAddedLines());
    }

    @Test
    public void shouldFailWhenMultipleMarkersWithSameIdForSingleEdit()
    {
        // Given - multiple markers with same ID pattern
        var currentContent = "  \"markers\": [\n" + "    {\n" + "    \"id\": 4002,\n" + "    \"severity\": \"error\"\n"
            + "    },\n" + "    {\n" + "    \"id\": 4002,\n" + "    \"severity\": \"warning\"\n" + "    }\n" + "  ]";
        var originContent = "    \"id\": 4002,";
        var newContent = "    \"id\": 4003,";

        // When - single replacement mode
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertFalse("Should fail due to multiple occurrences", result.isSuccess());
        Assert.assertTrue("Should detect multiple occurrences", result.hasMultipleOccurrences());
        Assert.assertEquals("Content should remain unchanged", currentContent, result.getUpdatedContent());
    }

    @Test
    public void shouldEditComplexMarkerWithNestedStructure()
    {
        // Given - marker with nested JSON structure
        var currentContent = "  {\n" + "    \"markers\": [\n" + "    {\n" + "      \"id\": 4002,\n"
            + "      \"absolute_path\": \"/path/to/project/MyProject/CommonModules/AIModule/Module.bsl\",\n"
            + "      \"relative_path\": \"CommonModules/AIModule/Module.bsl\",\n" + "      \"start_line\": 45,\n"
            + "      \"message\": \"AI error (AIError)\",\n" + "      \"type\": \"ai_marker\",\n"
            + "      \"severity\": \"error\",\n" + "      \"priority\": \"high\",\n"
            + "      \"marker_highlighted_text\": \"calculateTotal(items)\"\n" + "    }\n" + "    ],\n"
            + "    \"total_count\": 5,\n" + "    \"returned_count\": 3\n" + "  }";
        var originContent = "      \"message\": \"AI error (AIError)\",";
        var newContent = "      \"message\": \"AI warning - review suggested\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Complex marker edit should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain new message",
            result.getUpdatedContent().contains("AI warning - review suggested"));
        Assert.assertTrue("Should preserve other marker fields",
            result.getUpdatedContent().contains("\"id\": 4002")
                && result.getUpdatedContent().contains("\"severity\": \"error\"")
                && result.getUpdatedContent().contains("calculateTotal(items)"));
    }

    @Test
    public void shouldEditMarkerPreservingLineEndings()
    {
        // Given - marker content with CRLF line endings
        var currentContent = "{\r\n" + "  \"id\": 4002,\r\n" + "  \"severity\": \"error\",\r\n"
            + "  \"message\": \"AI error\"\r\n" + "}";
        var originContent = "  \"severity\": \"error\",";
        var newContent = "  \"severity\": \"warning\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, CRLF, false);

        // Then
        Assert.assertTrue("Edit should preserve CRLF endings", result.isSuccess());
        Assert.assertTrue("Updated content should use CRLF", result.getUpdatedContent().contains("\r\n"));
        Assert.assertTrue("Content should end with brace",
            result.getUpdatedContent().endsWith("}"));
    }

    @Test
    public void shouldEditMarkerWithSpecialCharactersInMessage()
    {
        // Given - marker with special characters
        var currentContent =
            "{\n" + "  \"message\": \"Error: unexpected token ';' at line 45\",\n" + "  \"type\": \"problem\"\n" + "}";
        var originContent = "  \"message\": \"Error: unexpected token ';' at line 45\",";
        var newContent = "  \"message\": \"Warning: consider using semicolon at line 45\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Edit with special characters should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain warning", result.getUpdatedContent().contains("Warning"));
        Assert.assertTrue("Should preserve semicolon and line number",
            result.getUpdatedContent().contains("at line 45"));
        Assert.assertFalse("Should not contain old error message",
            result.getUpdatedContent().contains("Error: unexpected token"));
    }

    @Test
    public void shouldEditEmptyMarkerMessage()
    {
        // Given - marker with empty message
        var currentContent = "{\n" + "  \"message\": \"\",\n" + "  \"severity\": \"error\"\n" + "}";
        var originContent = "  \"message\": \"\",";
        var newContent = "  \"message\": \"Error description added\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Edit from empty message should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain new message",
            result.getUpdatedContent().contains("Error description added"));
        Assert.assertFalse("Should not contain empty message",
            result.getUpdatedContent().contains("\"message\": \"\""));
    }

    @Test
    public void shouldEditToEmptyMarkerMessage()
    {
        // Given - marker with message to be cleared
        var currentContent = "{\n" + "  \"message\": \"Old message\",\n" + "  \"severity\": \"error\"\n" + "}";
        var originContent = "  \"message\": \"Old message\",";
        var newContent = "  \"message\": \"\",";

        // When
        var result = replacer.replace(currentContent, originContent, newContent, LF, false);

        // Then
        Assert.assertTrue("Edit to empty message should succeed", result.isSuccess());
        Assert.assertTrue("Updated content should contain empty message",
            result.getUpdatedContent().contains("\"message\": \"\""));
        Assert.assertFalse("Should not contain old message", result.getUpdatedContent().contains("Old message"));
    }

    @Test
    public void shouldEditMultipleFieldsInMarker()
    {
        // Given - consecutive edits to different fields
        var currentContent =
            "{\n" + "  \"id\": 4002,\n" + "  \"severity\": \"error\",\n" + "  \"priority\": \"high\"\n" + "}";

        // When - first edit: severity
        var result1 =
            replacer.replace(currentContent, "  \"severity\": \"error\",", "  \"severity\": \"warning\",", LF, false);
        Assert.assertTrue("First edit should succeed", result1.isSuccess());

        // When - second edit: priority on result
        var result2 = replacer.replace(result1.getUpdatedContent(), "  \"priority\": \"high\"",
            "  \"priority\": \"normal\"", LF, false);

        // Then
        Assert.assertTrue("Second edit should succeed", result2.isSuccess());
        Assert.assertTrue("Final content should have both changes",
            result2.getUpdatedContent().contains("\"severity\": \"warning\"")
                && result2.getUpdatedContent().contains("\"priority\": \"normal\""));
    }

    // Helper method to count occurrences of a substring
    private int countOccurrences(String text, String substring)
    {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1)
        {
            count++;
            index += substring.length();
        }
        return count;
    }
}
