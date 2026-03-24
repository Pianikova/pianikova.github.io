/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
* Tests for MarkdownUtils.escapeForMarkdown method
*/
@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class MarkdownUtilsTest
{
    private final ILinkProvider linkProvider = mock(ILinkProvider.class);
    private final IFiles files = mock(IFiles.class);

    @Parameter(0)
    public String input;

    @Parameter(1)
    public String expectedOutput;

    @Test
    public void testEscapeForMarkdown()
    {
        // Given - create MarkdownUtils instance only once
        var markdownUtils = new MarkdownUtils(linkProvider, files);

        // When
        var actualOutput = markdownUtils.escapeForMarkdown(input);

        // Then
        assertEquals(expectedOutput, actualOutput);
    }

    @Parameters(name = "{index}: escapeForMarkdown({0})")
    public static Collection<Object[]> testData()
    {
        // @formatter:off
        return Arrays.asList(new Object[][] {
            // Null input
            { null, "" },

            // Empty string
            { "", "" },

            // Plain text without special characters
            { "Hello World", "Hello World" },

            // Backslash (the most important - must be escaped first)
            { "\\", "\\\\" },
            { "\\path\\to\\file", "\\\\path\\\\to\\\\file" },

            // Backtick - used for inline code
            { "`", "\\`" },
            { "`code`", "\\`code\\`" },

            // Asterisk - used for bold/italic
            { "*", "\\*" },
            { "**bold**", "\\*\\*bold\\*\\*" },
            { "*italic*", "\\*italic\\*" },

            // Underscore - used for italic
            { "_", "\\_" },
            { "_italic_", "\\_italic\\_" },
            { "__bold__", "\\_\\_bold\\_\\_" },

            // Hash mark - used for headers
            { "#", "\\#" },
            { "# Heading", "\\# Heading" },
            { "## Heading 2", "\\#\\# Heading 2" },

            // Square brackets - used for links
            { "[", "\\[" },
            { "]", "\\]" },
            { "[link text](url)", "\\[link text\\]\\(url\\)" },

            // Parentheses - used for links
            { "(", "\\(" },
            { ")", "\\)" },
            { "(example)", "\\(example\\)" },

            // Curly braces
            { "{", "\\{" },
            { "}", "\\}" },
            { "{key: value}", "\\{key: value\\}" },

            // Plus sign
            { "+", "\\+" },
            { "+ item", "\\+ item" },

            // Minus sign - used for lists and emphasis
            { "-", "\\-" },
            { "- item", "\\- item" },
            { "--", "\\-\\-" },
            { "---", "\\-\\-\\-" },

            // Dot
            { ".", "\\." },
            { "item.", "item\\." },
            { "...", "\\.\\.\\." },

            // Exclamation mark
            { "!", "\\!" },
            { "![alt](url)", "\\!\\[alt\\]\\(url\\)" },

            // Combination of multiple special characters
            { "*bold_ and `code`*", "\\*bold\\_ and \\`code\\`\\*" },

            // Real-world example: file paths
            { "C:\\Users\\Name\\file.txt", "C:\\\\Users\\\\Name\\\\file\\.txt" },

            // Real-world example: code snippets
            { "String s = \"test\";", "String s = \"test\";" },

            // Real-world example: markdown-like content
            { "# Header", "\\# Header" },
            { "**Bold** and _italic_", "\\*\\*Bold\\*\\* and \\_italic\\_" },

            // Multiple special characters in sequence
            { "***", "\\*\\*\\*" },
            { "___", "\\_\\_\\_" },

            // Common patterns
            { "var x = 1 + 2;", "var x = 1 \\+ 2;" },
            { "if (x > 0)", "if \\(x > 0\\)" },

            // Mixed content
            { "Test *with* _different_ `special` chars", "Test \\*with\\* \\_different\\_ \\`special\\` chars" },

        });
        // @formatter:on
    }
}
